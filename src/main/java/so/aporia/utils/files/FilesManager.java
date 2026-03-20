package so.aporia.utils.files;

import aporia.cc.OsManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import so.aporia.utils.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * FilesManager — обёртка для работы с файлами Aporia.
 *
 * Поддерживаемые форматы:
 *   .apr  — архив (zip) с content.dat + time.json
 *   .zip  — стандартный zip
 *   .java — исходники
 *   .lua  — скрипты
 *   .cbm  — CatBoost модели
 *   .cfg  — конфиги
 *
 * Базовая директория:
 *   Windows : ~/.apr  (скрытая через attrib +s +h)
 *   Linux/macOS : ~/.config/apr
 */
public class FilesManager {

    // ─── Форматы ────────────────────────────────────────────────────────────────

    public enum FileType {
        APR, ZIP, JAVA, LUA, CBM, CFG, UNKNOWN;

        public static FileType fromPath(Path path) {
            String name = path.getFileName().toString().toLowerCase();
            if (name.endsWith(".apr"))  return APR;
            if (name.endsWith(".zip"))  return ZIP;
            if (name.endsWith(".java")) return JAVA;
            if (name.endsWith(".lua"))  return LUA;
            if (name.endsWith(".cbm"))  return CBM;
            if (name.endsWith(".cfg"))  return CFG;
            return UNKNOWN;
        }
    }

    // ─── Gson ────────────────────────────────────────────────────────────────────

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ─── Пути ───────────────────────────────────────────────────────────────────

    /**
     * Корневая директория — делегирует OsManager.mainDirectory.
     * Windows: ~/.apr  |  Linux/macOS: ~/.config/apr
     */
    public static final Path ROOT = OsManager.mainDirectory;

    /** config.apr */
    public static final Path CONFIG_FILE = ROOT.resolve("config.apr");

    /** accounts.apr */
    public static final Path ACCOUNTS_FILE = ROOT.resolve("accounts.apr");

    // ─── Инициализация ──────────────────────────────────────────────────────────

    /**
     * Создаёт ROOT директорию, скрывает её на Windows,
     * затем загружает все impl-файлы.
     */
    public static void init() throws IOException {
        if (!Files.exists(ROOT)) {
            Files.createDirectories(ROOT);
            Logger.success("Created root directory: " + ROOT);
            if (OsManager.getPlatform() == OsManager.Platform.WINDOWS) {
                hideWindowsDirectory(ROOT);
            }
        }

        // загружаем все impl
        so.aporia.utils.files.impl.ChatFile.load();
    }

    private static void hideWindowsDirectory(Path path) {
        try {
            Runtime.getRuntime().exec(new String[]{
                "attrib", "+s", "+h", path.toAbsolutePath().toString()
            });
            Logger.debug("Hidden directory: " + path);
        } catch (IOException e) {
            Logger.warn("Failed to hide directory: " + path);
        }
    }

    // ─── Парсинг путей ──────────────────────────────────────────────────────────

    /**
     * Резолвит путь относительно ROOT если он не абсолютный.
     */
    public static Path resolve(String pathStr) {
        Path p = Paths.get(pathStr);
        return p.isAbsolute() ? p : ROOT.resolve(p);
    }

    /**
     * Резолвит путь с заменой ~ на home пользователя.
     */
    public static Path resolveTilde(String pathStr) {
        if (pathStr.startsWith("~")) {
            pathStr = OsManager.userHome.toString() + pathStr.substring(1);
        }
        return Paths.get(pathStr);
    }

    // ─── .apr формат ────────────────────────────────────────────────────────────

