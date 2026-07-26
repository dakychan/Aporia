package so.aporia.utils.user.render.core

import com.mojang.blaze3d.IndexType
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.pipeline.BindGroupLayout
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.platform.BlendFactor
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import org.joml.Matrix4f
import org.joml.Vector4fc
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import so.aporia.utils.user.logger.Logger
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import java.util.ArrayList
import java.util.Comparator
import java.util.HashMap
import java.util.Optional

class PixelsRenderer {

    private val taskQueue = ArrayList<DrawTask>()
    private class DrawTask(val z: Float, val action: Runnable)

    fun flush() {
        val batch = ArrayList(taskQueue)
        taskQueue.clear()
        batch.sortWith(Comparator.comparingDouble { t -> t.z.toDouble() })
        for (task in batch) task.action.run()
    }

    lateinit var imagePipeline: RenderPipeline
    lateinit var blitPipeline: RenderPipeline
    lateinit var imageBlitPipeline: RenderPipeline
    lateinit var postPipeline: RenderPipeline
    lateinit var orthoProjection: ProjectionMatrixBuffer

    lateinit var cachedBlitVertexBuffer: GpuBuffer
    lateinit var cachedImageVertexBuffer: GpuBuffer
    lateinit var cachedImageShapeBuffer: GpuBuffer
    lateinit var postUbo: GpuBuffer

    internal var cachedImageBB: ByteBuffer? = null

    var postTempTarget: TextureTarget? = null
    var postTempW = -1
    var postTempH = -1
    private var cachedPostBB: ByteBuffer? = null

    private val imageIds = HashMap<String, Identifier>()
    private val imageTextures = HashMap<String, DynamicTexture>()

