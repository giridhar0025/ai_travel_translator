package com.divami.traveltranslator.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device translation used when the backend cannot be reached.
 *
 * The backend is the source of truth (better quality, context-aware, streams),
 * so this is strictly a fallback: it only runs when a network call fails with
 * connectivity/timeout/provider errors. It also keeps the app useful on a plane
 * or with a dead signal, which is exactly when a travel translator matters most.
 */
interface OfflineTranslator {
    /** Translate on-device, or `null` when the language pair is unsupported/not downloaded. */
    suspend fun translate(source: String, target: String, text: String): String?

    /**
     * Download the ML Kit model for this pair in the background so [translate]
     * can work later with no signal. Best effort, no-op when offline or metered.
     */
    fun warmUp(source: String, target: String)
}

/** Used by unit tests and anywhere offline support should be disabled. */
object NoopOfflineTranslator : OfflineTranslator {
    override suspend fun translate(source: String, target: String, text: String): String? = null

    override fun warmUp(source: String, target: String) = Unit
}

@Singleton
class MlKitOfflineTranslator @Inject constructor(
    @ApplicationContext private val context: Context,
) : OfflineTranslator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val clients = mutableMapOf<Pair<String, String>, Translator>()

    /** Pairs whose model has been downloaded — only these can translate offline. */
    private val ready = mutableSetOf<Pair<String, String>>()

    override suspend fun translate(source: String, target: String, text: String): String? {
        val client = ensureReady(source to target) ?: return null
        return runCatching { client.translate(text).await() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    override fun warmUp(source: String, target: String) {
        // Models are ~30 MB per direction; only fetch them on Wi-Fi/Ethernet so
        // warming up never spends the user's mobile data.
        if (!isUnmetered()) return
        scope.launch { runCatching { ensureReady(source to target) } }
    }

    private suspend fun ensureReady(key: Pair<String, String>): Translator? = mutex.withLock {
        if (key in ready) return@withLock clients[key]

        val sourceLang = TranslateLanguage.fromLanguageTag(key.first) ?: return@withLock null
        val targetLang = TranslateLanguage.fromLanguageTag(key.second) ?: return@withLock null

        val client = clients.getOrPut(key) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLang)
                    .setTargetLanguage(targetLang)
                    .build(),
            )
        }

        // Throws when there is no connectivity and the model is not on device,
        // which is the correct outcome: nothing to fall back to.
        val downloaded = runCatching {
            client.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        }.isSuccess

        if (!downloaded) return@withLock null
        ready += key
        client
    }

    private fun isUnmetered(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
