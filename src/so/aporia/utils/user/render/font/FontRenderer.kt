/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.font

import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import java.util.HashMap

class FontRenderer {

    private val pipeline = FontPipeline()
    private val fonts: MutableMap<String, FontAtlas> = HashMap()
    private var initialized = false

    fun loadFont(name: String, path: String) {
        val json = Identifier.fromNamespaceAndPath("aporia", "fonts/$path.json")
        val texture = Identifier.fromNamespaceAndPath("aporia", "fonts/$path.png")
        fonts[name] = FontAtlas(json, texture)
        LOGGER.info("Registered font: {} -> {}", name, path)
    }

    fun initialize() {
        if (initialized) return
        LOGGER.info("Initializing {} fonts...", fonts.size)
        val t = System.currentTimeMillis()
        fonts.values.forEach { it.ensureLoaded() }
        initialized = true
        LOGGER.info("Fonts ready in {}ms", System.currentTimeMillis() - t)
    }

    fun reload() {
        initialized = false
        fonts.clear()
        Fonts.register(this)
        initialize()
    }

    fun isInitialized(): Boolean = initialized

    fun getAtlas(name: String): FontAtlas? = fonts[name]

    fun drawText(fontName: String, text: String, x: Float, y: Float, size: Float, color: Int) {
        val atlas = fonts[fontName] ?: return
        pipeline.drawText(atlas, text, x, y, size, color)
    }

    fun drawText(fontName: String, text: String, x: Float, y: Float, size: Float, color: Int, rotation: Float) {
        val atlas = fonts[fontName] ?: return
        pipeline.drawText(atlas, text, x, y, size, color, 0f, 0, rotation)
    }

    fun drawTextWithOutline(fontName: String, text: String, x: Float, y: Float, size: Float,
                            color: Int, outlineWidth: Float, outlineColor: Int) {
        val atlas = fonts[fontName] ?: return
        pipeline.drawText(atlas, text, x, y, size, color, outlineWidth, outlineColor, 0f)
    }

    fun drawCenteredText(fontName: String, text: String, x: Float, y: Float, size: Float, color: Int) {
        val atlas = fonts[fontName] ?: return
        val w = pipeline.getTextWidth(atlas, text, size)
        pipeline.drawText(atlas, text, x - w / 2f, y, size, color)
    }

    fun getTextWidth(fontName: String, text: String, size: Float): Float {
        val atlas = fonts[fontName] ?: return 0f
        return pipeline.getTextWidth(atlas, text, size)
    }

    fun getLineHeight(fontName: String, size: Float): Float {
        val atlas = fonts[fontName] ?: return size
        return (atlas.getLineHeight() / atlas.getFontSize()) * size
    }

    fun drawGlyph(fontName: String, index: Int, x: Float, y: Float, size: Float, color: Int) {
        drawText(fontName, String(Character.toChars(0xE000 + index)), x, y, size, color)
    }

    fun close() {
        pipeline.close()
        fonts.clear()
        initialized = false
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger("aporia/FontRenderer")
    }
}
