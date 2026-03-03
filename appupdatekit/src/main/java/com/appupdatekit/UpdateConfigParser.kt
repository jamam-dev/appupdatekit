/**
 * AppUpdateKit
 * UpdateConfigParser.kt
 * Purpose: Reads the update config JSON from the already-active FirebaseRemoteConfig instance
 *          and parses it into an UpdateConfig. No fetch is performed here.
 */
package com.appupdatekit

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.json.JSONObject

/**
 * Reads a single Remote Config key whose value is a JSON string, then maps
 * it to an [UpdateConfig].
 *
 * The host application is responsible for calling `fetchAndActivate()` **before**
 * passing control to AppUpdateKit. This class only reads the already-activated value.
 *
 * On any parse error the defaults defined in [UpdateConfig.Defaults] are returned
 * so the library degrades gracefully.
 *
 * @param remoteConfigKey  The Remote Config key to read (default: [UpdateConfig.DEFAULT_REMOTE_CONFIG_KEY]).
 * @param loggingEnabled   Whether to emit diagnostic log messages.
 */
internal class UpdateConfigParser(
    private val remoteConfigKey: String = UpdateConfig.DEFAULT_REMOTE_CONFIG_KEY,
    private val loggingEnabled: Boolean = false
) {

    // ─── Constants ────────────────────────────────────────────────────────────

    private companion object {
        const val TAG = "AppUpdateKit"

        // JSON field names
        const val KEY_UPDATE_ENABLED          = "update_enabled"
        const val KEY_FORCE_UPDATE            = "force_update"
        const val KEY_MINIMUM_VERSION_CODE    = "minimum_version_code"
        const val KEY_SNOOZE_DURATION_HOURS   = "snooze_duration_hours"
        const val KEY_SNOOZE_MAX_COUNT        = "snooze_max_count"
        const val KEY_MAINTENANCE_ENABLED     = "maintenance_enabled"
        const val KEY_MAINTENANCE_MESSAGE     = "maintenance_message"
        const val KEY_FORCE_UPDATE_MESSAGE    = "force_update_message"
        const val KEY_FORCE_UPDATE_BTN        = "force_update_button_text"
        const val KEY_FLEXIBLE_MESSAGE        = "flexible_update_message"
        const val KEY_FLEXIBLE_BTN            = "flexible_update_button_text"
        const val KEY_FLEXIBLE_DISMISS_BTN    = "flexible_dismiss_button_text"
        const val KEY_STORE_URL               = "store_url"
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Reads the JSON string from the already-activated [FirebaseRemoteConfig] instance
     * and returns a parsed [UpdateConfig].
     *
     * If the key is missing, the JSON is empty, or parsing fails, a default
     * [UpdateConfig] is returned and a warning is logged.
     */
    fun parse(): UpdateConfig {
        return try {
            val json = FirebaseRemoteConfig.getInstance().getString(remoteConfigKey)
            if (json.isBlank()) {
                log("Remote Config key '$remoteConfigKey' is empty — using defaults.")
                return UpdateConfig()
            }
            log("Parsing config JSON from key '$remoteConfigKey': $json")
            parseJson(JSONObject(json))
        } catch (e: Exception) {
            Log.w(TAG, "AppUpdateKit: Failed to parse update config JSON — using defaults. Reason: ${e.message}")
            UpdateConfig()
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun parseJson(json: JSONObject): UpdateConfig {
        val d = UpdateConfig.Defaults
        return UpdateConfig(
            isUpdateEnabled       = json.optBoolean(KEY_UPDATE_ENABLED,       d.UPDATE_ENABLED),
            forceUpdate           = json.optBoolean(KEY_FORCE_UPDATE,          d.FORCE_UPDATE),
            minimumVersionCode    = json.optLong(KEY_MINIMUM_VERSION_CODE,     d.MINIMUM_VERSION_CODE),
            snoozeDurationHours   = json.optLong(KEY_SNOOZE_DURATION_HOURS,    d.SNOOZE_DURATION_HOURS),
            snoozeMaxCount        = json.optLong(KEY_SNOOZE_MAX_COUNT,         d.SNOOZE_MAX_COUNT),
            maintenanceEnabled    = json.optBoolean(KEY_MAINTENANCE_ENABLED,   d.MAINTENANCE_ENABLED),
            maintenanceMessage    = json.optString(KEY_MAINTENANCE_MESSAGE,    d.MAINTENANCE_MESSAGE),
            forceUpdateMessage    = json.optString(KEY_FORCE_UPDATE_MESSAGE,   d.FORCE_UPDATE_MESSAGE),
            forceUpdateButtonText = json.optString(KEY_FORCE_UPDATE_BTN,       d.FORCE_UPDATE_BUTTON_TEXT),
            flexibleUpdateMessage = json.optString(KEY_FLEXIBLE_MESSAGE,       d.FLEXIBLE_UPDATE_MESSAGE),
            flexibleUpdateButtonText   = json.optString(KEY_FLEXIBLE_BTN,      d.FLEXIBLE_UPDATE_BUTTON_TEXT),
            flexibleDismissButtonText  = json.optString(KEY_FLEXIBLE_DISMISS_BTN, d.FLEXIBLE_DISMISS_BUTTON_TEXT),
            storeUrl              = json.optString(KEY_STORE_URL,              d.STORE_URL)
        )
    }

    private fun log(message: String) {
        if (loggingEnabled) Log.d(TAG, message)
    }
}

