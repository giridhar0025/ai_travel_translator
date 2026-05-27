package com.divami.traveltranslator.feature.chat

import com.divami.traveltranslator.designsystem.components.AppLanguage

enum class Sender { You, OtherPerson }

data class ChatMessage(
    val id: String,
    val sender: Sender,
    val originalText: String,
    val originalLanguage: AppLanguage,
    val translatedText: String,
    val translatedLanguage: AppLanguage,
    val pronunciation: String? = null,
)

enum class ContextMode(val label: String) {
    Formal("Formal"),
    Casual("Casual"),
    Tourist("Tourist"),
}
