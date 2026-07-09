package com.divami.traveltranslator.data.repository

import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.core.network.map
import com.divami.traveltranslator.core.network.safeApiCall
import com.divami.traveltranslator.data.remote.TravelTranslatorApi
import com.divami.traveltranslator.data.remote.dto.toDomain
import com.divami.traveltranslator.domain.model.Language
import com.divami.traveltranslator.domain.model.Phrase
import com.divami.traveltranslator.domain.model.PhraseCategory
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface LanguageRepository {
    suspend fun languages(): ApiResult<List<Language>>
}

@Singleton
class LanguageRepositoryImpl @Inject constructor(
    private val api: TravelTranslatorApi,
    private val json: Json,
) : LanguageRepository {
    override suspend fun languages(): ApiResult<List<Language>> =
        safeApiCall(json) { api.languages() }.map { list -> list.map { it.toDomain() } }
}

interface PhraseRepository {
    suspend fun categories(): ApiResult<List<PhraseCategory>>
    suspend fun phrases(category: String, target: String): ApiResult<List<Phrase>>
}

@Singleton
class PhraseRepositoryImpl @Inject constructor(
    private val api: TravelTranslatorApi,
    private val json: Json,
) : PhraseRepository {
    override suspend fun categories(): ApiResult<List<PhraseCategory>> =
        safeApiCall(json) { api.phraseCategories() }.map { list -> list.map { it.toDomain() } }

    override suspend fun phrases(category: String, target: String): ApiResult<List<Phrase>> =
        safeApiCall(json) { api.phrases(category, target) }.map { list -> list.map { it.toDomain() } }
}
