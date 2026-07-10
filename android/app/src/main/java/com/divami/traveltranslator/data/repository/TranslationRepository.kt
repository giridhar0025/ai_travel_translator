package com.divami.traveltranslator.data.repository

import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.core.network.map
import com.divami.traveltranslator.core.network.safeApiCall
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
) : TranslationRepository {
    override suspend fun translate(
        source: String,
        target: String,
        text: String,
        context: String?,
    ): ApiResult<Translation> =
        safeApiCall(json) {
            api.translate(TranslateRequestDto(source = source, target = target, text = text, context = context))
        }.map { it.toDomain() }

    override fun translateStreaming(
        source: String,
        target: String,
        text: String,
        context: String?,
    ): Flow<TranslationChunk> = flow {
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
    }
}
