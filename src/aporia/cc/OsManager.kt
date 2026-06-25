package aporia.cc

import com.chaos.annotation.Obfuscate
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

@Obfuscate
object OsManager {

    enum class Platform {
        WINDOWS, LINUX, MAC, UNKNOWN
    }

    enum class CpuArch {
        X86, X86_64, ARM, ARM64, UNKNOWN
    }

    enum class DirectoryType {
        CONFIG, CACHE, DATA, LOGS, TEMP, BACKUP, MODULES, THEMES, SCRIPTS
    }

    val platform: Platform = detectPlatform()
    val osName: String = System.getProperty("os.name")
    val osVersion: String = System.getProperty("os.version")
    val osArch: String = System.getProperty("os.arch")
    val userName: String = System.getProperty("user.name")
    val userDir: String = System.getProperty("user.dir")
    val userHome: Path = Paths.get(System.getProperty("user.home"))

    val mainDirectory: Path = Paths.get(System.getProperty("user.home"), ".apr")
    val cacheDirectory: Path = mainDirectory.resolve("cache")
    val dataDirectory: Path = mainDirectory.resolve("data")
    val logsDirectory: Path = mainDirectory.resolve("logs")
    val tempDirectory: Path = cacheDirectory.resolve("temp")
    val backupDirectory: Path = mainDirectory.resolve("backup")
    val modulesDirectory: Path = mainDirectory.resolve("modules")
    val themesDirectory: Path = mainDirectory.resolve("themes")
    val scriptsDirectory: Path = mainDirectory.resolve("scripts")

    var enableLocationServices = false
    var loggerEnabled = false

    private var cachedGpuInfo: GpuInfo? = null
    private var cachedLocation: LocationInfo? = null
    private var locationCacheTime = 0L
    private var cachedWeather: WeatherInfo? = null
    private var weatherCacheTime = 0L

    private const val LOCATION_TTL = 30 * 60 * 1000L
    private const val WEATHER_TTL = 15 * 60 * 1000L

    fun getOsDetails(): OsDetails {
        return OsDetails(
            platform, osName, osVersion, osArch,
            getKernelVersion(), is64Bit(), isVirtualMachine(), isContainer()
        )
    }

