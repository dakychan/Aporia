# Aporia.cc Network Package

Веб-API подсистема для клиента Aporia. Обеспечивает HTTP сервер, управление файлами, HTML/CSS логику и REST API.

## Структура пакета

```
src/aporia/cc/network/
├── EthernetUtils.java      # Главный менеджер множества HTTP серверов
├── NetworkServer.java      # Отдельный HTTP сервер на порту (Netty)
├── NetworkServerManager.java # Менеджер множества NetworkServer инстансов
├── Requests.java          # Логика HTTP запросов (GET, POST, PUT, DELETE)
├── HTMLLogic.java         # Логика HTML/CSS файлов, шаблоны, хранение
├── NetworkInitializer.java # Инициализация всей подсистемы
├── ThreadManager.java     # Управление потоками с таймаутом неактивности
└── README.md              # Эта документация
```

## Основные возможности

### 1. EthernetUtils (Главный менеджер)
- Фасад для управления множеством HTTP серверов на разных портах
- Обратная совместимость со старым API (один сервер на порту 61033)
- Создание, остановка и мониторинг множества NetworkServer инстансов
- Централизованная регистрация эндпоинтов на любом порту
- Статистика и мониторинг всех активных серверов
- Интеграция с FilesManager для хранения

### 2. NetworkServer
- Отдельный HTTP сервер на **Netty 4.2.7** (полная совместимость с Minecraft)
- Управление портом с таймаутом bind (5 сек)
- Регистрация и удаление эндпоинтов с точным роутингом путей
- Асинхронный non-blocking I/O
- IdleStateHandler для закрытия неактивных соединений (5 мин)
- Каждый инстанс управляет своим портом и соединениями

### 3. NetworkServerManager
- Менеджер множества NetworkServer инстансов
- Создание и остановка серверов на разных портах
- Централизованное хранение и доступ к серверам
- Статистика по всем активным серверам

### 4. Requests
- Поддержка всех HTTP методов (GET, POST, PUT, DELETE, PATCH)
- Настраиваемые заголовки и таймауты
- Поддержка GZIP сжатия
- Кэширование ответов
- Логирование запросов через Logger

### 5. HTMLLogic
- Управление HTML и CSS файлами
- Система шаблонов с подстановкой переменных (`{{variable}}`)
- Стандартные шаблоны (dashboard, simple)
- Автоматическое создание структуры директорий
- Проверка существования файлов

### 6. NetworkInitializer
- Единая точка инициализации всей подсистемы
- Автоматическое создание дефолтных файлов
- Регистрация стандартных эндпоинтов на любых портах
- Управление жизненным циклом всех серверов
- Поддержка инициализации нескольких портов одновременно

### 7. ThreadManager
- Пул потоков с keep-alive time (5 минут неактивности)
- Автоматическое уничтожение неиспользуемых потоков
- ScheduledExecutor для периодических задач
- Graceful shutdown с awaitTermination

## Использование

### Быстрый старт (один сервер)

```java
// Инициализация с портом по умолчанию (61033)
boolean success = NetworkInitializer.init();

// Или с указанием порта
boolean success = NetworkInitializer.init(8080);

// Проверка статуса
if (NetworkInitializer.isRunning()) {
    int port = NetworkInitializer.getPort();
    System.out.println("Server running on port " + port);
}

// Остановка сервера
NetworkInitializer.shutdown();
```

### Работа с несколькими серверами

```java
// Инициализация нескольких портов одновременно
int[] ports = {61033, 61034, 61035};
boolean success = NetworkInitializer.initMultiple(ports);

// Создание дополнительных серверов через EthernetUtils
NetworkServer customServer = EthernetUtils.createServerOnPort(9090);
if (customServer != null) {
    customServer.registerEndpoint("/api/custom", (request, ctx) -> {
        String response = "{\"message\":\"Custom endpoint on port 9090\"}";
        EthernetUtils.sendResponse(ctx, HttpResponseStatus.OK, response, "application/json");
    });
}

// Регистрация эндпоинтов на конкретном порту
EthernetUtils.registerEndpoint(61034, "/api/v2/data", (request, ctx) -> {
    String response = "{\"data\":\"from port 61034\"}";
    EthernetUtils.sendResponse(ctx, HttpResponseStatus.OK, response, "application/json");
});

// Получение статистики всех серверов
String stats = NetworkInitializer.getStats();
System.out.println(stats);

// Остановка всех серверов
NetworkInitializer.shutdownAll();
```

### Примеры запросов

