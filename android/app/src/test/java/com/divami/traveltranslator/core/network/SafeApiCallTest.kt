package com.divami.traveltranslator.core.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class SafeApiCallTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun httpError(code: Int, body: String): HttpException =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    @Test
    fun `success wraps value`() = runTest {
        val result = safeApiCall(json) { 42 }
        assertEquals(ApiResult.Success(42), result)
    }

    @Test
    fun `IOException maps to Network`() = runTest {
        val result = safeApiCall<Int>(json) { throw IOException("offline") }
        assertEquals(ApiError.Kind.Network, (result as ApiResult.Failure).error.kind)
    }

    @Test
    fun `timeout maps to Timeout`() = runTest {
        val result = safeApiCall<Int>(json) { throw SocketTimeoutException() }
        assertEquals(ApiError.Kind.Timeout, (result as ApiResult.Failure).error.kind)
    }

    @Test
    fun `401 maps to Unauthorized`() = runTest {
        val result = safeApiCall<Int>(json) { throw httpError(401, """{"detail":"missing bearer token"}""") }
        assertEquals(ApiError.Kind.Unauthorized, (result as ApiResult.Failure).error.kind)
    }

    @Test
    fun `400 surfaces server detail string`() = runTest {
        val result = safeApiCall<Int>(json) { throw httpError(400, """{"detail":"unsupported language: xx"}""") }
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Client, error.kind)
        assertEquals("unsupported language: xx", error.serverDetail)
        assertEquals("unsupported language: xx", error.message)
    }

    @Test
    fun `415 nested object detail is parsed`() = runTest {
        val body = """{"detail":{"detail":"client_side_tts_required","language":"te"}}"""
        val result = safeApiCall<Int>(json) { throw httpError(415, body) }
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Client, error.kind)
        assertEquals("client_side_tts_required", error.serverDetail)
    }

    @Test
    fun `500 maps to Server with generic message`() = runTest {
        val result = safeApiCall<Int>(json) { throw httpError(502, """{"detail":"tts_provider_failed"}""") }
        val error = (result as ApiResult.Failure).error
        assertEquals(ApiError.Kind.Server, error.kind)
        assertTrue(error.message.contains("temporarily unavailable"))
    }
}
