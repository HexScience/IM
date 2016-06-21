

-skipnonpubliclibraryclasses
-target 1.6
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-useuniqueclassmembernames
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,Exceptions,InnerClasses
-renamesourcefileattribute SourceFile
-adaptresourcefilenames **.properties
-adaptresourcefilecontents **.properties,META-INF/MANIFEST.MF
-verbose
-dontnote com.android.vending.licensing.ILicensingService
-dontwarn android.support.**,org.springframework.**,eclipse.local.sdk.**,org.apache.commons.**,org.apache.commons.**
-dontwarn com.sina.**,com.weibo.**,com.tencent.**,com.facebook.**,com.renn.**
-dontwarn com.umeng.**,cn.jpush.android.**,com.networkbench.**,com.easemob.**,com.quvideo.xiaoying.IMClient.**,com.blueware.**,com.google.ads.**,com.google.android.gms.**,com.zhuge.**
-dontwarn com.mobvista.**
-dontwarn com.google.android.exoplayer.**
-dontwarn com.kf5.support.v4.**
-keepattributes Signature 
-keepattributes *Annotation*
-keep class com.mobvista.** {*; } 
-keep interface com.mobvista.** {*; } 
-keep class android.support.v4.** { *; }
-dontwarn android.app.**
-dontwarn com.flurry.**
-dontwarn com.pingstart.**
-dontwarn com.android.volley.**
-dontwarn com.alibaba.fastjson.**
-dontwarn com.altamob.sdk.**
-dontwarn org.apache.http.**
-dontwarn okio.**,com.google.gson.jpush.**
-dontwarn com.baidu.**
-dontwarn com.alibaba.**,com.ut.**

-keep public class com.pingstart.adsdk.R$*{
    public static final int *;
}
-dontwarn com.xiaoying.api.uploader.**
-dontwarn com.immersion.**
-dontnote com.immersion.**

# Preserve all fundamental application classes.
-keep public class * extends android.app.Activity

-keep public class * extends android.app.Application

-keep public class * extends android.app.Service

-keep public class * extends android.content.BroadcastReceiver

-keep public class * extends android.content.ContentProvider

-keep public class * extends android.app.backup.BackupAgentHelper

-keep public class * extends android.preference.Preference

-keep public class com.android.vending.licensing.ILicensingService

# Preserve all View implementations, their special context constructors, and
# their setters.
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context,android.util.AttributeSet);
    public <init>(android.content.Context,android.util.AttributeSet,int);
    public void set*(...);
}

# Preserve all classes that have special context constructors, and the
# constructors themselves.
-keepclasseswithmembers class * {
    public <init>(android.content.Context,android.util.AttributeSet);
}

# Preserve all classes that have special context constructors, and the
# constructors themselves.
-keepclasseswithmembers class * {
    public <init>(android.content.Context,android.util.AttributeSet,int);
}

# Preserve all possible onClick handlers.
-keepclassmembers class * extends android.content.Context {
    public void *(android.view.View);
    public void *(android.view.MenuItem);
}

# Preserve the special fields of all Parcelable implementations.
-keepclassmembers class * extends android.os.Parcelable {
    static android.os.Parcelable$Creator CREATOR;
}

# Preserve static fields of inner classes of R classes that might be accessed
# through introspection.
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Preserve the required interface from the License Verification Library
# (but don't nag the developer if the library is not used at all).
-keep public interface  com.android.vending.licensing.ILicensingService

