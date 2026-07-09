package com.divami.traveltranslator.core.network

/**
 * Outcome of a single network call. Repositories return this instead of
 * throwing, so ViewModels handle success and failure exhaustively via `when`.
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

/** A user-presentable failure plus the machine-readable classification. */
data class ApiError(
    val kind: Kind,
    val message: String,
    val httpCode: Int? = null,
    /** Backend `detail` string for 4xx/5xx, when parseable. */
    val serverDetail: String? = null,
) {
    enum class Kind {
        /** No connectivity / DNS / socket failure. */
        Network,

        /** Request timed out. */
        Timeout,

        /** 401/403 — token missing, expired or rejected. */
        Unauthorized,

        /** 4xx other than auth — bad request, validation, unsupported media. */
        Client,

        /** 5xx — backend or upstream provider failure. */
        Server,

        /** Anything we could not classify. */
        Unknown,
    }
}

/** Map the result's value while preserving a [ApiResult.Failure] unchanged. */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

/** The value on success, or `null` on failure. */
fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data
