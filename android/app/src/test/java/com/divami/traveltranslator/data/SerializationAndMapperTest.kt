package com.divami.traveltranslator.data

import com.divami.traveltranslator.data.remote.dto.AuthResponseDto
import com.divami.traveltranslator.data.remote.dto.ConversationDto
import com.divami.traveltranslator.data.remote.dto.FavoriteDto
import com.divami.traveltranslator.data.remote.dto.LanguageDto
import com.divami.traveltranslator.data.remote.dto.PhraseCategoryDto
import com.divami.traveltranslator.data.remote.dto.PhraseWithTranslationDto
import com.divami.traveltranslator.data.remote.dto.TranslateResultDto
import com.divami.traveltranslator.data.remote.dto.toDomain
import com.divami.traveltranslator.data.remote.dto.toInstant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Validates the serialization strategy and DTO→domain mapping against the exact
 * JSON shapes the live backend returns (captured from a real `/auth/guest`,
 * `/languages`, and `/translate` response).
 */
class SerializationAndMapperTest {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    @Test
    fun `decodes real auth guest payload with snake_case fields`() {
        val payload = """
            {"user":{"id":"b44506eb","firebase_uid":null,"email":null,"display_name":null,
            "photo_url":null,"is_guest":true,"timezone":"UTC","created_at":"2026-07-09T07:59:53.621334Z"},
            "settings":{"default_language":"en","playback_speed":"normal","offline_enabled":false},
            "token":"header.payload.sig"}
        """.trimIndent()

        val dto = json.decodeFromString(AuthResponseDto.serializer(), payload)

        assertEquals("b44506eb", dto.user.id)
        assertTrue(dto.user.isGuest)
        assertEquals("header.payload.sig", dto.token)
        assertEquals("en", dto.settings.defaultLanguage)
        // Domain mapping preserves identity + guest flag.
        assertTrue(dto.user.toDomain().isGuest)
    }

    @Test
    fun `decodes language capability flags`() {
        val payload =
            """{"code":"th","name":"Thai","flag_emoji":"🇹🇭","stt_supported":true,"tts_supported":false,"translate_supported":true}"""
        val dto = json.decodeFromString(LanguageDto.serializer(), payload)
        assertEquals("th", dto.code)
        assertEquals(false, dto.ttsSupported)
        assertEquals(true, dto.translateSupported)
        assertEquals("🇹🇭", dto.toDomain().flagEmoji)
    }

    @Test
    fun `decodes translate result and maps latency`() {
        val payload =
            """{"translation":"ट्रेन स्टेशन कहाँ है?","pronunciation":"ʈrenə","latency_ms":4892,"model":"opus-mt"}"""
        val dto = json.decodeFromString(TranslateResultDto.serializer(), payload)
        val domain = dto.toDomain()
        assertEquals("ट्रेन स्टेशन कहाँ है?", domain.translation)
        assertEquals(4892, domain.latencyMs)
        assertEquals("ʈrenə", domain.pronunciation)
    }

    @Test
    fun `conversation timestamps parse to instants`() {
        val payload = """
            {"id":"c1","user_id":"u1","title":"Trip","last_message_at":"2026-07-09T07:59:53.922836Z",
            "message_count":3,"created_at":"2026-07-09T07:59:53.922836Z"}
        """.trimIndent()
        val domain = json.decodeFromString(ConversationDto.serializer(), payload).toDomain()
        assertEquals(3, domain.messageCount)
        assertTrue(domain.lastMessageAt.isAfter(Instant.EPOCH))
    }

    @Test
    fun `toInstant handles Z offset and naive forms and never throws`() {
        assertTrue("Z form", "2026-07-09T07:59:53Z".toInstant().isAfter(Instant.EPOCH))
        assertTrue("offset form", "2026-07-09T07:59:53+00:00".toInstant().isAfter(Instant.EPOCH))
        assertTrue("naive form", "2026-07-09T07:59:53".toInstant().isAfter(Instant.EPOCH))
        // Garbage falls back to EPOCH rather than crashing a whole list.
        assertEquals(Instant.EPOCH, "not-a-date".toInstant())
    }

    @Test
    fun `patch dto omits null fields when explicitNulls is off`() {
        val dto = com.divami.traveltranslator.data.remote.dto.UserSettingsPatchDto(defaultLanguage = "hi")
        val encoded = json.encodeToString(com.divami.traveltranslator.data.remote.dto.UserSettingsPatchDto.serializer(), dto)
        assertTrue("keeps set field as snake_case", encoded.contains("\"default_language\":\"hi\""))
        assertTrue("omits null playback_speed", !encoded.contains("playback_speed"))
        assertNull("null stays null", dto.offlineEnabled)
    }

    @Test
    fun `decodes phrase category and translated phrase payloads`() {
        val categoryPayload =
            """{"slug":"restaurants","name":"Restaurants","icon":"🍴","color":"#F59E0B","sort_order":1}"""
        val category = json.decodeFromString(PhraseCategoryDto.serializer(), categoryPayload).toDomain()
        assertEquals("restaurants", category.slug)
        assertEquals("Restaurants", category.name)
        assertEquals("🍴", category.icon)
        assertEquals(1, category.sortOrder)

        val phrasePayload = """
            {"id":"fbc75eb1-db27-4d96-8465-a5f669508ec1","category_slug":"restaurants",
             "source_lang":"en","source_text":"Table for two, please","target_lang":"hi",
             "translation":"दो लोगों के लिए एक टेबल, कृपया।","pronunciation":"do loɡon","sort_order":1}
        """.trimIndent()
        val phrase = json.decodeFromString(PhraseWithTranslationDto.serializer(), phrasePayload).toDomain()
        assertEquals("restaurants", phrase.categorySlug)
        assertEquals("Table for two, please", phrase.sourceText)
        assertEquals("hi", phrase.targetLang)
        assertEquals("दो लोगों के लिए एक टेबल, कृपया।", phrase.translation)
    }

    @Test
    fun `decodes favorite payload and keeps phrase reference id`() {
        val payload = """
            {"id":"474678b8-0605-40f5-b40a-3bb0a3711ab1","user_id":"919b2a43",
             "kind":"phrase","ref_id":"fbc75eb1-db27-4d96-8465-a5f669508ec1",
             "source_lang":"en","target_lang":"hi","created_at":"2026-11-02T07:52:55Z"}
        """.trimIndent()
        val favorite = json.decodeFromString(FavoriteDto.serializer(), payload).toDomain()
        assertEquals("phrase", favorite.kind)
        assertEquals("fbc75eb1-db27-4d96-8465-a5f669508ec1", favorite.refId)
        assertEquals("en", favorite.sourceLang)
        assertEquals("hi", favorite.targetLang)
        assertTrue(favorite.createdAt.isAfter(Instant.EPOCH))
    }
}
