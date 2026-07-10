package so.aporia.utils.user.render.core

import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.platform.SourceFactor
import com.mojang.blaze3d.platform.DestFactor
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
import net.minecraft.resources.Identifier
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList
import java.util.Comparator
import java.util.OptionalInt

class ShapesRenderer {

    companion object {
        const val MODE_FILL = 0
        const val MODE_CIRCLE = 1
        const val MODE_ROUNDED_RECT = 2
    }

    var currentDepth = 0f

    private val taskQueue = ArrayList<DrawTask>()
    private class DrawTask(val z: Float, val action: Runnable)

    fun depth(z: Float) { currentDepth = z }
    fun flush() {
        taskQueue.sortWith(Comparator.comparingDouble { t -> t.z.toDouble() })
        for (task in taskQueue) task.action.run()
        taskQueue.clear()
    }

    lateinit var pipeline: RenderPipeline
    lateinit var roundedRectPipeline: RenderPipeline
    lateinit var mainmenuPipeline: RenderPipeline
    lateinit var logoPipeline: RenderPipeline
    lateinit var orthoProjection: CachedOrthoProjectionMatrixBuffer

    lateinit var cachedVertexBuffer: GpuBuffer
    lateinit var cachedShapeBuffer: GpuBuffer
    lateinit var cachedImageVertexBuffer: GpuBuffer
    lateinit var cachedImageShapeBuffer: GpuBuffer
    lateinit var mainmenuUbo: GpuBuffer
    lateinit var logoUbo: GpuBuffer

    private var cachedShapeBB: ByteBuffer? = null
    private var cachedMainmenuBB: ByteBuffer? = null
    internal var cachedImageBB: ByteBuffer? = null
    private var cachedLogoTimeBB: ByteBuffer? = null

    fun init() {
        val device = RenderSystem.getDevice()
        pipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/aporia"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/aporia"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/aporia"))
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("ShapeData", UniformType.UNIFORM_BUFFER)
            .withSampler("BlurTextureSampler")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
            .withBlend(BlendFunction(
                SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false).withCull(false).build()

        roundedRectPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/rounded_rect"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/rounded_rect"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/rounded_rect"))
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("ShapeData", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false).withCull(false).build()

        orthoProjection = CachedOrthoProjectionMatrixBuffer("aporia", -1000f, 1000f, true)

        mainmenuPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/mainmenu"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/mainmenu"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/mainmenu"))
            .withUniform("Time", UniformType.UNIFORM_BUFFER)
            .withUniform("Resolution", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false).withCull(false).build()

        logoPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/logo"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/logo"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/logo"))
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("LogoData", UniformType.UNIFORM_BUFFER)
            .withUniform("u_time", UniformType.UNIFORM_BUFFER)
            .withSampler("LogoTextureSampler")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false).withCull(false).build()

        cachedVertexBuffer = device.createBuffer({ -> "aporia:cached_vbo" },
            GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, 512L)
        cachedShapeBuffer = device.createBuffer({ -> "aporia:cached_shape" },
            GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, 128L)
        cachedImageVertexBuffer = device.createBuffer({ -> "aporia:cached_image_vbo" },
            GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, 256L)
        cachedImageShapeBuffer = device.createBuffer({ -> "aporia:cached_image_shape" },
            GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, 128L)
        mainmenuUbo = device.createBuffer({ -> "aporia:mainmenu_ubo" },
            GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, 32L)
        logoUbo = device.createBuffer({ -> "aporia:logo_ubo" },
            GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, 16L)
    }

    // ── Main menu ──

    fun drawMainMenuBackground(time: Float, width: Int, height: Int) {
        if (!::mainmenuPipeline.isInitialized) return
        val mainTarget = Minecraft.getInstance().mainRenderTarget
        val colorView = mainTarget.colorTextureView ?: return

        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()

        if (cachedMainmenuBB == null) cachedMainmenuBB = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder())
        cachedMainmenuBB!!.clear()
        cachedMainmenuBB!!.putFloat(time)
        cachedMainmenuBB!!.putFloat(width.toFloat())
        cachedMainmenuBB!!.putFloat(height.toFloat())
        cachedMainmenuBB!!.flip()
        encoder.writeToBuffer(mainmenuUbo.slice(), cachedMainmenuBB!!)

