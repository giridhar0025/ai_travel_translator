package com.divami.traveltranslator.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cheap "do we have a network?" check, used to fail fast when the device is
 * offline.
 *
 * Without this, a lost signal made the SSE translation call hang until OkHttp's
 * socket timeout — the user stared at typing dots for ~45 s instead of getting
 * the on-device translation. Checking first turns that into an immediate
 * fallback.
 */
interface NetworkStatus {
    fun isOnline(): Boolean
}

/** Default for unit tests: never short-circuit. */
object AlwaysOnlineNetworkStatus : NetworkStatus {
    override fun isOnline(): Boolean = true
}

@Singleton
class AndroidNetworkStatus @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkStatus {

    override fun isOnline(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
