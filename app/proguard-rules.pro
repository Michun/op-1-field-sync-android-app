# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep OP-1 Sync classes
-keep class com.op1sync.app.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Google Drive API
-keep class com.google.api.** { *; }
-dontwarn com.google.api.**
