/**
 * AppUpdateKit
 * UpdateConfig.kt
 * Purpose: Data class holding all resolved update policy values parsed from the Remote Config JSON.
 */
package com.appupdatekit

/**
 * Holds all update policy values parsed from the Remote Config JSON blob.
 *
 * The host app fetches and activates Remote Config, then AppUpdateKit reads the
 * value of a single key (default: [DEFAULT_REMOTE_CONFIG_KEY]) and parses this object.
 *
 * ### JSON schema example:
 * ```json
 * {
 *   "update_enabled": true,
 *   "force_update": false,
 *   "minimum_version_code": 10,
 *   "snooze_duration_hours": 24,
 *   "snooze_max_count": 3,
 *   "maintenance_enabled": false,
 *   "maintenance_message": "We are under maintenance. Please check back soon.",
 *   "force_update_message": "A critical update is required to continue.",
 *   "force_update_button_text": "Update Now",
 *   "flexible_update_message": "A new version is available!",
 *   "flexible_update_button_text": "Update",
 *   "flexible_dismiss_button_text": "Later",
 *   "store_url": "https://play.google.com/store/apps/details?id=com.your.app"
 * }
 * ```
 */
internal data class UpdateConfig(
    /** Master on/off toggle. `false` disables all update and maintenance prompts. */
    val isUpdateEnabled: Boolean = Defaults.UPDATE_ENABLED,

    /** `true` = show force update screen (blocking). `false` = flexible (dismissible). */
    val forceUpdate: Boolean = Defaults.FORCE_UPDATE,

    /** Minimum acceptable app version code. `0` means any available update is shown. */
    val minimumVersionCode: Long = Defaults.MINIMUM_VERSION_CODE,

    /** Hours before the flexible update prompt re-appears after snooze. */
    val snoozeDurationHours: Long = Defaults.SNOOZE_DURATION_HOURS,

    /** Max times user can snooze. `0` = unlimited. Only applies to flexible updates. */
    val snoozeMaxCount: Long = Defaults.SNOOZE_MAX_COUNT,

    /** `true` = show maintenance screen (fully blocking). */
    val maintenanceEnabled: Boolean = Defaults.MAINTENANCE_ENABLED,

    /** Message shown on the maintenance screen. */
    val maintenanceMessage: String = Defaults.MAINTENANCE_MESSAGE,

    /** Message shown on the force update screen. */
    val forceUpdateMessage: String = Defaults.FORCE_UPDATE_MESSAGE,

    /** Button label on the force update screen. */
    val forceUpdateButtonText: String = Defaults.FORCE_UPDATE_BUTTON_TEXT,

    /** Message shown on the flexible update prompt. */
    val flexibleUpdateMessage: String = Defaults.FLEXIBLE_UPDATE_MESSAGE,

    /** Confirm button label on the flexible update prompt. */
    val flexibleUpdateButtonText: String = Defaults.FLEXIBLE_UPDATE_BUTTON_TEXT,

    /** Dismiss button label on the flexible update prompt. */
    val flexibleDismissButtonText: String = Defaults.FLEXIBLE_DISMISS_BUTTON_TEXT,

    /**
     * Play Store URL to open if Play In-App Update IMMEDIATE flow is unavailable.
     * If empty the library constructs the URL from the host app's package name.
     */
    val storeUrl: String = Defaults.STORE_URL
) {
    /** Hard-coded defaults — used when JSON is missing a field or RC fetch fails. */
    internal object Defaults {
        const val UPDATE_ENABLED = true
        const val FORCE_UPDATE = false
        const val MINIMUM_VERSION_CODE = 0L
        const val SNOOZE_DURATION_HOURS = 24L
        const val SNOOZE_MAX_COUNT = 3L
        const val MAINTENANCE_ENABLED = false
        const val MAINTENANCE_MESSAGE = "We are currently under maintenance. Please check back soon."
        const val FORCE_UPDATE_MESSAGE = "A critical update is required to continue using this app."
        const val FORCE_UPDATE_BUTTON_TEXT = "Update Now"
        const val FLEXIBLE_UPDATE_MESSAGE = "A new version is available."
        const val FLEXIBLE_UPDATE_BUTTON_TEXT = "Update"
        const val FLEXIBLE_DISMISS_BUTTON_TEXT = "Later"
        const val STORE_URL = ""
    }

    /** The default Remote Config key the library reads when none is specified via the Builder. */
    companion object {
        const val DEFAULT_REMOTE_CONFIG_KEY = "auk_update_config"
    }
}
