# Add project specific ProGuard rules here.

# Keep data classes and DTOs
-keep class com.aiassistant.core.data.dto.** { *; }

# Keep Gson serialized classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Retrofit service interfaces
-keep interface com.aiassistant.core.data.api.** { *; }