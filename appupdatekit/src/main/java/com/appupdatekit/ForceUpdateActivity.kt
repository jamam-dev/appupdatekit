/**
 * AppUpdateKit
 * ForceUpdateActivity.kt
 * Purpose: Fully blocking screen shown when a force update is required.
 *          Tries Play In-App Update IMMEDIATE flow first; falls back to Play Store URL.
 */
package com.appupdatekit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * A fully blocking [AppCompatActivity] that forces the user to update the app.
 *
 * - Back press is disabled.
 * - Tapping "Update Now" attempts a Play In-App Update IMMEDIATE flow.
 * - If the IMMEDIATE flow is unavailable (sideloaded APK, Play not present, etc.)
 *   the user is redirected to the Play Store via the store URL from Remote Config.
 *
 * Started internally by [UpdateManager]. Do **not** start this Activity directly.
 */
internal class ForceUpdateActivity : AppCompatActivity() {

    // ─── Constants ────────────────────────────────────────────────────────────

    internal companion object {
        const val TAG = "AppUpdateKit"
        const val REQUEST_CODE_UPDATE = 7358

        const val EXTRA_MESSAGE          = "auk_extra_force_message"
        const val EXTRA_BUTTON_TEXT      = "auk_extra_force_btn"
        const val EXTRA_STORE_URL        = "auk_extra_store_url"
        const val EXTRA_BG_COLOR         = "auk_extra_force_bg_color"
        const val EXTRA_TITLE_COLOR      = "auk_extra_force_title_color"
        const val EXTRA_MSG_COLOR        = "auk_extra_force_msg_color"
        const val EXTRA_BTN_COLOR        = "auk_extra_force_btn_color"
        const val EXTRA_BTN_TEXT_COLOR   = "auk_extra_force_btn_txt_color"
        const val EXTRA_ILLUSTRATION_RES = "auk_extra_force_illustration"
        const val EXTRA_FONT_RES         = "auk_extra_force_font"
        const val EXTRA_LOGGING          = "auk_extra_force_logging"

        /** Builds the intent that launches this activity. */
        fun buildIntent(
            activity: android.app.Activity,
            config: UpdateConfig,
            screenConfig: ForceUpdateScreenConfig,
            loggingEnabled: Boolean
        ): Intent = Intent(activity, ForceUpdateActivity::class.java).apply {
            putExtra(EXTRA_MESSAGE,          config.forceUpdateMessage)
            putExtra(EXTRA_BUTTON_TEXT,      config.forceUpdateButtonText)
            putExtra(EXTRA_STORE_URL,        config.storeUrl)
            putExtra(EXTRA_LOGGING,          loggingEnabled)
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
    private lateinit var btnUpdate: Button

    private var loggingEnabled = false

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auk_force_update)
        loggingEnabled = intent.getBooleanExtra(EXTRA_LOGGING, false)

        bindViews()
        applyContent()
        applyCustomization()

        btnUpdate.setOnClickListener { launchUpdate() }
    }

    /** Fully block back press — the user MUST update. */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Intentionally empty — back is disabled on this screen.
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_UPDATE && resultCode != RESULT_OK) {
            // User cancelled or update failed — stay on this screen.
            log("IMMEDIATE update flow returned resultCode=$resultCode — staying on force update screen.")
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun bindViews() {
        root         = findViewById(R.id.auk_fu_root)
        illustration = findViewById(R.id.auk_fu_illustration)
        title        = findViewById(R.id.auk_fu_title)
        message      = findViewById(R.id.auk_fu_message)
        btnUpdate    = findViewById(R.id.auk_fu_btn_update)
    }

    private fun applyContent() {
        message.text   = intent.getStringExtra(EXTRA_MESSAGE)   ?: UpdateConfig.Defaults.FORCE_UPDATE_MESSAGE
        btnUpdate.text = intent.getStringExtra(EXTRA_BUTTON_TEXT) ?: UpdateConfig.Defaults.FORCE_UPDATE_BUTTON_TEXT
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
            btnUpdate.backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(it))
        }
        extras.getInt(EXTRA_BTN_TEXT_COLOR, 0).takeIf { it != 0 }?.let {
            btnUpdate.setTextColor(getColor(it))
        }
        extras.getInt(EXTRA_ILLUSTRATION_RES, 0).takeIf { it != 0 }?.let {
            illustration.setImageResource(it)
        }
        extras.getInt(EXTRA_FONT_RES, 0).takeIf { it != 0 }?.let { fontRes ->
            try {
                val typeface = ResourcesCompat.getFont(this, fontRes)
                title.typeface   = typeface
                message.typeface = typeface
                btnUpdate.typeface = typeface
            } catch (e: Exception) {
                Log.w(TAG, "AppUpdateKit: Could not load font resource $fontRes — ${e.message}")
            }
        }
    }

    private fun launchUpdate() {
        log("Force update button tapped — attempting IMMEDIATE Play flow.")
        val appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        this,
                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                        REQUEST_CODE_UPDATE
                    )
                    log("IMMEDIATE Play flow started.")
                } catch (e: Exception) {
                    Log.w(TAG, "AppUpdateKit: IMMEDIATE flow failed — falling back to Play Store. ${e.message}")
                    openPlayStore()
                }
            } else {
                log("IMMEDIATE flow not available — opening Play Store.")
                openPlayStore()
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "AppUpdateKit: getAppUpdateInfo failed — opening Play Store. ${e.message}")
            openPlayStore()
        }
    }

    private fun openPlayStore() {
        val storeUrl = intent.getStringExtra(EXTRA_STORE_URL).takeIf { !it.isNullOrBlank() }
            ?: "https://play.google.com/store/apps/details?id=$packageName"
        log("Opening Play Store: $storeUrl")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl)))
        } catch (e: Exception) {
            Log.e(TAG, "AppUpdateKit: Could not open Play Store URL: $storeUrl", e)
        }
    }

    private fun log(message: String) {
        if (loggingEnabled) Log.d(TAG, message)
    }
}

