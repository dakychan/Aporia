# 🔒 AniDumpProject API - Unicode Chaos Obfuscation

Система обфускации с ежедневной ротацией маппингов и поддержкой Unicode символов из 30+ языков мира.

---

## 📦 Что это?

**AniDumpProject API** - это мощная система обфускации для защиты Java/Kotlin кода от реверс-инжиниринга. Основные фичи:

- 🌍 **Unicode Chaos** - использует символы из 30+ языков (китайский, арабский, руны, иероглифы, эмодзи)
- 🔄 **Ежедневная ротация** - маппинги меняются каждый день в 00:00 UTC
- 🎯 **4 уровня защиты** - от легкой до экстремальной обфускации
- 🚀 **Kotlin поддержка** - работает с Java и Kotlin классами
- 🔥 **Fabric интеграция** - встроена в процесс сборки Gradle

---

## 🎯 Уровни обфускации

### 🟢 LIGHT - Базовая защита
```java
@Obfuscate(level = Level.LIGHT)
public class MyModule {
    // Результат: только имена классов меняются (ASCII)
    // MyModule → C/x/j/eU
    // Методы и поля остаются читаемыми
}
```

**Что обфусцируется:**
- ✅ Имена классов (ASCII: a, b, c, eU)
- ❌ Методы остаются как есть
- ❌ Поля остаются как есть

**Когда использовать:** Публичные API, библиотеки

---

### 🟡 MEDIUM - Средняя защита
```java
@Obfuscate(level = Level.MEDIUM)
public class MyModule {
    // Результат: классы с unicode (греческий + кириллица)
    // MyModule → Т/θ/σ/h_У
    // Методы и поля остаются читаемыми
}
```

**Что обфусцируется:**
- ✅ Имена классов (Unicode: α, β, γ, а, б, в)
- ❌ Методы остаются как есть
- ❌ Поля остаются как есть

**Когда использовать:** Обычные модули, утилиты

---

### 🟠 HEAVY - Высокая защита
```java
@Obfuscate(level = Level.HEAVY)
public class LicenseCheck {
    // Результат: классы + методы + поля в unicode
    // LicenseCheck → ड/‡/し/Oぬ大f
    // checkLicense() → ιจ
    // isValid → ฅ०
}
```

**Что обфусцируется:**
- ✅ Имена классов (Азиатские: 中, 日, 한, ก, अ)
- ✅ Методы (Unicode каша)
- ✅ Поля (Unicode каша)

**Когда использовать:** Важная бизнес-логика, лицензии

---

### 🔴 EXTREME - Максимальная защита
```java
@Obfuscate(level = Level.EXTREME)
public class AntiCheat {
    // Результат: ПОЛНАЯ КАША из всех языков мира
    // AntiCheat → ›ჩཽЛ可ძĐ"
    // detect() → ®"
    // isHacking → ː水
}
```

**Что обфусцируется:**
- ✅ Имена классов (ВСЁ: руны, иероглифы, эмодзи, брейль)
- ✅ Методы (Максимальная каша)
- ✅ Поля (Максимальная каша)

**Когда использовать:** Критичные системы (античит, DRM, защита)

---

## 🚀 Быстрый старт

### 1. Добавь аннотацию на класс

```java
import anidumpproject.api.annotation.Obfuscate;

@Obfuscate(level = Obfuscate.Level.EXTREME)
public class MySecretClass {
    
    private String apiKey = "secret";
    
    public boolean validate(String input) {
        return apiKey.equals(input);
    }
}
```

### 2. Собери проект

```bash
./gradlew build
```

Обфускация применяется автоматически! ✅

### 3. Результат

```
build/libs/
├── Aporia-0.4.jar              # Обфусцированный JAR с Fabric
├── Aporia-0.4-sources.jar      # Исходники
└── Aporia-0.4-obfuscated-dev.jar  # Dev версия

build/
└── chaos-mappings-2026-03-02.txt  # Маппинги (сохрани!)
```