    private fun getKernelVersion(): String {
        return try {
            when (platform) {
                Platform.WINDOWS -> System.getProperty("os.version")
                Platform.LINUX, Platform.MAC -> runCommand("uname", "-r") ?: "unknown"
                else -> "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun is64Bit(): Boolean {
        val arch = osArch.lowercase(Locale.ROOT)
        return arch.contains("64") || arch.contains("x86_64") ||
                arch.contains("amd64") || arch.contains("aarch64")
    }

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

    data class CpuInfo(
        val name: String,
        val cores: Int,
        val threads: Int,
        val frequency: String,
        val architecture: CpuArch
    )

    fun getCpuInfo(): CpuInfo {
        val cores = Runtime.getRuntime().availableProcessors()
        val arch = getCpuArch()
        val name = getCpuName()
        val frequency = getCpuFrequency()
        return CpuInfo(name, cores, cores, frequency, arch)
    }

    private fun getCpuFrequency(): String {
        return try {
            when (platform) {
                Platform.LINUX -> {
                    val cpuinfo = File("/proc/cpuinfo")
                    if (cpuinfo.exists()) {
                        for (line in readFileLines(cpuinfo)) {
                            if (line.lowercase().startsWith("cpu mhz")) {
                                val parts = line.split(":")
                                if (parts.size > 1) {
                                    try {
                                        val freq = parts[1].trim().toFloat()
                                        return String.format(Locale.US, "%.1f GHz", freq / 1000)
                                    } catch (e: NumberFormatException) {
                                        return "Unknown"
                                    }
                                }
                            }
                        }
                    }
                    "Unknown"
                }
                Platform.WINDOWS -> {
                    val freq = runCommandWithTimeout(
                        arrayOf("powershell", "-Command", "Get-CimInstance Win32_Processor | Select-Object -ExpandProperty MaxClockSpeed"),
                        3000
                    )
                    if (freq != null) {
                        for (line in freq.split("\n")) {
                            val trimmed = line.trim()
                            if (trimmed.isNotEmpty() && trimmed.matches("\\d+".toRegex())) {
                                try {
                                    val mhz = trimmed.toInt()
                                    return "${mhz / 1000} GHz"
                                } catch (e: NumberFormatException) {
                                    return "Unknown"
                                }
                            }
                        }
                    }
                    "Unknown"
                }
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

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

    fun getFile(type: DirectoryType, fileName: String): Path = getDirectory(type).resolve(fileName)

    fun pathExists(path: Path): Boolean = path.toFile().exists()

    fun isWritable(path: Path): Boolean {
        return try {
            val file = path.toFile()
            if (!file.exists()) file.mkdirs()
            file.canWrite()
        } catch (e: Exception) {
            false
        }
    }

    fun createDirectory(path: Path): Boolean {
        return try {
            val file = path.toFile()
            if (!file.exists()) file.mkdirs() else file.exists()
        } catch (e: Exception) {
            false
        }
    }

    fun createAllDirectories(): Boolean {
        return try {
            val dirs = arrayOf(
                mainDirectory, cacheDirectory, dataDirectory, logsDirectory,
                tempDirectory, backupDirectory, modulesDirectory, themesDirectory, scriptsDirectory
            )
            dirs.all { createDirectory(it) }
        } catch (e: Exception) {
            false
        }
    }

    fun getUserInfo(): UserInfo {
        return UserInfo(userName, userHome.toString(), isRoot(), isAdmin(), isVirtualMachine(), isContainer())
    }

    fun isRoot(): Boolean {
        return try {
            when (platform) {
                Platform.LINUX, Platform.MAC -> {
                    val user = runCommand("whoami")
                    user?.trim()?.lowercase() == "root"
                }
                Platform.WINDOWS -> isAdmin()
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun isAdmin(): Boolean {
        return try {
            when (platform) {
                Platform.WINDOWS -> {
                    val process = Runtime.getRuntime().exec(arrayOf("cmd.exe", "/c", "net", "session"))
                    process.waitFor() == 0
                }
                Platform.LINUX, Platform.MAC -> isRoot()
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun isVirtualMachine(): Boolean {
        return try {
            when (platform) {
                Platform.WINDOWS -> {
                    try {
                        val boardVendor = File("/sys/class/dmi/id/board_vendor")
                        if (boardVendor.exists()) {
                            for (line in readFileLines(boardVendor)) {
                                val vendor = line.lowercase()
                                if (vendor.contains("vmware") || vendor.contains("virtualbox") ||
                                    vendor.contains("qemu") || vendor.contains("xen")) {
                                    return true
                                }
                            }
                        }
                    } catch (e: Exception) {}

                    val bios = runCommandWithTimeout(
                        arrayOf("powershell", "-Command", "Get-CimInstance Win32_BIOS | Select-Object -ExpandProperty Manufacturer"),
                        3000
                    )
                    if (bios != null) {
                        val lowerBios = bios.lowercase()
                        return lowerBios.contains("vmware") ||
                                lowerBios.contains("virtualbox") ||
                                lowerBios.contains("qemu") ||
                                (lowerBios.contains("microsoft") && lowerBios.contains("hyper-v"))
                    }
                    false
                }
                Platform.LINUX -> {
                    try {
                        val dmiInfo = StringBuilder()
                        val productName = File("/sys/class/dmi/id/product_name")
                        val boardVendor = File("/sys/class/dmi/id/board_vendor")
                        val hypervisor = File("/sys/hypervisor/type")

                        if (productName.exists()) {
                            for (line in readFileLines(productName)) {
                                dmiInfo.append(line.lowercase())
                            }
                        }
                        if (boardVendor.exists()) {
                            for (line in readFileLines(boardVendor)) {
                                dmiInfo.append(line.lowercase())
                            }
                        }
                        if (hypervisor.exists()) {
                            for (line in readFileLines(hypervisor)) {
                                dmiInfo.append(line.lowercase())
                            }
                        }

                        val dmi = dmiInfo.toString()
                        dmi.contains("vmware") || dmi.contains("virtualbox") ||
                                dmi.contains("qemu") || dmi.contains("kvm") ||
                                dmi.contains("xen") || dmi.contains("hypervisor")
                    } catch (e: Exception) {
                        false
                    }
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun isContainer(): Boolean {
        return try {
            when (platform) {
                Platform.LINUX -> {
                    if (File("/.dockerenv").exists()) return true
                    val cgroup = File("/proc/1/cgroup")
                    if (cgroup.exists()) {
                        for (line in readFileLines(cgroup)) {
                            if (line.contains("docker") || line.contains("lxc")) return true
                        }
                    }
                    false
                }
                Platform.WINDOWS -> {
                    System.getenv("KUBERNETES_SERVICE_HOST") != null ||
                            System.getenv("CONTAINER") != null
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    data class RamInfo(
        val total: Long,
        val free: Long,
        val max: Long,
        val totalPhysical: Long,
        val freePhysical: Long
    )

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
                    val totalMemKbStr = runCommandWithTimeout(
                        arrayOf("powershell", "-Command", "(Get-CimInstance Win32_OperatingSystem).TotalVisibleMemorySize"),
                        3000
                    )
                    val freeMemKbStr = runCommandWithTimeout(
                        arrayOf("powershell", "-Command", "(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory"),
                        3000
                    )

                    if (totalMemKbStr != null) {
                        try { totalPhysical = totalMemKbStr.trim().toLong() * 1024 } catch (e: NumberFormatException) {}
                    }
                    if (freeMemKbStr != null) {
                        try { freePhysical = freeMemKbStr.trim().toLong() * 1024 } catch (e: NumberFormatException) {}
                    }

                    if (totalPhysical == 0L) {
                        totalPhysical = runtime.totalMemory()
                        freePhysical = runtime.freeMemory()
                    }
                }
                Platform.LINUX -> {
                    val meminfo = File("/proc/meminfo")
                    if (meminfo.exists()) {
                        for (line in readFileLines(meminfo)) {
                            when {
                                line.startsWith("MemTotal:") -> {
                                    val parts = line.split("\\s+".toRegex())
                                    if (parts.size > 1) {
                                        try { totalPhysical = parts[1].toLong() * 1024 } catch (e: NumberFormatException) {}
                                    }
                                }
                                line.startsWith("MemFree:") -> {
                                    val parts = line.split("\\s+".toRegex())
                                    if (parts.size > 1) {
                                        try { freePhysical = parts[1].toLong() * 1024 } catch (e: NumberFormatException) {}
                                    }
                                }
                            }
                        }
                    }
                }
                Platform.MAC -> {
                    val totalMem = runCommandWithTimeout(arrayOf("sysctl", "-n", "hw.memsize"), 3000)
                    if (totalMem != null) {
                        try { totalPhysical = totalMem.trim().toLong() } catch (e: NumberFormatException) {}
                    }
                    freePhysical = free
                }
                else -> {
                    totalPhysical = total
                    freePhysical = free
                }
            }
        } catch (e: Exception) {
            totalPhysical = total
            freePhysical = free
        }

        return RamInfo(total, free, max, totalPhysical, freePhysical)
    }

    data class DiskInfo(
        val total: Long,
        val free: Long,
        val usable: Long,
        val path: String
    )

    fun getDiskInfo(): DiskInfo = getDiskInfo(userHome)

    fun getDiskInfo(path: Path): DiskInfo {
        return try {
            val file = path.toFile()
            DiskInfo(file.totalSpace, file.freeSpace, file.usableSpace, path.toString())
        } catch (e: Exception) {
            DiskInfo(0, 0, 0, path.toString())
        }
    }

    data class GpuInfo(
        val name: String,
        val vendor: String,
        val version: String
    )

    fun getGpuInfo(): GpuInfo {
        cachedGpuInfo?.let { return it }

        val gpuInfo = try {
            val glVendor = System.getProperty("org.lwjgl.opengl.GL11.GL_VENDOR")
            val glRenderer = System.getProperty("org.lwjgl.opengl.GL11.GL_RENDERER")
            val glVersion = System.getProperty("org.lwjgl.opengl.GL11.GL_VERSION")

            if (!glRenderer.isNullOrEmpty() && glRenderer != "Unknown") {
                val vendor = determineGpuVendor(glRenderer, glVendor)
                GpuInfo(glRenderer, vendor, glVersion ?: "Unknown")
            } else {
                var gpuName: String? = null
                when (platform) {
                    Platform.WINDOWS -> {
                        gpuName = runCommandWithTimeout(
                            arrayOf("powershell", "-Command", "Get-CimInstance Win32_VideoController | Select-Object -ExpandProperty Name"),
                            3000
                        )?.split("\n")?.firstOrNull { it.trim().isNotEmpty() }?.trim()
                    }
                    Platform.LINUX -> {
                        val lspci = runCommandWithTimeout(arrayOf("lspci", "-v"), 3000)
                        if (lspci != null) {
                            gpuName = lspci.split("\n").firstOrNull { line ->
                                val lower = line.lowercase()
                                lower.contains("vga") || lower.contains("3d")
                            }?.let { line ->
                                val idx = line.indexOf(':')
                                if (idx >= 0 && idx + 1 < line.length) line.substring(idx + 1).trim() else null
                            }
                        }
                    }
                    Platform.MAC -> {
                        val sp = runCommandWithTimeout(arrayOf("system_profiler", "SPDisplaysDataType"), 3000)
                        if (sp != null) {
                            gpuName = sp.split("\n").firstOrNull { it.contains("Chipset Model:") }?.let { line ->
                                val idx = line.indexOf(':')
                                if (idx >= 0 && idx + 1 < line.length) line.substring(idx + 1).trim() else null
                            }
                        }
                    }
                    else -> {}
                }

                val vendor = determineGpuVendor(gpuName ?: "", glVendor)
                GpuInfo(gpuName ?: "Unknown", vendor, glVersion ?: "Unknown")
            }
        } catch (e: Exception) {
            GpuInfo("Unknown", "Unknown", "Unknown")
        }

        cachedGpuInfo = gpuInfo
        return gpuInfo
    }

    private fun determineGpuVendor(renderer: String, glVendor: String?): String {
        if (!glVendor.isNullOrEmpty() && glVendor != "Unknown") return glVendor

        val lower = renderer.lowercase()
        return when {
            lower.contains("nvidia") || lower.contains("geforce") ||
                    lower.contains("quadro") || lower.contains("rtx") ||
                    lower.contains("gtx") -> "NVIDIA"
            lower.contains("intel") || lower.contains("iris") ||
                    lower.contains("uhd graphics") || lower.contains("hd graphics") -> "Intel"
            lower.contains("amd") || lower.contains("radeon") ||
                    lower.contains("rx ") || lower.contains("vega") -> "AMD"
            lower.contains("apple") -> "Apple"
            else -> "Unknown"
        }
    }

    fun resetGpuCache() { cachedGpuInfo = null }

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

    data class UserInfo(
        val name: String,
        val homeDir: String,
        val isRoot: Boolean,
        val isAdmin: Boolean,
        val isVM: Boolean,
        val isContainer: Boolean
    )

    data class SystemInfo(
        val os: OsDetails,
        val user: UserInfo,
        val cpu: CpuInfo,
        val ram: RamInfo,
        val disk: DiskInfo,
        val gpu: GpuInfo
    )

    fun getFullSystemInfo(): SystemInfo {
        return SystemInfo(getOsDetails(), getUserInfo(), getCpuInfo(), getRamInfo(), getDiskInfo(), getGpuInfo())
    }

    private fun runCommandWithTimeout(command: Array<String>, timeoutMs: Long): String? {
        return try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()

            if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                return null
            }

            if (process.exitValue() != 0) return null

            val output = StringBuilder()
            BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (output.isNotEmpty()) output.append("\n")
                    output.append(line)
                }
            }
            output.toString().trim()
        } catch (e: Exception) {
            null
        }
    }

    private fun runCommand(vararg command: String): String? {
        return runCommandWithTimeout(arrayOf(*command), 5000)
    }

    private fun getCpuName(): String {
        return try {
            when (platform) {
                Platform.WINDOWS -> {
                    val cpuName = runCommandWithTimeout(
                        arrayOf("powershell", "-Command", "Get-CimInstance Win32_Processor | Select-Object -ExpandProperty Name"),
                        3000
                    )
                    cpuName?.split("\n")?.firstOrNull { it.trim().isNotEmpty() }?.trim() ?: "Unknown CPU"
                }
                Platform.LINUX -> {
                    val cpuinfo = File("/proc/cpuinfo")
                    if (cpuinfo.exists()) {
                        for (line in readFileLines(cpuinfo)) {
                            if (line.lowercase().startsWith("model name")) {
                                val parts = line.split(":", limit = 2)
                                if (parts.size > 1) return parts[1].trim()
                            }
                        }
                    }
                    "Unknown CPU"
                }
                Platform.MAC -> {
                    runCommandWithTimeout(arrayOf("sysctl", "-n", "machdep.cpu.brand_string"), 3000)?.trim()
                        ?: "Unknown CPU"
                }
                else -> "Unknown CPU"
            }
        } catch (e: Exception) {
            "Unknown CPU"
        }
    }

    private fun readFileLines(file: File): List<String> {
        return try {
            java.nio.file.Files.readAllLines(file.toPath())
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun detectPlatform(): Platform {
        val os = System.getProperty("os.name").lowercase(Locale.ROOT)
        return when {
            os.contains("win") -> Platform.WINDOWS
            os.contains("mac") -> Platform.MAC
            os.contains("nix") || os.contains("nux") || os.contains("aix") -> Platform.LINUX
            else -> Platform.UNKNOWN
        }
    }

    @JvmStatic
    fun getSystemLocale(): String {
        val sys = Locale.getDefault()
        val lang = sys.language
        val country = sys.country
        return if (country.isEmpty()) {
            when (lang.lowercase()) {
                "ru" -> "ru_RU"
                "zh" -> "ch_CH"
                else -> "en_EU"
            }
        } else "${lang}_$country"
    }

    fun getPlatformName(): String = when (platform) {
        Platform.WINDOWS -> "Windows"
        Platform.LINUX -> "Linux"
        Platform.MAC -> "macOS"
        else -> "Unknown"
    }

    fun getPathSeparator(): String = when (platform) {
        Platform.WINDOWS -> "\\"
        else -> "/"
    }

    fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return String.format(Locale.US, "%.2f %s", size, units[unitIndex])
    }

    fun printSystemInfo() {
        val info = getFullSystemInfo()
        println("=== System Information ===")
        println("OS: ${info.os.name} ${info.os.version} (${info.os.arch})")
        println("Kernel: ${info.os.kernel}")
        println("64-bit: ${info.os.is64Bit}")
        println("VM: ${info.os.isVM}, Container: ${info.os.isContainer}")
        println("User: ${info.user.name} (Admin: ${info.user.isAdmin}, Root: ${info.user.isRoot})")
        println("CPU: ${info.cpu.name} (${info.cpu.cores} cores, ${info.cpu.frequency})")
        println("RAM: ${formatBytes(info.ram.totalPhysical)} total, ${formatBytes(info.ram.freePhysical)} free")
        println("Disk: ${formatBytes(info.disk.total)} total, ${formatBytes(info.disk.free)} free")
        println("GPU: ${info.gpu.name} (${info.gpu.vendor})")
        println("=========================")
    }

    fun getCurrentTime(): LocalDateTime = LocalDateTime.now()

    fun getCurrentDateTimeIso(): String = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(getCurrentTime())

    fun getCurrentTimeString(): String = DateTimeFormatter.ofPattern("HH:mm:ss").format(getCurrentTime())

    fun getCurrentDateString(): String = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(getCurrentTime())

    fun getTimeFormatted(pattern: String): String = DateTimeFormatter.ofPattern(pattern).format(LocalDateTime.now())

    fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

    fun getSystemTimeZone(): ZoneId = ZoneId.systemDefault()

    data class LocationInfo(
        val city: String?,
        val region: String?,
        val country: String?,
        val ip: String?,
        val timezone: String?
    )

    data class WeatherInfo(
        val city: String,
        val temperature: String,
        val feelsLike: String,
        val description: String,
        val humidity: String,
        val windSpeed: String
    )

    fun getLocation(): LocationInfo? {
        if (!enableLocationServices) return null

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

            val response = StringBuilder()
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.lineSequence().forEach { response.append(it) }
            }

            val json = JsonParser.parseString(response.toString()).asJsonObject

            val location = LocationInfo(
                json.get("city")?.takeIf { !it.isJsonNull }?.asString,
                json.get("region")?.takeIf { !it.isJsonNull }?.asString,
                json.get("country")?.takeIf { !it.isJsonNull }?.asString,
                json.get("ip")?.takeIf { !it.isJsonNull }?.asString,
                json.get("timezone")?.takeIf { !it.isJsonNull }?.asString
            )

            cachedLocation = location
            locationCacheTime = now
            connection.disconnect()
            location
        } catch (e: Exception) {
            null
        }
    }

    fun getWeather(): WeatherInfo? {
        if (!enableLocationServices) return null

        val now = System.currentTimeMillis()
        if (cachedWeather != null && now - weatherCacheTime < WEATHER_TTL) {
            return cachedWeather
        }

        return try {
            val url = URL("http://wttr.in/?format=j1")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "AporiaClient/1.0")

            val response = StringBuilder()
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.lineSequence().forEach { response.append(it) }
            }

            val json = JsonParser.parseString(response.toString()).asJsonObject
            val currentCondition = json.getAsJsonArray("current_condition")[0].asJsonObject
            val nearestArea = json.getAsJsonArray("nearest_area")[0].asJsonObject

            val city = nearestArea.getAsJsonArray("areaName")[0].asJsonObject.get("value").asString
            val temperature = currentCondition.get("temp_C").asString
            val feelsLike = currentCondition.get("FeelsLikeC").asString
            val description = currentCondition.getAsJsonArray("weatherDesc")[0].asJsonObject.get("value").asString
            val humidity = currentCondition.get("humidity").asString
            val windSpeed = currentCondition.get("windspeedKmph").asString

            val weather = WeatherInfo(city, temperature, feelsLike, description, humidity, windSpeed)
            cachedWeather = weather
            weatherCacheTime = now
            connection.disconnect()
            weather
        } catch (e: Exception) {
            null
        }
    }

    fun getWeatherForCity(city: String): WeatherInfo? {
        if (!enableLocationServices) return null

        return try {
            val encodedCity = URLEncoder.encode(city, "UTF-8")
            val url = URL("http://wttr.in/$encodedCity?format=j1")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "AporiaClient/1.0")

            val response = StringBuilder()
            BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.lineSequence().forEach { response.append(it) }
            }

            val json = JsonParser.parseString(response.toString()).asJsonObject
            val currentCondition = json.getAsJsonArray("current_condition")[0].asJsonObject

            val temperature = currentCondition.get("temp_C").asString
            val feelsLike = currentCondition.get("FeelsLikeC").asString
            val description = currentCondition.getAsJsonArray("weatherDesc")[0].asJsonObject.get("value").asString
            val humidity = currentCondition.get("humidity").asString
            val windSpeed = currentCondition.get("windspeedKmph").asString

            connection.disconnect()
            WeatherInfo(city, temperature, feelsLike, description, humidity, windSpeed)
        } catch (e: Exception) {
            null
        }
    }

    fun clearLocationCache() {
        cachedLocation = null
        cachedWeather = null
        locationCacheTime = 0
        weatherCacheTime = 0
    }

    fun initLogger() {
        if (loggerEnabled) {
            println("OsManager logger already initialized")
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

        println(banner)
        println("OsManager initialized")
        println("Platform: ${getPlatformName()} $osVersion")
        println("Architecture: $osArch")
        println("User: $userName")
        println("================================================")
    }

    fun logFullSystemInfo() {
        if (!loggerEnabled) {
            System.err.println("OsManager logger not enabled. Call initLogger() first.")
            return
        }

        println("=== Full System Information ===")
        val osInfo = getOsDetails()
        println("OS: ${osInfo.name} ${osInfo.version} (${osInfo.arch})")
        println("Kernel: ${osInfo.kernel}")
        println("64-bit: ${osInfo.is64Bit}")
        println("VM: ${osInfo.isVM}, Container: ${osInfo.isContainer}")

        val userInfo = getUserInfo()
        println("User: ${userInfo.name} (Admin: ${userInfo.isAdmin}, Root: ${userInfo.isRoot})")

        val cpuInfo = getCpuInfo()
        println("CPU: ${cpuInfo.name} (${cpuInfo.cores} cores, ${cpuInfo.frequency})")

        val ramInfo = getRamInfo()
        println("RAM: ${formatBytes(ramInfo.totalPhysical)} total, ${formatBytes(ramInfo.freePhysical)} free")

        val diskInfo = getDiskInfo()
        println("Disk: ${formatBytes(diskInfo.total)} total, ${formatBytes(diskInfo.free)} free")

        val gpuInfo = getGpuInfo()
        println("GPU: ${gpuInfo.name} (${gpuInfo.vendor})")

        if (enableLocationServices) {
            getLocation()?.let { location ->
                println("Location: ${location.city}, ${location.region}, ${location.country}")
                getWeather()?.let { weather ->
                    println("Weather: ${weather.temperature}°C, ${weather.description}, ${weather.humidity}% humidity")
                }
            }
        }

        println("================================")
    }

}
