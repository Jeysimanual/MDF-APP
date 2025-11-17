# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- Fix for Gson + TypeToken crash ---

# Preserve generic type info and annotations for Gson
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Keep Gson classes
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class com.google.gson.stream.** { *; }

# Prevent warnings for sun.misc
-dontwarn sun.misc.**

# Keep your model classes and their generic signatures
-keep class com.capstone.mdfeventmanagementsystem.Utilities.NotificationService$NotificationItem { *; }
-keep class com.capstone.mdfeventmanagementsystem.Utilities.NotificationService$PendingNotificationItem { *; }
-keep class com.capstone.mdfeventmanagementsystem.model.** { *; }
-keep class com.capstone.mdfeventmanagementsystem.data.** { *; }

# Keep fields with @SerializedName in all classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve generic types for collections used with Gson
-keep class java.util.List { *; }
-keep class java.util.ArrayList { *; }

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# If you want to hide the original source file name, uncomment this
#-renamesourcefileattribute SourceFile