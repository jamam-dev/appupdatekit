/**
 * AppUpdateKit
 * MainActivity.kt (sample app)
 * Purpose: Demonstrates both integration styles for AppUpdateKit including
 *          force update screen, maintenance screen, and flexible update with snooze.
 */
package com.example.in_app_update

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.appupdatekit.AppUpdateKit
import com.appupdatekit.ForceUpdateScreenConfig
import com.appupdatekit.MaintenanceScreenConfig
import com.appupdatekit.UpdateCallback
import com.appupdatekit.UpdateType
import com.appupdatekit.extensions.checkAppUpdate
import com.google.android.material.snackbar.Snackbar

/**
 * Sample activity demonstrating AppUpdateKit integration.
 *
 * IMPORTANT: Call FirebaseRemoteConfig.getInstance().fetchAndActivate() in your
 * Application class (or early in this Activity) BEFORE calling kit.checkForUpdate().
 *
 * NOTE: Replace app/google-services.json with your real Firebase project file.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AppUpdateKit-Sample"
    }

    // ─── Style 1 — Verbose Builder (full control) ─────────────────────────────

    private val appUpdateKitVerbose: AppUpdateKit by lazy {
        AppUpdateKit.with(this)
            // The RC key whose value is the update config JSON blob.
            // Your app must have already called fetchAndActivate() on FirebaseRemoteConfig.
            .setRemoteConfigKey("auk_update_config")

            .setCallback(object : UpdateCallback {
                override fun onUpdateAvailable(updateType: UpdateType) {
                    log("Update available — type: $updateType")
                    if (updateType == UpdateType.FLEXIBLE) {
                        showToast("Downloading update in background…")
                    }
                    // For IMMEDIATE / force updates the blocking screen is shown automatically.
                }
                override fun onUpdateNotAvailable() {
                    log("App is up to date.")
                }
                override fun onUpdateDownloaded() {
                    log("Flexible update downloaded — showing Snackbar.")
                    showInstallSnackbar()
                }
                override fun onUpdateFailed(exception: Exception) {
                    Log.e(TAG, "Update failed: ${exception.message}", exception)
                }
                override fun onUpdateSnoozed(snoozeCount: Int) {
                    log("Update snoozed. Total snooze count: $snoozeCount")
                }
                override fun onUpdateDisabledByRemoteConfig() {
                    log("Updates disabled by Remote Config.")
                }
            })

            // Optional: customize the force update screen appearance.
            .setForceUpdateScreenConfig(
                ForceUpdateScreenConfig(
                    // Pass your app's color/drawable resource IDs here.
                    // All fields are optional — omit any you don't need to override.
                    // backgroundColorRes = R.color.my_background,
                    // buttonColorRes     = R.color.my_accent,
                    // illustrationRes    = R.drawable.ic_my_update_art,
                    // fontRes            = R.font.my_font
                )
            )

            // Optional: customize the maintenance screen appearance.
            .setMaintenanceScreenConfig(
                MaintenanceScreenConfig(
                    tryAgainButtonText = "Check Again"
                    // backgroundColorRes = R.color.my_background,
                    // illustrationRes    = R.drawable.ic_maintenance_art,
                )
            )

            .setFetchTimeoutSeconds(10L)
            .enableLogging(BuildConfig.DEBUG)
            .build()
    }

    // ─── Style 2 — Extension Shorthand ───────────────────────────────────────

    private val appUpdateKitShorthand: AppUpdateKit by lazy {
        checkAppUpdate(
            callback = object : UpdateCallback {
                override fun onUpdateDownloaded() { showInstallSnackbar() }
                override fun onUpdateFailed(exception: Exception) {
                    Log.e(TAG, "Update failed: ${exception.message}", exception)
                }
            }
        ) {
            setRemoteConfigKey("auk_update_config")
            enableLogging(BuildConfig.DEBUG)
        }
    }

    // Change this to appUpdateKitShorthand to demo Style 2.
    private val appUpdateKit: AppUpdateKit get() = appUpdateKitVerbose

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // NOTE: In a real app, fetch & activate Remote Config in your Application class,
        // then call checkForUpdate() here after activation completes.
        appUpdateKit.checkForUpdate()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        appUpdateKit.onActivityResult(requestCode, resultCode)
    }

    override fun onResume() {
        super.onResume()
        appUpdateKit.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateKit.onDestroy()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun showInstallSnackbar() {
        Snackbar.make(
            findViewById(R.id.main),
            "Update ready to install!",
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction("INSTALL") {
                appUpdateKit.completeFlexibleUpdate()
                appUpdateKit.resetSnooze()
            }
            .show()
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    private fun log(msg: String) = Log.d(TAG, msg)
}