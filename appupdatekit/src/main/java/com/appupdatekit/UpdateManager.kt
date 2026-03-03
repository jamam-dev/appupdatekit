/**
 * AppUpdateKit
 * UpdateManager.kt
 * Purpose: Core orchestration logic — reads Remote Config JSON, checks maintenance,
 *          evaluates update policy, and routes to the correct flow or screen.
 */
package com.appupdatekit

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.tasks.await
import java.lang.ref.WeakReference

/**
 * Coordinates all update-related logic:
 *
 * 1. Reads & parses the update config JSON from the already-active FirebaseRemoteConfig.
 * 2. Checks master kill-switch (`update_enabled`).
 * 3. Checks maintenance flag — shows [MaintenanceActivity] if true.
 * 4. Queries [AppUpdateManager] for update availability.
 * 5. Compares version codes against `minimum_version_code`.
 * 6. Checks snooze state (flexible only).
 * 7. Routes: force update → [ForceUpdateActivity]; flexible → [UpdateFlowHandler].
 *
 * @param activityRef          WeakReference to host Activity.
 * @param callback             UpdateCallback to notify.
 * @param remoteConfigKey      The RC key holding the JSON config blob.
 * @param fetchTimeoutSeconds  Kept for API compatibility (timeout on any async ops).
 * @param forceUpdateScreenConfig  Visual customization for the force update screen.
 * @param maintenanceScreenConfig  Visual customization for the maintenance screen.
 * @param loggingEnabled       Whether to emit log messages.
 */
internal class UpdateManager(
    private val activityRef: WeakReference<Activity>,
    private val callback: UpdateCallback,
    private val remoteConfigKey: String,
    private val fetchTimeoutSeconds: Long,
    private val forceUpdateScreenConfig: ForceUpdateScreenConfig,
    private val maintenanceScreenConfig: MaintenanceScreenConfig,
    private val loggingEnabled: Boolean
) {

    private companion object {
        const val TAG = "AppUpdateKit"
    }

    private val activity: Activity? get() = activityRef.get()
    private val context: Context? get() = activity?.applicationContext

    private val snoozeManager: SnoozeManager by lazy { SnoozeManager(context!!) }
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
            Log.w(TAG, "AppUpdateKit: Activity reference is null — aborting.")
            return
        }
        val ctx = act.applicationContext

        // 1. Parse config from already-active Remote Config.
        log("Reading update config from RC key '$remoteConfigKey'.")
        val config = UpdateConfigParser(
            remoteConfigKey = remoteConfigKey,
            loggingEnabled = loggingEnabled
        ).parse()
        log("Config: $config")

        // 2. Master kill-switch.
        if (!config.isUpdateEnabled) {
            log("Updates disabled via Remote Config.")
            callback.onUpdateDisabledByRemoteConfig()
            return
        }

        // 3. Maintenance check — takes priority over update checks.
        if (config.maintenanceEnabled) {
            log("Maintenance mode active — launching MaintenanceActivity.")
            act.startActivity(
                MaintenanceActivity.buildIntent(
                    act,
                    config,
                    maintenanceScreenConfig,
                    remoteConfigKey,
                    loggingEnabled
                )
            )
            return
        }

        // 4. Get update info from Play.
        val appUpdateManager = AppUpdateManagerFactory.create(ctx)
        val appUpdateInfo = try {
            appUpdateManager.appUpdateInfo.await()
        } catch (e: Exception) {
            Log.w(TAG, "AppUpdateKit: Failed to get AppUpdateInfo — ${e.message}")
            callback.onUpdateFailed(e)
            return
        }

        log("Update availability: ${appUpdateInfo.updateAvailability()}")

        if (appUpdateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
            log("No update available.")
            callback.onUpdateNotAvailable()
            return
        }

        // 5. Compare version codes.
        val availableVersionCode = appUpdateInfo.availableVersionCode().toLong()
        log("Available: $availableVersionCode  |  Min required: ${config.minimumVersionCode}")

        if (config.minimumVersionCode > 0L && availableVersionCode < config.minimumVersionCode) {
            log("Available version below minimum — no prompt.")
            callback.onUpdateNotAvailable()
            return
        }

        // 6. Force update → launch ForceUpdateActivity (blocking, no snooze).
        if (config.forceUpdate) {
            log("Force update required — launching ForceUpdateActivity.")
            act.startActivity(
                ForceUpdateActivity.buildIntent(
                    act,
                    config,
                    forceUpdateScreenConfig,
                    loggingEnabled
                )
            )
            callback.onUpdateAvailable(UpdateType.IMMEDIATE)
            return
        }

        // 7. Flexible update — check snooze.
        if (!snoozeManager.shouldShowUpdate(config)) {
            val count = snoozeManager.getSnoozeCount()
            log("Flexible update snoozed. Count: $count")
            callback.onUpdateSnoozed(count)
            return
        }

        log("Launching FLEXIBLE update flow.")
        flowHandler = UpdateFlowHandler(
            appUpdateManager = appUpdateManager,
            activityRef = activityRef,
            callback = callback,
            loggingEnabled = loggingEnabled
        )

        if (appUpdateInfo.isUpdateTypeAllowed(com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE)) {
            flowHandler?.launchFlexibleUpdate(appUpdateInfo)
        } else {
            Log.w(TAG, "AppUpdateKit: FLEXIBLE not allowed by Play — notifying not available.")
            callback.onUpdateNotAvailable()
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
        SnoozeManager(ctx).also {
            it.recordSnooze()
            callback.onUpdateSnoozed(it.getSnoozeCount())
        }
    }

    /**
     * Resets snooze state after a successful update.
     */
    fun resetSnooze() {
        context?.let { SnoozeManager(it).resetSnooze() }
    }

    /**
     * Cleans up listeners and resources. Forward from [Activity.onDestroy].
     */
    fun destroy() {
        flowHandler?.destroy()
        flowHandler = null
    }

    private fun log(message: String) {
        if (loggingEnabled) Log.d(TAG, message)
    }
}
