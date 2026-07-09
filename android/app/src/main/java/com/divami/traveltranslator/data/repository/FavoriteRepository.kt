package com.divami.traveltranslator.data.repository

import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.core.network.map
import com.divami.traveltranslator.core.network.safeApiCall
import com.divami.traveltranslator.data.remote.TravelTranslatorApi
import com.divami.traveltranslator.data.remote.dto.FavoriteCreateDto
import com.divami.traveltranslator.data.remote.dto.toDomain
import com.divami.traveltranslator.domain.model.Favorite
import com.divami.traveltranslator.domain.model.Recent
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface FavoriteRepository {
    suspend fun favorites(kind: String? = null): ApiResult<List<Favorite>>
    suspend fun addFavorite(
        kind: String,
        refId: String,
        sourceLang: String,
        targetLang: String,
    ): ApiResult<Favorite>
    suspend fun removeFavorite(id: String): ApiResult<Unit>
    suspend fun recents(limit: Int = 20): ApiResult<List<Recent>>
}

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val api: TravelTranslatorApi,
    private val json: Json,
) : FavoriteRepository {

    override suspend fun favorites(kind: String?): ApiResult<List<Favorite>> =
        safeApiCall(json) { api.favorites(kind) }.map { list -> list.map { it.toDomain() } }

    override suspend fun addFavorite(
        kind: String,
        refId: String,
        sourceLang: String,
        targetLang: String,
    ): ApiResult<Favorite> = safeApiCall(json) {
        api.createFavorite(FavoriteCreateDto(kind = kind, refId = refId, sourceLang = sourceLang, targetLang = targetLang))
    }.map { it.toDomain() }

    override suspend fun removeFavorite(id: String): ApiResult<Unit> =
        safeApiCall(json) { api.deleteFavorite(id) }

    override suspend fun recents(limit: Int): ApiResult<List<Recent>> =
        safeApiCall(json) { api.recents(limit) }.map { list -> list.map { it.toDomain() } }
}