# Preserve the special static methods that are required in all enumeration
# classes.
-keepclassmembers class * extends java.lang.Enum {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * {
    public <init>(org.json.JSONObject);
}

# keep 3rd libraries
# keep xiaoying sdk
-keep class com.toraysoft.trc.** {
    <fields>;
    <methods>;
}

-keep public class xiaoying.utils.QStream {
    <fields>;
    <methods>;
}

# for xiaoying_engine jar
-keep public class xiaoying.engine.** {
    <fields>;
    <methods>;
}

# for mediarecorder jar
-keep public class com.mediarecorder.engine.QBaseCamEngine {
    public protected <fields>;
    public protected <methods>;
    protected void postEventFromNative(...);
}

-keep public class * extends com.mediarecorder.engine.QBaseCamEngine {
    public protected <fields>;
    native <methods>;
    public protected <methods>;
}

-keep class com.quvideo.xiaoying.publish.sina.** {
    <fields>;
    <methods>;
}

-keep class com.baidu.** {
    <fields>;
    <methods>;
}

-keep class vi.com.gdi.bgl.android.java.** {
    <fields>;
    <methods>;
}

-keep class com.sina.** {
    <fields>;
    <methods>;
}

-keep class com.weibo.** {
    <fields>;
    <methods>;
}

-keep class com.tencent.** {
    <fields>;
    <methods>;
}

-keep class com.facebook.** {
    <fields>;
    <methods>;
}

-keep class com.renn.rennsdk.** {
    <fields>;
    <methods>;
}
-keep class com.quvideo.xiaoying.sns.** {
    <fields>;
    <methods>;
}

-keep class com.umeng.** {
    <fields>;
    <methods>;
}

-keep class eclipse.local.sdk.** {
    <fields>;
    <methods>;
}

-keep class cn.jpush.android.** {
    <fields>;
    <methods>;
}

-keep public class com.quvideo.xiaoying.R$* {
    public static final int *;
}

# ProGuard configurations for NetworkBench Lens
-keep class com.networkbench.** {
    <fields>;
    <methods>;
}

# End NetworkBench Lens
-keep class com.easemob.** {
    <fields>;
    <methods>;
}

-keep class org.jivesoftware.** {
    <fields>;
    <methods>;
}

-keep class org.apache.** {
    <fields>;
    <methods>;
}

# for google play service
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keep,allowshrinking @com.google.android.gms.common.annotation.KeepName class *

-keepclassmembers,allowshrinking class * {
    @com.google.android.gms.common.annotation.KeepName
    <fields>;
    @com.google.android.gms.common.annotation.KeepName
    <methods>;
}

-keep,allowshrinking class * extends android.os.Parcelable {
    public static final ** CREATOR;
}

-keep class org.apache.http.impl.client.**

-keep class com.blueware.** {
    <fields>;
    <methods>;
}

# for xiaoying_platformutils.jar
-keep,allowshrinking public class xiaoying.platform.** {
    public protected <fields>;
    public protected <methods>;
    native <methods>;
}

-keep,allowshrinking public class xiaoying.utils.** {
    public protected <fields>;
    public protected <methods>;
    native <methods>;
}

# forscene_navitator
-keep,allowshrinking public class com.quvideo.xiaoying.scenenavigator.** {
    public protected <fields>;
    public protected <methods>;
}

# for fastjson
-keep,allowshrinking public class com.xiaoying.fastjson.** {
    <fields>;
    <methods>;
}

# for svg jar
-keep,allowshrinking public class com.larvalabs.svgandroid.** {
    public protected <fields>;
    public protected <methods>;
}

# for trcparser jar
-keep,allowshrinking public class com.toraysoft.trc.** {
    public protected <fields>;
    public protected <methods>;
}

# for xiaoying_common jar
-keep,allowshrinking public class com.quvideo.xiaoying.common.** {
    public protected <fields>;
    public protected <methods>;
}

# for xiaoying_ads jar
-keep,allowshrinking public class com.quvideo.xiaoying.ads.** {
    public protected <fields>;
    public protected <methods>;
}

# for qiniu sdk
-keep,allowshrinking public class com.quniu.** {
    public protected <fields>;
    public protected <methods>;
}

# Keep - Applications. Keep all application classes, along with their 'main'
# methods.
-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

# Keep - Library. Keep all public and protected classes, fields, and methods.
-keep public class * {
    public protected <fields>;
    public protected <methods>;
}

# Also keep - Enumerations. Keep the special static methods that are required in
# enumeration classes.
-keepclassmembers enum  * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Also keep - Serialization code. Keep all fields and methods that are used for
# serialization.
-keepclassmembers class * extends java.io.Serializable {
    static final long serialVersionUID;
    static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep names - Native method names. Keep all native class/method names.
-keepclasseswithmembers,allowshrinking class *,* {
    native <methods>;
}

# Remove debugging - All logging API calls. Remove all invocations of the
# logging API whose return values are not used.
-assumenosideeffects public class java.util.logging.* {
    <methods>;
}

# Remove debugging - All Log4j API calls. Remove all invocations of the
# Log4j API whose return values are not used.
-assumenosideeffects public class org.apache.log4j.** {
    <methods>;
}

# Your application may contain more items that need to be preserved; 
# typically classes that are dynamically created using Class.forName:
# -keep public class mypackage.MyClass
# -keep public interface mypackage.MyInterface
# -keep public class * implements mypackage.MyInterface
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** i(...);
    public static *** e(...);
    public static *** w(...);
}

-keep class com.tencent.mm.sdk.openapi.WXMediaMessage {*;}
-keep class com.tencent.mm.sdk.openapi.** implements com.tencent.mm.sdk.openapi.WXMediaMessage$IMediaObject {*;}

#ProGuard configurations for NetworkBench Lens
-keep class com.networkbench.**{*;}
#End NetworkBench Lens
-keep class **.R$* {
    public static final int mobvista*;
}

-keep class com.adjust.sdk.plugin.MacAddressUtil {
    java.lang.String getMacAddress(android.content.Context);
}
-keep class com.adjust.sdk.plugin.AndroidIdUtil {
    java.lang.String getAndroidId(android.content.Context);
}
-keep class com.google.android.gms.common.ConnectionResult {
    int SUCCESS;
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    com.google.android.gms.ads.identifier.AdvertisingIdClient$Info
        getAdvertisingIdInfo (android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId ();
    boolean isLimitAdTrackingEnabled();
}
