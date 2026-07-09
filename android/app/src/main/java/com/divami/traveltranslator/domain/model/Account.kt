package com.divami.traveltranslator.domain.model

/** The signed-in account (real Firebase user or an anonymous guest). */
data class Account(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isGuest: Boolean,
)

enum class PlaybackSpeed(val wireValue: String) {
    Normal("normal"),
    Slow("slow"),
    VerySlow("very_slow");

    companion object {
        fun fromWire(value: String?): PlaybackSpeed =
            entries.firstOrNull { it.wireValue == value } ?: Normal
    }
}

data class UserSettings(
    val defaultLanguage: String,
    val playbackSpeed: PlaybackSpeed,
    val offlineEnabled: Boolean,
)
