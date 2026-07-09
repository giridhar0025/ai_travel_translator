package com.divami.traveltranslator.data.repository

import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.core.network.map
import com.divami.traveltranslator.core.network.safeApiCall
import com.divami.traveltranslator.data.remote.TravelTranslatorApi
import com.divami.traveltranslator.data.remote.dto.TranslateRequestDto
import com.divami.traveltranslator.data.remote.dto.toDomain
import com.divami.traveltranslator.domain.model.Translation
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface TranslationRepository {
    suspend fun translate(
        source: String,
        target: String,
        text: String,
        context: String? = null,
    ): ApiResult<Translation>
}

@Singleton
class TranslationRepositoryImpl @Inject constructor(
    private val api: TravelTranslatorApi,
    private val json: Json,
) : TranslationRepository {
    override suspend fun translate(
        source: String,
        target: String,
        text: String,
        context: String?,
    ): ApiResult<Translation> =
        safeApiCall(json) {
            api.translate(TranslateRequestDto(source = source, target = target, text = text, context = context))
        }.map { it.toDomain() }
}
