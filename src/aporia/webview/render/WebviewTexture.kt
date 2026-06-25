package aporia.webview.render

import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.opengl.GlTexture
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import java.nio.ByteBuffer
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.*

class WebviewTexture : AutoCloseable {

    private var texture: GpuTexture? = null
    private var textureView: GpuTextureView? = null
    private var texWidth = 0
    private var texHeight = 0

    fun getGlTextureId(): Int = (texture as? GlTexture)?.glId() ?: 0
    fun getTextureView(): GpuTextureView? = textureView
    fun getTexture(): GpuTexture? = texture
    fun isReady(): Boolean = textureView != null && !textureView!!.isClosed()
    fun getWidth(): Int = texWidth
    fun getHeight(): Int = texHeight

    fun updateTexture(buffer: ByteBuffer, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        RenderSystem.assertOnRenderThread()
        checkResize(width, height)

        val glTex = texture as? GlTexture ?: return
        GlStateManager._bindTexture(glTex.glId())
        buffer.rewind()
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_BGRA, GL_UNSIGNED_BYTE, buffer)
    }

    fun updateTextureRgba(buffer: ByteBuffer, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        RenderSystem.assertOnRenderThread()
        checkResize(width, height)

        val glTex = texture as? GlTexture ?: return
        GlStateManager._bindTexture(glTex.glId())
        buffer.rewind()
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer)
    }

    private fun checkResize(width: Int, height: Int) {
        if (texture != null && texWidth == width && texHeight == height) return

        if (textureView != null) {
            textureView!!.close()
            textureView = null
        }
        if (texture != null) {
            texture!!.close()
            texture = null
        }

        texWidth = width
        texHeight = height

        texture = RenderSystem.getDevice().createTexture(
            "WebView",
            GpuTexture.USAGE_TEXTURE_BINDING or GpuTexture.USAGE_COPY_DST,
            TextureFormat.RGBA8,
            width, height, 1, 1
        )
        textureView = RenderSystem.getDevice().createTextureView(texture!!)
    }

    override fun close() {
        if (textureView != null) {
            textureView!!.close()
            textureView = null
        }
        if (texture != null) {
            texture!!.close()
            texture = null
        }
        texWidth = 0
        texHeight = 0
    }
}
