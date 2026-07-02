# Workaround for R8 8.5.10 ConcurrentModificationException in shaking phase (AGP 8.5.0 bug)
# -dontshrink bypasses the crashing shaking phase; APK is larger but functional for beta testing
-dontoptimize
-dontshrink

# Keep app entry points
-keep class com.emireminder.app.** { *; }

# Room — keep entity and DAO classes used via reflection
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}

# Hilt — generated components and modules
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.InstallIn class *
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keepclasseswithmembernames class * {
    @javax.inject.Inject <fields>;
    @javax.inject.Inject <methods>;
}

# WorkManager + Hilt-Work — workers are instantiated by name
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# DataStore — Protobuf / Preferences serializer
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Kotlin metadata (needed for reflection-based libs)
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Logging removal disabled — requires shrinking which is off due to R8 8.5.0 bug workaround