---

## 📊 Примеры обфускации

### До обфускации:
```java
package aporia.su.modules.combat;

public class KillAura {
    private double range = 4.0;
    
    public void attack(Entity target) {
        target.damage(10);
    }
}
```

### После LIGHT:
```java
package C.x.j;

public class eU {
    private double range = 4.0;  // НЕ ТРОНУТО
    
    public void attack(Entity target) {  // НЕ ТРОНУТО
        target.damage(10);
    }
}
```

### После MEDIUM:
```java
package Т.θ.σ;

public class h_У {
    private double range = 4.0;  // НЕ ТРОНУТО
    
    public void attack(Entity target) {  // НЕ ТРОНУТО
        target.damage(10);
    }
}
```

### После HEAVY:
```java
package ड.‡.し;

public class Oぬ大f {
    private double ฅ० = 4.0;  // ОБФУСЦИРОВАНО
    
    public void ιจ(Entity अआ) {  // ОБФУСЦИРОВАНО
        अआ.damage(10);
    }
}
```

### После EXTREME:
```java
package ›.ჩ.ཽ;

public class Л可ძĐ" {
    private double ː水 = 4.0;  // ПОЛНАЯ КАША
    
    public void ®"(Entity ໃཟ) {  // ПОЛНАЯ КАША
        ໃཟ.damage(10);
    }
}
```

---

## 🛡️ Как это защищает?

### 1. Дамп памяти бесполезен
В runtime только unicode каша - без маппингов не разобраться:
```
›ჩཽЛ可ძĐ".®"(Lໃཟ;)V
```

### 2. Декомпиляция не помогает
Даже после декомпиляции код нечитаем:
```java
public class 🔥💀👻 {
    private double ː水;
    public void ®"(Entity ໃཟ) { ... }
}
```

### 3. Поиск по коду невозможен
Unicode символы не вводятся с клавиатуры - поиск не работает

### 4. Отладка затруднена
IDE не понимает unicode имена, breakpoints не ставятся

### 5. Ежедневная ротация
Завтра все маппинги изменятся - старые дампы устареют

---

## 🔧 RuntimeMapper - Деобфускация в runtime

Для отладки крашей используй `RuntimeMapper`:

```java
import anidumpproject.api.RuntimeMapper;

try {
    // Твой код
} catch (Exception e) {
    // Деобфусцировать stacktrace
    String deobfuscated = RuntimeMapper.deobfuscateStackTrace(e);
    System.err.println(deobfuscated);
    
    // Или деобфусцировать имя класса
    String className = RuntimeMapper.deobfuscateClass("›/ჩ/ཽ/Л可ძĐ");
    // Результат: aporia/su/modules/combat/KillAura
}
```

**Методы:**
- `deobfuscateClass(String)` - деобфусцировать имя класса
- `deobfuscate(String)` - деобфусцировать строку stacktrace
- `deobfuscateStackTrace(Throwable)` - деобфусцировать весь stacktrace
- `hasMappings()` - проверить загружены ли маппинги
- `getMappingsCount()` - количество маппингов
- `getSeed()` - seed текущего дня

---

## 📝 Аннотации

### @Obfuscate
Маркирует классы для обфускации:

```java
@Obfuscate(
    level = Level.EXTREME,  // Уровень обфускации
    comment = "Критичная система"  // Комментарий для разработчика
)
public class MyClass { }
```

**Уровни:**
- `Level.NONE` - не обфусцировать
- `Level.LIGHT` - легкая (ASCII)
- `Level.MEDIUM` - средняя (греческий + кириллица)
- `Level.HEAVY` - тяжелая (азиатские языки)
- `Level.EXTREME` - экстремальная (все языки + эмодзи)

### @Native
Маркирует классы для нативной защиты (VMProtect):

```java
@Native(
    type = Native.Type.VMProtectBeginUltra,
    priority = Native.Priority.HIGH
)
public class CriticalClass { }
```

