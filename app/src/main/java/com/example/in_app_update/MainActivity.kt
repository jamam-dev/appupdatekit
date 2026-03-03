/**
 * AppUpdateKit
 * MainActivity.kt (sample app)
 * Purpose: Demonstrates both integration styles for AppUpdateKit.
 */
package com.example.in_app_update

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.appupdatekit.AppUpdateKit
import com.appupdatekit.UpdateCallback
import com.appupdatekit.UpdateType
import com.appupdatekit.extensions.checkAppUpdate
import com.google.android.material.snackbar.Snackbar

/**
 * Sample activity demonstrating two AppUpdateKit integration styles:
 *
 * **Style 1 — Verbose (explicit Builder)**: Full control via [AppUpdateKit.Builder].
 * **Style 2 — Shorthand (extension function)**: One-liner via [checkAppUpdate].
 *
 * NOTE: This app requires a valid `google-services.json` placed in the `app/` directory.
 *       Download it from your Firebase Console project settings.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AppUpdateKit-Sample"
    }

    // ─── Style 1 — Verbose Builder ────────────────────────────────────────────

    /**
     * Explicit builder with a full [UpdateCallback] implementation.
     * Uncomment this block and comment out the Style 2 block to use.
     */
    private val appUpdateKitVerbose: AppUpdateKit by lazy {
        AppUpdateKit.with(this)
            .setCallback(object : UpdateCallback {

                override fun onUpdateAvailable(updateType: UpdateType) {
                    log("Update available — type: $updateType")
                    showToast("Update available ($updateType)")
                }

                override fun onUpdateNotAvailable() {
                    log("No update available.")
                }

                override fun onUpdateDownloaded() {
                    log("Flexible update downloaded — showing install Snackbar.")
                    showInstallSnackbar()
                }

                override fun onUpdateFailed(exception: Exception) {
                    Log.e(TAG, "Update failed: ${exception.message}", exception)
                    showToast("Update failed: ${exception.message}")
                }

                override fun onUpdateSnoozed(snoozeCount: Int) {
                    log("Update snoozed. Total snooze count: $snoozeCount")
                }

                override fun onUpdateDisabledByRemoteConfig() {
                    log("Updates disabled by Remote Config.")
                }
            })
            .setFetchTimeoutSeconds(10L)
            .enableLogging(BuildConfig.DEBUG)
            .build()
    }

    // ─── Style 2 — Extension Shorthand ───────────────────────────────────────

    /**
     * One-liner integration via the [checkAppUpdate] extension function.
     * Sufficient for most use cases where default callbacks are acceptable.
     */
    private val appUpdateKitShorthand: AppUpdateKit by lazy {
        checkAppUpdate(
            callback = object : UpdateCallback {
                override fun onUpdateDownloaded() { showInstallSnackbar() }
                override fun onUpdateFailed(exception: Exception) {
                    Log.e(TAG, "Update failed: ${exception.message}", exception)
                }
            }
        ) {
            enableLogging(BuildConfig.DEBUG)
        }
    }

    // ─── Active instance ──────────────────────────────────────────────────────

    /** Switch between verbose and shorthand by changing this reference. */
    private val appUpdateKit: AppUpdateKit get() = appUpdateKitVerbose

    // ─── Activity lifecycle ───────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Start the update check.
        appUpdateKit.checkForUpdate()
    }

    /**
     * Forward to AppUpdateKit so it can handle IMMEDIATE update results.
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        appUpdateKit.onActivityResult(requestCode, resultCode)
    }

    /**
     * Forward to AppUpdateKit so it can detect flexible updates downloaded in background.
     */
    override fun onResume() {
        super.onResume()
        appUpdateKit.onResume()
    }

    /**
     * Forward to AppUpdateKit to release resources and cancel coroutines.
     */
    override fun onDestroy() {
        super.onDestroy()
        appUpdateKit.onDestroy()
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /** Shows a Snackbar prompting the user to install the downloaded flexible update. */
    private fun showInstallSnackbar() {
        Snackbar.make(
            findViewById(R.id.main),
            "A new update is ready to install.",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("INSTALL") {
            appUpdateKit.completeFlexibleUpdate()
            appUpdateKit.resetSnooze()
        }.show()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun log(message: String) {
        Log.d(TAG, message)
    }
}