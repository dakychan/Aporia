package so.aporia.utils.user.render.font

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
import com.mojang.blaze3d.textures.GpuSampler
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.Identifier
import org.joml.Vector4fc
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.util.*

class FontPipeline {

    private data class CharData(
        val x: Float, val y: Float, val width: Float, val height: Float,
        val u0: Float, val v0: Float, val u1: Float, val v1: Float,
        val color: Int, val rotation: Float, val pivotX: Float, val pivotY: Float, val glyphScale: Float
    )

    private data class PendingBatch(
        val atlas: FontAtlas, val outlineWidth: Float, val outlineColor: Int, val chars: List<CharData>
    )

    private var uniformBuffer: GpuBuffer? = null
    private var dataBuffer: ByteBuffer? = null
    private var initialized = false

    private val charBatch = mutableListOf<CharData>()
    private var currentAtlas: FontAtlas? = null
    private var currentOutlineWidth = 0f
    private var currentOutlineColor = 0
    private val pendingBatches = mutableListOf<PendingBatch>()

    private fun ensureInitialized() {
        if (initialized) return
        dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE)
        initialized = true
    }

    @JvmOverloads
    fun drawText(atlas: FontAtlas, text: String, x: Float, y: Float, size: Float, color: Int,
                 outlineWidth: Float = 0f, outlineColor: Int = 0, rotation: Float = 0f) {
        if (text.isEmpty()) return
        atlas.ensureLoaded()
        if (atlas.glyphCount == 0) return
        ensureInitialized()

        if (currentAtlas != null && (currentAtlas != atlas || currentOutlineWidth != outlineWidth || currentOutlineColor != outlineColor)) {
            splitBatch()
        }
        currentAtlas = atlas
        currentOutlineWidth = outlineWidth
        currentOutlineColor = outlineColor

        val scale = size / atlas.fontSize
        val rotRad = Math.toRadians(rotation.toDouble()).toFloat()
        val pivotX = x + getTextWidth(atlas, text, size) / 2f
        val pivotY = y + getTextHeight(atlas, text, size) / 2f

        appendGlyphs(atlas, text, x, y, scale, color, rotRad, pivotX, pivotY)
    }

    fun drawTextRotatedAroundPoint(atlas: FontAtlas, text: String, x: Float, y: Float, size: Float,
                                   color: Int, outlineWidth: Float, outlineColor: Int,
                                   rotation: Float, pivotX: Float, pivotY: Float) {
        if (text.isEmpty()) return
        atlas.ensureLoaded()
        if (atlas.glyphCount == 0) return
        ensureInitialized()

        if (currentAtlas != null && (currentAtlas != atlas || currentOutlineWidth != outlineWidth || currentOutlineColor != outlineColor)) {
            splitBatch()
        }
        currentAtlas = atlas
        currentOutlineWidth = outlineWidth
        currentOutlineColor = outlineColor

        val scale = size / atlas.fontSize
        appendGlyphs(atlas, text, x, y, scale, color, Math.toRadians(rotation.toDouble()).toFloat(), pivotX, pivotY)
    }

    private fun splitBatch() {
        if (charBatch.isNotEmpty() && currentAtlas != null) {
            pendingBatches.add(PendingBatch(currentAtlas!!, currentOutlineWidth, currentOutlineColor, ArrayList(charBatch)))
            charBatch.clear()
        }
    }

    private fun appendGlyphs(atlas: FontAtlas, text: String, startX: Float, startY: Float,
                             scale: Float, baseColor: Int, rotRad: Float, pivotX: Float, pivotY: Float) {
        var cursorX = startX; var cursorY = startY
        var currentColor = baseColor
        var i = 0

        while (i < text.length) {
            val cp = text.codePointAt(i)
            val cc = Character.charCount(cp)

            if ((cp == '§'.code || cp == '&'.code) && i + cc < text.length) {
                val next = text.codePointAt(i + cc)
                if (next == '#'.code && i + cc + 6 < text.length) {
                    try {
                        currentColor = (0xFF000000.toInt()) or text.substring(i + cc + 1, i + cc + 7).toInt(16)
                        i += cc + 7; continue
                    } catch (_: Exception) {}
                }
                val code = "0123456789abcdefklmnor".indexOf(next.toChar().lowercaseChar())
                if (code >= 0) {
                    if (code < 16) currentColor = legacyColor(code)
                    else if (code == 21) currentColor = baseColor
                    i += cc + Character.charCount(next); continue
                }
            }

            if (cp == '\n'.code) {
                cursorX = startX
                cursorY += atlas.lineHeight * scale
                i += cc; continue
            }

            val glyph = atlas.getGlyph(cp)
            if (glyph == null) {
                splitBatch()
                val sz = scale * atlas.fontSize
                renderVanillaFallback(cp, cursorX, cursorY, currentColor, sz)
                val mc = Minecraft.getInstance()
                if (mc.font != null) {
                    val charStr = String(Character.toChars(cp))
                    cursorX += mc.font.width(charStr) * (sz / mc.font.lineHeight)
                } else {
                    cursorX += atlas.fontSize * scale * 0.5f
                }
                i += cc; continue
            }

            if (glyph.width > 0 && glyph.height > 0) {
                charBatch.add(CharData(
                    cursorX + glyph.xOffset * scale, cursorY + glyph.yOffset * scale,
                    glyph.width * scale, glyph.height * scale,
                    glyph.u0, glyph.v0, glyph.u1, glyph.v1,
                    currentColor, rotRad, pivotX, pivotY, scale))
            }

            cursorX += glyph.xAdvance * scale
            if (charBatch.size >= MAX_CHARS) splitBatch()
            i += cc
        }
    }

    private fun renderVanillaFallback(cp: Int, x: Float, y: Float, color: Int, size: Float) {
        // Vanilla fallback rendering removed in MC 26.2
    }

    fun flush() {
        for (pb in pendingBatches) {
            submitBatch(pb.atlas, pb.outlineWidth, pb.outlineColor, pb.chars)
        }
        pendingBatches.clear()
        if (charBatch.isNotEmpty() && currentAtlas != null) {
            submitBatch(currentAtlas!!, currentOutlineWidth, currentOutlineColor, charBatch)
        }
        charBatch.clear()
        currentAtlas = null
    }

    private fun submitBatch(atlas: FontAtlas, outlineWidth: Float, outlineColor: Int, batch: List<CharData>) {
        if (batch.isEmpty()) return
        val mc = Minecraft.getInstance()
        val texture: AbstractTexture = mc.textureManager.getTexture(atlas.textureId) ?: return

        prepareUniformData(atlas, outlineWidth, outlineColor, batch)

        val size = dataBuffer!!.remaining()
        if (uniformBuffer == null || uniformBuffer!!.size() < size) {
            uniformBuffer?.close()
            uniformBuffer = RenderSystem.getDevice().createBuffer(
                { "aporia:font_uniform" },
                GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST, size.toLong())
        }

        val encoder = RenderSystem.getDevice().createCommandEncoder()
        val db = dataBuffer!!; encoder.writeToBuffer(uniformBuffer!!.slice(), db)

        val sampler: GpuSampler = RenderSystem.getSamplerCache().getSampler(
            com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE,
            com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE,
            FilterMode.LINEAR, FilterMode.LINEAR, false)

        val colorView = mc.gameRenderer.mainRenderTarget().colorTextureView ?: return

        encoder.createRenderPass({ "aporia:font_pass" }, colorView, Optional.empty<Vector4fc>()).use { pass ->
            pass.setPipeline(PIPELINE)
            pass.bindTexture("Sampler0", texture.textureView, sampler)
            pass.setUniform("FontData", uniformBuffer!!.slice())
            pass.draw(batch.size * 6, 1, 0, 0)
        }
    }

    private fun prepareUniformData(atlas: FontAtlas, outlineWidth: Float, outlineColor: Int, batch: List<CharData>) {
        val mc = Minecraft.getInstance()
        val sw = mc.window.guiScaledWidth.toFloat()
        val sh = mc.window.guiScaledHeight.toFloat()

        dataBuffer!!.clear()
        dataBuffer!!.putFloat(sw).putFloat(sh).putFloat(FIXED_GUI_SCALE).putFloat(outlineWidth)

        dataBuffer!!.putFloat(((outlineColor shr 16) and 0xFF) / 255f)
        dataBuffer!!.putFloat(((outlineColor shr 8) and 0xFF) / 255f)
        dataBuffer!!.putFloat((outlineColor and 0xFF) / 255f)
        dataBuffer!!.putFloat(((outlineColor shr 24) and 0xFF) / 255f)

        dataBuffer!!.putFloat(atlas.atlasWidth).putFloat(atlas.atlasHeight)
        dataBuffer!!.putFloat(atlas.distanceRange).putFloat(atlas.fontSize)

        dataBuffer!!.putInt(batch.size).putInt(0).putInt(0).putInt(0)

        for (cd in batch) {
            dataBuffer!!.putFloat(cd.x).putFloat(cd.y).putFloat(cd.width).putFloat(cd.height)
            dataBuffer!!.putFloat(cd.u0).putFloat(cd.v0).putFloat(cd.u1).putFloat(cd.v1)
            dataBuffer!!.putFloat(((cd.color shr 16) and 0xFF) / 255f)
            dataBuffer!!.putFloat(((cd.color shr 8) and 0xFF) / 255f)
            dataBuffer!!.putFloat((cd.color and 0xFF) / 255f)
            dataBuffer!!.putFloat(((cd.color shr 24) and 0xFF) / 255f)
            dataBuffer!!.putFloat(cd.rotation).putFloat(cd.pivotX).putFloat(cd.pivotY).putFloat(cd.glyphScale)
        }

        dataBuffer!!.flip()
    }

    fun getTextWidth(atlas: FontAtlas, text: String, size: Float): Float {
        atlas.ensureLoaded()
        val scale = size / atlas.fontSize
        var width = 0f; var maxWidth = 0f
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val cc = Character.charCount(cp)
            if ((cp == '§'.code || cp == '&'.code) && i + cc < text.length) {
                val next = text.codePointAt(i + cc)
                if (next == '#'.code && i + cc + 6 < text.length) { i += cc + 7; continue }
                if ("0123456789abcdefklmnor".indexOf(next.toChar().lowercaseChar()) >= 0) {
                    i += cc + Character.charCount(next); continue
                }
            }
            if (cp == '\n'.code) { maxWidth = maxOf(maxWidth, width); width = 0f; i += cc; continue }
            val g = atlas.getGlyph(cp)
            if (g != null) {
                width += g.xAdvance * scale
            } else {
                val mc = Minecraft.getInstance()
                if (mc.font != null) {
                    val charStr = String(Character.toChars(cp))
                    width += mc.font.width(charStr) * (size / mc.font.lineHeight)
                } else {
                    width += size * 0.5f
                }
            }
            i += cc
        }
        return maxOf(maxWidth, width)
    }

    fun getTextHeight(atlas: FontAtlas, text: String, size: Float): Float {
        atlas.ensureLoaded()
        var lines = 1
        for (c in text) if (c == '\n') lines++
        return lines * atlas.lineHeight * (size / atlas.fontSize)
    }

    fun close() {
        uniformBuffer?.close(); uniformBuffer = null
        dataBuffer?.let { MemoryUtil.memFree(it) }; dataBuffer = null
        initialized = false
    }

    companion object {
        private const val MAX_CHARS = 256
        private const val HEADER_SIZE = 16 * 4 + 4 * 4
        private const val GLYPH_SIZE = 16 * 4
        private const val BUFFER_SIZE = HEADER_SIZE + MAX_CHARS * GLYPH_SIZE
        private const val FIXED_GUI_SCALE = 2.0f

        private val PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("aporia", "pipeline/msdf"))
            .withVertexShader(Identifier.fromNamespaceAndPath("aporia", "core/msdf"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("aporia", "core/msdf"))
            .withVertexBinding(0, VertexFormat.builder(0).build())
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withBindGroupLayout(BindGroupLayout.builder()
                .withUniform("FontData", UniformType.UNIFORM_BUFFER)
                .withSampler("Sampler0")
                .build())
            .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
            .withCull(false)
            .build()

        @JvmStatic
        fun legacyColor(index: Int): Int {
            val j = (index shr 3 and 1) * 85
            var r = (index shr 2 and 1) * 170 + j
            val g = (index shr 1 and 1) * 170 + j
            val b = (index and 1) * 170 + j
            if (index == 6) r += 85
            return (255 shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
}
