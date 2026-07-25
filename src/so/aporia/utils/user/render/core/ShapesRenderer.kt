package so.aporia.utils.user.render.core

import com.mojang.blaze3d.IndexType
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BindGroupLayout
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.platform.BlendFactor

import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import net.minecraft.resources.Identifier
import org.joml.Matrix4f
import org.joml.Vector4fc
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList
import java.util.Comparator
import java.util.Optional

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
        val batch = ArrayList(taskQueue)
        taskQueue.clear()
        batch.sortWith(Comparator.comparingDouble { t -> t.z.toDouble() })
        for (task in batch) task.action.run()
    }

    fun close() {
        if (::cachedVertexBuffer.isInitialized) cachedVertexBuffer.close()
        if (::cachedShapeBuffer.isInitialized) cachedShapeBuffer.close()
        if (::cachedImageVertexBuffer.isInitialized) cachedImageVertexBuffer.close()
        if (::cachedImageShapeBuffer.isInitialized) cachedImageShapeBuffer.close()
        if (::mainmenuUbo.isInitialized) mainmenuUbo.close()
        if (::logoUbo.isInitialized) logoUbo.close()
        taskQueue.clear()
    }

    lateinit var pipeline: RenderPipeline
    lateinit var roundedRectPipeline: RenderPipeline
    lateinit var mainmenuPipeline: RenderPipeline
    lateinit var logoPipeline: RenderPipeline
    lateinit var orthoProjection: ProjectionMatrixBuffer

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
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("ShapeData", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("BlurTextureSampler").build())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState(BlendFunction(
                BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA,
                BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA)))
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).build()

        roundedRectPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/rounded_rect"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/rounded_rect"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/rounded_rect"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("ShapeData", UniformType.UNIFORM_BUFFER).build())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).build()

        orthoProjection = ProjectionMatrixBuffer("aporia")

        mainmenuPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/mainmenu"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/mainmenu"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/mainmenu"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Time", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Resolution", UniformType.UNIFORM_BUFFER).build())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).build()

        logoPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/logo"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/logo"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/logo"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("LogoData", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("u_time", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("LogoTextureSampler").build())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).build()

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

        taskQueue.add(DrawTask(0f) { ->
            val colorView = Minecraft.getInstance().gameRenderer.mainRenderTarget().colorTextureView ?: return@DrawTask
            val device = RenderSystem.getDevice()
            val encoder = device.createCommandEncoder()

            if (cachedMainmenuBB == null) cachedMainmenuBB = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder())
            cachedMainmenuBB!!.clear()
            cachedMainmenuBB!!.putFloat(time)
            cachedMainmenuBB!!.putFloat(width.toFloat())
            cachedMainmenuBB!!.putFloat(height.toFloat())
            cachedMainmenuBB!!.flip()
            encoder.writeToBuffer(mainmenuUbo.slice(), cachedMainmenuBB!!)

            val bb = ByteBufferBuilder.exactlySized(2048)
            val buf = BufferBuilder(bb, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX)
            buf.addVertex(0f, height.toFloat(), 0f).setUv(0f, 1f)
            buf.addVertex(0f, 0f, 0f).setUv(0f, 0f)
            buf.addVertex(width.toFloat(), 0f, 0f).setUv(1f, 0f)
            buf.addVertex(0f, height.toFloat(), 0f).setUv(0f, 1f)
            buf.addVertex(width.toFloat(), 0f, 0f).setUv(1f, 0f)
            buf.addVertex(width.toFloat(), height.toFloat(), 0f).setUv(1f, 1f)
            val mesh = buf.buildOrThrow()
            encoder.writeToBuffer(cachedVertexBuffer.slice(), mesh.vertexBuffer())
            mesh.close()

            val projMatrix = Matrix4f().setOrtho(0f, width.toFloat(), height.toFloat(), 0f, -1000f, 1000f)
            val projSlice = orthoProjection.getBuffer(projMatrix)
            RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC)

            val pass = encoder.createRenderPass({ -> "aporia:mainmenu_bg" }, colorView, Optional.empty<Vector4fc>())
            pass.use {
                pass.setPipeline(mainmenuPipeline)
                RenderSystem.bindDefaultUniforms(pass)
                pass.setUniform("Time", mainmenuUbo.slice())
                pass.setVertexBuffer(0, cachedVertexBuffer.slice(0L, cachedVertexBuffer.size()))
                val seq = RenderSystem.getSequentialBuffer(PrimitiveTopology.TRIANGLES)
                pass.setIndexBuffer(seq.getBuffer(6), seq.type())
                pass.drawIndexed(6, 1, 0, 0, 0)
            }
        })
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
        val depth = currentDepth
        val sw = mc.window.guiScaledWidth.toFloat()
        val sh = mc.window.guiScaledHeight.toFloat()

        taskQueue.add(DrawTask(depth) { ->
            if (!::pipeline.isInitialized) return@DrawTask

            val blurReady = if (gui) BlurRenderer.guiBlurReady else BlurRenderer.blurReady
            val blurTarget = if (gui) BlurRenderer.guiBlurTarget else BlurRenderer.blurTarget
            if (!blurReady || blurTarget == null) {
                executeShapeDraw(x, y, w, h, radius, color, MODE_ROUNDED_RECT, cornerMask, depth, sw, sh)
                return@DrawTask
            }

            val colorView = mc.gameRenderer.mainRenderTarget().colorTextureView ?: return@DrawTask

            val overlayA = ((color shr 24) and 0xFF) / 255f
            val overlayR = ((color shr 16) and 0xFF) / 255f
            val overlayG = ((color shr 8) and 0xFF) / 255f
            val overlayB = (color and 0xFF) / 255f

            val projMatrix = Matrix4f().setOrtho(0f, sw, sh, 0f, -1000f, 1000f)
            val projSlice = orthoProjection.getBuffer(projMatrix)
            RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC)

            val bb = ByteBufferBuilder.exactlySized(2048)
            val buf = BufferBuilder(bb, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR)
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

            val mainTarget = mc.gameRenderer.mainRenderTarget()
            if (cachedShapeBB == null) cachedShapeBB = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder())
            cachedShapeBB!!.clear()
            cachedShapeBB!!.putFloat(x); cachedShapeBB!!.putFloat(y); cachedShapeBB!!.putFloat(w); cachedShapeBB!!.putFloat(h)
            cachedShapeBB!!.putFloat(radius); cachedShapeBB!!.putFloat(1.0f); cachedShapeBB!!.putFloat(MODE_ROUNDED_RECT.toFloat()); cachedShapeBB!!.putFloat(0f)
            cachedShapeBB!!.putFloat(0f); cachedShapeBB!!.putFloat(0f); cachedShapeBB!!.putFloat(1f); cachedShapeBB!!.putFloat(cornerMask.toFloat())
            cachedShapeBB!!.putFloat(mainTarget.width.toFloat()); cachedShapeBB!!.putFloat(mainTarget.height.toFloat()); cachedShapeBB!!.putFloat(0f); cachedShapeBB!!.putFloat(0f)
            cachedShapeBB!!.flip()
            encoder.writeToBuffer(cachedShapeBuffer.slice(), cachedShapeBB!!)

            val indexBuf = RenderSystem.getSequentialBuffer(PrimitiveTopology.TRIANGLES)
            val pass = encoder.createRenderPass({ -> "aporia:blur_rect" }, colorView, Optional.empty<Vector4fc>())
            pass.use {
                pass.setPipeline(pipeline)
                RenderSystem.bindDefaultUniforms(pass)
                pass.setUniform("ShapeData", cachedShapeBuffer.slice())
                val btv = blurTarget.colorTextureView ?: return@DrawTask
                pass.bindTexture("BlurTextureSampler", btv,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
                pass.setVertexBuffer(0, cachedVertexBuffer.slice(0L, cachedVertexBuffer.size()))
                pass.setIndexBuffer(indexBuf.getBuffer(6), indexBuf.type())
                pass.drawIndexed(6, 1, 0, 0, 0)
            }
        })
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

        val sw = mc.window.guiScaledWidth.toFloat()
        val sh = mc.window.guiScaledHeight.toFloat()
        val nx0 = -1f + 2f * x / sw; val ny0 = 1f - 2f * (y + h) / sh
        val nx1 = -1f + 2f * (x + w) / sw; val ny1 = 1f - 2f * y / sh
        val timeFloat = (time % 1000000L) / 1000f

        taskQueue.add(DrawTask(currentDepth + 0.1f) { ->
            val colorView = mc.gameRenderer.mainRenderTarget().colorTextureView ?: return@DrawTask
            if (cachedImageBB == null) cachedImageBB = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder())
            cachedImageBB!!.clear()
            repeat(8) { cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f) }
            cachedImageBB!!.flip()

            val bb = ByteBufferBuilder.exactlySized(2048)
            val buf = BufferBuilder(bb, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR)
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

            val indexBuf = RenderSystem.getSequentialBuffer(PrimitiveTopology.TRIANGLES)
            val logoTimeBB = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder())
            logoTimeBB.clear(); logoTimeBB.putFloat(timeFloat); logoTimeBB.flip()
            encoder.writeToBuffer(logoUbo.slice(), logoTimeBB)

            val projMatrix = Matrix4f().setOrtho(0f, sw, sh, 0f, -1000f, 1000f)
            val projSlice = orthoProjection.getBuffer(projMatrix)
            RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC)

            val pass = encoder.createRenderPass({ -> "aporia:logo_pass" }, colorView, Optional.empty<Vector4fc>())
            pass.use {
                pass.setPipeline(logoPipeline)
                RenderSystem.bindDefaultUniforms(pass)
                pass.setUniform("u_time", logoUbo.slice())
                pass.setUniform("LogoData", cachedImageShapeBuffer.slice())
                pass.bindTexture("LogoTextureSampler", view,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
                pass.setVertexBuffer(0, cachedImageVertexBuffer.slice(0L, cachedImageVertexBuffer.size()))
                pass.setIndexBuffer(indexBuf.getBuffer(6), indexBuf.type())
                pass.drawIndexed(6, 1, 0, 0, 0)
            }
        })
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
        if (!::pipeline.isInitialized) return

        val a = ((color shr 24) and 0xFF) / 255f
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f

        val sw = window.guiScaledWidth.toFloat()
        val sh = window.guiScaledHeight.toFloat()
        val depth = currentDepth
        val capturedVerts = Array(verts.size) { verts[it].copyOf() }

        taskQueue.add(DrawTask(depth) { ->
            val colorView = mc.gameRenderer.mainRenderTarget().colorTextureView ?: return@DrawTask
            executeShapeDrawDirect(capturedVerts, a, r, g, b, sw, sh, depth,
                colorView, bx, by, bw, bh, radius, mode, borderMode, thickness, fadeCorner, cornerMask)
        })
    }

    private fun executeShapeDraw(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Int,
                                 mode: Int, cornerMask: Int, depth: Float, sw: Float, sh: Float) {
        val mc = Minecraft.getInstance()
        val colorView = mc.gameRenderer.mainRenderTarget().colorTextureView ?: return
        if (!::pipeline.isInitialized) return
        val a = ((color shr 24) and 0xFF) / 255f
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val verts = arrayOf(
            floatArrayOf(x, y + h), floatArrayOf(x + w, y + h), floatArrayOf(x + w, y),
            floatArrayOf(x, y + h), floatArrayOf(x + w, y), floatArrayOf(x, y)
        )
        executeShapeDrawDirect(verts, a, r, g, b, sw, sh, depth,
            colorView, x, y, w, h, radius, mode, 0, 0f, 0f, cornerMask)
    }

    private fun executeShapeDrawDirect(verts: Array<FloatArray>, a: Float, r: Float, g: Float, b: Float,
                                       sw: Float, sh: Float, depth: Float, colorView: GpuTextureView,
                                       bx: Float, by: Float, bw: Float, bh: Float, radius: Float,
                                       mode: Int, borderMode: Int, thickness: Float, fadeCorner: Float,
                                       cornerMask: Int) {
        val projMatrix = Matrix4f().setOrtho(0f, sw, sh, 0f, -1000f, 1000f)
        val projSlice = orthoProjection.getBuffer(projMatrix)
        RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC)

        val bb = ByteBufferBuilder.exactlySized(2048)
        val buf = BufferBuilder(bb, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR)
        for (v in verts) buf.addVertex(v[0], v[1], depth).setUv(0f, 0f).setColor(r, g, b, a)
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

        val indexBuf = RenderSystem.getSequentialBuffer(PrimitiveTopology.TRIANGLES)
        val vertexCount = verts.size
        val pass = encoder.createRenderPass({ -> "aporia:draw" }, colorView, Optional.empty<Vector4fc>())
        pass.use {
            pass.setPipeline(roundedRectPipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("ShapeData", cachedShapeBuffer.slice())
            pass.setVertexBuffer(0, cachedVertexBuffer.slice(0L, cachedVertexBuffer.size()))
            pass.setIndexBuffer(indexBuf.getBuffer(vertexCount), indexBuf.type())
            pass.drawIndexed(vertexCount, 1, 0, 0, 0)
        }
    }
}
