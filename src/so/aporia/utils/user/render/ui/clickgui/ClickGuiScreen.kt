package so.aporia.utils.user.render.ui.clickgui

import so.aporia.utils.imports.*
import so.aporia.utils.user.render.core.AporiaRenderer
import aporia.webview.WebviewScreen
import com.chaos.annotation.Obfuscate
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.ModuleManager
import so.aporia.module.impl.misc.ClickGui
import so.aporia.module.impl.misc.DiscordRPCModule
import so.aporia.module.settings.*

import so.aporia.utils.user.render.font.Fonts
import so.aporia.utils.user.render.theme.ThemeManager
import so.aporia.utils.user.render.theme.ThemeManager.Theme
import java.nio.file.Files

@Obfuscate
@OnlyIn(Dist.CLIENT)
class ClickGuiScreen(private val clickGui: ClickGui) : Screen(Component.literal("ClickGui")) {

    private enum class Tab(val localeKey: String) {
        AVATAR("gui.tab.avatar"),
        QUESTS("gui.tab.quests"),
        BROWSER("gui.tab.browser"),
        SETTINGS("gui.tab.settings");

        val label: String get() = locale.get(localeKey) ?: name
    }

    private var activeTab = Tab.AVATAR
    private var selCategory = 0
    private var selectedModule: Module? = null
    private var scrollY = 0f
    private var settingsScrollY = 0f
    private var bindSet: BindSetting? = null
    private var bindMod: Module? = null
    private var dragSlider: DragInfo? = null
    private var dropSetting: Setting<*>? = null
    private var expandedCategories = mutableMapOf<String, Boolean>()
    private var dragColorSetting = false
    private var dragColorHue = false
    private var dragColorAlpha = false

    private var minimized = false
    private var maximized = false
    private var windowX = 0f
    private var windowY = 0f
    private var windowDragX = 0f
    private var windowDragY = 0f
    private var draggingWindow = false

    private var bgImage: Identifier? = null
    private val catExpanded = mutableMapOf<String, Boolean>()

    companion object {
        const val TOP_BAR_H = 24f
        const val CORNER_RADIUS = 2f
        const val PAD = 6f
        const val CAT_W_RATIO = 0.18f
        const val MOD_W_RATIO = 0.37f
        const val CAT_H = 22f
        const val MOD_H = 18f
        const val SET_H = 14f
        const val CAT_TITLE_H = 18f
        const val DH = 9.8f
        const val TAB_PAD = 4f
        const val AVATAR_SIZE = 28f

        @JvmStatic
        fun settingCategory(name: String, hierarchy: Int) {
            SettingCategoryManager.push(name, hierarchy)
        }

        @JvmStatic
        fun settingCategoryEnd() {
            SettingCategoryManager.pop()
        }
    }

    private val panelW get() = if (maximized) width.toFloat() else (width * 0.72f).coerceIn(500f, 900f)
    private val panelH get() = if (maximized) height.toFloat() else (height * 0.72f).coerceIn(360f, 680f)
    private val px get() = if (maximized) 0f else windowX
    private val py get() = if (maximized) 0f else windowY
    private val contentY get() = py + TOP_BAR_H + PAD
    private val catW get() = panelW * CAT_W_RATIO
    private val modW get() = panelW * MOD_W_RATIO
    private val setW get() = panelW - catW - modW

    override fun init() {
        val sw = width.toFloat()
        val sh = height.toFloat()
        val pw = (sw * 0.72f).coerceIn(500f, 900f)
        windowX = (sw - pw) / 2f
        windowY = (sh - (sh * 0.72f).coerceIn(360f, 680f)) / 2f

        val bgPath = files.ROOT.resolve("custom/clickgui.png")
        if (Files.exists(bgPath)) bgImage = r.loadImage(bgPath)
    }

    override fun render(g: GuiGraphics, mx: Int, my: Int, d: Float) {
        val th = theme ?: return
        val fmx = mx.toFloat()
        val fmy = my.toFloat()

        drawWindow(r, th)
        drawTopBar(r, th, fmx, fmy)
        drawContent(r, th, fmx, fmy)

        r.flush()

        val bm = bindMod ?: return
        val hint = locale.get("gui.presskey") ?: "Press any key..."
        val hs = 12f
        val hw = r.getTextWidth(Fonts.BOLD, hint, hs)
        val hx = (width - hw) / 2f
        r.drawRectBlurred(hx - 6f, 5f, hw + 12f, hs + 4f, 5f, colorUtil.rgba(0, 0, 0, 160))
        r.drawText(Fonts.BOLD, hint, hx, 6f, hs, 0xFFFFAA00.toInt())
        r.flush()
    }

    private fun drawWindow(r: AporiaRenderer, th: Theme) {
        val bg = bgImage
        if (bg != null) r.drawImage(px, py, panelW, panelH, bg, CORNER_RADIUS)
        else r.drawRectBlurred(px, py, panelW, panelH, CORNER_RADIUS, th.guiBackground, 3f)
    }

