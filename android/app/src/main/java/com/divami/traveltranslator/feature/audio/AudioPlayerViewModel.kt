package com.divami.traveltranslator.feature.audio

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.data.repository.SpeechRepository
import com.divami.traveltranslator.data.repository.SynthesizedAudio
import com.divami.traveltranslator.domain.model.PlaybackSpeed
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject

data class AudioUiState(
    val preparing: Boolean = false,
    val playing: Boolean = false,
    val error: String? = null,
)

/**
 * Plays a phrase's audio: primary path is backend TTS (`/tts/synthesize`) via
 * [MediaPlayer]; if the backend can't synthesize the language (e.g. Telugu/Thai
 * return 415 `client_side_tts_required`), it falls back to the on-device
 * [TextToSpeech] engine. Owns and releases both players.
 */
@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val speechRepository: SpeechRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AudioUiState())
    val state: StateFlow<AudioUiState> = _state.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var tempFile: File? = null

    // An utterance requested before the TTS engine finished initializing; spoken
    // from the init callback once the engine is ready.
    private data class Pending(val text: String, val languageCode: String, val slow: Boolean)
    private var pending: Pending? = null

    init {
        // Warm the on-device engine up front so the first tap doesn't fire before
        // the voice is loaded (the "random sounds on first play" symptom).
        ensureEngine()
    }

    fun toggle(text: String, languageCode: String, slow: Boolean) {
        if (_state.value.playing || _state.value.preparing) stop() else play(text, languageCode, slow)
    }

    fun play(text: String, languageCode: String, slow: Boolean) {
        if (text.isBlank()) return
        stopInternal()
        _state.update { it.copy(preparing = true, playing = false, error = null) }

        // Everything except English: use the on-device engine directly. The
        // backend's Replicate (chatterbox) voice is English-centric and renders
        // other languages inconsistently (or 415s outright); the device engines
        // are consistent and cover the full supported-language list.
        if (languageCode != SERVER_TTS_LANG) {
            speakOnDevice(text, languageCode, slow)
            return
        }

        viewModelScope.launch {
            val speed = if (slow) PlaybackSpeed.Slow else PlaybackSpeed.Normal
            when (val result = speechRepository.synthesize(text, languageCode, speed)) {
                is ApiResult.Success -> playBytes(result.data)
                is ApiResult.Failure -> speakOnDevice(text, languageCode, slow)
            }
        }
    }

    fun stop() {
        stopInternal()
        _state.update { it.copy(preparing = false, playing = false) }
    }

    private suspend fun playBytes(audio: SynthesizedAudio) {
        val file = withContext(Dispatchers.IO) {
            File.createTempFile("tts_", ".audio", context.cacheDir).apply { writeBytes(audio.bytes) }
        }
        tempFile = file
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnPreparedListener {
                start()
                _state.update { it.copy(preparing = false, playing = true) }
            }
            setOnCompletionListener {
                _state.update { it.copy(playing = false) }
            }
            setOnErrorListener { _, _, _ ->
                _state.update { it.copy(preparing = false, playing = false, error = "Playback failed.") }
                true
            }
            prepareAsync()
        }
    }

    /** Creates the on-device TTS engine once; safe to call repeatedly. */
    private fun ensureEngine() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            val queued = pending
            pending = null
            val engine = tts
            when {
                ttsReady && queued != null && engine != null ->
                    doSpeak(engine, queued.text, queued.languageCode, queued.slow)
                !ttsReady && queued != null ->
                    _state.update {
                        it.copy(preparing = false, playing = false, error = "Audio isn't available on this device.")
                    }
            }
        }
    }

    /** Speak via the on-device engine, deferring until it's initialized. */
    private fun speakOnDevice(text: String, languageCode: String, slow: Boolean) {
        ensureEngine()
        val engine = tts
        if (ttsReady && engine != null) {
            doSpeak(engine, text, languageCode, slow)
        } else {
            // Engine still initializing — remember what to say; the init callback fires it.
            pending = Pending(text, languageCode, slow)
        }
    }

    private fun doSpeak(engine: TextToSpeech, text: String, languageCode: String, slow: Boolean) {
        val locale = Locale.forLanguageTag(languageCode)
        // Set the language and verify the voice is actually installed. If the data
        // is missing we must NOT speak — the engine would render the script with the
        // default (wrong) voice, which is the "random sounds" the user heard.
        val availability = engine.setLanguage(locale)
        if (availability < TextToSpeech.LANG_AVAILABLE) {
            _state.update {
                it.copy(
                    preparing = false,
                    playing = false,
                    error = "The ${locale.displayLanguage} voice isn't installed. " +
                        "Add it in your phone's Text-to-speech settings.",
                )
            }
            return
        }
        engine.setSpeechRate(if (slow) 0.6f else 1.0f)
        engine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.update { it.copy(preparing = false, playing = true) }
            }
            override fun onDone(utteranceId: String?) {
                _state.update { it.copy(playing = false) }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.update { it.copy(playing = false, error = "Couldn't play audio for this language.") }
            }
        })
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts-${text.hashCode()}")
    }

    private fun stopInternal() {
        pending = null
        mediaPlayer?.run { runCatching { stop() }; release() }
        mediaPlayer = null
        tts?.stop()
        tempFile?.delete()
        tempFile = null
    }

    override fun onCleared() {
        stopInternal()
        tts?.shutdown()
        tts = null
        super.onCleared()
    }

    private companion object {
        /**
         * The only language whose backend (chatterbox) voice is validated.
         * Every other language speaks via the on-device engine.
         */
        const val SERVER_TTS_LANG = "en"
    }
}
