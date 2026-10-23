package com.divami.traveltranslator.di

import com.divami.traveltranslator.data.auth.FirebaseAuthGateway
import com.divami.traveltranslator.data.auth.FirebaseAuthGatewayImpl
import com.divami.traveltranslator.data.local.MlKitOfflineTranslator
import com.divami.traveltranslator.data.local.OfflineTranslator
import com.divami.traveltranslator.data.repository.AuthRepository
import com.divami.traveltranslator.data.repository.AuthRepositoryImpl
import com.divami.traveltranslator.data.repository.ConversationRepository
import com.divami.traveltranslator.data.repository.ConversationRepositoryImpl
import com.divami.traveltranslator.data.repository.FavoriteRepository
import com.divami.traveltranslator.data.repository.FavoriteRepositoryImpl
import com.divami.traveltranslator.data.repository.LanguageRepository
import com.divami.traveltranslator.data.repository.LanguageRepositoryImpl
import com.divami.traveltranslator.data.repository.PhraseRepository
import com.divami.traveltranslator.data.repository.PhraseRepositoryImpl
import com.divami.traveltranslator.data.repository.SettingsRepository
import com.divami.traveltranslator.data.repository.SettingsRepositoryImpl
import com.divami.traveltranslator.data.repository.SpeechRepository
import com.divami.traveltranslator.data.repository.SpeechRepositoryImpl
import com.divami.traveltranslator.data.repository.TranslationRepository
import com.divami.traveltranslator.data.repository.TranslationRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds each repository interface to its implementation. @Binds keeps the graph
 * declarative — ViewModels depend only on the interfaces (Dependency Inversion),
 * so implementations can be swapped (e.g. for fakes in tests) without touching
 * callers.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFirebaseAuthGateway(impl: FirebaseAuthGatewayImpl): FirebaseAuthGateway

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTranslationRepository(impl: TranslationRepositoryImpl): TranslationRepository

    /** On-device fallback used by [TranslationRepositoryImpl] when the backend fails. */
    @Binds
    @Singleton
    abstract fun bindOfflineTranslator(impl: MlKitOfflineTranslator): OfflineTranslator

    @Binds
    @Singleton
    abstract fun bindSpeechRepository(impl: SpeechRepositoryImpl): SpeechRepository

    @Binds
    @Singleton
    abstract fun bindLanguageRepository(impl: LanguageRepositoryImpl): LanguageRepository

    @Binds
    @Singleton
    abstract fun bindPhraseRepository(impl: PhraseRepositoryImpl): PhraseRepository

    @Binds
    @Singleton
    abstract fun bindConversationRepository(impl: ConversationRepositoryImpl): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
