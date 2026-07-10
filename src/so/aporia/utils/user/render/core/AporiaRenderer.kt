package so.aporia.utils.user.render.core

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import so.aporia.Aporia
import so.aporia.utils.user.logger.Logger
import org.joml.Matrix4f
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import java.util.OptionalDouble
import java.util.OptionalInt

object AporiaRenderer {

    // ── Sub-renderers ──
    val shapes = ShapesRenderer()
    val pixels = PixelsRenderer()

    // ── 3D projection matrices (set by GameRenderer) ──
    @JvmField var worldProjMatrix = Matrix4f()
    @JvmField var worldViewMatrix = Matrix4f()

    // ── Entity glow ──
    private var entityGlowPipeline: RenderPipeline? = null
    private var entityGlowVBO: GpuBuffer? = null
    private var entityGlowMVP: GpuBuffer? = null
    private var entityGlowParams: GpuBuffer? = null
    private var entityGlowIBO: GpuBuffer? = null

    // Pre-allocated scratch buffers
    private var scratchMVP = ByteBuffer.allocateDirect(192).order(ByteOrder.nativeOrder())
    private var scratchParams = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder())
    private var scratchVertex = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder())
    private var scratchIndexData = ByteBuffer.allocateDirect(12000).order(ByteOrder.nativeOrder())
    private val scratchProj = Matrix4f()
    private val scratchIdentity = Matrix4f()
    private val scratchCombined = Matrix4f()

    // ── Post-process ──
    var saturation = 0.5f

    // ── Depth sorting ──
    fun depth(z: Float) { shapes.depth(z) }
    fun flush() { shapes.flush() }

    // ── Init ──

    fun init() {
        val device = RenderSystem.getDevice()
        shapes.init()
        pixels.init()
        BlurRenderer.init()

        entityGlowPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/entity_glow"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/entity_glow"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/entity_glow"))
            .withUniform("ModelViewProj", UniformType.UNIFORM_BUFFER)
            .withUniform("DrawParams", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.TRIANGLES)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false).withCull(false).build()

        entityGlowVBO = device.createBuffer({ -> "aporia:entity_glow_vbo" },
            GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, 262144L)
        entityGlowMVP = device.createBuffer({ -> "aporia:entity_glow_mvp" },
            GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, 192L)
        entityGlowParams = device.createBuffer({ -> "aporia:entity_glow_params" },
            GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, 32L)
        val maxQuads = 2000
        val maxIndices = maxQuads * 6
        entityGlowIBO = device.createBuffer({ -> "aporia:entity_glow_ibo" },
            GpuBuffer.USAGE_INDEX or GpuBuffer.USAGE_COPY_DST, (maxIndices * 2L))
    }

    // ═══════════════════════════
    //  Shapes — delegated
    // ═══════════════════════════

    fun drawRectBlurred(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Int) =
        shapes.drawRectBlurred(x, y, w, h, radius, color, 4f, 15)
    fun drawRectBlurred(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Int, blurStrength: Float) =
        shapes.drawRectBlurred(x, y, w, h, radius, color, blurStrength, 15)
    fun drawRectBlurred(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Int, blurStrength: Float, cornerMask: Int) =
        shapes.drawRectBlurred(x, y, w, h, radius, color, blurStrength, cornerMask)
    @JvmOverloads fun drawRect(x: Float, y: Float, w: Float, h: Float, radius: Float = 0f, color: Int = -1, cornerMask: Int = 15) =
        shapes.drawRect(x, y, w, h, radius, color, cornerMask)
    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) =
        shapes.drawLine(x1, y1, x2, y2, thickness, color)
    fun drawFadeHLine(cx: Float, cy: Float, halfLen: Float, thickness: Float, progress: Float, color: Int) =
        shapes.drawFadeHLine(cx, cy, halfLen, thickness, progress, color)
    fun drawCircle(cx: Float, cy: Float, radius: Float, color: Int) =
        shapes.drawCircle(cx, cy, radius, color)
    fun drawTriangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, color: Int) =
        shapes.drawTriangle(x1, y1, x2, y2, x3, y3, color)
    fun drawStroke(x: Float, y: Float, w: Float, h: Float, radius: Float, thickness: Float, borderMode: Int, fadeCorner: Float, color: Int) =
        shapes.drawStroke(x, y, w, h, radius, thickness, borderMode, fadeCorner, color)
    fun drawMainMenuBackground(time: Float, width: Int, height: Int) =
        shapes.drawMainMenuBackground(time, width, height)
    fun drawLogo(x: Float, y: Float, w: Float, h: Float, tex: Identifier?, time: Long) {
        if (tex != null) shapes.drawLogo(x, y, w, h, tex, time)
    }
    fun drawLogo(x: Float, y: Float, w: Float, h: Float, view: GpuTextureView?, time: Long) =
        shapes.drawLogo(x, y, w, h, view, time)

    // ═══════════════════════════
    //  Pixels — delegated
    // ═══════════════════════════

    fun drawImage(x: Float, y: Float, w: Float, h: Float, id: Identifier?, radius: Float = 0f) =
        pixels.drawImage(x, y, w, h, id, radius)
    fun drawTexture(x: Float, y: Float, w: Float, h: Float, view: GpuTextureView?) =
        pixels.drawTexture(x, y, w, h, view)
    fun drawImageCropped(x: Float, y: Float, w: Float, h: Float, id: Identifier?, radius: Float, u0: Float, v0: Float, u1: Float, v1: Float) =
        pixels.drawImageCropped(x, y, w, h, id, radius, u0, v0, u1, v1)
    fun blitCropped(x: Float, y: Float, w: Float, h: Float, id: Identifier?, u0: Float, v0: Float, u1: Float, v1: Float) =
        pixels.blitCropped(x, y, w, h, id, u0, v0, u1, v1)
    fun applySaturation(mc: Minecraft, saturation: Float) = pixels.applySaturation(saturation)
    fun loadImage(path: Path): Identifier? = pixels.loadImage(path)
    fun loadImage(stream: InputStream): Identifier? = pixels.loadImage(stream)
    @JvmStatic fun cleanupImages() = pixels.cleanupImages()

    // ═══════════════════════════
    //  Fonts
    // ═══════════════════════════

    fun drawText(font: String, text: String, x: Float, y: Float, size: Float, color: Int) =
        Aporia.FONTS.drawText(font, text, x, y, size, color)
    fun getTextWidth(font: String, text: String, size: Float): Float =
        Aporia.FONTS.getTextWidth(font, text, size)

    // ═══════════════════════════
    //  Entity glow (3D)
    // ═══════════════════════════

    fun drawPlayerGlow(
        poseStack: com.mojang.blaze3d.vertex.PoseStack,
        model: net.minecraft.client.model.EntityModel<*>,
        packedLight: Int, packedOverlay: Int,
        colorR: Float, colorG: Float, colorB: Float, colorA: Float,
        glowIntensity: Float, rimPower: Float, fresnelPower: Float, pulseSpeed: Float
    ) {
        if (entityGlowPipeline == null) { Logger.warn("entityGlowPipeline is null"); return }
        val bb = com.mojang.blaze3d.vertex.ByteBufferBuilder(262144)
        val buf = BufferBuilder(bb, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY)
        model.renderToBuffer(poseStack, buf, packedLight, packedOverlay, -1)
        val mesh = buf.build() ?: run { bb.close(); return }
        val totalBytes = mesh.vertexBuffer().remaining()
        val rawData = ByteArray(totalBytes)
        mesh.vertexBuffer().get(rawData)
        val vertexCount = mesh.drawState().vertexCount()
        mesh.close(); bb.close()
        drawPlayerGlowSubmit(rawData, vertexCount, colorR, colorG, colorB, colorA,
            glowIntensity, rimPower, fresnelPower, pulseSpeed)
    }

    fun drawPlayerGlowSubmit(
        vertexData: ByteArray, vertexCount: Int,
        colorR: Float, colorG: Float, colorB: Float, colorA: Float,
        glowIntensity: Float, rimPower: Float, fresnelPower: Float, pulseSpeed: Float
    ) {
        val pipeline = entityGlowPipeline ?: run { Logger.warn("drawPlayerGlowSubmit: pipeline null"); return }
        val mc2 = Minecraft.getInstance()
        val colorView = mc2.mainRenderTarget.colorTextureView ?: return
        val depthView = mc2.mainRenderTarget.depthTextureView
        if (vertexCount == 0) { Logger.warn("drawPlayerGlowSubmit: vertexCount=0, skipping"); return }

        if (scratchVertex.capacity() < vertexData.size)
            scratchVertex = ByteBuffer.allocateDirect(vertexData.size).order(ByteOrder.nativeOrder())
        scratchVertex.clear(); scratchVertex.put(vertexData); scratchVertex.flip()

        val camPos = mc2.gameRenderer.mainCamera.position()
        val mv = Matrix4f(worldViewMatrix)
        mv.translate(-camPos.x.toFloat(), -camPos.y.toFloat(), -camPos.z.toFloat())
        scratchMVP.clear()
        worldProjMatrix.get(scratchMVP)
        mv.get(scratchMVP)
        scratchIdentity.get(scratchMVP)
        scratchMVP.flip()

        scratchParams.clear()
        scratchParams.putFloat(colorR).putFloat(colorG).putFloat(colorB).putFloat(colorA)
        scratchParams.putFloat(glowIntensity).putFloat(rimPower).putFloat(fresnelPower).putFloat(pulseSpeed)
        scratchParams.flip()

        val numQuads = vertexCount / 4
        val indexCount = numQuads * 6
        if (scratchIndexData.capacity() < indexCount * 2)
            scratchIndexData = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder())
        scratchIndexData.clear()
        for (i in 0 until numQuads) {
            val base = i * 4
            scratchIndexData.putShort(base.toShort()).putShort((base + 1).toShort()).putShort((base + 2).toShort())
            scratchIndexData.putShort(base.toShort()).putShort((base + 2).toShort()).putShort((base + 3).toShort())
        }
        scratchIndexData.flip()

        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        encoder.writeToBuffer(entityGlowVBO!!.slice(), scratchVertex)
        encoder.writeToBuffer(entityGlowMVP!!.slice(), scratchMVP)
        encoder.writeToBuffer(entityGlowParams!!.slice(), scratchParams)
        encoder.writeToBuffer(entityGlowIBO!!.slice(), scratchIndexData)

        val pass = encoder.createRenderPass({ -> "aporia:entity_glow" }, colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())
        pass.use {
            it.setPipeline(pipeline)
            it.setUniform("ModelViewProj", entityGlowMVP!!.slice())
            it.setUniform("DrawParams", entityGlowParams!!.slice())
            it.setVertexBuffer(0, entityGlowVBO!!)
            it.setIndexBuffer(entityGlowIBO!!, VertexFormat.IndexType.SHORT)
            it.drawIndexed(0, 0, indexCount, 0)
        }
    }

    fun drawTestQuad(
        colorR: Float, colorG: Float, colorB: Float, colorA: Float,
        glowIntensity: Float, rimPower: Float, fresnelPower: Float, pulseSpeed: Float
    ) {
        val pipeline = entityGlowPipeline ?: run { Logger.warn("drawTestQuad: pipeline null"); return }
        val mc2 = Minecraft.getInstance()
        val colorView = mc2.mainRenderTarget.colorTextureView ?: return
        val depthView = mc2.mainRenderTarget.depthTextureView

        val stride = DefaultVertexFormat.NEW_ENTITY.vertexSize
        val vCount = 4; val iCount = 6

        scratchVertex.clear()
        if (scratchVertex.capacity() < vCount * stride)
            scratchVertex = ByteBuffer.allocateDirect(vCount * stride).order(ByteOrder.nativeOrder())
        for (i in 0 until vCount) {
            val x = if (i == 0 || i == 3) -1.0f else 1.0f
            val y = if (i == 0 || i == 1) -1.0f else 1.0f
            scratchVertex.putFloat(x).putFloat(y).putFloat(-5.0f)
            scratchVertex.putInt(-1)
            scratchVertex.putFloat(i * 0.333f).putFloat(i * 0.333f)
            scratchVertex.putShort(15728880.toShort()).putShort(15728880.toShort())
            scratchVertex.putShort(0).putShort(0)
            scratchVertex.put(0).put(0).put(127).put(0)
        }
        scratchVertex.flip()

        val fov = Math.toRadians(70.0).toFloat()
        val aspect = Minecraft.getInstance().window.width.toFloat() / Minecraft.getInstance().window.height.toFloat()
        scratchCombined.setPerspective(fov, aspect, 0.1f, 1000.0f)
        scratchMVP.clear()
        scratchCombined.get(scratchMVP)
        scratchIdentity.get(scratchMVP)
        scratchIdentity.get(scratchMVP)
        scratchMVP.flip()

        scratchParams.clear()
        scratchParams.putFloat(colorR).putFloat(colorG).putFloat(colorB).putFloat(colorA)
        scratchParams.putFloat(glowIntensity).putFloat(rimPower).putFloat(fresnelPower).putFloat(pulseSpeed)
        scratchParams.flip()

        if (scratchIndexData.capacity() < iCount * 2)
            scratchIndexData = ByteBuffer.allocateDirect(iCount * 2).order(ByteOrder.nativeOrder())
        scratchIndexData.clear()
        scratchIndexData.putShort(0).putShort(1).putShort(2)
        scratchIndexData.putShort(0).putShort(2).putShort(3)
        scratchIndexData.flip()

        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        encoder.writeToBuffer(entityGlowVBO!!.slice(), scratchVertex)
        encoder.writeToBuffer(entityGlowMVP!!.slice(), scratchMVP)
        encoder.writeToBuffer(entityGlowParams!!.slice(), scratchParams)
        encoder.writeToBuffer(entityGlowIBO!!.slice(), scratchIndexData)

        val pass = encoder.createRenderPass({ -> "aporia:test_quad" }, colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())
        pass.use {
            it.setPipeline(pipeline)
            it.setUniform("ModelViewProj", entityGlowMVP!!.slice())
            it.setUniform("DrawParams", entityGlowParams!!.slice())
            it.setVertexBuffer(0, entityGlowVBO!!)
            it.setIndexBuffer(entityGlowIBO!!, VertexFormat.IndexType.SHORT)
            it.drawIndexed(0, 0, iCount, 0)
        }
    }
}
