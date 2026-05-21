# Add project specific ProGuard rules here.

-keep class com.expensetracker.app.core.model.** { *; }
-keep class com.expensetracker.app.core.database.entity.** { *; }

-keepattributes *Annotation*

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-dontwarn javax.annotation.**
-dontwarn kotlin.**
-dontwarn com.gemalto.jp2.**
