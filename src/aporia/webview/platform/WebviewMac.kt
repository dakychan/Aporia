package aporia.webview.platform

import aporia.webview.Webview
import java.nio.ByteBuffer

/** macOS WebView stub — replace with WKWebView JNA bindings when available. */
class WebviewMac(url: String, width: Int, height: Int) : Webview(url, width, height) {

    private var buf: ByteBuffer? = null
    private var ready = false

    override fun init(): Boolean {
        initialized = true; ready = true
        buf = ByteBuffer.allocateDirect(width * height * 4)
        fill(); buf?.rewind(); return true
    }

    private fun fill() {
        val b = buf ?: return
        for (y in 0 until height) for (x in 0 until width) {
            val c = (x / 40 + y / 40) % 2 == 0
            b.put(if (c) 0x30.toByte() else 0x10.toByte())
            b.put(if (c) 0x30.toByte() else 0x10.toByte())
            b.put(if (c) 0x50.toByte() else 0x20.toByte())
            b.put(0xFF.toByte())
        }
    }

    override fun navigate(url: String) { this.url = url }
    override fun loadHtml(html: String, baseUrl: String) {}
    override fun evaluateJs(script: String) {}
    override fun resize(width: Int, height: Int) { this.width = width; this.height = height; if (initialized) { buf = ByteBuffer.allocateDirect(width * height * 4); fill(); buf?.rewind() } }
    override fun sendMouseMove(x: Int, y: Int) {}
    override fun sendMousePress(x: Int, y: Int, button: Int) {}
    override fun sendMouseRelease(x: Int, y: Int, button: Int) {}
    override fun sendMouseWheel(delta: Double) {}
    override fun sendKeyPress(keyCode: Int, scanCode: Int, modifiers: Int) {}
    override fun sendKeyRelease(keyCode: Int, scanCode: Int, modifiers: Int) {}
    override fun sendKeyTyped(c: Char, modifiers: Int) {}
    override fun getTextureData(): ByteBuffer? = buf
    override fun isTextureReady(): Boolean = ready
    override fun setFocus(focused: Boolean) {}
    override fun getNativeHandle(): Int = 0
    override fun show(screenX: Int, screenY: Int, pixelW: Int, pixelH: Int) {}
    override fun hide() {}
    override fun close() { ready = false; buf = null }
}
