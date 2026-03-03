# AppUpdateKit

[![](https://jitpack.io/v/YOUR_USERNAME/AppUpdateKit.svg)](https://jitpack.io/#YOUR_USERNAME/AppUpdateKit)
![minSdk](https://img.shields.io/badge/minSdk-24-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)

**AppUpdateKit** is a production-grade, Firebase Remote Config–controlled Android in-app update library written entirely in Kotlin. It wraps Google Play's In-App Updates API (both **Flexible** and **Immediate** flows) behind a clean Builder API with zero-crash guarantees and full snooze management.

---

## ✨ Features

- 🔥 **Firebase Remote Config** — control every update policy remotely, no app release required
- 🔄 **Flexible & Immediate** update flows supported
- 😴 **Snooze / Remind-later** — configurable duration and max snooze count
- 🛡️ **Zero crash guarantee** — all edge cases handled gracefully
- 🔒 **No memory leaks** — `WeakReference<Activity>` used throughout
- 📝 **Full KDoc** on every public API
- 🧹 **ProGuard rules** included via `consumer-rules.pro`
- 🔇 **Logging gated** behind `enableLogging(BuildConfig.DEBUG)`
- ☕ **Kotlin-only**, coroutines-first

---

## 🚀 Installation

### Step 1 — Add JitPack to your `settings.gradle.kts`

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
    implementation("com.github.YOUR_USERNAME:AppUpdateKit:TAG")
}
```

Replace `YOUR_USERNAME` with your GitHub username and `TAG` with the release tag (e.g. `v1.0.0`).

---

## 🔥 Firebase Setup

### 1. Add `google-services.json`

Download your `google-services.json` from the [Firebase Console](https://console.firebase.google.com/) and place it in your `app/` directory.

### 2. Apply the Google Services plugin

```kotlin
// app/build.gradle.kts
plugins {
    id("com.google.gms.google-services")
}
```

### 3. Configure Remote Config keys

In Firebase Console → **Remote Config**, create the following parameters:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `auk_update_enabled` | Boolean | `true` | Master on/off toggle — set to `false` to disable all update prompts instantly |
| `auk_force_update` | Boolean | `false` | `true` = IMMEDIATE flow (blocking), `false` = FLEXIBLE flow (background download) |
| `auk_minimum_version_code` | Number | `0` | Min version code required; `0` means any update available on Play is shown |
| `auk_snooze_duration_hours` | Number | `24` | Hours before the prompt re-appears after the user taps "Remind later" |
| `auk_snooze_max_count` | Number | `3` | Max times a user can snooze; `0` = unlimited snoozes |

> All keys are prefixed with `auk_` to avoid conflicts with your app's own Remote Config keys.

---

## 🔧 Integration

### Style 1 — Verbose Builder (full control)

```kotlin
class MainActivity : AppCompatActivity() {

    private val appUpdateKit: AppUpdateKit by lazy {
        AppUpdateKit.with(this)
            .setCallback(object : UpdateCallback {
                override fun onUpdateAvailable(updateType: UpdateType) {
                    Log.d(TAG, "Update available: $updateType")
                }
                override fun onUpdateNotAvailable() {
                    Log.d(TAG, "App is up to date.")
                }
                override fun onUpdateDownloaded() {
                    // Show a Snackbar prompting the user to restart
                    Snackbar.make(binding.root, "Update ready!", Snackbar.LENGTH_INDEFINITE)
                        .setAction("INSTALL") { appUpdateKit.completeFlexibleUpdate() }
                        .show()
                }
                override fun onUpdateFailed(exception: Exception) {
                    Log.e(TAG, "Update failed", exception)
                }
                override fun onUpdateSnoozed(snoozeCount: Int) {
                    Log.d(TAG, "Update snoozed $snoozeCount time(s).")
                }
                override fun onUpdateDisabledByRemoteConfig() {
                    Log.d(TAG, "Updates disabled remotely.")
                }
            })
            .setFetchTimeoutSeconds(10L)
            .enableLogging(BuildConfig.DEBUG)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appUpdateKit.checkForUpdate()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        appUpdateKit.onActivityResult(requestCode, resultCode)
    }

    override fun onResume() {
        super.onResume()
        appUpdateKit.onResume()   // detects flexible updates downloaded in background
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateKit.onDestroy()  // cancels coroutines, unregisters listeners
    }
}
```

### Style 2 — Extension shorthand (one-liner)

```kotlin
class MainActivity : AppCompatActivity() {

    private val appUpdateKit: AppUpdateKit by lazy {
        checkAppUpdate(
            callback = object : UpdateCallback {
                override fun onUpdateDownloaded() { showInstallSnackbar() }
            }
        ) {
            enableLogging(BuildConfig.DEBUG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appUpdateKit.checkForUpdate()
    }

    // ... same onActivityResult / onResume / onDestroy wiring as Style 1
}
```

---

## 📋 UpdateCallback Reference

| Method | When it fires |
|--------|--------------|
| `onUpdateAvailable(updateType)` | An update was found and the Play flow was launched |
| `onUpdateNotAvailable()` | No update available, or available version is below `auk_minimum_version_code` |
| `onUpdateDownloaded()` | **FLEXIBLE only** — download completed; prompt user to restart |
| `onUpdateFailed(exception)` | Any error (network, Play not available, user cancelled IMMEDIATE, etc.) |
| `onUpdateSnoozed(snoozeCount)` | User tapped "Remind later" (call `appUpdateKit.snooze()` from your UI) |
| `onUpdateDisabledByRemoteConfig()` | `auk_update_enabled` is `false` in Remote Config |

All methods have **default no-op** implementations — override only what you need.

---

## 😴 Snooze Behaviour

1. When your UI shows a "Not now" button, call `appUpdateKit.snooze()`.
2. AppUpdateKit records the timestamp and increments the snooze counter in `SharedPreferences`.
3. On the next `checkForUpdate()` call:
   - If `auk_snooze_duration_hours` has **not** elapsed → prompt is suppressed.
   - If the snooze count ≥ `auk_snooze_max_count` (and max > 0) → prompt is permanently suppressed until `resetSnooze()` is called.
4. After a successful install call `appUpdateKit.resetSnooze()` to clear the counter.

SharedPreferences file: `auk_snooze_prefs`  
Keys used: `auk_snooze_last_timestamp`, `auk_snooze_count`

---

## 🔒 ProGuard

No manual ProGuard configuration needed. Rules for Firebase Remote Config, Play In-App Updates, and all public AppUpdateKit classes are automatically applied via `consumer-rules.pro` (bundled in the AAR).

---

## 🏗️ Publishing to JitPack

1. Push this repo to GitHub.
2. Create a release tag: `git tag v1.0.0 && git push origin v1.0.0`
3. Visit `https://jitpack.io/#YOUR_USERNAME/AppUpdateKit` — JitPack auto-builds on first request.
4. The `jitpack.yml` at the repo root ensures JDK 17 is used for the build.

---

## 📁 Project Structure

```
AppUpdateKit/
├── appupdatekit/                        ← Library module (published to JitPack)
│   ├── src/main/java/com/appupdatekit/
│   │   ├── AppUpdateKit.kt              ← Public API entry point (Builder pattern)
│   │   ├── UpdateManager.kt             ← Core orchestration logic
│   │   ├── RemoteConfigManager.kt       ← Firebase Remote Config wrapper
│   │   ├── UpdateFlowHandler.kt         ← FLEXIBLE + IMMEDIATE flow handler
│   │   ├── SnoozeManager.kt             ← Snooze/remind-later (SharedPrefs)
│   │   ├── UpdateConfig.kt              ← Typed Remote Config data class
│   │   ├── UpdateCallback.kt            ← App-level callback interface
│   │   ├── UpdateType.kt                ← Enum: FLEXIBLE, IMMEDIATE, NONE
│   │   └── extensions/
│   │       └── ActivityExtensions.kt    ← checkAppUpdate { } shorthand
│   ├── build.gradle.kts
│   └── consumer-rules.pro               ← Auto-applied ProGuard rules
│
├── app/                                 ← Sample/demo app
│   └── src/main/java/com/example/in_app_update/
│       └── MainActivity.kt              ← Both integration styles demonstrated
│
├── jitpack.yml                          ← JitPack build config (JDK 17)
└── README.md
```

---

## 📄 License

```
MIT License

Copyright (c) 2026 AppUpdateKit Contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

