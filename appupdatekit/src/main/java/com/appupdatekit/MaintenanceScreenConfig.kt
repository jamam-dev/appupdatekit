/**
 * AppUpdateKit
 * MaintenanceScreenConfig.kt
 * Purpose: Customization options for the maintenance blocking screen.
 */
package com.appupdatekit

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes

/**
 * Controls the visual appearance of the maintenance screen.
 *
 * All fields are optional — supply only the ones you want to override.
 * Color/drawable/font values must be valid resource IDs from the **host app's** resources.
 *
 * ### Example:
 * ```kotlin
 * MaintenanceScreenConfig(
 *     backgroundColorRes = R.color.my_background,
 *     buttonColorRes     = R.color.my_accent,
 *     illustrationRes    = R.drawable.ic_maintenance,
 *     tryAgainButtonText = "Retry",
 *     fontRes            = R.font.my_font
 * )
 * ```
 *
 * @property backgroundColorRes      Background color of the screen. `null` = library default (white).
 * @property titleColorRes           Color of the title text. `null` = library default (dark).
 * @property messageColorRes         Color of the message body text. `null` = library default (grey).
 * @property buttonColorRes          Background color of the "Try Again" button. `null` = library default.
 * @property buttonTextColorRes      Text color of the "Try Again" button. `null` = library default (white).
 * @property illustrationRes         Drawable shown above the title. `null` = built-in maintenance icon.
 * @property tryAgainButtonText      Label for the retry button. `null` = "Try Again".
 * @property fontRes                 Font resource applied to all text. `null` = system default.
 */
data class MaintenanceScreenConfig(
    @param:ColorRes    val backgroundColorRes: Int?  = null,
    @param:ColorRes    val titleColorRes: Int?       = null,
    @param:ColorRes    val messageColorRes: Int?     = null,
    @param:ColorRes    val buttonColorRes: Int?      = null,
    @param:ColorRes    val buttonTextColorRes: Int?  = null,
    @param:DrawableRes val illustrationRes: Int?     = null,
    val tryAgainButtonText: String?                  = null,
    @param:FontRes     val fontRes: Int?             = null
)
