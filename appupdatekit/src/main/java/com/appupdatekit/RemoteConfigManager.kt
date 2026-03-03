/**
 * AppUpdateKit
 * RemoteConfigManager.kt
 * Purpose: Wraps Firebase Remote Config to fetch and expose AppUpdateKit policy values.
 */
package com.appupdatekit

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

/**
 * Wraps [FirebaseRemoteConfig] to fetch the `auk_*` policy keys and map them
 * into an [UpdateConfig] instance.
 *
 * - Cache expiry is **0 seconds** in debug builds (always fresh) and
 *   **3600 seconds (1 hour)** in release builds.
 * - On any fetch failure the defaults defined in [UpdateConfig.Defaults] are
 *   returned so the library degrades gracefully without crashing.
 *
 * @param isDebug      Whether the host app is running a debug build.
 * @param loggingEnabled Whether to emit log messages for diagnostics.
 */
internal class RemoteConfigManager(
    private val isDebug: Boolean,
    private val loggingEnabled: Boolean
) {

    // ─── Constants ────────────────────────────────────────────────────────────

    private companion object {
        const val TAG = "AppUpdateKit"
        const val CACHE_EXPIRY_RELEASE_SECONDS = 3600L
        const val CACHE_EXPIRY_DEBUG_SECONDS = 0L
    }

    // ─── Initialization ───────────────────────────────────────────────────────

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().also { rc ->
            val cacheExpiry = if (isDebug) CACHE_EXPIRY_DEBUG_SECONDS else CACHE_EXPIRY_RELEASE_SECONDS
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(cacheExpiry)
                .build()
            rc.setConfigSettingsAsync(settings)
            rc.setDefaultsAsync(buildDefaults())
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Fetches and activates Remote Config values, then maps them to [UpdateConfig].
     *
     * This is a **suspending** function and must be called from a coroutine or
     * another suspend function.
     *
     * On any network or Firebase error the method logs a warning and returns the
     * default [UpdateConfig] so the app continues to work offline.
     *
     * @return The resolved [UpdateConfig] populated from Remote Config (or defaults).
     */
    suspend fun fetchUpdateConfig(): UpdateConfig {
        return try {
            remoteConfig.fetchAndActivate().await()
            log("Remote Config fetched and activated successfully.")
            mapToUpdateConfig()
        } catch (e: Exception) {
            Log.w(TAG, "AppUpdateKit: Remote Config fetch failed — using defaults. Reason: ${e.message}")
            UpdateConfig()
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /** Builds the in-app default map that mirrors [UpdateConfig.Defaults]. */
    private fun buildDefaults(): Map<String, Any> = mapOf(
        UpdateConfig.Keys.UPDATE_ENABLED to UpdateConfig.Defaults.UPDATE_ENABLED,
        UpdateConfig.Keys.FORCE_UPDATE to UpdateConfig.Defaults.FORCE_UPDATE,
        UpdateConfig.Keys.MINIMUM_VERSION_CODE to UpdateConfig.Defaults.MINIMUM_VERSION_CODE,
        UpdateConfig.Keys.SNOOZE_DURATION_HOURS to UpdateConfig.Defaults.SNOOZE_DURATION_HOURS,
        UpdateConfig.Keys.SNOOZE_MAX_COUNT to UpdateConfig.Defaults.SNOOZE_MAX_COUNT
    )

    /** Maps raw Remote Config values to a typed [UpdateConfig]. */
    private fun mapToUpdateConfig(): UpdateConfig {
        return UpdateConfig(
            isUpdateEnabled = remoteConfig.getBoolean(UpdateConfig.Keys.UPDATE_ENABLED),
            forceUpdate = remoteConfig.getBoolean(UpdateConfig.Keys.FORCE_UPDATE),
            minimumVersionCode = remoteConfig.getLong(UpdateConfig.Keys.MINIMUM_VERSION_CODE),
            snoozeDurationHours = remoteConfig.getLong(UpdateConfig.Keys.SNOOZE_DURATION_HOURS),
            snoozeMaxCount = remoteConfig.getLong(UpdateConfig.Keys.SNOOZE_MAX_COUNT)
        )
    }

    private fun log(message: String) {
        if (loggingEnabled) Log.d(TAG, message)
    }
}

