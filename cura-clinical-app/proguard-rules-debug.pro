#These optimization settings will only apply if the proguard-android-optimize rather than proguard-android is used within the build.gradle
##All of these disabled optimizations are required to build, except the "inlining", which is required to keep line numbers accurate without using ReTrace
-optimizations !code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable,!method/inlining/short,!method/inlining/unique

-dontobfuscate

#The annotations are not needed
-dontwarn edu.umd.cs.findbugs.annotations.**

#We do not use the login callback and therefore this class is removed on compile
-dontwarn com.google.common.util.concurrent.Futures

#Ignore throwable that will never be used, but is pulled by com.cerner.system:system-core that needs removal
-dontwarn com.cerner.system.exception.ExtendedFailedLoginException

# LeakCanary
#-dontwarn android.app.Notification
#-keep class org.eclipse.mat.** { *; }
#-keep class com.squareup.leakcanary.** { *; }

-keepclassmembers class com.cerner.nursing.nursing.BuildConfig { *; }

#Keep Careteam contacts models
-keepclassmembers class com.cerner.careaware.connect.contacts.model.** { *;}

#Keep the all methods that are referenced at runtime by javascript (have the JavascriptInterface annotation)
-keep public class android.webkit.JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.newrelic.** { *; }
-dontwarn com.newrelic.**
