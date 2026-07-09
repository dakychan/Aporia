package aporia.webview
import aporia.cc.OsManager
import so.aporia.utils.user.logger.Logger
import com.chaos.annotation.ChaosNative
@ChaosNative
object WebviewFactory {

    enum class Backend {
        WINDOWS_WEBVIEW2,
        LINUX_WEBKIT,
        MAC_WKWEBVIEW,
        POJAV_ANDROID,
        NONE
    }

    @JvmStatic
    fun detectBackend(): Backend {
        val os = System.getProperty("os.name").lowercase()

        if (System.getProperty("java.vm.name", "").contains("Dalvik") ||
            os.contains("android") ||
            System.getProperty("pojav.launcher.version") != null) {
            return Backend.POJAV_ANDROID
        }

        if (System.getProperty("mcef.path") != null || System.getProperty("jcef.path") != null) {
            return Backend.NONE
        }

        return when (OsManager.platform) {
            OsManager.Platform.WINDOWS -> Backend.WINDOWS_WEBVIEW2
            OsManager.Platform.LINUX -> Backend.LINUX_WEBKIT
            OsManager.Platform.MAC -> Backend.MAC_WKWEBVIEW
            else -> Backend.NONE
        }
    }

    @JvmStatic
    fun create(url: String, width: Int, height: Int): Webview {
        return when (detectBackend()) {
            Backend.WINDOWS_WEBVIEW2 -> {
                try {
                    aporia.webview.platform.WebviewWin32(url, width, height)
                } catch (t: Throwable) {
                    Logger.error("WebView2 init failed: ${t.message}")
                    aporia.webview.platform.WebviewLinux(url, width, height)
                }
            }
            Backend.LINUX_WEBKIT -> {
                Logger.warn("Linux WebView not yet natively implemented, using stub renderer")
                aporia.webview.platform.WebviewLinux(url, width, height)
            }
            Backend.MAC_WKWEBVIEW -> {
                Logger.warn("macOS WebView not yet natively implemented, using stub renderer")
                aporia.webview.platform.WebviewMac(url, width, height)
            }
            Backend.POJAV_ANDROID -> {
                Logger.warn("Android WebView not yet natively implemented, using stub renderer")
                aporia.webview.platform.WebviewAndroid(url, width, height)
            }
            Backend.NONE -> {
                Logger.warn("No WebView backend available, using stub renderer")
                aporia.webview.platform.WebviewLinux(url, width, height)
            }
        }
    }
}