        val tess = Tesselator.getInstance()
        val buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX)
        buf.addVertex(0f, height.toFloat(), 0f).setUv(0f, 1f)
        buf.addVertex(0f, 0f, 0f).setUv(0f, 0f)
        buf.addVertex(width.toFloat(), 0f, 0f).setUv(1f, 0f)
        buf.addVertex(0f, height.toFloat(), 0f).setUv(0f, 1f)
        buf.addVertex(width.toFloat(), 0f, 0f).setUv(1f, 0f)
        buf.addVertex(width.toFloat(), height.toFloat(), 0f).setUv(1f, 1f)
        val mesh = buf.buildOrThrow()
        encoder.writeToBuffer(cachedVertexBuffer.slice(), mesh.vertexBuffer())
        mesh.close()

        val projSlice = orthoProjection.getBuffer(width.toFloat(), height.toFloat())
        RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC)

        val pass = encoder.createRenderPass({ -> "aporia:mainmenu_bg" }, colorView, OptionalInt.empty())
        pass.use {
            pass.setPipeline(mainmenuPipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("Time", mainmenuUbo.slice())
            pass.setVertexBuffer(0, cachedVertexBuffer)
            val seq = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES)
            pass.setIndexBuffer(seq.getBuffer(6), seq.type())
            pass.drawIndexed(0, 0, 6, 0)
        }
    }

    // ── shapes ──

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) {
        val dx = x2 - x1; val dy = y2 - y1
        val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (len == 0f) return
        val nx = -dy / len * thickness * 0.5f
        val ny = dx / len * thickness * 0.5f
        val vx = floatArrayOf(x1 + nx, x1 - nx, x2 - nx, x2 + nx)
        val vy = floatArrayOf(y1 + ny, y1 - ny, y2 - ny, y2 + ny)
        val bx = Math.min(x1, x2) - thickness
        val by = Math.min(y1, y2) - thickness
        val bw = Math.abs(dx) + thickness * 2
        val bh = Math.abs(dy) + thickness * 2
        draw(arrayOf(
            floatArrayOf(vx[0], vy[0]), floatArrayOf(vx[1], vy[1]), floatArrayOf(vx[2], vy[2]),
            floatArrayOf(vx[0], vy[0]), floatArrayOf(vx[2], vy[2]), floatArrayOf(vx[3], vy[3])
        ), color, MODE_FILL, bx, by, bw, bh, 0f)
    }

    fun drawFadeHLine(centerX: Float, centerY: Float, halfLen: Float, thickness: Float, progress: Float, color: Int) {
        val len = halfLen * progress
        if (len < 1f) return
        val startX = centerX - len; val endX = centerX + len
        val segments = 20; val segW = len * 2f / segments
        val a = (color shr 24) and 0xFF
        for (i in 0 until segments) {
            val x0 = startX + i * segW; val x1 = startX + (i + 1) * segW
            val t0 = Math.abs(x0 - centerX) / len; val t1 = Math.abs(x1 - centerX) / len
            val fade0 = 1f - t0 * t0; val fade1 = 1f - t1 * t1
            val a0 = (a * fade0).toInt(); val a1 = (a * fade1).toInt()
            val c0 = (a0 shl 24) or (color and 0x00FFFFFF)
            drawLine(x0, centerY, x1, centerY, thickness, c0)
        }
    }

    fun drawCircle(cx: Float, cy: Float, radius: Float, color: Int) {
        val d = radius * 2
        drawShape(cx - radius, cy - radius, d, d, color, MODE_CIRCLE,
            cx - radius, cy - radius, d, d, radius, 0, 0f, 0f)
    }

    fun drawTriangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Int) {
        val bx = Math.min(x1, Math.min(x2, x3))
        val by = Math.min(y1, Math.min(y2, y3))
        val bw = Math.max(x1, Math.max(x2, x3)) - bx
        val bh = Math.max(y1, Math.max(y2, y3)) - by
        draw(arrayOf(
            floatArrayOf(x1, y1), floatArrayOf(x2, y2), floatArrayOf(x3, y3)
        ), color, MODE_FILL, bx, by, bw, bh, 0f)
    }

    // ── Rounded rects / blur ──

    @JvmOverloads
    fun drawRectBlurred(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Int,
                        blurStrength: Float = 4f, cornerMask: Int = 15) {
        val mc = Minecraft.getInstance()
        val gui = BlurRenderer.useGuiBlur
        if ((if (gui) (!BlurRenderer.guiBlurReady || !::pipeline.isInitialized || BlurRenderer.guiBlurTarget == null)
            else (!BlurRenderer.blurReady || !::pipeline.isInitialized || BlurRenderer.blurTarget == null))) {
            drawRect(x, y, w, h, radius, color)
            return
        }
        val mainTarget = mc.mainRenderTarget
        val window = mc.window
        val sw = window.guiScaledWidth.toFloat()
        val sh = window.guiScaledHeight.toFloat()
        val colorView = mainTarget.colorTextureView ?: return

        val overlayA = ((color shr 24) and 0xFF) / 255f
        val overlayR = ((color shr 16) and 0xFF) / 255f
        val overlayG = ((color shr 8) and 0xFF) / 255f
        val overlayB = (color and 0xFF) / 255f

        val projSlice = orthoProjection.getBuffer(sw, sh)
        RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC)

        val tess = Tesselator.getInstance()
        val buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR)
        buf.addVertex(x, y, 0f).setUv(0f, 1f).setColor(overlayR, overlayG, overlayB, overlayA)
        buf.addVertex(x + w, y, 0f).setUv(1f, 1f).setColor(overlayR, overlayG, overlayB, overlayA)
        buf.addVertex(x, y + h, 0f).setUv(0f, 0f).setColor(overlayR, overlayG, overlayB, overlayA)
        buf.addVertex(x + w, y, 0f).setUv(1f, 1f).setColor(overlayR, overlayG, overlayB, overlayA)
        buf.addVertex(x + w, y + h, 0f).setUv(1f, 0f).setColor(overlayR, overlayG, overlayB, overlayA)
        buf.addVertex(x, y + h, 0f).setUv(0f, 0f).setColor(overlayR, overlayG, overlayB, overlayA)
        val mesh = buf.buildOrThrow()

        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        encoder.writeToBuffer(cachedVertexBuffer.slice(), mesh.vertexBuffer())
        mesh.close()

        if (cachedShapeBB == null) cachedShapeBB = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder())
        cachedShapeBB!!.clear()
        cachedShapeBB!!.putFloat(x); cachedShapeBB!!.putFloat(y); cachedShapeBB!!.putFloat(w); cachedShapeBB!!.putFloat(h)
        cachedShapeBB!!.putFloat(radius); cachedShapeBB!!.putFloat(1.0f); cachedShapeBB!!.putFloat(MODE_ROUNDED_RECT.toFloat()); cachedShapeBB!!.putFloat(0f)
        cachedShapeBB!!.putFloat(0f); cachedShapeBB!!.putFloat(0f); cachedShapeBB!!.putFloat(1f); cachedShapeBB!!.putFloat(cornerMask.toFloat())
        cachedShapeBB!!.putFloat(mainTarget.width.toFloat()); cachedShapeBB!!.putFloat(mainTarget.height.toFloat()); cachedShapeBB!!.putFloat(0f); cachedShapeBB!!.putFloat(0f)
        cachedShapeBB!!.flip()
        encoder.writeToBuffer(cachedShapeBuffer.slice(), cachedShapeBB!!)

        val indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES)
        val pass = encoder.createRenderPass({ -> "aporia:blur_rect" }, colorView, OptionalInt.empty())
        pass.use {
            pass.setPipeline(pipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("ShapeData", cachedShapeBuffer.slice())
            pass.bindTexture("BlurTextureSampler",
                if (BlurRenderer.useGuiBlur) BlurRenderer.guiBlurTarget!!.colorTextureView!! else BlurRenderer.blurTarget!!.colorTextureView!!,
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            pass.setVertexBuffer(0, cachedVertexBuffer)
            pass.setIndexBuffer(indexBuf.getBuffer(6), indexBuf.type())
            pass.drawIndexed(0, 0, 6, 0)
        }
    }

    @JvmOverloads
    fun drawRect(x: Float, y: Float, w: Float, h: Float, radius: Float = 0f, color: Int = -1, cornerMask: Int = 15) {
        val mode = if (radius > 0) MODE_ROUNDED_RECT else MODE_FILL
        drawShape(x, y, w, h, color, mode, x, y, w, h, radius, 0, 0f, 0f, cornerMask)
    }

    fun drawStroke(x: Float, y: Float, w: Float, h: Float, radius: Float,
                   thickness: Float, borderMode: Int, fadeCorner: Float, color: Int) {
        drawShape(x, y, w, h, color, MODE_ROUNDED_RECT, x, y, w, h, radius,
            borderMode, thickness, fadeCorner)
    }

    // ── Logo ──

    fun drawLogo(x: Float, y: Float, w: Float, h: Float, textureId: Identifier, time: Long) {
        val tex = Minecraft.getInstance().textureManager.getTexture(textureId) ?: return
        drawLogo(x, y, w, h, tex.textureView, time)
    }

    fun drawLogo(x: Float, y: Float, w: Float, h: Float, view: GpuTextureView?, time: Long) {
        if (view == null) return
        val mc = Minecraft.getInstance()
        val colorView = mc.mainRenderTarget.colorTextureView ?: return

        if (cachedImageBB == null) cachedImageBB = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder())
        cachedImageBB!!.clear()
        repeat(8) { cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f) }
        cachedImageBB!!.flip()

        val sw = mc.window.guiScaledWidth.toFloat()
        val sh = mc.window.guiScaledHeight.toFloat()
        val nx0 = -1f + 2f * x / sw; val ny0 = 1f - 2f * (y + h) / sh
        val nx1 = -1f + 2f * (x + w) / sw; val ny1 = 1f - 2f * y / sh

        val tess = Tesselator.getInstance()
        val buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR)
        buf.addVertex(nx0, ny0, 0f).setUv(0f, 1f).setColor(1f, 1f, 1f, 1f)
        buf.addVertex(nx1, ny0, 0f).setUv(1f, 1f).setColor(1f, 1f, 1f, 1f)
        buf.addVertex(nx1, ny1, 0f).setUv(1f, 0f).setColor(1f, 1f, 1f, 1f)
        buf.addVertex(nx0, ny0, 0f).setUv(0f, 1f).setColor(1f, 1f, 1f, 1f)
        buf.addVertex(nx1, ny1, 0f).setUv(1f, 0f).setColor(1f, 1f, 1f, 1f)
        buf.addVertex(nx0, ny1, 0f).setUv(0f, 0f).setColor(1f, 1f, 1f, 1f)
        val mesh = buf.buildOrThrow()

        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        encoder.writeToBuffer(cachedImageVertexBuffer.slice(), mesh.vertexBuffer())
        encoder.writeToBuffer(cachedImageShapeBuffer.slice(), cachedImageBB!!)
        mesh.close()

        val indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES)
        val logoTimeBB = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder())
        logoTimeBB.clear(); logoTimeBB.putFloat((time % 1000000L) / 1000f); logoTimeBB.flip()
        encoder.writeToBuffer(logoUbo.slice(), logoTimeBB)

        val pass = encoder.createRenderPass({ -> "aporia:logo_pass" }, colorView, OptionalInt.empty())
        pass.use {
            pass.setPipeline(logoPipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("u_time", logoUbo.slice())
            pass.setUniform("LogoData", cachedImageShapeBuffer.slice())
            pass.bindTexture("LogoTextureSampler", view,
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            pass.setVertexBuffer(0, cachedImageVertexBuffer)
            pass.setIndexBuffer(indexBuf.getBuffer(6), indexBuf.type())
            pass.drawIndexed(0, 0, 6, 0)
        }
    }

    // ── Internal ──

    private fun drawShape(x: Float, y: Float, w: Float, h: Float, color: Int,
                          mode: Int, bx: Float, by: Float, bw: Float, bh: Float,
                          radius: Float, borderMode: Int, thickness: Float, fadeCorner: Float) {
        drawShape(x, y, w, h, color, mode, bx, by, bw, bh, radius, borderMode, thickness, fadeCorner, 15)
    }

    private fun drawShape(x: Float, y: Float, w: Float, h: Float, color: Int,
                          mode: Int, bx: Float, by: Float, bw: Float, bh: Float,
                          radius: Float, borderMode: Int, thickness: Float, fadeCorner: Float, cornerMask: Int) {
        draw(arrayOf(
            floatArrayOf(x, y + h), floatArrayOf(x + w, y + h), floatArrayOf(x + w, y),
            floatArrayOf(x, y + h), floatArrayOf(x + w, y), floatArrayOf(x, y)
        ), color, mode, bx, by, bw, bh, radius, borderMode, thickness, fadeCorner, cornerMask)
    }

    private fun draw(verts: Array<FloatArray>, color: Int, mode: Int,
                     bx: Float, by: Float, bw: Float, bh: Float, radius: Float) {
        draw(verts, color, mode, bx, by, bw, bh, radius, 0, 0f, 0f, 15)
    }

    private fun draw(verts: Array<FloatArray>, color: Int, mode: Int,
                     bx: Float, by: Float, bw: Float, bh: Float, radius: Float,
                     borderMode: Int, thickness: Float, fadeCorner: Float, cornerMask: Int) {
        val mc = Minecraft.getInstance()
        val window = mc.window
        val colorView = mc.mainRenderTarget.colorTextureView ?: return
        if (!::pipeline.isInitialized) return

        val a = ((color shr 24) and 0xFF) / 255f
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f

        val projSlice = orthoProjection.getBuffer(window.guiScaledWidth.toFloat(), window.guiScaledHeight.toFloat())
        RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC)

        val tess = Tesselator.getInstance()
        val buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR)
        for (v in verts) buf.addVertex(v[0], v[1], currentDepth).setUv(0f, 0f).setColor(r, g, b, a)
        val mesh = buf.buildOrThrow()
        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        encoder.writeToBuffer(cachedVertexBuffer.slice(), mesh.vertexBuffer())
        mesh.close()

        if (cachedShapeBB == null) cachedShapeBB = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder())
        cachedShapeBB!!.clear()
        cachedShapeBB!!.putFloat(bx); cachedShapeBB!!.putFloat(by); cachedShapeBB!!.putFloat(bw); cachedShapeBB!!.putFloat(bh)
        cachedShapeBB!!.putFloat(radius); cachedShapeBB!!.putFloat(1.0f); cachedShapeBB!!.putFloat(mode.toFloat()); cachedShapeBB!!.putFloat(borderMode.toFloat())
        cachedShapeBB!!.putFloat(thickness); cachedShapeBB!!.putFloat(fadeCorner); cachedShapeBB!!.putFloat(0f); cachedShapeBB!!.putFloat(cornerMask.toFloat())
        cachedShapeBB!!.putFloat(0f); cachedShapeBB!!.putFloat(0f); cachedShapeBB!!.putFloat(0f); cachedShapeBB!!.putFloat(0f)
        cachedShapeBB!!.flip()
        encoder.writeToBuffer(cachedShapeBuffer.slice(), cachedShapeBB!!)

        val indexBuf = RenderSystem.getSequentialBuffer(VertexFormat.Mode.TRIANGLES)
        val vertexCount = verts.size
        val pass = encoder.createRenderPass({ -> "aporia:draw" }, colorView, OptionalInt.empty())
        pass.use {
            pass.setPipeline(roundedRectPipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("ShapeData", cachedShapeBuffer.slice())
            pass.setVertexBuffer(0, cachedVertexBuffer)
            pass.setIndexBuffer(indexBuf.getBuffer(vertexCount), indexBuf.type())
            pass.drawIndexed(0, 0, vertexCount, 0)
        }
    }
}
