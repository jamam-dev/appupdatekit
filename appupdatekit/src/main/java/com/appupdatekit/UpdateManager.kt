/**
 * AppUpdateKit
 * UpdateManager.kt
 * Purpose: Core orchestration logic — coordinates Remote Config, Play update info,
 *          snooze state, and routing to the correct update flow.
 */
package com.appupdatekit

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.lang.ref.WeakReference

/**
 * Coordinates all update-related logic:
 *
 * 1. Fetches [UpdateConfig] from [RemoteConfigManager].
 * 2. Checks the master kill-switch (`auk_update_enabled`).
 * 3. Queries [AppUpdateManager] for the current update availability.
 * 4. Compares the available version code against `auk_minimum_version_code`.
 * 5. Consults [SnoozeManager] to decide whether the prompt should be shown.
 * 6. Routes to the correct flow via [UpdateFlowHandler].
 *
 * @param activityRef    A [WeakReference] to the host [Activity]. The manager will
 *                       refuse to operate if the reference is cleared.
 * @param callback       The [UpdateCallback] to notify.
 * @param loggingEnabled Whether diagnostic log output is enabled.
 */
internal class UpdateManager(
    private val activityRef: WeakReference<Activity>,
    private val callback: UpdateCallback,
    private val fetchTimeoutSeconds: Long,
    private val loggingEnabled: Boolean
) {

    // ─── Constants ────────────────────────────────────────────────────────────

    private companion object {
        const val TAG = "AppUpdateKit"
    }

    // ─── Dependencies ─────────────────────────────────────────────────────────

    private val activity: Activity? get() = activityRef.get()
    private val context: Context? get() = activity?.applicationContext

    private val remoteConfigManager: RemoteConfigManager by lazy {
        RemoteConfigManager(
            isDebug = isDebugBuild(),
            loggingEnabled = loggingEnabled
        )
    }

    private val snoozeManager: SnoozeManager by lazy {
        SnoozeManager(context!!)
    }

    private var flowHandler: UpdateFlowHandler? = null

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Main entry point. Fetches Remote Config, evaluates policy, and launches the
     * appropriate update flow.
     *
     * This is a **suspending** function; the caller is responsible for launching it
     * inside a coroutine (e.g., `lifecycleScope.launch`).
     */
    suspend fun checkAndPrompt() {
        val act = activity ?: run {
            Log.w(TAG, "AppUpdateKit: Activity reference is null — aborting update check.")
            return
        }
        val ctx = act.applicationContext

        // 1. Fetch Remote Config with timeout.
        log("Fetching Remote Config (timeout: ${fetchTimeoutSeconds}s)...")
        val config = try {
            withTimeout(fetchTimeoutSeconds * 1_000L) {
                remoteConfigManager.fetchUpdateConfig()
            }
        } catch (e: Exception) {
            Log.w(TAG, "AppUpdateKit: Remote Config fetch timed out or failed — using defaults.")
            UpdateConfig()
        }
        log("Config fetched: $config")

        // 2. Check master kill-switch.
        if (!config.isUpdateEnabled) {
            log("Updates disabled via Remote Config.")
            callback.onUpdateDisabledByRemoteConfig()
            return
        }

        // 3. Get update info from Play.
        val appUpdateManager = AppUpdateManagerFactory.create(ctx)
        val appUpdateInfo = try {
            appUpdateManager.appUpdateInfo.await()
        } catch (e: Exception) {
            Log.w(TAG, "AppUpdateKit: Failed to get AppUpdateInfo — ${e.message}")
            callback.onUpdateFailed(e)
            return
        }

        log("AppUpdateInfo availability: ${appUpdateInfo.updateAvailability()}")

        if (appUpdateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
            log("No update available.")
            callback.onUpdateNotAvailable()
            return
        }

        // 4. Compare version codes.
        val availableVersionCode = appUpdateInfo.availableVersionCode().toLong()
        log("Available version code: $availableVersionCode, minimum required: ${config.minimumVersionCode}")

        if (config.minimumVersionCode > 0L && availableVersionCode < config.minimumVersionCode) {
            log("Available version ($availableVersionCode) is below minimum required (${config.minimumVersionCode}) — no prompt.")
            callback.onUpdateNotAvailable()
            return
        }

        // 5. Check snooze state.
        if (!snoozeManager.shouldShowUpdate(config)) {
            val snoozeCount = snoozeManager.getSnoozeCount()
            log("Update snoozed. Snooze count: $snoozeCount")
            callback.onUpdateSnoozed(snoozeCount)
            return
        }

        // 6. Route to the correct flow.
        val updateType = if (config.forceUpdate) UpdateType.IMMEDIATE else UpdateType.FLEXIBLE
        log("Routing to $updateType update flow.")

        flowHandler = UpdateFlowHandler(
            appUpdateManager = appUpdateManager,
            activityRef = activityRef,
            callback = callback,
            loggingEnabled = loggingEnabled
        )

        when (updateType) {
            UpdateType.IMMEDIATE -> {
                if (appUpdateInfo.isUpdateTypeAllowed(com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE)) {
                    flowHandler?.launchImmediateUpdate(appUpdateInfo)
                } else {
                    Log.w(TAG, "AppUpdateKit: IMMEDIATE update not allowed by Play — falling back to FLEXIBLE.")
                    flowHandler?.launchFlexibleUpdate(appUpdateInfo)
                }
            }
            UpdateType.FLEXIBLE -> {
                if (appUpdateInfo.isUpdateTypeAllowed(com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE)) {
                    flowHandler?.launchFlexibleUpdate(appUpdateInfo)
                } else {
                    Log.w(TAG, "AppUpdateKit: FLEXIBLE update not allowed by Play — notifying not available.")
                    callback.onUpdateNotAvailable()
                }
            }
            UpdateType.NONE -> {
                callback.onUpdateNotAvailable()
            }
        }
    }

    /**
     * Forward from [Activity.onActivityResult].
     *
     * @param requestCode The request code.
     * @param resultCode  The result code.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int) {
        flowHandler?.onActivityResult(requestCode, resultCode)
    }

    /**
     * Forward from [Activity.onResume].
     *
     * Checks for a pending flexible update that was downloaded while the app was
     * in the background and triggers [UpdateCallback.onUpdateDownloaded] if one exists.
     */
    fun onResume() {
        flowHandler?.checkForPendingFlexibleUpdate()
    }

    /**
     * Completes a downloaded flexible update by triggering the restart/install.
     * Call this after the user confirms (e.g., from a Snackbar action).
     */
    fun completeFlexibleUpdate() {
        flowHandler?.completeFlexibleUpdate()
    }

    /**
     * Records that the user has chosen to snooze this update.
     */
    fun recordSnooze() {
        val ctx = context ?: return
        val config = UpdateConfig() // Use defaults for snooze recording.
        SnoozeManager(ctx).also {
            it.recordSnooze()
            callback.onUpdateSnoozed(it.getSnoozeCount())
        }
    }

    /**
     * Resets snooze state after a successful update.
     */
    fun resetSnooze() {
        val ctx = context ?: return
        SnoozeManager(ctx).resetSnooze()
    }

    /**
     * Cleans up listeners and resources. Forward from [Activity.onDestroy].
     */
    fun destroy() {
        flowHandler?.destroy()
        flowHandler = null
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun isDebugBuild(): Boolean {
        return try {
            val ctx = context ?: return false
            val appInfo = ctx.packageManager.getApplicationInfo(ctx.packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }

    private fun log(message: String) {
        if (loggingEnabled) Log.d(TAG, message)
    }
}

