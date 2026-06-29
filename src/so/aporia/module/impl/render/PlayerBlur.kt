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

@Obfuscate
class PlayerBlur : Module("PlayerBlur", Category.VISUAL) {

    private val mode = SelectSetting("Mode", "Render mode")
        .value("Outline", "Blur", "BlurOutline", "Color", "TestedBlur")
        .selected("BlurOutline")

    private val range = NumberSetting("Range", "Render distance (blocks)", 48.0, 8.0, 128.0, 8.0)

    private val outlineR = NumberSetting("Outline R", "Outline color red (0-255)", 102.0, 0.0, 255.0, 1.0)
    private val outlineG = NumberSetting("Outline G", "Outline color green (0-255)", 178.0, 0.0, 255.0, 1.0)
    private val outlineB = NumberSetting("Outline B", "Outline color blue (0-255)", 255.0, 0.0, 255.0, 1.0)
    private val outlineWidth = NumberSetting("Outline Width", "Outline thickness", 0.5, 0.0, 2.0, 0.05)
    private val outlineScale = NumberSetting("Outline Scale", "How much outline expands", 1.03, 1.0, 1.2, 0.01)

    // Color mode
    private val fillR = NumberSetting("Fill R", "Fill color red (0-255)", 102.0, 0.0, 255.0, 1.0)
        .apply { category = "Color Fill"; hierarchy = 0 }
    private val fillG = NumberSetting("Fill G", "Fill color green (0-255)", 178.0, 0.0, 255.0, 1.0)
        .apply { category = "Color Fill"; hierarchy = 1 }
    private val fillB = NumberSetting("Fill B", "Fill color blue (0-255)", 255.0, 0.0, 255.0, 1.0)
        .apply { category = "Color Fill"; hierarchy = 2 }
    private val fillAlpha = NumberSetting("Fill Alpha", "Fill opacity (0-255)", 100.0, 0.0, 255.0, 1.0)
        .apply { category = "Color Fill"; hierarchy = 3 }

    private val headEnable = BooleanSetting("Head Outline", "Enable outline for head", true)
    private val headR = NumberSetting("Head R", "Head outline red", 102.0, 0.0, 255.0, 1.0)
    private val headG = NumberSetting("Head G", "Head outline green", 178.0, 0.0, 255.0, 1.0)
    private val headB = NumberSetting("Head B", "Head outline blue", 255.0, 0.0, 255.0, 1.0)

    private val bodyEnable = BooleanSetting("Body Outline", "Enable outline for body", true)
    private val bodyR = NumberSetting("Body R", "Body outline red", 102.0, 0.0, 255.0, 1.0)
    private val bodyG = NumberSetting("Body G", "Body outline green", 178.0, 0.0, 255.0, 1.0)
    private val bodyB = NumberSetting("Body B", "Body outline blue", 255.0, 0.0, 255.0, 1.0)

    private val leftArmEnable = BooleanSetting("Left Arm Outline", "Enable outline for left arm", true)
    private val leftArmR = NumberSetting("Left Arm R", "Left arm outline red", 102.0, 0.0, 255.0, 1.0)
    private val leftArmG = NumberSetting("Left Arm G", "Left arm outline green", 178.0, 0.0, 255.0, 1.0)
    private val leftArmB = NumberSetting("Left Arm B", "Left arm outline blue", 255.0, 0.0, 255.0, 1.0)

    private val rightArmEnable = BooleanSetting("Right Arm Outline", "Enable outline for right arm", true)
    private val rightArmR = NumberSetting("Right Arm R", "Right arm outline red", 102.0, 0.0, 255.0, 1.0)
    private val rightArmG = NumberSetting("Right Arm G", "Right arm outline green", 178.0, 0.0, 255.0, 1.0)
    private val rightArmB = NumberSetting("Right Arm B", "Right arm outline blue", 255.0, 0.0, 255.0, 1.0)

    private val leftLegEnable = BooleanSetting("Left Leg Outline", "Enable outline for left leg", true)
    private val leftLegR = NumberSetting("Left Leg R", "Left leg outline red", 102.0, 0.0, 255.0, 1.0)
    private val leftLegG = NumberSetting("Left Leg G", "Left leg outline green", 178.0, 0.0, 255.0, 1.0)
    private val leftLegB = NumberSetting("Left Leg B", "Left leg outline blue", 255.0, 0.0, 255.0, 1.0)

