package aporia.webview.platform
import aporia.cc.OsManager
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import so.aporia.utils.user.logger.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import com.chaos.annotation.ChaosNative
@ChaosNative
interface NativeWebviewBindings : Library {

    fun webview_create(width: Int, height: Int, url: WString, userDataFolder: String?): Pointer?
    fun webview_wait_ready(handle: Pointer?, timeoutMs: Int): Int
    fun webview_is_ready(handle: Pointer?): Int
    fun webview_is_failed(handle: Pointer?): Int
    fun webview_destroy(handle: Pointer?)
    fun webview_navigate(handle: Pointer?, url: WString)
    fun webview_load_html(handle: Pointer?, html: WString)
    fun webview_show(handle: Pointer?, screenX: Int, screenY: Int, pixelW: Int, pixelH: Int)
    fun webview_hide(handle: Pointer?)
    fun webview_set_focus(handle: Pointer?)
    fun webview_resize(handle: Pointer?, width: Int, height: Int)
    fun webview_capture_rgba(handle: Pointer?, outBuf: Pointer?, outBufSize: Int): Int
    fun webview_send_mouse_move(handle: Pointer?, x: Int, y: Int)
    fun webview_send_mouse_press(handle: Pointer?, x: Int, y: Int, button: Int)
    fun webview_send_mouse_release(handle: Pointer?, x: Int, y: Int, button: Int)
    fun webview_send_mouse_wheel(handle: Pointer?, delta: Double)
    fun webview_send_key_press(handle: Pointer?, vk: Int, scancode: Int, modifiers: Int)
    fun webview_send_key_release(handle: Pointer?, vk: Int, scancode: Int, modifiers: Int)
    fun webview_send_key_char(handle: Pointer?, c: Char)
    fun webview_get_dpi(handle: Pointer?): Int

    companion object {
        const val RUNTIME_DIR = "Microsoft.WebView2.FixedVersionRuntime.148.0.3967.70.x64"
        const val RUNTIME_MARKER = "msedgewebview2.exe"

        val INSTANCE: NativeWebviewBindings = loadInstance()

        private fun extract(resource: String) {
            try {
                val arch = getArch()
                val targetDir = OsManager.cacheDirectory.resolve("natives").resolve(arch)
                Files.createDirectories(targetDir)
                val target = targetDir.resolve(resource)
                Files.deleteIfExists(target)

                val resourcePaths = listOf(
                    "/assets/natives/$arch/$resource",
                    "/$resource"
                )

                var inputStream: java.io.InputStream? = null
                for (path in resourcePaths) {
                    inputStream = NativeWebviewBindings::class.java.getResourceAsStream(path)
                    if (inputStream != null) break
                }

                if (inputStream == null) {
                    Logger.warn("Resource not found: $resource")
                    return
                }

                inputStream.use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }
                Logger.info("Extracted $resource to $target")
            } catch (e: Exception) {
                Logger.error("Failed to extract $resource: ${e.message}")
            }
        }

        private fun ensureRuntime() {
            val arch = getArch()
            val runtimePath = OsManager.cacheDirectory.resolve("natives").resolve(arch).resolve(RUNTIME_DIR)
            val marker = runtimePath.resolve(RUNTIME_MARKER)

            if (Files.exists(marker)) {
                Logger.info("WebView2 runtime already present at $runtimePath")
                return
            }

            var srcPath = System.getProperty("aporia.webview2.runtime.path")

            if (srcPath.isNullOrEmpty()) {
                srcPath = System.getenv("APORIA_WEBVIEW2_RUNTIME")
            }

            if (srcPath.isNullOrEmpty()) {
                val guesses = listOf(
                    "D:\\Aporia.loader\\$RUNTIME_DIR",
                    "..\\Aporia.loader\\$RUNTIME_DIR",
                    "../Aporia.loader/$RUNTIME_DIR",
                    "${System.getProperty("user.dir")}\\..\\Aporia.loader\\$RUNTIME_DIR"
                )
                for (guess in guesses) {
                    val p = Path.of(guess)
                    if (Files.exists(p.resolve(RUNTIME_MARKER))) {
                        srcPath = p.toString()
                        break
                    }
                }
            }

            if (srcPath.isNullOrEmpty()) {
                Logger.warn("WebView2 runtime not found. Set aporia.webview2.runtime.path or APORIA_WEBVIEW2_RUNTIME")
                return
            }

            val src = Path.of(srcPath)
            if (!Files.exists(src.resolve(RUNTIME_MARKER))) {
                Logger.warn("WebView2 runtime not valid at $src")
                return
            }

            try {
                Logger.info("Copying WebView2 runtime from $src to $runtimePath")
                Files.walk(src).use { stream ->
                    stream.forEach { source ->
                        try {
                            val target = runtimePath.resolve(src.relativize(source))
                            if (Files.isDirectory(source)) {
                                Files.createDirectories(target)
                            } else {
                                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                            }
                        } catch (e: Exception) {
                            Logger.error("Failed to copy $source: ${e.message}")
                        }
                    }
                }
                Logger.info("WebView2 runtime copied successfully")
            } catch (e: Exception) {
                Logger.error("Failed to copy WebView2 runtime: ${e.message}")
            }
        }

        private fun getArch(): String {
            val arch = System.getProperty("os.arch").lowercase()
            return when {
                arch.contains("64") -> "x64"
                arch.contains("86") -> "x86"
                arch.contains("arm64") || arch.contains("aarch64") -> "arm64"
                else -> "x64"
            }
        }

        private fun loadInstance(): NativeWebviewBindings {
            val arch = getArch()

            extract("webview_native.dll")
            extract("WebView2Loader.dll")

            ensureRuntime()

            val nativePath = OsManager.cacheDirectory.resolve("natives").resolve(arch)
            System.setProperty("jna.library.path", nativePath.toString())

            return try {
                Native.load(nativePath.resolve("webview_native.dll").toString(), NativeWebviewBindings::class.java)
            } catch (t: Throwable) {
                Logger.error("JNA load failed: ${t.message}")
                System.load(nativePath.resolve("webview_native.dll").toString())
                Native.load("webview_native", NativeWebviewBindings::class.java)
            }
        }
    }
}