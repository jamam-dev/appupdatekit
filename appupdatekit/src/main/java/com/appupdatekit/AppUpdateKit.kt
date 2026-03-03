/**
 * AppUpdateKit
 * AppUpdateKit.kt
 * Purpose: Public entry point for the AppUpdateKit library. Builder pattern API.
 */
package com.appupdatekit

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * **AppUpdateKit** — Production-grade Android in-app update library.
 *
 * Orchestrates Google Play In-App Updates (Flexible & Immediate) controlled via
 * Firebase Remote Config.
 *
 * ## Usage
 *
 * ### Verbose (explicit Builder):
 * ```kotlin
 * val kit = AppUpdateKit.with(this)
 *     .setCallback(object : UpdateCallback {
 *         override fun onUpdateAvailable(updateType: UpdateType) { ... }
 *         override fun onUpdateDownloaded() { showSnackbarToInstall() }
 *         override fun onUpdateFailed(exception: Exception) { Log.e(TAG, exception.message, exception) }
 *     })
 *     .enableLogging(BuildConfig.DEBUG)
 *     .build()
 *
 * kit.checkForUpdate()
 *
 * // Lifecycle wiring:
 * override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
 *     super.onActivityResult(requestCode, resultCode, data)
 *     kit.onActivityResult(requestCode, resultCode)
 * }
 * override fun onResume() { super.onResume(); kit.onResume() }
 * override fun onDestroy() { super.onDestroy(); kit.onDestroy() }
 * ```
 *
 * ### Shorthand (extension function):
 * ```kotlin
 * val kit = checkAppUpdate { enableLogging(BuildConfig.DEBUG) }
 * ```
 *
 * @see UpdateCallback
 * @see UpdateType
 * @see com.appupdatekit.extensions.checkAppUpdate
 */
class AppUpdateKit private constructor(
    activity: Activity,
    private val callback: UpdateCallback,
    private val fetchTimeoutSeconds: Long,
    private val loggingEnabled: Boolean
) {

    // ─── Constants ────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "AppUpdateKit"
        private const val DEFAULT_FETCH_TIMEOUT_SECONDS = 10L

        /**
         * Entry point for the Builder pattern.
         *
         * @param activity The host [Activity].
         * @return A new [Builder] instance.
         */
        @JvmStatic
        fun with(activity: Activity): Builder = Builder(activity)
    }

    // ─── Internal state ───────────────────────────────────────────────────────

    private val activityRef: WeakReference<Activity> = WeakReference(activity)
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val updateManager: UpdateManager = UpdateManager(
        activityRef = activityRef,
        callback = callback,
        fetchTimeoutSeconds = fetchTimeoutSeconds,
        loggingEnabled = loggingEnabled
    )

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Initiates the update check flow asynchronously.
     *
     * Internally launches a coroutine on [Dispatchers.Main]. The caller does NOT
     * need to manage the coroutine manually.
     */
    fun checkForUpdate() {
        scope.launch {
            try {
                updateManager.checkAndPrompt()
            } catch (e: Exception) {
                Log.e(TAG, "AppUpdateKit: Unexpected error during update check.", e)
                callback.onUpdateFailed(e)
            }
        }
    }

    /**
     * Forward this from your [Activity.onActivityResult] override.
     *
     * @param requestCode The request code.
     * @param resultCode  The result code.
     */
    fun onActivityResult(requestCode: Int, resultCode: Int) {
        updateManager.onActivityResult(requestCode, resultCode)
    }

    /**
     * Forward this from your [Activity.onResume] override.
     *
     * Checks for flexible updates that finished downloading while the app was in
     * the background and re-triggers [UpdateCallback.onUpdateDownloaded] if needed.
     */
    fun onResume() {
        updateManager.onResume()
    }

    /**
     * Completes a downloaded flexible update (triggers the app restart/install).
     *
     * Call this after the user confirms installation, for example from a Snackbar
     * action button. Only relevant for [UpdateType.FLEXIBLE] flows.
     */
    fun completeFlexibleUpdate() {
        updateManager.completeFlexibleUpdate()
    }

    /**
     * Records a user snooze action and fires [UpdateCallback.onUpdateSnoozed].
     *
     * Call this if your UI has a "Remind me later" / "Not now" option.
     */
    fun snooze() {
        updateManager.recordSnooze()
    }

    /**
     * Resets snooze state. Call after a successful update installation.
     */
    fun resetSnooze() {
        updateManager.resetSnooze()
    }

    /**
     * Releases all resources and cancels internal coroutines.
     *
     * **Must** be called from [Activity.onDestroy].
     */
    fun onDestroy() {
        updateManager.destroy()
        scope.cancel()
    }

    // ─── Builder ──────────────────────────────────────────────────────────────

    /**
     * Fluent builder for [AppUpdateKit]. Obtain an instance via [AppUpdateKit.with].
     *
     * @param activity The host [Activity] from which update flows will be launched.
     */
    class Builder(private val activity: Activity) {

        private var callback: UpdateCallback = object : UpdateCallback {}
        private var fetchTimeoutSeconds: Long = DEFAULT_FETCH_TIMEOUT_SECONDS
        private var loggingEnabled: Boolean = false

        /**
         * Sets the [UpdateCallback] to receive update lifecycle events.
         * All methods in [UpdateCallback] have default no-op implementations.
         */
        fun setCallback(callback: UpdateCallback): Builder = apply {
            this.callback = callback
        }

        /**
         * Sets the maximum time (in seconds) to wait for a Remote Config fetch.
         * Defaults to **10 seconds**.
         *
         * @param seconds Timeout in seconds (minimum 1).
         */
        fun setFetchTimeoutSeconds(seconds: Long): Builder = apply {
            this.fetchTimeoutSeconds = seconds.coerceAtLeast(1L)
        }

        /**
         * Enables or disables verbose diagnostic logging under the `AppUpdateKit` log tag.
         *
         * Defaults to `false`. Strongly recommended to pass `BuildConfig.DEBUG` so
         * logs are only emitted in debug builds.
         *
         * @param enable `true` to enable logging.
         */
        fun enableLogging(enable: Boolean): Builder = apply {
            this.loggingEnabled = enable
        }

        /**
         * Builds and returns an [AppUpdateKit] instance.
         */
        fun build(): AppUpdateKit = AppUpdateKit(
            activity = activity,
            callback = callback,
            fetchTimeoutSeconds = fetchTimeoutSeconds,
            loggingEnabled = loggingEnabled
        )
    }
}

