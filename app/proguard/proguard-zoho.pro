-keep class com.zoho.** {*;}
-keep interface android.support.v7.** { *; }
-keep class android.support.v7.** { *; }
-keep interface android.support.v4.** { *; }
-keep class android.support.v4.** { *; }
-keepattributes Signature
-keepattributes Annotation
-keep class okhttp3.* { *; }
-keep interface okhttp3.* { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn com.zoho.accounts.**
-dontwarn com.squareup.okhttp.**
-dontwarn okio.**
-dontwarn retrofit2.**