    private fun drawTopBar(r: AporiaRenderer, th: Theme, mx: Float, my: Float) {
        val bx = px
        val by = py
        val bw = panelW

        r.drawRect(bx, by, bw, TOP_BAR_H, 0f, colorUtil.rgba(0, 0, 0, 60))
        r.drawRect(bx, by + TOP_BAR_H - 1f, bw, 1f, 0f, th.guiSeparator)

        val leftTabs = listOf(Tab.BROWSER, Tab.SETTINGS)

        var tabX = bx + PAD
        for (tab in leftTabs) {
            val tw = r.getTextWidth(Fonts.BOLD, tab.label, 10f) + 16f
            val over = mx >= tabX && mx < tabX + tw && my >= by && my < by + TOP_BAR_H
            val active = tab == activeTab
            if (over || active) {
                r.drawRectBlurred(tabX, by + 2f, tw, TOP_BAR_H - 4f, 3f,
                    if (active) th.guiTitleBg else colorUtil.rgba(255, 255, 255, 12))
            }
            r.drawText(Fonts.BOLD, tab.label, tabX + 8f, by + (TOP_BAR_H - 10f) / 2f - 1f, 10f,
                if (active) 0xFFFFFFFF.toInt() else th.guiDisabledDot)
            tabX += tw + TAB_PAD
        }

        val ctrlY = by + (TOP_BAR_H - 12f) / 2f
        val ctrlR = 6f
        val closeX = bx + bw - PAD - ctrlR * 2 - 4f
        val maxX = closeX - ctrlR * 2 - 6f
        val minX = maxX - ctrlR * 2 - 6f

        var rightX = minX - TAB_PAD
        val avatarH = TOP_BAR_H - 6f
        val avatarS = avatarH - 2f
        val questW = r.getTextWidth(Fonts.BOLD, Tab.QUESTS.label, 10f) + 12f
        val combinedW = avatarH + 4f + questW + 4f
        rightX -= combinedW

        val isAvOrQs = activeTab == Tab.AVATAR || activeTab == Tab.QUESTS
        val overCombined = mx >= rightX && mx < rightX + combinedW && my >= by && my < by + TOP_BAR_H
        r.drawRectBlurred(rightX, by + 2f, combinedW, TOP_BAR_H - 4f, 3f,
            if (isAvOrQs) th.guiTitleBg else colorUtil.rgba(255, 255, 255, 12))

        val skinId = mc.player?.skin?.body?.texturePath()
        val ay = by + (TOP_BAR_H - avatarS) / 2f
        if (skinId != null && activeTab == Tab.QUESTS) {
            // Full-size skin render for QUESTS tab button
            r.drawImageCropped(rightX + 2f, by + 2f, combinedW - 4f, TOP_BAR_H - 4f,
                skinId, 0f, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f)
            r.drawRectBlurred(rightX, by + 2f, combinedW, TOP_BAR_H - 4f, 3f,
                colorUtil.rgba(0, 0, 0, 80))
        } else if (skinId != null) {
            r.drawImageCropped(rightX + 2f, ay, avatarS, avatarS, skinId, avatarS / 2f,
                8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f)
        } else {
            r.drawCircle(rightX + avatarS / 2f + 2f, ay + avatarS / 2f, avatarS / 2f,
                if (isAvOrQs) th.guiTitleBg else colorUtil.rgba(255, 255, 255, 40))
        }
        r.drawText(Fonts.BOLD, Tab.QUESTS.label, rightX + avatarH + 6f, by + (TOP_BAR_H - 10f) / 2f - 1f, 10f,
            if (isAvOrQs) 0xFFFFFFFF.toInt() else th.guiDisabledDot)

        val closeOver = mx >= closeX && mx < closeX + ctrlR * 2 + 4f && my >= ctrlY && my < ctrlY + ctrlR * 2 + 4f
        r.drawCircle(closeX + ctrlR, ctrlY + ctrlR, ctrlR, if (closeOver) 0xFFFF5555.toInt() else 0xFFFF6666.toInt())
        if (closeOver) {
            r.drawLine(closeX + ctrlR - 2f, ctrlY + ctrlR - 2f, closeX + ctrlR + 2f, ctrlY + ctrlR + 2f, 1.5f, 0xFFFFFFFF.toInt())
            r.drawLine(closeX + ctrlR + 2f, ctrlY + ctrlR - 2f, closeX + ctrlR - 2f, ctrlY + ctrlR + 2f, 1.5f, 0xFFFFFFFF.toInt())
        }

        val maxOver = mx >= maxX && mx < maxX + ctrlR * 2 + 4f && my >= ctrlY && my < ctrlY + ctrlR * 2 + 4f
        r.drawCircle(maxX + ctrlR, ctrlY + ctrlR, ctrlR, if (maxOver) 0xFFFFDD55.toInt() else 0xFFFFDD66.toInt())
        if (maxOver) {
            r.drawStroke(maxX + 2f, ctrlY + 2f, ctrlR * 2 - 4f, ctrlR * 2 - 4f, 1f, 1.2f, 0, 0f, 0xFFFFFFFF.toInt())
        }

        val minOver = mx >= minX && mx < minX + ctrlR * 2 + 4f && my >= ctrlY && my < ctrlY + ctrlR * 2 + 4f
        r.drawCircle(minX + ctrlR, ctrlY + ctrlR, ctrlR, if (minOver) 0xFF66DD66.toInt() else 0xFF66EE66.toInt())
        if (minOver) {
            r.drawLine(minX + 2f, ctrlY + ctrlR, minX + ctrlR * 2 - 2f, ctrlY + ctrlR, 1.5f, 0xFFFFFFFF.toInt())
        }
    }

    private fun drawContent(r: AporiaRenderer, th: Theme, mx: Float, my: Float) {
        when (activeTab) {
            Tab.AVATAR -> drawAvatarContent(r, th, mx, my)
            Tab.QUESTS -> drawQuestsContent(r, th)
            Tab.BROWSER -> {
                mc.setScreen(WebviewScreen("https://google.com"))
                return
            }
            Tab.SETTINGS -> drawSettingsContent(r, th, mx, my)
        }
    }

