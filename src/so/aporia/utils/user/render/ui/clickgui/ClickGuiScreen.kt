package so.aporia.utils.user.render.ui.clickgui

import so.aporia.utils.imports.*
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.ModuleManager
import so.aporia.module.impl.misc.ClickGui
import so.aporia.module.settings.*
import so.aporia.utils.user.render.font.Fonts
import org.lwjgl.glfw.GLFW
import com.chaos.annotation.ChaosNative
import so.aporia.utils.files.FilesManager
import kotlin.math.abs

@ChaosNative
class ClickGuiScreen(val clickGui: ClickGui) : Screen(Component.literal("ClickGui")) {

    // ── state ──
    private val expandedModules = mutableSetOf<Module>()
    private val settingsAnimations = mutableMapOf<Module, Float>()
    private val expandedMultiSettings = mutableMapOf<Module, MutableSet<MultiSelectSetting>>()
    private val panelScroll = mutableMapOf<Category, Float>()
    private val categoryPanels = mutableMapOf<Category, CategoryPanel>()
    private var draggingPanel: CategoryPanel? = null
    private var dragOffX = 0f
    private var dragOffY = 0f
    private var hoveredModule: Module? = null
    private var keybindTarget: Module? = null
    private var draggedSlider: Pair<Module, SliderSetting>? = null
    private var panelsPositioned = false
    private val PANEL_FILE = FilesManager.ROOT.resolve("gui_panels.json")

    // all dimensions in PHYSICAL PIXELS (like Java framebuffer coords)
    companion object {
        const val CAT_W_PX = 175
        const val CAT_SP_PX = 15
        const val PANEL_H_PX = 385
        const val MOD_H_PX = 32
        const val MOD_SP_PX = 8
        const val HEADER_H_PX = 38
        const val SET_SP_PX = 35
        const val SET_PAD_PX = 20
        const val ANIM_SPEED = 0.15f
    }

    // gui scale factor: framebuffer pixels per scaled unit
    private val sc: Float get() = mc.window.width.toFloat() / width.toFloat()
    private fun px(v: Int) = v / sc
    private fun px(v: Float) = v / sc
    private val useBasicBlur: Boolean get() = clickGui.blurStyle.value == "BasicBlur"

    init {
        initializePanels()
        loadPanelPositions()
    }

    private fun initializePanels() {
        categoryPanels.clear()
        var cx = px(100).roundToInt()
        for (cat in Category.values()) {
            categoryPanels[cat] = CategoryPanel(cat, cx, px(50).roundToInt())
            cx += (px(CAT_W_PX) + px(CAT_SP_PX)).roundToInt()
        }
    }

    private fun centerPanels() {
        if (panelsPositioned) return
        val cats = Category.values()
        val panelW = px(CAT_W_PX); val panelSp = px(CAT_SP_PX); val panelH = px(PANEL_H_PX)
        val totalW = panelW * cats.size + panelSp * (cats.size - 1)
        val startX = ((width - totalW) / 2f).toInt().coerceAtLeast(10)
        val startY = ((height - panelH) / 2f).toInt().coerceAtLeast(10)
        var cx = startX.toFloat()
        for (cat in cats) {
            categoryPanels[cat]?.setPosition(cx.roundToInt(), startY)
            cx += panelW + panelSp
        }
        panelsPositioned = true
    }

    override fun init() {
        centerPanels()
    }

    override fun extractBackground(g: GuiGraphicsExtractor, mx: Int, my: Int, d: Float) {}

