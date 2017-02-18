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

-keepattributes *Annotation*,EnclosingMethod,Signature

# Keep all jackson and codehaus classes, and don't warn for conflicts within jackson-databind classes.
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
-keep class org.codehaus.** { *; }

# The class JsonAutoDetect may not be obfuscated as well.
-keepclassmembers public final enum org.codehaus.jackson.annotate.JsonAutoDetect$Visibility {
   public static final org.codehaus.jackson.annotate.JsonAutoDetect$Visibility *;
}

#We need to keep the method names of all getters and setters of the models to allow Jackson to find them.
-keep public class com.arjanvlek.oxygenupdater.Model.** {
  public void set*(***);
  public *** get*();
}
-dontwarn javax.xml.**
#Due to removal of Apache http components in Android 6.0 these lines have to be added here.
-dontwarn org.apache.http.**
-dontwarn android.net.http.AndroidHttpClient
-dontwarn com.google.android.gms.internal.**
-dontwarn org.joda.convert.**
-dontwarn java.lang.invoke.*