    private fun drawAvatarContent(r: AporiaRenderer, th: Theme, mx: Float, my: Float) {
        val cy = contentY
        val categories = Category.values().toList()

        var catY = cy
        for ((i, cat) in categories.withIndex()) {
            val over = mx >= px + PAD && mx < px + catW && my >= catY && my < catY + CAT_H
            val sel = i == selCategory
            if (over || sel) r.drawRectBlurred(px + PAD, catY, catW - PAD * 2, CAT_H, 3f,
                if (sel) th.guiTitleBg else colorUtil.rgba(255, 255, 255, 12))
            r.drawText(Fonts.REGULAR, cat.icon.toString(), px + PAD + 4f, catY + (CAT_H - 12f) / 2f - 1f, 12f,
                if (sel) 0xFFFFFFFF.toInt() else th.guiDisabledDot)
            val label = locale.get("category.${cat.name.lowercase()}") ?: cat.name
            r.drawText(Fonts.REGULAR, label, px + PAD + 20f, catY + (CAT_H - 10f) / 2f - 1f, 10f,
                if (sel) 0xFFFFFFFF.toInt() else th.guiSettingValue)
            if (sel) r.drawRectBlurred(px + catW - PAD - 3f, catY + 3f, 2f, CAT_H - 6f, 1f, th.guiEnabledDot)
            catY += CAT_H
        }

        val player = mc.player
        val aSize = AVATAR_SIZE
        val aX = px + PAD + 2f
        val aY = py + panelH - PAD - aSize - 4f
        val avatarBgW = catW - PAD * 2

        r.drawRectBlurred(aX - 2f, aY - 2f, avatarBgW, aSize + 4f, 4f, colorUtil.rgba(0, 0, 0, 100))
        val discord = ModuleManager.get("Discord RPC")
        val avatarTex: Identifier?
        if (discord != null && discord.isEnabled) {
            val dm = discord as? DiscordRPCModule
            dm?.loadAvatarOnRenderThread()
            avatarTex = dm?.avatarId
        } else avatarTex = null

        if (avatarTex != null) r.drawImageCropped(aX, aY, aSize, aSize, avatarTex, aSize / 2f, 0f, 0f, 1f, 1f)
        else if (player != null) {
            val skinId = player.skin.body.texturePath()
            if (skinId != null) {
                r.drawImageCropped(aX, aY, aSize, aSize, skinId, aSize / 2f, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f)
                r.drawImageCropped(aX, aY, aSize, aSize, skinId, aSize / 2f, 40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f)
            } else r.drawRectBlurred(aX, aY, aSize, aSize, aSize / 2f, th.guiSettingValue)
        } else r.drawRectBlurred(aX, aY, aSize, aSize, aSize / 2f, th.guiSettingValue)
        if (player != null) r.drawText(Fonts.BOLD, player.name.string, aX + aSize + 6f, aY + (aSize - 11f) / 2f - 1f, 11f, 0xFFFFFFFF.toInt())

        val mLeft = px + catW
        val mTop = cy
        var my2 = mTop + scrollY
        val mods = ModuleManager.getByCategory(categories[selCategory])
        for (mod in mods) {
            if (my2 + MOD_H < mTop || my2 > py + panelH - PAD) { my2 += MOD_H; continue }
            val over = mx >= mLeft && mx < mLeft + modW && my >= my2 && my < my2 + MOD_H
            val en = mod.isEnabled
            val selMod = mod === selectedModule
            if (over || selMod) r.drawRectBlurred(mLeft, my2, modW, MOD_H, 2f,
                if (selMod) th.guiTitleBg else colorUtil.rgba(255, 255, 255, 12))
            if (en) r.drawRectBlurred(mLeft + 2f, my2 + 2f, 2f, MOD_H - 4f, 1f, th.guiEnabledDot)
            r.drawText(Fonts.REGULAR, mod.name, mLeft + 8f, my2 + (MOD_H - 10f) / 2f - 1f, 10f,
                if (en) 0xFFFFFFFF.toInt() else th.guiDisabledDot)
            if (selMod) r.drawText(Fonts.REGULAR, "\u25C0", mLeft + modW - 12f, my2 + (MOD_H - 9f) / 2f - 1f, 9f, colorUtil.rgba(180, 180, 200, 200))
            my2 += MOD_H
        }

        val sLeft = px + catW + modW
        val sTop = cy
        val sWidth = setW - PAD
        val selMod = selectedModule
        if (selMod != null) {
            r.drawRectBlurred(sLeft, sTop, sWidth, MOD_H, 2f, th.guiTitleBg)
            r.drawText(Fonts.BOLD, selMod.name, sLeft + 4f, sTop + (MOD_H - 11f) / 2f - 1f, 11f, 0xFFFFFFFF.toInt())

            var sy3 = sTop + MOD_H + PAD + settingsScrollY
            var lastCat = ""
            for (s in getSettings(selMod)) {
                if (s.category != lastCat) {
                    lastCat = s.category
                    if (lastCat.isNotEmpty()) {
                        val sh = CAT_TITLE_H
                        if (sy3 + sh < sTop || sy3 > py + panelH - PAD) { sy3 += sh; continue }
                        val exp = catExpanded.getOrPut(lastCat) { true }
                        val over = mx >= sLeft && mx < sLeft + sWidth && my >= sy3 && my < sy3 + sh
                        if (over) r.drawRectBlurred(sLeft, sy3, sWidth, sh, 0f, colorUtil.rgba(255, 255, 255, 10))
                        r.drawText(Fonts.BOLD, "${if (exp) "\u25BC" else "\u25B6"} $lastCat",
                            sLeft + 8f, sy3 + (sh - 10f) / 2f - 1f, 10f, th.guiSettingText)
                        sy3 += sh
                        if (!exp) continue
                    }
                }
                val sh = settingHeight(s)
                if (sy3 + sh < sTop || sy3 > py + panelH - PAD) { sy3 += sh; continue }
                val sov = mx >= sLeft && mx < sLeft + sWidth && my >= sy3 && my < sy3 + sh
                if (sov) r.drawRectBlurred(sLeft, sy3, sWidth, sh, 0f, colorUtil.rgba(255, 255, 255, 6))
                drawSetting(r, s, sy3, th, sLeft, sWidth, sov, mx, my)
                sy3 += sh
            }
        }
    }

