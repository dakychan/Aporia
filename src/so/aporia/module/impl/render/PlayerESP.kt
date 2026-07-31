package so.aporia.module.impl.render

import so.aporia.utils.imports.*
import java.util.UUID
import com.chaos.annotation.Obfuscate
import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.IndexType
import com.mojang.blaze3d.PrimitiveTopology
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
import org.joml.Vector4fc
import java.util.Optional
import com.mojang.blaze3d.pipeline.BindGroupLayout
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.impl.world.FakeLag
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.ColorSetting
import so.aporia.module.settings.SliderSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.WorldRenderEvent
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.core.BlurRenderer
import so.aporia.utils.user.render.core.RenderFilter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.OptionalDouble
import com.chaos.annotation.ChaosNative

@Obfuscate
@ChaosNative
class PlayerESP : Module("PlayerESP", Category.VISUAL) {

    private val mode = SelectSetting("Mode", "Render mode")
        .value("Outline", "Blur", "TestedBlur", "Color")
        .selected("TestedBlur")

    private val range = SliderSetting("Range", "Render distance (blocks)", 48.0, 8.0, 128.0, 8.0)

    private val outlineColor = ColorSetting("Outline Color", "Contour color",
        ColorSetting.fromRGB(102, 178, 255, 220))
    private val outlineWidth = SliderSetting("Outline Width", "Thickness (px)", 1.0, 0.5, 3.0, 0.1)
    private val outlineScale = SliderSetting("Outline Scale", "Expansion factor", 1.03, 1.0, 1.2, 0.005)
    private val renderHead = BooleanSetting("Include Head", "Render head in outline", false)
    private val fillColor = ColorSetting("Fill Color", "Body fill color",
        ColorSetting.fromRGB(102, 178, 255, 100))
    private val friendColor = ColorSetting("Friend Color", "Glow color for friends",
        ColorSetting.fromRGB(0, 255, 100, 220))
    private val damageColor = ColorSetting("Damage Color", "Flash overlay on damage",
        ColorSetting.fromRGB(255, 0, 0, 150))
    private val self = BooleanSetting("Self", "Render on self", false)

