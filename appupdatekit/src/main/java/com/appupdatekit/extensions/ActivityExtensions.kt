/**
 * AppUpdateKit
 * ActivityExtensions.kt
 * Purpose: Extension functions for Activity to simplify AppUpdateKit integration.
 */
package com.appupdatekit.extensions

import android.app.Activity
import com.appupdatekit.AppUpdateKit
import com.appupdatekit.UpdateCallback

/**
 * Convenience extension function that creates and returns an [AppUpdateKit] instance
 * from any [Activity] with an optional builder configuration block.
 *
 * ## Example
 *
 * ### No callback (fire-and-forget):
 * ```kotlin
 * val kit = checkAppUpdate { enableLogging(BuildConfig.DEBUG) }
 * kit.checkForUpdate()
 * ```
 *
 * ### With callback:
 * ```kotlin
 * val kit = checkAppUpdate(
 *     callback = object : UpdateCallback {
 *         override fun onUpdateDownloaded() { showSnackbar() }
 *     }
 * ) {
 *     enableLogging(BuildConfig.DEBUG)
 * }
 * kit.checkForUpdate()
 * ```
 *
 * @param callback Optional [UpdateCallback] for update lifecycle events.
 * @param block    Optional DSL-style configuration block applied to [AppUpdateKit.Builder].
 * @return A fully configured [AppUpdateKit] instance (update check NOT started yet).
 */
fun Activity.checkAppUpdate(
    callback: UpdateCallback? = null,
    block: (AppUpdateKit.Builder.() -> Unit)? = null
): AppUpdateKit {
    val builder = AppUpdateKit.Builder(this)
    callback?.let { builder.setCallback(it) }
    block?.let { builder.block() }
    return builder.build()
}