    private fun drawQuestsContent(r: AporiaRenderer, th: Theme) {
        val cy = contentY
        val label = locale.get("gui.quests.coming_soon") ?: "Quests \u2014 Coming Soon"
        r.drawText(Fonts.BOLD, label, px + panelW / 2f - r.getTextWidth(Fonts.BOLD, label, 14f) / 2f,
            cy + 20f, 14f, th.guiSettingText)
    }

    private fun drawSettingsContent(r: AporiaRenderer, th: Theme, mx: Float, my: Float) {
        val cy = contentY
        val sWidth = panelW - PAD * 2

        r.drawText(Fonts.BOLD, locale.get("gui.tab.settings_title") ?: "ClickGui Settings", px + PAD, cy, 12f, 0xFFFFFFFF.toInt())

        var sy = cy + 20f
        for (s in getSettings(clickGui)) {
            val sh = settingHeight(s)
            val sov = mx >= px + PAD && mx < px + PAD + sWidth && my >= sy && my < sy + sh
            if (sov) r.drawRectBlurred(px + PAD, sy, sWidth, sh, 2f, colorUtil.rgba(255, 255, 255, 6))
            drawSetting(r, s, sy, th, px + PAD, sWidth, sov, mx, my)
            sy += sh
        }
    }

    private fun getSettings(m: Module): List<Setting<*>> {
        val list = mutableListOf<Setting<*>>()
        for (f in m.javaClass.declaredFields) {
            if (!Setting::class.java.isAssignableFrom(f.type)) continue
            f.isAccessible = true
            val s = f.get(m) as? Setting<*> ?: continue
            if (s.category.isEmpty()) {
                try {
                    val ann = f.getAnnotation(SettingCategory::class.java)
                    if (ann != null) {
                        val catF = Setting::class.java.getDeclaredField("category")
                        catF.isAccessible = true; catF.set(s, ann.name)
                        val hierF = Setting::class.java.getDeclaredField("hierarchy")
                        hierF.isAccessible = true; hierF.set(s, ann.hierarchy)
                    }
                } catch (_: Exception) {}
            }
            list.add(s)
        }
        return list
    }

    private fun settingHeight(s: Setting<*>): Float = when {
        s == dropSetting && (s is SelectSetting || s is MultiSelectSetting) -> {
            val opts = if (s is SelectSetting) s.getOptions() else (s as MultiSelectSetting).getOptions()
            SET_H + 4f + opts.size * DH + 3f
        }
        s == dropSetting && s is ColorSetting -> SET_H + 50f
        s is NumberSetting || s is RangeSetting -> SET_H + 8f
        else -> SET_H
    }

