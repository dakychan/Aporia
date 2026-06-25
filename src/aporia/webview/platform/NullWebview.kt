package aporia.webview.platform

import aporia.webview.Webview
import java.nio.ByteBuffer

class NullWebview(url: String, width: Int, height: Int) : Webview(url, width, height) {

    private var ready = false
    private var testBuffer: ByteBuffer? = null

    override fun init(): Boolean {
        initialized = true
        ready = true
        testBuffer = ByteBuffer.allocateDirect(width * height * 4)
        fillTestPattern()
        testBuffer?.rewind()
        return true
    }

    private fun fillTestPattern() {
        val buf = testBuffer ?: return
        for (y in 0 until height) {
            for (x in 0 until width) {
                val check = (x / 40 + y / 40) % 2 == 0
                if (check) {
                    buf.put(0x00.toByte())
                    buf.put(0x80.toByte())
                    buf.put(0xFF.toByte())
                } else {
                    buf.put(0xFF.toByte())
                    buf.put(0x00.toByte())
                    buf.put(0x00.toByte())
                }
                buf.put(0xFF.toByte())
            }
        }
    }

    override fun navigate(url: String) { this.url = url }
    override fun loadHtml(html: String, baseUrl: String) {}
    override fun evaluateJs(script: String) {}

    override fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
        if (initialized) {
            testBuffer = ByteBuffer.allocateDirect(width * height * 4)
            fillTestPattern()
            testBuffer?.rewind()
        }
    }

    override fun sendMouseMove(x: Int, y: Int) {}
    override fun sendMousePress(x: Int, y: Int, button: Int) {}
    override fun sendMouseRelease(x: Int, y: Int, button: Int) {}
    override fun sendMouseWheel(delta: Double) {}
    override fun sendKeyPress(keyCode: Int, scanCode: Int, modifiers: Int) {}
    override fun sendKeyRelease(keyCode: Int, scanCode: Int, modifiers: Int) {}
    override fun sendKeyTyped(c: Char, modifiers: Int) {}

    override fun getTextureData(): ByteBuffer? = testBuffer
    override fun isTextureReady(): Boolean = ready
    override fun setFocus(focused: Boolean) {}
    override fun getNativeHandle(): Int = 0

    override fun show(screenX: Int, screenY: Int, pixelW: Int, pixelH: Int) {}
    override fun hide() {}

    override fun close() {
        ready = false
        testBuffer = null
    }
}
