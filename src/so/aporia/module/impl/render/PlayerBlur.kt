package so.aporia.module.impl.render

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
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
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
import so.aporia.module.settings.NumberSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.WorldRenderEvent
import so.aporia.utils.user.logger.Logger
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.core.BlurRenderer
import so.aporia.utils.user.render.core.RenderFilter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.OptionalDouble
import java.util.OptionalInt

@Obfuscate
class PlayerBlur : Module("PlayerBlur", Category.VISUAL) {

    private val range = NumberSetting("Range", "Render distance (blocks)", 48.0, 8.0, 128.0, 8.0)
    private val blurRadius = NumberSetting("Blur Radius", "Blur strength multiplier", 2.0, 0.5, 8.0, 0.25)

    private var renderState: AvatarRenderState? = null
    private var playerModel: PlayerModel? = null

    private var entityBlurPipeline: RenderPipeline? = null
    private var entityBlurVBO: GpuBuffer? = null
    private var entityBlurIBO: GpuBuffer? = null

    private var scratchVertex = ByteBuffer.allocateDirect(262144).order(ByteOrder.nativeOrder())
    private var scratchIndex = ByteBuffer.allocateDirect(12000).order(ByteOrder.nativeOrder())

    override fun onEnable() {
        Logger.info("PlayerBlur enabled")
        EventBus.register(this)
        initPipeline()
        RenderFilter.setEntityFilter { e ->
            e is Player && e != Minecraft.getInstance().player
                && e.distanceTo(Minecraft.getInstance().player!!) <= range.getFloat()
        }
    }

    override fun onDisable() {
        Logger.info("PlayerBlur disabled")
        EventBus.unregister(this)
        RenderFilter.clearEntityFilter()
    }

    private fun initPipeline() {
        val device = RenderSystem.getDevice()

        entityBlurPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/entity_blur"))
            .withVertexShader("core/entity")
            .withFragmentShader("core/entity")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withUniform("Globals", UniformType.UNIFORM_BUFFER)
            .withUniform("Lighting", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withSampler("Sampler2")
            .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build()

        entityBlurVBO = device.createBuffer({ -> "aporia:entity_blur_vbo" },
            GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, 262144L)
        entityBlurIBO = device.createBuffer({ -> "aporia:entity_blur_ibo" },
            GpuBuffer.USAGE_INDEX or GpuBuffer.USAGE_COPY_DST, 12000L)
    }

    @EventHandler
    fun onWorldRender(e: WorldRenderEvent) {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val self = mc.player ?: return
        val pt = e.partialTick
        val camPos = mc.gameRenderer.mainCamera.position()

        if (!BlurRenderer.prePlayerBlurReady || BlurRenderer.prePlayerBlurTarget == null) return
        val blurView = BlurRenderer.prePlayerBlurTarget!!.getColorTextureView() ?: return

        val pipeline = entityBlurPipeline ?: return
        val vbo = entityBlurVBO ?: return
        val ibo = entityBlurIBO ?: return

        val overlayView = mc.gameRenderer.overlayTexture().getTextureView()
        val lightmapView = mc.gameRenderer.lightTexture().getTextureView()

        val view = Matrix4f(AporiaRenderer.worldViewMatrix)
        view.translate(-camPos.x.toFloat(), -camPos.y.toFloat(), -camPos.z.toFloat())
        val mvp = Matrix4f(AporiaRenderer.worldProjMatrix)
            .mul(view)

        val maxRange = range.getFloat()
        val dispatcher = mc.entityRenderDispatcher
        val colorView = mc.mainRenderTarget.getColorTextureView() ?: return

        val clipPos = Vector4f()

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

            val modelPose = PoseStack()
            modelPose.translate(px.toDouble(), py.toDouble(), pz.toDouble())
            val sc = renderState!!.scale
            modelPose.scale(sc, sc, sc)
            if (!renderState!!.hasPose(Pose.SLEEPING)) {
                modelPose.mulPose(Axis.YP.rotationDegrees(180.0f - renderState!!.bodyRot))
            }
            modelPose.scale(-1.0f, -1.0f, 1.0f)
            if (renderState!!.isBaby) {
                modelPose.scale(0.5f, 0.5f, 0.5f)
            }
            modelPose.scale(0.9375f, 0.9375f, 0.9375f)
            modelPose.translate(0.0, -1.501, 0.0)

            val bb = ByteBufferBuilder(262144)
            val buf = BufferBuilder(bb, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY)
            val uvConsumer = ScreenSpaceUvConsumer(buf, mvp, clipPos)
            playerModel!!.renderToBuffer(modelPose, uvConsumer, 15728880, OverlayTexture.NO_OVERLAY, -1)
            val mesh = buf.build()
            if (mesh == null) { bb.close(); continue }

            val totalBytes = mesh.vertexBuffer().remaining()
            val vertexCount = mesh.drawState().vertexCount()
            val vertexData = ByteArray(totalBytes)
            mesh.vertexBuffer().get(vertexData)
            mesh.close()
            bb.close()

            drawInternal(vertexData, vertexCount, pipeline, vbo, ibo, blurView, overlayView, lightmapView, colorView)
        }
    }

