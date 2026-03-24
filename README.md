# APORIA

**Minecraft-клиент нового поколения** — быстро, чисто, незаметно.

---

## 🚀 Быстрый старт

```bash
# Запуск через IntelliJ IDEA
# Main class: mcp.client.Start
# JVM args: -Xmx2G -Djava.library.path=natives
```

---

## 📦 Структура проекта

```
src/
├── aporia/cc/           # Системные утилиты (PanicSystem, UserData)
├── com/mojang/          # Mojang Blaze3D (рендеринг, аудио, OpenGL)
├── mcp/client/          # Точка входа (Start.java)
├── net/minecraft/       # Minecraft исходники
└── so/aporia/           # Ядро клиента
    ├── module/          # Модули и настройки
    │   ├── impl/        # Реализации модулей
    │   │   ├── combat/  # Боевые модули
    │   │   ├── misc/    # Разное (ClickGui, ServerHelper)
    │   │   └── render/  # Визуальные (HUD, Blur)
    │   └── settings/    # Система настроек
    └── utils/           # Утилиты
        ├── events/      # Event-система
        ├── files/       # Работа с файлами
        └── user/        # UI, ввод, рендер, логгер
```

---

## 🎯 Модули

| Категория | Модули |
|-----------|--------|
| **VISUAL** | `HUD`, `Blur`, `ClickGui` |
| **MISC** | `ServerHelper`, `AutoConfig`, `PacketDebug`, `TestModule` |
| **COMBAT** | _в разработке_ |
| **MOVE** | _в разработке_ |
| **PLAYER** | _в разработке_ |
| **WORLD** | _в разработке_ |

### Ключевые модули

- **HUD** — FPS, пинг, ник, frame-time
- **ServerHelper** — AutoFlyMe, MathResolver (авто-капча), скрытие сообщений
- **ClickGui** — встроенный GUI для управления модулями
- **Blur** — радиальное размытие фона

---

## 🛠 Архитектура

### Event-система
```java
@EventHandler
public void onRenderHud(RenderHudEvent e) {
    // Обработка события
}

EventBus.INSTANCE.post(new RenderHudEvent(gfx, partialTick));
```

### Базовый модуль
```java
public class MyModule extends Module {
    public MyModule() {
        super("Name", Category.MISC, GLFW.GLFW_KEY_K);
    }
    
    @Override protected void onEnable()  { EventBus.INSTANCE.register(this); }
    @Override protected void onDisable() { EventBus.INSTANCE.unregister(this); }
}
```

### Настройки
```java
private final BooleanSetting enabled = new BooleanSetting("Enabled", "Description", true);
private final BindSetting keybind = new BindSetting("Key", -1);
private final TextSetting text = new TextSetting("Text", "default");
```

---

## 🎨 Рендеринг

### AporiaRenderer — кастомный 2D-рендерер на OpenGL

**Шейдерная система:**
- `aporia.vsh/fsh` — универсальный шейдер для примитивов
- `blur.vsh/fsh` — двухпроходное гауссово размытие
- `postprocess.vsh/fsh` — постобработка (saturation grading)

**Примитивы:**
```java
// Прямоугольник (в т.ч. скруглённый)
drawRect(x, y, w, h, radius, color)

// Размытый прямоугольник
drawRectBlurred(x, y, w, h, radius, color, blurStrength)

// Круг, треугольник, линия
drawCircle(cx, cy, radius, color)
drawTriangle(x1, y1, x2, y2, x3, y3, color)
drawLine(x1, y1, x2, y2, thickness, color)

// Градиенты
drawRectGradient(x, y, w, h, radius, c1, c2, dir) // dir: 0=horiz, 1=vert, 2=radial

// Обводка
drawStroke(x, y, w, h, radius, thickness, borderMode, fadeCorner, color)
```

**Blur-система:**
```java
// Подготовка (вызывается перед отрисовкой размытых элементов)
prepareBlur(mc, strength, saturation)

// Отрисовка с размытием
drawRectBlurred(x, y, w, h, radius, color)
```

### FontRenderer — MSDF шрифты

**Регистрация шрифтов:**
```java
Fonts.register(renderer);
// Загружает: regular, bold, icons, caticons, font
```

**API:**
```java
// Базовый текст
drawText("regular", "Hello", x, y, size, color)

// С обводкой (MSDF)
drawTextWithOutline("regular", "Text", x, y, size, color, outlineWidth, outlineColor)

// По центру
drawCenteredText("bold", "Title", x, y, size, color)

// Иконки из PUA-диапазона
drawGlyph(Fonts.FONT, index, x, y, size, color)
```

### ColorUtil — работа с цветом

