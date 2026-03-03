/**
 * AppUpdateKit
 * UpdateConfig.kt
 * Purpose: Data class holding all resolved Remote Config values for update logic.
 */
package com.appupdatekit

/**
 * Holds the resolved configuration values fetched from Firebase Remote Config.
 *
 * All Remote Config keys use the `auk_` prefix to avoid conflicts with the
 * host application's own Remote Config keys.
 *
 * @property isUpdateEnabled     Master on/off toggle. Key: `auk_update_enabled`
 * @property forceUpdate         If true, use [UpdateType.IMMEDIATE]; else [UpdateType.FLEXIBLE].
 *                               Key: `auk_force_update`
 * @property minimumVersionCode  Minimum acceptable app version code. If the current version
 *                               is below this value an update is required.
 *                               Key: `auk_minimum_version_code`
 * @property snoozeDurationHours Hours to wait before re-prompting after the user snoozes.
 *                               Key: `auk_snooze_duration_hours`
 * @property snoozeMaxCount      Maximum number of times the user can snooze (0 = unlimited).
 *                               Key: `auk_snooze_max_count`
 */
internal data class UpdateConfig(
    /** `auk_update_enabled` — master switch; false disables all update prompts. */
    val isUpdateEnabled: Boolean = Defaults.UPDATE_ENABLED,

    /** `auk_force_update` — true maps to IMMEDIATE flow, false to FLEXIBLE. */
    val forceUpdate: Boolean = Defaults.FORCE_UPDATE,

    /** `auk_minimum_version_code` — version code below which an update is mandatory. */
    val minimumVersionCode: Long = Defaults.MINIMUM_VERSION_CODE,

    /** `auk_snooze_duration_hours` — hours before the prompt reappears after snooze. */
    val snoozeDurationHours: Long = Defaults.SNOOZE_DURATION_HOURS,

    /** `auk_snooze_max_count` — 0 means the user can snooze indefinitely. */
    val snoozeMaxCount: Long = Defaults.SNOOZE_MAX_COUNT
) {
    /** Default values mirroring the Firebase Remote Config defaults. */
    internal object Defaults {
        const val UPDATE_ENABLED: Boolean = true
        const val FORCE_UPDATE: Boolean = false
        const val MINIMUM_VERSION_CODE: Long = 0L
        const val SNOOZE_DURATION_HOURS: Long = 24L
        const val SNOOZE_MAX_COUNT: Long = 3L
    }

    /** Remote Config key names. All prefixed with `auk_`. */
    internal object Keys {
        const val UPDATE_ENABLED = "auk_update_enabled"
        const val FORCE_UPDATE = "auk_force_update"
        const val MINIMUM_VERSION_CODE = "auk_minimum_version_code"
        const val SNOOZE_DURATION_HOURS = "auk_snooze_duration_hours"
        const val SNOOZE_MAX_COUNT = "auk_snooze_max_count"
    }
}

