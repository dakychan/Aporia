/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.font
import com.chaos.annotation.ChaosNative
@ChaosNative
class Glyph(
    @JvmField val id: Int,
    @JvmField val x: Float,
    @JvmField val y: Float,
    @JvmField val width: Float,
    @JvmField val height: Float,
    @JvmField val xOffset: Float,
    @JvmField val yOffset: Float,
    @JvmField val xAdvance: Float,
    atlasWidth: Float,
    atlasHeight: Float
) {
    @JvmField val u0: Float = x / atlasWidth
    @JvmField val v0: Float = y / atlasHeight
    @JvmField val u1: Float = (x + width) / atlasWidth
    @JvmField val v1: Float = (y + height) / atlasHeight
}