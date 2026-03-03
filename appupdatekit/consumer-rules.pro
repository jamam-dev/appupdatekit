# ProGuard consumer rules for AppUpdateKit
# These rules are automatically applied to apps that consume this library.

# ─── Google Play In-App Updates ───────────────────────────────────────────────
-keep class com.google.android.play.core.appupdate.** { *; }
-keep interface com.google.android.play.core.appupdate.** { *; }
-keep class com.google.android.play.core.install.** { *; }
-keep interface com.google.android.play.core.install.** { *; }
-keep class com.google.android.play.core.install.model.** { *; }
-keep class com.google.android.play.core.tasks.** { *; }

# ─── Firebase Remote Config ───────────────────────────────────────────────────
-keep class com.google.firebase.remoteconfig.** { *; }
-keep interface com.google.firebase.remoteconfig.** { *; }
-dontwarn com.google.firebase.remoteconfig.**

# ─── AppUpdateKit public API ──────────────────────────────────────────────────
-keep public class com.appupdatekit.AppUpdateKit { *; }
-keep public class com.appupdatekit.AppUpdateKit$Builder { *; }
-keep public enum com.appupdatekit.UpdateType { *; }
-keep public class com.appupdatekit.UpdateConfig { *; }
-keep public interface com.appupdatekit.UpdateCallback { *; }
-keep public class com.appupdatekit.extensions.ActivityExtensionsKt { *; }

