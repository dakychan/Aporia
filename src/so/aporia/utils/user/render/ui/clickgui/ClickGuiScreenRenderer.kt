package so.aporia.utils.user.render.ui.clickgui

import so.aporia.utils.imports.*
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import so.aporia.module.Category
import so.aporia.module.impl.render.clickgui.ThemeManagerModule.Theme
import aporia.webview.WebviewScreen
import org.lwjgl.glfw.GLFW

/**
 * Orchestrates the full render pipeline and handles mouse/keyboard events.
 * Delegates to TopBarRenderer and ModulesRenderer for specific sections.
 */
object ClickGuiScreenRenderer {

    fun render(r: AporiaRenderer, screen: ClickGuiScreen, mx: Int, my: Int) {
        screen.openAnim.update()
        screen.moduleSwitchAnim.update()
        screen.dropAnim.update()
        screen.categoryAnim.update()
        screen.selectionSpring.update(0.016f)
        screen.selectionSpring.setTarget(screen.selCategory.toFloat())
        screen.otherPanelSpring.update(0.016f)

        // Scroll physics
        if (kotlin.math.abs(screen.scrollVelocity) > 0.1f) {
            screen.scrollVelocity *= 0.85f
            screen.scrollY += screen.scrollVelocity
            if (screen.scrollY > 0f) { screen.scrollY = 0f; screen.scrollVelocity = 0f }
        } else { screen.scrollVelocity = 0f }

        if (kotlin.math.abs(screen.settingsScrollVelocity) > 0.1f) {
            screen.settingsScrollVelocity *= 0.85f
            screen.settingsScrollY += screen.settingsScrollVelocity
            val sMod = screen.selectedModule
            if (sMod != null) {
                val totalH = ModulesRenderer.getTotalSettingsHeight(sMod, screen)
                val visibleH = screen.panelH - ClickGuiScreen.TOP_BAR_H - ClickGuiScreen.MOD_H - ClickGuiScreen.PAD * 3
                val maxScroll = -(totalH - visibleH).coerceAtLeast(0f)
                if (screen.settingsScrollY > 0f) { screen.settingsScrollY = 0f; screen.settingsScrollVelocity = 0f }
                if (screen.settingsScrollY < maxScroll) { screen.settingsScrollY = maxScroll; screen.settingsScrollVelocity = 0f }
            }
        } else { screen.settingsScrollVelocity = 0f }

        if (screen.closing && screen.openAnim.isFinished()) {
            screen.onCloseReal()
            return
        }

        val th = screen.theme ?: return
        val fmx = mx.toFloat()
        val fmy = my.toFloat()

        // Window background
        r.drawBg(screen, screen.px, screen.py, screen.panelW, screen.panelH, ClickGuiScreen.CORNER_RADIUS, th.guiBackground, 3f)

        // Top bar
        TopBarRenderer.draw(r, th, screen, fmx, fmy)

        // Content
        when (screen.activeTab) {
            ClickGuiScreen.Tab.AVATAR -> {
                val cy = screen.contentY
                val categories = Category.values().toList()
                ModulesRenderer.drawCategories(r, th, screen, fmx, fmy)

                val otherFrac = screen.otherPanelSpring.value()
                if (otherFrac < 0.01f || screen.otherPanelSpring.getTarget() > 0.5f) {
                    if (screen.showOtherPanel) {
                        ModulesRenderer.drawScriptModuleList(r, th, screen, fmx, fmy, cy, categories)
                    } else {
                        ModulesRenderer.drawModuleList(r, th, screen, fmx, fmy, cy, categories)
                    }
                    ModulesRenderer.drawSettingsPanel(r, th, screen, fmx, fmy, cy)
                }
            }
            ClickGuiScreen.Tab.QUESTS -> ModulesRenderer.drawQuestsContent(r, th, screen)
            ClickGuiScreen.Tab.BROWSER -> {
                mc.gui.setScreen(WebviewScreen("https://google.com"))
                return
            }
            ClickGuiScreen.Tab.SETTINGS -> ModulesRenderer.drawSettingsContent(r, th, screen, fmx, fmy)
        }

        if (screen.closing) {
            screen.onCloseReal()
            return
        }

        // Bind hint overlay
        val bm = screen.bindMod ?: return
        if (screen.bindTypeAnim.getTarget() != locale.get("gui.bindprompt")) {
            screen.bindTypeAnim.setTarget(locale.get("gui.bindprompt"))
        }
        val hint = screen.bindTypeAnim.update()
        val hs = 12f
        val hw = r.getTextWidth(Fonts.BOLD, hint, hs)
        val hx = (screen.width - hw) / 2f
        r.drawBg(screen, hx - 6f, 5f, hw + 12f, hs + 4f, 5f, colorUtil.rgba(0, 0, 0, 160))
        r.drawText(Fonts.BOLD, hint, hx, 6f, hs, 0xFFFFAA00.toInt())
    }

