/*
 * Copyright (c) 2025-2026 BEVoid Project
 * Distributed under the BEVoid Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.files;

import aporia.cc.OsManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import so.aporia.utils.files.impl.ChatFile;
import so.aporia.utils.user.logger.Logger;

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
 * Files manager for Aporia.
 * <p>
 * Supported file formats:
 * <ul>
 *   <li>{@code .apr} — zip archive with content.dat + time.json</li>
 *   <li>{@code .zip} — standard zip</li>
 *   <li>{@code .java} — source files</li>
 *   <li>{@code .lua} — scripts</li>
 *   <li>{@code .cbm} — CatBoost models</li>
 *   <li>{@code .cfg} — config files</li>
 * </ul>
 * <p>
 * Base directory:
 * <ul>
 *   <li>Windows: {@code ~/.apr} (hidden via attrib +s +h)</li>
 *   <li>Linux/macOS: {@code ~/.config/apr}</li>
 * </ul>
 */
public class FilesManager {
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

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Корневая директория — делегирует OsManager.mainDirectory.
     * <p>
     * Root directory — delegates to OsManager.mainDirectory.
     * <ul>
     *   <li>Windows: {@code ~/.apr}</li>
     *   <li>Linux/macOS: {@code ~/.config/apr}</li>
     * </ul>
     */
    public static final Path ROOT = OsManager.mainDirectory;

    /**
     * Файл конфигурации config.apr.
     * <p>
     * Configuration file config.apr.
     */
    public static final Path CONFIG_FILE = ROOT.resolve("config.apr");

    /**
     * Файл аккаунтов accounts.apr.
     * <p>
     * Accounts file accounts.apr.
     */
    public static final Path ACCOUNTS_FILE = ROOT.resolve("accounts.apr");

    /**
     * Создаёт ROOT директорию, скрывает её на Windows, затем загружает все impl-файлы.
     * <p>
     * Creates ROOT directory, hides it on Windows, then loads all impl files.
     */
    public static void init() throws IOException {
        if (!Files.exists(ROOT)) {
            Files.createDirectories(ROOT);
            Logger.success("Created root directory: " + ROOT);
            if (OsManager.getPlatform() == OsManager.Platform.WINDOWS) {
                hideWindowsDirectory(ROOT);
            }
        }
        ChatFile.load();
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

    /**
     * Резолвит путь относительно ROOT если он не абсолютный.
     * <p>
     * Resolves path relative to ROOT if not absolute.
     */
    public static Path resolve(String pathStr) {
        Path p = Paths.get(pathStr);
        return p.isAbsolute() ? p : ROOT.resolve(p);
    }

    /**
     * Резолвит путь с заменой ~ на home пользователя.
     * <p>
     * Resolves path replacing ~ with user home.
     */
    public static Path resolveTilde(String pathStr) {
        if (pathStr.startsWith("~")) {
            pathStr = OsManager.userHome.toString() + pathStr.substring(1);
        }
        return Paths.get(pathStr);
    }

    /**
     * Создаёт .apr файл из произвольного контента (байты).
     * Внутри zip: content.dat + time.json.
     * <p>
     * Creates .apr file from arbitrary content (bytes).
     * Inside zip: content.dat + time.json.
     */
    public static void writeApr(Path aprPath, byte[] content) throws IOException {
        ensureParentDirs(aprPath);
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
     * <p>
     * Creates .apr file from string (UTF-8).
     */
    public static void writeApr(Path aprPath, String content) throws IOException {
        writeApr(aprPath, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Читает content.dat из .apr файла как байты.
     * <p>
     * Reads content.dat from .apr file as bytes.
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
     * <p>
     * Reads content.dat from .apr file as string (UTF-8).
     */
    public static String readApr(Path aprPath) throws IOException {
        return new String(readAprBytes(aprPath), StandardCharsets.UTF_8);
    }

    /**
     * Читает time.json из .apr файла, возвращает Map с "created" и "modified".
     * <p>
     * Reads time.json from .apr file, returns Map with "created" and "modified".
     */
    public static Map<String, String> readAprTimeJson(Path aprPath) throws IOException {
        Map<String, String> result = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(aprPath.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("time.json")) {
                    String json = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
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

    /**
     * Читает любой файл как байты.
     * <p>
     * Reads any file as bytes.
     */
    public static byte[] readBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    /**
     * Читает любой файл как строку (UTF-8).
     * <p>
     * Reads any file as string (UTF-8).
     */
    public static String readText(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * Записывает байты в файл.
     * <p>
     * Writes bytes to file.
     */
    public static void writeBytes(Path path, byte[] data) throws IOException {
        ensureParentDirs(path);
        Files.write(path, data);
    }

    /**
     * Записывает строку в файл (UTF-8).
     * <p>
     * Writes string to file (UTF-8).
     */
    public static void writeText(Path path, String text) throws IOException {
        writeBytes(path, text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Удаляет файл если существует.
     * <p>
     * Deletes file if exists.
     */
    public static boolean delete(Path path) {
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Проверяет существование файла.
     * <p>
     * Checks if file exists.
     */
    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    /**
     * Возвращает размер файла в байтах, -1 если не существует.
     * <p>
     * Returns file size in bytes, -1 if not exists.
     */
    public static long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Копирует файл.
     * <p>
     * Copies file.
     */
    public static void copy(Path src, Path dst) throws IOException {
        ensureParentDirs(dst);
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Перемещает файл.
     * <p>
     * Moves file.
     */
    public static void move(Path src, Path dst) throws IOException {
        ensureParentDirs(dst);
        Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Читает .cfg как Map<String, String>.
     * Формат: key=value, строки с # — комментарии.
     * <p>
     * Reads .cfg as Map<String, String>.
     * Format: key=value, lines starting with # are comments.
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
     * <p>
     * Writes Map<String, String> to .cfg file.
     */
    public static void writeCfg(Path path, Map<String, String> data) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Aporia config\n");
        for (Map.Entry<String, String> e : data.entrySet()) {
            sb.append(e.getKey()).append("=").append(e.getValue()).append("\n");
        }
        writeText(path, sb.toString());
    }

    /**
     * Список файлов в директории с фильтром по расширению.
     * extension — например ".apr", ".cfg". Если null — все файлы.
     * <p>
     * List files in directory filtered by extension.
     * extension — e.g. ".apr", ".cfg". If null — all files.
     */
    public static List<Path> listFiles(Path dir, String extension) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> extension == null || p.getFileName().toString().endsWith(extension))
                .collect(Collectors.toList());
        }
    }

    /**
     * Дописывает текст в конец файла (UTF-8).
     * <p>
     * Appends text to end of file (UTF-8).
     */
    public static void append(Path path, String text) throws IOException {
        ensureParentDirs(path);
        Files.write(path, text.getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Сериализует объект в JSON и записывает в файл.
     * <p>
     * Serializes object to JSON and writes to file.
     */
    public static void writeJson(Path path, Object obj) throws IOException {
        writeText(path, GSON.toJson(obj));
    }

    /**
     * Читает JSON файл и десериализует в указанный тип.
     * <p>
     * Reads JSON file and deserializes to specified type.
     */
    public static <T> T readJson(Path path, Class<T> type) throws IOException {
        return GSON.fromJson(readText(path), type);
    }

    private static void ensureParentDirs(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    /**
     * Минимальный JSON value extractor без зависимостей.
     * <p>
     * Minimal JSON value extractor without dependencies.
     */
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
     * <p>
     * Returns FileType by path.
     */
    public static FileType getFileType(Path path) {
        return FileType.fromPath(path);
    }
}
