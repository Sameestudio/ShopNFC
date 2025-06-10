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

# Midtrans SDK ProGuard Rules
# Keep all classes in com.midtrans.sdk package and its subpackages
#-keep class com.midtrans.sdk.** { *; }

# Keep all members of Parcelable classes for correct unmarshalling
#-keep class * implements android.os.Parcelable {
#    public static final android.os.Parcelable$Creator *;
#}

# Keep classes that are annotated with @Keep (often used by libraries for retention)
#-keepattributes *Annotation*

# If you explicitly use GSON for any serialization/deserialization, ensure its classes are kept
#-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.reflect.TypeToken { *; }
#-keep class com.google.gson.internal.UnsafeAllocator { *; }
#-keep class com.google.gson.internal.UnsafeAllocator$1 { *; }
#-keep class com.google.gson.internal.UnsafeAllocator$2 { *; }
#-keep class com.google.gson.internal.UnsafeAllocator$3 { *; }
#-keep class com.google.gson.internal.UnsafeAllocator$4 { *; }
#-keep class com.google.gson.internal.ConstructorConstructor { *; }

# You might also need specific rules for OkHttp/Retrofit if not handled by default optimize rules
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn com.google.android.gms.** # To warn about Google Play Services issues, if any
#-keep class com.midtrans.sdk.uikit.api.model.** { *; }
#-keep class com.midtrans.** { *; }
#-keep class id.co.veritrans.** { *; }
-keep class com.midtrans.sdk.uikit.api.model.** { *; }
-keepclassmembers class com.midtrans.sdk.uikit.api.model.** { *; }

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

