package so.aporia.module.impl.render

import so.aporia.utils.imports.*
import java.util.UUID
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
import java.util.OptionalInt
import com.chaos.annotation.ChaosNative
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
@ChaosNative
class PlayerESP : Module("PlayerESP", Category.VISUAL) {

    private val mode = SelectSetting("Mode", "Render mode")
        .value("Outline", "Blur", "TestedBlur", "Color")
        .selected("TestedBlur")

    private val range = SliderSetting("Range", "Render distance (blocks)", 48.0, 8.0, 128.0, 8.0)

    /* ===== Outline (через ColorSetting, не RGB-ползунки) ===== */
    private val outlineColor = ColorSetting(
        "Outline Color", "Contour color",
        ColorSetting.fromRGB(102, 178, 255, 220)
    )
    private val outlineWidth = SliderSetting("Outline Width", "Thickness (px)", 1.0, 0.5, 3.0, 0.1)
    private val outlineScale = SliderSetting("Outline Scale", "Expansion factor", 1.03, 1.0, 1.2, 0.005)

    /* ===== Head toggle (вместо двух «голов») ===== */
    private val renderHead = BooleanSetting("Include Head", "Render head in outline", false)

    /* ===== Color mode ===== */
    private val fillColor = ColorSetting(
        "Fill Color", "Body fill color",
        ColorSetting.fromRGB(102, 178, 255, 100)
    )

    /* ===== Friend system ===== */
    private val friendColor = ColorSetting(
        "Friend Color", "Glow color for friends",
        ColorSetting.fromRGB(0, 255, 100, 220)
    )

    /* ===== Damage flash ===== */
    private val damageColor = ColorSetting(
        "Damage Color", "Flash overlay on damage",
        ColorSetting.fromRGB(255, 0, 0, 150)
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
    private val scratchOutlineVertex = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder())

    private val self = BooleanSetting("Self", "Render on self", false)

