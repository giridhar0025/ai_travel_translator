package com.divami.traveltranslator.domain.model

/** A language supported by the backend, with its capability flags. */
data class Language(
    val code: String,
    val name: String,
    val flagEmoji: String,
    val sttSupported: Boolean,
    val ttsSupported: Boolean,
    val translateSupported: Boolean,
)

/** Result of a single translation call. */
data class Translation(
    val translation: String,
    val pronunciation: String?,
    val latencyMs: Int,
    val model: String,
)

/** Result of a speech-to-text transcription. */
data class Transcription(
    val transcript: String,
    val language: String?,
    val confidence: Double?,
)

data class PhraseCategory(
    val slug: String,
    val name: String,
    val icon: String,
    val color: String,
    val sortOrder: Int,
)

data class Phrase(
    val id: String,
    val categorySlug: String,
    val sourceLang: String,
    val sourceText: String,
    val targetLang: String,
    val translation: String,
    val pronunciation: String?,
    val sortOrder: Int,
)
