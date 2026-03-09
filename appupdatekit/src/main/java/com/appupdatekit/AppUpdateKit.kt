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
 * Reads update policy from either:
 * - A **Firebase Remote Config JSON key** (default) — the host app is responsible for
 *   calling `fetchAndActivate()` before invoking this library.
 * - A **raw JSON string** supplied directly — useful when your app fetches config from
 *   its own server or any other source.
 *
 * Supports:
 * - **Force update screen** — fully blocking, tries Play IMMEDIATE flow, falls back to Play Store.
 * - **Maintenance screen** — fully blocking, with "Try Again" button.
 * - **Flexible update** — Play In-App Update background download via [UpdateFlowHandler].
 * - **Snooze** — configurable duration and max count (flexible only).
 *
 * ## Usage — Remote Config (default):
 * ```kotlin
 * val kit = AppUpdateKit.with(this)
 *     .setRemoteConfigKey("auk_update_config")   // optional, this is the default
 *     .setCallback(object : UpdateCallback {
 *         override fun onUpdateDownloaded() { showSnackbar() }
 *     })
 *     .build()
 * kit.checkForUpdate()
 * ```
 *
 * ## Usage — Raw JSON from your own server:
 * ```kotlin
 * val kit = AppUpdateKit.with(this)
 *     .setJsonConfig(myServerJson)   // pass the JSON string directly
 *     .setCallback(object : UpdateCallback {
 *         override fun onUpdateDownloaded() { showSnackbar() }
 *     })
 *     .build()
 * kit.checkForUpdate()
 * ```
 *
 * > **Note:** `setRemoteConfigKey` and `setJsonConfig` are mutually exclusive — the last
 * > call on the builder wins.
 */
class AppUpdateKit private constructor(
    activity: Activity,
    private val callback: UpdateCallback,
    private val configSource: UpdateConfigSource,
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
        configSource = configSource,
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
        private var configSource: UpdateConfigSource      = UpdateConfigSource.RemoteConfig()
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
         * Reads the update policy JSON from a **Firebase Remote Config** key.
         *
         * The host app must call `fetchAndActivate()` before [AppUpdateKit.checkForUpdate].
         * Default key: `"auk_update_config"`.
         *
         * Mutually exclusive with [setJsonConfig] — last call wins.
         */
        fun setRemoteConfigKey(key: String): Builder = apply {
            this.configSource = UpdateConfigSource.RemoteConfig(key.trim())
        }

        /**
         * Supplies the update policy as a **raw JSON string**.
         *
         * Use this when your app fetches config from its own server, a local asset,
         * or any source other than Firebase Remote Config.
         *
         * The JSON must conform to the schema documented in [UpdateConfig].
         * An empty string falls back to [UpdateConfig.Defaults] gracefully.
         *
         * Mutually exclusive with [setRemoteConfigKey] — last call wins.
         *
         * @param json The raw JSON string to parse.
         */
        fun setJsonConfig(json: String): Builder = apply {
            this.configSource = UpdateConfigSource.RawJson(json)
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
            configSource = configSource,
            fetchTimeoutSeconds = fetchTimeoutSeconds,
            forceUpdateScreenConfig = forceUpdateScreenConfig,
            maintenanceScreenConfig = maintenanceScreenConfig,
            loggingEnabled = loggingEnabled
        )
    }
}
