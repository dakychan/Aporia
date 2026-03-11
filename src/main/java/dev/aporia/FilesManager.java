package dev.aporia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.*;

import static dev.aporia.OsManager.platform;
import static dev.aporia.OsManager.Platform.WINDOWS;

/**
 * Кроссплатформенный файловый менеджер.
 *
 * <p>Поддерживаемые форматы: APR, ZIP, GZIP, LUA, JAR, JAVA, KT, TXT, JSON, MP3, OGG, WAV, MP4</p>
 *
 * <h2>Разделение ответственности:</h2>
 * <ul>
 *   <li>{@link FileFormat} — технический формат файла (расширение, MIME тип)</li>
 *   <li>{@link AprFileInfo.FileType} — логическое назначение APR контейнера (конфиг, модуль, скрипт)</li>
 * </ul>
 *
 * <h2>Пример использования:</h2>
 * <pre>{@code
 * // Создание APR файла с метаданными
 * AprFileInfo info = AprFileInfo.builder()
 *     .type(AprFileInfo.FileType.CONFIG)
 *     .name("myconfig")
 *     .build();
 * FilesManager.create(Paths.get("config.apr"), info, "data".getBytes());
 *
 * // Чтение любого файла
 * byte[] data = FilesManager.readFile(Paths.get("file.txt"));
 *
 * // Определение формата
 * FileFormat format = FilesManager.detectFormat(Paths.get("script.lua"));
 * // format.extension() -> ".lua"
 * // format.mimeType() -> "text/x-lua"
 *
 * // Алиасы
 * FilesManager.aliases().addAlias("gmc", "/gamemode creative");
 * }</pre>
 */
