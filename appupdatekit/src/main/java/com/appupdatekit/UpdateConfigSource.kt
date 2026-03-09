/**
 * AppUpdateKit
 * UpdateConfigSource.kt
 * Purpose: Sealed interface representing where the update config JSON comes from.
 */
package com.appupdatekit

/**
 * Describes the source from which AppUpdateKit will read the update policy JSON.
 *
 * Use [RemoteConfig] when Firebase Remote Config is your config back-end (default).
 * Use [RawJson] when your app fetches the config from its own server, a local
 * asset, or any other source and already holds the JSON string at call time.
 */
sealed interface UpdateConfigSource {

    /**
     * Read the JSON blob from an already-activated Firebase Remote Config instance.
     *
     * The host application **must** call `fetchAndActivate()` before invoking
     * [AppUpdateKit.checkForUpdate]; this library only reads the cached value.
     *
     * @param key The Remote Config key whose value is the update config JSON.
     *            Defaults to [UpdateConfig.DEFAULT_REMOTE_CONFIG_KEY].
     */
    data class RemoteConfig(
        val key: String = UpdateConfig.DEFAULT_REMOTE_CONFIG_KEY
    ) : UpdateConfigSource

    /**
     * Supply the update config JSON string directly.
     *
     * Use this when your app retrieves config from your own API, a local file,
     * or any source that is not Firebase Remote Config.  The string must conform
     * to the same JSON schema documented in [UpdateConfig].
     *
     * An empty or blank string will be treated the same as a missing Remote Config
     * value — all [UpdateConfig.Defaults] are applied and the library degrades
     * gracefully.
     *
     * @param json The raw JSON string to parse.
     */
    data class RawJson(val json: String) : UpdateConfigSource
}

