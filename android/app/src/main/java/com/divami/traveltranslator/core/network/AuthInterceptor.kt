package com.divami.traveltranslator.core.network

import com.divami.traveltranslator.data.local.SessionStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches `Authorization: Bearer <token>` to every request when a session
 * token exists. Auth-free endpoints (`auth/verify`, `auth/guest`) simply ignore
 * the header, so there's no need to special-case them here.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionStore.blockingToken()
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
