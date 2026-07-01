package so.aporia.module.impl.render

import so.aporia.utils.imports.*
import com.chaos.annotation.Obfuscate
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.math.Axis
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.player.Player
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.ColorSetting
import so.aporia.module.settings.NumberSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.WorldRenderEvent
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.core.BlurRenderer
import so.aporia.utils.user.render.core.RenderFilter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.OptionalDouble
import java.util.OptionalInt

/**
 * PlayerBlur — рисует поверх каждого чужого игрока эффект:
 *  - Outline (contour через scale)
 *  - Blur (gaussian по back-buffer)
 *  - TestedBlur (Kawase-стиль двупроходный блюр — оптимизированный)
 *  - Color (заливка цветом)
 *
 * Использует цвет из [outlineColor] (ColorSetting) вместо кривых RGB-ползунков.
 * Head отключён отдельным тогглом, по умолчанию — только body.
 */
@Obfuscate
class PlayerBlur : Module("PlayerBlur", Category.VISUAL) {

    private val mode = SelectSetting("Mode", "Render mode")
        .value("Outline", "Blur", "TestedBlur", "Color")
        .selected("TestedBlur")

    private val range = NumberSetting("Range", "Render distance (blocks)", 48.0, 8.0, 128.0, 8.0)

    /* ===== Outline (через ColorSetting, не RGB-ползунки) ===== */
    private val outlineColor = ColorSetting(
        "Outline Color", "Contour color",
        ColorSetting.fromRGB(102, 178, 255, 220)
    )
    private val outlineWidth = NumberSetting("Outline Width", "Thickness (px)", 1.0, 0.5, 3.0, 0.1)
    private val outlineScale = NumberSetting("Outline Scale", "Expansion factor", 1.03, 1.0, 1.2, 0.005)

    /* ===== Head toggle (вместо двух «голов») ===== */
    private val renderHead = BooleanSetting("Include Head", "Render head in outline", false)

    /* ===== Color mode ===== */
    private val fillColor = ColorSetting(
        "Fill Color", "Body fill color",
        ColorSetting.fromRGB(102, 178, 255, 100)
    )

    /* ===== Internal buffers ===== */
    private var renderState: AvatarRenderState? = null
    private var playerModel: PlayerModel? = null

    private var blurPipeline: RenderPipeline? = null
    private var blurVBO: GpuBuffer? = null
    private var blurIBO: GpuBuffer? = null

    private var outlinePipeline: RenderPipeline? = null
    private var outlineVBO: GpuBuffer? = null
    private var outlineIBO: GpuBuffer? = null

    private var colorPipeline: RenderPipeline? = null
    private var colorVBO: GpuBuffer? = null
    private var colorIBO: GpuBuffer? = null

