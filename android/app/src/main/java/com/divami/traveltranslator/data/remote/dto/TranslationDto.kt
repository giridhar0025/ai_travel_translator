package com.divami.traveltranslator.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TranslateRequestDto(
    val source: String,
    val target: String,
    val text: String,
    val context: String? = null,
)

@Serializable
data class TranslateBatchRequestDto(
    val source: String,
    val target: String,
    val items: List<String>,
)

@Serializable
data class TranslateResultDto(
    val translation: String,
    val pronunciation: String? = null,
    // Diagnostics only (unused by the domain model) — defaulted so their absence
    // never fails deserialization of an otherwise-valid translation.
    val latencyMs: Int = 0,
    val model: String = "",
)

@Serializable
data class TranslateBatchResponseDto(
    val results: List<TranslateResultDto>,
)

@Serializable
data class TranscriptionResponseDto(
    val transcript: String,
    val language: String? = null,
    val confidence: Double? = null,
    // Diagnostics only — defaulted so their absence never fails a valid transcript.
    val latencyMs: Int = 0,
    val model: String = "",
)

@Serializable
data class SynthesizeRequestDto(
    val text: String,
    val language: String,
    val speed: String = "normal",
)
