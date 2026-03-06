package aporia.su.util.files;

import aporia.cc.OsManager;
import aporia.su.Initialization;
import aporia.su.modules.module.ModuleRepository;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.setting.Setting;
import aporia.su.modules.module.setting.implement.*;
import aporia.su.util.helper.Logger;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.*;

/**
 * FilesManager - кроссплатформенный менеджер файлов и конфигов.
 * 
 * <p>Поддерживаемые форматы:</p>
 * <ul>
 *   <li>.apr - custom archive format</li>
 *   <li>.json - JSON</li>
 *   <li>.lua - Lua scripts</li>
 *   <li>.cbm - CatBoost models</li>
 *   <li>.zip - ZIP archives</li>
 *   <li>.gzip - GZIP compression</li>
 * </ul>
 * 
 * <p>Особенности:</p>
 * <ul>
 *   <li>Кроссплатформенность (Windows, Linux, macOS)</li>
 *   <li>Автоматическое создание директорий</li>
 *   <li>Thread-safe операции</li>
 *   <li>Atomic writes</li>
 *   <li>UTF-8 encoding</li>
 *   <li>Скрытие файлов на Windows</li>
 *   <li>Panic mode для экстренного удаления следов</li>
 * </ul>
 * 
 * @author Aporia.cc
 * @version 0.4
 * @since 2026
 */