    private fun drawSetting(r: AporiaRenderer, s: Setting<*>, y: Float, th: Theme, sx: Float, sw: Float, sov: Boolean, mx: Float, my: Float) {
        val pw = sw - 14f
        when (s) {
            is BooleanSetting -> {
                val vt = if (s.isEnabled) locale.get("gui.on") ?: "ON" else locale.get("gui.off") ?: "OFF"
                val vc = if (s.isEnabled) 0xFF64FF64.toInt() else th.guiSettingValue
                val vw = r.getTextWidth(Fonts.REGULAR, vt, 8f)
                r.drawText(Fonts.REGULAR, s.name, sx + 8f, y + (SET_H - 9f) / 2f - 1f, 9f, th.guiSettingText)
                r.drawText(Fonts.REGULAR, vt, sx + sw - 6f - vw, y + (SET_H - 8f) / 2f - 1f, 8f, vc)
            }
            is NumberSetting -> {
                val frac = ((s.get() - s.min) / (s.max - s.min)).toFloat().coerceIn(0f, 1f)
                r.drawText(Fonts.REGULAR, s.name, sx + 8f, y + 1f, 9f, th.guiSettingText)
                val txt = String.format("%.1f", s.get())
                val tw = r.getTextWidth(Fonts.REGULAR, txt, 7f)
                val by = y + SET_H / 2f + 4f
                r.drawRectBlurred(sx + 10f, by, pw, 2f, 1f, colorUtil.rgba(255, 255, 255, 30))
                r.drawRectBlurred(sx + 10f, by, pw * frac, 2f, 1f, th.guiSettingValue)
                r.drawText(Fonts.REGULAR, txt, sx + 10f + pw * frac - tw / 2f, by + 3f, 7f, th.guiSettingValue)
            }
            is RangeSetting -> {
                val frac = ((s.get() - s.min) / (s.max - s.min)).toFloat().coerceIn(0f, 1f)
                r.drawText(Fonts.REGULAR, s.name, sx + 8f, y + 1f, 9f, th.guiSettingText)
                val txt = String.format("%.1f", s.get())
                val tw = r.getTextWidth(Fonts.REGULAR, txt, 7f)
                val by = y + SET_H / 2f + 4f
                r.drawRectBlurred(sx + 10f, by, pw, 2f, 1f, colorUtil.rgba(255, 255, 255, 30))
                r.drawRectBlurred(sx + 10f, by, pw * frac, 2f, 1f, th.guiSettingValue)
                r.drawText(Fonts.REGULAR, txt, sx + 10f + pw * frac - tw / 2f, by + 3f, 7f, th.guiSettingValue)
            }
            is SelectSetting -> {
                val opts = s.getOptions()
                r.drawText(Fonts.REGULAR, s.name, sx + 8f, y + (SET_H - 9f) / 2f - 1f, 9f, th.guiSettingText)
                val disp = s.get() + " \u25BC"
                val dw = r.getTextWidth(Fonts.REGULAR, disp, 8f)
                r.drawText(Fonts.REGULAR, disp, sx + sw - 8f - dw, y + (SET_H - 8f) / 2f - 1f, 8f, th.guiSettingValue)
                if (s == dropSetting) {
                    val dy = y + SET_H + 2f
                    for (oi in opts.indices) {
                        val oy = dy + oi * DH
                        val sel = s.getSelectedIndex() == oi
                        val oh = mx >= sx + 8f && mx < sx + sw - 8f && my >= oy && my < oy + DH
                        if (oh) r.drawRectBlurred(sx + 8f, oy, pw, DH, 0f, colorUtil.rgba(255, 255, 255, 20))
                        r.drawText(Fonts.REGULAR, opts[oi], sx + 12f, oy + (DH - 8f) / 2f - 1f, 8f, if (sel) 0xFFFFFFFF.toInt() else th.guiSettingValue)
                        val dx = sx + sw - 16f
                        val dy2 = oy + DH / 2f
                        r.drawCircle(dx, dy2, 3f, if (sel) th.guiSettingValue else colorUtil.rgba(255, 255, 255, 60))
                        if (sel) r.drawCircle(dx, dy2, 1.5f, 0xFFFFFFFF.toInt())
                    }
                }
            }
            is MultiSelectSetting -> {
                val opts = s.getOptions()
                r.drawText(Fonts.REGULAR, s.name, sx + 8f, y + (SET_H - 9f) / 2f - 1f, 9f, th.guiSettingText)
                val sum = s.getSelected().joinToString(", ").ifEmpty { "-" }
                val disp = sum + " \u25BC"
                val dw = r.getTextWidth(Fonts.REGULAR, disp, 8f)
                r.drawText(Fonts.REGULAR, disp, sx + sw - 8f - dw, y + (SET_H - 8f) / 2f - 1f, 8f, th.guiSettingValue)
                if (s == dropSetting) {
                    val dy = y + SET_H + 2f
                    for (oi in opts.indices) {
                        val oy = dy + oi * DH
                        val sel = s.isSelected(opts[oi])
                        val oh = mx >= sx + 8f && mx < sx + sw - 8f && my >= oy && my < oy + DH
                        if (oh) r.drawRectBlurred(sx + 8f, oy, pw, DH, 0f, colorUtil.rgba(255, 255, 255, 20))
                        r.drawText(Fonts.REGULAR, opts[oi], sx + 12f, oy + (DH - 8f) / 2f - 1f, 8f, if (sel) 0xFFFFFFFF.toInt() else th.guiSettingValue)
                        val bx = sx + sw - 18f
                        val by2 = oy + DH / 2f - 2.5f
                        r.drawRectBlurred(bx, by2, 6f, 6f, 1f, if (sel) th.guiSettingValue else colorUtil.rgba(255, 255, 255, 60))
                        if (sel) {
                            r.drawLine(bx, by2 + 2.5f, bx + 1.5f, by2 + 4f, 1f, 0xFFFFFFFF.toInt())
                            r.drawLine(bx + 1.5f, by2 + 4f, bx + 4f, by2 + 1f, 1f, 0xFFFFFFFF.toInt())
                        }
                    }
                }
            }
            is BindSetting -> s.render(r, sx.toInt(), y.toInt(), sw.toInt(), s == bindSet)
            is TextSetting -> {
                val v = s.get()
                val txt = if (v.length > 10) v.substring(0, 8) + ".." else v
                val tw = r.getTextWidth(Fonts.REGULAR, txt, 8f)
                r.drawText(Fonts.REGULAR, s.name, sx + 8f, y + (SET_H - 9f) / 2f - 1f, 9f, th.guiSettingText)
                r.drawText(Fonts.REGULAR, txt, sx + sw - 8f - tw, y + (SET_H - 8f) / 2f - 1f, 8f, th.guiSettingValue)
            }
            is ButtonSetting -> {
                val bw = r.getTextWidth(Fonts.REGULAR, s.name, 8f) + 12f
                val bx = sx + sw - 8f - bw
                r.drawRectBlurred(bx, y + 1f, bw, SET_H - 3f, 2f, colorUtil.rgba(255, 255, 255, 30))
                r.drawText(Fonts.REGULAR, s.name, bx + 4f, y + (SET_H - 8f) / 2f - 1f, 8f, th.guiSettingValue)
            }
            is ColorSetting -> {
                val color = s.get()
                val previewSize = SET_H - 4f
                val previewX = sx + sw - 8f - previewSize
                val previewY = y + (SET_H - previewSize) / 2f
                r.drawRect(previewX, previewY, previewSize, previewSize, 2f, color)
                r.drawStroke(previewX, previewY, previewSize, previewSize, 2f, 1f, 0, 0f,
                    colorUtil.rgba(255, 255, 255, 60))
                r.drawText(Fonts.REGULAR, s.name, sx + 8f, y + (SET_H - 9f) / 2f - 1f, 9f, th.guiSettingText)
                val hex = String.format("#%06X", color and 0xFFFFFF)
                val hw = r.getTextWidth(Fonts.REGULAR, hex, 7f)
                r.drawText(Fonts.REGULAR, hex, previewX - 4f - hw, y + (SET_H - 7f) / 2f, 7f, th.guiSettingValue)
                if (s == dropSetting) {
                    val pickerY = y + SET_H + 4f
                    val pickerW = (sw - 16f).coerceAtMost(180f)
                    val hueBarH = 10f
                    val svSize = 80f
                    val swatchSize = 14f
                    val gap = 2f

                    // HSV picker: Sat/Val box
                    val hue = s.getHue()
                    for (sy in 0 until svSize.toInt()) {
                        for (sx2 in 0 until svSize.toInt()) {
                            val sat = sx2 / svSize
                            val val_ = 1f - sy / svSize
                            val clr = ColorSetting.fromHSV(hue, sat, val_, 255)
                            r.drawRect(sx + 8f + sx2, pickerY + sy, 1f, 1f, 0f, clr)
                        }
                    }
                    r.drawStroke(sx + 8f, pickerY, svSize, svSize, 1f, 1f, 0, 0f, colorUtil.rgba(255, 255, 255, 80))

                    // Hue bar
                    val hueBarX = sx + 8f + svSize + 6f
                    for (hi in 0 until (pickerW - svSize - 6f).toInt()) {
                        val hFrac = hi / (pickerW - svSize - 6f)
                        val hClr = ColorSetting.fromHSV(hFrac, 1f, 1f)
                        r.drawRect(hueBarX + hi, pickerY, 1f, hueBarH, 0f, hClr)
                    }
                    r.drawStroke(hueBarX, pickerY, pickerW - svSize - 6f, hueBarH, 1f, 1f, 0, 0f, colorUtil.rgba(255, 255, 255, 80))

                    // Presets row
                    val presetsY = pickerY + svSize + 4f
                    val presets = intArrayOf(
                        0xFFFF0000.toInt(), 0xFFFF8800.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt(),
                        0xFF00FFFF.toInt(), 0xFF0000FF.toInt(), 0xFF8800FF.toInt(), 0xFFFF00FF.toInt(),
                        0xFFFFFFFF.toInt(), 0xFF888888.toInt(), 0xFF000000.toInt()
                    )
                    var psX = sx + 8f
                    for (preset in presets) {
                        r.drawRect(psX, presetsY, swatchSize, swatchSize, 2f, preset)
                        r.drawStroke(psX, presetsY, swatchSize, swatchSize, 2f, 0.5f, 0, 0f,
                            colorUtil.rgba(255, 255, 255, 40))
                        psX += swatchSize + gap
                    }
                }
            }
            else -> {
                val txt = s.value?.toString() ?: ""
                val tw = r.getTextWidth(Fonts.REGULAR, txt, 8f)
                r.drawText(Fonts.REGULAR, txt, sx + sw - 8f - tw, y + (SET_H - 8f) / 2f - 1f, 8f, th.guiSettingValue)
            }
        }
    }