    fun init() {
        val device = RenderSystem.getDevice()
        imagePipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/image"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/image"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/image"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("ShapeData", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("ImageTextureSampler").build())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).build()

        blitPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/blit"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/blit"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/blit"))
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("InputTexture").build())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState(BlendFunction(BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE, BlendFactor.ZERO)))
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).build()

        // Same blit shader, but with proper alpha blending so transparent PNG pixels (corners of
        // rounded/masked images) don't overwrite the framebuffer with black. blitPipeline stays
        // ONE/ZERO because applySaturation needs an exact copy.
        imageBlitPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/image_blit"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/blit"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/blit"))
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("InputTexture").build())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).build()

        postPipeline = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/post"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/post"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/post"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("PostData", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("InputTexture").build())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withColorTargetState(ColorTargetState(BlendFunction(BlendFactor.ONE, BlendFactor.ZERO, BlendFactor.ONE, BlendFactor.ZERO)))
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false)).withCull(false).build()

        cachedBlitVertexBuffer = device.createBuffer({ -> "aporia:cached_blit_vbo" },
            GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, 128L)
        cachedImageVertexBuffer = device.createBuffer({ -> "aporia:cached_image_vbo" },
            GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST, 256L)
        cachedImageShapeBuffer = device.createBuffer({ -> "aporia:cached_image_shape" },
            GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, 128L)
        postUbo = device.createBuffer({ -> "aporia:post_ubo" },
            GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, 16L)

        orthoProjection = ProjectionMatrixBuffer("aporia")
    }

    // ── drawImage ──

    fun drawImage(x: Float, y: Float, w: Float, h: Float, id: Identifier?, radius: Float = 0f) {
        if (id == null) return
        val mc = Minecraft.getInstance()
        val tex = mc.textureManager.getTexture(id) ?: return
        if (tex.textureView == null) return

        val sw = mc.window.guiScaledWidth.toFloat()
        val sh = mc.window.guiScaledHeight.toFloat()

        taskQueue.add(DrawTask(0f) { ->
            val colorView = mc.gameRenderer.mainRenderTarget().colorTextureView ?: return@DrawTask
            val view = tex.textureView ?: return@DrawTask

            if (radius <= 0f) {
                val nx0 = -1f + 2f * x / sw; val ny0 = 1f - 2f * (y + h) / sh
                val nx1 = -1f + 2f * (x + w) / sw; val ny1 = 1f - 2f * y / sh
                val bb = ByteBufferBuilder.exactlySized(2048)
                val buf = BufferBuilder(bb, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX)
                buf.addVertex(nx0, ny0, 0f).setUv(0f, 1f)
                buf.addVertex(nx1, ny0, 0f).setUv(1f, 1f)
                buf.addVertex(nx1, ny1, 0f).setUv(1f, 0f)
                buf.addVertex(nx0, ny0, 0f).setUv(0f, 1f)
                buf.addVertex(nx1, ny1, 0f).setUv(1f, 0f)
                buf.addVertex(nx0, ny1, 0f).setUv(0f, 0f)
                val mesh = buf.buildOrThrow()
                val device = RenderSystem.getDevice()
                val encoder = device.createCommandEncoder()
                encoder.writeToBuffer(cachedBlitVertexBuffer.slice(), mesh.vertexBuffer())
                mesh.close()
                val pass = encoder.createRenderPass({ -> "aporia:img_pass" }, colorView, Optional.empty<Vector4fc>())
                pass.use {
                    pass.setPipeline(imageBlitPipeline)
                    pass.bindTexture("InputTexture", view,
                        RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
                    pass.setVertexBuffer(0, cachedBlitVertexBuffer.slice(0L, cachedBlitVertexBuffer.size()))
                    pass.draw(6, 1, 0, 0)
                }
                return@DrawTask
            }

            val projMatrix = Matrix4f().setOrtho(0f, sw, sh, 0f, -1000f, 1000f)
            val projSlice = orthoProjection.getBuffer(projMatrix)
            RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC)

            val bb = ByteBufferBuilder.exactlySized(2048)
            val buf = BufferBuilder(bb, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR)
            buf.addVertex(x, y + h, 0f).setUv(0f, 1f).setColor(1f, 1f, 1f, 1f)
            buf.addVertex(x + w, y + h, 0f).setUv(1f, 1f).setColor(1f, 1f, 1f, 1f)
            buf.addVertex(x + w, y, 0f).setUv(1f, 0f).setColor(1f, 1f, 1f, 1f)
            buf.addVertex(x, y + h, 0f).setUv(0f, 1f).setColor(1f, 1f, 1f, 1f)
            buf.addVertex(x + w, y, 0f).setUv(1f, 0f).setColor(1f, 1f, 1f, 1f)
            buf.addVertex(x, y, 0f).setUv(0f, 0f).setColor(1f, 1f, 1f, 1f)
            val mesh = buf.buildOrThrow()
            val device = RenderSystem.getDevice()
            val encoder = device.createCommandEncoder()
            encoder.writeToBuffer(cachedImageVertexBuffer.slice(), mesh.vertexBuffer())
            mesh.close()

            if (cachedImageBB == null) cachedImageBB = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder())
            cachedImageBB!!.clear()
            cachedImageBB!!.putFloat(x); cachedImageBB!!.putFloat(y); cachedImageBB!!.putFloat(w); cachedImageBB!!.putFloat(h)
            cachedImageBB!!.putFloat(radius); cachedImageBB!!.putFloat(1.0f); cachedImageBB!!.putFloat(ShapesRenderer.MODE_ROUNDED_RECT.toFloat()); cachedImageBB!!.putFloat(0f)
            cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(15f)
            cachedImageBB!!.putFloat(1.0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f)
            cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f)
            cachedImageBB!!.flip()
            encoder.writeToBuffer(cachedImageShapeBuffer.slice(), cachedImageBB!!)

            val indexBuf = RenderSystem.getSequentialBuffer(PrimitiveTopology.TRIANGLES)
            val pass = encoder.createRenderPass({ -> "aporia:img_rounded_pass" }, colorView, Optional.empty<Vector4fc>())
            pass.use {
                pass.setPipeline(imagePipeline)
                RenderSystem.bindDefaultUniforms(pass)
                pass.setUniform("ShapeData", cachedImageShapeBuffer.slice())
                pass.bindTexture("ImageTextureSampler", view,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
                pass.setVertexBuffer(0, cachedImageVertexBuffer.slice(0L, cachedImageVertexBuffer.size()))
                pass.setIndexBuffer(indexBuf.getBuffer(6), indexBuf.type())
                pass.drawIndexed(6, 1, 0, 0, 0)
            }
        })
    }

    // ── drawTexture (raw GpuTextureView) ──

    fun drawTexture(x: Float, y: Float, w: Float, h: Float, view: GpuTextureView?) {
        if (view == null) return
        val mc = Minecraft.getInstance()

        taskQueue.add(DrawTask(0f) { ->
            val colorView = mc.gameRenderer.mainRenderTarget().colorTextureView ?: return@DrawTask
            val sw = mc.window.guiScaledWidth.toFloat()
            val sh = mc.window.guiScaledHeight.toFloat()
            val nx0 = -1f + 2f * x / sw; val ny0 = 1f - 2f * (y + h) / sh
            val nx1 = -1f + 2f * (x + w) / sw; val ny1 = 1f - 2f * y / sh
            val bb = ByteBufferBuilder.exactlySized(2048)
            val buf = BufferBuilder(bb, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX)
            buf.addVertex(nx0, ny0, 0f).setUv(0f, 1f)
            buf.addVertex(nx1, ny0, 0f).setUv(1f, 1f)
            buf.addVertex(nx1, ny1, 0f).setUv(1f, 0f)
            buf.addVertex(nx0, ny0, 0f).setUv(0f, 1f)
            buf.addVertex(nx1, ny1, 0f).setUv(1f, 0f)
            buf.addVertex(nx0, ny1, 0f).setUv(0f, 0f)
            val mesh = buf.buildOrThrow()
            val device = RenderSystem.getDevice()
            val encoder = device.createCommandEncoder()
            encoder.writeToBuffer(cachedBlitVertexBuffer.slice(), mesh.vertexBuffer())
            mesh.close()
            val pass = encoder.createRenderPass({ -> "aporia:tex_pass" }, colorView, Optional.empty<Vector4fc>())
            pass.use {
                pass.setPipeline(blitPipeline)
                pass.bindTexture("InputTexture", view,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
                pass.setVertexBuffer(0, cachedBlitVertexBuffer.slice(0L, cachedBlitVertexBuffer.size()))
                pass.draw(6, 1, 0, 0)
            }
        })
    }

    // ── drawImageCropped ──

    fun drawImageCropped(x: Float, y: Float, w: Float, h: Float, id: Identifier?, radius: Float,
                         u0: Float, v0: Float, u1: Float, v1: Float) {
        if (id == null) return
        val mc = Minecraft.getInstance()
        val tex = mc.textureManager.getTexture(id) ?: return
        try { if (tex.textureView == null) return } catch (_: Exception) { return }

        val sw = mc.window.guiScaledWidth.toFloat()
        val sh = mc.window.guiScaledHeight.toFloat()

        taskQueue.add(DrawTask(0f) { ->
            val colorView = mc.gameRenderer.mainRenderTarget().colorTextureView ?: return@DrawTask
            val view = tex.textureView ?: return@DrawTask
            val projMatrix = Matrix4f().setOrtho(0f, sw, sh, 0f, -1000f, 1000f)
            val projSlice = orthoProjection.getBuffer(projMatrix)
            RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC)

            val bb = ByteBufferBuilder.exactlySized(2048)
            val buf = BufferBuilder(bb, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR)
            buf.addVertex(x, y + h, 0f).setUv(u0, v1).setColor(1f, 1f, 1f, 1f)
            buf.addVertex(x + w, y + h, 0f).setUv(u1, v1).setColor(1f, 1f, 1f, 1f)
            buf.addVertex(x + w, y, 0f).setUv(u1, v0).setColor(1f, 1f, 1f, 1f)
            buf.addVertex(x, y + h, 0f).setUv(u0, v1).setColor(1f, 1f, 1f, 1f)
            buf.addVertex(x + w, y, 0f).setUv(u1, v0).setColor(1f, 1f, 1f, 1f)
            buf.addVertex(x, y, 0f).setUv(u0, v0).setColor(1f, 1f, 1f, 1f)
            val mesh = buf.buildOrThrow()
            val device = RenderSystem.getDevice()
            val encoder = device.createCommandEncoder()
            encoder.writeToBuffer(cachedImageVertexBuffer.slice(), mesh.vertexBuffer())
            mesh.close()

            if (cachedImageBB == null) cachedImageBB = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder())
            cachedImageBB!!.clear()
            cachedImageBB!!.putFloat(x); cachedImageBB!!.putFloat(y); cachedImageBB!!.putFloat(w); cachedImageBB!!.putFloat(h)
            cachedImageBB!!.putFloat(radius); cachedImageBB!!.putFloat(1.0f); cachedImageBB!!.putFloat(ShapesRenderer.MODE_ROUNDED_RECT.toFloat()); cachedImageBB!!.putFloat(0f)
            cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(15f)
            cachedImageBB!!.putFloat(1.0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f)
            cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f); cachedImageBB!!.putFloat(0f)
            cachedImageBB!!.flip()
            encoder.writeToBuffer(cachedImageShapeBuffer.slice(), cachedImageBB!!)

            val indexBuf = RenderSystem.getSequentialBuffer(PrimitiveTopology.TRIANGLES)
            val pass = encoder.createRenderPass({ -> "aporia:img_cropped" }, colorView, Optional.empty<Vector4fc>())
            pass.use {
                pass.setPipeline(imagePipeline)
                RenderSystem.bindDefaultUniforms(pass)
                pass.setUniform("ShapeData", cachedImageShapeBuffer.slice())
                pass.bindTexture("ImageTextureSampler", view,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST))
                pass.setVertexBuffer(0, cachedImageVertexBuffer.slice(0L, cachedImageVertexBuffer.size()))
                pass.setIndexBuffer(indexBuf.getBuffer(6), indexBuf.type())
                pass.drawIndexed(6, 1, 0, 0, 0)
            }
        })
    }

    // ── blitCropped ──

    fun blitCropped(x: Float, y: Float, w: Float, h: Float, id: Identifier?,
                    u0: Float, v0: Float, u1: Float, v1: Float) {
        if (id == null) return
        val mc = Minecraft.getInstance()
        val tex = mc.textureManager.getTexture(id) ?: return
        if (tex.textureView == null) return

        taskQueue.add(DrawTask(0f) { ->
            val colorView = mc.gameRenderer.mainRenderTarget().colorTextureView ?: return@DrawTask
            val view = tex.textureView ?: return@DrawTask
            val sw = mc.window.guiScaledWidth.toFloat()
            val sh = mc.window.guiScaledHeight.toFloat()
            val nx0 = -1f + 2f * x / sw; val ny0 = 1f - 2f * (y + h) / sh
            val nx1 = -1f + 2f * (x + w) / sw; val ny1 = 1f - 2f * y / sh
            val bb = ByteBufferBuilder.exactlySized(2048)
            val buf = BufferBuilder(bb, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX)
            buf.addVertex(nx0, ny0, 0f).setUv(u0, v1)
            buf.addVertex(nx1, ny0, 0f).setUv(u1, v1)
            buf.addVertex(nx1, ny1, 0f).setUv(u1, v0)
            buf.addVertex(nx0, ny0, 0f).setUv(u0, v1)
            buf.addVertex(nx1, ny1, 0f).setUv(u1, v0)
            buf.addVertex(nx0, ny1, 0f).setUv(u0, v0)
            val mesh = buf.buildOrThrow()
            val device = RenderSystem.getDevice()
            val encoder = device.createCommandEncoder()
            encoder.writeToBuffer(cachedBlitVertexBuffer.slice(), mesh.vertexBuffer())
            mesh.close()
            val pass = encoder.createRenderPass({ -> "aporia:blit_cropped_pass" }, colorView, Optional.empty<Vector4fc>())
            pass.use {
                pass.setPipeline(imageBlitPipeline)
                pass.bindTexture("InputTexture", view,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
                pass.setVertexBuffer(0, cachedBlitVertexBuffer.slice(0L, cachedBlitVertexBuffer.size()))
                pass.draw(6, 1, 0, 0)
            }
        })
    }

    // ── saturation post-process ──

    fun applySaturation(saturation: Float) {
        val mc = Minecraft.getInstance()
        val mainTarget = mc.gameRenderer.mainRenderTarget()
        val w = mainTarget.width; val h = mainTarget.height
        if (w <= 0 || h <= 0 || !::blitPipeline.isInitialized) return

        taskQueue.add(DrawTask(100f) { ->
            val colorView = mc.gameRenderer.mainRenderTarget().colorTextureView ?: return@DrawTask
            if (postTempTarget == null || postTempW != w || postTempH != h) {
                postTempTarget?.destroyBuffers()
                postTempTarget = TextureTarget("aporia_post_temp", w, h, false, GpuFormat.RGBA8_UNORM)
                postTempW = w; postTempH = h
            }
            val tempView = postTempTarget!!.colorTextureView ?: return@DrawTask

            val device = RenderSystem.getDevice()
            val encoder = device.createCommandEncoder()

            val bb = ByteBufferBuilder.exactlySized(2048)
            val quad = BufferBuilder(bb, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_TEX)
            quad.addVertex(-1f, -1f, 0f).setUv(0f, 0f)
            quad.addVertex(1f, -1f, 0f).setUv(1f, 0f)
            quad.addVertex(1f, 1f, 0f).setUv(1f, 1f)
            quad.addVertex(-1f, -1f, 0f).setUv(0f, 0f)
            quad.addVertex(1f, 1f, 0f).setUv(1f, 1f)
            quad.addVertex(-1f, 1f, 0f).setUv(0f, 1f)
            val mesh = quad.buildOrThrow()
            encoder.writeToBuffer(cachedBlitVertexBuffer.slice(), mesh.vertexBuffer())
            mesh.close()

            val pass1 = encoder.createRenderPass({ -> "aporia:post_pass" }, tempView, Optional.empty<Vector4fc>())
            pass1.use {
                it.setPipeline(blitPipeline)
                it.bindTexture("InputTexture", colorView,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
                it.setVertexBuffer(0, cachedBlitVertexBuffer.slice(0L, cachedBlitVertexBuffer.size()))
                it.draw(6, 1, 0, 0)
            }

            val pass2 = encoder.createRenderPass({ -> "aporia:post_blit" }, colorView, Optional.empty<Vector4fc>())
            pass2.use {
                if (cachedPostBB == null) cachedPostBB = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder())
                cachedPostBB!!.clear()
                cachedPostBB!!.putFloat(saturation)
                cachedPostBB!!.putFloat(0f); cachedPostBB!!.putFloat(0f); cachedPostBB!!.putFloat(0f)
                cachedPostBB!!.flip()
                encoder.writeToBuffer(postUbo.slice(), cachedPostBB!!)

                it.setPipeline(postPipeline)
                RenderSystem.bindDefaultUniforms(it)
                it.setUniform("PostData", postUbo.slice())
                it.bindTexture("InputTexture", tempView,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR))
                it.setVertexBuffer(0, cachedBlitVertexBuffer.slice(0L, cachedBlitVertexBuffer.size()))
                it.draw(6, 1, 0, 0)
            }
        })
    }

    // ── loadImage ──

    fun loadImage(path: Path): Identifier? {
        val key = path.toAbsolutePath().toString()
        imageIds[key]?.let { return it }
        try {
            FileInputStream(path.toFile()).use { stream ->
                val img = com.mojang.blaze3d.platform.NativeImage.read(stream)
                val tex = DynamicTexture({ -> key }, img)
                val name = path.fileName.toString().lowercase().replace(Regex("[^a-z0-9_.-]"), "_")
                val id = Identifier.fromNamespaceAndPath("aporia", "user_image/" + name + "_" + Math.abs(key.hashCode()))
                Minecraft.getInstance().textureManager.register(id, tex)
                imageIds[key] = id
                imageTextures[key] = tex
                return id
            }
        } catch (e: IOException) {
            Logger.warn("[PixelsRenderer] Failed to load: $path — ${e.message}")
            return null
        }
    }

    fun loadImage(stream: InputStream): Identifier? {
        val key = "discord_avatar_" + System.currentTimeMillis()
        imageIds[key]?.let { return it }
        try {
            val img = com.mojang.blaze3d.platform.NativeImage.read(stream)
            val tex = DynamicTexture({ -> key }, img)
            val id = Identifier.fromNamespaceAndPath("aporia", "user_image/discord_avatar_" + Math.abs(key.hashCode()))
            Minecraft.getInstance().textureManager.register(id, tex)
            imageIds[key] = id
            imageTextures[key] = tex
            return id
        } catch (e: IOException) {
            Logger.warn("[PixelsRenderer] Failed to load from stream: ${e.message}")
            return null
        }
    }

    fun cleanupImages() {
        for ((_, tex) in imageTextures) tex.close()
        imageIds.clear()
        imageTextures.clear()
    }

    fun close() {
        cleanupImages()
        postTempTarget?.destroyBuffers(); postTempTarget = null
        if (::cachedBlitVertexBuffer.isInitialized) cachedBlitVertexBuffer.close()
        if (::cachedImageVertexBuffer.isInitialized) cachedImageVertexBuffer.close()
        if (::cachedImageShapeBuffer.isInitialized) cachedImageShapeBuffer.close()
        if (::postUbo.isInitialized) postUbo.close()
        taskQueue.clear()
    }
}
