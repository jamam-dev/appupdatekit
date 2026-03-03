/**
 * AppUpdateKit
 * UpdateCallback.kt
 * Purpose: Interface for receiving update lifecycle events in the host application.
 */
package com.appupdatekit

/**
 * Callback interface that lets the host application react to update lifecycle events.
 *
 * All methods are optional — default implementations are no-ops so callers only
 * override the events they care about.
 */
interface UpdateCallback {

    /**
     * Called when an update is available and the update flow has been launched.
     *
     * @param updateType The type of flow that was started ([UpdateType.FLEXIBLE] or
     *                   [UpdateType.IMMEDIATE]).
     */
    fun onUpdateAvailable(updateType: UpdateType) {}

    /**
     * Called when there is no update available for the current device/version.
     * Also called when Remote Config indicates no update is needed.
     */
    fun onUpdateNotAvailable() {}

    /**
     * Called when a **flexible** update has been fully downloaded and is ready to install.
     * The host application should prompt the user to restart (e.g., show a Snackbar).
     * Call [AppUpdateKit.completeFlexibleUpdate] to trigger the restart.
     */
    fun onUpdateDownloaded() {}

    /**
     * Called when the update flow fails for any reason (network error, Play unavailable,
     * user cancellation counted as failure, etc.).
     *
     * @param exception The underlying exception describing the failure.
     */
    fun onUpdateFailed(exception: Exception) {}

    /**
     * Called when the user chooses to snooze (remind later) the update prompt.
     *
     * @param snoozeCount The total number of times the user has snoozed so far.
     */
    fun onUpdateSnoozed(snoozeCount: Int) {}

    /**
     * Called when the update check was skipped because Remote Config has disabled
     * updates (`auk_update_enabled = false`).
     */
    fun onUpdateDisabledByRemoteConfig() {}
}

