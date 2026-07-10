package com.divami.traveltranslator.feature.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.data.repository.SpeechRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import javax.inject.Inject

enum class VoicePhase { Recording, Transcribing, Review }

data class VoiceUiState(
    val phase: VoicePhase = VoicePhase.Recording,
    val transcript: String = "",
    val error: String? = null,
)

/**
 * Records the user's speech and transcribes it via the backend STT endpoint.
 * The screen supplies mic-permission gating; this owns the recorder + network
 * round-trip and exposes a simple phase machine.
 */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val speechRepository: SpeechRepository,
) : ViewModel() {

    private val recorder = AudioRecorder(context)

    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    /** Language hint for Whisper; null lets the backend auto-detect. */
    private var languageHint: String? = null

    fun startRecording(languageCode: String? = null) {
        languageHint = languageCode
        _state.value = VoiceUiState(phase = VoicePhase.Recording)
        runCatching { recorder.start() }.onFailure {
            _state.value = VoiceUiState(phase = VoicePhase.Review, error = "Couldn't access the microphone.")
        }
    }

    fun stopAndTranscribe() {
        val file = recorder.stop()
        if (file == null) {
            _state.update { it.copy(phase = VoicePhase.Review, error = "No audio captured — try again.") }
            return
        }
        _state.update { it.copy(phase = VoicePhase.Transcribing, error = null) }
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) { file.readBytes() }
            val result = speechRepository.transcribe(
                audioBytes = bytes,
                audioContentType = "audio/mp4",
                fileName = file.name,
                language = languageHint,
            )
            withContext(Dispatchers.IO) { file.delete() }
            when (result) {
                is ApiResult.Success ->
                    _state.update { it.copy(phase = VoicePhase.Review, transcript = result.data.transcript) }
                is ApiResult.Failure ->
                    _state.update { it.copy(phase = VoicePhase.Review, error = result.error.message) }
            }
        }
    }

    fun onTranscriptChange(text: String) = _state.update { it.copy(transcript = text) }

    fun recordAgain() = startRecording(languageHint)

    fun cancel() = recorder.cancel()

    /** Live mic amplitude (0..32767) for the inline waveform. */
    fun amplitude(): Int = recorder.amplitude()

    /** Reset to the initial idle state after a result has been consumed. */
    fun clear() {
        _state.value = VoiceUiState()
    }

    override fun onCleared() {
        recorder.cancel()
        super.onCleared()
    }
}
