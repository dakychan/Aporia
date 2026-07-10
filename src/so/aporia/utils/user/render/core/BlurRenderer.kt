package so.aporia.utils.user.render.core

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.platform.DestFactor
import com.mojang.blaze3d.platform.SourceFactor
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.OptionalInt

object BlurRenderer {

    lateinit var kawaseUpPipeline: RenderPipeline
    lateinit var blitPipeline: RenderPipeline

    var kawaseDownTargets: Array<TextureTarget?> = arrayOfNulls(5)
    var blurTarget: TextureTarget? = null
    var guiBlurTarget: TextureTarget? = null
    var guiBlurReady = false
    var guiBlurTargetsDirty = true

    var prePlayerBlurTarget: TextureTarget? = null
    var prePlayerBlurReady = false

    var blurTargetW = -1
    var blurTargetH = -1
    var blurReady = false
    var blurTargetsDirty = true

    lateinit var blurQuadVbo: GpuBuffer
    lateinit var blurUbo: GpuBuffer

    var cachedBlurStrength = -1f
    var cachedBlurSaturation = -1f

    var useGuiBlur = false

    private var cachedBlurBB: ByteBuffer? = null

    fun init() {
        val device = RenderSystem.getDevice()

        kawaseUpPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/kawase_up"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/kawase_up"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/kawase_up"))
            .withSampler("InputTexture")
            .withUniform("KawaseData", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false).withCull(false).build()

        val tess = Tesselator.getInstance()
        val buf = tess.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX)
        buf.addVertex(-1f, 1f, 0f).setUv(0f, 0f)
        buf.addVertex(-1f, -1f, 0f).setUv(0f, 1f)
        buf.addVertex(1f, -1f, 0f).setUv(1f, 1f)
        buf.addVertex(-1f, 1f, 0f).setUv(0f, 0f)
        buf.addVertex(1f, -1f, 0f).setUv(1f, 1f)
        buf.addVertex(1f, 1f, 0f).setUv(1f, 0f)
        val quadMesh = buf.buildOrThrow()
        blurQuadVbo = device.createBuffer({ -> "aporia:blur_quad" },
            GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, quadMesh.vertexBuffer())
        quadMesh.close()

        blurUbo = device.createBuffer({ -> "aporia:blur_ubo" },
            GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, 32L)

        blitPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/blit"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/blit"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/blit"))
            .withSampler("InputTexture")
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.TRIANGLES)
            .withBlend(BlendFunction(SourceFactor.ONE, DestFactor.ZERO, SourceFactor.ONE, DestFactor.ZERO))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false).withCull(false).build()
    }

    fun ensureBlurTarget(mc: Minecraft) {
        val mainTarget = mc.mainRenderTarget
        val mainW = mainTarget.width; val mainH = mainTarget.height
        if (kawaseDownTargets.isNotEmpty() && kawaseDownTargets[0] != null) {
            if (blurTargetW == mainW && blurTargetH == mainH) return
        }
        for (i in kawaseDownTargets.indices) {
            kawaseDownTargets[i]?.destroyBuffers()
            kawaseDownTargets[i] = null
        }
        blurTarget?.destroyBuffers(); blurTarget = null
        blurTargetW = mainW; blurTargetH = mainH; blurTargetsDirty = true
        blurTarget = TextureTarget("aporia_blur_final", mainW, mainH, false)
        var cw = mainW; var ch = mainH
        for (i in kawaseDownTargets.indices) {
            cw = Math.max(1, cw / 2); ch = Math.max(1, ch / 2)
            kawaseDownTargets[i] = TextureTarget("aporia_blur_down_$i", cw, ch, false)
        }
    }

    fun ensureGuiBlurTarget(mc: Minecraft) {
        ensureBlurTarget(mc)
        val mainTarget = mc.mainRenderTarget
        val mainW = mainTarget.width; val mainH = mainTarget.height
        if (guiBlurTarget != null) { if (blurTargetW == mainW && blurTargetH == mainH) return }
        guiBlurTarget?.destroyBuffers(); guiBlurTarget = null
        guiBlurTarget = TextureTarget("aporia_gui_blur", mainW, mainH, false)
        guiBlurTargetsDirty = true
    }

    fun ensurePrePlayerBlurTarget(mc: Minecraft) {
        val mainTarget = mc.mainRenderTarget
        val mainW = mainTarget.width; val mainH = mainTarget.height
        if (prePlayerBlurTarget != null && prePlayerBlurTarget!!.width == mainW && prePlayerBlurTarget!!.height == mainH) return
        prePlayerBlurTarget?.destroyBuffers(); prePlayerBlurTarget = null
        prePlayerBlurTarget = TextureTarget("aporia_pre_player_blur", mainW, mainH, false)
        prePlayerBlurReady = false
    }

    private fun fillBlurBB(target: RenderTarget, offset: Float) {
        if (cachedBlurBB == null) cachedBlurBB = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder())
        cachedBlurBB!!.clear()
        cachedBlurBB!!.putFloat(target.width.toFloat())
        cachedBlurBB!!.putFloat(target.height.toFloat())
        cachedBlurBB!!.putFloat(offset)
        cachedBlurBB!!.putFloat(0f)
        cachedBlurBB!!.putFloat(0f); cachedBlurBB!!.putFloat(0f); cachedBlurBB!!.putFloat(0f); cachedBlurBB!!.putFloat(0f)
        cachedBlurBB!!.flip()
    }

    @JvmStatic fun prepareGuiBlur(mc: Minecraft, strength: Float, saturation: Float) {
        ensureGuiBlurTarget(mc)
        if (guiBlurTargetsDirty) { guiBlurTargetsDirty = false; guiBlurReady = false; return }

        val mainTarget = mc.mainRenderTarget
        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        val maxSteps = Math.max(1, Math.min(5, Math.round(strength / 6.0f)))
        var currentSrc: RenderTarget = mainTarget

        for (i in maxSteps - 1 downTo 0) {
            val currentDst = if (i == 0) guiBlurTarget!! else kawaseDownTargets[i - 1]!!
            val offset = 0.5f + (maxSteps - 1 - i) * 0.25f
            fillBlurBB(currentDst, offset); encoder.writeToBuffer(blurUbo.slice(), cachedBlurBB!!)
            val pass = encoder.createRenderPass({ -> "aporia:gui_kawase_d_$i" }, currentDst.colorTextureView!!, OptionalInt.of(0))
            pass.use {
                it.setPipeline(kawaseUpPipeline)
                it.bindTexture("InputTexture", currentSrc.colorTextureView!!,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
                it.setUniform("KawaseData", blurUbo.slice())
                it.setVertexBuffer(0, blurQuadVbo)
                it.draw(0, 6)
            }
            currentSrc = currentDst
        }

        for (i in maxSteps - 1 downTo 0) {
            val currentDst = if (i == 0) guiBlurTarget!! else kawaseDownTargets[i - 1]!!
            val offset = 0.5f
            fillBlurBB(currentDst, offset); encoder.writeToBuffer(blurUbo.slice(), cachedBlurBB!!)
            val pass = encoder.createRenderPass({ -> "aporia:gui_kawase_u_$i" }, currentDst.colorTextureView!!, OptionalInt.of(0))
            pass.use {
                it.setPipeline(kawaseUpPipeline)
                it.bindTexture("InputTexture", currentSrc.colorTextureView!!,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
                it.setUniform("KawaseData", blurUbo.slice())
                it.setVertexBuffer(0, blurQuadVbo)
                it.draw(0, 6)
            }
            currentSrc = currentDst
        }
        guiBlurReady = true
    }

    @JvmStatic fun prepareBlur(mc: Minecraft, strength: Float, saturation: Float) {
        ensureBlurTarget(mc)
        if (blurTargetsDirty) { blurTargetsDirty = false; blurReady = false; return }

        val mainTarget = mc.mainRenderTarget
        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        val maxSteps = Math.max(1, Math.min(5, Math.round(strength / 6.0f)))
        var currentSrc: RenderTarget = mainTarget

        for (i in maxSteps - 1 downTo 0) {
            val currentDst = if (i == 0) blurTarget!! else kawaseDownTargets[i - 1]!!
            val offset = 0.5f + (maxSteps - 1 - i) * 0.25f
            fillBlurBB(currentDst, offset); encoder.writeToBuffer(blurUbo.slice(), cachedBlurBB!!)
            val pass = encoder.createRenderPass({ -> "aporia:kawase_d_$i" }, currentDst.colorTextureView!!, OptionalInt.of(0))
            pass.use {
                it.setPipeline(kawaseUpPipeline)
                it.bindTexture("InputTexture", currentSrc.colorTextureView!!,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
                it.setUniform("KawaseData", blurUbo.slice())
                it.setVertexBuffer(0, blurQuadVbo)
                it.draw(0, 6)
            }
            currentSrc = currentDst
        }

        for (i in maxSteps - 1 downTo 0) {
            val currentDst = if (i == 0) blurTarget!! else kawaseDownTargets[i - 1]!!
            val offset = 0.5f
            fillBlurBB(currentDst, offset); encoder.writeToBuffer(blurUbo.slice(), cachedBlurBB!!)
            val pass = encoder.createRenderPass({ -> "aporia:kawase_u_$i" }, currentDst.colorTextureView!!, OptionalInt.of(0))
            pass.use {
                it.setPipeline(kawaseUpPipeline)
                it.bindTexture("InputTexture", currentSrc.colorTextureView!!,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
                it.setUniform("KawaseData", blurUbo.slice())
                it.setVertexBuffer(0, blurQuadVbo)
                it.draw(0, 6)
            }
            currentSrc = currentDst
        }
        blurReady = true
    }

    @JvmStatic fun prepareBlurForPlayer(mc: Minecraft, strength: Float, saturation: Float) {
        ensureBlurTarget(mc)
        ensurePrePlayerBlurTarget(mc)
        if (blurTargetsDirty) { blurTargetsDirty = false; prePlayerBlurReady = false; return }

        val mainTarget = mc.mainRenderTarget
        val device = RenderSystem.getDevice()
        val encoder = device.createCommandEncoder()
        val pass = encoder.createRenderPass({ -> "aporia:player_blit" }, prePlayerBlurTarget!!.colorTextureView!!, OptionalInt.empty())
        pass.use {
            it.setPipeline(blitPipeline)
            it.bindTexture("InputTexture", mainTarget.colorTextureView!!,
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
            it.setVertexBuffer(0, blurQuadVbo)
            it.draw(0, 6)
        }
        prePlayerBlurReady = true
    }

    @JvmStatic fun prepareFrameBlur(mc: Minecraft, strength: Float, saturation: Float) {
        prepareBlur(mc, strength, saturation)
        cachedBlurStrength = strength
        cachedBlurSaturation = saturation
    }

    @JvmStatic fun cleanup() {
        blurTarget?.destroyBuffers(); blurTarget = null
        guiBlurTarget?.destroyBuffers(); guiBlurTarget = null
        prePlayerBlurTarget?.destroyBuffers(); prePlayerBlurTarget = null
        for (t in kawaseDownTargets) { t?.destroyBuffers() }
        kawaseDownTargets = arrayOfNulls(5)
        blurTargetW = -1; blurTargetH = -1
        blurReady = false; prePlayerBlurReady = false; guiBlurReady = false
        blurTargetsDirty = true; guiBlurTargetsDirty = true
    }
}
