package com.divami.traveltranslator.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
private val EmptyPackState: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

interface OfflineTranslator {
    /** App language codes whose on-device pack is downloaded. */
    val downloaded: StateFlow<Set<String>> get() = EmptyPackState

    /** App language codes currently being downloaded. */
    val downloading: StateFlow<Set<String>> get() = EmptyPackState

    /** Translate on-device, or `null` when the language pair is unsupported/not downloaded. */
    suspend fun translate(source: String, target: String, text: String): String?

    /**
     * Download the ML Kit model for this pair in the background so [translate]
     * can work later with no signal. Best effort, no-op when offline or metered.
     */
    fun warmUp(source: String, target: String)

    /** Explicit user-requested pack download. Returns true when ready offline. */
    suspend fun download(languageCode: String): Boolean = false

    /** Remove a downloaded pack and release any live translators that use it. */
    suspend fun delete(languageCode: String) = Unit

    /** Fast check used by the UI before claiming a pair is ready offline. */
    fun isReady(source: String, target: String): Boolean = false
}

/** Used by unit tests and anywhere offline support should be disabled. */
object NoopOfflineTranslator : OfflineTranslator {
    private val empty = MutableStateFlow<Set<String>>(emptySet())
    override val downloaded: StateFlow<Set<String>> = empty.asStateFlow()
    override val downloading: StateFlow<Set<String>> = empty.asStateFlow()

    override suspend fun translate(source: String, target: String, text: String): String? = null
    override fun warmUp(source: String, target: String) = Unit
    override suspend fun download(languageCode: String): Boolean = false
    override suspend fun delete(languageCode: String) = Unit
    override fun isReady(source: String, target: String): Boolean = false
}

@Singleton
class MlKitOfflineTranslator @Inject constructor(
    @ApplicationContext private val context: Context,
) : OfflineTranslator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val clients = mutableMapOf<Pair<String, String>, Translator>()

    /** Pairs whose model has been downloaded and whose translator is live. */
    private val ready = mutableSetOf<Pair<String, String>>()

    private val _downloaded = MutableStateFlow<Set<String>>(emptySet())
    override val downloaded: StateFlow<Set<String>> = _downloaded.asStateFlow()

    private val _downloading = MutableStateFlow<Set<String>>(emptySet())
    override val downloading: StateFlow<Set<String>> = _downloading.asStateFlow()

    init {
        scope.launch { loadDownloadedModels() }
    }

    override suspend fun translate(source: String, target: String, text: String): String? {
        val client = ensureReady(source to target) ?: return null
        return runCatching { client.translate(text).await() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    override fun warmUp(source: String, target: String) {
        // Models are ~30 MB per language; only fetch them automatically on
        // Wi-Fi/Ethernet so warming up never spends the user's mobile data.
        if (!isUnmetered()) return
        if (target in downloaded.value || target in downloading.value) return
        scope.launch { runCatching { download(target) } }
    }

    override suspend fun download(languageCode: String): Boolean {
        val mlKitCode = TranslateLanguage.fromLanguageTag(languageCode) ?: return false
        if (languageCode in downloaded.value) return true
        _downloading.update { it + languageCode }
        val success = runCatching {
            val model = TranslateRemoteModel.Builder(mlKitCode).build()
            RemoteModelManager.getInstance()
                .download(model, DownloadConditions.Builder().build())
                .await()
        }.isSuccess
        _downloading.update { it - languageCode }
        if (success) {
            _downloaded.update { it + languageCode }
            // Warm a default English pair so the pack is immediately useful.
            if (languageCode != "en") {
                runCatching { ensureReady("en" to languageCode) }
            }
        }
        return success
    }

    override suspend fun delete(languageCode: String) {
        val mlKitCode = TranslateLanguage.fromLanguageTag(languageCode) ?: return
        runCatching {
            val model = TranslateRemoteModel.Builder(mlKitCode).build()
            RemoteModelManager.getInstance().deleteDownloadedModel(model).await()
        }
        clients.filterKeys { it.second == languageCode || it.first == languageCode }.forEach { (key, client) ->
            runCatching { client.close() }
            clients.remove(key)
            ready.remove(key)
        }
        _downloaded.update { it - languageCode }
    }

    override fun isReady(source: String, target: String): Boolean =
        (source to target) in ready || target in downloaded.value

    private suspend fun loadDownloadedModels() {
        runCatching {
            RemoteModelManager.getInstance()
                .getDownloadedModels(TranslateRemoteModel::class.java)
                .await()
        }.getOrNull()?.forEach { model ->
            TranslateLanguage.fromLanguageTag(model.language)?.let { code ->
                _downloaded.update { it + code }
            }
        }
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
        _downloaded.update { it + key.second }
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
