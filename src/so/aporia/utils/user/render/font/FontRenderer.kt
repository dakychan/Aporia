package so.aporia.utils.user.render.font

import so.aporia.utils.imports.*
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.Font

import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.math.min
import com.chaos.annotation.ChaosNative
@ChaosNative
enum class FontMode { MSDF, TTF, OTF }
@ChaosNative
class FontRenderer {

    private val pipeline = FontPipeline()
    private val fonts: MutableMap<String, FontAtlas> = HashMap()
    private var initialized = false

    @Volatile var currentMode = FontMode.MSDF
    @Volatile var currentFamily = "Default"

    private val customFonts = mutableMapOf<String, java.awt.Font>()
    private var interAwt: java.awt.Font? = null
    private val texCache = LinkedHashMap<String, CachedTex>(256, 0.75f, true)

    fun loadFont(name: String, path: String) {
        val json = Identifier.fromNamespaceAndPath("aporia", "fonts/$path.json")
        val texture = Identifier.fromNamespaceAndPath("aporia", "fonts/$path.png")
        fonts[name] = FontAtlas(json, texture)
        LOGGER.info("Registered font: {} -> {}", name, path)
    }

    fun loadCustomTTF(name: String, ttfPath: Path, size: Float = 24f): Boolean {
        return try {
            if (!Files.exists(ttfPath)) {
                LOGGER.warn("TTF not found: {}", ttfPath)
                return false
            }
            Files.newInputStream(ttfPath).use { istream ->
                val awtFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, istream).deriveFont(size)
                customFonts[name] = awtFont
            }
            if (currentFamily == name) clearCache()
            LOGGER.info("Loaded custom TTF: {} from {}", name, ttfPath)
            true
        } catch (e: Exception) {
            LOGGER.error("Failed to load TTF {} from {}", name, ttfPath, e)
            false
        }
    }

    fun initialize() {
        if (initialized) return
        LOGGER.info("Initializing {} fonts...", fonts.size)
        val t = System.currentTimeMillis()
        fonts.values.forEach { it.ensureLoaded() }
        loadTTFsFromDir()
        if (customFonts.isEmpty()) loadInter()
        initialized = true
        LOGGER.info("Fonts ready in {}ms", System.currentTimeMillis() - t)
    }

    private fun loadTTFsFromDir() {
        val dir: Path = files.ROOT.resolve(".assets/aporia/fonts")
        if (!dir.toFile().exists()) {
            LOGGER.info("TTF dir not found, skipping: {}", dir)
            return
        }
        try {
            dir.toFile().listFiles()?.filter { it.name.lowercase().endsWith(".ttf") }?.forEach { ttf ->
                val name = ttf.name.removeSuffix(".ttf")
                if (loadCustomTTF(name, ttf.toPath()) && customFonts.size == 1) {
                    currentFamily = name
                }
            }
            LOGGER.info("Loaded {} custom TTF(s) from {}", customFonts.size, dir)
        } catch (e: Exception) {
            LOGGER.error("Failed to scan TTF dir {}", dir, e)
        }
    }

    private fun loadInter() {
        try {
            javaClass.classLoader.getResourceAsStream("data/aporia/font/Inter-Regular.ttf")?.use { istr ->
                interAwt = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, istr).deriveFont(24f)
                LOGGER.info("Loaded Inter TTF")
            } ?: LOGGER.warn("Inter-Regular.ttf not in classpath")
        } catch (e: Exception) {
            LOGGER.error("Failed to load Inter", e)
        }
    }

    fun reload() {
        initialized = false
        fonts.clear()
        clearCache()
        Fonts.register(this)
        initialize()
    }

    fun clearCache() {
        
        texCache.forEach { (_, v) ->
            try { mc.textureManager.release(v.id) } catch (_: Exception) {}
        }
        texCache.clear()
    }

    fun isInitialized(): Boolean = initialized

    /** Flush pending MSDF glyphs to GPU — called by AporiaRenderer.flush() */
    fun flushPipeline() { pipeline.flush() }

    fun getAtlas(name: String): FontAtlas? = fonts[name]

    fun setMode(m: FontMode) {
        currentMode = m
        LOGGER.info("Font mode changed to: {}", m)
    }

    fun setFamily(family: String) {
        currentFamily = family
        clearCache()
        LOGGER.info("Font family changed to: {}", family)
    }

    fun availableFamilies(): List<String> = customFonts.keys.toList() + if (interAwt != null) listOf("Inter") else emptyList()

    fun drawText(fontName: String, text: String, x: Float, y: Float, size: Float, color: Int) {
        if (currentMode != FontMode.MSDF) {
            renderTTF(text, x, y, size, color, 0f, 0)
            return
        }
        val atlas = fonts[fontName] ?: return
        pipeline.drawText(atlas, text, x, y, size, color)
    }

    fun drawText(fontName: String, text: String, x: Float, y: Float, size: Float, color: Int, rotation: Float) {
        if (currentMode != FontMode.MSDF) {
            renderTTF(text, x, y, size, color, 0f, 0)
            return
        }
        val atlas = fonts[fontName] ?: return
        pipeline.drawText(atlas, text, x, y, size, color, 0f, 0, rotation)
    }

    fun drawTextWithOutline(fontName: String, text: String, x: Float, y: Float, size: Float,
                            color: Int, outlineWidth: Float, outlineColor: Int) {
        if (currentMode != FontMode.MSDF) {
            renderTTF(text, x, y, size, color, outlineWidth, outlineColor)
            return
        }
        val atlas = fonts[fontName] ?: return
        pipeline.drawText(atlas, text, x, y, size, color, outlineWidth, outlineColor, 0f)
    }

    fun drawCenteredText(fontName: String, text: String, x: Float, y: Float, size: Float, color: Int) {
        val w = getTextWidth(fontName, text, size)
        drawText(fontName, text, x - w / 2f, y, size, color)
    }

    fun getTextWidth(fontName: String, text: String, size: Float): Float {
        if (currentMode != FontMode.MSDF) return getTTFTextWidth(text, size)
        val atlas = fonts[fontName] ?: return 0f
        return pipeline.getTextWidth(atlas, text, size)
    }

    fun getLineHeight(fontName: String, size: Float): Float {
        if (currentMode != FontMode.MSDF) return size * 1.2f
        val atlas = fonts[fontName] ?: return size
        return (atlas.getLineHeight() / atlas.getFontSize()) * size
    }

    fun drawGlyph(fontName: String, index: Int, x: Float, y: Float, size: Float, color: Int) {
        drawText(fontName, String(Character.toChars(0xE000 + index)), x, y, size, color)
    }

    private fun resolveAwtFont(): java.awt.Font? {
        val fam = currentFamily
        return customFonts[fam] ?: interAwt
    }

    private fun renderTTF(text: String, x: Float, y: Float, size: Float, color: Int, outlineW: Float, outlineColor: Int) {
        if (text.isEmpty()) return
        val awtFont = resolveAwtFont() ?: run {
            renderVanillaFallback(text, x, y, size, color)
            return
        }
        
        val key = "${text}_${size}_${color}_${outlineW}_${outlineColor}"
        val cached = texCache[key]
        if (cached != null) {
            texCache.remove(key); texCache[key] = cached
            r.drawImage(x, y, cached.w / cached.scale, cached.h / cached.scale, cached.id, 0f)
            return
        }

        val renderSize = size * 2f
        val awtRenderFont = awtFont.deriveFont(renderSize)

        val tmp = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val tg = tmp.createGraphics()
        tg.font = awtRenderFont
        val fm = tg.fontMetrics
        val tw = fm.stringWidth(text)
        val th = fm.height
        tg.dispose()

        if (tw <= 0 || th <= 0) return

        val pad = (outlineW * 2f + 4f).toInt().coerceIn(2, 20)
        val img = BufferedImage(tw + pad * 2, th + pad * 2, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()

        if (outlineW > 0f) {
            val outlineSize = renderSize * 0.1f * outlineW + 0.5f
            g.font = awtRenderFont.deriveFont(renderSize + outlineSize * 2f)
            val or = (outlineColor shr 16) and 0xFF
            val og = (outlineColor shr 8) and 0xFF
            val ob = outlineColor and 0xFF
            val oa = (outlineColor shr 24) and 0xFF
            g.color = java.awt.Color(or, og, ob, oa)
            for (dx in -1..1) for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                g.drawString(text, pad + dx, pad + fm.ascent + dy)
            }
        }

        g.font = awtRenderFont
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        val red = (color shr 16) and 0xFF
        val gr = (color shr 8) and 0xFF
        val b = color and 0xFF
        val a = (color shr 24) and 0xFF
        g.color = java.awt.Color(red, gr, b, a)
        g.drawString(text, pad.toFloat(), pad + fm.ascent.toFloat())
        g.dispose()

        val nio = NativeImage(img.width, img.height, false)
        for (py in 0 until img.height) {
            for (px in 0 until img.width) {
                nio.setPixel(px, py, img.getRGB(px, py))
            }
        }

        val texId = Identifier.fromNamespaceAndPath("aporia", "ttf_cache_${key.hashCode().toLong() and 0xFFFFFFFFL}")
        val dynTex = DynamicTexture({ -> key }, nio)
        mc.textureManager.register(texId, dynTex)

        val displayScale = 1f
        texCache[key] = CachedTex(texId, img.width, img.height, displayScale)
        evictCache()
        r.drawImage(x, y, img.width.toFloat(), img.height.toFloat(), texId, 0f)
    }

    private fun evictCache() {
        if (texCache.size <= 256) return
        val iter = texCache.entries.iterator()
        
        var removed = 0
        while (iter.hasNext() && removed < 64) {
            val entry = iter.next()
            try { mc.textureManager.release(entry.value.id) } catch (_: Exception) {}
            iter.remove()
            removed++
        }
    }

    private fun renderVanillaFallback(text: String, x: Float, y: Float, size: Float, color: Int) {
        // Font fallback removed in 26.2 - TTF rendering is primary
    }

    private fun getTTFTextWidth(text: String, size: Float): Float {
        if (text.isEmpty()) return 0f
        val awtFont = resolveAwtFont() ?: return size * text.length * 0.6f
        val tmp = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        val tg = tmp.createGraphics()
        val f = awtFont.deriveFont(size)
        tg.font = f
        val w = tg.fontMetrics.stringWidth(text).toFloat()
        tg.dispose()
        return w
    }

    fun close() {
        pipeline.close()
        fonts.clear()
        customFonts.clear()
        clearCache()
        initialized = false
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger("aporia/FontRenderer")
    }

    private data class CachedTex(val id: Identifier, val w: Int, val h: Int, val scale: Float)
}
