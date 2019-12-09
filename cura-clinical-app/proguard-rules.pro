#These optimization settings will only apply if the proguard-android-optimize rather than proguard-android is used within the build.gradle
##All of these disabled optimizations are required to build, except the "inlining", which is required to keep line numbers accurate without using ReTrace
-optimizations !code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable,!method/inlining/short,!method/inlining/unique

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

-keepattributes **

-keep,allowshrinking class com.cerner.cura.** { *; }

-keep,allowshrinking,allowoptimization class com.cerner.nursing.** { *; }
-keep,allowshrinking,allowoptimization class com.cerner.scanning.** { *; }
-keep,allowshrinking,allowoptimization class com.cerner.careaware.** { *; }

-keep,allowshrinking,allowoptimization class android.** { *; }
-keep,allowshrinking,allowoptimization class androidx.** { *; }
-keep,allowshrinking,allowoptimization class org.** { *; }
-keep,allowshrinking,allowoptimization class com.android.** { *; }
-keep,allowshrinking,allowoptimization class com.google.** { *; }
-keep,allowshrinking,allowoptimization class com.cerner.android.** { *; }
-keep,allowshrinking,allowoptimization class com.cerner.system.** { *; }

-keep class com.honeywell.** { *; }
-keep class com.intermec.** { *; }

-keep class com.newrelic.** { *; }
-dontwarn com.newrelic.**
