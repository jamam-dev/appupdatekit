# AppUpdateKit

[![](https://jitpack.io/v/YOUR_USERNAME/AppUpdateKit.svg)](https://jitpack.io/#YOUR_USERNAME/AppUpdateKit)
![minSdk](https://img.shields.io/badge/minSdk-24-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)

**AppUpdateKit** is a production-grade Android in-app update library written entirely in Kotlin.
It reads a single **Firebase Remote Config JSON key** (your app owns the fetch cycle), and provides:

- 🚨 **Force Update screen** — fully blocking, tries Play IMMEDIATE flow, falls back to Play Store
- 🔧 **Maintenance screen** — fully blocking with "Try Again" button
- 🔄 **Flexible update** — Play In-App Update background download
- 😴 **Snooze** — configurable duration & max count (flexible only)
- 🎨 **Customizable screens** — colors, fonts, illustrations via Builder

---

## ✨ Features

| Feature | Details |
|---|---|
| Remote Config ownership | Host app fetches/activates; library reads the cached JSON value |
| Force update screen | Blocking Activity, tries Play IMMEDIATE flow first, falls back to Play Store URL |
| Maintenance screen | Blocking Activity, "Try Again" re-reads RC without network call |
| Flexible update | Play In-App Update background download + install Snackbar |
| Snooze | SharedPrefs-backed, configurable duration & max count |
| Screen customization | Background, title/message colors, button color/text, illustration, font |
| No crashes | All edge cases handled — Play unavailable, RC empty, Activity null |
| No memory leaks | `WeakReference<Activity>` throughout |
| Logging | Gated behind `enableLogging(BuildConfig.DEBUG)` |
| ProGuard | Rules auto-applied via `consumer-rules.pro` |

---

## 🚀 Installation

### Step 1 — Add JitPack to `settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2 — Add the dependency

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.YOUR_USERNAME:appupdatekit:1.0.0")
}
```

Replace `YOUR_USERNAME` with your GitHub username and `1.0.0` with the release tag.

---

## 🏗️ Publishing to JitPack

### First time setup

1. **Push** the repo to GitHub (public repository).
2. **Replace** `YOUR_USERNAME` in `gradle.properties` with your real GitHub username:
   ```properties
   GROUP=com.github.yourusername
   ARTIFACT_ID=appupdatekit
   VERSION_NAME=1.0.0
   ```
3. **Create a release tag** and push it:
   ```bash
   git add .
   git commit -m "Release 1.0.0"
   git tag v1.0.0
   git push origin main --tags
   ```
4. **Trigger JitPack** — visit:
   ```
   https://jitpack.io/#YOUR_USERNAME/AppUpdateKit/v1.0.0
   ```
   Click **"Get it"** to trigger the first build. JitPack will run:
   ```bash
   ./gradlew :appupdatekit:publishReleasePublicationToMavenLocal
   ```
5. Once the build shows ✅ green, your library is live.

### How it works

- `jitpack.yml` specifies **JDK 17** and runs `publishReleasePublicationToMavenLocal`.
- The `maven-publish` block in `appupdatekit/build.gradle.kts` generates the POM + AAR.
- JitPack finds the artifacts at `~/.m2/repository/com/github/YOUR_USERNAME/appupdatekit/`.

---

## 🔥 Firebase Remote Config Setup

### 1. Your app owns the fetch cycle

AppUpdateKit does **not** call `fetchAndActivate()`. You do it in your `Application` class
(or wherever you currently fetch RC), then call `kit.checkForUpdate()` afterwards:

```kotlin
// In your Application class or main Activity
FirebaseRemoteConfig.getInstance()
    .fetchAndActivate()
    .addOnCompleteListener {
        appUpdateKit.checkForUpdate()
    }
```

### 2. Create the Remote Config parameter

In Firebase Console → **Remote Config**, create one parameter:

| Key | Type | Value |
|---|---|---|
| `auk_update_config` | JSON | *(see schema below)* |

You can use any key name — just pass it to `.setRemoteConfigKey("your_key")`.

### 3. JSON schema

```json
{
  "update_enabled": true,
  "force_update": false,
  "minimum_version_code": 10,
  "snooze_duration_hours": 24,
  "snooze_max_count": 3,
  "maintenance_enabled": false,
  "maintenance_message": "We are currently under maintenance. Please check back soon.",
  "force_update_message": "A critical update is required to continue using this app.",
  "force_update_button_text": "Update Now",
  "flexible_update_message": "A new version is available.",
  "flexible_update_button_text": "Update",
  "flexible_dismiss_button_text": "Later",
  "store_url": "https://play.google.com/store/apps/details?id=com.your.app"
}
```

### JSON field reference

| Field | Type | Default | Description |
|---|---|---|---|
| `update_enabled` | boolean | `true` | Master kill-switch — `false` disables everything |
| `force_update` | boolean | `false` | `true` = show force update screen (blocking) |
| `minimum_version_code` | number | `0` | Min version code; `0` = any update shown |
| `snooze_duration_hours` | number | `24` | Hours before flexible prompt re-appears |
| `snooze_max_count` | number | `3` | Max snooze count; `0` = unlimited |
| `maintenance_enabled` | boolean | `false` | `true` = show maintenance screen (blocking) |
| `maintenance_message` | string | *(see above)* | Message on maintenance screen |
| `force_update_message` | string | *(see above)* | Message on force update screen |
| `force_update_button_text` | string | `"Update Now"` | Force update button label |
| `flexible_update_message` | string | `"A new version is available."` | Flexible prompt message |
| `flexible_update_button_text` | string | `"Update"` | Flexible prompt confirm button |
| `flexible_dismiss_button_text` | string | `"Later"` | Flexible prompt dismiss button |
| `store_url` | string | `""` | Play Store fallback URL (auto-built from package name if empty) |

> **Priority:** `maintenance_enabled` is checked first. If true, no update check is performed.

---

## 🔧 Integration

### Style 1 — Verbose Builder

```kotlin
class MainActivity : AppCompatActivity() {

    private val appUpdateKit: AppUpdateKit by lazy {
        AppUpdateKit.with(this)
            .setRemoteConfigKey("auk_update_config")   // default — can omit
            .setCallback(object : UpdateCallback {
                override fun onUpdateAvailable(updateType: UpdateType) {
                    // FLEXIBLE: download started in background
                    // IMMEDIATE: force update screen shown automatically
                }
                override fun onUpdateNotAvailable() { /* up to date */ }
                override fun onUpdateDownloaded() {
                    // FLEXIBLE only — show Snackbar
                    Snackbar.make(binding.root, "Update ready!", Snackbar.LENGTH_INDEFINITE)
                        .setAction("INSTALL") { appUpdateKit.completeFlexibleUpdate() }
                        .show()
                }
                override fun onUpdateFailed(exception: Exception) {
                    Log.e(TAG, "Update failed", exception)
                }
                override fun onUpdateSnoozed(snoozeCount: Int) { /* user tapped Later */ }
                override fun onUpdateDisabledByRemoteConfig() { /* RC kill-switch off */ }
            })
            // Optional screen customizations:
            .setForceUpdateScreenConfig(
                ForceUpdateScreenConfig(
                    backgroundColorRes = R.color.my_bg,
                    buttonColorRes     = R.color.my_accent,
                    illustrationRes    = R.drawable.ic_update_art,
                    fontRes            = R.font.my_font
                )
            )
            .setMaintenanceScreenConfig(
                MaintenanceScreenConfig(
                    illustrationRes    = R.drawable.ic_maintenance_art,
                    tryAgainButtonText = "Check Again",
                    buttonColorRes     = R.color.my_accent
                )
            )
            .enableLogging(BuildConfig.DEBUG)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Assumes RC was already fetched in Application class.
        appUpdateKit.checkForUpdate()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        appUpdateKit.onActivityResult(requestCode, resultCode)
    }

    override fun onResume() {
        super.onResume()
        appUpdateKit.onResume()          // detects flexible updates downloaded in background
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateKit.onDestroy()         // cancels coroutines, unregisters listeners
    }
}
```

### Style 2 — Extension shorthand

```kotlin
val kit = checkAppUpdate(
    callback = object : UpdateCallback {
        override fun onUpdateDownloaded() { showInstallSnackbar() }
    }
) {
    setRemoteConfigKey("auk_update_config")
    enableLogging(BuildConfig.DEBUG)
}
kit.checkForUpdate()
```

---

## 🚨 Force Update Screen

When `force_update = true` in the JSON:

1. AppUpdateKit starts `ForceUpdateActivity` (declared in library manifest — no host action needed).
2. The screen is **fully blocking** — back press is disabled.
3. Tapping the button:
   - Tries **Play In-App Update IMMEDIATE** flow if available.
   - Falls back to opening `store_url` in the browser if Play flow is unavailable (sideloaded builds, emulators, etc.).
4. **Snooze does not apply** to force updates.

---

## 🔧 Maintenance Screen

When `maintenance_enabled = true` in the JSON:

1. AppUpdateKit starts `MaintenanceActivity` — checked **before** any update logic.
2. Fully blocking — back press is disabled.
3. "Try Again" re-reads the **already-active** Remote Config value (no network call).
   - If `maintenance_enabled` is now `false` → activity finishes, user continues.
   - If still `true` → message refreshes, button re-enables.
4. To actually clear maintenance for users, publish a new RC value and let your app's normal RC fetch cycle pick it up.

---

## 😴 Snooze Behaviour (Flexible only)

1. Call `appUpdateKit.snooze()` from your UI ("Not now" / "Later" button).
2. AppUpdateKit saves timestamp + increments counter in `SharedPreferences` (`auk_snooze_prefs`).
3. Next `checkForUpdate()` call:
   - Duration not elapsed → prompt suppressed.
   - Count ≥ `snooze_max_count` (and > 0) → prompt permanently suppressed.
4. Call `appUpdateKit.resetSnooze()` after a successful install.

---

## 🎨 Screen Customization

Both screens accept an optional config data class via the Builder. All fields default to `null` (use library defaults).

### `ForceUpdateScreenConfig`

| Property | Type | Description |
|---|---|---|
| `backgroundColorRes` | `@ColorRes Int?` | Screen background |
| `titleColorRes` | `@ColorRes Int?` | Title text color |
| `messageColorRes` | `@ColorRes Int?` | Message body color |
| `buttonColorRes` | `@ColorRes Int?` | Button background color |
| `buttonTextColorRes` | `@ColorRes Int?` | Button text color |
| `illustrationRes` | `@DrawableRes Int?` | Illustration/icon above title |
| `fontRes` | `@FontRes Int?` | Font applied to all text |

### `MaintenanceScreenConfig`

Same fields as above, plus:

| Property | Type | Description |
|---|---|---|
| `tryAgainButtonText` | `String?` | Label for "Try Again" button |

---

## 📋 UpdateCallback Reference

| Method | When it fires |
|---|---|
| `onUpdateAvailable(updateType)` | Update flow launched (`FLEXIBLE` or `IMMEDIATE`) |
| `onUpdateNotAvailable()` | No update / version below minimum |
| `onUpdateDownloaded()` | FLEXIBLE download complete — show install prompt |
| `onUpdateFailed(exception)` | Any error during update flow |
| `onUpdateSnoozed(snoozeCount)` | User snoozed (call `kit.snooze()` from your UI) |
| `onUpdateDisabledByRemoteConfig()` | `update_enabled = false` in JSON |

All methods have **default no-op** implementations.

---

## 🔒 ProGuard

No manual rules needed. `consumer-rules.pro` (bundled in the AAR) automatically keeps:
- `com.google.android.play.core.appupdate.**`
- `com.google.firebase.remoteconfig.**`
- All public AppUpdateKit classes

---

## 🏗️ JitPack Distribution

1. Push repo to GitHub.
2. Tag: `git tag v1.0.0 && git push origin v1.0.0`
3. Visit `https://jitpack.io/#YOUR_USERNAME/AppUpdateKit` — auto-built on first request.
4. `jitpack.yml` at repo root ensures JDK 17.

---

## 📄 License

```
MIT License — Copyright (c) 2026 AppUpdateKit Contributors
```
