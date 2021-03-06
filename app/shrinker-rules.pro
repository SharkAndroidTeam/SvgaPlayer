# This is a configuration file for ProGuard.
# https://proguard.sourceforge.net/index.html#manual/usage.html

-allowaccessmodification
-flattenpackagehierarchy
-mergeinterfacesaggressively
-dontnote *

# Remove intrinsic assertions.
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkFieldIsNotNull(...);
    public static void checkReturnedValueIsNotNull(...);
}

# https://github.com/square/okhttp/issues/6258
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
