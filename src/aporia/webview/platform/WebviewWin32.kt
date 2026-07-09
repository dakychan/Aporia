package aporia.webview.platform
import aporia.webview.Webview
import aporia.webview.render.WebviewTexture
import com.mojang.blaze3d.systems.RenderSystem
import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.WString
import net.minecraft.client.Minecraft
import so.aporia.utils.files.FilesManager
import so.aporia.utils.user.logger.Logger
import java.nio.ByteBuffer
import java.nio.file.Files
import com.chaos.annotation.ChaosNative
/**
 * WebView2-backed off-screen browser.
 * Uses PrintWindow + DIB section for zero-encoding raw BGRA capture.
 */
@ChaosNative
class WebviewWin32(url: String, width: Int, height: Int) : Webview(url, width, height) {

    private var handle: Pointer? = null
    private var ready = false

    private var captureBuf: Memory? = null
    private var capW = 0
    private var capH = 0

    private val texture = WebviewTexture()

    private var dpiScale = 1.0f

    override fun init(): Boolean {
        Logger.info("WebView2 texture-mode init: ${width}x${height} url=$url")

        val userDataDir = FilesManager.ROOT.resolve(".webview")
        Files.createDirectories(userDataDir)
        val userDataPath = userDataDir.toAbsolutePath().toString()

        handle = NativeWebviewBindings.INSTANCE.webview_create(width, height, WString(url), userDataPath)
        if (handle == null || handle == Pointer.NULL) {
            Logger.error("webview_create returned null")
            return false
        }

        NativeWebviewBindings.INSTANCE.webview_hide(handle)

        Logger.info("Waiting for WebView2 to be ready...")
        val result = NativeWebviewBindings.INSTANCE.webview_wait_ready(handle, 30000)
        ready = NativeWebviewBindings.INSTANCE.webview_is_ready(handle) != 0
        Logger.info("WebView2 ready: $ready (wait_ready=$result)")

        if (ready) {
            NativeWebviewBindings.INSTANCE.webview_hide(handle)
            val dpi = NativeWebviewBindings.INSTANCE.webview_get_dpi(handle)
            dpiScale = dpi / 96.0f
            Logger.info("WebView2 DPI: $dpi (scale=$dpiScale)")
        }

        initialized = ready
        return ready
    }

    override fun navigate(url: String) {
        this.url = url
        handle?.let { NativeWebviewBindings.INSTANCE.webview_navigate(it, WString(url)) }
    }

    override fun loadHtml(html: String, baseUrl: String) {
        handle?.let { NativeWebviewBindings.INSTANCE.webview_load_html(it, WString(html)) }
    }

    override fun evaluateJs(script: String) {
        Logger.debug("evaluateJs not implemented")
    }

    override fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
        handle?.let { NativeWebviewBindings.INSTANCE.webview_resize(it, width, height) }
    }

    override fun sendMouseMove(x: Int, y: Int) {
        val sx = (x * dpiScale).toInt()
        val sy = (y * dpiScale).toInt()
        handle?.let { NativeWebviewBindings.INSTANCE.webview_send_mouse_move(it, sx, sy) }
    }

    override fun sendMousePress(x: Int, y: Int, button: Int) {
        val sx = (x * dpiScale).toInt()
        val sy = (y * dpiScale).toInt()
        handle?.let { NativeWebviewBindings.INSTANCE.webview_send_mouse_press(it, sx, sy, button) }
    }

    override fun sendMouseRelease(x: Int, y: Int, button: Int) {
        val sx = (x * dpiScale).toInt()
        val sy = (y * dpiScale).toInt()
        handle?.let { NativeWebviewBindings.INSTANCE.webview_send_mouse_release(it, sx, sy, button) }
    }

    override fun sendMouseWheel(delta: Double) {
        handle?.let { NativeWebviewBindings.INSTANCE.webview_send_mouse_wheel(it, delta) }
    }

    override fun sendKeyPress(keyCode: Int, scanCode: Int, modifiers: Int) {
        handle?.let { NativeWebviewBindings.INSTANCE.webview_send_key_press(it, keyCode, scanCode, modifiers) }
    }

    override fun sendKeyRelease(keyCode: Int, scanCode: Int, modifiers: Int) {
        handle?.let { NativeWebviewBindings.INSTANCE.webview_send_key_release(it, keyCode, scanCode, modifiers) }
    }

    override fun sendKeyTyped(c: Char, modifiers: Int) {
        handle?.let { NativeWebviewBindings.INSTANCE.webview_send_key_char(it, c) }
    }

    /**
     * Capture WebView2 pixels via PrintWindow → raw BGRA → upload to GL texture.
     * No PNG/JPEG encode or decode — direct pixel copy.
     */
    fun tickCapture() {
        if (!ready || handle == null) return
        if (!RenderSystem.isOnRenderThread()) return

        val w = width
        val h = height
        val needed = w.toLong() * h * 4
        if (needed <= 0) return

        if (captureBuf == null || capW != w || capH != h) {
            capW = w
            capH = h
            captureBuf = Memory(needed)
        }

        val buf = captureBuf ?: return
        val size = NativeWebviewBindings.INSTANCE.webview_capture_rgba(handle, buf, needed.toInt())
        if (size <= 0) return

        val bb = buf.getByteBuffer(0, size.toLong())
        texture.updateTexture(bb, w, h)
    }

    override fun getTextureData(): ByteBuffer? {
        val buf = captureBuf ?: return null
        return buf.getByteBuffer(0, capW.toLong() * capH * 4L)
    }

    override fun isTextureReady(): Boolean = ready && texture.isReady()

    fun getTexture(): WebviewTexture = texture

    override fun setFocus(focused: Boolean) {
        if (focused && handle != null) {
            NativeWebviewBindings.INSTANCE.webview_set_focus(handle)
        }
    }

    override fun getGlTextureId(): Int = texture.getGlTextureId()
    override fun getNativeHandle(): Int = handle?.hashCode() ?: 0

    override fun show(screenX: Int, screenY: Int, pixelW: Int, pixelH: Int) {}

    override fun hide() {}

    override fun close() {
        ready = false
        handle?.let { NativeWebviewBindings.INSTANCE.webview_destroy(it) }
        handle = null
        texture.close()
        captureBuf = null
        Logger.info("WebviewWin32 closed (texture mode)")
    }
}