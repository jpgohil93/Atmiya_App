# Keep application classes
-keep class com.atmiya.** { *; }
-keep class com.atmiya.innovation.data.** { *; }
-keep class com.atmiya.innovation.repository.** { *; }

# Keep AndroidX and Compose
-keep class androidx.navigation.** { *; }
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }

# Keep Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }

# Gson specific rules
# Gson uses generic type information stored in a class file when working with fields. ProGuard/R8 removes such information by default, so configure it to keep all of it.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.examples.android.model.** { *; }
-keep class com.google.gson.** { *; }

# Prevent R8 from stripping interface information from serialized classes
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

# Keep Generative AI
-keep class com.google.ai.client.generativeai.** { *; }

# General safety
-dontwarn androidx.**