public final class FilesManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String ALIASES_FILE = "aliases.apr";

    /** Расширения файлов */
    public static final String EXT_APR = ".apr";
    public static final String EXT_ZIP = ".zip";
    public static final String EXT_GZ = ".gz";
    public static final String EXT_LUA = ".lua";
    public static final String EXT_JAR = ".jar";
    public static final String EXT_JAVA = ".java";
    public static final String EXT_KT = ".kt";
    public static final String EXT_TXT = ".txt";
    public static final String EXT_JSON = ".json";
    public static final String EXT_MP3 = ".mp3";
    public static final String EXT_OGG = ".ogg";
    public static final String EXT_WAV = ".wav";
    public static final String EXT_MP4 = ".mp4";
    public static final String EXT_PNG = ".png";
    public static final String EXT_JPG = ".jpg";
    public static final String EXT_PY = ".py";

    private static Path defaultStoragePath;
    private static AliasManager aliasManager;

    private FilesManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ========================================================================
    // ИНИЦИАЛИЗАЦИЯ
    // ========================================================================

    /**
     * Устанавливает путь хранения по умолчанию.
     */
    public static void setDefaultStoragePath(Path path) {
        defaultStoragePath = path;
        executeIfPresent(path, FilesManager::createDirectories);
        executeIfPresent(path, FilesManager::hideDirectory);
    }

    /**
     * Возвращает путь хранения по умолчанию.
     */
    public static Path getDefaultStoragePath() {
        if (defaultStoragePath == null) {
            defaultStoragePath = createDefaultPath();
        }
        return defaultStoragePath;
    }

    private static Path createDefaultPath() {
        Path userHome = Paths.get(System.getProperty("user.home"));
        Path path = (platform == WINDOWS)
            ? userHome.resolve(".apr")
            : userHome.resolve(".config").resolve("apr");
        createDirectories(path);
        hideDirectory(path);
        return path;
    }

    // ========================================================================
    // УПРАВЛЕНИЕ ДИРЕКТОРИЯМИ
    // ========================================================================

    /**
     * Создаёт директорию и все родительские директории.
     */
    public static void createDirectories(Path path) {
        executeSafely((ThrowingRunnable) () -> {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        }, "Failed to create directory: " + path);
    }

    /**
     * Скрывает директорию (Windows: hidden + system, Linux/Mac: . префикс).
     */
    public static void hideDirectory(Path path) {
        if (platform != WINDOWS) return;
        executeSafely((ThrowingRunnable) () -> {
            Files.setAttribute(path, "dos:hidden", true);
            Files.setAttribute(path, "dos:system", true);
            new ProcessBuilder("cmd.exe", "/c", "attrib", "+s", "+h", path.toString()).start();
        }, "Failed to hide directory: " + path);
    }

    /**
     * Показывает скрытую директорию.
     */
    public static void unhideDirectory(Path path) {
        if (platform != WINDOWS) return;
        executeSafely((ThrowingRunnable) () -> {
            Files.setAttribute(path, "dos:hidden", false);
            Files.setAttribute(path, "dos:system", false);
            new ProcessBuilder("cmd.exe", "/c", "attrib", "-s", "-h", path.toString()).start();
        }, "Failed to unhide directory: " + path);
    }

    // ========================================================================
    // CRUD ОПЕРАЦИИ
    // ========================================================================

    /**
     * Создаёт APR файл с метаданными.
     */
    public static Path create(Path path, AprFileInfo info, byte[] content) throws IOException {
        return createAprFile(path, info, content);
    }

    /**
     * Создаёт APR файл из существующего файла.
     */
    public static Path create(Path path, AprFileInfo info, Path contentPath) throws IOException {
        return createAprFile(path, info, Files.readAllBytes(contentPath));
    }

    /**
     * Читает APR файл.
     */
    public static AprData read(Path path) throws IOException {
        return readAprFile(path);
    }

    /**
     * Обновляет содержимое APR файла.
     */
    public static void update(Path path, byte[] newContent) throws IOException {
        AprData data = readAprFile(path);
        AprFileInfo updatedInfo = AprFileInfo.builder()
            .version(data.info.getVersion())
            .type(AprFileInfo.FileType.fromString(data.info.getType()))
            .name(data.info.getName())
            .description(data.info.getDescription())
            .createdAt(data.info.getCreatedAt())
            .author(data.info.getAuthor())
            .size(newContent.length)
            .build();
        createAprFile(path, updatedInfo, newContent);
    }

    /**
     * Обновляет метаданные APR файла.
     */
    public static void updateMetadata(Path path, AprFileInfo newInfo) throws IOException {
        AprData data = readAprFile(path);
        AprFileInfo updatedInfo = AprFileInfo.builder()
            .version(newInfo.getVersion())
            .type(AprFileInfo.FileType.fromString(newInfo.getType()))
            .name(newInfo.getName())
            .description(newInfo.getDescription())
            .createdAt(newInfo.getCreatedAt())
            .author(newInfo.getAuthor())
            .size(data.content.length)
            .build();
        createAprFile(path, updatedInfo, data.content);
    }

    /**
     * Удаляет файл.
     */
    public static boolean delete(Path path) throws IOException {
        return Files.deleteIfExists(path);
    }

    /**
     * Проверяет существование файла.
     */
    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    /**
     * Читает любой файл в байты.
     */
    public static byte[] readFile(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    /**
     * Записывает байты в файл.
     */
    public static Path writeFile(Path path, byte[] content) throws IOException {
        createDirectories(path.getParent());
        return Files.write(path, content);
    }

    // ========================================================================
    // APR ФОРМАТ
    // ========================================================================

    private static Path createAprFile(Path path, AprFileInfo info, byte[] content) throws IOException {
        Path resolved = ensureExtension(path, EXT_APR);
        createDirectories(resolved.getParent());

        AprFileInfo finalInfo = AprFileInfo.builder()
            .version(info.getVersion())
            .type(AprFileInfo.FileType.fromString(info.getType()))
            .name(info.getName())
            .description(info.getDescription())
            .createdAt(info.getCreatedAt())
            .author(info.getAuthor())
            .size(content.length)
            .build();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(resolved.toFile()))) {
            writeZipEntry(zos, "info.json", finalInfo.toJson().getBytes());
            writeZipEntry(zos, "content.dat", content);
        }

        hideFile(resolved);
        return resolved;
    }

    private static AprData readAprFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new FileNotFoundException("APR file not found: " + path);
        }

        AprFileInfo info = null;
        byte[] content = null;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(path.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if ("info.json".equals(name)) {
                    info = AprFileInfo.fromJson(new String(zis.readAllBytes()));
                } else if ("content.dat".equals(name)) {
                    content = zis.readAllBytes();
                }
                zis.closeEntry();
            }
        }

        if (info == null || content == null) {
            throw new IOException("Invalid APR file structure");
        }

        return new AprData(info, content);
    }

    private static void hideFile(Path path) {
        if (platform != WINDOWS) return;
        executeSafely((ThrowingRunnable) () -> Files.setAttribute(path, "dos:hidden", true), null);
    }

    // ========================================================================
    // ZIP ФОРМАТ
    // ========================================================================

    /**
     * Создаёт ZIP архив.
     */
    public static Path createZip(Path path, byte[] content) throws IOException {
        return createZip(path, Map.of("data.bin", content));
    }

    /**
     * Создаёт ZIP архив с несколькими файлами.
     */
    public static Path createZip(Path path, Map<String, byte[]> entries) throws IOException {
        Path resolved = ensureExtension(path, EXT_ZIP);
        createDirectories(resolved.getParent());

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(resolved.toFile()))) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                writeZipEntry(zos, entry.getKey(), entry.getValue());
            }
        }

        return resolved;
    }

    /**
     * Извлекает все записи из ZIP архива.
     */
    public static Map<String, byte[]> readZip(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new FileNotFoundException("ZIP file not found: " + path);
        }

        Map<String, byte[]> entries = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(path.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.put(entry.getName(), zis.readAllBytes());
                zis.closeEntry();
            }
        }

        return entries;
    }

    // ========================================================================
    // GZIP ФОРМАТ
    // ========================================================================

    /**
     * Сжимает данные в GZIP.
     */
    public static byte[] compressGzip(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(data);
        }
        return baos.toByteArray();
    }

    /**
     * Разжимает GZIP данные.
     */
    public static byte[] decompressGzip(byte[] compressed) throws IOException {
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return gis.readAllBytes();
        }
    }

    /**
     * Создаёт GZIP файл.
     */
    public static Path createGzip(Path path, byte[] content) throws IOException {
        Path resolved = ensureExtension(path, EXT_GZ);
        createDirectories(resolved.getParent());
        Files.write(resolved, compressGzip(content));
        return resolved;
    }

    /**
     * Читает GZIP файл.
     */
    public static byte[] readGzip(Path path) throws IOException {
        return decompressGzip(Files.readAllBytes(path));
    }

    // ========================================================================
    // УТИЛИТЫ
    // ========================================================================

    /**
     * Определяет формат файла по расширению.
     *
     * @return {@link FileFormat} с информацией о расширении и MIME типе
     */
    public static FileFormat detectFormat(Path path) {
        return FileFormat.fromPath(path);
    }

    /**
     * Определяет расширение файла.
     */
    public static String detectExtension(Path path) {
        return detectFormat(path).extension();
    }

    /**
     * Возвращает MIME тип файла.
     */
    public static String getMimeType(Path path) {
        return detectFormat(path).mimeType();
    }

    /**
     * Проверяет, является ли файл текстовым.
     */
    public static boolean isTextFile(Path path) {
        FileFormat.Category cat = detectFormat(path).category();
        return cat == FileFormat.Category.TEXT || cat == FileFormat.Category.SOURCE || cat == FileFormat.Category.SCRIPT;
    }

    /**
     * Проверяет, является ли файл медиа.
     */
    public static boolean isMediaFile(Path path) {
        FileFormat.Category cat = detectFormat(path).category();
        return cat == FileFormat.Category.AUDIO || cat == FileFormat.Category.VIDEO || cat == FileFormat.Category.IMAGE;
    }

    /**
     * Проверяет, является ли файл архивом.
     */
    public static boolean isArchive(Path path) {
        return detectFormat(path).category() == FileFormat.Category.ARCHIVE;
    }

    private static Path ensureExtension(Path path, String extension) {
        return path.toString().toLowerCase().endsWith(extension)
            ? path
            : Paths.get(path.toString() + extension);
    }

    private static void writeZipEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(data);
        zos.closeEntry();
    }

    private static void executeSafely(ThrowingRunnable action, String errorMessage) {
        try {
            action.run();
        } catch (Exception e) {
            if (errorMessage != null) {
                System.err.println("[FilesManager] " + errorMessage);
            }
        }
    }

    private static void executeIfPresent(Path path, java.util.function.Consumer<Path> action) {
        if (path != null) action.accept(path);
    }

    // ========================================================================
    // ALIAS MANAGER
    // ========================================================================

    /**
     * Возвращает менеджер алиасов.
     */
    public static AliasManager aliases() {
        if (aliasManager == null) {
            aliasManager = new AliasManager();
        }
        return aliasManager;
    }

    /**
     * Менеджер алиасов команд с автосохранением в APR файл.
     */
    public static class AliasManager {

        private final Map<String, String> aliases = new LinkedHashMap<>();
        private final Path storagePath = getDefaultStoragePath();

        public AliasManager() {
            loadAliases();
        }

        public void loadAliases() {
            Path aliasesPath = storagePath.resolve(ALIASES_FILE);
            if (!Files.exists(aliasesPath)) return;

            try {
                AprData data = readAprFile(aliasesPath);
                String json = new String(data.getContent());
                @SuppressWarnings("unchecked")
                Map<String, String> loaded = GSON.fromJson(json, Map.class);
                if (loaded != null) {
                    aliases.putAll(loaded);
                }
            } catch (IOException e) {
                System.err.println("[FilesManager] Failed to load aliases");
            }
        }

        public void saveAliases() {
            try {
                String json = GSON.toJson(aliases);

                AprFileInfo info = AprFileInfo.builder()
                    .version("2.0")
                    .type(AprFileInfo.FileType.CONFIG)
                    .name("aliases")
                    .description("Command aliases for Aporia client")
                    .author("Aporia")
                    .build();

                createAprFile(storagePath.resolve(ALIASES_FILE), info, json.getBytes());
            } catch (IOException e) {
                System.err.println("[FilesManager] Failed to save aliases");
            }
        }

        public void addAlias(String name, String command) {
            aliases.put(name, command);
            saveAliases();
        }

        public void removeAlias(String name) {
            if (aliases.remove(name) != null) saveAliases();
        }

        public String getCommand(String name) {
            return aliases.get(name);
        }

        public boolean hasAlias(String name) {
            return aliases.containsKey(name);
        }

        public Map<String, String> getAllAliases() {
            return Collections.unmodifiableMap(aliases);
        }

        public void clearAliases() {
            aliases.clear();
            saveAliases();
        }

        public int count() {
            return aliases.size();
        }
    }

    // ========================================================================
    // APR FILE INFO
    // ========================================================================

    /**
     * Метаданные APR файла.
     *
     * <p>Важно: {@link FileType} здесь описывает <b>логическое назначение</b> APR контейнера
     * (конфигурация, модуль, скрипт), а не технический формат файла.</p>
     *
     * @see FileFormat для определения технического формата по расширению
     */
    public static class AprFileInfo {

        @SerializedName("version") private final String version;
        @SerializedName("type") private final String type;
        @SerializedName("name") private final String name;
        @SerializedName("description") private final String description;
        @SerializedName("created_at") private final String createdAt;
        @SerializedName("modified_at") private String modifiedAt;
        @SerializedName("author") private final String author;
        @SerializedName("size") private long size;

        private AprFileInfo(Builder builder) {
            version = builder.version;
            type = builder.type != null ? builder.type.name() : FileType.UNKNOWN.name();
            name = builder.name;
            description = builder.description;
            createdAt = builder.createdAt;
            modifiedAt = builder.modifiedAt;
            author = builder.author;
            size = builder.size;
        }

        public static Builder builder() { return new Builder(); }

        public static AprFileInfo fromJson(String json) {
            return GSON.fromJson(json, AprFileInfo.class);
        }

        public String toJson() { return GSON.toJson(this); }

        public String getVersion() { return version; }

        /**
         * @return логический тип назначения (CONFIG, MODULE, SCRIPT, etc.)
         */
        public String getType() { return type; }

        /**
         * @return FileType enum для логического типа
         */
        public FileType getFileType() {
            return FileType.fromString(type);
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getCreatedAt() { return createdAt; }
        public String getModifiedAt() { return modifiedAt; }
        public String getAuthor() { return author; }
        public long getSize() { return size; }

        public void updateModified() {
            modifiedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        /**
         * Логические типы назначения APR файлов.
         *
         * <p>Не путать с {@link FileFormat} — это описывает назначение контейнера,
         * а не технический формат файла.</p>
         */
        public enum FileType {
            CONFIG("Конфигурация"),
            MODULE("Модуль"),
            THEME("Тема"),
            SCRIPT("Скрипт"),
            BACKUP("Резервная копия"),
            PROFILE("Профиль"),
            DATA("Данные"),
            UNKNOWN("Неизвестно");

            private final String displayName;

            FileType(String displayName) {
                this.displayName = displayName;
            }

            public String getDisplayName() { return displayName; }

            public static FileType fromString(String value) {
                if (value == null) return UNKNOWN;
                for (FileType type : values()) {
                    if (type.name().equalsIgnoreCase(value)) return type;
                }
                return UNKNOWN;
            }
        }

        public static class Builder {
            private String version = "1.0";
            private FileType type = FileType.UNKNOWN;
            private String name = "unnamed";
            private String description = "";
            private String createdAt;
            private String modifiedAt;
            private String author = "unknown";
            private long size = 0;

            public Builder version(String v) { version = v; return this; }
            public Builder type(FileType t) { type = t; return this; }
            public Builder name(String n) { name = n; return this; }
            public Builder description(String d) { description = d; return this; }
            public Builder createdAt(String c) { createdAt = c; return this; }
            public Builder modifiedAt(String m) { modifiedAt = m; return this; }
            public Builder author(String a) { author = a; return this; }
            public Builder size(long s) { size = s; return this; }

            public AprFileInfo build() {
                if (createdAt == null) createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                if (modifiedAt == null) modifiedAt = createdAt;
                return new AprFileInfo(this);
            }
        }
    }

    // ========================================================================
    // APR DATA
    // ========================================================================

    /**
     * Данные APR файла.
     */
    public record AprData(AprFileInfo info, byte[] content) {
        public AprFileInfo getInfo() { return info; }
        public byte[] getContent() { return content; }
    }

    // ========================================================================
    // FILE FORMAT (TECHNICAL)
    // ========================================================================

    /**
     * Технический формат файла (расширение, MIME тип, категория).
     *
     * <p>Определяет физический формат файла по расширению, а не его логическое назначение.</p>
     *
     * <h2>Пример добавления нового формата:</h2>
     * <pre>{@code
     * // Добавь в enum:
     * PYTHON(".py", "text/x-python", Category.SCRIPT)
     * }</pre>
     *
     * @see AprFileInfo.FileType для логического назначения APR контейнеров
     */
    public enum FileFormat {
        // Archive
        APR(EXT_APR, "application/x-apr", Category.ARCHIVE),
        ZIP(EXT_ZIP, "application/zip", Category.ARCHIVE),
        GZIP(EXT_GZ, "application/gzip", Category.ARCHIVE),
        JAR(EXT_JAR, "application/java-archive", Category.ARCHIVE),

        // Script
        LUA(EXT_LUA, "text/x-lua", Category.SCRIPT),
        PYTHON(EXT_PY, "text/x-python", Category.SCRIPT),

        // Source
        JAVA(EXT_JAVA, "text/x-java-source", Category.SOURCE),
        KOTLIN(EXT_KT, "text/x-kotlin", Category.SOURCE),

        // Text
        TXT(EXT_TXT, "text/plain", Category.TEXT),
        JSON(EXT_JSON, "application/json", Category.TEXT),

        // Audio
        MP3(EXT_MP3, "audio/mpeg", Category.AUDIO),
        OGG(EXT_OGG, "audio/ogg", Category.AUDIO),
        WAV(EXT_WAV, "audio/wav", Category.AUDIO),

        // Video
        MP4(EXT_MP4, "video/mp4", Category.VIDEO),

        // Image
        PNG(EXT_PNG, "image/png", Category.IMAGE),
        JPG(EXT_JPG, "image/jpeg", Category.IMAGE),

        // Unknown
        UNKNOWN("", "application/octet-stream", Category.UNKNOWN);

        private final String extension;
        private final String mimeType;
        private final Category category;

        FileFormat(String extension, String mimeType, Category category) {
            this.extension = extension;
            this.mimeType = mimeType;
            this.category = category;
        }

        /**
         * @return расширение файла (например, ".java")
         */
        public String extension() { return extension; }

        /**
         * @return MIME тип (например, "text/x-java-source")
         */
        public String mimeType() { return mimeType; }

        /**
         * @return категория формата
         */
        public Category category() { return category; }

        /**
         * Проверяет, соответствует ли имя файла этому формату.
         */
        public boolean matches(String filename) {
            return filename.toLowerCase().endsWith(extension);
        }

        /**
         * Определяет формат файла по пути.
         */
        public static FileFormat fromPath(Path path) {
            String name = path.toString().toLowerCase();
            for (FileFormat format : values()) {
                if (format != UNKNOWN && format.matches(name)) {
                    return format;
                }
            }
            return UNKNOWN;
        }

        /**
         * Категории форматов файлов.
         */
        public enum Category {
            ARCHIVE("Архив"),
            SCRIPT("Скрипт"),
            SOURCE("Исходный код"),
            TEXT("Текст"),
            AUDIO("Аудио"),
            VIDEO("Видео"),
            IMAGE("Изображение"),
            UNKNOWN("Неизвестно");

            private final String displayName;

            Category(String displayName) {
                this.displayName = displayName;
            }

            public String displayName() { return displayName; }
        }
    }

    // ========================================================================
    // FUNCTIONAL INTERFACES
    // ========================================================================

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