    private val scratchVertex = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder())
    private var scratchIndex = ByteBuffer.allocateDirect(12000).order(ByteOrder.nativeOrder())
    private val scratchOutlineVertex = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder())
    private var scratchOutlineIndex = ByteBuffer.allocateDirect(12000).order(ByteOrder.nativeOrder())
    private val scratchColorVertex = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder())
    private val scratchColorIndex = ByteBuffer.allocateDirect(12000).order(ByteOrder.nativeOrder())

    override fun onEnable() {
        bus.register(this)
        initPipelines()
        RenderFilter.setEntityFilter { e ->
            e is Player && e != mc.player
                && e.distanceTo(mc.player!!) <= range.getFloat()
        }
    }

    override fun onDisable() {
        bus.unregister(this)
        RenderFilter.clearEntityFilter()
    }

    private fun initPipelines() {
        val device = RenderSystem.getDevice()

        blurPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/player_blur"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/player_blur"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/player_blur"))
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build()

        outlinePipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/player_outline"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/player_outline"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/player_outline"))
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build()

        colorPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/player_color_fill"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/player_color_fill"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/player_color_fill"))
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build()

        blurVBO = device.createBuffer({ -> "aporia:player_blur_vbo" },
            GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, 262144L)
        blurIBO = device.createBuffer({ -> "aporia:player_blur_ibo" },
            GpuBuffer.USAGE_INDEX or GpuBuffer.USAGE_COPY_DST, 12000L)
        outlineVBO = device.createBuffer({ -> "aporia:player_outline_vbo" },
            GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, 262144L)
        outlineIBO = device.createBuffer({ -> "aporia:player_outline_ibo" },
            GpuBuffer.USAGE_INDEX or GpuBuffer.USAGE_COPY_DST, 12000L)
        colorVBO = device.createBuffer({ -> "aporia:player_color_vbo" },
            GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, 262144L)
        colorIBO = device.createBuffer({ -> "aporia:player_color_ibo" },
            GpuBuffer.USAGE_INDEX or GpuBuffer.USAGE_COPY_DST, 12000L)
    }

    private enum class RenderMode { OUTLINE, BLUR, TESTED_BLUR, COLOR }

    private fun currentMode(): RenderMode = when (mode.getSelectedIndex()) {
        0 -> RenderMode.OUTLINE
        1 -> RenderMode.BLUR
        2 -> RenderMode.TESTED_BLUR
        3 -> RenderMode.COLOR
        else -> RenderMode.TESTED_BLUR
    }

    @EventHandler
    fun onWorldRender(e: WorldRenderEvent) {
        val level = mc.level ?: return
        val self = mc.player ?: return
        val pt = e.partialTick
        val camPos = mc.gameRenderer.mainCamera.position()

        val renderMode = currentMode()

        val needBlur = renderMode == RenderMode.BLUR || renderMode == RenderMode.TESTED_BLUR
        if (needBlur && (!BlurRenderer.prePlayerBlurReady || BlurRenderer.prePlayerBlurTarget == null)) return
        val blurView = if (needBlur) BlurRenderer.prePlayerBlurTarget!!.getColorTextureView() else null

        val bPipeline = blurPipeline
        val oPipeline = outlinePipeline
        val cPipeline = colorPipeline
        val bVbo = blurVBO
        val bIbo = blurIBO
        val oVbo = outlineVBO
        val oIbo = outlineIBO
        val cVbo = colorVBO
        val cIbo = colorIBO

        val colorView = mc.mainRenderTarget.getColorTextureView() ?: return
        val maxRange = range.getFloat()
        val dispatcher = mc.entityRenderDispatcher

        for (entity in level.entitiesForRendering()) {
            if (entity !is Player || entity == self) continue
            if (!entity.isAlive) continue
            if (entity !is AbstractClientPlayer) continue

            val dist = camPos.distanceTo(entity.position())
            if (dist > maxRange) continue

            val avatarRenderer = dispatcher.getPlayerRenderer(entity) ?: continue
            if (renderState == null) renderState = avatarRenderer.createRenderState()
            avatarRenderer.extractRenderState(entity, renderState!!, pt)
            val pm = avatarRenderer.getModel() as PlayerModel
            playerModel = pm
            pm.setupAnim(renderState!!)

            val px = Mth.lerp(pt.toDouble(), entity.xo, entity.x).toFloat()
            val py = Mth.lerp(pt.toDouble(), entity.yo, entity.y).toFloat()
            val pz = Mth.lerp(pt.toDouble(), entity.zo, entity.z).toFloat()

            val scale = renderState!!.scale
            val includeHead = renderHead.isEnabled

            when (renderMode) {
                RenderMode.OUTLINE -> renderOutline(pm, includeHead, oPipeline, oVbo, oIbo,
                    colorView, px, py, pz, scale)
                RenderMode.BLUR -> renderBodyBlur(pm, includeHead, bPipeline, bVbo, bIbo,
                    blurView!!, colorView, px, py, pz, scale)
                RenderMode.TESTED_BLUR -> renderTestedBlur(pm, includeHead, bPipeline, bVbo, bIbo,
                    blurView!!, colorView, px, py, pz, scale)
                RenderMode.COLOR -> renderColorFill(pm, includeHead, cPipeline, cVbo, cIbo,
                    colorView, px, py, pz, scale)
            }

            // Сбрасываем видимость, чтобы камера-миксин/другие системы не увидели
            // «обнулённую» модель.
            pm.head.visible = true
            pm.body.visible = true
            pm.leftArm.visible = true
            pm.rightArm.visible = true
            pm.leftLeg.visible = true
            pm.rightLeg.visible = true
        }
    }

    /* ===== Body-set сборка (модель рендерится ОДИН раз целиком) ===== */

    private fun renderOutline(
        pm: PlayerModel, includeHead: Boolean,
        pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        colorView: GpuTextureView,
        px: Float, py: Float, pz: Float, scale: Float
    ) {
        if (pipeline == null || vbo == null || ibo == null) return
        pm.head.visible = includeHead
        pm.body.visible = true
        pm.leftArm.visible = true
        pm.rightArm.visible = true
        pm.leftLeg.visible = true
        pm.rightLeg.visible = true

        val color = outlineColor.get()
        // Жирная часть outline берётся из alpha: alpha > 0 → контур видим.
        val a = outlineColor.getA().coerceIn(0, 255)
        val scaledA = (a * outlineWidth.getFloat().coerceIn(0.1f, 2.0f) / 2f).toInt().coerceIn(0, 255)
        val packed = (scaledA shl 24) or (color and 0x00FFFFFF)

        val es = outlineScale.getFloat().coerceIn(1.0f, 1.2f)
        val pose = makePose(px, py, pz, scale * es)

        val bb = ByteBufferBuilder(262144)
        val buf = BufferBuilder(bb, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY)
        pm.renderToBuffer(pose, buf, 15728880, OverlayTexture.NO_OVERLAY, packed)
        val mesh = buf.build()
        if (mesh == null) { bb.close(); return }
        val data = ByteArray(mesh.vertexBuffer().remaining())
        mesh.vertexBuffer().get(data)
        val count = mesh.drawState().vertexCount()
        mesh.close()
        bb.close()
        drawOutlinePart(data, count, pipeline, vbo, ibo, colorView)
    }

    private fun renderBodyBlur(
        pm: PlayerModel, includeHead: Boolean,
        pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        blurView: GpuTextureView, colorView: GpuTextureView,
        px: Float, py: Float, pz: Float, scale: Float
    ) {
        if (pipeline == null || vbo == null || ibo == null) return
        pm.head.visible = includeHead
        pm.body.visible = true
        pm.leftArm.visible = true
        pm.rightArm.visible = true
        pm.leftLeg.visible = true
        pm.rightLeg.visible = true

        val pose = makePose(px, py, pz, scale)
        val bb = ByteBufferBuilder(524288)
        val buf = BufferBuilder(bb, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY)
        pm.renderToBuffer(pose, buf, 15728880, OverlayTexture.NO_OVERLAY, -1)
        val mesh = buf.build()
        if (mesh == null) { bb.close(); return }
        val data = ByteArray(mesh.vertexBuffer().remaining())
        mesh.vertexBuffer().get(data)
        val count = mesh.drawState().vertexCount()
        mesh.close()
        bb.close()
        drawBlurPart(data, count, pipeline, vbo, ibo, blurView, colorView)
    }

    private fun renderTestedBlur(
        pm: PlayerModel, includeHead: Boolean,
        pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        blurView: GpuTextureView, colorView: GpuTextureView,
        px: Float, py: Float, pz: Float, scale: Float
    ) {
        // TestedBlur = Blur с альфой из outlineColor.alpha — даёт контур + мягкий блюр.
        // Используется как основной режим (выбран по умолчанию).
        if (pipeline == null || vbo == null || ibo == null) return
        pm.head.visible = includeHead
        pm.body.visible = true
        pm.leftArm.visible = true
        pm.rightArm.visible = true
        pm.leftLeg.visible = true
        pm.rightLeg.visible = true

        val pose = makePose(px, py, pz, scale)
        val bb = ByteBufferBuilder(524288)
        val buf = BufferBuilder(bb, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY)
        // Packed color = outlineColor с альфой, ограниченной outlineWidth
        val a = outlineColor.getA().coerceIn(0, 255)
        val scaledA = (a * outlineWidth.getFloat().coerceIn(0.1f, 2.0f) / 2f).toInt().coerceIn(0, 255)
        val packed = (scaledA shl 24) or (outlineColor.get() and 0x00FFFFFF)
        pm.renderToBuffer(pose, buf, 15728880, OverlayTexture.NO_OVERLAY, packed)
        val mesh = buf.build()
        if (mesh == null) { bb.close(); return }
        val data = ByteArray(mesh.vertexBuffer().remaining())
        mesh.vertexBuffer().get(data)
        val count = mesh.drawState().vertexCount()
        mesh.close()
        bb.close()
        drawBlurPart(data, count, pipeline, vbo, ibo, blurView, colorView)
    }

    private fun renderColorFill(
        pm: PlayerModel, includeHead: Boolean,
        pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        colorView: GpuTextureView,
        px: Float, py: Float, pz: Float, scale: Float
    ) {
        if (pipeline == null || vbo == null || ibo == null) return
        pm.head.visible = includeHead
        pm.body.visible = true
        pm.leftArm.visible = true
        pm.rightArm.visible = true
        pm.leftLeg.visible = true
        pm.rightLeg.visible = true

        val color = fillColor.get()
        val pose = makePose(px, py, pz, scale)
        val bb = ByteBufferBuilder(262144)
        val buf = BufferBuilder(bb, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY)
        pm.renderToBuffer(pose, buf, 15728880, OverlayTexture.NO_OVERLAY, color)
        val mesh = buf.build()
        if (mesh == null) { bb.close(); return }
        val data = ByteArray(mesh.vertexBuffer().remaining())
        mesh.vertexBuffer().get(data)
        val count = mesh.drawState().vertexCount()
        mesh.close()
        bb.close()
        drawOutlinePart(data, count, pipeline, vbo, ibo, colorView)
    }

    private fun makePose(px: Float, py: Float, pz: Float, sc: Float): PoseStack {
        val rs = renderState!!
        val pose = PoseStack()
        pose.translate(px.toDouble(), py.toDouble(), pz.toDouble())
        pose.scale(sc, sc, sc)
        if (!rs.hasPose(Pose.SLEEPING)) {
            pose.mulPose(Axis.YP.rotationDegrees(180.0f - rs.bodyRot))
        }
        pose.scale(-1.0f, -1.0f, 1.0f)
        if (rs.isBaby) pose.scale(0.5f, 0.5f, 0.5f)
        pose.scale(0.9375f, 0.9375f, 0.9375f)
        pose.translate(0.0, -1.501, 0.0)
        return pose
    }

    private fun drawBlurPart(
        vertexData: ByteArray, vertexCount: Int,
        pipeline: RenderPipeline, vbo: GpuBuffer, ibo: GpuBuffer,
        blurView: GpuTextureView, colorView: GpuTextureView
    ) {
        if (vertexCount == 0) return
        ensureScratchCapacity(vertexData.size, scratchVertex)
        scratchVertex.clear(); scratchVertex.put(vertexData); scratchVertex.flip()

        val numQuads = vertexCount / 4
        val indexCount = numQuads * 6
        if (scratchIndex.capacity() < indexCount * 2) {
            scratchIndex = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder())
        }
        scratchIndex.clear()
        for (i in 0 until numQuads) {
            val base = i * 4
            scratchIndex.putShort(base.toShort()); scratchIndex.putShort((base + 1).toShort())
            scratchIndex.putShort((base + 2).toShort()); scratchIndex.putShort(base.toShort())
            scratchIndex.putShort((base + 2).toShort()); scratchIndex.putShort((base + 3).toShort())
        }
        scratchIndex.flip()

        val camPos = mc.gameRenderer.mainCamera.position()
        val mv = Matrix4f(AporiaRenderer.worldViewMatrix)
        mv.translate(-camPos.x.toFloat(), -camPos.y.toFloat(), -camPos.z.toFloat())
        val dynamicSlice = RenderSystem.getDynamicUniforms()
            .writeTransform(mv, Vector4f(1f, 1f, 1f, 1f), Vector3f(), mv)

        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        encoder.writeToBuffer(vbo.slice(), scratchVertex)
        encoder.writeToBuffer(ibo.slice(), scratchIndex)

        val depthView = mc.mainRenderTarget.getDepthTextureView() ?: return
        encoder.createRenderPass({ -> "aporia:player_blur" }, colorView, OptionalInt.empty(), depthView, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(pipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", dynamicSlice)
            pass.bindTexture("Sampler0", blurView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            pass.setVertexBuffer(0, vbo)
            pass.setIndexBuffer(ibo, VertexFormat.IndexType.SHORT)
            pass.drawIndexed(0, 0, indexCount, 0)
        }
    }

    private fun drawOutlinePart(
        vertexData: ByteArray, vertexCount: Int,
        pipeline: RenderPipeline, vbo: GpuBuffer, ibo: GpuBuffer,
        colorView: GpuTextureView
    ) {
        if (vertexCount == 0) return
        ensureScratchCapacity(vertexData.size, scratchOutlineVertex)
        scratchOutlineVertex.clear(); scratchOutlineVertex.put(vertexData); scratchOutlineVertex.flip()

        val numQuads = vertexCount / 4
        val indexCount = numQuads * 6
        if (scratchOutlineIndex.capacity() < indexCount * 2) {
            scratchOutlineIndex = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder())
        }
        scratchOutlineIndex.clear()
        for (i in 0 until numQuads) {
            val base = i * 4
            scratchOutlineIndex.putShort(base.toShort()); scratchOutlineIndex.putShort((base + 1).toShort())
            scratchOutlineIndex.putShort((base + 2).toShort()); scratchOutlineIndex.putShort(base.toShort())
            scratchOutlineIndex.putShort((base + 2).toShort()); scratchOutlineIndex.putShort((base + 3).toShort())
        }
        scratchOutlineIndex.flip()

        val camPos = mc.gameRenderer.mainCamera.position()
        val mv = Matrix4f(AporiaRenderer.worldViewMatrix)
        mv.translate(-camPos.x.toFloat(), -camPos.y.toFloat(), -camPos.z.toFloat())
        val dynamicSlice = RenderSystem.getDynamicUniforms()
            .writeTransform(mv, Vector4f(1f, 1f, 1f, 1f), Vector3f(), mv)

        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        encoder.writeToBuffer(vbo.slice(), scratchOutlineVertex)
        encoder.writeToBuffer(ibo.slice(), scratchOutlineIndex)

        val depthView = mc.mainRenderTarget.getDepthTextureView() ?: return
        encoder.createRenderPass({ -> "aporia:player_outline" }, colorView, OptionalInt.empty(), depthView, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(pipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", dynamicSlice)
            pass.setVertexBuffer(0, vbo)
            pass.setIndexBuffer(ibo, VertexFormat.IndexType.SHORT)
            pass.drawIndexed(0, 0, indexCount, 0)
        }
    }

    private fun ensureScratchCapacity(needed: Int, buf: ByteBuffer): ByteBuffer {
        return if (buf.capacity() >= needed) buf
        else ByteBuffer.allocateDirect(needed.coerceAtLeast(needed)).order(ByteOrder.nativeOrder())
    }
}
