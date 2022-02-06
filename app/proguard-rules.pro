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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Allow the access modifiers of classes and class members to be modified, while optimizing.
-allowaccessmodification

-dontobfuscate

## If we are obfuscating we need to keep the dataprovider class names
#-keepnames class rocks.tbog.tblauncher.dataprovider.*
#
## If we are obfuscating check rocks/tbog/tblauncher/utils/EdgeGlowHelper.java
#-keepnames class android.**
#-keepclassmembernames class android.** {
#    private <fields>;
#}
#-keepnames class androidx.**
#-keepclassmembernames class androidx.** {
#    private <fields>;
#}

# Need to keep constructor of worker
-keepclassmembers class * extends rocks.tbog.tblauncher.WorkAsync.AsyncTask { <init>(...); }
# Keep constructor of ViewHolder
-keepclassmembers class * extends rocks.tbog.tblauncher.utils.ViewHolderAdapter$ViewHolder { <init>(...); }