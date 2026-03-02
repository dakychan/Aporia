# Proguard конфигурация для Aporia Client

# Сохраняем главные классы
-keep @annotation.anidumpproject.api.MainClass class * {
    public static void main(java.lang.String[]);
}

# Сохраняем Fabric entry points
-keep class aporia.su.Initialization {
    public void onInitializeClient();
    public void init();
}

# Обфусцируем классы с @Native аннотацией
-keep,allowobfuscation @annotation.anidumpproject.api.Native class *
-keep,allowobfuscation @annotation.anidumpproject.api.Native class * {
    <methods>;
}

# Переименовываем в короткие имена
-repackageclasses 'aporia.su'
-allowaccessmodification
-overloadaggressively

# Сохраняем аннотации для runtime
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Сохраняем Kotlin metadata
-keep class kotlin.Metadata { *; }
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# Сохраняем Fabric API
-keep class net.fabricmc.** { *; }
-dontwarn net.fabricmc.**

# Сохраняем Minecraft классы
-keep class net.minecraft.** { *; }
-dontwarn net.minecraft.**

# Сохраняем LWJGL
-keep class org.lwjgl.** { *; }
-dontwarn org.lwjgl.**

# Сохраняем Mixin
-keep class org.spongepowered.asm.** { *; }
-dontwarn org.spongepowered.asm.**

# Оптимизации
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Удаляем логирование в release
-assumenosideeffects class aporia.cc.Logger {
    public static void debug(...);
    public static void info(...);
}

# Обфускация строк
-adaptclassstrings
-adaptresourcefilenames
-adaptresourcefilecontents

# Удаляем неиспользуемый код
-dontshrink

# Не предупреждать о missing classes
-dontwarn **
