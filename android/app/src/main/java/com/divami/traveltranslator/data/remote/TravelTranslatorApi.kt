package com.divami.traveltranslator.data.remote

import com.divami.traveltranslator.data.remote.dto.AppendMessageRequestDto
import com.divami.traveltranslator.data.remote.dto.AuthResponseDto
import com.divami.traveltranslator.data.remote.dto.ConversationDetailResponseDto
import com.divami.traveltranslator.data.remote.dto.ConversationDto
import com.divami.traveltranslator.data.remote.dto.ConversationsListResponseDto
import com.divami.traveltranslator.data.remote.dto.CreateConversationRequestDto
import com.divami.traveltranslator.data.remote.dto.FavoriteCreateDto
import com.divami.traveltranslator.data.remote.dto.FavoriteDto
import com.divami.traveltranslator.data.remote.dto.LanguageDto
import com.divami.traveltranslator.data.remote.dto.MeResponseDto
import com.divami.traveltranslator.data.remote.dto.MessageDto
import com.divami.traveltranslator.data.remote.dto.PhraseCategoryDto
import com.divami.traveltranslator.data.remote.dto.PhraseWithTranslationDto
import com.divami.traveltranslator.data.remote.dto.RecentDto
import com.divami.traveltranslator.data.remote.dto.SynthesizeRequestDto
import com.divami.traveltranslator.data.remote.dto.TranscriptionResponseDto
import com.divami.traveltranslator.data.remote.dto.TranslateBatchRequestDto
import com.divami.traveltranslator.data.remote.dto.TranslateBatchResponseDto
import com.divami.traveltranslator.data.remote.dto.TranslateRequestDto
import com.divami.traveltranslator.data.remote.dto.TranslateResultDto
import com.divami.traveltranslator.data.remote.dto.UserSettingsDto
import com.divami.traveltranslator.data.remote.dto.UserSettingsPatchDto
import com.divami.traveltranslator.data.remote.dto.VerifyTokenRequestDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * The complete Travel Translator backend contract. One method per route, typed
 * to the DTOs. The Bearer token is attached transparently by AuthInterceptor,
 * so no method takes an @Header — the auth-free endpoints (auth/verify and
 * auth/guest) simply ignore the header the interceptor may or may not add.
 *
 * Base URL already includes the `/api/v1/` prefix (see NetworkModule), so paths
 * here are relative to it.
 */
interface TravelTranslatorApi {

    // ---- Auth ---------------------------------------------------------------

    @POST("auth/verify")
    suspend fun verify(@Body body: VerifyTokenRequestDto): AuthResponseDto

    @POST("auth/guest")
    suspend fun guest(): AuthResponseDto

    @GET("me")
    suspend fun me(): MeResponseDto

    @DELETE("me")
    suspend fun deleteAccount()

    // ---- Translation --------------------------------------------------------

    @POST("translate")
    suspend fun translate(@Body body: TranslateRequestDto): TranslateResultDto

    @POST("translate/batch")
    suspend fun translateBatch(@Body body: TranslateBatchRequestDto): TranslateBatchResponseDto

    // ---- Speech -------------------------------------------------------------

    @Multipart
    @POST("stt/transcribe")
    suspend fun transcribe(
        @Part audio: MultipartBody.Part,
        @Part("language") language: RequestBody?,
    ): TranscriptionResponseDto

    @Streaming
    @POST("tts/synthesize")
    suspend fun synthesize(@Body body: SynthesizeRequestDto): ResponseBody

    // ---- Catalog ------------------------------------------------------------

    @GET("languages")
    suspend fun languages(): List<LanguageDto>

    @GET("phrases/categories")
    suspend fun phraseCategories(): List<PhraseCategoryDto>

    @GET("phrases")
    suspend fun phrases(
        @Query("category") category: String,
        @Query("target") target: String,
    ): List<PhraseWithTranslationDto>

    // ---- Conversations ------------------------------------------------------

    @GET("conversations")
    suspend fun conversations(
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null,
    ): ConversationsListResponseDto

    @POST("conversations")
    suspend fun createConversation(@Body body: CreateConversationRequestDto): ConversationDto

    @GET("conversations/{id}")
    suspend fun conversationDetail(
        @Path("id") id: String,
        @Query("limit") limit: Int = 50,
        @Query("before_id") beforeId: String? = null,
    ): ConversationDetailResponseDto

    @DELETE("conversations/{id}")
    suspend fun deleteConversation(@Path("id") id: String)

    @POST("conversations/{id}/messages")
    suspend fun appendMessage(
        @Path("id") id: String,
        @Body body: AppendMessageRequestDto,
    ): MessageDto

    // ---- Favorites & recents ------------------------------------------------

    @GET("favorites")
    suspend fun favorites(@Query("kind") kind: String? = null): List<FavoriteDto>

    @POST("favorites")
    suspend fun createFavorite(@Body body: FavoriteCreateDto): FavoriteDto

    @DELETE("favorites/{id}")
    suspend fun deleteFavorite(@Path("id") id: String)

    @GET("recents")
    suspend fun recents(@Query("limit") limit: Int = 20): List<RecentDto>

    // ---- Settings -----------------------------------------------------------

    @GET("settings")
    suspend fun settings(): UserSettingsDto

    @PATCH("settings")
    suspend fun patchSettings(@Body body: UserSettingsPatchDto): UserSettingsDto
}
