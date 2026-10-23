package com.divami.traveltranslator.data.repository

import com.divami.traveltranslator.core.network.AlwaysOnlineNetworkStatus
import com.divami.traveltranslator.core.network.ApiError
import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.core.network.NetworkStatus
import com.divami.traveltranslator.core.network.map
import com.divami.traveltranslator.core.network.safeApiCall
import com.divami.traveltranslator.data.local.NoopOfflineTranslator
import com.divami.traveltranslator.data.local.OfflineTranslator
import com.divami.traveltranslator.data.remote.TravelTranslatorApi
import com.divami.traveltranslator.data.remote.dto.TranslateRequestDto
import com.divami.traveltranslator.data.remote.dto.toDomain
import com.divami.traveltranslator.domain.model.Translation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** A chunk emitted while a translation streams in. */
sealed interface TranslationChunk {
    /** An incremental piece of translated text. */
    data class Delta(val text: String) : TranslationChunk

    /** The terminal chunk carrying the assembled result + pronunciation. */
    data class Done(val translation: Translation) : TranslationChunk
}

interface TranslationRepository {
    suspend fun translate(
        source: String,
        target: String,
        text: String,
        context: String? = null,
    ): ApiResult<Translation>

    /**
     * Streams a translation token-by-token via the SSE endpoint. Emits
     * [TranslationChunk.Delta]s as text arrives, then one [TranslationChunk.Done].
     * Throws on transport/parse failure so callers can fall back to [translate].
     */
    fun translateStreaming(
        source: String,
        target: String,
        text: String,
        context: String? = null,
    ): Flow<TranslationChunk>
}

@Singleton
class TranslationRepositoryImpl @Inject constructor(
    private val api: TravelTranslatorApi,
    private val json: Json,
    private val offline: OfflineTranslator = NoopOfflineTranslator,
    private val network: NetworkStatus = AlwaysOnlineNetworkStatus,
) : TranslationRepository {
    override suspend fun translate(
        source: String,
        target: String,
        text: String,
        context: String?,
    ): ApiResult<Translation> {
        // No point burning a socket timeout when we already know there is no
        // network — go straight to the on-device model.
        if (!network.isOnline()) {
            return offlineFallback(OFFLINE_ERROR, source, target, text)
                ?: ApiResult.Failure(OFFLINE_ERROR)
        }

        val result = safeApiCall(json) {
            api.translate(TranslateRequestDto(source = source, target = target, text = text, context = context))
        }.map { it.toDomain() }

        return when (result) {
            is ApiResult.Success -> {
                // Download the on-device model while we still have connectivity so
                // this pair keeps working once the signal drops. Best effort only.
                offline.warmUp(source, target)
                result
            }
            is ApiResult.Failure -> offlineFallback(result.error, source, target, text) ?: result
        }
    }

    /**
     * On-device translation for failures the backend cannot fix: no connectivity,
     * a timeout, or an upstream provider error (5xx/502). User errors — a bad
     * language pair or expired auth — are returned unchanged, since translating
     * locally would mask a real bug.
     */
    private suspend fun offlineFallback(
        error: ApiError,
        source: String,
        target: String,
        text: String,
    ): ApiResult<Translation>? {
        val eligible = when (error.kind) {
            ApiError.Kind.Network,
            ApiError.Kind.Timeout,
            ApiError.Kind.Server,
            ApiError.Kind.Unknown,
            -> true

            ApiError.Kind.Unauthorized, ApiError.Kind.Client -> false
        }
        if (!eligible) return null

        val startedAt = System.currentTimeMillis()
        val translated = offline.translate(source, target, text) ?: return null
        val elapsedMs = (System.currentTimeMillis() - startedAt).toInt()

        return ApiResult.Success(
            Translation(
                translation = translated,
                pronunciation = null,
                latencyMs = elapsedMs,
                model = OFFLINE_MODEL,
            ),
        )
    }

    override fun translateStreaming(
        source: String,
        target: String,
        text: String,
        context: String?,
    ): Flow<TranslationChunk> = flow {
        // Fail fast when offline so the caller's fallback runs immediately
        // instead of waiting out the SSE socket timeout.
        if (!network.isOnline()) throw IOException(OFFLINE_MESSAGE)

        val responseBody = api.translateStream(
            TranslateRequestDto(source = source, target = target, text = text, context = context),
        )
        responseBody.use { body ->
            val reader = body.charStream().buffered()
            while (true) {
                val line = reader.readLine() ?: break
                if (!line.startsWith("data:")) continue
                val payload = line.substring(SSE_DATA_PREFIX_LENGTH).trim()
                if (payload.isEmpty()) continue

                val obj = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull() ?: continue

                obj["error"]?.jsonPrimitive?.contentOrNull?.let { throw IOException(it) }

                if (obj["done"]?.jsonPrimitive?.booleanOrNull == true) {
                    emit(
                        TranslationChunk.Done(
                            Translation(
                                translation = obj["translation"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                                pronunciation = obj["pronunciation"]?.jsonPrimitive?.contentOrNull,
                                latencyMs = obj["latency_ms"]?.jsonPrimitive?.intOrNull ?: 0,
                                model = obj["model"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            ),
                        ),
                    )
                    break
                }

                obj["delta"]?.jsonPrimitive?.contentOrNull?.let { emit(TranslationChunk.Delta(it)) }
            }
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val SSE_DATA_PREFIX_LENGTH = 5 // length of "data:"

        /** Marker stored on translations produced on-device by ML Kit. */
        const val OFFLINE_MODEL = "mlkit-offline"

        /** Shown when the device is offline and no on-device model can help. */
        const val OFFLINE_MESSAGE = "No internet connection. Check your network and retry."

        val OFFLINE_ERROR = ApiError(ApiError.Kind.Network, OFFLINE_MESSAGE)
    }
}
