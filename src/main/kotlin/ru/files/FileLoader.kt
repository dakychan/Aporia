package ru.files

import aporia.cc.Logger
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Расширенный загрузчик файлов с поддержкой различных форматов.
 * Поддерживает: JSON, APR (config), CBM (binary), GZIP, текстовые файлы.
 */
class FileLoader {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .setLenient()
        .serializeNulls()
        .create()

    /**
     * Загрузить JSON файл.
     *
     * @param path Путь к файлу
     * @return JsonObject или null если ошибка
     */
    fun loadJson(path: Path): JsonObject? {
        return try {
            if (!Files.exists(path)) {
                Logger.debug("JSON file not found: $path")
                return null
            }
            val content = Files.readString(path, StandardCharsets.UTF_8)
            JsonParser.parseString(content).asJsonObject
        } catch (e: Exception) {
            Logger.error("Failed to load JSON from $path: ${e.message}", e)
            null
        }
    }

    /**
     * Загрузить JSON файл со сжатием GZIP.
     *
     * @param path Путь к .json.gz файлу
     * @return JsonObject или null если ошибка
     */
    fun loadJsonGzip(path: Path): JsonObject? {
        return try {
            if (!Files.exists(path)) return null

            GZIPInputStream(Files.newInputStream(path)).use { gzipStream ->
                BufferedReader(InputStreamReader(gzipStream, StandardCharsets.UTF_8)).use { reader ->
                    val content = reader.readText()
                    JsonParser.parseString(content).asJsonObject
                }
            }
        } catch (e: Exception) {
            Logger.error("Failed to load GZIP JSON from $path: ${e.message}", e)
            null
        }
    }

    /**
     * Загрузить файл в формате APR (Aporia Config Format).
     * Формат: ключ=значение, комментарии через #
     *
     * @param path Путь к .apr файлу
     * @return Map<String, String> или null если ошибка
     */
    fun loadApr(path: Path): Map<String, String>? {
        return try {
            if (!Files.exists(path)) return null
            val content = Files.readString(path, StandardCharsets.UTF_8)
            parseAprFormat(content)
        } catch (e: Exception) {
            Logger.error("Failed to load APR from $path: ${e.message}", e)
            null
        }
    }

    /**
     * Загрузить бинарный файл (CBM - Custom Binary Format).
     *
     * @param path Путь к .cbm файлу
     * @return ByteArray или null если ошибка
     */
    fun loadCbm(path: Path): ByteArray? {
        return try {
            if (!Files.exists(path)) return null
            Files.readAllBytes(path)
        } catch (e: Exception) {
            Logger.error("Failed to load CBM from $path: ${e.message}", e)
            null
        }
    }

    /**
     * Загрузить текстовый файл.
     *
     * @param path Путь к файлу
     * @return Список строк или null если ошибка
     */
    fun loadText(path: Path): List<String>? {
        return try {
            if (!Files.exists(path)) return null
            Files.readAllLines(path, StandardCharsets.UTF_8)
                .filter { it.isNotBlank() && !it.startsWith("#") }
        } catch (e: Exception) {
            Logger.error("Failed to load text from $path: ${e.message}", e)
            null
        }
    }

