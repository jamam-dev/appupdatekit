/**
 * AppUpdateKit
 * UpdateFlowHandler.kt
 * Purpose: Handles launching FLEXIBLE and IMMEDIATE in-app update flows via the Play library.
 */
package com.appupdatekit

import android.app.Activity
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import java.lang.ref.WeakReference

/**
 * Handles launching [AppUpdateType.FLEXIBLE] and [AppUpdateType.IMMEDIATE] update flows
 * using the Google Play In-App Updates API.
 *
 * A [WeakReference] to the [Activity] is held to avoid memory leaks when the activity
 * is destroyed while an update is in progress.
 *
 * @param appUpdateManager   The Play [AppUpdateManager] instance.
 * @param activityRef        A [WeakReference] to the host [Activity].
 * @param callback           The [UpdateCallback] to notify of events.
 * @param loggingEnabled     Whether diagnostic logging is active.
 */
internal class UpdateFlowHandler(
    private val appUpdateManager: AppUpdateManager,
    private val activityRef: WeakReference<Activity>,
    private val callback: UpdateCallback,
    private val loggingEnabled: Boolean
) {

    // ─── Constants ────────────────────────────────────────────────────────────

    private companion object {
        const val TAG = "AppUpdateKit"
        const val REQUEST_CODE_UPDATE = 7357
    }

    // ─── State ────────────────────────────────────────────────────────────────

    /** Listener for flexible update download state changes. */
    private var installStateListener: InstallStateUpdatedListener? = null

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Launches a **flexible** update flow.
     *
     * Registers an [InstallStateUpdatedListener] to monitor download progress and
     * fires [UpdateCallback.onUpdateDownloaded] when the download completes.
     *
     * @param appUpdateInfo The [AppUpdateInfo] returned by [AppUpdateManager.getAppUpdateInfo].
     */
    fun launchFlexibleUpdate(appUpdateInfo: AppUpdateInfo) {
        val activity = activityRef.get() ?: run {
            Log.w(TAG, "AppUpdateKit: Activity is null — cannot launch flexible update.")
            return
        }

        log("Launching FLEXIBLE update flow.")

        // Register listener before starting the flow.
        installStateListener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> {
                    log("Flexible update downloaded — notifying callback.")
                    callback.onUpdateDownloaded()
                }
                InstallStatus.FAILED -> {
                    val msg = "Flexible update install failed with error: ${state.installErrorCode()}"
                    Log.w(TAG, "AppUpdateKit: $msg")
                    callback.onUpdateFailed(Exception(msg))
                    unregisterInstallStateListener()
                }
                InstallStatus.INSTALLED -> {
                    log("Flexible update installed successfully.")
                    unregisterInstallStateListener()
                }
                InstallStatus.CANCELED -> {
                    log("Flexible update installation cancelled by user.")
                    unregisterInstallStateListener()
                }
                else -> {
                    log("Flexible update install status: ${state.installStatus()}")
                }
            }
        }.also { appUpdateManager.registerListener(it) }

        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                REQUEST_CODE_UPDATE
            )
            callback.onUpdateAvailable(UpdateType.FLEXIBLE)
        } catch (e: Exception) {
            Log.e(TAG, "AppUpdateKit: Failed to start flexible update flow.", e)
            callback.onUpdateFailed(e)
            unregisterInstallStateListener()
        }
    }

    /**
     * Launches an **immediate** update flow.
     *
     * The user cannot interact with the app until the update is installed.
     *
     * @param appUpdateInfo The [AppUpdateInfo] returned by [AppUpdateManager.getAppUpdateInfo].
     */
    fun launchImmediateUpdate(appUpdateInfo: AppUpdateInfo) {
        val activity = activityRef.get() ?: run {
            Log.w(TAG, "AppUpdateKit: Activity is null — cannot launch immediate update.")
            return
        }

        log("Launching IMMEDIATE update flow.")

        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                REQUEST_CODE_UPDATE
            )
            callback.onUpdateAvailable(UpdateType.IMMEDIATE)
        } catch (e: Exception) {
            Log.e(TAG, "AppUpdateKit: Failed to start immediate update flow.", e)
            callback.onUpdateFailed(e)
        }
    }

    /**
     * Handles the [Activity.onActivityResult] callback.
     * Should be forwarded from the host [Activity].
     *
     * @param requestCode The request code passed to [Activity.startActivityForResult].
     * @param resultCode  The result code returned by the child activity.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int) {
        if (requestCode != REQUEST_CODE_UPDATE) return

        when (resultCode) {
            Activity.RESULT_OK -> log("Update flow completed with RESULT_OK.")
            Activity.RESULT_CANCELED -> {
                log("Update flow cancelled by user.")
                callback.onUpdateFailed(Exception("Update cancelled by user."))
            }
            else -> {
                val msg = "Update flow returned unexpected result code: $resultCode"
                Log.w(TAG, "AppUpdateKit: $msg")
                callback.onUpdateFailed(Exception(msg))
            }
        }
    }

    /**
     * Checks whether a flexible update has been downloaded but not yet installed
     * (e.g., after the app was backgrounded). Call from [Activity.onResume].
     *
     * If a downloaded update is waiting, [UpdateCallback.onUpdateDownloaded] is invoked
     * so the host app can prompt the user to complete installation.
     */
    fun checkForPendingFlexibleUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                log("Pending flexible update found on resume — notifying callback.")
                callback.onUpdateDownloaded()
            }
        }
    }

    /**
     * Triggers the final installation of a downloaded flexible update.
     * Call this after the user confirms they want to install (e.g., after tapping
     * a Snackbar action).
     */
    fun completeFlexibleUpdate() {
        log("Completing flexible update installation.")
        appUpdateManager.completeUpdate()
    }

    /**
     * Unregisters the [InstallStateUpdatedListener] and releases resources.
     * Must be called from [Activity.onDestroy].
     */
    fun destroy() {
        unregisterInstallStateListener()
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun unregisterInstallStateListener() {
        installStateListener?.let {
            appUpdateManager.unregisterListener(it)
            installStateListener = null
            log("InstallStateUpdatedListener unregistered.")
        }
    }

    private fun log(message: String) {
        if (loggingEnabled) Log.d(TAG, message)
    }
}

