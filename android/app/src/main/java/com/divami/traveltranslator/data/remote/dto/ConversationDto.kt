package com.divami.traveltranslator.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val id: String,
    val conversationId: String,
    val role: String,
    val sourceText: String,
    val sourceLang: String,
    val translatedText: String? = null,
    val targetLang: String? = null,
    val pronunciation: String? = null,
    val createdAt: String,
)

@Serializable
data class ConversationDto(
    val id: String,
    val userId: String,
    val title: String? = null,
    val lastMessageAt: String,
    val messageCount: Int,
    val createdAt: String,
)

@Serializable
data class GroupInfoDto(
    val label: String,
    val count: Int,
)

@Serializable
data class ConversationsListResponseDto(
    val items: List<ConversationDto>,
    val nextCursor: String? = null,
    val groups: List<GroupInfoDto>,
)

@Serializable
data class CreateConversationRequestDto(
    val title: String? = null,
)

@Serializable
data class AppendMessageRequestDto(
    val role: String,
    val sourceText: String,
    val sourceLang: String,
    val translatedText: String? = null,
    val targetLang: String? = null,
    val pronunciation: String? = null,
)

@Serializable
data class ConversationDetailResponseDto(
    val conversation: ConversationDto,
    val messages: List<MessageDto>,
    val nextBefore: String? = null,
)
