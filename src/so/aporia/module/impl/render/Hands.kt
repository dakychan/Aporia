package so.aporia.module.impl.render

import so.aporia.utils.imports.*
import com.chaos.annotation.Obfuscate
import com.chaos.annotation.ChaosNative
import com.mojang.blaze3d.IndexType
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BindGroupLayout
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.QuadInstance
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.Identifier
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4fc
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.ModuleManager
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.ColorSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.module.settings.SliderSetting
import so.aporia.module.settings.TextSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.WorldRenderEvent
import so.aporia.utils.user.render.core.BlurRenderer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Optional
import java.util.OptionalDouble

/**
 * Hands — re-renders the first-person player arm (and, optionally, the held item) through our own
 * pipeline + shader, letting us overlay a custom look on the hands. Hooked from
 * ItemInHandRenderer.renderPlayerArm / renderItem (which, in 26.2, only submit to a deferred
 * SubmitNodeCollector — so we bypass them and draw the mesh ourselves in the hand projection).
 */
@Obfuscate
@ChaosNative
class Hands : Module("Hands", Category.VISUAL) {

    private val effect = SelectSetting("Effect", "Hand look")
        .value("Default", "Color", "Fade").selected("Color")
    private val color = ColorSetting("Color", "Overlay colour (Color effect)", ColorSetting.fromRGB(120, 160, 255, 255), { effect.get() == "Color" })
    private val items = BooleanSetting("Items", "Re-render the held item through the same pipeline", true)
    private val frosted = BooleanSetting("Frosted", "Frosted glass: show the kawase-blurred scene through the hand", false)
    // Fade: cycle through a configurable pool of colours. Bump "Fade Colors" to reveal more pickers.
    private val fadeCount = SliderSetting("Fade Colors", "How many colours to cycle through", 3.0, 2.0, 6.0, 1.0, { effect.get() == "Fade" })
    private val fadeSpeed = SliderSetting("Fade Speed", "Cycle speed multiplier", 1.0, 0.1, 5.0, 0.1, { effect.get() == "Fade" })
    private val fadeColors = listOf(
        ColorSetting("Fade 1", "Fade colour 1", ColorSetting.fromRGB(255, 80, 80, 255), { effect.get() == "Fade" && fadeCount.getFloat() >= 1f }),
        ColorSetting("Fade 2", "Fade colour 2", ColorSetting.fromRGB(255, 200, 80, 255), { effect.get() == "Fade" && fadeCount.getFloat() >= 2f }),
        ColorSetting("Fade 3", "Fade colour 3", ColorSetting.fromRGB(80, 255, 140, 255), { effect.get() == "Fade" && fadeCount.getFloat() >= 3f }),
        ColorSetting("Fade 4", "Fade colour 4", ColorSetting.fromRGB(80, 200, 255, 255), { effect.get() == "Fade" && fadeCount.getFloat() >= 4f }),
        ColorSetting("Fade 5", "Fade colour 5", ColorSetting.fromRGB(160, 120, 255, 255), { effect.get() == "Fade" && fadeCount.getFloat() >= 5f }),
        ColorSetting("Fade 6", "Fade colour 6", ColorSetting.fromRGB(255, 120, 220, 255), { effect.get() == "Fade" && fadeCount.getFloat() >= 6f })
    )
    private val shaderName = TextSetting("Shader", "Shader pair under core/ (vsh+fsh)", "hands", { !frosted.isEnabled })

    override val settings = listOf(effect, color, items, frosted, fadeCount, fadeSpeed) + fadeColors + shaderName

    private var pipeline: RenderPipeline? = null
    private var pipelineFailed = false
    private var builtShader: String? = null
    private var vbo: GpuBuffer? = null
    private var ibo: GpuBuffer? = null
    private val scratch = ByteBuffer.allocateDirect(VBO_BYTES).order(ByteOrder.nativeOrder())
    private val indexCache = HashMap<Int, ByteBuffer>()
    private val quadInstance = QuadInstance()

    override fun onEnable() { bus.register(this) }
    override fun onDisable() { bus.unregister(this) }

