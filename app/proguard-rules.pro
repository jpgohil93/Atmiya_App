# Keep Data Models for Firestore Reflection
-keep class com.atmiya.innovation.data.** { *; }

# Keep AndroidX and Compose
-keep class androidx.navigation.** { *; }
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }

# Keep Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }

# General safety
-dontwarn androidx.**
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
