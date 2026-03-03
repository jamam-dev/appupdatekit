/**
 * AppUpdateKit
 * UpdateType.kt
 * Purpose: Enum representing the type of in-app update to perform.
 */
package com.appupdatekit

/**
 * Represents the type of in-app update flow to launch.
 *
 * - [IMMEDIATE]: Blocks the user until the update is downloaded and installed.
 *   Best for critical/security updates.
 * - [FLEXIBLE]: Runs in the background; user can continue using the app.
 *   A callback ([UpdateCallback.onUpdateDownloaded]) fires when the download
 *   completes so the app can prompt the user to restart.
 * - [NONE]: No update should be triggered (remote config disabled or no update
 *   available for this version).
 */
enum class UpdateType {
    IMMEDIATE,
    FLEXIBLE,
    NONE
}