    fun mouseReleased(screen: ClickGuiScreen) {
        screen.draggingWindow = false
        screen.dragSlider = null
        screen.dragColorSetting = false
        screen.dragColorHue = false
        screen.dragColorAlpha = false
        screen.dragColorSV = false
        screen.dragColorSettingRef = null
    }

    fun mouseDragged(mx: Float, my: Float, screen: ClickGuiScreen): Boolean {
        // Window drag
        if (screen.draggingWindow && !screen.maximized) {
            screen.windowX = (mx - screen.windowDragX).coerceIn(0f, screen.width - screen.panelW)
            screen.windowY = (my - screen.windowDragY).coerceIn(0f, screen.height - screen.panelH)
            return true
        }

        // Color picker drag
        val ref = screen.dragColorSettingRef
        if (ref != null) {
            val padX = 8f
            val pickerX = if (screen.activeTab == ClickGuiScreen.Tab.SETTINGS) screen.px + ClickGuiScreen.PAD + padX else screen.px + screen.catW + screen.modW + padX
            val sw = if (screen.activeTab == ClickGuiScreen.Tab.SETTINGS) screen.panelW - ClickGuiScreen.PAD * 2 else screen.setW - ClickGuiScreen.PAD - padX * 2f
            val pickerY = screen.pickerBoundsY
            val svSize = 60f
            val gap = 4f
            val hueX = pickerX + svSize + gap
            val hueW = 8f
            val alphaY = pickerY + svSize + gap

            if (screen.dragColorSV) {
                val sat = ((mx - pickerX) / svSize).coerceIn(0f, 1f)
                val v = (1f - (my - pickerY) / svSize).coerceIn(0f, 1f)
                ref.setHSV(ref.getHue(), sat, v, ref.getA())
                return true
            }
            if (screen.dragColorHue) {
                val h = ((my - pickerY) / svSize).coerceIn(0f, 1f)
                ref.setHSV(h, ref.getSaturation(), ref.getValue(), ref.getA())
                return true
            }
            if (screen.dragColorAlpha) {
                val a = ((mx - pickerX) / sw.coerceAtMost(160f)).coerceIn(0f, 1f)
                ref.setA((a * 255).toInt())
                return true
            }
        }

        // Slider drag
        val info = screen.dragSlider ?: return false
        val (mod, si) = info
        val sets = mod.settings; val s = sets.getOrNull(si) ?: run { screen.dragSlider = null; return true }
        if (s !is so.aporia.module.settings.SliderSetting) { screen.dragSlider = null; return true }
        val sLeft = if (screen.activeTab == ClickGuiScreen.Tab.SETTINGS) screen.px + ClickGuiScreen.PAD else screen.px + screen.catW + screen.modW
        val sWidth = if (screen.activeTab == ClickGuiScreen.Tab.SETTINGS) screen.panelW - ClickGuiScreen.PAD * 2 else screen.setW - ClickGuiScreen.PAD
        val frac = ((mx - sLeft - 10f) / (sWidth - 14f)).coerceIn(0f, 1f)
        val v = s.min + (s.max - s.min) * frac
        s.setValue((kotlin.math.round(v / s.step) * s.step).coerceIn(s.min, s.max))
        return true
    }

    fun mouseScrolled(mx: Double, my: Double, dY: Double, screen: ClickGuiScreen): Boolean {
        val mLeft = screen.px + screen.catW
        val sLeft = screen.px + screen.catW + screen.modW; val sWidth = screen.setW - ClickGuiScreen.PAD
        if (mx >= mLeft && mx < mLeft + screen.modW && my >= screen.py && my < screen.py + screen.panelH) {
            screen.scrollVelocity += (dY * 30).toFloat()
            return true
        }
        if (mx >= sLeft && mx < sLeft + sWidth && my >= screen.py && my < screen.py + screen.panelH) {
            if (screen.selectedModule != null) {
                screen.settingsScrollVelocity += (dY * 30).toFloat()
            }
            return true
        }
        return false
    }
}
