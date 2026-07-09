package com.divami.traveltranslator.data.repository

import com.divami.traveltranslator.core.network.ApiResult
import com.divami.traveltranslator.core.network.map
import com.divami.traveltranslator.core.network.safeApiCall
import com.divami.traveltranslator.data.remote.TravelTranslatorApi
import com.divami.traveltranslator.data.remote.dto.AppendMessageRequestDto
import com.divami.traveltranslator.data.remote.dto.CreateConversationRequestDto
import com.divami.traveltranslator.data.remote.dto.toDomain
import com.divami.traveltranslator.domain.model.Conversation
import com.divami.traveltranslator.domain.model.ConversationDetail
import com.divami.traveltranslator.domain.model.ConversationPage
import com.divami.traveltranslator.domain.model.Message
import com.divami.traveltranslator.domain.model.MessageRole
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface ConversationRepository {
    suspend fun list(query: String? = null, limit: Int = 20, cursor: String? = null): ApiResult<ConversationPage>
    suspend fun detail(id: String, limit: Int = 50, beforeId: String? = null): ApiResult<ConversationDetail>
    suspend fun create(title: String? = null): ApiResult<Conversation>
    suspend fun delete(id: String): ApiResult<Unit>
    suspend fun appendMessage(
        conversationId: String,
        role: MessageRole,
        sourceText: String,
        sourceLang: String,
        translatedText: String? = null,
        targetLang: String? = null,
        pronunciation: String? = null,
    ): ApiResult<Message>
}

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val api: TravelTranslatorApi,
    private val json: Json,
) : ConversationRepository {

    override suspend fun list(query: String?, limit: Int, cursor: String?): ApiResult<ConversationPage> =
        safeApiCall(json) { api.conversations(query = query, limit = limit, cursor = cursor) }
            .map { it.toDomain() }

    override suspend fun detail(id: String, limit: Int, beforeId: String?): ApiResult<ConversationDetail> =
        safeApiCall(json) { api.conversationDetail(id = id, limit = limit, beforeId = beforeId) }
            .map { it.toDomain() }

    override suspend fun create(title: String?): ApiResult<Conversation> =
        safeApiCall(json) { api.createConversation(CreateConversationRequestDto(title)) }
            .map { it.toDomain() }

    override suspend fun delete(id: String): ApiResult<Unit> =
        safeApiCall(json) { api.deleteConversation(id) }

    override suspend fun appendMessage(
        conversationId: String,
        role: MessageRole,
        sourceText: String,
        sourceLang: String,
        translatedText: String?,
        targetLang: String?,
        pronunciation: String?,
    ): ApiResult<Message> = safeApiCall(json) {
        api.appendMessage(
            id = conversationId,
            body = AppendMessageRequestDto(
                role = role.wireValue,
                sourceText = sourceText,
                sourceLang = sourceLang,
                translatedText = translatedText,
                targetLang = targetLang,
                pronunciation = pronunciation,
            ),
        )
    }.map { it.toDomain() }
}