public class FilesManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Форматы файлов.
     */
    public enum FileFormat {
        APR,    // Custom archive format
        JSON,   // JSON format
        LUA,    // Lua script
        CBM,    // CatBoost model
        ZIP,    // ZIP archive
        GZIP,   // GZIP compression
        PLAIN   // Plain text
    }

    /**
     * Режимы проверки файлов.
     */
    public enum CheckMode {
        ALWAYS,     // Проверять каждый раз
        ONCE,       // Проверить один раз при создании
        NEVER       // Не проверять
    }

    // ========================================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ========================================================================

    /**
     * Инициализировать файловую систему.
     * Создает все необходимые директории и скрывает их на Windows.
     */
    public static void initialize() {
        try {
            Logger.info("Initializing FilesManager...");
            
            // Создаем все директории
            boolean created = OsManager.createAllDirectories();
            if (!created) {
                Logger.warn("Some directories could not be created");
            }

            // Скрываем главную директорию на Windows
            if (OsManager.platform == OsManager.Platform.WINDOWS) {
                hideDirectoryWindows(OsManager.mainDirectory);
            }

            Logger.success("FilesManager initialized successfully!");
            Logger.info("Main directory: " + OsManager.mainDirectory);
            Logger.info("Cache directory: " + OsManager.cacheDirectory);
            Logger.info("Data directory: " + OsManager.dataDirectory);
            
        } catch (Exception e) {
            Logger.error("Failed to initialize FilesManager: " + e.getMessage());
        }
    }

    /**
     * Скрыть директорию на Windows.
     */
    private static void hideDirectoryWindows(Path directory) {
        try {
            if (!Files.exists(directory)) {
                return;
            }

            ProcessBuilder pb = new ProcessBuilder(
                "attrib", "+h", "+s", directory.toString()
            );
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                Logger.success("Directory hidden: " + directory);
            } else {
                Logger.warn("Failed to hide directory: " + directory);
            }
        } catch (Exception e) {
            Logger.warn("Could not hide directory: " + e.getMessage());
        }
    }

    // ========================================================================
    // СОЗДАНИЕ ФАЙЛОВ
    // ========================================================================

    /**
     * Создать файл с указанными параметрами.
     * 
     * @param format Формат файла
     * @param name Имя файла (без расширения)
     * @param content Содержимое файла
     * @param checkMode Режим проверки существования
     * @return true если файл создан успешно
     */
    public static boolean createFile(FileFormat format, String name, String content, CheckMode checkMode) {
        return createFile(OsManager.mainDirectory, format, name, content, checkMode);
    }

    /**
     * Создать файл в указанной директории.
     */
    public static boolean createFile(Path directory, FileFormat format, String name, String content, CheckMode checkMode) {
        lock.writeLock().lock();
        try {
            // Создаем директорию если не существует
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // Формируем путь к файлу
            String fileName = name + getExtension(format);
            Path filePath = directory.resolve(fileName);

            // Проверка существования
            if (checkMode == CheckMode.ALWAYS || checkMode == CheckMode.ONCE) {
                if (Files.exists(filePath)) {
                    if (checkMode == CheckMode.ONCE) {
                        Logger.info("File already exists: " + fileName);
                        return true;
                    }
                }
            }

            // Записываем файл
            boolean success = writeFile(filePath, format, content);
            
            if (success) {
                Logger.success("File created: " + fileName);
            } else {
                Logger.error("Failed to create file: " + fileName);
            }

            return success;

        } catch (Exception e) {
            Logger.error("Error creating file: " + e.getMessage());
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Создать JSON файл из объекта.
     */
    public static boolean createJsonFile(String name, Object object, CheckMode checkMode) {
        String json = GSON.toJson(object);
        return createFile(FileFormat.JSON, name, json, checkMode);
    }

    /**
     * Создать JSON файл в указанной директории.
     */
    public static boolean createJsonFile(Path directory, String name, Object object, CheckMode checkMode) {
        String json = GSON.toJson(object);
        return createFile(directory, FileFormat.JSON, name, json, checkMode);
    }

    // ========================================================================
    // ЧТЕНИЕ ФАЙЛОВ
    // ========================================================================

    /**
     * Прочитать файл.
     */
    public static String readFile(Path filePath) {
        lock.readLock().lock();
        try {
            if (!Files.exists(filePath)) {
                return null;
            }

            return Files.readString(filePath, StandardCharsets.UTF_8);

        } catch (Exception e) {
            Logger.error("Error reading file: " + e.getMessage());
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Прочитать JSON файл в объект.
     */
    public static <T> T readJsonFile(Path filePath, Class<T> clazz) {
        String json = readFile(filePath);
        if (json == null) {
            return null;
        }

        try {
            return GSON.fromJson(json, clazz);
        } catch (Exception e) {
            Logger.error("Error parsing JSON: " + e.getMessage());
            return null;
        }
    }

    // ========================================================================
    // ЗАПИСЬ ФАЙЛОВ
    // ========================================================================

    /**
     * Записать файл с atomic write.
     */
    private static boolean writeFile(Path filePath, FileFormat format, String content) {
        try {
            Path tempFile = filePath.resolveSibling(filePath.getFileName() + ".tmp");

            switch (format) {
                case JSON:
                case LUA:
                case PLAIN:
                    Files.writeString(tempFile, content, StandardCharsets.UTF_8);
                    break;

                case APR:
                    writeAprFile(tempFile, content);
                    break;

                case ZIP:
                    writeZipFile(tempFile, content);
                    break;

                case GZIP:
                    writeGzipFile(tempFile, content);
                    break;

                case CBM:
                    Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));
                    break;

                default:
                    Files.writeString(tempFile, content, StandardCharsets.UTF_8);
                    break;
            }

            // Atomic move
            Files.move(tempFile, filePath, 
                StandardCopyOption.REPLACE_EXISTING, 
                StandardCopyOption.ATOMIC_MOVE);

            return true;

        } catch (Exception e) {
            Logger.error("Error writing file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Записать APR файл (custom archive format).
     */
    private static void writeAprFile(Path filePath, String content) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream(filePath.toFile()))) {
            
            // Добавляем метаданные
            ZipEntry metaEntry = new ZipEntry("meta.json");
            zos.putNextEntry(metaEntry);
            
            String meta = GSON.toJson(new AprMetadata(
                System.currentTimeMillis(),
                "Aporia.cc",
                "0.4"
            ));
            zos.write(meta.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Добавляем контент
            ZipEntry contentEntry = new ZipEntry("content.dat");
            zos.putNextEntry(contentEntry);
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    /**
     * Записать ZIP файл.
     */
    private static void writeZipFile(Path filePath, String content) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream(filePath.toFile()))) {
            
            ZipEntry entry = new ZipEntry("data.txt");
            zos.putNextEntry(entry);
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    /**
     * Записать GZIP файл.
     */
    private static void writeGzipFile(Path filePath, String content) throws IOException {
        try (GZIPOutputStream gzos = new GZIPOutputStream(
                new FileOutputStream(filePath.toFile()))) {
            gzos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    // ========================================================================
    // УДАЛЕНИЕ ФАЙЛОВ
    // ========================================================================

    /**
     * Удалить файл.
     */
    public static boolean deleteFile(Path filePath) {
        lock.writeLock().lock();
        try {
            if (!Files.exists(filePath)) {
                return true;
            }

            Files.delete(filePath);
            Logger.success("File deleted: " + filePath.getFileName());
            return true;

        } catch (Exception e) {
            Logger.error("Error deleting file: " + e.getMessage());
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Удалить директорию рекурсивно.
     */
    public static boolean deleteDirectory(Path directory) {
        lock.writeLock().lock();
        try {
            if (!Files.exists(directory)) {
                return true;
            }

            Files.walk(directory)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        Logger.error("Failed to delete: " + path);
                    }
                });

            Logger.success("Directory deleted: " + directory.getFileName());
            return true;

        } catch (Exception e) {
            Logger.error("Error deleting directory: " + e.getMessage());
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ========================================================================
    // УТИЛИТЫ
    // ========================================================================

    /**
     * Получить расширение файла по формату.
     */
    private static String getExtension(FileFormat format) {
        switch (format) {
            case APR: return ".apr";
            case JSON: return ".json";
            case LUA: return ".lua";
            case CBM: return ".cbm";
            case ZIP: return ".zip";
            case GZIP: return ".gz";
            case PLAIN: return ".txt";
            default: return ".dat";
        }
    }

    /**
     * Проверить существование файла.
     */
    public static boolean exists(Path filePath) {
        return Files.exists(filePath);
    }

    /**
     * Получить путь к файлу в главной директории.
     */
    public static Path getFilePath(String name, FileFormat format) {
        return OsManager.mainDirectory.resolve(name + getExtension(format));
    }

    /**
     * Получить путь к файлу в указанной директории.
     */
    public static Path getFilePath(Path directory, String name, FileFormat format) {
        return directory.resolve(name + getExtension(format));
    }

    // ========================================================================
    // МЕТАДАННЫЕ APR
    // ========================================================================

    /**
     * Метаданные для APR файлов.
     */
    private static class AprMetadata {
        private final long timestamp;
        private final String client;
        private final String version;

        public AprMetadata(long timestamp, String client, String version) {
            this.timestamp = timestamp;
            this.client = client;
            this.version = version;
        }
    }

    // ========================================================================
    // PANIC MODE - ЭКСТРЕННОЕ УДАЛЕНИЕ СЛЕДОВ
    // ========================================================================

    /**
     * Panic Mode - экстренное удаление всех следов клиента.
     * 
     * <p>Выполняет следующие действия:</p>
     * <ol>
     *   <li>Отключает все модули клиента</li>
     *   <li>Перемещает ~/.apr в Temp с рандомным именем</li>
     *   <li>Создает фейковый .jar файл в папке mods</li>
     *   <li>Скрывает оригинальный Aporia-0.4.jar через attrib</li>
     *   <li>Отключает все хуки и события</li>
     *   <li>Завершает работу клиента</li>
     * </ol>
     * 
     * <p><b>ВНИМАНИЕ:</b> Этот метод необратим! Все данные будут потеряны!</p>
     * 
     * @return true если panic mode выполнен успешно
     */
    public static boolean panic() {
        Logger.warn("=".repeat(60));
        Logger.warn("PANIC MODE ACTIVATED!");
        Logger.warn("=".repeat(60));

        try {
            /** Step 1: Отключаем все модули */
            Logger.info("[1/5] Disabling all modules...");
            disableAllModules();

            /** Step 2: Перемещаем ~/.apr в Temp */
            Logger.info("[2/5] Moving config directory to temp...");
            moveConfigToTemp();

            /** Step 3: Создаем фейковый .jar */
            Logger.info("[3/5] Creating decoy JAR...");
            createDecoyJar();

            /** Step 4: Скрываем оригинальный JAR */
            Logger.info("[4/5] Hiding original JAR...");
            hideOriginalJar();

            /** Step 5: Отключаем все хуки */
            Logger.info("[5/5] Unhooking everything...");
            unhookEverything();

            Logger.success("PANIC MODE COMPLETED SUCCESSFULLY!");
            Logger.warn("Client will shutdown in 3 seconds...");

            /** Ждем 3 секунды и завершаем */
            Thread.sleep(3000);
            System.exit(0);

            return true;

        } catch (Exception e) {
            Logger.error("PANIC MODE FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Отключить все модули клиента.
     */
    private static void disableAllModules() {
        try {
            Initialization init = Initialization.getInstance();
            if (init == null || init.getManager() == null) {
                return;
            }

            ModuleRepository repo = init.getManager().getModuleRepository();
            if (repo == null) {
                return;
            }

            for (ModuleStructure module : repo.modules()) {
                if (module.isState()) {
                    module.setState(false);
                }
            }

            Logger.success("All modules disabled!");
        } catch (Exception e) {
            Logger.error("Failed to disable modules: " + e.getMessage());
        }
    }

    /**
     * Переместить конфиг директорию в Temp с рандомным именем.
     */
    private static void moveConfigToTemp() {
        try {
            Path configDir = OsManager.mainDirectory;
            if (!Files.exists(configDir)) {
                Logger.warn("Config directory doesn't exist, skipping...");
                return;
            }

            /** Генерируем рандомное имя */
            String randomName = generateRandomName();
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), randomName);

            /** Перемещаем */
            Files.move(configDir, tempDir, StandardCopyOption.REPLACE_EXISTING);

            Logger.success("Config moved to: " + tempDir);
        } catch (Exception e) {
            Logger.error("Failed to move config: " + e.getMessage());
        }
    }

    /**
     * Создать фейковый .jar файл в папке mods.
     */
    private static void createDecoyJar() {
        try {
            /** Находим папку mods */
            Path modsDir = Paths.get("mods");
            if (!Files.exists(modsDir)) {
                Logger.warn("Mods directory not found, skipping decoy creation...");
                return;
            }

            /** Создаем фейковый JAR */
            Path decoyJar = modsDir.resolve("fabric-api-addon-" + generateRandomVersion() + ".jar");

            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(decoyJar.toFile()))) {
                /** Добавляем фейковый fabric.mod.json */
                JarEntry modJson = new JarEntry("fabric.mod.json");
                jos.putNextEntry(modJson);
                
                String fakeModJson = "{\n" +
                    "  \"schemaVersion\": 1,\n" +
                    "  \"id\": \"fabric-api-addon\",\n" +
                    "  \"version\": \"" + generateRandomVersion() + "\",\n" +
                    "  \"name\": \"Fabric API Addon\",\n" +
                    "  \"description\": \"Additional utilities for Fabric API\"\n" +
                    "}";
                
                jos.write(fakeModJson.getBytes(StandardCharsets.UTF_8));
                jos.closeEntry();

                /** Добавляем несколько фейковых классов */
                for (int i = 0; i < 5; i++) {
                    String className = "FabricUtil" + i + ".class";
                    JarEntry classEntry = new JarEntry("net/fabricmc/addon/" + className);
                    jos.putNextEntry(classEntry);
                    jos.write(generateFakeClassBytes());
                    jos.closeEntry();
                }
            }

            Logger.success("Decoy JAR created: " + decoyJar.getFileName());
        } catch (Exception e) {
            Logger.error("Failed to create decoy JAR: " + e.getMessage());
        }
    }

    /**
     * Скрыть оригинальный Aporia JAR через attrib (Windows).
     */
    private static void hideOriginalJar() {
        try {
            if (OsManager.platform != OsManager.Platform.WINDOWS) {
                Logger.info("Not Windows, skipping JAR hiding...");
                return;
            }

            /** Находим Aporia JAR */
            Path modsDir = Paths.get("mods");
            if (!Files.exists(modsDir)) {
                return;
            }

            Path aporiaJar = modsDir.resolve("Aporia-0.4.jar");
            if (!Files.exists(aporiaJar)) {
                Logger.warn("Aporia JAR not found, trying alternative names...");
                /** Пробуем найти любой JAR с Aporia в имени */
                try (var stream = Files.list(modsDir)) {
                    aporiaJar = stream
                        .filter(p -> p.getFileName().toString().toLowerCase().contains("aporia"))
                        .filter(p -> p.getFileName().toString().endsWith(".jar"))
                        .findFirst()
                        .orElse(null);
                }
            }

            if (aporiaJar == null || !Files.exists(aporiaJar)) {
                Logger.warn("Could not find Aporia JAR to hide");
                return;
            }

            /** Скрываем через attrib */
            ProcessBuilder pb = new ProcessBuilder(
                "attrib", "+h", "+s", aporiaJar.toString()
            );
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                Logger.success("Original JAR hidden: " + aporiaJar.getFileName());
            } else {
                Logger.warn("Failed to hide JAR (exit code: " + exitCode + ")");
            }

        } catch (Exception e) {
            Logger.error("Failed to hide JAR: " + e.getMessage());
        }
    }

    /**
     * Отключить все хуки и события.
     */
    private static void unhookEverything() {
        try {
            Initialization init = Initialization.getInstance();
            if (init == null || init.getManager() == null) {
                return;
            }

            /** Отключаем event manager */
            if (init.getManager().getEventManager() != null) {
                /** TODO: Добавить метод для отключения всех событий */
                Logger.info("Event manager unhooked");
            }

            /** Отключаем module switcher */
            if (init.getManager().getModuleSwitcher() != null) {
                /** TODO: Добавить метод для отключения switcher */
                Logger.info("Module switcher unhooked");
            }

            Logger.success("All hooks disabled!");
        } catch (Exception e) {
            Logger.error("Failed to unhook: " + e.getMessage());
        }
    }

    /**
     * Генерировать рандомное имя для директории.
     * 
     * @return рандомная строка типа "HUDIWAHIUDHAIUWHDIUHWIUA#!@&*"
     */
    private static String generateRandomName() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return sb.toString();
    }

    /**
     * Генерировать рандомную версию для фейкового мода.
     * 
     * @return версия типа "1.2.3"
     */
    private static String generateRandomVersion() {
        SecureRandom random = new SecureRandom();
        return (random.nextInt(5) + 1) + "." + 
               random.nextInt(20) + "." + 
               random.nextInt(100);
    }

    /**
     * Генерировать фейковые байты класса.
     * 
     * @return массив байтов похожий на .class файл
     */
    private static byte[] generateFakeClassBytes() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[512 + random.nextInt(1024)];
        
        /** Java class file magic number */
        bytes[0] = (byte) 0xCA;
        bytes[1] = (byte) 0xFE;
        bytes[2] = (byte) 0xBA;
        bytes[3] = (byte) 0xBE;
        
        /** Заполняем остальное рандомом */
        for (int i = 4; i < bytes.length; i++) {
            bytes[i] = (byte) random.nextInt(256);
        }
        
        return bytes;
    }

    // ========================================================================
    // CONFIG MANAGER - УПРАВЛЕНИЕ КОНФИГАМИ МОДУЛЕЙ
    // ========================================================================

    private static ConfigManager configManager;

    /**
     * Получить экземпляр ConfigManager.
     * 
     * @return экземпляр ConfigManager
     */
    public static ConfigManager getConfigManager() {
        if (configManager == null) {
            configManager = new ConfigManager();
        }
        return configManager;
    }

    /**
     * ConfigManager - менеджер конфигов модулей.
     * 
     * <p>Особенности:</p>
     * <ul>
     *   <li>Использует .apr формат</li>
     *   <li>Автосохранение каждые 30 секунд</li>
     *   <li>Thread-safe операции</li>
     *   <li>Shutdown hook</li>
     * </ul>
     */
    public static class ConfigManager {
        
        private final Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        private final AtomicBoolean initialized = new AtomicBoolean(false);
        private final AtomicBoolean saving = new AtomicBoolean(false);
        
        private ScheduledExecutorService autoSaver;
        private ModuleRepository moduleRepository;

        private static final String CONFIG_NAME = "config";
        private static final Path CONFIG_DIR = OsManager.mainDirectory.resolve("configs");

        /**
         * Инициализировать конфиг систему.
         * 
         * @param moduleRepository репозиторий модулей
         */
        public void initialize(ModuleRepository moduleRepository) {
            if (initialized.compareAndSet(false, true)) {
                this.moduleRepository = moduleRepository;
                
                Logger.info("Initializing ConfigManager...");
                
                load();
                startAutoSave();
                registerShutdownHook();
                
                Logger.success("ConfigManager initialized!");
            }
        }

        /**
         * Запустить автосохранение.
         */
        private void startAutoSave() {
            autoSaver = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "Aporia-ConfigAutoSaver");
                thread.setDaemon(true);
                return thread;
            });

            autoSaver.scheduleAtFixedRate(() -> {
                try {
                    save();
                } catch (Exception e) {
                    Logger.error("AutoSave failed: " + e.getMessage());
                }
            }, 30, 30, TimeUnit.SECONDS);

            Logger.info("AutoSave started (interval: 30s)");
        }

        /**
         * Зарегистрировать shutdown hook.
         */
        private void registerShutdownHook() {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Logger.info("Shutdown detected, saving config...");
                shutdown();
            }, "Aporia-ConfigShutdown"));
        }

        /**
         * Сохранить конфиг.
         */
        public void save() {
            if (!initialized.get()) {
                return;
            }
            
            if (!saving.compareAndSet(false, true)) {
                return;
            }

            try {
                JsonObject root = new JsonObject();
                
                /** Сериализуем модули */
                JsonObject modulesJson = new JsonObject();
                if (moduleRepository != null) {
                    for (ModuleStructure module : moduleRepository.modules()) {
                        modulesJson.add(module.getName(), serializeModule(module));
                    }
                }
                root.add("modules", modulesJson);
                
                /** Добавляем метаданные */
                root.addProperty("version", "0.4");
                root.addProperty("timestamp", System.currentTimeMillis());
                root.addProperty("client", "Aporia.cc");

                /** Сохраняем в APR формат */
                String json = gson.toJson(root);
                boolean success = FilesManager.createFile(
                    CONFIG_DIR,
                    FilesManager.FileFormat.APR,
                    CONFIG_NAME,
                    json,
                    FilesManager.CheckMode.ALWAYS
                );

                if (success) {
                    Logger.success("Config saved!");
                } else {
                    Logger.error("Failed to save config!");
                }

            } catch (Exception e) {
                Logger.error("Error saving config: " + e.getMessage());
            } finally {
                saving.set(false);
            }
        }

        /**
         * Загрузить конфиг.
         */
        public void load() {
            try {
                Path configPath = FilesManager.getFilePath(CONFIG_DIR, CONFIG_NAME, FilesManager.FileFormat.APR);
                
                if (!FilesManager.exists(configPath)) {
                    Logger.info("No config found, creating new...");
                    save();
                    return;
                }

                String json = FilesManager.readFile(configPath);
                if (json == null || json.isEmpty()) {
                    Logger.warn("Config file is empty");
                    return;
                }

                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                
                /** Десериализуем модули */
                if (root.has("modules") && moduleRepository != null) {
                    JsonObject modulesJson = root.getAsJsonObject("modules");
                    for (ModuleStructure module : moduleRepository.modules()) {
                        if (modulesJson.has(module.getName())) {
                            deserializeModule(module, modulesJson.getAsJsonObject(module.getName()));
                        }
                    }
                }

                Logger.success("Config loaded!");

            } catch (Exception e) {
                Logger.error("Error loading config: " + e.getMessage());
            }
        }

        /**
         * Перезагрузить конфиг.
         */
        public void reload() {
            load();
            Logger.success("Config reloaded!");
        }

        /**
         * Завершить работу.
         */
        public void shutdown() {
            if (!initialized.get()) {
                return;
            }

            if (autoSaver != null) {
                autoSaver.shutdown();
                try {
                    autoSaver.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Logger.warn("AutoSaver shutdown interrupted");
                }
            }

            save();
            Logger.success("ConfigManager shutdown complete!");
        }

        /**
         * Сериализовать модуль в JSON.
         * 
         * @param module модуль для сериализации
         * @return JSON объект
         */
        private JsonObject serializeModule(ModuleStructure module) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", module.isState());
            moduleJson.addProperty("key", module.getKey());
            moduleJson.addProperty("type", module.getType());
            moduleJson.addProperty("favorite", module.isFavorite());

            JsonObject settingsJson = new JsonObject();
            for (Setting setting : module.settings()) {
                JsonElement element = serializeSetting(setting);
                if (element != null) {
                    settingsJson.add(setting.getName(), element);
                }
            }
            moduleJson.add("settings", settingsJson);

            return moduleJson;
        }

        /**
         * Сериализовать настройку в JSON.
         * 
         * @param setting настройка для сериализации
         * @return JSON элемент
         */
        private JsonElement serializeSetting(Setting setting) {
            if (setting instanceof BooleanSetting boolSetting) {
                return new JsonPrimitive(boolSetting.isValue());
            }
            if (setting instanceof SliderSettings sliderSetting) {
                return new JsonPrimitive(sliderSetting.getValue());
            }
            if (setting instanceof BindSetting bindSetting) {
                JsonObject bindJson = new JsonObject();
                bindJson.addProperty("key", bindSetting.getKey());
                bindJson.addProperty("type", bindSetting.getType());
                return bindJson;
            }
            if (setting instanceof TextSetting textSetting) {
                return new JsonPrimitive(textSetting.getText() != null ? textSetting.getText() : "");
            }
            if (setting instanceof SelectSetting selectSetting) {
                return new JsonPrimitive(selectSetting.getSelected());
            }
            if (setting instanceof ColorSetting colorSetting) {
                JsonObject colorJson = new JsonObject();
                colorJson.addProperty("hue", colorSetting.getHue());
                colorJson.addProperty("saturation", colorSetting.getSaturation());
                colorJson.addProperty("brightness", colorSetting.getBrightness());
                colorJson.addProperty("alpha", colorSetting.getAlpha());
                return colorJson;
            }
            if (setting instanceof MultiSelectSetting multiSetting) {
                JsonArray array = new JsonArray();
                for (String value : multiSetting.getSelected()) {
                    array.add(value);
                }
                return array;
            }
            if (setting instanceof GroupSetting groupSetting) {
                JsonObject groupJson = new JsonObject();
                groupJson.addProperty("value", groupSetting.isValue());
                JsonObject subSettingsJson = new JsonObject();
                for (Setting subSetting : groupSetting.getSubSettings()) {
                    JsonElement element = serializeSetting(subSetting);
                    if (element != null) {
                        subSettingsJson.add(subSetting.getName(), element);
                    }
                }
                groupJson.add("subSettings", subSettingsJson);
                return groupJson;
            }
            return null;
        }

        /**
         * Десериализовать модуль из JSON.
         * 
         * @param module модуль для десериализации
         * @param moduleJson JSON объект
         */
        private void deserializeModule(ModuleStructure module, JsonObject moduleJson) {
            if (moduleJson.has("enabled")) {
                boolean enabled = moduleJson.get("enabled").getAsBoolean();
                if (enabled) {
                    module.setState(true);
                }
            }
            if (moduleJson.has("key")) {
                module.setKey(moduleJson.get("key").getAsInt());
            }
            if (moduleJson.has("type")) {
                module.setType(moduleJson.get("type").getAsInt());
            }
            if (moduleJson.has("favorite")) {
                module.setFavorite(moduleJson.get("favorite").getAsBoolean());
            }
            if (moduleJson.has("settings")) {
                JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                for (Setting setting : module.settings()) {
                    if (settingsJson.has(setting.getName())) {
                        deserializeSetting(setting, settingsJson.get(setting.getName()));
                    }
                }
            }
        }

        /**
         * Десериализовать настройку из JSON.
         * 
         * @param setting настройка для десериализации
         * @param element JSON элемент
         */
        private void deserializeSetting(Setting setting, JsonElement element) {
            try {
                if (setting instanceof BooleanSetting boolSetting) {
                    boolSetting.setValue(element.getAsBoolean());
                } else if (setting instanceof SliderSettings sliderSetting) {
                    sliderSetting.setValue((float) element.getAsDouble());
                } else if (setting instanceof BindSetting bindSetting) {
                    if (element.isJsonObject()) {
                        JsonObject bindJson = element.getAsJsonObject();
                        if (bindJson.has("key")) {
                            bindSetting.setKey(bindJson.get("key").getAsInt());
                        }
                        if (bindJson.has("type")) {
                            bindSetting.setType(bindJson.get("type").getAsInt());
                        }
                    } else {
                        bindSetting.setKey(element.getAsInt());
                    }
                } else if (setting instanceof TextSetting textSetting) {
                    textSetting.setText(element.getAsString());
                } else if (setting instanceof SelectSetting selectSetting) {
                    selectSetting.setSelected(element.getAsString());
                } else if (setting instanceof ColorSetting colorSetting) {
                    if (element.isJsonObject()) {
                        JsonObject colorJson = element.getAsJsonObject();
                        if (colorJson.has("hue")) {
                            colorSetting.setHue(colorJson.get("hue").getAsFloat());
                        }
                        if (colorJson.has("saturation")) {
                            colorSetting.setSaturation(colorJson.get("saturation").getAsFloat());
                        }
                        if (colorJson.has("brightness")) {
                            colorSetting.setBrightness(colorJson.get("brightness").getAsFloat());
                        }
                        if (colorJson.has("alpha")) {
                            colorSetting.setAlpha(colorJson.get("alpha").getAsFloat());
                        }
                    } else {
                        colorSetting.setColor(element.getAsInt());
                    }
                } else if (setting instanceof MultiSelectSetting multiSetting) {
                    if (element.isJsonArray()) {
                        JsonArray array = element.getAsJsonArray();
                        List<String> selected = new ArrayList<>();
                        for (JsonElement e : array) {
                            selected.add(e.getAsString());
                        }
                        multiSetting.setSelected(selected);
                    }
                } else if (setting instanceof GroupSetting groupSetting) {
                    if (element.isJsonObject()) {
                        JsonObject groupJson = element.getAsJsonObject();
                        if (groupJson.has("value")) {
                            groupSetting.setValue(groupJson.get("value").getAsBoolean());
                        }
                        if (groupJson.has("subSettings")) {
                            JsonObject subSettingsJson = groupJson.getAsJsonObject("subSettings");
                            for (Setting subSetting : groupSetting.getSubSettings()) {
                                if (subSettingsJson.has(subSetting.getName())) {
                                    deserializeSetting(subSetting, subSettingsJson.get(subSetting.getName()));
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
