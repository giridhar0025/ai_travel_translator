package com.divami.traveltranslator.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LanguageDto(
    val code: String,
    val name: String,
    val flagEmoji: String,
    val sttSupported: Boolean,
    val ttsSupported: Boolean,
    val translateSupported: Boolean,
)

@Serializable
data class PhraseCategoryDto(
    val slug: String,
    val name: String,
    val icon: String,
    val color: String,
    val sortOrder: Int,
)

@Serializable
data class PhraseWithTranslationDto(
    val id: String,
    val categorySlug: String,
    val sourceLang: String,
    val sourceText: String,
    val targetLang: String,
    val translation: String,
    val pronunciation: String? = null,
    val sortOrder: Int,
)

@Serializable
data class FavoriteDto(
    val id: String,
    val userId: String,
    val kind: String,
    val refId: String,
    val sourceLang: String,
    val targetLang: String,
    val createdAt: String,
)

@Serializable
data class FavoriteCreateDto(
    val kind: String,
    val refId: String,
    val sourceLang: String,
    val targetLang: String,
)

@Serializable
data class RecentDto(
    val id: String,
    val sourceText: String,
    val sourceLang: String,
    val translatedText: String,
    val targetLang: String,
    val createdAt: String,
)
