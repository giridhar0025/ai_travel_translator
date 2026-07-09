package com.divami.traveltranslator.core.network

import com.divami.traveltranslator.BuildConfig
import com.divami.traveltranslator.data.remote.TravelTranslatorApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            // TTS/STT hit slow HF/Replicate upstreams (backend allows up to 90s).
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(100, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(normalizedApiBaseUrl())
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): TravelTranslatorApi =
        retrofit.create(TravelTranslatorApi::class.java)

    /**
     * Ensures the base URL ends with the `/api/v1/` prefix and a trailing slash
     * (Retrofit requires the latter). Accepts a host with or without the prefix
     * in `BuildConfig.BACKEND_BASE_URL`.
     */
    private fun normalizedApiBaseUrl(): String {
        val root = BuildConfig.BACKEND_BASE_URL.trimEnd('/')
        val withPrefix = if (root.endsWith("/api/v1")) root else "$root/api/v1"
        return "$withPrefix/"
    }
}
