package com.divami.traveltranslator.data.repository

import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.core.network.map
import com.divami.traveltranslator.core.network.safeApiCall
import com.divami.traveltranslator.data.remote.TravelTranslatorApi
import com.divami.traveltranslator.data.remote.dto.UserSettingsPatchDto
import com.divami.traveltranslator.data.remote.dto.toDomain
import com.divami.traveltranslator.domain.model.PlaybackSpeed
import com.divami.traveltranslator.domain.model.UserSettings
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface SettingsRepository {
    suspend fun get(): ApiResult<UserSettings>
    suspend fun update(
        defaultLanguage: String? = null,
        playbackSpeed: PlaybackSpeed? = null,
        offlineEnabled: Boolean? = null,
    ): ApiResult<UserSettings>
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val api: TravelTranslatorApi,
    private val json: Json,
) : SettingsRepository {

    override suspend fun get(): ApiResult<UserSettings> =
        safeApiCall(json) { api.settings() }.map { it.toDomain() }

    override suspend fun update(
        defaultLanguage: String?,
        playbackSpeed: PlaybackSpeed?,
        offlineEnabled: Boolean?,
    ): ApiResult<UserSettings> = safeApiCall(json) {
        api.patchSettings(
            UserSettingsPatchDto(
                defaultLanguage = defaultLanguage,
                playbackSpeed = playbackSpeed?.wireValue,
                offlineEnabled = offlineEnabled,
            ),
        )
    }.map { it.toDomain() }
}