    private fun drawInternal(
        vertexData: ByteArray, vertexCount: Int,
        pipeline: RenderPipeline, vbo: GpuBuffer, ibo: GpuBuffer,
        blurView: GpuTextureView, overlayView: GpuTextureView, lightmapView: GpuTextureView,
        colorView: GpuTextureView
    ) {
        if (vertexCount == 0) return

        if (scratchVertex.capacity() < vertexData.size) {
            scratchVertex = ByteBuffer.allocateDirect(vertexData.size).order(ByteOrder.nativeOrder())
        }
        scratchVertex.clear()
        scratchVertex.put(vertexData)
        scratchVertex.flip()

        val numQuads = vertexCount / 4
        val indexCount = numQuads * 6
        if (scratchIndex.capacity() < indexCount * 2) {
            scratchIndex = ByteBuffer.allocateDirect(indexCount * 2).order(ByteOrder.nativeOrder())
        }
        scratchIndex.clear()
        for (i in 0 until numQuads) {
            val base = i * 4
            scratchIndex.putShort(base.toShort())
            scratchIndex.putShort((base + 1).toShort())
            scratchIndex.putShort((base + 2).toShort())
            scratchIndex.putShort(base.toShort())
            scratchIndex.putShort((base + 2).toShort())
            scratchIndex.putShort((base + 3).toShort())
        }
        scratchIndex.flip()

        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        encoder.writeToBuffer(vbo.slice(), scratchVertex)
        encoder.writeToBuffer(ibo.slice(), scratchIndex)

        val camPos = Minecraft.getInstance().gameRenderer.mainCamera.position()
        val mv = Matrix4f(AporiaRenderer.worldViewMatrix)
        mv.translate(-camPos.x.toFloat(), -camPos.y.toFloat(), -camPos.z.toFloat())
        val dynamicSlice = RenderSystem.getDynamicUniforms()
            .writeTransform(mv, Vector4f(1f, 1f, 1f, 1f), Vector3f(), mv)

        val depthView = Minecraft.getInstance().mainRenderTarget.getDepthTextureView() ?: return
        encoder.createRenderPass({ -> "aporia:entity_blur" }, colorView, OptionalInt.empty(), depthView, OptionalDouble.empty()).use { pass ->
            pass.setPipeline(pipeline)
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", dynamicSlice)
            pass.bindTexture("Sampler0", blurView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            pass.bindTexture("Sampler1", overlayView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            pass.bindTexture("Sampler2", lightmapView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            pass.setVertexBuffer(0, vbo)
            pass.setIndexBuffer(ibo, VertexFormat.IndexType.SHORT)
            pass.drawIndexed(0, 0, indexCount, 0)
        }
    }

    private class ScreenSpaceUvConsumer(
        private val delegate: VertexConsumer,
        private val mvp: Matrix4f,
        private val clipPos: Vector4f
    ) : VertexConsumer {
        private var curX = 0f
        private var curY = 0f
        private var curZ = 0f

        override fun addVertex(x: Float, y: Float, z: Float): VertexConsumer {
            curX = x; curY = y; curZ = z
            delegate.addVertex(x, y, z)
            return this
        }

        override fun setUv(u: Float, v: Float): VertexConsumer {
            clipPos.set(curX, curY, curZ, 1f)
            mvp.transform(clipPos)
            if (clipPos.w > 0f) {
                val ndcX = clipPos.x / clipPos.w
                val ndcY = clipPos.y / clipPos.w
                delegate.setUv(ndcX * 0.5f + 0.5f, 0.5f - ndcY * 0.5f)
            } else {
                delegate.setUv(u, v)
            }
            return this
        }

        override fun setColor(r: Int, g: Int, b: Int, a: Int): VertexConsumer = delegate.setColor(r, g, b, a)
        override fun setColor(argb: Int): VertexConsumer = delegate.setColor(argb)
        override fun setUv1(u: Int, v: Int): VertexConsumer = delegate.setUv1(u, v)
        override fun setUv2(u: Int, v: Int): VertexConsumer = delegate.setUv2(u, v)
        override fun setNormal(x: Float, y: Float, z: Float): VertexConsumer = delegate.setNormal(x, y, z)
        override fun setLineWidth(width: Float): VertexConsumer = delegate.setLineWidth(width)
    }

}