    override fun extractRenderState(g: GuiGraphicsExtractor, mx: Int, my: Int, d: Float) {
        val fbW = width.toFloat(); val fbH = height.toFloat()
        r.shapes.depth(100f)

        hoveredModule = null
        updateAnimations()

        for (panel in categoryPanels.values) renderCategory(panel, mx, my)

        // description tooltip
        val hm = hoveredModule
        if (hm != null && keybindTarget == null) {
            val name = hm.name
            val descVal = hm.settings.firstOrNull()?.description ?: ""
            val nfs = px(30f); val dfs = px(26f)
            val nw = r.getTextWidth(Fonts.BOLD, name, nfs)
            val dashW = r.getTextWidth(Fonts.REGULAR, " - ", dfs)
            val dw = r.getTextWidth(Fonts.REGULAR, descVal, dfs)
            val totalW = nw + dashW + dw
            val tx = ((fbW - totalW) / 2f)
            val ty = px(60f)
            r.drawText(Fonts.BOLD, name, tx, ty, nfs, -0x1)
            r.drawText(Fonts.REGULAR, " - $descVal", tx + nw + px(8f), ty + px(3f), dfs, 0xFFC8C8D2.toInt())
        }

        // bind prompt
        if (keybindTarget != null) {
            val prompt = locale.get("gui.bindprompt")
            val ps = px(12f)
            val pw = r.getTextWidth(Fonts.BOLD, prompt, ps)
            val bx = (fbW - pw) / 2f
            r.drawRect(bx - px(6f), px(5f), pw + px(12f), ps + px(4f), px(5f), 0xA0000000.toInt())
            r.drawText(Fonts.BOLD, prompt, bx, px(5f), ps, -0x1)
        }

        // reset depth so subsequent renders (HUD etc.) don't inherit our z
        r.shapes.depth(0f)
    }

    // ── Category Panel ──────────────────────────────────────────

    private fun renderCategory(panel: CategoryPanel, mx: Int, my: Int) {
        val x = panel.x; val y = panel.y
        val cat = panel.category
        val modules = ModuleManager.getByCategory(cat)
        val panelW = px(CAT_W_PX); val panelH = px(PANEL_H_PX)
        val headerH = px(HEADER_H_PX); val modH = px(MOD_H_PX); val modSp = px(MOD_SP_PX)
        val contentTop = y.toFloat() + headerH + px(5f)
        val contentBottom = y + panelH - px(5f)
        val viewH = contentBottom - contentTop

        // total content height — use ANIMATED settings height so clamping is exact
        val totalContentH = modules.sumOf { mod ->
            val maxH = calcSettingsHeight(mod).toFloat()
            val progress = settingsAnimations[mod] ?: 0f
            (modH + maxH * progress).toDouble()
        }.toFloat() + maxOf(0, modules.size - 1) * modSp
        val maxScroll = minOf(0f, viewH - totalContentH)
        val scroll = (panelScroll[cat] ?: 0f).coerceIn(maxScroll, 0f)
        panelScroll[cat] = scroll

        // panel bg (30% color + 70% blur = frosted glass)
        drawRectWithBlur(x.toFloat(), y.toFloat(), panelW, panelH, px(8f), 0x4D141419.toInt(), 3f)
        // header bg
        drawRectWithBlur(x.toFloat(), y.toFloat(), panelW, headerH, px(8f), 0x4D1E1E28.toInt(), 3f)

        // header: icon + name (vertically centered in header)
        val iconChar = cat.icon()
        r.drawText(Fonts.CATICONS, iconChar.toString(), x.toFloat() + px(10f), y.toFloat() + px(10f), px(18f), -0x1)
        val label = locale.get("category.${cat.name.lowercase()}")
        r.drawText(Fonts.REGULAR, label, x.toFloat() + px(35f), y.toFloat() + px(11f), px(16f), -0x1)

        // modules
        var moduleY = contentTop + scroll
        val clipTop = contentTop
        val clipBottom = contentBottom
        for (mod in modules) {
            if (moduleY > clipBottom) break
            val settingsProgress = settingsAnimations[mod] ?: 0f
            val maxSettingsH = calcSettingsHeight(mod).toFloat()
            val settingsH = maxSettingsH * settingsProgress
            val moduleBottom = moduleY + modH + modSp + settingsH
            if (moduleY < clipTop) { moduleY = moduleBottom; continue }

            renderModule(mod, x.toFloat() + px(5f), moduleY, panelW - px(10f), modH, mx, my)
            if (settingsProgress > 0.01f && settingsH > 0f) {
                val cappedH = minOf(settingsH, (clipBottom - (moduleY + modH)).coerceAtLeast(0f))
                renderModuleSettings(mod, x.toFloat() + px(5f), moduleY + modH, panelW - px(10f), cappedH.roundToInt(), settingsProgress, mx, my)
            }
            moduleY = moduleBottom
        }
    }

