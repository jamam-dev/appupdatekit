/**
 * AppUpdateKit
 * SnoozeManager.kt
 * Purpose: Manages snooze / remind-later state using SharedPreferences.
 */
package com.appupdatekit

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages the snooze (remind-later) state for update prompts.
 *
 * All SharedPreferences keys are prefixed with `auk_snooze_` to avoid conflicts
 * with the host application's own preferences.
 *
 * @param context An application [Context] used to access [SharedPreferences].
 */
internal class SnoozeManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ─── Constants ────────────────────────────────────────────────────────────

    private companion object {
        const val PREFS_NAME = "auk_snooze_prefs"
        const val KEY_LAST_SNOOZE_TIMESTAMP = "auk_snooze_last_timestamp"
        const val KEY_SNOOZE_COUNT = "auk_snooze_count"
        const val MILLIS_PER_HOUR = 3_600_000L
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Returns `true` if the update prompt should be shown based on snooze state.
     *
     * Returns `false` when:
     * 1. The user has snoozed the maximum allowed number of times AND
     *    [UpdateConfig.snoozeMaxCount] is > 0.
     * 2. The snooze duration since the last snooze has not yet elapsed.
     *
     * @param config The resolved [UpdateConfig] containing snooze policy values.
     */
    fun shouldShowUpdate(config: UpdateConfig): Boolean {
        val snoozeCount = getSnoozeCount()

        // If max snooze count exceeded (and limit is not unlimited), block prompt.
        if (config.snoozeMaxCount > 0 && snoozeCount >= config.snoozeMaxCount) {
            return false
        }

        val lastSnoozeTimestamp = prefs.getLong(KEY_LAST_SNOOZE_TIMESTAMP, 0L)
        if (lastSnoozeTimestamp == 0L) {
            return true // Never snoozed before.
        }

        val elapsedHours = (System.currentTimeMillis() - lastSnoozeTimestamp) / MILLIS_PER_HOUR
        return elapsedHours >= config.snoozeDurationHours
    }

    /**
     * Records a snooze action: increments the snooze counter and saves the current
     * timestamp as the last snooze time.
     */
    fun recordSnooze() {
        prefs.edit()
            .putLong(KEY_LAST_SNOOZE_TIMESTAMP, System.currentTimeMillis())
            .putInt(KEY_SNOOZE_COUNT, getSnoozeCount() + 1)
            .apply()
    }

    /**
     * Resets all snooze state. Call this after a successful update installation.
     */
    fun resetSnooze() {
        prefs.edit()
            .remove(KEY_LAST_SNOOZE_TIMESTAMP)
            .remove(KEY_SNOOZE_COUNT)
            .apply()
    }

    /**
     * Returns the number of times the user has snoozed the update prompt.
     */
    fun getSnoozeCount(): Int = prefs.getInt(KEY_SNOOZE_COUNT, 0)
}

