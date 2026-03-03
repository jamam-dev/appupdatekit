/**
 * AppUpdateKit
 * MaintenanceActivity.kt
 * Purpose: Fully blocking screen shown during app maintenance.
 *          "Try Again" re-reads Remote Config; dismisses if maintenance is cleared.
 */
package com.appupdatekit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat

/**
 * A fully blocking [AppCompatActivity] shown when Remote Config signals that the app is
 * under maintenance (`maintenance_enabled = true`).
 *
 * - Back press is disabled.
 * - "Try Again" re-reads the **already-active** Remote Config value and dismisses the
 *   screen if maintenance has been turned off. No new network fetch is performed here —
 *   the host app controls when RC is refreshed.
 *
 * Started internally by [UpdateManager]. Do **not** start this Activity directly.
 */
internal class MaintenanceActivity : AppCompatActivity() {

    // ─── Constants ────────────────────────────────────────────────────────────

    internal companion object {
        const val TAG = "AppUpdateKit"

        const val EXTRA_MESSAGE          = "auk_extra_mt_message"
        const val EXTRA_BUTTON_TEXT      = "auk_extra_mt_btn"
        const val EXTRA_RC_KEY           = "auk_extra_mt_rc_key"
        const val EXTRA_BG_COLOR         = "auk_extra_mt_bg_color"
        const val EXTRA_TITLE_COLOR      = "auk_extra_mt_title_color"
        const val EXTRA_MSG_COLOR        = "auk_extra_mt_msg_color"
        const val EXTRA_BTN_COLOR        = "auk_extra_mt_btn_color"
        const val EXTRA_BTN_TEXT_COLOR   = "auk_extra_mt_btn_txt_color"
        const val EXTRA_ILLUSTRATION_RES = "auk_extra_mt_illustration"
        const val EXTRA_FONT_RES         = "auk_extra_mt_font"
        const val EXTRA_LOGGING          = "auk_extra_mt_logging"

        /** Builds the intent that launches this activity. */
        fun buildIntent(
            activity: android.app.Activity,
            config: UpdateConfig,
            screenConfig: MaintenanceScreenConfig,
            remoteConfigKey: String,
            loggingEnabled: Boolean
        ): Intent = Intent(activity, MaintenanceActivity::class.java).apply {
            putExtra(EXTRA_MESSAGE,     config.maintenanceMessage)
            putExtra(EXTRA_BUTTON_TEXT, screenConfig.tryAgainButtonText ?: "Try Again")
            putExtra(EXTRA_RC_KEY,      remoteConfigKey)
            putExtra(EXTRA_LOGGING,     loggingEnabled)
            screenConfig.backgroundColorRes?.let  { putExtra(EXTRA_BG_COLOR, it) }
            screenConfig.titleColorRes?.let       { putExtra(EXTRA_TITLE_COLOR, it) }
            screenConfig.messageColorRes?.let     { putExtra(EXTRA_MSG_COLOR, it) }
            screenConfig.buttonColorRes?.let      { putExtra(EXTRA_BTN_COLOR, it) }
            screenConfig.buttonTextColorRes?.let  { putExtra(EXTRA_BTN_TEXT_COLOR, it) }
            screenConfig.illustrationRes?.let     { putExtra(EXTRA_ILLUSTRATION_RES, it) }
            screenConfig.fontRes?.let             { putExtra(EXTRA_FONT_RES, it) }
        }
    }

    // ─── Views ────────────────────────────────────────────────────────────────

    private lateinit var root: android.view.View
    private lateinit var illustration: ImageView
    private lateinit var title: TextView
    private lateinit var message: TextView
    private lateinit var btnRetry: Button

    private var loggingEnabled = false
    private var remoteConfigKey = UpdateConfig.DEFAULT_REMOTE_CONFIG_KEY

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auk_maintenance)

        loggingEnabled  = intent.getBooleanExtra(EXTRA_LOGGING, false)
        remoteConfigKey = intent.getStringExtra(EXTRA_RC_KEY) ?: UpdateConfig.DEFAULT_REMOTE_CONFIG_KEY

        bindViews()
        applyContent()
        applyCustomization()

        btnRetry.setOnClickListener { checkMaintenanceStatus() }
    }

    /** Fully block back press — the user cannot bypass the maintenance screen. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Intentionally empty — back is disabled on this screen.
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun bindViews() {
        root         = findViewById(R.id.auk_mt_root)
        illustration = findViewById(R.id.auk_mt_illustration)
        title        = findViewById(R.id.auk_mt_title)
        message      = findViewById(R.id.auk_mt_message)
        btnRetry     = findViewById(R.id.auk_mt_btn_retry)
    }

    private fun applyContent() {
        message.text  = intent.getStringExtra(EXTRA_MESSAGE)     ?: UpdateConfig.Defaults.MAINTENANCE_MESSAGE
        btnRetry.text = intent.getStringExtra(EXTRA_BUTTON_TEXT) ?: "Try Again"
    }

    private fun applyCustomization() {
        val extras = intent.extras ?: return

        extras.getInt(EXTRA_BG_COLOR, 0).takeIf { it != 0 }?.let {
            root.setBackgroundColor(getColor(it))
        }
        extras.getInt(EXTRA_TITLE_COLOR, 0).takeIf { it != 0 }?.let {
            title.setTextColor(getColor(it))
        }
        extras.getInt(EXTRA_MSG_COLOR, 0).takeIf { it != 0 }?.let {
            message.setTextColor(getColor(it))
        }
        extras.getInt(EXTRA_BTN_COLOR, 0).takeIf { it != 0 }?.let {
            btnRetry.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(it))
        }
        extras.getInt(EXTRA_BTN_TEXT_COLOR, 0).takeIf { it != 0 }?.let {
            btnRetry.setTextColor(getColor(it))
        }
        extras.getInt(EXTRA_ILLUSTRATION_RES, 0).takeIf { it != 0 }?.let {
            illustration.setImageResource(it)
        }
        extras.getInt(EXTRA_FONT_RES, 0).takeIf { it != 0 }?.let { fontRes ->
            try {
                val typeface = ResourcesCompat.getFont(this, fontRes)
                title.typeface   = typeface
                message.typeface = typeface
                btnRetry.typeface = typeface
            } catch (e: Exception) {
                Log.w(TAG, "AppUpdateKit: Could not load font resource $fontRes — ${e.message}")
            }
        }
    }

    /**
     * Re-reads the already-active Remote Config value.
     * If [UpdateConfig.maintenanceEnabled] is now `false`, finish this activity
     * so the user can continue using the app.
     *
     * No network request is made here — the host app controls Remote Config refresh cycles.
     */
    private fun checkMaintenanceStatus() {
        log("Try Again tapped — re-reading Remote Config key '$remoteConfigKey'.")
        btnRetry.isEnabled = false
        btnRetry.text = "Checking…"

        val parser = UpdateConfigParser(
            remoteConfigKey = remoteConfigKey,
            loggingEnabled = loggingEnabled
        )
        val config = parser.parse()

        if (!config.maintenanceEnabled) {
            log("Maintenance cleared — finishing MaintenanceActivity.")
            finish()
        } else {
            log("Maintenance still active.")
            // Update message in case it changed in RC.
            message.text = config.maintenanceMessage
            btnRetry.isEnabled = true
            btnRetry.text = intent.getStringExtra(EXTRA_BUTTON_TEXT) ?: "Try Again"
        }
    }

    private fun log(message: String) {
        if (loggingEnabled) Log.d(TAG, message)
    }
}

