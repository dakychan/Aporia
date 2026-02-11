package aporia.cc.user

import org.lwjgl.system.Platform
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest
import java.util.*
import java.io.File

object UserData {
    private const val CONFIG_DIR = "sorray"
    private const val USER_DATA_FILE = "user.dat"

    // ============= ПОЛУЧЕНИЕ СИСТЕМНОГО ЮЗЕРНЕЙМА =============
    fun getSystemUsername(): String {
        val username = when {
            Platform.get() == Platform.LINUX -> getLinuxUsername()
            Platform.get() == Platform.WINDOWS -> getWindowsUsername()
            Platform.get() == Platform.MACOSX -> getMacUsername()
            else -> null
        }

        return when {
            username.isNullOrBlank() -> generateDefaultUsername()
            username.lowercase() in listOf("root", "admin", "administrator", "sudo") -> generateDefaultUsername()
            else -> username
        }
    }

    private fun getLinuxUsername(): String? {
        return try {
            val process = ProcessBuilder("whoami").start()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readLine()?.trim()
            }
        } catch (e: Exception) {
            System.getProperty("user.name")
        }
    }

    private fun getWindowsUsername(): String? {
        return try {
            val process = ProcessBuilder("whoami").start()
            BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8)).use { reader ->
                reader.readLine()?.trim()?.substringAfter("\\")
            }
        } catch (e: Exception) {
            System.getProperty("user.name")
        }
    }

    private fun getMacUsername(): String? {
        return System.getProperty("user.name")
    }

    private fun generateDefaultUsername(): String {
        val random = Random()
        return "User${random.nextInt(1000, 9999)}"
    }

    // ============= ПОЛУЧЕНИЕ РОЛИ =============
    fun getUserRole(username: String): UserRole {
        return when (username.lowercase()) {
            "daky_chan", "dusky2", "kotay" -> UserRole.DEVELOPER
            else -> UserRole.USER
        }
    }

    // ============= ПОЛУЧЕНИЕ UUID =============
    fun getUserUUID(username: String): String {
        // Используем сжатый числовой UUID
        return UserGenerator.generateCompressedNumericUUID(username)
    }

    // ============= ПОЛУЧЕНИЕ HWID =============
    fun getHardwareId(): String {
        // Сначала пробуем получить системный HWID
        val systemId = UserGenerator.generateSystemHardwareId()
        if (systemId != null && systemId.isNotBlank()) {
            return systemId
        }
        // Если не получилось - генерируем из компонентов
        return UserGenerator.generateHardwareId()
    }

    // ============= ПОЛНЫЕ ДАННЫЕ ПОЛЬЗОВАТЕЛЯ =============
    fun getUserData(): UserDataClass {
        val username = getSystemUsername()
        val uuid = getUserUUID(username)
        val role = getUserRole(username)
        val hardwareId = getHardwareId()

        return UserDataClass(
            username = username,
            uuid = uuid,
            role = role,
            hardwareId = hardwareId
        )
    }
    data class UserDataClass(
        val username: String,
        val uuid: String,
        val role: UserRole,
        val hardwareId: String
    )
}