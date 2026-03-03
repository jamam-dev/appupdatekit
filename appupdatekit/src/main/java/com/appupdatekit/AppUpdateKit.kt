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
 * Reads update policy from a **single Firebase Remote Config JSON key** (the host app
 * is responsible for calling `fetchAndActivate()` before invoking this library).
 *
 * Supports:
 * - **Force update screen** — fully blocking, tries Play IMMEDIATE flow, falls back to Play Store.
 * - **Maintenance screen** — fully blocking, with "Try Again" button.
 * - **Flexible update** — Play In-App Update background download via [UpdateFlowHandler].
 * - **Snooze** — configurable duration and max count (flexible only).
 *
 * ## Usage — Verbose Builder:
 * ```kotlin
 * val kit = AppUpdateKit.with(this)
 *     .setRemoteConfigKey("auk_update_config")   // optional, this is the default
 *     .setCallback(object : UpdateCallback {
 *         override fun onUpdateDownloaded() { showSnackbar() }
 *     })
 *     .setForceUpdateScreenConfig(ForceUpdateScreenConfig(buttonColorRes = R.color.accent))
 *     .setMaintenanceScreenConfig(MaintenanceScreenConfig(illustrationRes = R.drawable.my_icon))
 *     .enableLogging(BuildConfig.DEBUG)
 *     .build()
 * kit.checkForUpdate()
 * ```
 *
 * ## Usage — Shorthand:
 * ```kotlin
 * val kit = checkAppUpdate { setRemoteConfigKey("auk_update_config") }
 * kit.checkForUpdate()
 * ```
 */
class AppUpdateKit private constructor(
    activity: Activity,
    private val callback: UpdateCallback,
    private val remoteConfigKey: String,
    private val fetchTimeoutSeconds: Long,
    private val forceUpdateScreenConfig: ForceUpdateScreenConfig,
    private val maintenanceScreenConfig: MaintenanceScreenConfig,
    private val loggingEnabled: Boolean
) {

    companion object {
        private const val TAG = "AppUpdateKit"
        private const val DEFAULT_FETCH_TIMEOUT_SECONDS = 10L

        /** Entry point for the Builder pattern. */
        @JvmStatic
        fun with(activity: Activity): Builder = Builder(activity)
    }

    private val activityRef = WeakReference(activity)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val updateManager = UpdateManager(
        activityRef = activityRef,
        callback = callback,
        remoteConfigKey = remoteConfigKey,
        fetchTimeoutSeconds = fetchTimeoutSeconds,
        forceUpdateScreenConfig = forceUpdateScreenConfig,
        maintenanceScreenConfig = maintenanceScreenConfig,
        loggingEnabled = loggingEnabled
    )

    // ─── Public API ───────────────────────────────────────────────────────────

    /** Starts the update/maintenance check asynchronously. */
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

    /** Forward from [Activity.onActivityResult]. */
    fun onActivityResult(requestCode: Int, resultCode: Int) =
        updateManager.onActivityResult(requestCode, resultCode)

    /** Forward from [Activity.onResume] — detects pending flexible download. */
    fun onResume() = updateManager.onResume()

    /** Completes a downloaded flexible update (triggers restart). */
    fun completeFlexibleUpdate() = updateManager.completeFlexibleUpdate()

    /** Records a snooze action for flexible updates. */
    fun snooze() = updateManager.recordSnooze()

    /** Resets snooze state after a successful install. */
    fun resetSnooze() = updateManager.resetSnooze()

    /** Releases all resources. Must be called from [Activity.onDestroy]. */
    fun onDestroy() {
        updateManager.destroy()
        scope.cancel()
    }

    // ─── Builder ──────────────────────────────────────────────────────────────

    /**
     * Fluent builder for [AppUpdateKit].
     */
    class Builder(private val activity: Activity) {

        private var callback: UpdateCallback              = object : UpdateCallback {}
        private var remoteConfigKey: String               = UpdateConfig.DEFAULT_REMOTE_CONFIG_KEY
        private var fetchTimeoutSeconds: Long             = DEFAULT_FETCH_TIMEOUT_SECONDS
        private var forceUpdateScreenConfig               = ForceUpdateScreenConfig()
        private var maintenanceScreenConfig               = MaintenanceScreenConfig()
        private var loggingEnabled: Boolean               = false

        /**
         * Sets the [UpdateCallback] to receive update lifecycle events.
         */
        fun setCallback(callback: UpdateCallback): Builder = apply {
            this.callback = callback
        }

        /**
         * Overrides the Firebase Remote Config key that holds the update JSON blob.
         *
         * Default: `"auk_update_config"`. The host app must ensure this key is fetched
         * and activated before calling [AppUpdateKit.checkForUpdate].
         */
        fun setRemoteConfigKey(key: String): Builder = apply {
            this.remoteConfigKey = key.trim()
        }

        /**
         * Sets the maximum time (seconds) for any async operations.
         * Defaults to **10 seconds**.
         */
        fun setFetchTimeoutSeconds(seconds: Long): Builder = apply {
            this.fetchTimeoutSeconds = seconds.coerceAtLeast(1L)
        }

        /**
         * Customizes the appearance of the force update screen.
         *
         * @see ForceUpdateScreenConfig
         */
        fun setForceUpdateScreenConfig(config: ForceUpdateScreenConfig): Builder = apply {
            this.forceUpdateScreenConfig = config
        }

        /**
         * Customizes the appearance of the maintenance screen.
         *
         * @see MaintenanceScreenConfig
         */
        fun setMaintenanceScreenConfig(config: MaintenanceScreenConfig): Builder = apply {
            this.maintenanceScreenConfig = config
        }

        /**
         * Enables verbose diagnostic logging under the `AppUpdateKit` tag.
         * Pass `BuildConfig.DEBUG` so logs only appear in debug builds.
         */
        fun enableLogging(enable: Boolean): Builder = apply {
            this.loggingEnabled = enable
        }

        /** Builds and returns the [AppUpdateKit] instance. */
        fun build(): AppUpdateKit = AppUpdateKit(
            activity = activity,
            callback = callback,
            remoteConfigKey = remoteConfigKey,
            fetchTimeoutSeconds = fetchTimeoutSeconds,
            forceUpdateScreenConfig = forceUpdateScreenConfig,
            maintenanceScreenConfig = maintenanceScreenConfig,
            loggingEnabled = loggingEnabled
        )
    }
}