    /**
     * Frosted samples the GLOBAL kawase blur (BlurRenderer.blurTarget). That global target is only
     * refreshed at GameRenderer:452 — AFTER the hand renders — so without this it would lag one frame.
     * WorldRenderEvent fires at GameRenderer:596 (world done, hands not yet) — the perfect moment to
     * re-blur the current frame's scene into the same global target so the hand samples it in-sync.
     */
    @EventHandler
    fun onWorldRender(e: WorldRenderEvent) {
        if (frosted.isEnabled) BlurRenderer.prepareBlur(mc, 20f, 0.75f)
    }

    private fun ensurePipeline() {
        // Frosted forces the built-in scene-sampling shader; otherwise use the user's chosen pair.
        val want = if (frosted.isEnabled) FROSTED_SHADER else shaderName.get().trim().ifEmpty { "hands" }
        // Rebuild when the chosen shader changes (supports any hands.vsh/fsh pair dropped in core/).
        if (builtShader != null && builtShader != want) { pipeline = null; pipelineFailed = false }
        if (pipeline != null || pipelineFailed) return
        try {
            val device = RenderSystem.getDevice()
            pipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/hands_$want"))
                .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/$want"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/$want"))
                .withBindGroupLayout(BindGroupLayout.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).build())
                .withBindGroupLayout(BindGroupLayout.builder().withUniform("Projection", UniformType.UNIFORM_BUFFER).build())
                .withBindGroupLayout(BindGroupLayout.builder().withSampler("Sampler0").build())
                .withVertexBinding(0, DefaultVertexFormat.ENTITY).withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
                // First-person hands are an overlay — always draw on top, no depth test against the world.
                .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
                // Cull back faces: the arm/item are convex boxes, so culling fixes the face ordering
                // ("sides sliding through each other") without needing a real depth buffer.
                .withCull(true).build()
            if (vbo == null) vbo = device.createBuffer({ -> "aporia:hands_vbo" }, GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, VBO_BYTES.toLong())
            if (ibo == null) ibo = device.createBuffer({ -> "aporia:hands_ibo" }, GpuBuffer.USAGE_INDEX or GpuBuffer.USAGE_COPY_DST, IBO_BYTES.toLong())
            builtShader = want
        } catch (e: Exception) {
            pipelineFailed = true
            builtShader = want
            logger.error("Hands pipeline init failed (shader '$want' loaded?): ${e.message}")
        }
    }

    private fun tintColor(): Int = when (effect.get()) {
        "Default" -> -0x1 // white -> untouched skin
        "Fade" -> fadeColorNow()
        else -> color.get() // "Color"
    }

    /** Smoothly loops through the active fade colours (Fade 1..N) by wall-clock time. */
    private fun fadeColorNow(): Int {
        val n = fadeCount.getFloat().toInt().coerceIn(2, fadeColors.size)
        val segMs = (1500.0 / fadeSpeed.getFloat().coerceAtLeast(0.05f)).toLong().coerceAtLeast(1L)
        val pos = (System.currentTimeMillis() % (segMs * n)) / segMs.toDouble()
        val i = pos.toInt() % n
        val frac = (pos - pos.toInt()).toFloat()
        return colorUtil.lerp(fadeColors[i].get(), fadeColors[(i + 1) % n].get(), frac)
    }

    /** Builds the arm mesh (pose already baked in) and draws it with our pipeline in the hand projection. */
    private fun renderArmInternal(model: PlayerModel, arm: ModelPart, poseStack: PoseStack, skin: Identifier, hasSleeve: Boolean) {
        ensurePipeline()
        val pipe = pipeline ?: return
        val v = vbo ?: return
        val ib = ibo ?: return

        // Mirror ItemInHandRenderer's hand setup so the geometry matches the vanilla arm exactly.
        arm.resetPose(); arm.visible = true
        model.leftSleeve.visible = hasSleeve; model.rightSleeve.visible = hasSleeve
        model.leftArm.zRot = -0.1f; model.rightArm.zRot = 0.1f

        val bb = ByteBufferBuilder.exactlySized(VBO_BYTES)
        val buf = BufferBuilder(bb, PrimitiveTopology.QUADS, DefaultVertexFormat.ENTITY)
        arm.render(poseStack, buf, 15728880, OverlayTexture.NO_OVERLAY, tintColor())
        val vertexCount = copyMesh(buf, bb) ?: return
        if (vertexCount == 0) return

        val tex = mc.textureManager.getTexture(skin) ?: return
        val skinView = tex.textureView ?: return
        drawCaptured(pipe, v, ib, vertexCount, skinView)
    }

    /**
     * Re-renders a held item's baked quads through the same pipeline (its sprite atlas as Sampler0).
     * Returns true if it drew the item (so the caller skips the vanilla submit), false to fall back.
     */
    private fun renderItemInternal(state: ItemStackRenderState, poseStack: PoseStack): Boolean {
        if (!items.isEnabled) return false
        val layerCount = state.aporiaLayerCount()
        if (layerCount == 0) return false
        ensurePipeline()
        val pipe = pipeline ?: return false
        val v = vbo ?: return false
        val ib = ibo ?: return false

        val effectColor = tintColor()
        // A single item can span more than one atlas (e.g. an ITEMS-atlas base + a BLOCKS-atlas
        // overlay). Bucket the quads by their real atlas so each is drawn with the correct Sampler0 —
        // otherwise quads on the "wrong" bound atlas sample garbage / a blank square (the compass bug).
        val buckets = HashMap<Identifier, Pair<ByteBufferBuilder, BufferBuilder>>()
        fun bucketFor(atlas: Identifier): BufferBuilder = buckets.getOrPut(atlas) {
            val nbb = ByteBufferBuilder.exactlySized(VBO_BYTES)
            nbb to BufferBuilder(nbb, PrimitiveTopology.QUADS, DefaultVertexFormat.ENTITY)
        }.second
        fun closeBuckets() = buckets.values.forEach { it.first.close() }

        var wrote = false
        for (li in 0 until layerCount) {
            val layer = state.aporiaLayer(li)
            // Special renderers (shields, tridents, banners…) don't expose plain quads — bail to vanilla.
            if (layer.aporiaHasSpecialRenderer()) { closeBuckets(); return false }
            val quads = layer.prepareQuadList()
            if (quads.isEmpty()) continue
            val tints = layer.aporiaTints()
            poseStack.pushPose()
            layer.aporiaApplyTransform(poseStack.last())
            val pose = poseStack.last()
            for (q in quads) {
                val mi = q.materialInfo()
                val itemColor = if (mi.isTinted && mi.tintIndex() in tints.indices) tints[mi.tintIndex()] else -0x1
                quadInstance.setColor(itemColor)
                quadInstance.multiplyColor(effectColor)
                quadInstance.setLightCoords(15728880)
                quadInstance.setOverlayCoords(OverlayTexture.NO_OVERLAY)
                bucketFor(mi.sprite().atlasLocation()).putBakedQuad(pose, q, quadInstance)
                wrote = true
            }
            poseStack.popPose()
        }
        if (!wrote) { closeBuckets(); return false }

        var drewAny = false
        for ((atlasLoc, pair) in buckets) {
            val vertexCount = copyMesh(pair.second, pair.first) ?: continue // copyMesh closes its bb
            if (vertexCount == 0) continue
            val atlasView = mc.textureManager.getTexture(atlasLoc)?.textureView ?: continue
            drawCaptured(pipe, v, ib, vertexCount, atlasView)
            drewAny = true
        }
        return drewAny
    }

    /** Builds the BufferBuilder mesh, copies vertices into [scratch]. Returns vertexCount, or null on failure. */
    private fun copyMesh(buf: BufferBuilder, bb: ByteBufferBuilder): Int? {
        val mesh = buf.build() ?: run { bb.close(); return null }
        val vertexCount = mesh.drawState().vertexCount()
        val src = mesh.vertexBuffer()
        if (src.remaining() > scratch.capacity()) { mesh.close(); bb.close(); return null }
        scratch.clear(); scratch.put(src); scratch.flip()
        mesh.close(); bb.close()
        return vertexCount
    }

    /** Uploads [scratch] + a matching quad index buffer and draws with the given texture as Sampler0. */
    private fun drawCaptured(pipe: RenderPipeline, v: GpuBuffer, ib: GpuBuffer, vertexCount: Int, textureView: com.mojang.blaze3d.textures.GpuTextureView) {
        val numQuads = vertexCount / 4; val indexCount = numQuads * 6
        if (indexCount * 2 > IBO_BYTES) return

        // Frosted: reuse the GLOBAL kawase-blurred scene (BlurRenderer.blurTarget, full-screen, produced
        // every frame at GameRenderer:452). No extra target/blur pass — the shader just samples the slice
        // behind each fragment via gl_FragCoord. Skip the frame if the global blur isn't ready yet.
        var sampler0 = textureView
        if (frosted.isEnabled) {
            val bt = if (BlurRenderer.blurReady) BlurRenderer.blurTarget?.colorTextureView else null
            bt ?: return
            sampler0 = bt
        }
        val indexBuf = indexCache.getOrPut(indexCount) {
            val b = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder())
            for (i in 0 until numQuads) { val base = i * 4
                b.putShort(base.toShort()); b.putShort((base + 1).toShort()); b.putShort((base + 2).toShort())
                b.putShort(base.toShort()); b.putShort((base + 2).toShort()); b.putShort((base + 3).toShort()) }
            b.flip(); b
        }
        indexBuf.rewind()

        val colorView = mc.gameRenderer.mainRenderTarget().colorTextureView ?: return
        val depthView = mc.gameRenderer.mainRenderTarget().getDepthTextureView() ?: return

        // In 26.2 the hand's model-view is on RenderSystem.getModelViewStack() (set before submit),
        // and the arm/item poseStack is baked into the vertex positions. So ModelView = that stack,
        // and Projection is the level projection (bound via bindDefaultUniforms).
        val mv = Matrix4f(RenderSystem.getModelViewStack())
        val dynamicSlice = RenderSystem.getDynamicUniforms().writeTransform(mv, Vector4f(1f, 1f, 1f, 1f), Vector3f(), mv)

        val device = RenderSystem.getDevice(); val encoder = device.createCommandEncoder()
        encoder.writeToBuffer(v.slice(), scratch); encoder.writeToBuffer(ib.slice(), indexBuf)
        encoder.createRenderPass({ -> "aporia:hands" }, colorView, Optional.empty<Vector4fc>(), depthView, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(pipe); RenderSystem.bindDefaultUniforms(pass); pass.setUniform("DynamicTransforms", dynamicSlice)
            pass.bindTexture("Sampler0", sampler0, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
            pass.setVertexBuffer(0, v.slice(0L, v.size())); pass.setIndexBuffer(ib, IndexType.SHORT); pass.drawIndexed(indexCount, 1, 0, 0, 0)
        }
    }

    companion object {
        private const val VBO_BYTES = 262144
        private const val IBO_BYTES = 24576
        private const val FROSTED_SHADER = "hands_frosted"

        @JvmStatic
        fun active(): Boolean {
            val m = ModuleManager.get("Hands")
            return m != null && m.isEnabled
        }

        /** Whether held items should be re-rendered through the Hands pipeline. */
        @JvmStatic
        fun captureItems(): Boolean {
            val m = ModuleManager.get("Hands") as? Hands ?: return false
            return m.isEnabled && m.items.isEnabled
        }

        /** Called from ItemInHandRenderer.renderPlayerArm in place of the vanilla arm submit. */
        @JvmStatic
        fun renderArm(model: PlayerModel, arm: ModelPart, poseStack: PoseStack, skin: Identifier, hasSleeve: Boolean) {
            (ModuleManager.get("Hands") as? Hands)?.renderArmInternal(model, arm, poseStack, skin, hasSleeve)
        }

        /** Called from ItemInHandRenderer.renderItem; returns true if the item was drawn by us. */
        @JvmStatic
        fun renderItemCaptured(state: ItemStackRenderState, poseStack: PoseStack): Boolean {
            val m = ModuleManager.get("Hands") as? Hands ?: return false
            return m.renderItemInternal(state, poseStack)
        }
    }
}
