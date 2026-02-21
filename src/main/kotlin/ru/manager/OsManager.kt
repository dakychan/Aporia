package ru.manager

import aporia.cc.Logger
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Operating System Manager - полноценное управление системой.
 *
 * Возможности:
 * - Определение ОС и версии
 * - Определение архитектуры
 * - Проверка путей и директорий
 * - Проверка пользователя (root/admin)
 * - Определение VM/Container
 * - Информация о CPU, GPU, RAM, диске
 * - Погода и геолокация
 * - Дата/время
 */
object OsManager {

    // ========================================================================
    // 1. ПОЛУЧЕНИЕ ОС И ВЕРСИИ
    // ========================================================================

    /**
     * Поддерживаемые платформы.
     */
    enum class Platform {
        WINDOWS,
        LINUX,
        MAC,
        UNKNOWN
    }

    /** Текущая платформа */
    val platform: Platform = detectPlatform()

    /** Полное имя ОС */
    val osName: String = System.getProperty("os.name")

    /** Версия ОС */
    val osVersion: String = System.getProperty("os.version")

    /** Архитектура ОС */
    val osArch: String = System.getProperty("os.arch")

    /**
     * Получить детальную информацию об ОС.
     */
    fun getOsDetails(): OsDetails {
        return OsDetails(
            platform = platform,
            name = osName,
            version = osVersion,
            arch = osArch,
            kernel = getKernelVersion(),
            is64Bit = is64Bit(),
            isVM = isVirtualMachine(),
            isContainer = isContainer()
        )
    }