    private fun renderModule(mod: Module, rx: Float, ry: Float, rw: Float, rh: Float, mx: Int, my: Int) {
        val isExpanded = expandedModules.contains(mod)
        val isHovered = rectHit(rx.roundToInt(), ry.roundToInt(), rw.roundToInt(), rh.roundToInt(), mx, my) &&
            (!isExpanded || (settingsAnimations[mod] ?: 0f) <= 0.01f || my <= ry.roundToInt() + rh.roundToInt())
        val enabled = mod.isEnabled

        if (isHovered) hoveredModule = mod

        val bgColor: Int
        val textColor: Int
        if (enabled) {
            bgColor = if (isHovered) 0x663C78F5.toInt() else 0x4D3C78F5.toInt()
            textColor = -0x1
        } else {
            bgColor = if (isHovered) 0x66282832.toInt() else 0x4D282832.toInt()
            textColor = 0xFFB4B4BE.toInt()
        }
        drawRectWithBlur(rx, ry, rw, rh, px(5f), bgColor, 2f)
        r.drawText(Fonts.REGULAR, mod.name, rx + px(8f), ry + (rh - px(15f)) / 2f, px(15f), textColor)

        val hasSettings = mod.settings.isNotEmpty()
        if (hasSettings) {
            val arrow = if (isExpanded) "\u25bc" else "\u25b6"
            val aw = r.getTextWidth(Fonts.REGULAR, arrow, px(12f))
            r.drawText(Fonts.REGULAR, arrow, rx + rw - aw - px(8f), ry + (rh - px(12f)) / 2f, px(12f), textColor)
        }
    }