    override fun mouseClicked(e: MouseButtonEvent, b: Boolean): Boolean {
        val mx = e.x().toFloat()
        val my = e.y().toFloat()
        val btn = e.button()

        if (bindMod != null) {
            val vk = if (btn in 0..7) 500 + btn else btn
            bindSet?.setKey(vk); bindMod!!.keybind = vk; bindSet = null; bindMod = null
            return true
        }

        val bx = px; val by = py; val bw = panelW

        if (mx >= bx && mx < bx + bw && my >= by && my < by + TOP_BAR_H) {
            draggingWindow = true
            windowDragX = mx - px
            windowDragY = my - py
        }

        val ctrlY = by + (TOP_BAR_H - 12f) / 2f
        val ctrlR = 6f
        val closeX = bx + bw - PAD - ctrlR * 2 - 4f
        val maxX = closeX - ctrlR * 2 - 6f
        val minX = maxX - ctrlR * 2 - 6f

        if (mx >= closeX && mx < closeX + ctrlR * 2 + 4f && my >= ctrlY && my < ctrlY + ctrlR * 2 + 4f) {
            onClose(); return true
        }
        if (mx >= maxX && mx < maxX + ctrlR * 2 + 4f && my >= ctrlY && my < ctrlY + ctrlR * 2 + 4f) {
            maximized = !maximized
            if (maximized) { windowX = px; windowY = py }
            return true
        }
        if (mx >= minX && mx < minX + ctrlR * 2 + 4f && my >= ctrlY && my < ctrlY + ctrlR * 2 + 4f) {
            minimized = true
            onClose()
            return true
        }

        val leftTabs = listOf(Tab.BROWSER, Tab.SETTINGS)
        val rightTabs = listOf(Tab.AVATAR, Tab.QUESTS)

        var tabX = bx + PAD
        for (tab in leftTabs) {
            val tw = r.getTextWidth(Fonts.BOLD, tab.label, 10f) + 16f
            if (mx >= tabX && mx < tabX + tw && my >= by && my < by + TOP_BAR_H) {
                if (tab == Tab.BROWSER) {
                    mc.setScreen(WebviewScreen("https://google.com"))
                } else {
                    activeTab = tab
                    selCategory = 0; selectedModule = null; scrollY = 0f; dropSetting = null; settingsScrollY = 0f
                }
                return true
            }
            tabX += tw + TAB_PAD
        }

        val ctrlY2 = by + (TOP_BAR_H - 12f) / 2f
        val ctrlR2 = 6f
        val closeX2 = bx + bw - PAD - ctrlR2 * 2 - 4f
        val maxX2 = closeX2 - ctrlR2 * 2 - 6f
        val minX2 = maxX2 - ctrlR2 * 2 - 6f

        val avatarH2 = TOP_BAR_H - 6f
        val questW2 = r.getTextWidth(Fonts.BOLD, Tab.QUESTS.label, 10f) + 12f
        val combinedW2 = avatarH2 + 4f + questW2 + 4f
        val combinedX = minX2 - TAB_PAD - combinedW2
        if (mx >= combinedX && mx < combinedX + combinedW2 && my >= by && my < by + TOP_BAR_H) {
            activeTab = if (activeTab == Tab.AVATAR) Tab.QUESTS else Tab.AVATAR
            selCategory = 0; selectedModule = null; scrollY = 0f; dropSetting = null; settingsScrollY = 0f
            return true
        }

        if (activeTab == Tab.SETTINGS) {
            val sWidth = panelW - PAD * 2
            var sy = contentY + 20f
            for (s in getSettings(clickGui)) {
                val sh = settingHeight(s)
                if (my >= sy && my < sy + sh) {
                    when (s) {
                        is BooleanSetting -> { s.toggle(); return true }
                        is NumberSetting -> { dragSlider = DragInfo(clickGui, getSettings(clickGui).indexOf(s)); return true }
                        is RangeSetting -> { dragSlider = DragInfo(clickGui, getSettings(clickGui).indexOf(s)); return true }
                        is SelectSetting -> {
                            val opts = s.getOptions(); if (opts.isEmpty()) return true
                            if (s == dropSetting) {
                                val rel = my - sy - SET_H - 2f; val idx = (rel / DH).toInt()
                                if (idx in opts.indices) {
                                    s.setSelectedIndex(idx)
                                    dropSetting = null
                                    if (s.name() == "Font Renderer") {
                                        fonts.setMode(when (idx) {
                                            1 -> so.aporia.utils.user.render.font.FontMode.TTF
                                            2 -> so.aporia.utils.user.render.font.FontMode.OTF
                                            else -> so.aporia.utils.user.render.font.FontMode.MSDF
                                        })
                                    }
                                    if (s.name() == "Font Family") {
                                        fonts.setFamily(opts[idx])
                                    }
                                } else dropSetting = null
                                return true
                            } else { dropSetting = s; return true }
                        }
                        is ColorSetting -> {
                            if (s == dropSetting) {
                                dropSetting = null
                            } else { dropSetting = s }
                            return true
                        }
                        is ButtonSetting -> { s.click(); return true }
                        is BindSetting -> return true
                        else -> return false
                    }
                }
                sy += sh
            }
            return false
        }

        if (activeTab != Tab.AVATAR) return false

        val cy = contentY
        var catY = cy
        val categories = Category.values().toList()
        for ((i, _) in categories.withIndex()) {
            if (mx >= px + PAD && mx < px + catW && my >= catY && my < catY + CAT_H) {
                selCategory = i; selectedModule = null; scrollY = 0f; dropSetting = null; settingsScrollY = 0f; return true
            }
            catY += CAT_H
        }

        val mLeft = px + catW; val mTop = cy
        val mods = ModuleManager.getByCategory(categories[selCategory])
        var my2 = mTop + scrollY
        for (mod in mods) {
            if (my2 + MOD_H < mTop || my2 > py + panelH - PAD) { my2 += MOD_H; continue }
            if (mx >= mLeft && mx < mLeft + modW && my >= my2 && my < my2 + MOD_H) {
                when (btn) {
                    0 -> { mod.toggle() }
                    1 -> { selectedModule = if (selectedModule === mod) null else mod; dropSetting = null; settingsScrollY = 0f }
                    2 -> { val bs = getSettings(mod).find { it is BindSetting } as? BindSetting; if (bs != null) { bindSet = bs; bindMod = mod } }
                }
                return true
            }
            my2 += MOD_H
        }

        val selMod = selectedModule
        if (selMod != null) {
            val sLeft = px + catW + modW; val sTop = cy; val sWidth = setW - PAD
            var sy3 = sTop + MOD_H + PAD + settingsScrollY
            var lastCat = ""
            for (s in getSettings(selMod)) {
                if (s.category != lastCat) {
                    lastCat = s.category
                    if (lastCat.isNotEmpty()) {
                        val sh = CAT_TITLE_H
                        if (my >= sy3 && my < sy3 + sh) {
                            catExpanded[lastCat] = !catExpanded.getOrDefault(lastCat, true)
                            return true
                        }
                        sy3 += sh
                        if (!catExpanded.getOrDefault(lastCat, true)) continue
                    }
                }
                val sh = settingHeight(s)
                if (my >= sy3 && my < sy3 + sh) {
                    when (s) {
                        is BooleanSetting -> { s.toggle(); return true }
                        is BindSetting -> { bindSet = s; bindMod = selMod; return true }
                        is NumberSetting -> { dragSlider = DragInfo(selMod, getSettings(selMod).indexOf(s)); return true }
                        is RangeSetting -> { dragSlider = DragInfo(selMod, getSettings(selMod).indexOf(s)); return true }
                        is SelectSetting -> {
                            val opts = s.getOptions(); if (opts.isEmpty()) return true
                            if (s == dropSetting) {
                                val rel = my - sy3 - SET_H - 2f; val idx = (rel / DH).toInt()
                                if (idx in opts.indices) {
                                    s.setSelectedIndex(idx)
                                    dropSetting = null
                                } else dropSetting = null
                                return true
                            } else { dropSetting = s; return true }
                        }
                        is MultiSelectSetting -> {
                            val opts = s.getOptions(); if (opts.isEmpty()) return true
                            if (s == dropSetting) {
                                val rel = my - sy3 - SET_H - 2f; val idx = (rel / DH).toInt()
                                if (idx in opts.indices) s.toggle(opts[idx]); else dropSetting = null
                                return true
                            } else { dropSetting = s; return true }
                        }
                        is ButtonSetting -> { s.click(); return true }
                        is ColorSetting -> {
                            if (s == dropSetting) {
                                val sLeft = px + catW + modW
                                val sWidth = setW - PAD
                                val pickerY = sy3 + SET_H + 4f
                                val svSize = 80f
                                val svBoxX = sLeft + PAD + 8f
                                val svBoxY = pickerY
                                val hueBarX = svBoxX + svSize + 6f
                                val hueBarH = 10f

                                // SV box click
                                if (mx >= svBoxX && mx < svBoxX + svSize && my >= svBoxY && my < svBoxY + svSize) {
                                    val sat = ((mx - svBoxX) / svSize).coerceIn(0f, 1f)
                                    val value = (1f - (my - svBoxY) / svSize).coerceIn(0f, 1f)
                                    val alpha = s.getA()
                                    s.set(ColorSetting.fromHSV(s.getHue(), sat, value, alpha))
                                    dropSetting = null
                                    return true
                                }

                                // Hue bar click
                                val hueBarW = (sWidth - 22f - svSize).coerceAtMost(180f - svSize - 6f)
                                if (mx >= hueBarX && mx < hueBarX + hueBarW && my >= svBoxY && my < svBoxY + hueBarH) {
                                    val hue = ((mx - hueBarX) / hueBarW).coerceIn(0f, 1f)
                                    val sat = s.getSaturation()
                                    val value = s.getValue()
                                    val alpha = s.getA()
                                    s.set(ColorSetting.fromHSV(hue, sat, value, alpha))
                                    dropSetting = null
                                    return true
                                }

                                // Presets
                                val presetsY = pickerY + svSize + 4f
                                val relX = mx - sLeft - PAD - 8f
                                if (relX >= 0f) {
                                    val swatchSize = 14f
                                    val gap = 2f
                                    val idx = (relX / (swatchSize + gap)).toInt()
                                    val presets = intArrayOf(
                                        0xFFFF0000.toInt(), 0xFFFF8800.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt(),
                                        0xFF00FFFF.toInt(), 0xFF0000FF.toInt(), 0xFF8800FF.toInt(), 0xFFFF00FF.toInt(),
                                        0xFFFFFFFF.toInt(), 0xFF888888.toInt(), 0xFF000000.toInt()
                                    )
                                    if (idx in presets.indices) {
                                        s.set(presets[idx])
                                        dropSetting = null
                                        return true
                                    }
                                }
                                dropSetting = null
                            } else { dropSetting = s }
                            return true
                        }
                        else -> return false
                    }
                }
                sy3 += sh
            }
        }

        dropSetting = null
        return super.mouseClicked(e, b)
    }