    /**
     * Получить версию ядра.
     */
    private fun getKernelVersion(): String {
        return try {
            when (platform) {
                Platform.WINDOWS -> System.getProperty("os.version")
                Platform.LINUX -> runCommand("uname", "-r") ?: "unknown"
                Platform.MAC -> runCommand("uname", "-r") ?: "unknown"
                Platform.UNKNOWN -> "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Проверить, 64-битная ли система.
     */
    fun is64Bit(): Boolean {
        val arch = osArch.lowercase(Locale.ROOT)
        return arch.contains("64") || 
               arch.contains("x86_64") || 
               arch.contains("amd64") || 
               arch.contains("aarch64")
    }

    // ========================================================================
    // 2. ПОЛУЧЕНИЕ АРХИТЕКТУРЫ И ПОНИМАНИЕ СИСТЕМЫ
    // ========================================================================

    /**
     * Тип архитектуры процессора.
     */
    enum class CpuArch {
        X86,
        X86_64,
        ARM,
        ARM64,
        UNKNOWN
    }

    /**
     * Получить архитектуру процессора.
     */
    fun getCpuArch(): CpuArch {
        val arch = osArch.lowercase(Locale.ROOT)
        return when {
            arch.contains("aarch64") || arch.contains("arm64") -> CpuArch.ARM64
            arch.contains("arm") -> CpuArch.ARM
            arch.contains("64") -> CpuArch.X86_64
            arch.contains("x86") -> CpuArch.X86
            else -> CpuArch.UNKNOWN
        }
    }

    /**
     * Информация о процессоре.
     */
    data class CpuInfo(
        val name: String,
        val cores: Int,
        val threads: Int,
        val frequency: String,
        val architecture: CpuArch
    )

    /**
     * Получить информацию о процессоре.
     */
    fun getCpuInfo(): CpuInfo {
        val cores = Runtime.getRuntime().availableProcessors()
        val threads = cores // Для Java threads = cores (hyperthreading не виден)
        val arch = getCpuArch()
        val name = getCpuName()
        val frequency = getCpuFrequency()

        return CpuInfo(name, cores, threads, frequency, arch)
    }

    /**
     * Получить частоту процессора.
     */
    private fun getCpuFrequency(): String {
        return try {
            when (platform) {
                Platform.LINUX -> {
                    // Читаем напрямую из /proc/cpuinfo - быстро
                    val freq = File("/proc/cpuinfo").readLines()
                        .find { it.startsWith("cpu MHz", ignoreCase = true) }
                        ?.substringAfter(":")
                        ?.trim()
                        ?.toFloatOrNull()
                    freq?.let { "${(it / 1000).toString().take(3)} GHz" } ?: "Unknown"
                }
                Platform.WINDOWS -> {
                    // PowerShell вместо wmic
                    runCommandWithTimeout(
                        arrayOf("powershell", "-Command", "Get-CimInstance Win32_Processor | Select-Object -ExpandProperty MaxClockSpeed"),
                        timeoutMs = 3000
                    )?.lines()
                        ?.filter { it.isNotBlank() && it.all { c -> c.isDigit() } }
                        ?.firstOrNull()
                        ?.let { "${it.toInt() / 1000} GHz" }
                        ?: "Unknown"
                }
                Platform.MAC -> "Unknown"
                Platform.UNKNOWN -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // ========================================================================
    // 3. ПРОВЕРКА ПУТЕЙ ДО НУЖНЫХ ПАПОК
    // ========================================================================

    /**
     * Типы директорий.
     */
    enum class DirectoryType {
        CONFIG,
        CACHE,
        DATA,
        LOGS,
        TEMP,
        BACKUP,
        MODULES,
        THEMES,
        SCRIPTS
    }

    /** Домашняя директория пользователя */
    val userHome: Path = Paths.get(System.getProperty("user.home"))

    /**
     * Основная директория для конфигов.
     */
    val mainDirectory: Path = when (platform) {
        Platform.WINDOWS -> userHome.resolve(".apr")
        Platform.LINUX, Platform.MAC -> userHome.resolve(".config").resolve(".apr")
        Platform.UNKNOWN -> userHome.resolve(".apr")
    }

    /**
     * Директория кэша.
     */
    val cacheDirectory: Path = when (platform) {
        Platform.WINDOWS -> Paths.get(System.getenv("APPDATA") ?: userHome.toString(), "Aporia.cc")
        Platform.LINUX -> userHome.resolve(".cache").resolve("Aporia")
        Platform.MAC -> userHome.resolve("Library").resolve("Caches").resolve("Aporia")
        Platform.UNKNOWN -> userHome.resolve(".cache").resolve("Aporia")
    }

    /**
     * Директория данных.
     */
    val dataDirectory: Path = when (platform) {
        Platform.WINDOWS -> Paths.get(System.getenv("LOCALAPPDATA") ?: userHome.toString(), "Aporia")
        Platform.LINUX -> userHome.resolve(".local").resolve("share").resolve("Aporia")
        Platform.MAC -> userHome.resolve("Library").resolve("Application Support").resolve("Aporia")
        Platform.UNKNOWN -> userHome.resolve(".local").resolve("share").resolve("Aporia")
    }

    /**
     * Директория логов.
     */
    val logsDirectory: Path = when (platform) {
        Platform.WINDOWS, Platform.LINUX -> dataDirectory.resolve("logs")
        Platform.MAC -> userHome.resolve("Library").resolve("Logs").resolve("Aporia")
        Platform.UNKNOWN -> dataDirectory.resolve("logs")
    }

    /**
     * Временная директория.
     */
    val tempDirectory: Path = cacheDirectory.resolve("temp")

    /**
     * Директория резервных копий.
     */
    val backupDirectory: Path = mainDirectory.resolve("backup")

    /**
     * Директория модулей.
     */
    val modulesDirectory: Path = mainDirectory.resolve("modules")

    /**
     * Директория тем.
     */
    val themesDirectory: Path = mainDirectory.resolve("themes")

    /**
     * Директория скриптов.
     */
    val scriptsDirectory: Path = mainDirectory.resolve("scripts")

    /**
     * Получить директорию по типу.
     */
    fun getDirectory(type: DirectoryType): Path = when (type) {
        DirectoryType.CONFIG -> mainDirectory
        DirectoryType.CACHE -> cacheDirectory
        DirectoryType.DATA -> dataDirectory
        DirectoryType.LOGS -> logsDirectory
        DirectoryType.TEMP -> tempDirectory
        DirectoryType.BACKUP -> backupDirectory
        DirectoryType.MODULES -> modulesDirectory
        DirectoryType.THEMES -> themesDirectory
        DirectoryType.SCRIPTS -> scriptsDirectory
    }

    /**
     * Получить файл в директории.
     */
    fun getFile(type: DirectoryType, fileName: String): Path = getDirectory(type).resolve(fileName)

    /**
     * Проверить существование пути.
     */
    fun pathExists(path: Path): Boolean = path.toFile().exists()

    /**
     * Проверить, доступна ли директория для записи.
     */
    fun isWritable(path: Path): Boolean {
        return try {
            if (!path.toFile().exists()) {
                path.toFile().mkdirs()
            }
            path.toFile().canWrite()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Создать директорию.
     */
    fun createDirectory(path: Path): Boolean {
        return try {
            if (!path.toFile().exists()) {
                path.toFile().mkdirs()
            }
            path.toFile().exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Создать все необходимые директории.
     */
    fun createAllDirectories(): Boolean {
        return try {
            val dirs = listOf(
                mainDirectory, cacheDirectory, dataDirectory, logsDirectory,
                tempDirectory, backupDirectory, modulesDirectory, themesDirectory, scriptsDirectory
            )
            dirs.all { createDirectory(it) }
        } catch (e: Exception) {
            false
        }
    }

    /** ========================================================================
     * 4. ПРОВЕРКА ПОЛЬЗОВАТЕЛЯ
     * ========================================================================
     *
     * Имя текущего пользователя.
     */
    val userName: String = System.getProperty("user.name")

    /**
     * Домашняя директория пользователя.
     */
    val userDir: String = System.getProperty("user.dir")

    /**
     * Получить информацию о пользователе.
     */
    fun getUserInfo(): UserInfo {
        return UserInfo(
            name = userName,
            homeDir = userHome.toString(),
            isRoot = isRoot(),
            isAdmin = isAdmin(),
            isVM = isVirtualMachine(),
            isContainer = isContainer()
        )
    }

    /**
     * Проверить, запущено ли от root (Linux/Mac).
     */
    fun isRoot(): Boolean {
        return try {
            when (platform) {
                Platform.LINUX, Platform.MAC -> {
                    val user = runCommand("whoami")?.trim()?.lowercase()
                    user == "root"
                }
                Platform.WINDOWS -> isAdmin()
                Platform.UNKNOWN -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Проверить, запущено ли от администратора (Windows).
     */
    fun isAdmin(): Boolean {
        return try {
            when (platform) {
                Platform.WINDOWS -> {
                    val process = Runtime.getRuntime().exec(
                        arrayOf("cmd.exe", "/c", "net", "session")
                    )
                    process.waitFor() == 0
                }
                Platform.LINUX, Platform.MAC -> isRoot()
                Platform.UNKNOWN -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Проверить, запущено ли в виртуальной машине.
     * Оптимизировано для минимизации вызовов процессов.
     */
    fun isVirtualMachine(): Boolean {
        return try {
            when (platform) {
                Platform.WINDOWS -> {
                    // Читаем из реестра - быстрее и тише чем wmic
                    try {
                        val biosVendor = java.io.File("/sys/class/dmi/id/board_vendor")
                        if (biosVendor.exists()) {
                            val vendor = biosVendor.readText().lowercase()
                            if (vendor.contains("vmware") || vendor.contains("virtualbox") || 
                                vendor.contains("qemu") || vendor.contains("xen")) {
                                return true
                            }
                        }
                    } catch (_: Exception) {}

                    // Fallback: PowerShell (тише чем wmic)
                    val bios = runCommandWithTimeout(
                        arrayOf("powershell", "-Command", "Get-CimInstance Win32_BIOS | Select-Object -ExpandProperty Manufacturer"),
                        timeoutMs = 3000
                    )
                    bios?.lowercase()?.let {
                        it.contains("vmware") ||
                        it.contains("virtualbox") ||
                        it.contains("qemu") ||
                        (it.contains("microsoft") && it.contains("hyper-v"))
                    } ?: false
                }
                Platform.LINUX -> {
                    // Читаем напрямую из DMI - без процессов
                    try {
                        val productName = File("/sys/class/dmi/id/product_name")
                        val boardVendor = File("/sys/class/dmi/id/board_vendor")
                        val hypervisor = File("/sys/hypervisor/type")

                        val dmiInfo = buildString {
                            if (productName.exists()) append(productName.readText().lowercase())
                            if (boardVendor.exists()) append(boardVendor.readText().lowercase())
                            if (hypervisor.exists()) append(hypervisor.readText().lowercase())
                        }

                        dmiInfo.contains("vmware") ||
                        dmiInfo.contains("virtualbox") ||
                        dmiInfo.contains("qemu") ||
                        dmiInfo.contains("kvm") ||
                        dmiInfo.contains("xen") ||
                        dmiInfo.contains("hypervisor")
                    } catch (_: Exception) {
                        false
                    }
                }
                Platform.MAC -> false
                Platform.UNKNOWN -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Проверить, запущено ли в контейнере (Docker/LXC).
     */
    fun isContainer(): Boolean {
        return try {
            when (platform) {
                Platform.LINUX -> {
                    File("/.dockerenv").exists() ||
                    File("/proc/1/cgroup").readText().let { 
                        it.contains("docker") || it.contains("lxc") 
                    }
                }
                Platform.WINDOWS -> {
                    System.getenv("KUBERNETES_SERVICE_HOST") != null ||
                    System.getenv("CONTAINER") != null
                }
                Platform.MAC -> false
                Platform.UNKNOWN -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    // ========================================================================
    // 5. ИНФОРМАЦИЯ О ЖЕЛЕЗЕ
    // ========================================================================

    /**
     * Информация об оперативной памяти.
     */
    data class RamInfo(
        val total: Long,
        val free: Long,
        val max: Long,
        val totalPhysical: Long,
        val freePhysical: Long
    )

    /**
     * Получить информацию об оперативной памяти.
     */
    fun getRamInfo(): RamInfo {
        val runtime = Runtime.getRuntime()
        val total = runtime.totalMemory()
        val free = runtime.freeMemory()
        val max = runtime.maxMemory()

        var totalPhysical = 0L
        var freePhysical = 0L

        try {
            when (platform) {
                Platform.WINDOWS -> {
                    // Используем PowerShell с Get-CimInstance (быстрее и надежнее чем wmic)
                    val totalMemKb = runCommandWithTimeout(
                        arrayOf("powershell", "-Command", 
                            "(Get-CimInstance Win32_OperatingSystem).TotalVisibleMemorySize"),
                        timeoutMs = 3000
                    )?.trim()?.toLongOrNull()
                    
                    val freeMemKb = runCommandWithTimeout(
                        arrayOf("powershell", "-Command", 
                            "(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory"),
                        timeoutMs = 3000
                    )?.trim()?.toLongOrNull()
                    
                    // TotalVisibleMemorySize и FreePhysicalMemory уже в KB
                    totalPhysical = (totalMemKb ?: 0) * 1024
                    freePhysical = (freeMemKb ?: 0) * 1024
                    
                    // Fallback через Java если PowerShell не сработал
                    if (totalPhysical == 0L) {
                        totalPhysical = Runtime.getRuntime().totalMemory()
                        freePhysical = Runtime.getRuntime().freeMemory()
                    }
                }
                Platform.LINUX -> {
                    val meminfo = File("/proc/meminfo").readLines()
                    val memTotal = meminfo.find { it.startsWith("MemTotal:") }
                        ?.substringAfter(":")?.trim()?.substringBefore(" ")?.toLongOrNull() ?: 0
                    val memFree = meminfo.find { it.startsWith("MemFree:") }
                        ?.substringAfter(":")?.trim()?.substringBefore(" ")?.toLongOrNull() ?: 0
                    totalPhysical = memTotal * 1024
                    freePhysical = memFree * 1024
                }
                Platform.MAC -> {
                    val totalMem = runCommandWithTimeout(
                        arrayOf("sysctl", "-n", "hw.memsize"),
                        timeoutMs = 3000
                    )?.trim()?.toLongOrNull()
                    totalPhysical = totalMem ?: 0
                    freePhysical = free
                }
                Platform.UNKNOWN -> {
                    totalPhysical = total
                    freePhysical = free
                }
            }
        } catch (e: Exception) {
            Logger.warn("Error getting RAM info: ${e.message}")
            totalPhysical = total
            freePhysical = free
        }

        return RamInfo(total, free, max, totalPhysical, freePhysical)
    }

    /**
     * Информация о диске.
     */
    data class DiskInfo(
        val total: Long,
        val free: Long,
        val usable: Long,
        val path: String
    )

    /**
     * Получить информацию о диске.
     */
    fun getDiskInfo(path: Path = userHome): DiskInfo {
        return try {
            val file = path.toFile()
            DiskInfo(
                total = file.totalSpace,
                free = file.freeSpace,
                usable = file.usableSpace,
                path = path.toString()
            )
        } catch (e: Exception) {
            DiskInfo(0, 0, 0, path.toString())
        }
    }

    /**
     * Информация о видеокарте.
     */
    data class GpuInfo(
        val name: String,
        val vendor: String,
        val version: String
    )

    /**
     * Lazy инициализация GPU info (только после создания OpenGL контекста).
     */
    private var cachedGpuInfo: GpuInfo? = null

    /**
     * Получить информацию о видеокарте.
     *
     * Важно: Вызывать только после инициализации OpenGL контекста!
     * Для Minecraft modding - использовать после RenderSetup или в tick事件中.
     */
    fun getGpuInfo(): GpuInfo {
        // Возвращаем кэш если есть
        cachedGpuInfo?.let { return it }

        return try {
            // Попытка получить из OpenGL (только если контекст создан)
            val glVendor = System.getProperty("org.lwjgl.opengl.GL11.GL_VENDOR")
            val glRenderer = System.getProperty("org.lwjgl.opengl.GL11.GL_RENDERER")
            val glVersion = System.getProperty("org.lwjgl.opengl.GL11.GL_VERSION")

            val gpuInfo = if (!glRenderer.isNullOrBlank() && glRenderer != "Unknown") {
                // OpenGL контекст активен - определяем vendor по renderer
                val vendor = determineGpuVendor(glRenderer, glVendor)
                GpuInfo(glRenderer, vendor, glVersion ?: "Unknown")
            } else {
                // Fallback через команды ОС (медленнее, но работает без OpenGL)
                val gpuName = when (platform) {
                    Platform.WINDOWS -> {
                        // Используем PowerShell вместо wmic (тише для антивирусов)
                        runCommandWithTimeout(
                            arrayOf("powershell", "-Command", "Get-CimInstance Win32_VideoController | Select-Object -ExpandProperty Name"),
                            timeoutMs = 3000
                        )?.trim()?.takeIf { it.isNotBlank() }
                    }
                    Platform.LINUX -> {
                        runCommandWithTimeout(
                            arrayOf("lspci", "-v"),
                            timeoutMs = 3000
                        )?.lines()?.find {
                            it.lowercase().contains("vga") || it.lowercase().contains("3d")
                        }?.substringAfter(":")?.trim()
                    }
                    Platform.MAC -> {
                        runCommandWithTimeout(
                            arrayOf("system_profiler", "SPDisplaysDataType"),
                            timeoutMs = 3000
                        )?.lines()?.find { it.contains("Chipset Model:") }
                            ?.substringAfter(":")?.trim()
                    }
                    Platform.UNKNOWN -> null
                }
                
                val vendor = determineGpuVendor(gpuName ?: "", glVendor)
                GpuInfo(gpuName ?: "Unknown", vendor, glVersion ?: "Unknown")
            }

            // Кэшируем результат
            cachedGpuInfo = gpuInfo
            gpuInfo
        } catch (e: Exception) {
            Logger.warn("Error getting GPU info: ${e.message}")
            cachedGpuInfo = GpuInfo("Unknown", "Unknown", "Unknown")
            cachedGpuInfo!!
        }
    }

    /**
     * Определить vendor GPU по имени рендерера.
     */
    private fun determineGpuVendor(renderer: String, glVendor: String?): String {
        // Сначала проверяем явный vendor из OpenGL
        if (!glVendor.isNullOrBlank() && glVendor != "Unknown") {
            return glVendor
        }

        // Определяем по названию рендерера
        val rendererLower = renderer.lowercase()
        return when {
            rendererLower.contains("nvidia") || rendererLower.contains("geforce") || 
            rendererLower.contains("quadro") || rendererLower.contains("rtx") ||
            rendererLower.contains("gtx") -> "NVIDIA"
            
            rendererLower.contains("intel") || rendererLower.contains("iris") ||
            rendererLower.contains("uhd graphics") || rendererLower.contains("hd graphics") -> "Intel"
            
            rendererLower.contains("amd") || rendererLower.contains("radeon") ||
            rendererLower.contains("rx ") || rendererLower.contains("vega") -> "AMD"
            
            rendererLower.contains("apple") -> "Apple"
            
            else -> "Unknown"
        }
    }

    /**
     * Сбросить кэш GPU info (если нужно обновить).
     */
    fun resetGpuCache() {
        cachedGpuInfo = null
    }

    /**
     * Полная информация о системе.
     */
    data class OsDetails(
        val platform: Platform,
        val name: String,
        val version: String,
        val arch: String,
        val kernel: String,
        val is64Bit: Boolean,
        val isVM: Boolean,
        val isContainer: Boolean
    )

    /**
     * Информация о пользователе.
     */
    data class UserInfo(
        val name: String,
        val homeDir: String,
        val isRoot: Boolean,
        val isAdmin: Boolean,
        val isVM: Boolean,
        val isContainer: Boolean
    )

    /**
     * Получить полную информацию о системе.
     */
    fun getFullSystemInfo(): SystemInfo {
        return SystemInfo(
            os = getOsDetails(),
            user = getUserInfo(),
            cpu = getCpuInfo(),
            ram = getRamInfo(),
            disk = getDiskInfo(),
            gpu = getGpuInfo()
        )
    }

    /**
     * Полная информация о системе.
     */
    data class SystemInfo(
        val os: OsDetails,
        val user: UserInfo,
        val cpu: CpuInfo,
        val ram: RamInfo,
        val disk: DiskInfo,
        val gpu: GpuInfo
    )

    // ========================================================================
    // УТИЛИТЫ
    // ========================================================================

    /**
     * Выполнить команду с таймаутом.
     * 
     * @param command Массив команды
     * @param timeoutMs Таймаут в миллисекундах (по умолчанию 5000ms)
     * @return Результат выполнения или null при ошибке/таймауте
     */
    private fun runCommandWithTimeout(command: Array<String>, timeoutMs: Long = 5000): String? {
        return try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()

            // Ждём завершения с таймаутом
            if (!process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                Logger.warn("Command timed out: ${command.joinToString(" ")}")
                return null
            }

            if (process.exitValue() != 0) {
                return null
            }

            BufferedReader(InputStreamReader(process.inputStream, java.nio.charset.StandardCharsets.UTF_8))
                .use { reader -> reader.readText().trim() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Выполнить команду (старая версия для совместимости).
     * Использует таймаут 5 секунд по умолчанию.
     */
    private fun runCommand(vararg command: String): String? {
        val cmdArray = arrayOf(*command)
        return runCommandWithTimeout(cmdArray, 5000)
    }

    /**
     * Выполнить команду для получения CPU имени (с оптимизациями).
     * Для MCP - используем более быстрые методы где возможно.
     */
    private fun getCpuName(): String {
        return try {
            when (platform) {
                Platform.WINDOWS -> {
                    // PowerShell быстрее и тише чем wmic
                    runCommandWithTimeout(
                        arrayOf("powershell", "-Command", "Get-CimInstance Win32_Processor | Select-Object -ExpandProperty Name"),
                        timeoutMs = 3000
                    )?.trim()?.takeIf { it.isNotBlank() } ?: "Unknown CPU"
                }
                Platform.LINUX -> {
                    // Читаем напрямую из /proc/cpuinfo - быстро и без процессов
                    try {
                        val cpuinfo = java.io.File("/proc/cpuinfo")
                        if (cpuinfo.exists()) {
                            cpuinfo.readLines()
                                .find { it.startsWith("model name", ignoreCase = true) }
                                ?.substringAfter(":")
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?: "Unknown CPU"
                        } else {
                            "Unknown CPU"
                        }
                    } catch (e: Exception) {
                        "Unknown CPU"
                    }
                }
                Platform.MAC -> {
                    runCommandWithTimeout(
                        arrayOf("sysctl", "-n", "machdep.cpu.brand_string"),
                        timeoutMs = 3000
                    )?.trim() ?: "Unknown CPU"
                }
                Platform.UNKNOWN -> "Unknown CPU"
            }
        } catch (e: Exception) {
            "Unknown CPU"
        }
    }

    /**
     * Определить платформу.
     */
    private fun detectPlatform(): Platform {
        val os = System.getProperty("os.name").lowercase(Locale.ROOT)
        return when {
            os.contains("win") -> Platform.WINDOWS
            os.contains("mac") -> Platform.MAC
            os.contains("nix") || os.contains("nux") || os.contains("aix") -> Platform.LINUX
            else -> Platform.UNKNOWN
        }
    }

    /**
     * Получить строковое представление платформы.
     */
    fun getPlatformName(): String = when (platform) {
        Platform.WINDOWS -> "Windows"
        Platform.LINUX -> "Linux"
        Platform.MAC -> "macOS"
        Platform.UNKNOWN -> "Unknown"
    }

    /**
     * Получить разделитель путей.
     */
    fun getPathSeparator(): String = when (platform) {
        Platform.WINDOWS -> "\\"
        Platform.LINUX, Platform.MAC, Platform.UNKNOWN -> "/"
    }

    /**
     * Вывести полную информацию о системе в лог.
     */
    fun logSystemInfo() {
        val info = getFullSystemInfo()
        
        Logger.info("=== System Information ===")
        Logger.info("OS: ${info.os.name} ${info.os.version} (${info.os.arch})")
        Logger.info("Kernel: ${info.os.kernel}")
        Logger.info("64-bit: ${info.os.is64Bit}")
        Logger.info("VM: ${info.os.isVM}, Container: ${info.os.isContainer}")
        Logger.info("User: ${info.user.name} (Admin: ${info.user.isAdmin}, Root: ${info.user.isRoot})")
        Logger.info("CPU: ${info.cpu.name} (${info.cpu.cores} cores, ${info.cpu.frequency})")
        Logger.info("RAM: ${formatBytes(info.ram.totalPhysical)} total, ${formatBytes(info.ram.freePhysical)} free")
        Logger.info("Disk: ${formatBytes(info.disk.total)} total, ${formatBytes(info.disk.free)} free")
        Logger.info("GPU: ${info.gpu.name} (${info.gpu.vendor})")
        Logger.info("=========================")
    }

    /**
     * Форматировать размер в байтах.
     */
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return String.format("%.2f %s", size, units[unitIndex])
    }

    // ========================================================================
    // 7. ДАТА / ВРЕМЯ (LAZY)
    // ========================================================================

    /**
     * Текущая дата и время (ленивое свойство).
     */
    val currentTime: LocalDateTime by lazy {
        LocalDateTime.now()
    }

    /**
     * Текущая дата и время в формате ISO 8601.
     */
    val currentDateTimeIso: String by lazy {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(currentTime)
    }

    /**
     * Текущее время в формате HH:mm:ss.
     */
    val currentTimeString: String by lazy {
        DateTimeFormatter.ofPattern("HH:mm:ss").format(currentTime)
    }

    /**
     * Текущая дата в формате DD.MM.YYYY.
     */
    val currentDateString: String by lazy {
        DateTimeFormatter.ofPattern("dd.MM.yyyy").format(currentTime)
    }

    /**
     * Получить текущее время с заданным форматом.
     */
    fun getTimeFormatted(pattern: String): String {
        return DateTimeFormatter.ofPattern(pattern).format(LocalDateTime.now())
    }

    /**
     * Получить текущее время в миллисекундах (epoch).
     */
    fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Получить часовой пояс системы.
     */
    val systemTimeZone: ZoneId by lazy {
        ZoneId.systemDefault()
    }

    // ========================================================================
    // 8. ПОГОДА / ГЕОЛОКАЦИЯ
    // ========================================================================

    /**
     * Информация о геолокации.
     */
    data class LocationInfo(
        val city: String?,
        val region: String?,
        val country: String?,
        val ip: String?,
        val timezone: String?
    )

    /**
     * Информация о погоде.
     */
    data class WeatherInfo(
        val city: String?,
        val temperature: String?,
        val feelsLike: String?,
        val description: String?,
        val humidity: String?,
        val windSpeed: String?
    )

    /** Кэш геолокации (TTL 30 минут) */
    private var cachedLocation: LocationInfo? = null
    private var locationCacheTime: Long = 0
    private val LOCATION_TTL = 30 * 60 * 1000L // 30 минут

    /** Кэш погоды (TTL 15 минут) */
    private var cachedWeather: WeatherInfo? = null
    private var weatherCacheTime: Long = 0
    private val WEATHER_TTL = 15 * 60 * 1000L // 15 минут

    /**
     * Включить погоду/геолокацию (по умолчанию false).
     */
    var enableLocationServices: Boolean = false

    /**
     * Получить геолокацию по IP.
     * Использует ipwho.is (без ключа, бесплатно).
     */
    fun getLocation(): LocationInfo? {
        if (!enableLocationServices) return null

        // Проверяем кэш
        val now = System.currentTimeMillis()
        if (cachedLocation != null && now - locationCacheTime < LOCATION_TTL) {
            return cachedLocation
        }

        return try {
            val url = URL("http://ipwho.is/json")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = BufferedReader(InputStreamReader(connection.inputStream))
                .use { it.readText() }

            val json = com.google.gson.JsonParser.parseString(response).asJsonObject

            val location = LocationInfo(
                city = json.get("city")?.asString,
                region = json.get("region")?.asString,
                country = json.get("country")?.asString,
                ip = json.get("ip")?.asString,
                timezone = json.get("timezone")?.asString
            )

            cachedLocation = location
            locationCacheTime = now

            connection.disconnect()
            location
        } catch (e: Exception) {
            Logger.warn("Failed to get location: ${e.message}")
            null
        }
    }

    /**
     * Получить погоду через wttr.in.
     * Формат: JSON (без ключа).
     */
    fun getWeather(): WeatherInfo? {
        if (!enableLocationServices) return null

        // Проверяем кэш
        val now = System.currentTimeMillis()
        if (cachedWeather != null && now - weatherCacheTime < WEATHER_TTL) {
            return cachedWeather
        }

        return try {
            // wttr.in возвращает погоду для IP пользователя
            val url = URL("http://wttr.in/?format=j1")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "AporiaClient/1.0")

            val response = BufferedReader(InputStreamReader(connection.inputStream))
                .use { it.readText() }

            val json = com.google.gson.JsonParser.parseString(response).asJsonObject

            val currentCondition = json.getAsJsonArray("current_condition")?.get(0)?.asJsonObject
            val nearestArea = json.getAsJsonArray("nearest_area")?.get(0)?.asJsonObject

            val weather = WeatherInfo(
                city = nearestArea?.getAsJsonArray("areaName")?.get(0)?.asJsonObject?.get("value")?.asString,
                temperature = currentCondition?.get("temp_C")?.asString,
                feelsLike = currentCondition?.get("FeelsLikeC")?.asString,
                description = currentCondition?.getAsJsonArray("weatherDesc")?.get(0)?.asJsonObject?.get("value")?.asString,
                humidity = currentCondition?.get("humidity")?.asString,
                windSpeed = currentCondition?.get("windspeedKmph")?.asString
            )

            cachedWeather = weather
            weatherCacheTime = now

            connection.disconnect()
            weather
        } catch (e: Exception) {
            Logger.warn("Failed to get weather: ${e.message}")
            null
        }
    }

    /**
     * Получить погоду для конкретного города.
     */
    fun getWeatherForCity(city: String): WeatherInfo? {
        if (!enableLocationServices) return null

        return try {
            val encodedCity = java.net.URLEncoder.encode(city, "UTF-8")
            val url = URL("http://wttr.in/$encodedCity?format=j1")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "AporiaClient/1.0")

            val response = BufferedReader(InputStreamReader(connection.inputStream))
                .use { it.readText() }

            val json = com.google.gson.JsonParser.parseString(response).asJsonObject
            val currentCondition = json.getAsJsonArray("current_condition")?.get(0)?.asJsonObject

            WeatherInfo(
                city = city,
                temperature = currentCondition?.get("temp_C")?.asString,
                feelsLike = currentCondition?.get("FeelsLikeC")?.asString,
                description = currentCondition?.getAsJsonArray("weatherDesc")?.get(0)?.asJsonObject?.get("value")?.asString,
                humidity = currentCondition?.get("humidity")?.asString,
                windSpeed = currentCondition?.get("windspeedKmph")?.asString
            )
        } catch (e: Exception) {
            Logger.warn("Failed to get weather for $city: ${e.message}")
            null
        }
    }

    /**
     * Сбросить кэш геолокации и погоды.
     */
    fun clearLocationCache() {
        cachedLocation = null
        cachedWeather = null
        locationCacheTime = 0
        weatherCacheTime = 0
    }

    // ========================================================================
    // 9. ЛОГГЕР (ИНТЕГРАЦИЯ)
    // ========================================================================

    /**
     * Флаг включения логгера OsManager.
     */
    var loggerEnabled: Boolean = false

    /**
     * Инициализировать логгер OsManager.
     * Выводит ASCII баннер при первом включении.
     */
    fun initLogger() {
        if (loggerEnabled) {
            Logger.info("OsManager logger already initialized")
            return
        }

        loggerEnabled = true

        val banner = """

        ╔═══════════════════════════════════════════════════════════╗
        ║                                                           ║
        ║    █████╗ ██╗      █████╗  ██████╗██████╗ ██╗████████╗    ║
        ║   ██╔══██╗██║     ██╔══██╗██╔════╝██╔══██╗██║╚══██╔══╝    ║
        ║   ███████║██║     ███████║██║     ██████╔╝██║   ██║       ║
        ║   ██╔══██║██║     ██╔══██║██║     ██╔══██╗██║   ██║       ║
        ║   ██║  ██║███████╗██║  ██║╚██████╗██║  ██║██║   ██║       ║
        ║   ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝╚═╝   ╚═╝       ║
        ║                                                           ║
        ║                   . C C                                   ║
        ║                                                           ║
        ║              O S   M A N A G E R                          ║
        ║                                                           ║
        ╚═══════════════════════════════════════════════════════════╝

        """.trimIndent()

        Logger.info(banner)
        Logger.info("OsManager initialized")
        Logger.info("Platform: ${getPlatformName()} ${osVersion}")
        Logger.info("Architecture: ${osArch}")
        Logger.info("User: $userName")
        Logger.info("================================================")
    }

    /**
     * Вывести полную информацию о системе в лог.
     */
    fun logFullSystemInfo() {
        if (!loggerEnabled) {
            Logger.warn("OsManager logger not enabled. Call initLogger() first.")
            return
        }

        Logger.info("=== Full System Information ===")

        // OS Info
        val osInfo = getOsDetails()
        Logger.info("OS: ${osInfo.name} ${osInfo.version} (${osInfo.arch})")
        Logger.info("Kernel: ${osInfo.kernel}")
        Logger.info("64-bit: ${osInfo.is64Bit}")
        Logger.info("VM: ${osInfo.isVM}, Container: ${osInfo.isContainer}")

        // User Info
        val userInfo = getUserInfo()
        Logger.info("User: ${userInfo.name} (Admin: ${userInfo.isAdmin}, Root: ${userInfo.isRoot})")

        // CPU Info
        val cpuInfo = getCpuInfo()
        Logger.info("CPU: ${cpuInfo.name} (${cpuInfo.cores} cores, ${cpuInfo.frequency})")

        // RAM Info
        val ramInfo = getRamInfo()
        Logger.info("RAM: ${formatBytes(ramInfo.totalPhysical)} total, ${formatBytes(ramInfo.freePhysical)} free")

        // Disk Info
        val diskInfo = getDiskInfo()
        Logger.info("Disk: ${formatBytes(diskInfo.total)} total, ${formatBytes(diskInfo.free)} free")

        // GPU Info
        val gpuInfo = getGpuInfo()
        Logger.info("GPU: ${gpuInfo.name} (${gpuInfo.vendor})")

        // Location/Weather (если включено)
        if (enableLocationServices) {
            val location = getLocation()
            if (location != null) {
                Logger.info("Location: ${location.city}, ${location.region}, ${location.country}")

                val weather = getWeather()
                if (weather != null) {
                    Logger.info("Weather: ${weather.temperature}°C, ${weather.description}, ${weather.humidity}% humidity")
                }
            }
        }

        Logger.info("================================")
    }
}
