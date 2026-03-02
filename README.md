<div align="center">

![Aporia](src/main/resources/assets/Aporia.png)

# ✨ Aporia Client ✨

*Современный Minecraft клиент с продвинутыми возможностями* 🚀

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-red)](https://www.minecraft.net/ru-ru/about-minecraft)
[![Fabric](https://img.shields.io/badge/Fabric-0.18.4-yellow.svg)](https://fabricmc.net)
[![Java](https://img.shields.io/badge/Java-25-green.svg)](https://openjdk.org)

</div>

---

## 🌟 Особенности

### 🎮 Модульная система
- **Гибкая архитектура** - легко добавляй новые модули
- **Горячие клавиши** - настраиваемые кейбинды для всех функций
- **Event-driven** - современная система событий

### 🎨 Продвинутый рендеринг
- **MSDF текст** - четкий и красивый текст любого размера
- **Blur эффекты** - стильное размытие для GUI
- **Анимации** - плавные переходы и эффекты
- **CometRenderer** - мощный рендеринг движок

### 💬 Система уведомлений
- **Красивые нотификации** - стильные всплывающие сообщения
- **Анимированные** - плавное появление и исчезновение
- **Настраиваемые** - разные типы и стили

### 🖱️ ClickGUI
- **Интуитивный интерфейс** - удобное управление модулями
- **Dropdown меню** - организованные категории
- **Drag & Drop** - перетаскивай элементы куда хочешь

### 📊 HUD элементы
- **Таймеры расходников** - отслеживай зелья и эффекты
- **Дистанция до игроков** - видь расстояние до других
- **Elytra индикатор** - контроль прочности элитр

---

## 🛠️ Технологии

- **Fabric API** - модлоадер нового поколения
- **CometRenderer** - кастомный OpenGL рендеринг
- **LWJGL** - низкоуровневая графика
- **Mixin** - инъекции в код Minecraft
- **JOML** - математика для 3D

---

## 📦 Установка

1. Установи [Fabric 1.18.4](https://fabricmc.net/use/installer//)
2. Скачай последнюю версию Aporia
3. Помести `.jar` в папку `mods`
4. Запускай и наслаждайся! 🎉

---

## ⚙️ Разработка

```bash
git clone https://github.com/dakychan/aporia/aporia.git

./gradlew build

./gradlew runClient

# Обфускация для релиза
./gradlew chaosObfuscate
./gradlew chaosJar
```

### 🌍 Unicode Chaos Obfuscation

Aporia использует продвинутую систему обфускации с поддержкой Unicode символов из 30+ языков!

**Уровни защиты:**
- 🟢 **LIGHT** - ASCII only (базовая защита)
- 🟡 **MEDIUM** - Greek + Cyrillic (средняя защита)
- 🟠 **HEAVY** - Asian languages (высокая защита)
- 🔴 **EXTREME** - Full Unicode chaos (максимальная защита)

**Что включено:**
- Китайский, Японский, Корейский, Тайский, Арабский
- Руны, Египетские иероглифы, Клинопись
- Математические символы, Эмодзи, Брейль
- Ежедневная ротация маппингов (00:00 UTC)

Подробнее: [UNICODE_CHAOS_GUIDE.md](UNICODE_CHAOS_GUIDE.md)

---

## 🎯 Структура проекта

```
src/main/java/ru/
├── 📁 event/          # Система событий
├── 📁 gui/            # GUI менеджер
├── 📁 input/          # Обработка ввода
├── 📁 module/         # Модули клиента
├── 📁 render/         # Рендеринг системы
├── 📁 ui/             # UI компоненты
│   ├── clickgui/      # ClickGUI
│   ├── hud/           # HUD элементы
│   └── notify/        # Уведомления
└── 📁 util/           # Утилиты
```

---

## 🤝 Вклад в проект

Мы рады любому вкладу! 💖

1. Fork проект
2. Создай feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit изменения (`git commit -m 'Add some AmazingFeature'`)
4. Push в branch (`git push origin feature/AmazingFeature`)
5. Открой Pull Request

---

## 📝 Лицензия

Этот проект распространяется под лицензией MIT. Подробности в файле `LICENSE`.

---

## 💎 Авторы

Создано с любовью командой Aporia ❤️

---

<div align="center">

### ⭐ Поставь звезду, если проект понравился! ⭐

**Aporia** - *твой путь к идеальному Minecraft опыту* 🌈

*добавте /**/ на то что не робит либо //*
</div>