```java
// GET запрос
Requests.Response response = Requests.get("http://localhost:61033/health");
if (response.isSuccess()) {
    System.out.println("Response: " + response.getBody());
}

// POST запрос с JSON
String json = "{\"name\":\"test\",\"value\":123}";
Requests.Response postResponse = Requests.postJson("http://localhost:61033/api/data", json);

// Создание кастомного HTML
HTMLLogic.saveHTML("custom.html", "<html><body><h1>Custom Page</h1></body></html>");

// Чтение HTML
String content = HTMLLogic.readHTML("custom.html");

// Использование шаблонов
Map<String, String> variables = new HashMap<>();
variables.put("title", "My Dashboard");
variables.put("description", "Custom dashboard page");
String html = HTMLLogic.createFromTemplate("dashboard", variables);
HTMLLogic.saveHTML("dashboard.html", html);
```

### Стандартные эндпоинты

После инициализации доступны:

- `GET /` - Главная страница
- `GET /health` - Проверка здоровья сервера
- `GET /api/info` - Информация об API
- `GET /files/html/{filename}` - Получение HTML файлов
- `GET /files/css/{filename}` - Получение CSS файлов
- `GET /api/files/html/list` - Список HTML файлов
- `GET /api/health/detailed` - Детальная проверка здоровья

## Структура хранения файлов

```
~/.apr/network/
├── html/
│   ├── index.html
│   ├── dashboard.html
│   └── *.html
├── css/
│   └── styles.css
└── cache/
    └── [кэшированные ответы]
```

## Интеграция с Aporia

Пакет использует:
- `FilesManager.ROOT` для хранения файлов
- `Logger` для логирования
- `OsManager` для определения путей

## Примечания

1. **Порт по умолчанию**: 61033 (можно изменить через `NetworkInitializer.init(port)`)
2. **Безопасность**: Только localhost доступ (можно расширить при необходимости)
3. **Производительность**: Использует пул из 10 потоков для обработки запросов
4. **Хранение**: Все файлы сохраняются в `~/.apr/network/` (скрытая директория на Windows)

## Исправления и улучшения

### Критические исправления:
1. **HTMLLogic.createFromTemplate** - исправлен бесконечный цикл/IndexOutOfBounds при замене шаблонов. 
   Теперь используется безопасный `Matcher.appendReplacement()`.
2. **EthernetUtils.init()** - добавлен таймаут 5 секунд для операции bind().
   Если порт занят или сервер не может запуститься, поток не зависает навсегда.
3. **Роутинг путей** - улучшен алгоритм поиска обработчиков для путей с префиксами.
   Теперь корректно обрабатываются пути вида `/files/html/index.html` с обработчиком `/files/html/`.
4. **Утечка ресурсов** - `Requests.clearCache()` теперь закрывает `Files.walk()` поток через try-with-resources.
5. **IdleStateHandler** - добавлен для закрытия неактивных HTTP соединений через 5 минут.

### Новые возможности:
1. **ThreadManager** - интеллектуальный пул потоков с автоматическим уничтожением 
   неиспользуемых потоков после 5 минут неактивности.
2. **Netty вместо HttpServer** - полная перепись на Netty 4.2.7 для лучшей 
   производительности и совместимости с Minecraft.
3. **Graceful shutdown** - все компоненты корректно останавливаются с awaitTermination.
4. **Множество серверов** - новая архитектура для запуска нескольких HTTP серверов
   на разных портах с независимыми эндпоинтами.

## Архитектура множества серверов

### Иерархия управления:
```
EthernetUtils (фасад/менеджер)
    ├── NetworkServerManager (центральное хранилище)
    │   ├── NetworkServer порт 61033 (дефолтный)
    │   ├── NetworkServer порт 61034
    │   ├── NetworkServer порт 61035
    │   └── ...
    └── NetworkInitializer (инициализатор)
```

### Ключевые принципы:
1. **EthernetUtils** - единая точка входа для всего сетевого API
2. **NetworkServer** - изолированный HTTP сервер со своим портом и эндпоинтами
3. **Обратная совместимость** - старый код продолжает работать с дефолтным портом 61033
4. **Расширяемость** - можно создавать произвольное количество серверов на любых портах

### Преимущества новой архитектуры:
- **Изоляция** - каждый сервер работает независимо
- **Масштабируемость** - можно запускать десятки серверов на разных портах
- **Гибкость** - разные API версии на разных портах (v1 на 61033, v2 на 61034)
- **Надежность** - падение одного сервера не влияет на другие
- **Мониторинг** - централизованная статистика по всем серверам

## Дальнейшее развитие

По ТЗ следующие этапы:
1. Интеграция с AI пакетом (`ai.aporia.network`)
2. Создание CatBoost API эндпоинтов
3. Реализация WebSocket для real-time данных бота
4. Аутентификация и авторизация
5. HTTPS поддержка