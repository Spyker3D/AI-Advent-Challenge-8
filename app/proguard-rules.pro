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

# Keep data classes
-keep class com.aiassistant.core.domain.entity.** { *; }
-keep class com.aiassistant.core.data.dto.** { *; }

# Keep Dagger generated classes
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class **_Provide* { *; }

# Keep Gson serialized classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Retrofit service interfaces
-keep interface com.aiassistant.core.data.api.** { *; }