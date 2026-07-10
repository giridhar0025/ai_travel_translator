package com.divami.traveltranslator.data.repository

import com.divami.traveltranslator.core.network.ApiError
import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.data.remote.TravelTranslatorApi
import com.divami.traveltranslator.data.remote.dto.ConversationDto
import com.divami.traveltranslator.data.remote.dto.MessageDto
import com.divami.traveltranslator.data.remote.dto.TranslateRequestDto
import com.divami.traveltranslator.data.remote.dto.TranslateResultDto
import com.divami.traveltranslator.domain.model.MessageRole
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `translation repository maps success to domain`() = runTest {
        val api = object : FakeApi() {
            override suspend fun translate(body: TranslateRequestDto): TranslateResultDto {
                assertEquals("en", body.source)
                assertEquals("hi", body.target)
                return TranslateResultDto("नमस्ते", "namaste", 120, "opus-mt")
            }
        }
        val repo = TranslationRepositoryImpl(api, json)
        val result = repo.translate("en", "hi", "hello", "Casual")
        result as ApiResult.Success
        assertEquals("नमस्ते", result.data.translation)
        assertEquals(120, result.data.latencyMs)
    }

    @Test
    fun `translation repository surfaces backend 400 as client error`() = runTest {
        val api = object : FakeApi() {
            override suspend fun translate(body: TranslateRequestDto): TranslateResultDto =
                throw HttpException(
                    Response.error<Any>(400, """{"detail":"same language"}""".toResponseBody("application/json".toMediaType())),
                )
        }
        val repo = TranslationRepositoryImpl(api, json)
        val result = repo.translate("en", "en", "hello", null)
        result as ApiResult.Failure
        assertEquals(ApiError.Kind.Client, result.error.kind)
        assertEquals("same language", result.error.serverDetail)
    }

    @Test
    fun `translation repository streams SSE deltas then done`() = runTest {
        val sse = buildString {
            append("data: {\"delta\": \"ना\"}\n\n")
            append("data: {\"delta\": \"मस्ते\"}\n\n")
            append("data: {\"done\": true, \"translation\": \"नामस्ते\", \"pronunciation\": \"naːmste\", \"latency_ms\": 42, \"model\": \"m\"}\n\n")
        }
        val api = object : FakeApi() {
            override suspend fun translateStream(body: TranslateRequestDto) =
                sse.toResponseBody("text/event-stream".toMediaType())
        }
        val repo = TranslationRepositoryImpl(api, json)

        val chunks = repo.translateStreaming("en", "hi", "hello", null).toList()

        val deltas = chunks.filterIsInstance<TranslationChunk.Delta>().map { it.text }
        val done = chunks.filterIsInstance<TranslationChunk.Done>()
        assertEquals(listOf("ना", "मस्ते"), deltas)
        assertEquals(1, done.size)
        assertEquals("नामस्ते", done.first().translation.translation)
        assertEquals("naːmste", done.first().translation.pronunciation)
        assertEquals(42, done.first().translation.latencyMs)
    }

    @Test
    fun `conversation repository create + append map correctly`() = runTest {
        val api = object : FakeApi() {
            override suspend fun appendMessage(id: String, body: com.divami.traveltranslator.data.remote.dto.AppendMessageRequestDto): MessageDto {
                assertEquals("conv1", id)
                assertEquals("user", body.role)
                return MessageDto(
                    id = "m1", conversationId = id, role = body.role,
                    sourceText = body.sourceText, sourceLang = body.sourceLang,
                    translatedText = body.translatedText, targetLang = body.targetLang,
                    pronunciation = body.pronunciation, createdAt = "2026-07-09T08:00:00Z",
                )
            }
        }
        val repo = ConversationRepositoryImpl(api, json)
        val result = repo.appendMessage("conv1", MessageRole.User, "hi", "en", "नमस्ते", "hi", "namaste")
        result as ApiResult.Success
        assertEquals(MessageRole.User, result.data.role)
        assertEquals("नमस्ते", result.data.translatedText)
        assertTrue(result.data.createdAt.epochSecond > 0)
    }
}

/**
 * Full [TravelTranslatorApi] fake. Every method throws by default; tests
 * override only the calls they exercise. Keeps repository tests network-free
 * and deterministic.
 */
open class FakeApi : TravelTranslatorApi {
    private fun nope(): Nothing = throw NotImplementedError("not stubbed")
    override suspend fun verify(body: com.divami.traveltranslator.data.remote.dto.VerifyTokenRequestDto) = nope()
    override suspend fun guest() = nope()
    override suspend fun me() = nope()
    override suspend fun deleteAccount() = nope()
    override suspend fun translate(body: TranslateRequestDto): TranslateResultDto = nope()
    override suspend fun translateStream(body: TranslateRequestDto): okhttp3.ResponseBody = nope()
    override suspend fun translateBatch(body: com.divami.traveltranslator.data.remote.dto.TranslateBatchRequestDto) = nope()
    override suspend fun transcribe(audio: okhttp3.MultipartBody.Part, language: okhttp3.RequestBody?) = nope()
    override suspend fun synthesize(body: com.divami.traveltranslator.data.remote.dto.SynthesizeRequestDto) = nope()
    override suspend fun languages() = nope()
    override suspend fun phraseCategories() = nope()
    override suspend fun phrases(category: String, target: String) = nope()
    override suspend fun conversations(query: String?, limit: Int, cursor: String?) = nope()
    override suspend fun createConversation(body: com.divami.traveltranslator.data.remote.dto.CreateConversationRequestDto): ConversationDto = nope()
    override suspend fun conversationDetail(id: String, limit: Int, beforeId: String?) = nope()
    override suspend fun deleteConversation(id: String) = nope()
    override suspend fun appendMessage(id: String, body: com.divami.traveltranslator.data.remote.dto.AppendMessageRequestDto): MessageDto = nope()
    override suspend fun favorites(kind: String?) = nope()
    override suspend fun createFavorite(body: com.divami.traveltranslator.data.remote.dto.FavoriteCreateDto) = nope()
    override suspend fun deleteFavorite(id: String) = nope()
    override suspend fun recents(limit: Int) = nope()
    override suspend fun settings() = nope()
    override suspend fun patchSettings(body: com.divami.traveltranslator.data.remote.dto.UserSettingsPatchDto) = nope()
}