    override val settings = listOf(mode, range, outlineColor, outlineWidth, outlineScale, renderHead, fillColor, friendColor, damageColor, self)

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
    private val scratchOutlineVertex = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder())

    private val entityFilter = java.util.function.Predicate<net.minecraft.world.entity.Entity> { e ->
        e is Player && (self.isEnabled || e != mc.player)
            && e.distanceTo(mc.player!!) <= range.getFloat()
    }

    override fun onEnable() {
        bus.register(this)
        initPipelines()
        RenderFilter.addFilter(entityFilter)
    }

    override fun onDisable() {
        bus.unregister(this)
        RenderFilter.removeFilter(entityFilter)
    }

    private fun initPipelines() {
        val device = RenderSystem.getDevice()
        blurPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/player_blur"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/player_blur"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/player_blur"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").build())
            .withVertexBinding(0, DefaultVertexFormat.ENTITY).withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false).build()

        outlinePipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/player_outline"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/player_outline"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/player_outline"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection", UniformType.UNIFORM_BUFFER).build())
            .withVertexBinding(0, DefaultVertexFormat.ENTITY).withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false).build()

        colorPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/player_color_fill"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/player_color_fill"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/player_color_fill"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection", UniformType.UNIFORM_BUFFER).build())
            .withVertexBinding(0, DefaultVertexFormat.ENTITY).withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false).build()

        blurVBO = device.createBuffer({ -> "aporia:player_blur_vbo" }, GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, 262144L)
        blurIBO = device.createBuffer({ -> "aporia:player_blur_ibo" }, GpuBuffer.USAGE_INDEX or GpuBuffer.USAGE_COPY_DST, 12000L)
        outlineVBO = device.createBuffer({ -> "aporia:player_outline_vbo" }, GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, 262144L)
        outlineIBO = device.createBuffer({ -> "aporia:player_outline_ibo" }, GpuBuffer.USAGE_INDEX or GpuBuffer.USAGE_COPY_DST, 12000L)
        colorVBO = device.createBuffer({ -> "aporia:player_color_vbo" }, GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, 262144L)
        colorIBO = device.createBuffer({ -> "aporia:player_color_ibo" }, GpuBuffer.USAGE_INDEX or GpuBuffer.USAGE_COPY_DST, 12000L)
    }

    private enum class RenderMode { OUTLINE, BLUR, TESTED_BLUR, COLOR }

    private fun currentMode(): RenderMode = when (mode.getSelectedIndex()) {
        0 -> RenderMode.OUTLINE; 1 -> RenderMode.BLUR; 2 -> RenderMode.TESTED_BLUR; 3 -> RenderMode.COLOR; else -> RenderMode.TESTED_BLUR
    }

    @EventHandler
    fun onWorldRender(e: WorldRenderEvent) {
        val level = mc.level ?: return
        val selfPlayer = mc.player ?: return
        val pt = e.partialTick
        val camPos = mc.gameRenderer.mainCamera().position()
        val renderMode = currentMode()

        val needBlur = renderMode == RenderMode.BLUR || renderMode == RenderMode.TESTED_BLUR
        if (needBlur && (!BlurRenderer.prePlayerBlurReady || BlurRenderer.prePlayerBlurTarget == null)) return
        val blurView = if (needBlur) BlurRenderer.prePlayerBlurTarget!!.getColorTextureView() else null

        val bPipeline = blurPipeline; val oPipeline = outlinePipeline; val cPipeline = colorPipeline
        val bVbo = blurVBO; val bIbo = blurIBO; val oVbo = outlineVBO; val oIbo = outlineIBO
        val cVbo = colorVBO; val cIbo = colorIBO

        val colorView = mc.gameRenderer.mainRenderTarget().getColorTextureView() ?: return
        val maxRange = range.getFloat()
        val dispatcher = mc.entityRenderDispatcher

        for (entity in level.entitiesForRendering()) {
            if (entity !is Player || entity == selfPlayer) continue
            if (!entity.isAlive) continue
            if (entity !is AbstractClientPlayer) continue

            val dist = camPos.distanceTo(entity.position())
            if (dist > maxRange) continue

            val avatarRenderer = dispatcher.getPlayerRenderer(entity)
            if (renderState == null) renderState = avatarRenderer.createRenderState()
            avatarRenderer.extractRenderState(entity, renderState!!, pt)
            val pm = avatarRenderer.getModel()
            playerModel = pm
            pm.setupAnim(renderState!!)

            val px = Mth.lerp(pt.toDouble(), entity.xo, entity.x).toFloat()
            val py = Mth.lerp(pt.toDouble(), entity.yo, entity.y).toFloat()
            val pz = Mth.lerp(pt.toDouble(), entity.zo, entity.z).toFloat()

            val scale = renderState!!.scale
            val friendOverride = if (fm.isFriend(entity.name.string)) friendColor.get() else null

            when (renderMode) {
                RenderMode.OUTLINE -> {
                    renderOutline(pm, oPipeline, oVbo, oIbo, colorView, px, py, pz, scale, friendOverride)
                    spawnOutlineParticles(entity, 1)
                    updateAndRenderParticles(entity, camPos, friendOverride ?: outlineColor.get())
                }
                RenderMode.BLUR -> renderBodyBlur(pm, bPipeline, bVbo, bIbo, blurView!!, colorView, px, py, pz, scale)
                RenderMode.TESTED_BLUR -> renderTestedBlur(pm, bPipeline, bVbo, bIbo, blurView!!, colorView, px, py, pz, scale, friendOverride)
                RenderMode.COLOR -> renderColorFill(pm, cPipeline, cVbo, cIbo, colorView, px, py, pz, scale, friendOverride)
            }

            if (entity.hurtTime > 0) {
                renderColorFill(pm, cPipeline, cVbo, cIbo, colorView, px, py, pz, scale, damageColor.get())
            }

            pm.head.visible = true; pm.body.visible = true
            pm.leftArm.visible = true; pm.rightArm.visible = true
            pm.leftLeg.visible = true; pm.rightLeg.visible = true
        }

        // -- TPAura LiquidBounce ghost player --
        val tpAura = so.aporia.module.ModuleManager.get("TPAura") as? so.aporia.module.impl.combat.TPAura
        if (tpAura != null && tpAura.isEnabled && tpAura.mode.get() == "LiquidBounce") {
            val ghostPos = tpAura.fakePlayerPos
            if (ghostPos != null && oPipeline != null && oVbo != null && oIbo != null) {
                val disp = mc.entityRenderDispatcher
                val ar = disp.getPlayerRenderer(selfPlayer)
                val gs = ar.createRenderState()
                ar.extractRenderState(selfPlayer, gs, pt)
                val gm = ar.getModel()
                playerModel = gm
                gm.setupAnim(gs)
                val col = colorUtil.rgba(255, 120, 120, 180)
                renderOutline(gm, oPipeline, oVbo, oIbo, colorView,
                    ghostPos.x.toFloat(), ghostPos.y.toFloat(), ghostPos.z.toFloat(), gs.scale, col)
                gm.head.visible = true; gm.body.visible = true
                gm.leftArm.visible = true; gm.rightArm.visible = true
                gm.leftLeg.visible = true; gm.rightLeg.visible = true
            }
        }

        // -- FakeLag ghost player (server-side position outline) --
        val fakeLag = so.aporia.module.ModuleManager.get("FakeLag") as? FakeLag
        if (fakeLag != null && fakeLag.isEnabled && oPipeline != null && oVbo != null && oIbo != null) {
            val disp = mc.entityRenderDispatcher
            val ar = disp.getPlayerRenderer(selfPlayer)
            val gs = ar.createRenderState()
            ar.extractRenderState(selfPlayer, gs, pt)
            val gm = ar.getModel()
            playerModel = gm
            gm.setupAnim(gs)
            val col = colorUtil.rgba(255, 200, 100, 180)
            renderOutline(gm, oPipeline, oVbo, oIbo, colorView,
                FakeLag.serverPosX.toFloat(), FakeLag.serverPosY.toFloat(), FakeLag.serverPosZ.toFloat(), gs.scale, col)
            gm.head.visible = true; gm.body.visible = true
            gm.leftArm.visible = true; gm.rightArm.visible = true
            gm.leftLeg.visible = true; gm.rightLeg.visible = true
        }
    }

    private fun buildMeshData(pm: PlayerModel, pose: PoseStack, packedColor: Int, capacity: Int): MeshData? {
        pm.head.visible = renderHead.isEnabled; pm.body.visible = true
        pm.leftArm.visible = true; pm.rightArm.visible = true
        pm.leftLeg.visible = true; pm.rightLeg.visible = true
        val bb = ByteBufferBuilder.exactlySized(capacity)
        val buf = BufferBuilder(bb, PrimitiveTopology.QUADS, DefaultVertexFormat.ENTITY)
        pm.renderToBuffer(pose, buf, 15728880, OverlayTexture.NO_OVERLAY, packedColor)
        val mesh = buf.build()
        if (mesh == null) { bb.close(); return null }
        val src = mesh.vertexBuffer()
        val count = mesh.drawState().vertexCount()
        val size = src.remaining()
        val scratch = if (capacity > 262288) scratchVertex else scratchOutlineVertex
        ensureScratchCapacity(size, scratch)
        scratch.clear(); scratch.put(src); scratch.flip()
        mesh.close(); bb.close()
        return MeshData(count, capacity > 262288)
    }

    private class MeshData(val vertexCount: Int, val isBlur: Boolean)

    private fun renderOutline(pm: PlayerModel, pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        colorView: GpuTextureView, px: Float, py: Float, pz: Float, scale: Float, overrideColor: Int? = null) {
        if (pipeline == null || vbo == null || ibo == null) return
        val color = overrideColor ?: outlineColor.get()
        val a = (color shr 24 and 0xFF).coerceIn(0, 255)
        val scaledA = (a * outlineWidth.getFloat().coerceIn(0.1f, 2.0f) / 2f).toInt().coerceIn(0, 255)
        val packed = (scaledA shl 24) or (color and 0x00FFFFFF)
        val es = outlineScale.getFloat().coerceIn(1.0f, 1.2f)
        val pose = makePose(px, py, pz, scale * es)
        val data = buildMeshData(pm, pose, packed, 262144) ?: return
        drawOutlinePartDirect(data.vertexCount, pipeline, vbo, ibo, colorView)
    }

    private fun renderBodyBlur(pm: PlayerModel, pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        blurView: GpuTextureView, colorView: GpuTextureView, px: Float, py: Float, pz: Float, scale: Float) {
        if (pipeline == null || vbo == null || ibo == null) return
        val pose = makePose(px, py, pz, scale)
        val data = buildMeshData(pm, pose, -1, 524288) ?: return
        drawBlurPartDirect(data.vertexCount, pipeline, vbo, ibo, blurView, colorView)
    }

    private fun renderTestedBlur(pm: PlayerModel, pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        blurView: GpuTextureView, colorView: GpuTextureView, px: Float, py: Float, pz: Float, scale: Float, overrideColor: Int? = null) {
        if (pipeline == null || vbo == null || ibo == null) return
        val pose = makePose(px, py, pz, scale)
        val baseColor = overrideColor ?: outlineColor.get()
        val a = (baseColor shr 24 and 0xFF).coerceIn(0, 255)
        val scaledA = (a * outlineWidth.getFloat().coerceIn(0.1f, 2.0f) / 2f).toInt().coerceIn(0, 255)
        val packed = (scaledA shl 24) or (baseColor and 0x00FFFFFF)
        val data = buildMeshData(pm, pose, packed, 524288) ?: return
        drawBlurPartDirect(data.vertexCount, pipeline, vbo, ibo, blurView, colorView)
    }

    private fun renderColorFill(pm: PlayerModel, pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        colorView: GpuTextureView, px: Float, py: Float, pz: Float, scale: Float, overrideColor: Int? = null) {
        if (pipeline == null || vbo == null || ibo == null) return
        val color = overrideColor ?: fillColor.get()
        val pose = makePose(px, py, pz, scale)
        val data = buildMeshData(pm, pose, color, 262144) ?: return
        drawOutlinePartDirect(data.vertexCount, pipeline, vbo, ibo, colorView)
    }

    private fun makePose(px: Float, py: Float, pz: Float, sc: Float): PoseStack {
        val rs = renderState!!
        val pose = PoseStack()
        pose.translate(px.toDouble(), py.toDouble(), pz.toDouble())
        pose.scale(sc, sc, sc)
        if (!rs.hasPose(Pose.SLEEPING)) { pose.mulPose(Axis.YP.rotationDegrees(180.0f - rs.bodyRot)) }
        pose.scale(-1.0f, -1.0f, 1.0f)
        if (rs.isBaby) pose.scale(0.5f, 0.5f, 0.5f)
        pose.scale(0.9375f, 0.9375f, 0.9375f)
        pose.translate(0.0, -1.501, 0.0)
        return pose
    }

    private val blurIndices = mutableMapOf<Int, ByteBuffer>()
    private fun drawBlurPartDirect(vertexCount: Int, pipeline: RenderPipeline, vbo: GpuBuffer, ibo: GpuBuffer, blurView: GpuTextureView, colorView: GpuTextureView) {
        if (vertexCount == 0) return
        val numQuads = vertexCount / 4; val indexCount = numQuads * 6
        val indexBuf = blurIndices.getOrPut(indexCount) {
            val buf = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder())
            for (i in 0 until numQuads) { val base = i * 4; buf.putShort(base.toShort()); buf.putShort((base + 1).toShort()); buf.putShort((base + 2).toShort()); buf.putShort(base.toShort()); buf.putShort((base + 2).toShort()); buf.putShort((base + 3).toShort()) }
            buf.flip(); buf
        }
        indexBuf.rewind()
        val camPos = mc.gameRenderer.mainCamera().position()
        val mv = Matrix4f(AporiaRenderer.worldViewMatrix)
        mv.translate(-camPos.x.toFloat(), -camPos.y.toFloat(), -camPos.z.toFloat())
        val dynamicSlice = RenderSystem.getDynamicUniforms().writeTransform(mv, Vector4f(1f, 1f, 1f, 1f), Vector3f(), mv)
        val device = RenderSystem.getDevice(); val encoder = device.createCommandEncoder()
        encoder.writeToBuffer(vbo.slice(), scratchVertex); encoder.writeToBuffer(ibo.slice(), indexBuf)
        val depthView = mc.gameRenderer.mainRenderTarget().getDepthTextureView() ?: return
        encoder.createRenderPass({ -> "aporia:player_blur" }, colorView, Optional.empty<Vector4fc>(), depthView, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(pipeline); RenderSystem.bindDefaultUniforms(pass); pass.setUniform("DynamicTransforms", dynamicSlice)
            pass.bindTexture("Sampler0", blurView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            pass.setVertexBuffer(0, vbo.slice(0L, vbo.size())); pass.setIndexBuffer(ibo, IndexType.SHORT); pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }

    private val outlineIndices = mutableMapOf<Int, ByteBuffer>()
    private fun drawOutlinePartDirect(vertexCount: Int, pipeline: RenderPipeline, vbo: GpuBuffer, ibo: GpuBuffer, colorView: GpuTextureView) {
        if (vertexCount == 0) return
        val numQuads = vertexCount / 4; val indexCount = numQuads * 6
        val indexBuf = outlineIndices.getOrPut(indexCount) {
            val buf = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder())
            for (i in 0 until numQuads) { val base = i * 4; buf.putShort(base.toShort()); buf.putShort((base + 1).toShort()); buf.putShort((base + 2).toShort()); buf.putShort(base.toShort()); buf.putShort((base + 2).toShort()); buf.putShort((base + 3).toShort()) }
            buf.flip(); buf
        }
        indexBuf.rewind()
        val camPos = mc.gameRenderer.mainCamera().position()
        val mv = Matrix4f(AporiaRenderer.worldViewMatrix)
        mv.translate(-camPos.x.toFloat(), -camPos.y.toFloat(), -camPos.z.toFloat())
        val dynamicSlice = RenderSystem.getDynamicUniforms().writeTransform(mv, Vector4f(1f, 1f, 1f, 1f), Vector3f(), mv)
        val device = RenderSystem.getDevice(); val encoder = device.createCommandEncoder()
        encoder.writeToBuffer(vbo.slice(), scratchOutlineVertex); encoder.writeToBuffer(ibo.slice(), indexBuf)
        val depthView = mc.gameRenderer.mainRenderTarget().getDepthTextureView() ?: return
        encoder.createRenderPass({ -> "aporia:player_outline" }, colorView, Optional.empty<Vector4fc>(), depthView, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(pipeline); RenderSystem.bindDefaultUniforms(pass); pass.setUniform("DynamicTransforms", dynamicSlice)
            pass.setVertexBuffer(0, vbo.slice(0L, vbo.size())); pass.setIndexBuffer(ibo, IndexType.SHORT); pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }

    private val outlineParticles = mutableMapOf<UUID, MutableList<OutlineParticle>>()
    private val rand = java.util.Random()
    private var particleFrameCounter = 0
    private class OutlineParticle(var x: Float, var y: Float, var z: Float, var vx: Float, var vy: Float, var vz: Float, var life: Float, var maxLife: Float, val size: Float)

    private fun spawnOutlineParticles(entity: Player, count: Int) {
        // Only spawn every 5 frames to prevent particle spam.
        particleFrameCounter++
        if (particleFrameCounter % 5 != 0) return
        val uuid = entity.uuid; val parts = outlineParticles.getOrPut(uuid) { mutableListOf() }
        val bb = entity.boundingBox; val cx = (bb.minX + bb.maxX) / 2.0; val cy = (bb.minY + bb.maxY) / 2.0; val cz = (bb.minZ + bb.maxZ) / 2.0
        val hw = (bb.maxX - bb.minX) / 2.0; val hh = (bb.maxY - bb.minY) / 2.0; val hz = (bb.maxZ - bb.minZ) / 2.0
        repeat(count) {
            parts.add(OutlineParticle((cx + (rand.nextDouble() - 0.5) * hw * 1.5).toFloat(), (cy + (rand.nextDouble() - 0.5) * hh * 1.5).toFloat(), (cz + (rand.nextDouble() - 0.5) * hz * 1.5).toFloat(),
                (rand.nextFloat() - 0.5f) * 0.3f, (rand.nextFloat() - 0.5f) * 0.3f, (rand.nextFloat() - 0.5f) * 0.3f,
                rand.nextFloat() * 0.8f + 0.3f, rand.nextFloat() * 0.8f + 0.3f, rand.nextFloat() * 0.08f + 0.03f))
        }
    }

    private val particleProjMat = Matrix4f(); private val particleClip = Vector4f()
    private fun updateAndRenderParticles(entity: Player, camPos: net.minecraft.world.phys.Vec3, color: Int) {
        val parts = outlineParticles[entity.uuid] ?: return; val dt = 0.016f
        val sw = mc.window.guiScaledWidth; val sh = mc.window.guiScaledHeight
        particleProjMat.set(AporiaRenderer.worldProjMatrix).mul(AporiaRenderer.worldViewMatrix)
        var i = 0
        while (i < parts.size) {
            val p = parts[i]; p.life -= dt
            if (p.life <= 0f) { parts.removeAt(i); continue }
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt
            val lifeFrac = (p.life / p.maxLife).coerceIn(0f, 1f)
            val col = colorUtil.rgba((color shr 16 and 0xFF), (color shr 8 and 0xFF), color and 0xFF, ((color shr 24 and 0xFF) * lifeFrac).toInt().coerceIn(0, 255))
            val dx = p.x.toDouble() - camPos.x; val dy = p.y.toDouble() - camPos.y; val dz = p.z.toDouble() - camPos.z
            particleClip.set(dx.toFloat(), dy.toFloat(), dz.toFloat(), 1f).mul(particleProjMat)
            if (particleClip.w <= 0) { i++; continue }
            val ndx = (particleClip.x / particleClip.w * 0.5f + 0.5f) * sw
            val ndy = (1f - (particleClip.y / particleClip.w * 0.5f + 0.5f)) * sh
            r.drawCircle(ndx, ndy, p.size * 20f, col); i++
        }
        if (parts.isEmpty()) outlineParticles.remove(entity.uuid)
    }

    private fun ensureScratchCapacity(needed: Int, buf: ByteBuffer): ByteBuffer {
        return if (buf.capacity() >= needed) buf else ByteBuffer.allocateDirect(needed.coerceAtLeast(needed)).order(ByteOrder.nativeOrder())
    }
}
