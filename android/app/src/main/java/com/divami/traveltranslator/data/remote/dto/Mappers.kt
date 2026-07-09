package com.divami.traveltranslator.data.remote.dto

import com.divami.traveltranslator.domain.model.Account
import com.divami.traveltranslator.domain.model.Conversation
import com.divami.traveltranslator.domain.model.ConversationDetail
import com.divami.traveltranslator.domain.model.ConversationPage
import com.divami.traveltranslator.domain.model.Favorite
import com.divami.traveltranslator.domain.model.Language
import com.divami.traveltranslator.domain.model.Message
import com.divami.traveltranslator.domain.model.MessageRole
import com.divami.traveltranslator.domain.model.Phrase
import com.divami.traveltranslator.domain.model.PhraseCategory
import com.divami.traveltranslator.domain.model.PlaybackSpeed
import com.divami.traveltranslator.domain.model.Recent
import com.divami.traveltranslator.domain.model.Transcription
import com.divami.traveltranslator.domain.model.Translation
import com.divami.traveltranslator.domain.model.UserSettings
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * DTO → domain conversions, in one place so the wire format never leaks past
 * the data layer. Kept as pure extension functions (no state, easy to test).
 */

fun UserDto.toDomain(): Account = Account(
    id = id,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    isGuest = isGuest,
)

fun UserSettingsDto.toDomain(): UserSettings = UserSettings(
    defaultLanguage = defaultLanguage,
    playbackSpeed = PlaybackSpeed.fromWire(playbackSpeed),
    offlineEnabled = offlineEnabled,
)

fun TranslateResultDto.toDomain(): Translation = Translation(
    translation = translation,
    pronunciation = pronunciation,
    latencyMs = latencyMs,
    model = model,
)

fun TranscriptionResponseDto.toDomain(): Transcription = Transcription(
    transcript = transcript,
    language = language,
    confidence = confidence,
)

fun LanguageDto.toDomain(): Language = Language(
    code = code,
    name = name,
    flagEmoji = flagEmoji,
    sttSupported = sttSupported,
    ttsSupported = ttsSupported,
    translateSupported = translateSupported,
)

fun PhraseCategoryDto.toDomain(): PhraseCategory = PhraseCategory(
    slug = slug,
    name = name,
    icon = icon,
    color = color,
    sortOrder = sortOrder,
)

fun PhraseWithTranslationDto.toDomain(): Phrase = Phrase(
    id = id,
    categorySlug = categorySlug,
    sourceLang = sourceLang,
    sourceText = sourceText,
    targetLang = targetLang,
    translation = translation,
    pronunciation = pronunciation,
    sortOrder = sortOrder,
)

fun ConversationDto.toDomain(): Conversation = Conversation(
    id = id,
    title = title,
    lastMessageAt = lastMessageAt.toInstant(),
    messageCount = messageCount,
    createdAt = createdAt.toInstant(),
)

fun MessageDto.toDomain(): Message = Message(
    id = id,
    conversationId = conversationId,
    role = MessageRole.fromWire(role),
    sourceText = sourceText,
    sourceLang = sourceLang,
    translatedText = translatedText,
    targetLang = targetLang,
    pronunciation = pronunciation,
    createdAt = createdAt.toInstant(),
)

fun ConversationsListResponseDto.toDomain(): ConversationPage = ConversationPage(
    items = items.map { it.toDomain() },
    nextCursor = nextCursor,
)

fun ConversationDetailResponseDto.toDomain(): ConversationDetail = ConversationDetail(
    conversation = conversation.toDomain(),
    messages = messages.map { it.toDomain() },
    nextBefore = nextBefore,
)

fun FavoriteDto.toDomain(): Favorite = Favorite(
    id = id,
    kind = kind,
    refId = refId,
    sourceLang = sourceLang,
    targetLang = targetLang,
    createdAt = createdAt.toInstant(),
)

fun RecentDto.toDomain(): Recent = Recent(
    id = id,
    sourceText = sourceText,
    sourceLang = sourceLang,
    translatedText = translatedText,
    targetLang = targetLang,
    createdAt = createdAt.toInstant(),
)

/**
 * Parses the backend's ISO-8601 timestamps. FastAPI/Pydantic may emit them with
 * an explicit offset (`+00:00`), a `Z`, or naive (no zone) — handle all three,
 * treating a naive value as UTC. Falls back to [Instant.EPOCH] rather than
 * throwing, so a single malformed row can't crash a whole list.
 */
internal fun String.toInstant(): Instant =
    runCatching { OffsetDateTime.parse(this).toInstant() }
        .recoverCatching { Instant.parse(this) }
        .recoverCatching { LocalDateTime.parse(this).toInstant(ZoneOffset.UTC) }
        .getOrDefault(Instant.EPOCH)
