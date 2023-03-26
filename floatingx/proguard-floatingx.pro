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
# basepopup

-dontwarn com.petterp.floatingx.**
-keep public class com.petterp.floatingx.view.FxMagnetView{*;}
-keep public class com.petterp.floatingx.view.FxViewHolder{*;}
-keep class * implements com.petterp.floatingx.listener.control.IFxAppControl { *; }
-keep class * implements com.petterp.floatingx.listener.control.IFxControl { *; }
-keep public class com.petterp.floatingx.util.FxScreenExtKt{
  private boolean checkNavigationBarShow(android.content.Context);
}

