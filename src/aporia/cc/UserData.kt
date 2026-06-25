package aporia.cc

import com.chaos.annotation.Obfuscate
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Random

@Obfuscate
object UserData {

    private const val CONFIG_DIR = "sorray"
    private const val USER_DATA_FILE = "user.dat"

    @JvmStatic
    fun getSystemUsername(): String {
        val os = System.getProperty("os.name").lowercase()
        val username = when {
            os.contains("linux") -> getLinuxUsername()
            os.contains("win") -> getWindowsUsername()
            os.contains("mac") -> getMacUsername()
            else -> null
        }

        if (username.isNullOrBlank()) {
            return generateDefaultUsername()
        }

        val lowerUsername = username.lowercase()
        if (listOf("root", "admin", "administrator", "sudo").contains(lowerUsername)) {
            return generateDefaultUsername()
        }

        return username
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
                reader.readLine()?.let { line ->
                    val trimmed = line.trim()
                    val backslashIndex = trimmed.indexOf('\\')
                    if (backslashIndex >= 0) {
                        trimmed.substring(backslashIndex + 1)
                    } else {
                        trimmed
                    }
                }
            }
        } catch (e: Exception) {
            System.getProperty("user.name")
        }
    }

    private fun getMacUsername(): String? = System.getProperty("user.name")

    private fun generateDefaultUsername(): String {
        val random = Random()
        return "User${random.nextInt(1000, 9999)}"
    }

    @JvmStatic
    fun getUserRole(username: String): UserRole {
        return when (username.lowercase()) {
            "daky_chan", "dusky2", "kotay" -> UserRole.DEVELOPER
            else -> UserRole.USER
        }
    }

    @JvmStatic
    fun getUserUUID(username: String): String {
        return UserGenerator.generateCompressedNumericUUID(username)
    }

    @JvmStatic
    fun getHardwareId(): String {
        val systemId = UserGenerator.generateSystemHardwareId()
        return if (!systemId.isNullOrBlank()) systemId else UserGenerator.generateHardwareId()
    }

    @JvmStatic
    fun getUserData(): UserDataClass {
        val username = getSystemUsername()
        val uuid = getUserUUID(username)
        val role = getUserRole(username)
        val hardwareId = getHardwareId()

        return UserDataClass(username, uuid, role, hardwareId)
    }

    class UserDataClass(
        private val username: String,
        private val uuid: String,
        private val role: UserRole,
        private val hardwareId: String
    ) {
        fun getUsername(): String = username
        fun getUuid(): String = uuid
        fun getRole(): UserRole = role
        fun getHardwareId(): String = hardwareId

        override fun toString(): String {
            return "UserDataClass{" +
                    "username='$username', " +
                    "uuid='$uuid', " +
                    "role=$role, " +
                    "hardwareId='$hardwareId'" +
                    "}"
        }
    }

    enum class UserRole {
        USER,
        DEVELOPER,
        ADMIN,
        CONTRIBUTOR
    }
}
