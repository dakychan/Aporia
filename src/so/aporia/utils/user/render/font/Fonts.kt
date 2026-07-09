/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.font
import com.chaos.annotation.ChaosNative
@ChaosNative
object Fonts {

    @JvmField val REGULAR = "regular"
    @JvmField val BOLD = "bold"
    @JvmField val ICONS = "icons"
    @JvmField val CATICONS = "caticons"
    @JvmField val FONT = "font"

    @JvmStatic fun register(renderer: FontRenderer) {
        renderer.loadFont(REGULAR, "regularnew")
        renderer.loadFont(BOLD, "bold")
        renderer.loadFont(ICONS, "icons")
        renderer.loadFont(CATICONS, "categoryicons")
        renderer.loadFont(FONT, "font")
    }
}