package com.divami.traveltranslator.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * DTOs mirror the FastAPI schemas 1:1. Field names are camelCase here and map
 * to the backend's snake_case via the global [JsonNamingStrategy.SnakeCase]
 * configured in NetworkModule — so no per-field @SerialName is needed.
 */
@Serializable
data class UserDto(
    val id: String,
    val firebaseUid: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isGuest: Boolean,
    val timezone: String,
    val createdAt: String,
)

@Serializable
data class UserSettingsDto(
    val defaultLanguage: String,
    val playbackSpeed: String,
    val offlineEnabled: Boolean,
)

@Serializable
data class UserSettingsPatchDto(
    val defaultLanguage: String? = null,
    val playbackSpeed: String? = null,
    val offlineEnabled: Boolean? = null,
)

@Serializable
data class MeResponseDto(
    val user: UserDto,
    val settings: UserSettingsDto? = null,
)

@Serializable
data class VerifyTokenRequestDto(
    val idToken: String,
)

@Serializable
data class AuthResponseDto(
    val user: UserDto,
    val settings: UserSettingsDto,
    val token: String,
)
