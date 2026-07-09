package aporia.webview
import java.nio.ByteBuffer
import com.chaos.annotation.ChaosNative
@ChaosNative
abstract class Webview(
    var url: String,
    var width: Int,
    var height: Int
) : AutoCloseable {

    var initialized: Boolean = false
        protected set

    abstract fun init(): Boolean
    abstract fun navigate(url: String)
    abstract fun loadHtml(html: String, baseUrl: String)
    abstract fun resize(width: Int, height: Int)
    abstract fun evaluateJs(script: String)

    abstract fun sendMouseMove(x: Int, y: Int)
    abstract fun sendMousePress(x: Int, y: Int, button: Int)
    abstract fun sendMouseRelease(x: Int, y: Int, button: Int)
    abstract fun sendMouseWheel(delta: Double)
    abstract fun sendKeyPress(keyCode: Int, scanCode: Int, modifiers: Int)
    abstract fun sendKeyRelease(keyCode: Int, scanCode: Int, modifiers: Int)
    abstract fun sendKeyTyped(c: Char, modifiers: Int)

    abstract fun getTextureData(): ByteBuffer?
    abstract fun isTextureReady(): Boolean
    abstract fun setFocus(focused: Boolean)
    abstract fun getNativeHandle(): Int

    abstract fun show(screenX: Int, screenY: Int, pixelW: Int, pixelH: Int)
    abstract fun hide()

    /** Returns the OpenGL texture ID (0 if not available). */
    open fun getGlTextureId(): Int = 0
}