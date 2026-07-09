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

    fun toggle(text: String, languageCode: String, slow: Boolean) {
        if (_state.value.playing || _state.value.preparing) stop() else play(text, languageCode, slow)
    }

    fun play(text: String, languageCode: String, slow: Boolean) {
        if (text.isBlank()) return
        stopInternal()
        _state.update { it.copy(preparing = true, playing = false, error = null) }
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

    /** On-device fallback for languages the backend won't synthesize. */
    private fun speakOnDevice(text: String, languageCode: String, slow: Boolean) {
        val engine = tts
        if (engine != null && ttsReady) {
            doSpeak(engine, text, languageCode, slow)
            return
        }
        // Lazily create the engine and speak once it reports ready. The init
        // callback runs off the main thread; StateFlow updates are thread-safe.
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            val ready = tts
            if (ttsReady && ready != null) {
                doSpeak(ready, text, languageCode, slow)
            } else {
                _state.update { it.copy(preparing = false, playing = false, error = "Audio isn't available for this language.") }
            }
        }
    }

    private fun doSpeak(engine: TextToSpeech, text: String, languageCode: String, slow: Boolean) {
        val locale = Locale.forLanguageTag(languageCode)
        if (engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = locale
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
}
