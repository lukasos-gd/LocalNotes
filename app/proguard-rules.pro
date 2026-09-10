# Keep model classes (parsed reflectively-free, but keep names stable for txt IO debugging)
-keep class com.lukasosstudios.localnotes.model.** { *; }
-dontwarn kotlinx.**

# The translation-template export walks R.string fields via reflection --
# without this, R8 strips the field table in release builds and the
# generated file ends up with nothing but the header.
-keepclassmembers class com.lukasosstudios.localnotes.R$string {
    public static final int *;
}
