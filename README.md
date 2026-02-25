# Aporia MCP - Чистая версия без Gradle

Minecraft 1.21.11 + Aporia Client

## Структура

```
Aporia.clear/
├── src/
│   ├── classes/        # Скомпилированные классы (для запуска)
│   ├── assets/         # Ассеты Minecraft + Aporia
│   ├── net/minecraft/  # Декомпилированный Minecraft (Java)
│   ├── com/mojang/     # Декомпилированный Mojang (Java)
│   ├── main.kotlin.ru/             # Твой чит (Java + Kotlin)
│   └── run/            # Рабочая папка
├── libs/               # Библиотеки (144 JAR)
├── vineflower-1.11.2.jar  # Декомпилятор
├── decompile_all.bat      # Декомпиляция классов в исходники
└── run_mcp.bat            # Запуск клиента
```

## Запуск

```bash
run_mcp.bat
```

## Редактирование кода

**Классы уже декомпилированы** в `src/net/minecraft/`, `src/com/mojang/`, `src/main.kotlin.ru/`

1. Редактируй `.java` файлы в `src/`
2. Компилируй обратно в `src/classes/`
3. Запускай `run_mcp.bat`

## Декомпиляция (если нужно)

```bash
decompile_all.bat
```

Декомпилирует `src/classes/` → `src/` (обновляет исходники)

## Требования

- **Java 21** или выше
- **Windows 10/11** (для нативов LWJGL)

## Что внутри

- Minecraft 1.21.11 (декомпилированный)
- Aporia Client (чит)
- CometRenderer (кастомный рендерер)
- LWJGL 3.3.3
- Kotlin 2.1.10
- Все необходимые библиотеки

## Примечания

- **БЕЗ GRADLE** - работает напрямую через java
- `src/classes/` - для ЗАПУСКА (скомпилированные .class)
- `src/*.java` - для РЕДАКТИРОВАНИЯ (исходники)
- Ассеты в `src/assets/`

---
Собрано без Gradle 🎉
