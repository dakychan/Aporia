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

/**
 * WebView2-backed off-screen browser.
 * Never creates a visible HWND overlay — renders to a texture via pixel capture.
 */
class WebviewWin32(url: String, width: Int, height: Int) : Webview(url, width, height) {

    private var handle: Pointer? = null
    private var ready = false

    // Pixel capture state
    private var captureBuf: Memory? = null
    private var capW = 0
    private var capH = 0

    // Texture
    private val texture = WebviewTexture()

    // Throttle capture: don't capture every single frame
    private var captureInterval = 3 // capture every N render calls
    private var captureCounter = 0

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

        // Immediately hide any native window — we only want off-screen capture
        NativeWebviewBindings.INSTANCE.webview_hide(handle)

        Logger.info("Waiting for WebView2 to be ready...")
        val result = NativeWebviewBindings.INSTANCE.webview_wait_ready(handle, 30000)
        ready = NativeWebviewBindings.INSTANCE.webview_is_ready(handle) != 0
        Logger.info("WebView2 ready: $ready (wait_ready=$result)")

        if (ready) {
            // Ensure HWND stays hidden even after ready
            NativeWebviewBindings.INSTANCE.webview_hide(handle)
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
        handle?.let { NativeWebviewBindings.INSTANCE.webview_send_mouse_move(it, x, y) }
    }

    override fun sendMousePress(x: Int, y: Int, button: Int) {
        handle?.let { NativeWebviewBindings.INSTANCE.webview_send_mouse_press(it, x, y, button) }
    }

    override fun sendMouseRelease(x: Int, y: Int, button: Int) {
        handle?.let { NativeWebviewBindings.INSTANCE.webview_send_mouse_release(it, x, y, button) }
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
     * Capture current WebView2 pixels and upload to texture.
     * Should be called from the render thread.
     */
    fun tickCapture() {
        if (!ready || handle == null) return
        if (!RenderSystem.isOnRenderThread()) return

        captureCounter++
        if (captureCounter < captureInterval) return
        captureCounter = 0

        val w = width
        val h = height
        val needed = w.toLong() * h * 4
        if (needed <= 0) return

        // Ensure capture buffer
        if (captureBuf == null || capW != w || capH != h) {
            capW = w
            capH = h
            captureBuf = Memory(needed)
        }

        val buf = captureBuf ?: return
        val size = NativeWebviewBindings.INSTANCE.webview_capture_rgba(handle, buf, needed.toInt())
        if (size <= 0) return

        val bb = buf.getByteBuffer(0, size.toLong())
        // Native outputs BGRA (JPEG cap + WIC decode → BGRA), upload via GL_BGRA — no CPU conversion
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

    override fun show(screenX: Int, screenY: Int, pixelW: Int, pixelH: Int) {
        // NO-OP: we never show an HWND overlay
    }

    override fun hide() {
        // NO-OP: HWND stays hidden always
    }

    override fun close() {
        ready = false
        handle?.let { NativeWebviewBindings.INSTANCE.webview_destroy(it) }
        handle = null
        texture.close()
        captureBuf = null
        Logger.info("WebviewWin32 closed (texture mode)")
    }
}
