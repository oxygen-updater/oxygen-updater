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

# BEGIN: Crashlytics suggestions: https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports?platform=android#config-r8-proguard-dexguard
-keepattributes SourceFile,LineNumberTable        # Keep file names and line numbers.
-keep public class * extends java.lang.Exception  # Optional: Keep custom exceptions.
# END: Crashlytics suggestions