    override fun mouseReleased(e: MouseButtonEvent): Boolean {
        draggingWindow = false
        dragSlider = null
        return super.mouseReleased(e)
    }

    override fun mouseDragged(e: MouseButtonEvent, ddx: Double, ddy: Double): Boolean {
        val mx = e.x().toFloat(); val my = e.y().toFloat()

        if (draggingWindow && !maximized) {
            windowX = (mx - windowDragX).coerceIn(0f, width - panelW)
            windowY = (my - windowDragY).coerceIn(0f, height - panelH)
            return true
        }

        val info = dragSlider ?: return super.mouseDragged(e, ddx, ddy)
        if (e.button() != 0) return true
        val (mod, si) = info
        val sets = getSettings(mod); val s = sets.getOrNull(si) ?: run { dragSlider = null; return true }
        if (s !is NumberSetting && s !is RangeSetting) { dragSlider = null; return true }
        val sLeft = if (activeTab == Tab.SETTINGS) px + PAD else px + catW + modW
        val sWidth = if (activeTab == Tab.SETTINGS) panelW - PAD * 2 else setW - PAD
        val frac = ((mx - sLeft - 10f) / (sWidth - 20f)).coerceIn(0f, 1f)
        when (s) { is NumberSetting -> s.setValue(s.min + (s.max - s.min) * frac); is RangeSetting -> s.setValue(s.min + (s.max - s.min) * frac) }
        return true
    }

