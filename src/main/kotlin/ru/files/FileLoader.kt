package ru.files

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.nio.file.Files
import java.nio.file.Path

class FileLoader {
    
    private val gson = Gson()
    
    fun loadJson(path: Path): JsonObject? {
        return try {
            if (!Files.exists(path)) return null
            val content = Files.readString(path)
            gson.fromJson(content, JsonObject::class.java)
        } catch (e: Exception) {
            Logger.error("Failed to load JSON from $path: ${e.message}", e)
            null
        }
    }
    
    fun loadApr(path: Path): Map<String, Any>? {
        return try {
            if (!Files.exists(path)) return null
            val content = Files.readString(path)
            parseAprFormat(content)
        } catch (e: Exception) {
            Logger.error("Failed to load APR from $path: ${e.message}", e)
            null
        }
    }
    
    fun loadCbm(path: Path): ByteArray? {
        return try {
            if (!Files.exists(path)) return null
            Files.readAllBytes(path)
        } catch (e: Exception) {
            Logger.error("Failed to load CBM from $path: ${e.message}", e)
            null
        }
    }
    
    private fun parseAprFormat(content: String): Map<String, Any> {
        return content.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0].trim() to parts[1].trim()
            }
    }
}
