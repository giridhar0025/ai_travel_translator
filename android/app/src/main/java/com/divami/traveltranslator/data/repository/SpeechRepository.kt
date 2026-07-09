package com.divami.traveltranslator.data.repository

import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.core.network.map
import com.divami.traveltranslator.core.network.safeApiCall
import com.divami.traveltranslator.data.remote.TravelTranslatorApi
import com.divami.traveltranslator.data.remote.dto.SynthesizeRequestDto
import com.divami.traveltranslator.data.remote.dto.toDomain
import com.divami.traveltranslator.domain.model.PlaybackSpeed
import com.divami.traveltranslator.domain.model.Transcription
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/** Audio synthesised by the backend, ready to be written to a temp file/played. */
data class SynthesizedAudio(
    val bytes: ByteArray,
    val contentType: String,
) {
    // Data-class equals/hashCode on a ByteArray compares references; override so
    // two identical clips are actually equal (and to satisfy lint).
    override fun equals(other: Any?): Boolean =
        this === other || (other is SynthesizedAudio && bytes.contentEquals(other.bytes) && contentType == other.contentType)

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + contentType.hashCode()
}

interface SpeechRepository {
    /** Speech-to-text. [audioContentType] must be one the backend accepts (e.g. `audio/mp4`). */
    suspend fun transcribe(
        audioBytes: ByteArray,
        audioContentType: String,
        fileName: String,
        language: String?,
    ): ApiResult<Transcription>

    /** Text-to-speech. Returns raw audio bytes for playback. */
    suspend fun synthesize(
        text: String,
        language: String,
        speed: PlaybackSpeed,
    ): ApiResult<SynthesizedAudio>
}

@Singleton
class SpeechRepositoryImpl @Inject constructor(
    private val api: TravelTranslatorApi,
    private val json: Json,
) : SpeechRepository {

    override suspend fun transcribe(
        audioBytes: ByteArray,
        audioContentType: String,
        fileName: String,
        language: String?,
    ): ApiResult<Transcription> = safeApiCall(json) {
        val mediaType = audioContentType.toMediaTypeOrNull()
        val audioPart = MultipartBody.Part.createFormData(
            name = "audio",
            filename = fileName,
            body = audioBytes.toRequestBody(mediaType),
        )
        val languagePart = language
            ?.takeIf { it.isNotBlank() }
            ?.toRequestBody("text/plain".toMediaTypeOrNull())
        api.transcribe(audioPart, languagePart)
    }.map { it.toDomain() }

    override suspend fun synthesize(
        text: String,
        language: String,
        speed: PlaybackSpeed,
    ): ApiResult<SynthesizedAudio> = safeApiCall(json) {
        // TTS only supports normal/slow (see backend SynthesizeRequest); collapse
        // very_slow to slow.
        val wireSpeed = if (speed == PlaybackSpeed.Normal) "normal" else "slow"
        val body = api.synthesize(SynthesizeRequestDto(text = text, language = language, speed = wireSpeed))
        SynthesizedAudio(
            bytes = body.bytes(),
            contentType = body.contentType()?.toString() ?: "audio/wav",
        )
    }
}
