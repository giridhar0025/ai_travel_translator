package com.divami.traveltranslator.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Runs a suspending network [block] and normalises the outcome into an
 * [ApiResult]. This is the single place that knows how to classify transport
 * exceptions and extract the FastAPI `{"detail": ...}` body, so no repository
 * repeats try/catch boilerplate (DRY).
 *
 * [CancellationException] is rethrown so structured-concurrency cancellation
 * propagates correctly rather than being swallowed as a "failure".
 */
suspend fun <T> safeApiCall(
    json: Json,
    block: suspend () -> T,
): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: SocketTimeoutException) {
    ApiResult.Failure(ApiError(ApiError.Kind.Timeout, "The request timed out. Please try again."))
} catch (e: IOException) {
    ApiResult.Failure(ApiError(ApiError.Kind.Network, "No internet connection. Check your network and retry."))
} catch (e: HttpException) {
    ApiResult.Failure(e.toApiError(json))
} catch (e: Exception) {
    ApiResult.Failure(ApiError(ApiError.Kind.Unknown, e.message ?: "Something went wrong."))
}

private fun HttpException.toApiError(json: Json): ApiError {
    val code = code()
    val detail = runCatching {
        response()?.errorBody()?.string()?.let { parseDetail(json, it) }
    }.getOrNull()
    val kind = when (code) {
        401, 403 -> ApiError.Kind.Unauthorized
        in 400..499 -> ApiError.Kind.Client
        in 500..599 -> ApiError.Kind.Server
        else -> ApiError.Kind.Unknown
    }
    val message = when (kind) {
        ApiError.Kind.Unauthorized -> "Your session has expired. Please sign in again."
        ApiError.Kind.Server -> "The service is temporarily unavailable. Please try again."
        else -> detail ?: "Request failed (HTTP $code)."
    }
    return ApiError(kind = kind, message = message, httpCode = code, serverDetail = detail)
}

/**
 * FastAPI's `detail` is usually a string, but some routes (e.g. TTS) return a
 * nested object. Extract a human-readable string from either shape.
 */
private fun parseDetail(json: Json, body: String): String? {
    val element = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return null
    val detail = (element as? JsonObject)?.get("detail") ?: return null
    return detail.asDisplayString()
}

private fun JsonElement.asDisplayString(): String? = when (this) {
    is JsonPrimitive -> contentOrNull
    is JsonObject -> (this["message"] ?: this["detail"])?.jsonPrimitive?.contentOrNull
    else -> null
}