    override fun mouseScrolled(mx: Double, my: Double, dX: Double, dY: Double): Boolean {
        val mLeft = px + catW
        val sLeft = px + catW + modW; val sWidth = setW - PAD
        if (mx >= mLeft && mx < mLeft + modW && my >= py && my < py + panelH) {
            scrollY = minOf(scrollY + (dY * 30).toFloat(), 0f); return true
        }
        if (mx >= sLeft && mx < sLeft + sWidth && my >= py && my < py + panelH) {
            val selMod = selectedModule
            if (selMod != null) {
                val totalH = getTotalSettingsHeight(selMod)
                val visibleH = panelH - TOP_BAR_H - MOD_H - PAD * 3
                val maxScroll = -(totalH - visibleH).coerceAtLeast(0f)
                settingsScrollY = (settingsScrollY + (dY * 30).toFloat()).coerceIn(maxScroll, 0f)
            }
            return true
        }
        return false
    }

    private fun getTotalSettingsHeight(m: Module): Float {
        var h = 0f; var lastCat = ""
        for (s in getSettings(m)) {
            if (s.category != lastCat) { lastCat = s.category; if (lastCat.isNotEmpty()) h += CAT_TITLE_H }
            h += settingHeight(s)
        }
        return h
    }

    override fun keyPressed(e: KeyEvent): Boolean {
        if (bindMod != null) {
            if (e.isEscape) { bindSet?.setKey(-1); bindMod!!.keybind = -1 }
            else { bindSet?.setKey(e.scancode()); bindMod!!.keybind = e.scancode() }
            bindSet = null; bindMod = null; return true
        }
        if (e.isEscape) { onClose(); return true }
        return super.keyPressed(e)
    }

    override fun renderBackground(g: GuiGraphics, mx: Int, my: Int, d: Float) {}
    override fun removed() {}
    override fun isPauseScreen() = false
    override fun onClose() { ThemeManager.INSTANCE.saveAll(); mc.setScreen(null) }

    data class DragInfo(val mod: Module, val si: Int)
}
