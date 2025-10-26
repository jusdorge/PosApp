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

# ============================================================================
# قواعد للتعامل مع مشاكل MediaTek والأجهزة المختلفة
# ============================================================================

# تجاهل فئات MediaTek المفقودة
-dontwarn com.mediatek.**
-dontwarn com.mtk.**
-dontwarn android.view.ViewRootImpl

# حماية من أخطاء فئات النظام المفقودة
-dontwarn java.lang.invoke.**
-dontwarn java.lang.ClassValue
-dontwarn java.lang.SafeVarargs

# حماية Firebase من التحسين الزائد
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# حماية فئات التطبيق الأساسية
-keep class com.example.islamicquiz.model.** { *; }
-keep class com.example.islamicquiz.UserSession { *; }
-keep class com.example.islamicquiz.MyPOSApplication { *; }

# حماية فئات Firestore
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.PropertyName <fields>;
}

# حماية من تحسين الانعكاس (Reflection)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# تجاهل تحذيرات المكتبات الخارجية
-dontwarn org.osmdroid.**
-dontwarn com.journeyapps.barcodescanner.**