
-dontobfuscate
-verbose
-android
#-dontwarn *

-dontwarn com.google.errorprone.annotations.MustBeClosed
-ignorewarnings

-keep class com.sun.jna.** { *; }

# clean & rebuild if problems occur