    /**
     * Загрузить текстовый файл как одну строку.
     *
     * @param path Путь к файлу
     * @return Строка или null если ошибка
     */
    fun loadTextSingle(path: Path): String? {
        return try {
            if (!Files.exists(path)) return null
            Files.readString(path, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Logger.error("Failed to load text from $path: ${e.message}", e)
            null
        }
    }

    /**
     * Сохранить JSON файл.
     *
     * @param path Путь к файлу
     * @param data JsonObject для сохранения
     * @return true если успешно
     */
    fun saveJson(path: Path, data: JsonObject): Boolean {
        return try {
            Files.createDirectories(path.parent)
            val json = gson.toJson(data)
            Files.writeString(path, json, StandardCharsets.UTF_8)
            Logger.debug("✓ JSON saved: $path")
            true
        } catch (e: Exception) {
            Logger.error("Failed to save JSON to $path: ${e.message}", e)
            false
        }
    }

    /**
     * Сохранить JSON файл со сжатием GZIP.
     *
     * @param path Путь к .json.gz файлу
     * @param data JsonObject для сохранения
     * @return true если успешно
     */
    fun saveJsonGzip(path: Path, data: JsonObject): Boolean {
        return try {
            Files.createDirectories(path.parent)
            val json = gson.toJson(data)

            GZIPOutputStream(Files.newOutputStream(path)).use { gzipStream ->
                OutputStreamWriter(gzipStream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(json)
                }
            }

            Logger.debug("✓ GZIP JSON saved: $path")
            true
        } catch (e: Exception) {
            Logger.error("Failed to save GZIP JSON to $path: ${e.message}", e)
            false
        }
    }

    /**
     * Сохранить файл в формате APR.
     *
     * @param path Путь к .apr файлу
     * @param data Map с данными
     * @param header Заголовок файла (комментарий)
     * @return true если успешно
     */
    fun saveApr(path: Path, data: Map<String, String>, header: String = "# Aporia Configuration"): Boolean {
        return try {
            Files.createDirectories(path.parent)
            val content = buildString {
                appendLine(header)
                appendLine("# Generated: ${java.time.LocalDateTime.now()}")
                appendLine()
                data.forEach { (key, value) ->
                    appendLine("$key=$value")
                }
            }
            Files.writeString(path, content, StandardCharsets.UTF_8)
            Logger.debug("✓ APR saved: $path")
            true
        } catch (e: Exception) {
            Logger.error("Failed to save APR to $path: ${e.message}", e)
            false
        }
    }

    /**
     * Сохранить бинарный файл.
     *
     * @param path Путь к файлу
     * @param data ByteArray для сохранения
     * @return true если успешно
     */
    fun saveCbm(path: Path, data: ByteArray): Boolean {
        return try {
            Files.createDirectories(path.parent)
            Files.write(path, data)
            Logger.debug("✓ CBM saved: $path")
            true
        } catch (e: Exception) {
            Logger.error("Failed to save CBM to $path: ${e.message}", e)
            false
        }
    }

    /**
     * Сохранить текстовый файл.
     *
     * @param path Путь к файлу
     * @param lines Список строк
     * @param header Заголовок файла
     * @return true если успешно
     */
    fun saveText(path: Path, lines: List<String>, header: String = ""): Boolean {
        return try {
            Files.createDirectories(path.parent)
            val content = buildString {
                if (header.isNotEmpty()) {
                    appendLine(header)
                    appendLine()
                }
                lines.forEach { appendLine(it) }
            }
            Files.writeString(path, content, StandardCharsets.UTF_8)
            Logger.debug("✓ Text saved: $path")
            true
        } catch (e: Exception) {
            Logger.error("Failed to save text to $path: ${e.message}", e)
            false
        }
    }

    /**
     * Скопировать файл.
     *
     * @param source Исходный путь
     * @param target Целевой путь
     * @param overwrite Перезаписывать если существует
     * @return true если успешно
     */
    fun copyFile(source: Path, target: Path, overwrite: Boolean = false): Boolean {
        return try {
            if (!Files.exists(source)) {
                Logger.warn("Source file not found: $source")
                return false
            }

            if (Files.exists(target) && !overwrite) {
                Logger.warn("Target file exists: $target")
                return false
            }

            Files.createDirectories(target.parent)
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
            Logger.debug("✓ File copied: $source -> $target")
            true
        } catch (e: Exception) {
            Logger.error("Failed to copy file: ${e.message}", e)
            false
        }
    }

    /**
     * Удалить файл.
     *
     * @param path Путь к файлу
     * @return true если успешно
     */
    fun deleteFile(path: Path): Boolean {
        return try {
            if (!Files.exists(path)) return false
            Files.delete(path)
            Logger.debug("✓ File deleted: $path")
            true
        } catch (e: Exception) {
            Logger.error("Failed to delete file $path: ${e.message}", e)
            false
        }
    }

    /**
     * Проверить существование файла.
     *
     * @param path Путь к файлу
     * @return true если файл существует
     */
    fun exists(path: Path): Boolean = Files.exists(path)

    /**
     * Получить размер файла в байтах.
     *
     * @param path Путь к файлу
     * @return Размер файла или -1 если ошибка
     */
    fun getFileSize(path: Path): Long {
        return try {
            if (!Files.exists(path)) return -1
            Files.size(path)
        } catch (e: Exception) {
            Logger.error("Failed to get file size: ${e.message}", e)
            -1
        }
    }

    /**
     * Получить размер файла в читаемом формате.
     *
     * @param path Путь к файлу
     * @return Читаемый размер (например, "1.5 MB")
     */
    fun getFileSizeReadable(path: Path): String {
        val size = getFileSize(path)
        if (size < 0) return "Unknown"
        return formatFileSize(size)
    }

    /**
     * Парсинг APR формата.
     *
     * @param content Содержимое файла
     * @return Map<String, String>
     */
    private fun parseAprFormat(content: String): Map<String, String> {
        return content.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0].trim() to parts[1].trim()
            }
    }

    /**
     * Форматировать размер файла в читаемый вид.
     *
     * @param bytes Размер в байтах
     * @return Читаемый размер
     */
    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return String.format("%.1f %s", size, units[unitIndex])
    }
}
