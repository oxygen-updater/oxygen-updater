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

# Proguard configuration for Jackson 2.x, taken from https://github.com/FasterXML/jackson-docs/wiki/JacksonOnAndroid
-keep class com.fasterxml.jackson.databind.ObjectMapper {
    public <methods>;
    protected <methods>;
}
-keep class com.fasterxml.jackson.databind.ObjectWriter {
    public ** writeValueAsString(**);
}
# Removing this results in a Cannot deserialize value of type `boolean` from String "0": only "true" or "false" recognized error
-keepclassmembers class * {
     @com.fasterxml.jackson.annotation.* *;
}
-keep class kotlin.Metadata {
    *;
}
-keep class kotlin.reflect.** {
    *;
}

# We need to keep the method names of all getters and setters to allow Jackson to find them.
-keep public class com.arjanvlek.oxygenupdater.** {
    public void set*(***);
    public *** get*();
}
