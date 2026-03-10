# Aporia Client

Minecraft клиент с системой команд и событий.

## Структура проекта

### aporia.su - Основной пакет клиента

#### Команды (`aporia.su.utils.chat`)
Система команд с автодополнением и кастомным префиксом.

- `Command` - базовый класс для всех команд
- `CommandManager` - менеджер команд (Singleton)
- `InfoCommand` - информация о системе команд
- `PrefixCommand` - изменение префикса команд
- `AliasCommand` - управление алиасами команд

Префикс по умолчанию: `^`

Доступные команды:
- `^info` (alias: help, ?) - показать список команд
- `^prefix <новый>` - изменить префикс
- `^alias add <имя>` - создать алиас
- `^alias list` - список алиасов
- `^alias remove <имя>` - удалить алиас

#### События (`aporia.su.utils.events`)

Система событий с аннотациями `@EventHandler`.

- `Event` - базовый класс событий + аннотация `@EventHandler`
- `EventManager` - менеджер событий с автоматической регистрацией через рефлексию
- `TabCompleteEvent` - событие автодополнения в чате

Пример использования:
```java
@EventHandler
public void onTabComplete(TabCompleteEvent event) {
    // обработка события
}
```

#### Инициализация (`aporia.su`)

- `AporiaInit` - точка входа, регистрирует команды и события

### dev.aporia - Утилиты

- `OsManager` - управление системой (ОС, железо, пути, погода)
- `UserData` - данные пользователя (username, UUID, роль, HWID)
- `UserGenerator` - генерация UUID и идентификаторов

### dev.anidumpproject - API проекта

Дополнительный API (см. `src/main/java/dev/anidumpproject/api/README.md`)

## Сборка

```bash
нет идите нахуй мне лень
```

## Запуск

```bash
./gradlew runClient
```
