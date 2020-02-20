# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/arjan/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Crashlytics suggestion: to allow for meaningful crash reports
-keepattributes *Annotation*,EnclosingMethod,Signature,SourceFile,LineNumberTable

# Crashlytics suggestion: custom exceptions should be be skipped during obfuscation
-keep public class * extends java.lang.Exception

# The following two lines are required for https://github.com/FasterXML/jackson-module-kotlin to work properly
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }

# We need to keep the method names of all getters and setters to allow Jackson to find them.
-keep class com.arjanvlek.oxygenupdater.models.** { *; }

# Removing this results in `java.lang.IllegalStateException: Incomplete hierarchy for class UpdateData, unresolved classes [com.arjanvlek.oxygenupdater.models.FormattableUpdateData]`
-keepnames class com.arjanvlek.oxygenupdater.models.FormattableUpdateData
-keepnames class com.arjanvlek.oxygenupdater.internal.settings.SettingsManager

# Glide proguard config, copied from https://bumptech.github.io/glide/doc/download-setup.html#proguard
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
# If you're targeting any API level less than Android API 27, also include:
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder

# Disable the annoying "Parameter specified as non-null is null" exceptions
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}
