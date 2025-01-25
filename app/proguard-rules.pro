# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep all public APIs
-keep public class com.github.teranes10.androidutils.** { *; }

# Retrofit
-keepclassmembers class ** {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.google.gson.** { *; }

# Room Database
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep class * extends androidx.room.RoomDatabase { *; }

# SignalR
-keep class com.microsoft.signalr.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Firebase
-keepnames class com.google.firebase.** { *; }

# General
-dontwarn java.awt.**
-dontwarn javax.annotation.**