    /**
     * Создаёт .apr файл из произвольного контента (байты).
     * Внутри zip: content.dat + time.json
     */
    public static void writeApr(Path aprPath, byte[] content) throws IOException {
        ensureParentDirs(aprPath);
        // читаем старый created ДО того как перезаписываем файл
        String now = OsManager.getCurrentDateTimeIso();
        String created = now;
        if (Files.exists(aprPath)) {
            try {
                String old = readAprTimeJson(aprPath).getOrDefault("created", now);
                if (!old.isEmpty()) created = old;
            } catch (Exception ignored) {}
        }
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(aprPath.toFile()))) {
            zos.putNextEntry(new ZipEntry("content.dat"));
            zos.write(content);
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("time.json"));
            zos.write(buildTimeJson(created, now).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    /**
     * Создаёт .apr файл из строки (UTF-8).
     */
    public static void writeApr(Path aprPath, String content) throws IOException {
        writeApr(aprPath, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Читает content.dat из .apr файла как байты.
     */
    public static byte[] readAprBytes(Path aprPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(aprPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("content.dat")) {
                    return zis.readAllBytes();
                }
            }
        }
        throw new IOException("content.dat not found in " + aprPath);
    }

    /**
     * Читает content.dat из .apr файла как строку (UTF-8).
     */
    public static String readApr(Path aprPath) throws IOException {
        return new String(readAprBytes(aprPath), StandardCharsets.UTF_8);
    }

    /**
     * Читает time.json из .apr файла, возвращает Map с "created" и "modified".
     */
    public static Map<String, String> readAprTimeJson(Path aprPath) throws IOException {
        Map<String, String> result = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(aprPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("time.json")) {
                    String json = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    // простой парсинг без зависимостей
                    result.put("created",  extractJsonValue(json, "created"));
                    result.put("modified", extractJsonValue(json, "modified"));
                    return result;
                }
            }
        }
        return result;
    }

    private static String buildTimeJson(String created, String modified) {
        return "{\n  \"created\": \"" + created + "\",\n  \"modified\": \"" + modified + "\"\n}";
    }

    // ─── Общие файловые операции ────────────────────────────────────────────────

    /** Читает любой файл как байты. */
    public static byte[] readBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    /** Читает любой файл как строку (UTF-8). */
    public static String readText(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /** Записывает байты в файл. */
    public static void writeBytes(Path path, byte[] data) throws IOException {
        ensureParentDirs(path);
        Files.write(path, data);
    }

    /** Записывает строку в файл (UTF-8). */
    public static void writeText(Path path, String text) throws IOException {
        writeBytes(path, text.getBytes(StandardCharsets.UTF_8));
    }

    /** Удаляет файл если существует. */
    public static boolean delete(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    /** Проверяет существование файла. */
    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    /** Возвращает размер файла в байтах, -1 если не существует. */
    public static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }

    /** Копирует файл. */
    public static void copy(Path src, Path dst) throws IOException {
        ensureParentDirs(dst);
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Перемещает файл. */
    public static void move(Path src, Path dst) throws IOException {
        ensureParentDirs(dst);
        Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    // ─── .cfg ───────────────────────────────────────────────────────────────────

    /**
     * Читает .cfg как Map<String, String>.
     * Формат: key=value, строки с # — комментарии.
     */
    public static Map<String, String> readCfg(Path path) throws IOException {
        Map<String, String> map = new HashMap<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq > 0) {
                map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }
        return map;
    }

    /**
     * Записывает Map<String, String> в .cfg файл.
     */
    public static void writeCfg(Path path, Map<String, String> data) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Aporia config\n");
        for (Map.Entry<String, String> e : data.entrySet()) {
            sb.append(e.getKey()).append("=").append(e.getValue()).append("\n");
        }
        writeText(path, sb.toString());
    }

    // ─── Директории ─────────────────────────────────────────────────────────────

    /**
     * Список файлов в директории с фильтром по расширению.
     * extension — например ".apr", ".cfg". Если null — все файлы.
     */
    public static List<Path> listFiles(Path dir, String extension) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> extension == null || p.getFileName().toString().endsWith(extension))
                .collect(Collectors.toList());
        }
    }

    // ─── Append ─────────────────────────────────────────────────────────────────

    /** Дописывает текст в конец файла (UTF-8). */
    public static void append(Path path, String text) throws IOException {
        ensureParentDirs(path);
        Files.write(path, text.getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    // ─── JSON ────────────────────────────────────────────────────────────────────

    /** Сериализует объект в JSON и записывает в файл. */
    public static void writeJson(Path path, Object obj) throws IOException {
        writeText(path, GSON.toJson(obj));
    }

    /** Читает JSON файл и десериализует в указанный тип. */
    public static <T> T readJson(Path path, Class<T> type) throws IOException {
        return GSON.fromJson(readText(path), type);
    }

    // ─── Утилиты ────────────────────────────────────────────────────────────────

    private static void ensureParentDirs(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    /** Минимальный JSON value extractor без зависимостей. */
    private static String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return "";
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return "";
        int end = json.indexOf('"', start + 1);
        if (end < 0) return "";
        return json.substring(start + 1, end);
    }

    /**
     * Возвращает FileType по пути.
     */
    public static FileType getFileType(Path path) {
        return FileType.fromPath(path);
    }
}