    private fun renderModuleSettings(mod: Module, rx: Float, ry: Float, rw: Float, rh: Int, alpha: Float, mx: Int, my: Int) {
        if (rh <= 0) return
        val mixWeight = (77 * alpha).toInt().coerceIn(0, 77)
        drawRectWithBlur(rx, ry.toFloat(), rw, rh.toFloat(), px(5f), rgba(35, 35, 45, mixWeight), 2f, 12)
        if (alpha < 0.3f) return

        val sets = mod.settings
        if (sets.isEmpty()) {
            r.drawText(Fonts.REGULAR, locale.get("gui.no_settings"), rx + px(10f), ry + px(15f), px(13f),
                rgba(150, 150, 160, (255 * alpha).toInt()))
            return
        }

        var sy = ry + px(8f)
        val sp = px(SET_SP_PX)
        for (s in sets) {
            if (sy > ry + rh - px(25f)) break
            if (!s.isVisible()) { sy += sp; continue }

            when (s) {
                is BooleanSetting -> {
                    r.drawText(Fonts.REGULAR, s.name, rx + px(10f), sy + px(8f), px(12f), rgba(200, 200, 210, (255 * alpha).toInt()))
                    val value = if (s.isEnabled) "On" else "Off"
                    val vw = r.getTextWidth(Fonts.REGULAR, value, px(12f))
                    r.drawText(Fonts.REGULAR, value, rx + rw - vw - px(10f), sy + px(8f), px(12f),
                        if (s.isEnabled) rgba(80, 200, 120, (255 * alpha).toInt()) else rgba(180, 180, 190, (255 * alpha).toInt()))
                }
                is SelectSetting -> {
                    r.drawText(Fonts.REGULAR, s.name, rx + px(10f), sy + px(8f), px(12f), rgba(200, 200, 210, (255 * alpha).toInt()))
                    val value = s.get()
                    val vw = r.getTextWidth(Fonts.REGULAR, value, px(12f))
                    r.drawText(Fonts.REGULAR, value, rx + rw - vw - px(10f), sy + px(8f), px(12f), rgba(100, 150, 255, (255 * alpha).toInt()))
                }
                is SliderSetting -> {
                    val frac = ((s.get() - s.min) / (s.max - s.min)).toFloat().coerceIn(0f, 1f)
                    r.drawText(Fonts.REGULAR, "${s.name}: ${String.format("%.1f", s.get())}", rx + px(10f), sy + px(8f), px(12f), rgba(200, 200, 210, (255 * alpha).toInt()))
                    val barX = rx + px(10f); val barY = sy + px(24f); val barW = rw - px(20f); val barH = px(4f)
                    drawRectWithBlur(barX, barY, barW, barH, px(2f), rgba(60, 60, 70, (200 * alpha).toInt()), 1f)
                    if (frac > 0f) drawRectWithBlur(barX, barY, barW * frac, barH, px(2f), rgba(80, 160, 255, (255 * alpha).toInt()), 1f)
                }
                is MultiSelectSetting -> {
                    val arrow = if (expandedMultiSettings[mod]?.contains(s) == true) "\u25bc" else "\u25b6"
                    r.drawText(Fonts.REGULAR, "$arrow ${s.name}", rx + px(10f), sy + px(8f), px(12f), rgba(200, 200, 210, (255 * alpha).toInt()))
                    val sel = s.getSelected()
                    val summary = if (sel.isEmpty()) "-" else sel.joinToString(", ")
                    val truncated = if (summary.length > 12) summary.take(10) + ".." else summary
                    val vw = r.getTextWidth(Fonts.REGULAR, truncated, px(12f))
                    r.drawText(Fonts.REGULAR, truncated, rx + rw - vw - px(10f), sy + px(8f), px(12f), rgba(180, 180, 190, (255 * alpha).toInt()))
                    // draw expanded sub-options
                    if (expandedMultiSettings[mod]?.contains(s) == true) {
                        val optCount = s.getOptions().size
                        val optsH = optCount * px(14f)
                        val optBgH = optsH + px(4f)
                        drawRectWithBlur(rx, sy + sp, rw, optBgH, px(4f), rgba(30, 30, 40, mixWeight), 1f, 12)
                        var oy = sy + sp + px(2f)
                        for (opt in s.getOptions()) {
                            val optSel = s.isSelected(opt)
                            val optOver = rectHit(rx.roundToInt(), oy.roundToInt(), rw.roundToInt(), px(14f).roundToInt(), mx, my)
                            if (optOver) drawRectWithBlur(rx + px(4f), oy, rw - px(8f), px(14f), px(3f), rgba(60, 60, 80, (100 * alpha).toInt()), 1f)
                            r.drawText(Fonts.REGULAR, if (optSel) "\u2713 $opt" else "  $opt", rx + px(14f), oy + px(3f), px(12f),
                                if (optSel) rgba(200, 230, 255, (255 * alpha).toInt()) else rgba(160, 160, 170, (255 * alpha).toInt()))
                            oy += px(14f)
                        }
                    }
                }
                is TextSetting -> {
                    r.drawText(Fonts.REGULAR, s.name, rx + px(10f), sy + px(8f), px(12f), rgba(200, 200, 210, (255 * alpha).toInt()))
                    val value = s.get()
                    val truncated = if (value.length > 15) value.take(12) + "..." else value
                    val vw = r.getTextWidth(Fonts.REGULAR, truncated, px(12f))
                    r.drawText(Fonts.REGULAR, truncated, rx + rw - vw - px(10f), sy + px(8f), px(12f), rgba(180, 180, 190, (255 * alpha).toInt()))
                }
                is ColorSetting -> {
                    r.drawText(Fonts.REGULAR, s.name, rx + px(10f), sy + px(8f), px(12f), rgba(200, 200, 210, (255 * alpha).toInt()))
                    drawRectWithBlur(rx + rw - px(20f), sy + px(4f), px(16f), px(16f), px(3f), s.get(), 1f)
                }
                is ButtonSetting -> {
                    r.drawText(Fonts.REGULAR, s.name, rx + px(10f), sy + px(8f), px(12f), rgba(100, 200, 255, (255 * alpha).toInt()))
                }
            }
            sy += sp
        }
    }

