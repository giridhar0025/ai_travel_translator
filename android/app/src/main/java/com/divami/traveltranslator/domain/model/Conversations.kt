package com.divami.traveltranslator.domain.model

import java.time.Instant

enum class MessageRole(val wireValue: String) {
    User("user"),
    Ai("ai");

    companion object {
        fun fromWire(value: String): MessageRole =
            entries.firstOrNull { it.wireValue == value } ?: User
    }
}

data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val sourceText: String,
    val sourceLang: String,
    val translatedText: String?,
    val targetLang: String?,
    val pronunciation: String?,
    val createdAt: Instant,
)

data class Conversation(
    val id: String,
    val title: String?,
    val lastMessageAt: Instant,
    val messageCount: Int,
    val createdAt: Instant,
)

/** A page of the history list plus the cursor to load older items. */
data class ConversationPage(
    val items: List<Conversation>,
    val nextCursor: String?,
)

data class ConversationDetail(
    val conversation: Conversation,
    val messages: List<Message>,
    val nextBefore: String?,
)

data class Favorite(
    val id: String,
    val kind: String,
    val refId: String,
    val sourceLang: String,
    val targetLang: String,
    val createdAt: Instant,
)

data class Recent(
    val id: String,
    val sourceText: String,
    val sourceLang: String,
    val translatedText: String,
    val targetLang: String,
    val createdAt: Instant,
)
