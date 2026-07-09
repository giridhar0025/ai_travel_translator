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
    val latencyMs: Int,
    val model: String,
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
    val latencyMs: Int,
    val model: String,
)

@Serializable
data class SynthesizeRequestDto(
    val text: String,
    val language: String,
    val speed: String = "normal",
)