    private val rightLegEnable = BooleanSetting("Right Leg Outline", "Enable outline for right leg", true)
    private val rightLegR = NumberSetting("Right Leg R", "Right leg outline red", 102.0, 0.0, 255.0, 1.0)
    private val rightLegG = NumberSetting("Right Leg G", "Right leg outline green", 178.0, 0.0, 255.0, 1.0)
    private val rightLegB = NumberSetting("Right Leg B", "Right leg outline blue", 255.0, 0.0, 255.0, 1.0)

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

    private var scratchVertex = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder())
    private var scratchIndex = ByteBuffer.allocateDirect(12000).order(ByteOrder.nativeOrder())
    private var scratchOutlineVertex = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder())
    private var scratchOutlineIndex = ByteBuffer.allocateDirect(12000).order(ByteOrder.nativeOrder())
    private var scratchColorVertex = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder())
    private var scratchColorIndex = ByteBuffer.allocateDirect(12000).order(ByteOrder.nativeOrder())

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

    private data class PartDef(
        val part: net.minecraft.client.model.geom.ModelPart,
        val enableSetting: BooleanSetting,
        val rSetting: NumberSetting,
        val gSetting: NumberSetting,
        val bSetting: NumberSetting
    )

    private enum class RenderMode { OUTLINE, BLUR, BLUR_OUTLINE, COLOR, TESTED_BLUR }

    private fun currentMode(): RenderMode = when (mode.getSelectedIndex()) {
        0 -> RenderMode.OUTLINE
        1 -> RenderMode.BLUR
        2 -> RenderMode.BLUR_OUTLINE
        3 -> RenderMode.COLOR
        4 -> RenderMode.TESTED_BLUR
        else -> RenderMode.BLUR_OUTLINE
    }

    @EventHandler
    fun onWorldRender(e: WorldRenderEvent) {
        val level = mc.level ?: return
        val self = mc.player ?: return
        val pt = e.partialTick
        val camPos = mc.gameRenderer.mainCamera.position()

        val renderMode = currentMode()

        // For Color mode: don't need blur target
        val needBlur = renderMode == RenderMode.BLUR || renderMode == RenderMode.BLUR_OUTLINE || renderMode == RenderMode.TESTED_BLUR
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
            playerModel = avatarRenderer.getModel() as PlayerModel
            playerModel!!.setupAnim(renderState!!)

            val px = Mth.lerp(pt.toDouble(), entity.xo, entity.x).toFloat()
            val py = Mth.lerp(pt.toDouble(), entity.yo, entity.y).toFloat()
            val pz = Mth.lerp(pt.toDouble(), entity.zo, entity.z).toFloat()

            val model = playerModel!!
            val allParts = listOf(model.head, model.body, model.leftArm, model.rightArm, model.leftLeg, model.rightLeg)
            val scale = renderState!!.scale
            val es = outlineScale.getFloat()

            when (renderMode) {
                RenderMode.OUTLINE -> {
                    val parts = listOf(
                        PartDef(model.head, headEnable, headR, headG, headB),
                        PartDef(model.body, bodyEnable, bodyR, bodyG, bodyB),
                        PartDef(model.leftArm, leftArmEnable, leftArmR, leftArmG, leftArmB),
                        PartDef(model.rightArm, rightArmEnable, rightArmR, rightArmG, rightArmB),
                        PartDef(model.leftLeg, leftLegEnable, leftLegR, leftLegG, leftLegB),
                        PartDef(model.rightLeg, rightLegEnable, rightLegR, rightLegG, rightLegB)
                    )
                    renderOutlineParts(model, allParts, parts, oPipeline, oVbo, oIbo, colorView, px, py, pz, scale, es)
                }
                RenderMode.BLUR -> {
                    val parts = listOf(
                        PartDef(model.head, headEnable, headR, headG, headB),
                        PartDef(model.body, bodyEnable, bodyR, bodyG, bodyB),
                        PartDef(model.leftArm, leftArmEnable, leftArmR, leftArmG, leftArmB),
                        PartDef(model.rightArm, rightArmEnable, rightArmR, rightArmG, rightArmB),
                        PartDef(model.leftLeg, leftLegEnable, leftLegR, leftLegG, leftLegB),
                        PartDef(model.rightLeg, rightLegEnable, rightLegR, rightLegG, rightLegB)
                    )
                    renderBlurParts(model, allParts, parts, bPipeline, bVbo, bIbo, blurView!!, colorView, px, py, pz, scale)
                }
                RenderMode.BLUR_OUTLINE -> {
                    val parts = listOf(
                        PartDef(model.head, headEnable, headR, headG, headB),
                        PartDef(model.body, bodyEnable, bodyR, bodyG, bodyB),
                        PartDef(model.leftArm, leftArmEnable, leftArmR, leftArmG, leftArmB),
                        PartDef(model.rightArm, rightArmEnable, rightArmR, rightArmG, rightArmB),
                        PartDef(model.leftLeg, leftLegEnable, leftLegR, leftLegG, leftLegB),
                        PartDef(model.rightLeg, rightLegEnable, rightLegR, rightLegG, rightLegB)
                    )
                    renderBlurParts(model, allParts, parts, bPipeline, bVbo, bIbo, blurView!!, colorView, px, py, pz, scale)
                    renderOutlineParts(model, allParts, parts, oPipeline, oVbo, oIbo, colorView, px, py, pz, scale, es)
                }
                RenderMode.COLOR -> {
                    renderColorFill(model, allParts, cPipeline, cVbo, cIbo, colorView, px, py, pz, scale)
                }
                RenderMode.TESTED_BLUR -> {
                    renderTestedBlur(model, allParts, bPipeline, bVbo, bIbo, blurView!!, colorView, px, py, pz, scale)
                }
            }

            allParts.forEach { it.visible = true }
        }
    }

    private fun renderBlurParts(
        model: PlayerModel, allParts: List<net.minecraft.client.model.geom.ModelPart>,
        parts: List<PartDef>, pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        blurView: GpuTextureView, colorView: GpuTextureView,
        px: Float, py: Float, pz: Float, scale: Float
    ) {
        if (pipeline == null || vbo == null || ibo == null) return
        for (def in parts) {
            allParts.forEach { it.visible = false }
            def.part.visible = true
            val bb = ByteBufferBuilder(524288)
            val buf = BufferBuilder(bb, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY)
            model.renderToBuffer(makePose(px, py, pz, scale), buf, 15728880, OverlayTexture.NO_OVERLAY, -1)
            val mesh = buf.build()
            if (mesh == null) { bb.close(); continue }
            val totalBytes = mesh.vertexBuffer().remaining()
            val vertexCount = mesh.drawState().vertexCount()
            val vertexData = ByteArray(totalBytes)
            mesh.vertexBuffer().get(vertexData)
            mesh.close()
            bb.close()
            drawBlurPart(vertexData, vertexCount, pipeline, vbo, ibo, blurView, colorView)
        }
    }

    private fun renderOutlineParts(
        model: PlayerModel, allParts: List<net.minecraft.client.model.geom.ModelPart>,
        parts: List<PartDef>, pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        colorView: GpuTextureView, px: Float, py: Float, pz: Float, scale: Float, es: Float
    ) {
        if (pipeline == null || vbo == null || ibo == null) return
        for (def in parts) {
            if (!def.enableSetting.isEnabled) continue
            allParts.forEach { it.visible = false }
            def.part.visible = true

            val r = def.rSetting.getInt().coerceIn(0, 255)
            val g = def.gSetting.getInt().coerceIn(0, 255)
            val b = def.bSetting.getInt().coerceIn(0, 255)
            val a = (outlineWidth.getFloat() * 255f).toInt().coerceIn(0, 255)
            val packedColor = (a shl 24) or (b shl 16) or (g shl 8) or r

            val bb2 = ByteBufferBuilder(262144)
            val buf2 = BufferBuilder(bb2, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY)
            model.renderToBuffer(makePose(px, py, pz, scale * es), buf2, 15728880, OverlayTexture.NO_OVERLAY, packedColor)
            val mesh2 = buf2.build()
            if (mesh2 != null) {
                val vertexData2 = ByteArray(mesh2.vertexBuffer().remaining())
                val vertexCount2 = mesh2.drawState().vertexCount()
                mesh2.vertexBuffer().get(vertexData2)
                mesh2.close()
                bb2.close()
                drawOutlinePart(vertexData2, vertexCount2, pipeline, vbo, ibo, colorView)
            } else { bb2.close() }
        }
    }

    private fun renderColorFill(
        model: PlayerModel, allParts: List<net.minecraft.client.model.geom.ModelPart>,
        pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        colorView: GpuTextureView, px: Float, py: Float, pz: Float, scale: Float
    ) {
        if (pipeline == null || vbo == null || ibo == null) return
        allParts.forEach { it.visible = true }

        val cr = fillR.getInt().coerceIn(0, 255)
        val cg = fillG.getInt().coerceIn(0, 255)
        val cb = fillB.getInt().coerceIn(0, 255)
        val ca = fillAlpha.getInt().coerceIn(0, 255)
        val packedColor = (ca shl 24) or (cb shl 16) or (cg shl 8) or cr

        val bb = ByteBufferBuilder(262144)
        val buf = BufferBuilder(bb, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY)
        model.renderToBuffer(makePose(px, py, pz, scale), buf, 15728880, OverlayTexture.NO_OVERLAY, packedColor)
        val mesh = buf.build()
        if (mesh != null) {
            val totalBytes = mesh.vertexBuffer().remaining()
            val vertexCount = mesh.drawState().vertexCount()
            val vertexData = ByteArray(totalBytes)
            mesh.vertexBuffer().get(vertexData)
            mesh.close()
            bb.close()
            drawOutlinePart(vertexData, vertexCount, pipeline, vbo, ibo, colorView)
        } else { bb.close() }
    }

    private fun renderTestedBlur(
        model: PlayerModel, allParts: List<net.minecraft.client.model.geom.ModelPart>,
        pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        blurView: GpuTextureView, colorView: GpuTextureView,
        px: Float, py: Float, pz: Float, scale: Float
    ) {
        // TestedBlur: same as Blur mode but applies Kawase up/down on skin vertices
        // Currently uses the same blur pipeline with test parameters
        allParts.forEach { it.visible = true }
        if (pipeline == null || vbo == null || ibo == null) return

        val bb = ByteBufferBuilder(524288)
        val buf = BufferBuilder(bb, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY)
        model.renderToBuffer(makePose(px, py, pz, scale), buf, 15728880, OverlayTexture.NO_OVERLAY, -1)
        val mesh = buf.build()
        if (mesh == null) { bb.close(); return }
        val totalBytes = mesh.vertexBuffer().remaining()
        val vertexCount = mesh.drawState().vertexCount()
        val vertexData = ByteArray(totalBytes)
        mesh.vertexBuffer().get(vertexData)
        mesh.close()
        bb.close()
        drawBlurPart(vertexData, vertexCount, pipeline, vbo, ibo, blurView, colorView)
    }

    private fun makePose(px: Float, py: Float, pz: Float, sc: Float): PoseStack {
        val pose = PoseStack()
        pose.translate(px.toDouble(), py.toDouble(), pz.toDouble())
        pose.scale(sc, sc, sc)
        if (!renderState!!.hasPose(Pose.SLEEPING)) {
            pose.mulPose(Axis.YP.rotationDegrees(180.0f - renderState!!.bodyRot))
        }
        pose.scale(-1.0f, -1.0f, 1.0f)
        if (renderState!!.isBaby) pose.scale(0.5f, 0.5f, 0.5f)
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
        if (scratchVertex.capacity() < vertexData.size) {
            scratchVertex = ByteBuffer.allocateDirect(vertexData.size).order(ByteOrder.nativeOrder())
        }
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
        if (scratchOutlineVertex.capacity() < vertexData.size) {
            scratchOutlineVertex = ByteBuffer.allocateDirect(vertexData.size).order(ByteOrder.nativeOrder())
        }
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
}
