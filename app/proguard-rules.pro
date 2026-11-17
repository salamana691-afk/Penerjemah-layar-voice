# Keep Room database entities, DAOs, and migrations
-keep class com.autodub.edge.data.database.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep OkHttp and Kotlin Coroutines metadata
-dontwarn okhttp3.**
-dontwarn okio.**
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
}

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