    override val settings = listOf(mode, range, outlineColor, outlineWidth, outlineScale, renderHead, fillColor, friendColor, damageColor, self)

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
                    renderOutline(pm, oPipeline, oVbo, oIbo,
                        colorView, px, py, pz, scale, friendOverride)
                    val col = friendOverride ?: outlineColor.get()
                    spawnOutlineParticles(entity, 1)
                    updateAndRenderParticles(entity, camPos, col)
                }
                RenderMode.BLUR -> renderBodyBlur(pm, bPipeline, bVbo, bIbo,
                    blurView!!, colorView, px, py, pz, scale)
                RenderMode.TESTED_BLUR -> renderTestedBlur(pm, bPipeline, bVbo, bIbo,
                    blurView!!, colorView, px, py, pz, scale, friendOverride)
                RenderMode.COLOR -> renderColorFill(pm, cPipeline, cVbo, cIbo,
                    colorView, px, py, pz, scale, friendOverride)
            }

            // Damage flash overlay — red overlay when recently hurt
            if (entity.hurtTime > 0) {
                val dmg = damageColor.get()
                renderColorFill(pm, cPipeline, cVbo, cIbo,
                    colorView, px, py, pz, scale, dmg)
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

    private fun buildMeshData(pm: PlayerModel, pose: PoseStack, packedColor: Int, capacity: Int): MeshData? {
        pm.head.visible = renderHead.isEnabled
        pm.body.visible = true
        pm.leftArm.visible = true
        pm.rightArm.visible = true
        pm.leftLeg.visible = true
        pm.rightLeg.visible = true
        val bb = ByteBufferBuilder(capacity)
        val buf = BufferBuilder(bb, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY)
        pm.renderToBuffer(pose, buf, 15728880, OverlayTexture.NO_OVERLAY, packedColor)
        val mesh = buf.build()
        if (mesh == null) { bb.close(); return null }
        val src = mesh.vertexBuffer()
        val count = mesh.drawState().vertexCount()
        val size = src.remaining()
        val scratch = if (capacity > 262288) scratchVertex else scratchOutlineVertex
        ensureScratchCapacity(size, scratch)
        scratch.clear(); scratch.put(src); scratch.flip()
        mesh.close()
        bb.close()
        return MeshData(count, capacity > 262288)
    }

    private class MeshData(val vertexCount: Int, val isBlur: Boolean)

    private fun renderOutline(
        pm: PlayerModel,
        pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        colorView: GpuTextureView,
        px: Float, py: Float, pz: Float, scale: Float,
        overrideColor: Int? = null
    ) {
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

    private fun renderBodyBlur(
        pm: PlayerModel,
        pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        blurView: GpuTextureView, colorView: GpuTextureView,
        px: Float, py: Float, pz: Float, scale: Float
    ) {
        if (pipeline == null || vbo == null || ibo == null) return

        val pose = makePose(px, py, pz, scale)
        val data = buildMeshData(pm, pose, -1, 524288) ?: return
        drawBlurPartDirect(data.vertexCount, pipeline, vbo, ibo, blurView, colorView)
    }

    private fun renderTestedBlur(
        pm: PlayerModel,
        pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        blurView: GpuTextureView, colorView: GpuTextureView,
        px: Float, py: Float, pz: Float, scale: Float,
        overrideColor: Int? = null
    ) {
        if (pipeline == null || vbo == null || ibo == null) return

        val pose = makePose(px, py, pz, scale)
        val baseColor = overrideColor ?: outlineColor.get()
        val a = (baseColor shr 24 and 0xFF).coerceIn(0, 255)
        val scaledA = (a * outlineWidth.getFloat().coerceIn(0.1f, 2.0f) / 2f).toInt().coerceIn(0, 255)
        val packed = (scaledA shl 24) or (baseColor and 0x00FFFFFF)
        val data = buildMeshData(pm, pose, packed, 524288) ?: return
        drawBlurPartDirect(data.vertexCount, pipeline, vbo, ibo, blurView, colorView)
    }

    private fun renderColorFill(
        pm: PlayerModel,
        pipeline: RenderPipeline?, vbo: GpuBuffer?, ibo: GpuBuffer?,
        colorView: GpuTextureView,
        px: Float, py: Float, pz: Float, scale: Float,
        overrideColor: Int? = null
    ) {
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
        if (!rs.hasPose(Pose.SLEEPING)) {
            pose.mulPose(Axis.YP.rotationDegrees(180.0f - rs.bodyRot))
        }
        pose.scale(-1.0f, -1.0f, 1.0f)
        if (rs.isBaby) pose.scale(0.5f, 0.5f, 0.5f)
        pose.scale(0.9375f, 0.9375f, 0.9375f)
        pose.translate(0.0, -1.501, 0.0)
        return pose
    }

    private val blurIndices = mutableMapOf<Int, ByteBuffer>()

    private fun drawBlurPartDirect(
        vertexCount: Int,
        pipeline: RenderPipeline, vbo: GpuBuffer, ibo: GpuBuffer,
        blurView: GpuTextureView, colorView: GpuTextureView
    ) {
        if (vertexCount == 0) return

        val numQuads = vertexCount / 4
        val indexCount = numQuads * 6
        val indexBuf = blurIndices.getOrPut(indexCount) {
            val buf = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder())
            for (i in 0 until numQuads) {
                val base = i * 4
                buf.putShort(base.toShort()); buf.putShort((base + 1).toShort())
                buf.putShort((base + 2).toShort()); buf.putShort(base.toShort())
                buf.putShort((base + 2).toShort()); buf.putShort((base + 3).toShort())
            }
            buf.flip()
            buf
        }
        indexBuf.rewind()

        val camPos = mc.gameRenderer.mainCamera.position()
        val mv = Matrix4f(AporiaRenderer.worldViewMatrix)
        mv.translate(-camPos.x.toFloat(), -camPos.y.toFloat(), -camPos.z.toFloat())
        val dynamicSlice = RenderSystem.getDynamicUniforms()
            .writeTransform(mv, Vector4f(1f, 1f, 1f, 1f), Vector3f(), mv)

        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        encoder.writeToBuffer(vbo.slice(), scratchVertex)
        encoder.writeToBuffer(ibo.slice(), indexBuf)

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

    private val outlineIndices = mutableMapOf<Int, ByteBuffer>()

    private fun drawOutlinePartDirect(
        vertexCount: Int,
        pipeline: RenderPipeline, vbo: GpuBuffer, ibo: GpuBuffer,
        colorView: GpuTextureView
    ) {
        if (vertexCount == 0) return

        val numQuads = vertexCount / 4
        val indexCount = numQuads * 6

        val indexBuf = outlineIndices.getOrPut(indexCount) {
            val buf = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder())
            for (i in 0 until numQuads) {
                val base = i * 4
                buf.putShort(base.toShort()); buf.putShort((base + 1).toShort())
                buf.putShort((base + 2).toShort()); buf.putShort(base.toShort())
                buf.putShort((base + 2).toShort()); buf.putShort((base + 3).toShort())
            }
            buf.flip()
            buf
        }
        indexBuf.rewind()

        val camPos = mc.gameRenderer.mainCamera.position()
        val mv = Matrix4f(AporiaRenderer.worldViewMatrix)
        mv.translate(-camPos.x.toFloat(), -camPos.y.toFloat(), -camPos.z.toFloat())
        val dynamicSlice = RenderSystem.getDynamicUniforms()
            .writeTransform(mv, Vector4f(1f, 1f, 1f, 1f), Vector3f(), mv)

        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        encoder.writeToBuffer(vbo.slice(), scratchOutlineVertex)
        encoder.writeToBuffer(ibo.slice(), indexBuf)

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

    private val outlineParticles = mutableMapOf<UUID, MutableList<OutlineParticle>>()
    private val rand = java.util.Random()

    private class OutlineParticle(var x: Float, var y: Float, var z: Float, var vx: Float, var vy: Float, var vz: Float, var life: Float, var maxLife: Float, val size: Float)

    private fun spawnOutlineParticles(entity: Player, count: Int) {
        val uuid = entity.uuid
        val parts = outlineParticles.getOrPut(uuid) { mutableListOf() }
        val bb = entity.boundingBox
        val cx = (bb.minX + bb.maxX) / 2.0
        val cy = (bb.minY + bb.maxY) / 2.0
        val cz = (bb.minZ + bb.maxZ) / 2.0
        val hw = (bb.maxX - bb.minX) / 2.0
        val hh = (bb.maxY - bb.minY) / 2.0
        val hz = (bb.maxZ - bb.minZ) / 2.0
        repeat(count) {
            val x = cx + (rand.nextDouble() - 0.5) * hw * 1.5
            val y = cy + (rand.nextDouble() - 0.5) * hh * 1.5
            val z = cz + (rand.nextDouble() - 0.5) * hz * 1.5
            val vx = (rand.nextFloat() - 0.5f) * 0.3f
            val vy = (rand.nextFloat() - 0.5f) * 0.3f
            val vz = (rand.nextFloat() - 0.5f) * 0.3f
            val life = rand.nextFloat() * 2f + 1f
            parts.add(OutlineParticle(x.toFloat(), y.toFloat(), z.toFloat(), vx, vy, vz, life, life, rand.nextFloat() * 0.08f + 0.03f))
        }
    }

    private val particleProjMat = Matrix4f()
    private val particleClip = Vector4f()

    private fun updateAndRenderParticles(entity: Player, camPos: net.minecraft.world.phys.Vec3, color: Int) {
        val uuid = entity.uuid
        val parts = outlineParticles[uuid] ?: return
        val dt = 0.016f
        val sw = mc.window.guiScaledWidth
        val sh = mc.window.guiScaledHeight
        particleProjMat.set(AporiaRenderer.worldProjMatrix).mul(AporiaRenderer.worldViewMatrix)
        var i = 0
        while (i < parts.size) {
            val p = parts[i]
            p.life -= dt
            if (p.life <= 0f) { parts.removeAt(i); continue }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            val lifeFrac = (p.life / p.maxLife).coerceIn(0f, 1f)
            val col = colorUtil.rgba(
                (color shr 16 and 0xFF),
                (color shr 8 and 0xFF),
                color and 0xFF,
                ((color shr 24 and 0xFF) * lifeFrac).toInt().coerceIn(0, 255)
            )
            val dx = p.x.toDouble() - camPos.x
            val dy = p.y.toDouble() - camPos.y
            val dz = p.z.toDouble() - camPos.z
            particleClip.set(dx.toFloat(), dy.toFloat(), dz.toFloat(), 1f).mul(particleProjMat)
            if (particleClip.w <= 0) { i++; continue }
            val ndx = (particleClip.x / particleClip.w * 0.5f + 0.5f) * sw
            val ndy = (1f - (particleClip.y / particleClip.w * 0.5f + 0.5f)) * sh
            r.drawCircle(ndx, ndy, p.size * 20f, col)
            i++
        }
        if (parts.isEmpty()) outlineParticles.remove(uuid)
    }

    private fun ensureScratchCapacity(needed: Int, buf: ByteBuffer): ByteBuffer {
        return if (buf.capacity() >= needed) buf
        else ByteBuffer.allocateDirect(needed.coerceAtLeast(needed)).order(ByteOrder.nativeOrder())
    }
}