    private fun calcSettingsHeight(mod: Module): Int {
        val sets = mod.settings
        if (sets.isEmpty()) return px(SET_PAD_PX).roundToInt()
        var h = px(SET_PAD_PX) + sets.size * px(SET_SP_PX)
        val mset = expandedMultiSettings[mod] ?: return h.roundToInt()
        for (ms in mset) {
            val opts = ms.getOptions().size
            if (opts > 0) h += opts * px(14f)
        }
        return h.roundToInt()
    }

    // ── Animations ──────────────────────────────────────────────

    private fun updateAnimations() {
        for (mod in ModuleManager.getAll()) {
            val cur = settingsAnimations[mod] ?: 0f
            val target = if (expandedModules.contains(mod)) 1f else 0f
            if (abs(cur - target) > 0.01f) {
                settingsAnimations[mod] = cur + (target - cur) * ANIM_SPEED
            } else {
                settingsAnimations[mod] = target
            }
        }
    }

    // ── Input ───────────────────────────────────────────────────

    override fun mouseClicked(e: MouseButtonEvent, bl: Boolean): Boolean {
        val mx = e.x().toInt(); val my = e.y().toInt(); val btn = e.button()
        val shift = GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                    GLFW.glfwGetKey(mc.window.handle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS

        val panelW = px(CAT_W_PX).roundToInt(); val headerH = px(HEADER_H_PX).roundToInt()

        if (btn == 0) {
            for (panel in categoryPanels.values) {
                if (rectHit(panel.x, panel.y, panelW, headerH, mx, my)) {
                    draggingPanel = panel
                    dragOffX = mx - panel.x.toFloat()
                    dragOffY = my - panel.y.toFloat()
                    return true
                }
            }

            for (panel in categoryPanels.values) {
                for (mod in ModuleManager.getByCategory(panel.category)) {
                    if (handleSettingClick(mod, mx, my)) return true
                }
            }

            for (panel in categoryPanels.values) {
                val mod = panel.getHoveredModule(mx, my) ?: continue
                if (shift) { keybindTarget = mod }
                else mod.toggle()
                return true
            }
        } else if (btn == 1) {
            for (panel in categoryPanels.values) {
                val mod = panel.getHoveredModule(mx, my) ?: continue
                if (expandedModules.contains(mod)) expandedModules.remove(mod)
                else expandedModules.add(mod)
                return true
            }
        }
        return super.mouseClicked(e, bl)
    }

    override fun mouseReleased(e: MouseButtonEvent): Boolean {
        if (e.button() == 0) {
            if (draggingPanel != null) {
                draggingPanel = null
                savePanelPositions()
                return true
            }
            draggedSlider = null
        }
        return super.mouseReleased(e)
    }

    override fun mouseDragged(e: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        val mx = e.x().toFloat(); val my = e.y().toFloat()
        if (draggingPanel != null && e.button() == 0) {
            draggingPanel!!.setPosition((mx - dragOffX).roundToInt(), (my - dragOffY).roundToInt())
            return true
        }
        val drag = draggedSlider
        if (drag != null && e.button() == 0) {
            val (mod, s) = drag
            val panel = categoryPanels[mod.category] ?: return false
            val headerH = px(HEADER_H_PX).roundToInt()
            val panelW = px(CAT_W_PX).roundToInt()
            val modH = px(MOD_H_PX).roundToInt()
            val modSp = px(MOD_SP_PX).roundToInt()
            val inset = px(5f).roundToInt()
            val scroll = (panelScroll[mod.category] ?: 0f).roundToInt()
            val modules = ModuleManager.getByCategory(mod.category)
            val moduleX = panel.x + inset
            val moduleW = panelW - px(10f).roundToInt()
            val sliderX = moduleX + px(10f).roundToInt()
            val sliderW = moduleW - px(20f).roundToInt()
            var moduleY = panel.y + headerH + inset + scroll
            for (m in modules) {
                if (m == mod) {
                    val setsY = moduleY + modH
                    val setY = setsY + px(10f).roundToInt()
                    var curY = setY
                    for (st in mod.settings) {
                        if (st == s) {
                            val frac = ((mx.toInt() - sliderX).toFloat() / sliderW).coerceIn(0f, 1f)
                            val v = s.min + (s.max - s.min) * frac
                            s.setValue((kotlin.math.round(v / s.step) * s.step).coerceIn(s.min, s.max))
                            return true
                        }
                        curY += px(SET_SP_PX).roundToInt()
                    }
                    return true
                }
                val prog = settingsAnimations[m] ?: 0f
                val mh = calcSettingsHeight(m)
                moduleY += modH + modSp + (mh * prog).toInt()
            }
        }
        return super.mouseDragged(e, dx, dy)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val mx = mouseX.toInt(); val my = mouseY.toInt()
        val panelW = px(CAT_W_PX).roundToInt(); val panelH = px(PANEL_H_PX).roundToInt()
        for ((cat, panel) in categoryPanels) {
            if (mx in panel.x..(panel.x + panelW) && my in panel.y..(panel.y + panelH)) {
                val cur = panelScroll[cat] ?: 0f
                panelScroll[cat] = cur + (scrollY * px(24f)).toFloat()
                return true
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun keyPressed(e: KeyEvent): Boolean {
        if (keybindTarget != null) {
            if (e.isEscape) { keybindTarget!!.keybind = -1; keybindTarget = null }
            else { keybindTarget!!.keybind = e.scancode(); keybindTarget = null }
            return true
        }
        if (e.isEscape) { onClose(); return true }
        return super.keyPressed(e)
    }

    // ── Setting click handler ───────────────────────────────────

    private fun handleSettingClick(mod: Module, mx: Int, my: Int): Boolean {
        if (!expandedModules.contains(mod)) return false
        val settingsProgress = settingsAnimations[mod] ?: 0f
        if (settingsProgress < 0.3f) return false

        val panel = categoryPanels[mod.category] ?: return false
        val modules = ModuleManager.getByCategory(mod.category)

        val headerH = px(HEADER_H_PX).roundToInt()
        val panelW = px(CAT_W_PX).roundToInt()
        val modH = px(MOD_H_PX).roundToInt()
        val modSp = px(MOD_SP_PX).roundToInt()
        val sp = px(SET_SP_PX).roundToInt()
        val inset = px(5f).roundToInt()
        val scroll = (panelScroll[mod.category] ?: 0f).roundToInt()

        var moduleY = panel.y + headerH + inset + scroll
        val moduleX = panel.x + inset
        val moduleW = panelW - px(10f).roundToInt()

        for (m in modules) {
            if (m == mod) {
                val setsY = moduleY + modH
                val maxH = calcSettingsHeight(mod)
                val setsH = (maxH * settingsProgress).toInt()

                if (!rectHit(moduleX, setsY, moduleW, setsH, mx, my)) return false

                var setY = setsY + px(10f).roundToInt()
                for (s in mod.settings) {
                    if (setY > setsY + setsH - px(25f).roundToInt()) break
                    if (!s.isVisible()) { setY += sp; continue }

                    val extraH = if (s is MultiSelectSetting && expandedMultiSettings[mod]?.contains(s) == true)
                        s.getOptions().size * px(14f).roundToInt() else 0
                    if (rectHit(moduleX, setY, moduleW, sp + extraH, mx, my)) {
                        when (s) {
                            is BooleanSetting -> { s.toggle(); return true }
                            is SelectSetting -> {
                                val opts = s.getOptions()
                                if (opts.isNotEmpty()) s.setSelectedIndex((s.getSelectedIndex() + 1) % opts.size)
                                return true
                            }
                            is MultiSelectSetting -> {
                                val expanded = expandedMultiSettings.getOrPut(mod) { mutableSetOf() }
                                if (expanded.contains(s)) {
                                    var oy = setY + sp
                                    for (opt in s.getOptions()) {
                                        if (rectHit(moduleX, oy, moduleW, px(14f).roundToInt(), mx, my)) {
                                            s.toggle(opt); return true
                                        }
                                        oy += px(14f).roundToInt()
                                    }
                                    expanded.remove(s)
                                } else {
                                    expanded.add(s)
                                }
                                return true
                            }
                            is ButtonSetting -> { s.click(); return true }
                            is SliderSetting -> {
                                draggedSlider = mod to s
                                val frac = ((mx - (moduleX + px(10f).roundToInt())).toFloat() / (moduleW - px(20f).roundToInt())).coerceIn(0f, 1f)
                                val v = s.min + (s.max - s.min) * frac
                                s.setValue((kotlin.math.round(v / s.step) * s.step).coerceIn(s.min, s.max))
                                return true
                            }
                        }
                        return true
                    }

                    setY += sp + extraH
                }
                return true
            }

            val progress = settingsAnimations[m] ?: 0f
            val maxH = calcSettingsHeight(m)
            moduleY += modH + modSp + (maxH * progress).toInt()
        }
        return false
    }

    // ── Utility ─────────────────────────────────────────────────

    private fun rectHit(rx: Int, ry: Int, rw: Int, rh: Int, mx: Int, my: Int): Boolean =
        mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh

    private fun drawRectWithBlur(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Int, blur: Float, cornerMask: Int = 15) {
        r.drawRectBlurred(x, y, w, h, radius, color, blur, cornerMask, useBasicBlur)
    }

    private fun rgba(r: Int, g: Int, b: Int, a: Int): Int =
        (a shl 24) or (r shl 16) or (g shl 8) or b

    private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()

    // ── Panel save / load ───────────────────────────────────────

    private fun savePanelPositions() {
        try {
            val data = mutableMapOf<String, Map<String, Int>>()
            for ((cat, panel) in categoryPanels) {
                data[cat.name] = mapOf("x" to (panel.x * sc).roundToInt(), "y" to (panel.y * sc).roundToInt())
            }
            FilesManager.writeJson(PANEL_FILE, data)
        } catch (_: Exception) {}
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadPanelPositions() {
        try {
            if (!FilesManager.exists(PANEL_FILE)) return
            val raw = FilesManager.readJson(PANEL_FILE, Map::class.java) as? Map<String, Map<String, Any>> ?: return
            for ((catName, pos) in raw) {
                val cat = try { Category.valueOf(catName) } catch (_: Exception) { continue }
                val pxVal = (pos["x"] as? Number)?.toFloat()?.let { it / sc } ?: continue
                val pyVal = (pos["y"] as? Number)?.toFloat()?.let { it / sc } ?: continue
                categoryPanels[cat]?.setPosition(pxVal.roundToInt(), pyVal.roundToInt())
            }
        } catch (_: Exception) {}
    }

    override fun isPauseScreen() = false
    override fun removed() {}
    override fun shouldCloseOnEsc() = true

    // ── CategoryPanel inner class ───────────────────────────────

    inner class CategoryPanel(val category: Category) {
        var x: Int = 0
            private set
        var y: Int = 0
            private set

        constructor(category: Category, x: Int, y: Int) : this(category) {
            this.x = x; this.y = y
        }

        fun setPosition(x: Float, y: Float) { this.x = x.roundToInt(); this.y = y.roundToInt() }
        fun setPosition(ix: Int, iy: Int) { this.x = ix; this.y = iy }

        fun getHoveredModule(mx: Int, my: Int): Module? {
            val panelW = px(CAT_W_PX).roundToInt(); val panelH = px(PANEL_H_PX).roundToInt()
            if (mx < x || mx > x + panelW || my < y || my > y + panelH) return null
            val modules = ModuleManager.getByCategory(category)
            val headerH = px(HEADER_H_PX).roundToInt(); val modH = px(MOD_H_PX).roundToInt(); val modSp = px(MOD_SP_PX).roundToInt()
            val inset = px(5f).roundToInt()
            val scroll = (panelScroll[category] ?: 0f).roundToInt()
            var moduleY = y + headerH + inset + scroll
            val moduleX = x + inset; val moduleW = panelW - px(10f).roundToInt()
            for (mod in modules) {
                if (rectHit(moduleX, moduleY, moduleW, modH, mx, my)) return mod
                val prog = settingsAnimations[mod] ?: 0f
                val mh = calcSettingsHeight(mod)
                moduleY += modH + modSp + (mh * prog).toInt()
            }
            return null
        }
    }
}
