# Add project specific ProGuard rules here.

# Keep domain entities
-keep class com.aiassistant.core.domain.entity.** { *; }

# Keep repository interfaces
-keep interface com.aiassistant.core.domain.repository.** { *; }

# Keep use cases
-keep class com.aiassistant.core.domain.usecase.** { *; }