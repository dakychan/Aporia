/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.font

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.HashMap
import java.util.concurrent.atomic.AtomicBoolean

class FontAtlas(private val jsonId: Identifier, private val textureId: Identifier) {

    private val glyphs: MutableMap<Int, Glyph> = HashMap()
    private val loaded = AtomicBoolean(false)

    private var atlasWidth: Float = 512f
    private var atlasHeight: Float = 512f
    private var fontSize: Float = 32f
    private var lineHeight: Float = 40f
    private var distanceRange: Float = 4f
    private var ascender: Float = 0.95f
    private var yOriginBottom: Boolean = false

    fun ensureLoaded() {
        if (loaded.get()) return
        synchronized(this) {
            if (loaded.get()) return
            doLoad()
        }
    }

    private fun doLoad() {
        try {
            val res = Minecraft.getInstance().resourceManager.getResource(jsonId)
            if (res.isEmpty) {
                LOGGER.warn("Font JSON not found in ResourceManager: {}, trying AssetManager...", jsonId)
                loadFromAssetManager()
                return
            }
            res.get().open().use { `is` ->
                InputStreamReader(`is`, StandardCharsets.UTF_8).use { reader ->
                    parseJson(JsonParser.parseReader(reader).asJsonObject)
                    loaded.set(true)
                    LOGGER.info("Loaded font: {} ({} glyphs)", jsonId, glyphs.size)
                }
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load font: {}", jsonId, e)
            loaded.set(true)
        }
    }

    private fun loadFromAssetManager() {
        try {
            val json = so.aporia.utils.assets.AssetManager.getResourceString(jsonId)

            if (json != null) {
                parseJson(JsonParser.parseString(json).asJsonObject)
                loaded.set(true)
                LOGGER.info("Loaded font from AssetManager: {} ({} glyphs)", jsonId, glyphs.size)
            } else {
                throw Exception("AssetManager returned null (file not found)")
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load font from AssetManager: {}", jsonId, e)
            loaded.set(true)
        }
    }

    private fun parseJson(root: JsonObject) {
        if (root.has("atlas")) {
            val atlas = root.getAsJsonObject("atlas")
            atlasWidth = getFloat(atlas, "width", 512f)
            atlasHeight = getFloat(atlas, "height", 512f)
            fontSize = getFloat(atlas, "size", 32f)
            distanceRange = getFloat(atlas, "distanceRange", 4f)
            if (atlas.has("yOrigin")) {
                yOriginBottom = atlas.get("yOrigin").asString.equals("bottom", ignoreCase = true)
            }
        }

        if (root.has("metrics")) {
            val metrics = root.getAsJsonObject("metrics")
            lineHeight = getFloat(metrics, "lineHeight", 1.2f) * fontSize
            ascender = getFloat(metrics, "ascender", 0.95f)
        }

        if (root.has("glyphs")) {
            for (elem in root.getAsJsonArray("glyphs")) {
                parseGlyph(elem.asJsonObject)
            }
        }
    }

    private fun parseGlyph(g: JsonObject) {
        var unicode = -1
        if (g.has("unicode")) unicode = g.get("unicode").asInt
        else if (g.has("char")) {
            val s = g.get("char").asString
            if (s.isNotEmpty()) unicode = s.codePointAt(0)
        } else if (g.has("id")) unicode = g.get("id").asInt
        else if (g.has("index")) unicode = g.get("index").asInt + 0xE000
        if (unicode < 0) return

        var advance = getFloat(g, "advance", 0f) * fontSize
        if (advance == 0f) advance = getFloat(g, "xadvance", 0f)

        var x = 0f; var y = 0f; var w = 0f; var h = 0f; var xOff = 0f; var yOff = 0f

        if (g.has("atlasBounds")) {
            val b = g.getAsJsonObject("atlasBounds")
            val left = getFloat(b, "left", 0f); val bottom = getFloat(b, "bottom", 0f)
            val right = getFloat(b, "right", 0f); val top = getFloat(b, "top", 0f)
            x = left
            w = right - left
            h = top - bottom
            y = if (yOriginBottom) atlasHeight - top else bottom
        } else if (g.has("x")) {
            x = getFloat(g, "x", 0f); y = getFloat(g, "y", 0f)
            w = getFloat(g, "width", 0f); h = getFloat(g, "height", 0f)
        }

        if (g.has("planeBounds")) {
            val p = g.getAsJsonObject("planeBounds")
            xOff = getFloat(p, "left", 0f) * fontSize
            yOff = (ascender - getFloat(p, "top", 0f)) * fontSize
        } else {
            xOff = getFloat(g, "xoffset", 0f)
            yOff = getFloat(g, "yoffset", 0f)
        }

        glyphs[unicode] = Glyph(unicode, x, y, w, h, xOff, yOff, advance, atlasWidth, atlasHeight)
    }

    private fun getFloat(obj: JsonObject, key: String, def: Float): Float {
        return if (obj.has(key)) obj.get(key).asFloat else def
    }

    fun getGlyph(codePoint: Int): Glyph? = glyphs[codePoint]

    fun hasGlyph(codePoint: Int): Boolean = glyphs.containsKey(codePoint)

    fun getTextureId(): Identifier = textureId

    fun getFontSize(): Float = fontSize

    fun getLineHeight(): Float = lineHeight

    fun getAtlasWidth(): Float = atlasWidth

    fun getAtlasHeight(): Float = atlasHeight

    fun getDistanceRange(): Float = distanceRange

    fun isLoaded(): Boolean = loaded.get()

    fun getGlyphCount(): Int = glyphs.size

    fun getAscender(): Float = ascender

    companion object {
        private val LOGGER = LoggerFactory.getLogger("aporia/Font")
    }
}