### @MainClass
Исключает класс из обфускации:

```java
@MainClass
public class Initialization {
    // Этот класс НЕ будет обфусцирован
}
```

---

## 🌍 Unicode символы

Обфускатор использует символы из:

**Азиатские языки:**
- 🇨🇳 Китайский (的, 一, 是, 中)
- 🇯🇵 Японский (あ, ア, 犬, 猫)
- 🇰🇷 Корейский (가, 나, 다)
- 🇹🇭 Тайский (ก, ข, ค)
- 🇮🇳 Деванагари (अ, आ, इ)

**Древние письменности:**
- ⚔️ Руны (ᚠ, ᚢ, ᚦ)
- 🏛️ Египетские иероглифы (𓀀, 𓀁, 𓀂)
- 📜 Клинопись (𒀀, 𒀁, 𒀂)

**Специальные:**
- 🔤 Математические (𝐀, 𝑎, 𝒜, 𝔄)
- 🔄 Зеркальные (ɐ, q, ɔ, p)
- ⠿ Брейль (⠁, ⠃, ⠉)
- 😀 Эмодзи (🔥, 💀, 👻)

**Всего:** 2000+ уникальных символов!

---

## ⚙️ Настройка

### Отключить обфускацию для класса

```java
@Obfuscate(level = Level.NONE)
public class PublicAPI {
    // Не будет обфусцирован
}
```

### Kotlin поддержка

```kotlin
import anidumpproject.api.annotation.Obfuscate

@Obfuscate(level = Obfuscate.Level.EXTREME)
class MyKotlinClass {
    fun myMethod() { }
}
```

Работает идеально! ✅

---

## 📋 Маппинги

Маппинги сохраняются в `build/chaos-mappings-YYYY-MM-DD.txt`:

```
# CHAOS OBFUSCATION MAPPINGS
# Date: 2026-03-02
# Seed: -6222067556439006302
# ⚠️  EXPIRES TOMORROW AT 00:00 UTC!

# Classes:
aporia/su/modules/combat/KillAura -> ›/ჩ/ཽ/Л可ძĐ"

# Methods:
# Class: aporia/su/modules/combat/KillAura
  attack(Lnet/minecraft/entity/Entity;)V -> ®"
  
# Fields:
# Class: aporia/su/modules/combat/KillAura
  range -> ː水
```

**ВАЖНО:** Сохраняй маппинги! Они нужны для деобфускации логов.

---

## ⚠️ Важные моменты

1. **Маппинги меняются каждый день** в 00:00 UTC
2. **Сохраняй маппинги** для каждого релиза
3. **Не коммить маппинги в git** - они содержат структуру проекта
4. **Миксины не трогаются** - классы с `@Mixin` пропускаются автоматически
5. **JVM совместимость** - Unicode идентификаторы поддерживаются с Java 1.1

---

## 🎯 Рекомендации

| Тип кода | Уровень | Причина |
|----------|---------|---------|
| Публичные API | LIGHT | Нужна читаемость |
| Обычные модули | MEDIUM | Баланс защиты/производительности |
| Бизнес-логика | HEAVY | Важная логика |
| Лицензии, DRM | EXTREME | Максимальная защита |
| Античит | EXTREME | Критичная безопасность |

---

## 🔥 Что дальше?

- [ ] Anti-dump агент (защита от дампа памяти)
- [ ] Proguard-style keep rules
- [ ] String encryption
- [ ] Control flow obfuscation
- [ ] VMProtect интеграция

---

## 📞 Поддержка

Если что-то не работает:
1. Проверь что аннотация правильная
2. Убедись что класс не Mixin
3. Проверь маппинги в `build/chaos-mappings-*.txt`
4. Используй `RuntimeMapper` для деобфускации

---

**Создано с 💀 командой Aporia**

*Unicode Chaos - потому что обычная обфускация это скучно* 🔥
