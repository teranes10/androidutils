# Preserve all public APIs
-keep public class com.github.teranes10.androidutils.** { *; }

# Room (Preserve annotations and classes for Room Database)
-keep class * extends androidx.room.RoomDatabase {
    public static *** databaseBuilder(android.content.Context, java.lang.Class, java.lang.String);
}

-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Gson (Preserve classes used with Gson)
-keep class com.google.gson.** { *; }

# SignalR (Preserve SignalR classes)
-keep class com.microsoft.signalr.** { *; }
-keep interface com.microsoft.signalr.** { *; }

# Retrofit (Preserve classes used by Retrofit)
-keepclassmembers class ** {
    @retrofit2.http.* <methods>;
}

-keepattributes Signature
-keepattributes Exceptions