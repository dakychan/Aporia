package aporia.cc;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
public class OsManager {

    // ========================================================================
    // 1. ПОЛУЧЕНИЕ ОС И ВЕРСИИ
    // ========================================================================

    /**
     * Поддерживаемые платформы.
     */
    public enum Platform {
        WINDOWS,
        LINUX,
        MAC,
        UNKNOWN
    }

    /** Текущая платформа */
    public static final Platform platform = detectPlatform();

    /** Полное имя ОС */
    public static final String osName = System.getProperty("os.name");

    /** Версия ОС */
    public static final String osVersion = System.getProperty("os.version");

    /** Архитектура ОС */
    public static final String osArch = System.getProperty("os.arch");

    /**
     * Получить детальную информацию об ОС.
     */
    public static OsDetails getOsDetails() {
        return new OsDetails(
            platform,
            osName,
            osVersion,
            osArch,
            getKernelVersion(),
            is64Bit(),
            isVirtualMachine(),
            isContainer()
        );
    }

    /**
     * Получить версию ядра.
     */
    private static String getKernelVersion() {
        try {
            switch (platform) {
                case WINDOWS:
                    return System.getProperty("os.version");
                case LINUX:
                    String result = runCommand("uname", "-r");
                    return result != null ? result : "unknown";
                case MAC:
                    result = runCommand("uname", "-r");
                    return result != null ? result : "unknown";
                default:
                    return "unknown";
            }
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Проверить, 64-битная ли система.
     */
    public static boolean is64Bit() {
        String arch = osArch.toLowerCase(Locale.ROOT);
        return arch.contains("64") || 
               arch.contains("x86_64") || 
               arch.contains("amd64") || 
               arch.contains("aarch64");
    }

    // ========================================================================
    // 2. ПОЛУЧЕНИЕ АРХИТЕКТУРЫ И ПОНИМАНИЕ СИСТЕМЫ
    // ========================================================================

    /**
     * Тип архитектуры процессора.
     */
    public enum CpuArch {
        X86,
        X86_64,
        ARM,
        ARM64,
        UNKNOWN
    }

    /**
     * Получить архитектуру процессора.
     */
    public static CpuArch getCpuArch() {
        String arch = osArch.toLowerCase(Locale.ROOT);
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return CpuArch.ARM64;
        } else if (arch.contains("arm")) {
            return CpuArch.ARM;
        } else if (arch.contains("64")) {
            return CpuArch.X86_64;
        } else if (arch.contains("x86")) {
            return CpuArch.X86;
        } else {
            return CpuArch.UNKNOWN;
        }
    }

    /**
     * Информация о процессоре.
     */
    public static class CpuInfo {
        public final String name;
        public final int cores;
        public final int threads;
        public final String frequency;
        public final CpuArch architecture;

        public CpuInfo(String name, int cores, int threads, String frequency, CpuArch architecture) {
            this.name = name;
            this.cores = cores;
            this.threads = threads;
            this.frequency = frequency;
            this.architecture = architecture;
        }

        @Override
        public String toString() {
            return "CpuInfo{" +
                "name='" + name + '\'' +
                ", cores=" + cores +
                ", threads=" + threads +
                ", frequency='" + frequency + '\'' +
                ", architecture=" + architecture +
                '}';
        }
    }

    /**
     * Получить информацию о процессоре.
     */
    public static CpuInfo getCpuInfo() {
        int cores = Runtime.getRuntime().availableProcessors();
        int threads = cores;
        CpuArch arch = getCpuArch();
        String name = getCpuName();
        String frequency = getCpuFrequency();

        return new CpuInfo(name, cores, threads, frequency, arch);
    }

    /**
     * Получить частоту процессора.
     */
    private static String getCpuFrequency() {
        try {
            switch (platform) {
                case LINUX:
                    File cpuinfo = new File("/proc/cpuinfo");
                    if (cpuinfo.exists()) {
                        String[] lines = readFileLines(cpuinfo);
                        for (String line : lines) {
                            if (line.toLowerCase().startsWith("cpu mhz")) {
                                String[] parts = line.split(":");
                                if (parts.length > 1) {
                                    try {
                                        float freq = Float.parseFloat(parts[1].trim());
                                        return String.format(Locale.US, "%.1f GHz", freq / 1000);
                                    } catch (NumberFormatException e) {
                                        return "Unknown";
                                    }
                                }
                            }
                        }
                    }
                    return "Unknown";
                case WINDOWS:
                    String freq = runCommandWithTimeout(
                        new String[]{"powershell", "-Command", "Get-CimInstance Win32_Processor | Select-Object -ExpandProperty MaxClockSpeed"},
                        3000
                    );
                    if (freq != null) {
                        String[] lines = freq.split("\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (!line.isEmpty() && line.matches("\\d+")) {
                                try {
                                    int mhz = Integer.parseInt(line);
                                    return (mhz / 1000) + " GHz";
                                } catch (NumberFormatException e) {
                                    return "Unknown";
                                }
                            }
                        }
                    }
                    return "Unknown";
                default:
                    return "Unknown";
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // ========================================================================
    // 3. ПРОВЕРКА ПУТЕЙ ДО НУЖНЫХ ПАПОК
    // ========================================================================

    /**
     * Типы директорий.
     */
    public enum DirectoryType {
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
    public static final Path userHome = Paths.get(System.getProperty("user.home"));

    /**
     * Основная директория для конфигов.
     */
    public static final Path mainDirectory = createMainDirectory();

    private static Path createMainDirectory() {
        switch (platform) {
            case WINDOWS:
                return userHome.resolve(".apr");
            case LINUX:
            case MAC:
                return userHome.resolve(".config").resolve(".apr");
            default:
                return userHome.resolve(".apr");
        }
    }

    /**
     * Директория кэша.
     */
    public static final Path cacheDirectory = createCacheDirectory();

    private static Path createCacheDirectory() {
        switch (platform) {
            case WINDOWS:
                String appData = System.getenv("APPDATA");
                if (appData != null) {
                    return Paths.get(appData, "Aporia.cc");
                }
                return userHome.resolve("Aporia.cc");
            case LINUX:
                return userHome.resolve(".cache").resolve("Aporia");
            case MAC:
                return userHome.resolve("Library").resolve("Caches").resolve("Aporia");
            default:
                return userHome.resolve(".cache").resolve("Aporia");
        }
    }

    /**
     * Директория данных.
     */
    public static final Path dataDirectory = createDataDirectory();

    private static Path createDataDirectory() {
        switch (platform) {
            case WINDOWS:
                String localAppData = System.getenv("LOCALAPPDATA");
                if (localAppData != null) {
                    return Paths.get(localAppData, "Aporia");
                }
                return userHome.resolve("Aporia");
            case LINUX:
                return userHome.resolve(".local").resolve("share").resolve("Aporia");
            case MAC:
                return userHome.resolve("Library").resolve("Application Support").resolve("Aporia");
            default:
                return userHome.resolve(".local").resolve("share").resolve("Aporia");
        }
    }

    /**
     * Директория логов.
     */
    public static final Path logsDirectory = createLogsDirectory();

    private static Path createLogsDirectory() {
        switch (platform) {
            case WINDOWS:
            case LINUX:
                return dataDirectory.resolve("logs");
            case MAC:
                return userHome.resolve("Library").resolve("Logs").resolve("Aporia");
            default:
                return dataDirectory.resolve("logs");
        }
    }

    /**
     * Временная директория.
     */
    public static final Path tempDirectory = cacheDirectory.resolve("temp");

    /**
     * Директория резервных копий.
     */
    public static final Path backupDirectory = mainDirectory.resolve("backup");

    /**
     * Директория модулей.
     */
    public static final Path modulesDirectory = mainDirectory.resolve("modules");

    /**
     * Директория тем.
     */
    public static final Path themesDirectory = mainDirectory.resolve("themes");

    /**
     * Директория скриптов.
     */
    public static final Path scriptsDirectory = mainDirectory.resolve("scripts");

    /**
     * Получить директорию по типу.
     */
    public static Path getDirectory(DirectoryType type) {
        switch (type) {
            case CONFIG:
                return mainDirectory;
            case CACHE:
                return cacheDirectory;
            case DATA:
                return dataDirectory;
            case LOGS:
                return logsDirectory;
            case TEMP:
                return tempDirectory;
            case BACKUP:
                return backupDirectory;
            case MODULES:
                return modulesDirectory;
            case THEMES:
                return themesDirectory;
            case SCRIPTS:
                return scriptsDirectory;
            default:
                return mainDirectory;
        }
    }

    /**
     * Получить файл в директории.
     */
    public static Path getFile(DirectoryType type, String fileName) {
        return getDirectory(type).resolve(fileName);
    }

    /**
     * Проверить существование пути.
     */
    public static boolean pathExists(Path path) {
        return path.toFile().exists();
    }

    /**
     * Проверить, доступна ли директория для записи.
     */
    public static boolean isWritable(Path path) {
        try {
            File file = path.toFile();
            if (!file.exists()) {
                file.mkdirs();
            }
            return file.canWrite();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Создать директорию.
     */
    public static boolean createDirectory(Path path) {
        try {
            File file = path.toFile();
            if (!file.exists()) {
                return file.mkdirs();
            }
            return file.exists();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Создать все необходимые директории.
     */
    public static boolean createAllDirectories() {
        try {
            Path[] dirs = {
                mainDirectory, cacheDirectory, dataDirectory, logsDirectory,
                tempDirectory, backupDirectory, modulesDirectory, themesDirectory, scriptsDirectory
            };
            for (Path dir : dirs) {
                if (!createDirectory(dir)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** ========================================================================
     * 4. ПРОВЕРКА ПОЛЬЗОВАТЕЛЯ
     * ========================================================================
     *
     * Имя текущего пользователя.
     */
    public static final String userName = System.getProperty("user.name");

    /**
     * Домашняя директория пользователя.
     */
    public static final String userDir = System.getProperty("user.dir");

    /**
     * Получить информацию о пользователе.
     */
    public static UserInfo getUserInfo() {
        return new UserInfo(
            userName,
            userHome.toString(),
            isRoot(),
            isAdmin(),
            isVirtualMachine(),
            isContainer()
        );
    }

    /**
     * Проверить, запущено ли от root (Linux/Mac).
     */
    public static boolean isRoot() {
        try {
            switch (platform) {
                case LINUX:
                case MAC:
                    String user = runCommand("whoami");
                    if (user != null) {
                        return user.trim().toLowerCase().equals("root");
                    }
                    return false;
                case WINDOWS:
                    return isAdmin();
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверить, запущено ли от администратора (Windows).
     */
    public static boolean isAdmin() {
        try {
            switch (platform) {
                case WINDOWS:
                    Process process = Runtime.getRuntime().exec(
                        new String[]{"cmd.exe", "/c", "net", "session"}
                    );
                    return process.waitFor() == 0;
                case LINUX:
                case MAC:
                    return isRoot();
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверить, запущено ли в виртуальной машине.
     */
    public static boolean isVirtualMachine() {
        try {
            switch (platform) {
                case WINDOWS:
                    try {
                        File boardVendor = new File("/sys/class/dmi/id/board_vendor");
                        if (boardVendor.exists()) {
                            String[] lines = readFileLines(boardVendor);
                            for (String line : lines) {
                                String vendor = line.toLowerCase();
                                if (vendor.contains("vmware") || vendor.contains("virtualbox") || 
                                    vendor.contains("qemu") || vendor.contains("xen")) {
                                    return true;
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    String bios = runCommandWithTimeout(
                        new String[]{"powershell", "-Command", "Get-CimInstance Win32_BIOS | Select-Object -ExpandProperty Manufacturer"},
                        3000
                    );
                    if (bios != null) {
                        String lowerBios = bios.toLowerCase();
                        return lowerBios.contains("vmware") ||
                               lowerBios.contains("virtualbox") ||
                               lowerBios.contains("qemu") ||
                               (lowerBios.contains("microsoft") && lowerBios.contains("hyper-v"));
                    }
                    return false;
                    
                case LINUX:
                    try {
                        File productName = new File("/sys/class/dmi/id/product_name");
                        File boardVendor = new File("/sys/class/dmi/id/board_vendor");
                        File hypervisor = new File("/sys/hypervisor/type");
                        
                        StringBuilder dmiInfo = new StringBuilder();
                        if (productName.exists()) {
                            String[] lines = readFileLines(productName);
                            for (String line : lines) {
                                dmiInfo.append(line.toLowerCase());
                            }
                        }
                        if (boardVendor.exists()) {
                            String[] lines = readFileLines(boardVendor);
                            for (String line : lines) {
                                dmiInfo.append(line.toLowerCase());
                            }
                        }
                        if (hypervisor.exists()) {
                            String[] lines = readFileLines(hypervisor);
                            for (String line : lines) {
                                dmiInfo.append(line.toLowerCase());
                            }
                        }
                        
                        String dmi = dmiInfo.toString();
                        return dmi.contains("vmware") ||
                               dmi.contains("virtualbox") ||
                               dmi.contains("qemu") ||
                               dmi.contains("kvm") ||
                               dmi.contains("xen") ||
                               dmi.contains("hypervisor");
                    } catch (Exception e) {
                        return false;
                    }
                    
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Проверить, запущено ли в контейнере (Docker/LXC).
     */
    public static boolean isContainer() {
        try {
            switch (platform) {
                case LINUX:
                    File dockerenv = new File("/.dockerenv");
                    if (dockerenv.exists()) {
                        return true;
                    }
                    
                    File cgroup = new File("/proc/1/cgroup");
                    if (cgroup.exists()) {
                        String[] lines = readFileLines(cgroup);
                        for (String line : lines) {
                            if (line.contains("docker") || line.contains("lxc")) {
                                return true;
                            }
                        }
                    }
                    return false;
                    
                case WINDOWS:
                    return System.getenv("KUBERNETES_SERVICE_HOST") != null ||
                           System.getenv("CONTAINER") != null;
                           
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    // ========================================================================
    // 5. ИНФОРМАЦИЯ О ЖЕЛЕЗЕ
    // ========================================================================

    /**
     * Информация об оперативной памяти.
     */
    public static class RamInfo {
        public final long total;
        public final long free;
        public final long max;
        public final long totalPhysical;
        public final long freePhysical;

        public RamInfo(long total, long free, long max, long totalPhysical, long freePhysical) {
            this.total = total;
            this.free = free;
            this.max = max;
            this.totalPhysical = totalPhysical;
            this.freePhysical = freePhysical;
        }

        @Override
        public String toString() {
            return "RamInfo{" +
                "total=" + total +
                ", free=" + free +
                ", max=" + max +
                ", totalPhysical=" + totalPhysical +
                ", freePhysical=" + freePhysical +
                '}';
        }
    }

    /**
     * Получить информацию об оперативной памяти.
     */
    public static RamInfo getRamInfo() {
        Runtime runtime = Runtime.getRuntime();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long max = runtime.maxMemory();

        long totalPhysical = 0;
        long freePhysical = 0;

        try {
            switch (platform) {
                case WINDOWS:
                    String totalMemKbStr = runCommandWithTimeout(
                        new String[]{"powershell", "-Command", 
                            "(Get-CimInstance Win32_OperatingSystem).TotalVisibleMemorySize"},
                        3000
                    );
                    String freeMemKbStr = runCommandWithTimeout(
                        new String[]{"powershell", "-Command", 
                            "(Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory"},
                        3000
                    );
                    
                    if (totalMemKbStr != null) {
                        try {
                            long totalMemKb = Long.parseLong(totalMemKbStr.trim());
                            totalPhysical = totalMemKb * 1024;
                        } catch (NumberFormatException ignored) {}
                    }
                    
                    if (freeMemKbStr != null) {
                        try {
                            long freeMemKb = Long.parseLong(freeMemKbStr.trim());
                            freePhysical = freeMemKb * 1024;
                        } catch (NumberFormatException ignored) {}
                    }
                    
                    if (totalPhysical == 0) {
                        totalPhysical = Runtime.getRuntime().totalMemory();
                        freePhysical = Runtime.getRuntime().freeMemory();
                    }
                    break;
                    
                case LINUX:
                    File meminfo = new File("/proc/meminfo");
                    if (meminfo.exists()) {
                        String[] lines = readFileLines(meminfo);
                        for (String line : lines) {
                            if (line.startsWith("MemTotal:")) {
                                String[] parts = line.split("\\s+");
                                if (parts.length > 1) {
                                    try {
                                        totalPhysical = Long.parseLong(parts[1]) * 1024;
                                    } catch (NumberFormatException ignored) {}
                                }
                            } else if (line.startsWith("MemFree:")) {
                                String[] parts = line.split("\\s+");
                                if (parts.length > 1) {
                                    try {
                                        freePhysical = Long.parseLong(parts[1]) * 1024;
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                        }
                    }
                    break;
                    
                case MAC:
                    String totalMem = runCommandWithTimeout(
                        new String[]{"sysctl", "-n", "hw.memsize"},
                        3000
                    );
                    if (totalMem != null) {
                        try {
                            totalPhysical = Long.parseLong(totalMem.trim());
                        } catch (NumberFormatException ignored) {}
                    }
                    freePhysical = free;
                    break;
                    
                default:
                    totalPhysical = total;
                    freePhysical = free;
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error getting RAM info: " + e.getMessage());
            totalPhysical = total;
            freePhysical = free;
        }

        return new RamInfo(total, free, max, totalPhysical, freePhysical);
    }

    /**
     * Информация о диске.
     */
    public static class DiskInfo {
        public final long total;
        public final long free;
        public final long usable;
        public final String path;

        public DiskInfo(long total, long free, long usable, String path) {
            this.total = total;
            this.free = free;
            this.usable = usable;
            this.path = path;
        }

        @Override
        public String toString() {
            return "DiskInfo{" +
                "total=" + total +
                ", free=" + free +
                ", usable=" + usable +
                ", path='" + path + '\'' +
                '}';
        }
    }

    /**
     * Получить информацию о диске.
     */
    public static DiskInfo getDiskInfo() {
        return getDiskInfo(userHome);
    }

    /**
     * Получить информацию о диске по указанному пути.
     */
    public static DiskInfo getDiskInfo(Path path) {
        try {
            File file = path.toFile();
            return new DiskInfo(
                file.getTotalSpace(),
                file.getFreeSpace(),
                file.getUsableSpace(),
                path.toString()
            );
        } catch (Exception e) {
            return new DiskInfo(0, 0, 0, path.toString());
        }
    }

    /**
     * Информация о видеокарте.
     */
    public static class GpuInfo {
        public final String name;
        public final String vendor;
        public final String version;

        public GpuInfo(String name, String vendor, String version) {
            this.name = name;
            this.vendor = vendor;
            this.version = version;
        }

        @Override
        public String toString() {
            return "GpuInfo{" +
                "name='" + name + '\'' +
                ", vendor='" + vendor + '\'' +
                ", version='" + version + '\'' +
                '}';
        }
    }

    /** Кэш GPU info */
    private static GpuInfo cachedGpuInfo = null;

    /**
     * Получить информацию о видеокарте.
     */
    public static GpuInfo getGpuInfo() {
        if (cachedGpuInfo != null) {
            return cachedGpuInfo;
        }

        try {
            String glVendor = System.getProperty("org.lwjgl.opengl.GL11.GL_VENDOR");
            String glRenderer = System.getProperty("org.lwjgl.opengl.GL11.GL_RENDERER");
            String glVersion = System.getProperty("org.lwjgl.opengl.GL11.GL_VERSION");

            GpuInfo gpuInfo;
            if (glRenderer != null && !glRenderer.isEmpty() && !glRenderer.equals("Unknown")) {
                String vendor = determineGpuVendor(glRenderer, glVendor);
                gpuInfo = new GpuInfo(glRenderer, vendor, glVersion != null ? glVersion : "Unknown");
            } else {
                String gpuName = null;
                switch (platform) {
                    case WINDOWS:
                        gpuName = runCommandWithTimeout(
                            new String[]{"powershell", "-Command", "Get-CimInstance Win32_VideoController | Select-Object -ExpandProperty Name"},
                            3000
                        );
                        if (gpuName != null) {
                            String[] lines = gpuName.split("\n");
                            for (String line : lines) {
                                line = line.trim();
                                if (!line.isEmpty()) {
                                    gpuName = line;
                                    break;
                                }
                            }
                        }
                        break;
                        
                    case LINUX:
                        String lspci = runCommandWithTimeout(
                            new String[]{"lspci", "-v"},
                            3000
                        );
                        if (lspci != null) {
                            String[] lines = lspci.split("\n");
                            for (String line : lines) {
                                String lowerLine = line.toLowerCase();
                                if (lowerLine.contains("vga") || lowerLine.contains("3d")) {
                                    int colonIndex = line.indexOf(':');
                                    if (colonIndex >= 0 && colonIndex + 1 < line.length()) {
                                        gpuName = line.substring(colonIndex + 1).trim();
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                        
                    case MAC:
                        String sp = runCommandWithTimeout(
                            new String[]{"system_profiler", "SPDisplaysDataType"},
                            3000
                        );
                        if (sp != null) {
                            String[] lines = sp.split("\n");
                            for (String line : lines) {
                                if (line.contains("Chipset Model:")) {
                                    int colonIndex = line.indexOf(':');
                                    if (colonIndex >= 0 && colonIndex + 1 < line.length()) {
                                        gpuName = line.substring(colonIndex + 1).trim();
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                        
                    default:
                        break;
                }
                
                String vendor = determineGpuVendor(gpuName != null ? gpuName : "", glVendor);
                gpuInfo = new GpuInfo(gpuName != null ? gpuName : "Unknown", vendor, glVersion != null ? glVersion : "Unknown");
            }

            cachedGpuInfo = gpuInfo;
            return gpuInfo;
        } catch (Exception e) {
            System.err.println("Error getting GPU info: " + e.getMessage());
            cachedGpuInfo = new GpuInfo("Unknown", "Unknown", "Unknown");
            return cachedGpuInfo;
        }
    }

    /**
     * Определить vendor GPU по имени рендерера.
     */
    private static String determineGpuVendor(String renderer, String glVendor) {
        if (glVendor != null && !glVendor.isEmpty() && !glVendor.equals("Unknown")) {
            return glVendor;
        }

        String rendererLower = renderer.toLowerCase();
        if (rendererLower.contains("nvidia") || rendererLower.contains("geforce") || 
            rendererLower.contains("quadro") || rendererLower.contains("rtx") ||
            rendererLower.contains("gtx")) {
            return "NVIDIA";
        } else if (rendererLower.contains("intel") || rendererLower.contains("iris") ||
                   rendererLower.contains("uhd graphics") || rendererLower.contains("hd graphics")) {
            return "Intel";
        } else if (rendererLower.contains("amd") || rendererLower.contains("radeon") ||
                   rendererLower.contains("rx ") || rendererLower.contains("vega")) {
            return "AMD";
        } else if (rendererLower.contains("apple")) {
            return "Apple";
        } else {
            return "Unknown";
        }
    }

    /**
     * Сбросить кэш GPU info (если нужно обновить).
     */
    public static void resetGpuCache() {
        cachedGpuInfo = null;
    }

    /**
     * Полная информация о системе.
     */
    public static class OsDetails {
        public final Platform platform;
        public final String name;
        public final String version;
        public final String arch;
        public final String kernel;
        public final boolean is64Bit;
        public final boolean isVM;
        public final boolean isContainer;

        public OsDetails(Platform platform, String name, String version, String arch, 
                         String kernel, boolean is64Bit, boolean isVM, boolean isContainer) {
            this.platform = platform;
            this.name = name;
            this.version = version;
            this.arch = arch;
            this.kernel = kernel;
            this.is64Bit = is64Bit;
            this.isVM = isVM;
            this.isContainer = isContainer;
        }

        @Override
        public String toString() {
            return "OsDetails{" +
                "platform=" + platform +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", arch='" + arch + '\'' +
                ", kernel='" + kernel + '\'' +
                ", is64Bit=" + is64Bit +
                ", isVM=" + isVM +
                ", isContainer=" + isContainer +
                '}';
        }
    }

    /**
     * Информация о пользователе.
     */
    public static class UserInfo {
        public final String name;
        public final String homeDir;
        public final boolean isRoot;
        public final boolean isAdmin;
        public final boolean isVM;
        public final boolean isContainer;

        public UserInfo(String name, String homeDir, boolean isRoot, boolean isAdmin, 
                        boolean isVM, boolean isContainer) {
            this.name = name;
            this.homeDir = homeDir;
            this.isRoot = isRoot;
            this.isAdmin = isAdmin;
            this.isVM = isVM;
            this.isContainer = isContainer;
        }

        @Override
        public String toString() {
            return "UserInfo{" +
                "name='" + name + '\'' +
                ", homeDir='" + homeDir + '\'' +
                ", isRoot=" + isRoot +
                ", isAdmin=" + isAdmin +
                ", isVM=" + isVM +
                ", isContainer=" + isContainer +
                '}';
        }
    }

    /**
     * Получить полную информацию о системе.
     */
    public static SystemInfo getFullSystemInfo() {
        return new SystemInfo(
            getOsDetails(),
            getUserInfo(),
            getCpuInfo(),
            getRamInfo(),
            getDiskInfo(),
            getGpuInfo()
        );
    }

    /**
     * Полная информация о системе.
     */
    public static class SystemInfo {
        public final OsDetails os;
        public final UserInfo user;
        public final CpuInfo cpu;
        public final RamInfo ram;
        public final DiskInfo disk;
        public final GpuInfo gpu;

        public SystemInfo(OsDetails os, UserInfo user, CpuInfo cpu, 
                         RamInfo ram, DiskInfo disk, GpuInfo gpu) {
            this.os = os;
            this.user = user;
            this.cpu = cpu;
            this.ram = ram;
            this.disk = disk;
            this.gpu = gpu;
        }

        @Override
        public String toString() {
            return "SystemInfo{" +
                "os=" + os +
                ", user=" + user +
                ", cpu=" + cpu +
                ", ram=" + ram +
                ", disk=" + disk +
                ", gpu=" + gpu +
                '}';
        }
    }

    // ========================================================================
    // УТИЛИТЫ
    // ========================================================================

    /**
     * Выполнить команду с таймаутом.
     */
    private static String runCommandWithTimeout(String[] command, long timeoutMs) {
        try {
            Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

            if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                System.err.println("Command timed out: " + String.join(" ", command));
                return null;
            }

            if (process.exitValue() != 0) {
                return null;
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append("\n");
                    }
                    output.append(line);
                }
            }
            return output.toString().trim();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Выполнить команду.
     */
    private static String runCommand(String... command) {
        return runCommandWithTimeout(command, 5000);
    }

    /**
     * Получить имя CPU.
     */
    private static String getCpuName() {
        try {
            switch (platform) {
                case WINDOWS:
                    String cpuName = runCommandWithTimeout(
                        new String[]{"powershell", "-Command", "Get-CimInstance Win32_Processor | Select-Object -ExpandProperty Name"},
                        3000
                    );
                    if (cpuName != null) {
                        String[] lines = cpuName.split("\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (!line.isEmpty()) {
                                return line;
                            }
                        }
                    }
                    return "Unknown CPU";
                    
                case LINUX:
                    File cpuinfo = new File("/proc/cpuinfo");
                    if (cpuinfo.exists()) {
                        String[] lines = readFileLines(cpuinfo);
                        for (String line : lines) {
                            if (line.toLowerCase().startsWith("model name")) {
                                String[] parts = line.split(":", 2);
                                if (parts.length > 1) {
                                    return parts[1].trim();
                                }
                            }
                        }
                    }
                    return "Unknown CPU";
                    
                case MAC:
                    String cpuBrand = runCommandWithTimeout(
                        new String[]{"sysctl", "-n", "machdep.cpu.brand_string"},
                        3000
                    );
                    if (cpuBrand != null) {
                        return cpuBrand.trim();
                    }
                    return "Unknown CPU";
                    
                default:
                    return "Unknown CPU";
            }
        } catch (Exception e) {
            return "Unknown CPU";
        }
    }

    /**
     * Чтение строк из файла.
     */
    private static String[] readFileLines(File file) {
        try {
            return java.nio.file.Files.readAllLines(file.toPath()).toArray(new String[0]);
        } catch (Exception e) {
            return new String[0];
        }
    }

    /**
     * Определить платформу.
     */
    private static Platform detectPlatform() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return Platform.WINDOWS;
        } else if (os.contains("mac")) {
            return Platform.MAC;
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return Platform.LINUX;
        } else {
            return Platform.UNKNOWN;
        }
    }

    /**
     * Получить строковое представление платформы.
     */
    public static String getPlatformName() {
        switch (platform) {
            case WINDOWS:
                return "Windows";
            case LINUX:
                return "Linux";
            case MAC:
                return "macOS";
            default:
                return "Unknown";
        }
    }

    /**
     * Получить разделитель путей.
     */
    public static String getPathSeparator() {
        switch (platform) {
            case WINDOWS:
                return "\\";
            case LINUX:
            case MAC:
            default:
                return "/";
        }
    }

    /**
     * Форматировать размер в байтах.
     */
    public static String formatBytes(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double size = bytes;
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format(Locale.US, "%.2f %s", size, units[unitIndex]);
    }

    /**
     * Вывести полную информацию о системе в консоль.
     */
    public static void printSystemInfo() {
        SystemInfo info = getFullSystemInfo();
        
        System.out.println("=== System Information ===");
        System.out.println("OS: " + info.os.name + " " + info.os.version + " (" + info.os.arch + ")");
        System.out.println("Kernel: " + info.os.kernel);
        System.out.println("64-bit: " + info.os.is64Bit);
        System.out.println("VM: " + info.os.isVM + ", Container: " + info.os.isContainer);
        System.out.println("User: " + info.user.name + " (Admin: " + info.user.isAdmin + ", Root: " + info.user.isRoot + ")");
        System.out.println("CPU: " + info.cpu.name + " (" + info.cpu.cores + " cores, " + info.cpu.frequency + ")");
        System.out.println("RAM: " + formatBytes(info.ram.totalPhysical) + " total, " + formatBytes(info.ram.freePhysical) + " free");
        System.out.println("Disk: " + formatBytes(info.disk.total) + " total, " + formatBytes(info.disk.free) + " free");
        System.out.println("GPU: " + info.gpu.name + " (" + info.gpu.vendor + ")");
        System.out.println("=========================");
    }

    // ========================================================================
    // 7. ДАТА / ВРЕМЯ
    // ========================================================================

    /**
     * Текущая дата и время.
     */
    public static LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }

    /**
     * Текущая дата и время в формате ISO 8601.
     */
    public static String getCurrentDateTimeIso() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(getCurrentTime());
    }

    /**
     * Текущее время в формате HH:mm:ss.
     */
    public static String getCurrentTimeString() {
        return DateTimeFormatter.ofPattern("HH:mm:ss").format(getCurrentTime());
    }

    /**
     * Текущая дата в формате DD.MM.YYYY.
     */
    public static String getCurrentDateString() {
        return DateTimeFormatter.ofPattern("dd.MM.yyyy").format(getCurrentTime());
    }

    /**
     * Получить текущее время с заданным форматом.
     */
    public static String getTimeFormatted(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).format(LocalDateTime.now());
    }

    /**
     * Получить текущее время в миллисекундах (epoch).
     */
    public static long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Получить часовой пояс системы.
     */
    public static ZoneId getSystemTimeZone() {
        return ZoneId.systemDefault();
    }

    // ========================================================================
    // 8. ПОГОДА / ГЕОЛОКАЦИЯ
    // ========================================================================

    /**
     * Информация о геолокации.
     */
    public static class LocationInfo {
        public final String city;
        public final String region;
        public final String country;
        public final String ip;
        public final String timezone;

        public LocationInfo(String city, String region, String country, String ip, String timezone) {
            this.city = city;
            this.region = region;
            this.country = country;
            this.ip = ip;
            this.timezone = timezone;
        }

        @Override
        public String toString() {
            return "LocationInfo{" +
                "city='" + city + '\'' +
                ", region='" + region + '\'' +
                ", country='" + country + '\'' +
                ", ip='" + ip + '\'' +
                ", timezone='" + timezone + '\'' +
                '}';
        }
    }

    /**
     * Информация о погоде.
     */
    public static class WeatherInfo {
        public final String city;
        public final String temperature;
        public final String feelsLike;
        public final String description;
        public final String humidity;
        public final String windSpeed;

        public WeatherInfo(String city, String temperature, String feelsLike, 
                          String description, String humidity, String windSpeed) {
            this.city = city;
            this.temperature = temperature;
            this.feelsLike = feelsLike;
            this.description = description;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
        }

        @Override
        public String toString() {
            return "WeatherInfo{" +
                "city='" + city + '\'' +
                ", temperature='" + temperature + '\'' +
                ", feelsLike='" + feelsLike + '\'' +
                ", description='" + description + '\'' +
                ", humidity='" + humidity + '\'' +
                ", windSpeed='" + windSpeed + '\'' +
                '}';
        }
    }

    /** Кэш геолокации (TTL 30 минут) */
    private static LocationInfo cachedLocation = null;
    private static long locationCacheTime = 0;
    private static final long LOCATION_TTL = 30 * 60 * 1000L;

    /** Кэш погоды (TTL 15 минут) */
    private static WeatherInfo cachedWeather = null;
    private static long weatherCacheTime = 0;
    private static final long WEATHER_TTL = 15 * 60 * 1000L;

    /**
     * Включить погоду/геолокацию (по умолчанию false).
     */
    public static boolean enableLocationServices = false;

    /**
     * Получить геолокацию по IP.
     */
    public static LocationInfo getLocation() {
        if (!enableLocationServices) return null;

        long now = System.currentTimeMillis();
        if (cachedLocation != null && now - locationCacheTime < LOCATION_TTL) {
            return cachedLocation;
        }

        try {
            URL url = new URL("http://ipwho.is/json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

            LocationInfo location = new LocationInfo(
                json.has("city") && !json.get("city").isJsonNull() ? json.get("city").getAsString() : null,
                json.has("region") && !json.get("region").isJsonNull() ? json.get("region").getAsString() : null,
                json.has("country") && !json.get("country").isJsonNull() ? json.get("country").getAsString() : null,
                json.has("ip") && !json.get("ip").isJsonNull() ? json.get("ip").getAsString() : null,
                json.has("timezone") && !json.get("timezone").isJsonNull() ? json.get("timezone").getAsString() : null
            );

            cachedLocation = location;
            locationCacheTime = now;

            connection.disconnect();
            return location;
        } catch (Exception e) {
            System.err.println("Failed to get location: " + e.getMessage());
            return null;
        }
    }

    /**
     * Получить погоду через wttr.in.
     */
    public static WeatherInfo getWeather() {
        if (!enableLocationServices) return null;

        long now = System.currentTimeMillis();
        if (cachedWeather != null && now - weatherCacheTime < WEATHER_TTL) {
            return cachedWeather;
        }

        try {
            URL url = new URL("http://wttr.in/?format=j1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "AporiaClient/1.0");

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

            JsonObject currentCondition = json.getAsJsonArray("current_condition").get(0).getAsJsonObject();
            JsonObject nearestArea = json.getAsJsonArray("nearest_area").get(0).getAsJsonObject();

            String city = nearestArea.getAsJsonArray("areaName").get(0).getAsJsonObject().get("value").getAsString();
            String temperature = currentCondition.get("temp_C").getAsString();
            String feelsLike = currentCondition.get("FeelsLikeC").getAsString();
            String description = currentCondition.getAsJsonArray("weatherDesc").get(0).getAsJsonObject().get("value").getAsString();
            String humidity = currentCondition.get("humidity").getAsString();
            String windSpeed = currentCondition.get("windspeedKmph").getAsString();

            WeatherInfo weather = new WeatherInfo(
                city, temperature, feelsLike, description, humidity, windSpeed
            );

            cachedWeather = weather;
            weatherCacheTime = now;

            connection.disconnect();
            return weather;
        } catch (Exception e) {
            System.err.println("Failed to get weather: " + e.getMessage());
            return null;
        }
    }

    /**
     * Получить погоду для конкретного города.
     */
    public static WeatherInfo getWeatherForCity(String city) {
        if (!enableLocationServices) return null;

        try {
            String encodedCity = URLEncoder.encode(city, "UTF-8");
            URL url = new URL("http://wttr.in/" + encodedCity + "?format=j1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "AporiaClient/1.0");

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
            JsonObject currentCondition = json.getAsJsonArray("current_condition").get(0).getAsJsonObject();

            String temperature = currentCondition.get("temp_C").getAsString();
            String feelsLike = currentCondition.get("FeelsLikeC").getAsString();
            String description = currentCondition.getAsJsonArray("weatherDesc").get(0).getAsJsonObject().get("value").getAsString();
            String humidity = currentCondition.get("humidity").getAsString();
            String windSpeed = currentCondition.get("windspeedKmph").getAsString();

            connection.disconnect();
            return new WeatherInfo(city, temperature, feelsLike, description, humidity, windSpeed);
        } catch (Exception e) {
            System.err.println("Failed to get weather for " + city + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Сбросить кэш геолокации и погоды.
     */
    public static void clearLocationCache() {
        cachedLocation = null;
        cachedWeather = null;
        locationCacheTime = 0;
        weatherCacheTime = 0;
    }

    // ========================================================================
    // 9. ЛОГГЕР
    // ========================================================================

    /**
     * Флаг включения логгера OsManager.
     */
    public static boolean loggerEnabled = false;

    /**
     * Инициализировать логгер OsManager.
     */
    public static void initLogger() {
        if (loggerEnabled) {
            System.out.println("OsManager logger already initialized");
            return;
        }

        loggerEnabled = true;

        String banner = "\n" +
        "╔═══════════════════════════════════════════════════════════╗\n" +
        "║                                                           ║\n" +
        "║    █████╗ ██╗      █████╗  ██████╗██████╗ ██╗████████╗    ║\n" +
        "║   ██╔══██╗██║     ██╔══██╗██╔════╝██╔══██╗██║╚══██╔══╝    ║\n" +
        "║   ███████║██║     ███████║██║     ██████╔╝██║   ██║       ║\n" +
        "║   ██╔══██║██║     ██╔══██║██║     ██╔══██╗██║   ██║       ║\n" +
        "║   ██║  ██║███████╗██║  ██║╚██████╗██║  ██║██║   ██║       ║\n" +
        "║   ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝╚═╝   ╚═╝       ║\n" +
        "║                                                           ║\n" +
        "║                   . C C                                   ║\n" +
        "║                                                           ║\n" +
        "║              O S   M A N A G E R                          ║\n" +
        "║                                                           ║\n" +
        "╚═══════════════════════════════════════════════════════════╝\n";

        System.out.println(banner);
        System.out.println("OsManager initialized");
        System.out.println("Platform: " + getPlatformName() + " " + osVersion);
        System.out.println("Architecture: " + osArch);
        System.out.println("User: " + userName);
        System.out.println("================================================");
    }

    /**
     * Вывести полную информацию о системе в лог.
     */
    public static void logFullSystemInfo() {
        if (!loggerEnabled) {
            System.err.println("OsManager logger not enabled. Call initLogger() first.");
            return;
        }

        System.out.println("=== Full System Information ===");

        OsDetails osInfo = getOsDetails();
        System.out.println("OS: " + osInfo.name + " " + osInfo.version + " (" + osInfo.arch + ")");
        System.out.println("Kernel: " + osInfo.kernel);
        System.out.println("64-bit: " + osInfo.is64Bit);
        System.out.println("VM: " + osInfo.isVM + ", Container: " + osInfo.isContainer);

        UserInfo userInfo = getUserInfo();
        System.out.println("User: " + userInfo.name + " (Admin: " + userInfo.isAdmin + ", Root: " + userInfo.isRoot + ")");

        CpuInfo cpuInfo = getCpuInfo();
        System.out.println("CPU: " + cpuInfo.name + " (" + cpuInfo.cores + " cores, " + cpuInfo.frequency + ")");

        RamInfo ramInfo = getRamInfo();
        System.out.println("RAM: " + formatBytes(ramInfo.totalPhysical) + " total, " + formatBytes(ramInfo.freePhysical) + " free");

        DiskInfo diskInfo = getDiskInfo();
        System.out.println("Disk: " + formatBytes(diskInfo.total) + " total, " + formatBytes(diskInfo.free) + " free");

        GpuInfo gpuInfo = getGpuInfo();
        System.out.println("GPU: " + gpuInfo.name + " (" + gpuInfo.vendor + ")");

        if (enableLocationServices) {
            LocationInfo location = getLocation();
            if (location != null) {
                System.out.println("Location: " + location.city + ", " + location.region + ", " + location.country);

                WeatherInfo weather = getWeather();
                if (weather != null) {
                    System.out.println("Weather: " + weather.temperature + "°C, " + weather.description + ", " + weather.humidity + "% humidity");
                }
            }
        }

        System.out.println("================================");
    }
    // ========================================================================
// ГЕТТЕРЫ ДЛЯ ПОЛЕЙ (ДОБАВИТЬ В КОНЕЦ КЛАССА)
// ========================================================================

    /**
     * Получить текущую платформу.
     */
    public static Platform getPlatform() {
        return platform;
    }

    /**
     * Получить имя ОС.
     */
    public static String getOsName() {
        return osName;
    }

    /**
     * Получить версию ОС.
     */
    public static String getOsVersion() {
        return osVersion;
    }

    /**
     * Получить архитектуру ОС.
     */
    public static String getOsArch() {
        return osArch;
    }

    /**
     * Получить имя пользователя.
     */
    public static String getUserName() {
        return userName;
    }

    /**
     * Получить домашнюю директорию пользователя.
     */
    public static String getUserDir() {
        return userDir;
    }
}