```java
// Создание
int c = ColorUtil.rgba(80, 200, 200, 255);
int c = ColorUtil.fromHex("#50C8C8", 0xFFFFFFFF);
int c = ColorUtil.fromLegacyCode('c'); // §c

// Компоненты
int a = ColorUtil.alpha(c);
int r = ColorUtil.red(c);

// Интерполяция
int mid = ColorUtil.lerp(c1, c2, 0.5f);
int[] gradient = ColorUtil.gradient(start, end, steps);
int[] rainbow = ColorUtil.rainbow(steps, alpha);

// HSV
float[] hsv = ColorUtil.toHSV(c);
int c = ColorUtil.fromHSV(h, s, v);
```

### Easing — функции анимации

```java
// Полиномиальные
Easing.quadIn(t), Easing.cubicOut(t), Easing.quartInOut(t)

// Тригонометрические
Easing.sineInOut(t), Easing.circOut(t)

// Экспоненциальные
Easing.expoIn(t), Easing.expoOut(t)

// Эластичные
Easing.elasticOut(t), Easing.backOut(t, overshoot)

// Пружинные
Easing.springCritical(t, stiffness)
Easing.springUnderdamped(t, damping, frequency)

// Специальные
Easing.smoothstep(t), Easing.smootherstep(t)
Easing.cubicBezier(t, x1, y1, x2, y2)
Easing.cssEaseInOut(t)
```

### Анимационная система

```java
// SpringSimulator — физическая пружина
SpringSimulator spring = new SpringSimulator(stiffness, damping);
spring.update(dt, target);
float value = spring.getValue();

// TypeAnim — печатная машинка
TypeAnim anim = new TypeAnim("Hello World", speed);
anim.render(x, y, size, color);

// MessageAnim — анимация сообщений
MessageAnim msg = new MessageAnim(text, duration);
msg.render(gfx);
```

---

## 🔐 Panic Mode

Экстренное скрытие клиента:
1. Активация — через `PanicSystem.INSTANCE.panic()`
2. Восстановление — ввести свой ник в чат
3. Все сообщения чата очищаются

---

## 📚 Зависимости

### Графика и ввод
| Библиотека | Версия | Назначение |
|------------|--------|------------|
| **LWJGL** | 3.3.3 | OpenGL, GLFW, ввод |
| **JNA** | 5.17.0 | Native access |
| **oshi-core** | 6.9.0 | Системная информация |

### Утилиты
| Библиотека | Версия | Назначение |
|------------|--------|------------|
| **Guava** | 33.5.0 | Коллекции, кэш |
| **Gson** | 2.13.2 | JSON |
| **fastutil** | 8.5.18 | Примитивные коллекции |

### Сеть
| Библиотека | Версия | Назначение |
|------------|--------|------------|
| **Netty** | 4.2.7.Final | Асинхронный I/O |
| **commons-codec** | 1.19.0 | Кодирование |
| **commons-io** | 2.20.0 | Ввод-вывод |

### Логирование
| Библиотека | Версия | Назначение |
|------------|--------|------------|
| **Log4j** | 2.25.2 | Логирование |
| **slf4j** | 2.0.17 | Logging facade |

### ML / Анализ
| Библиотека | Версия | Назначение |
|------------|--------|------------|
| **catboost-common** | 1.2.10 | ML-модели (аномалии, паттерны) |

### Прочее
- **ASM 9.2** — байткод манипуляции
- **authlib-7.0.61** — аутентификация
- **brigadier-1.3.10** — командный движок
- **kotlin-stdlib-1.8.20** — Kotlin runtime

---

## ⚙️ Конфигурация

```
run/
└── aporia/
    ├── config/     # Конфиги модулей
    └── logs/       # Логи клиента
```

---

## 🧠 Для разработчиков

### Добавить модуль
1. Создать класс в `so/aporia/module/impl/<category>/`
2. Расширить `Module`
3. Зарегистрировать в `ModuleManager.registerAll()`

### Собрать проект
```bash
# Через IDEA: Build → Build Artifacts → Aporia.mcp:jar
# Или: gradle build (если есть build.gradle)
```

---

## 📋 Планы

> // потому что лень

---

## ⚖️ Лицензия

**© 2026 Aporia.cc. Все права защищены.**

Кратко:
- ✅ Использование **только в личных целях**
- ✅ Изучение кода в **образовательных целях**
- ✅ Модификация для **личного использования**

- ❌ Копирование, распространение, публикация кода
- ❌ Использование для создания **взломов, читов, вредоносного ПО**
- ❌ Коммерческое использование без письменного разрешения
- ❌ Выдача кода за свой
- ❌ Обход защит и лицензионных ограничений

📄 Полная версия лицензии: [LICENSE](LICENSE) (100 разделов, ~500 строк)

**Нарушение этой лицензии влечёт юридическую ответственность.**

---

<p align="center">
<b>fully by protect3ed</b><br>
<b>APORIA.CC ALL RIGHTS RESERVED © 2026</b>
</p>
