package so.aporia.utils.user.render.ui.clickgui

import aporia.webview.WebviewScreen
import com.chaos.annotation.Obfuscate
import net.minecraft.client.Minecraft
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
import so.aporia.module.settings.*
import so.aporia.utils.files.FilesManager
import so.aporia.utils.user.locale.LocaleManager
import so.aporia.utils.user.render.color.ColorUtil
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import so.aporia.utils.user.render.theme.ThemeManager
import so.aporia.utils.user.render.theme.ThemeManager.Theme
import java.nio.file.Files
import kotlin.math.*

@Obfuscate
@OnlyIn(Dist.CLIENT)
class ClickGuiScreen(private val clickGui: ClickGui) : Screen(Component.literal("ClickGui")) {

    private var selCategory = 0
    private var selectedModule: Module? = null
    private var scrollY = 0f
    private var settingsScrollY = 0f
    private var bindSet: BindSetting? = null
    private var bindMod: Module? = null
    private var dragSlider: DragInfo? = null
    private var dropSetting: Setting<*>? = null
    private var expandedCategories = mutableMapOf<String, Boolean>()

    private var bgImage: Identifier? = null

    // ── Category expansion states (persistent across renders) ──
    private val catExpanded = mutableMapOf<String, Boolean>()

    // ── Dynamic layout (computed from window size) ──
    private val panelW get() = (width * 0.38f).coerceAtMost(420f)
    private val panelH get() = (height * 0.42f).coerceAtMost(260f)
    private val px get() = (width - panelW) / 2f
    private val py get() = (height - panelH) / 2f
    private val catW get() = panelW * 0.19f
    private val modW get() = panelW * 0.39f
    private val setW get() = panelW - catW - modW
    private val catH get() = maxOf(panelH * 0.065f, 20f)
    private val modH get() = maxOf(panelH * 0.05f, 16f)
    private val setH get() = maxOf(panelH * 0.04f, 14f)
    private val catTitleH get() = maxOf(panelH * 0.055f, 18f) // category header height
    private val dh get() = setH * 0.7f
    private val pad get() = maxOf(panelH * 0.015f, 4f)

    private val categories: List<Category> = Category.values().toList()

    override fun init() {
        val bgPath = FilesManager.ROOT.resolve("custom/clickgui.png")
        if (Files.exists(bgPath)) bgImage = AporiaRenderer.INSTANCE.loadImage(bgPath)
    }

    override fun render(g: GuiGraphics, mx: Int, my: Int, d: Float) {
        val r = AporiaRenderer.INSTANCE
        val th = ThemeManager.INSTANCE.active() ?: return

        // ── Avatar popup (screen bottom-left, outside GUI) ──
        val aSize = 28f; val aX = 4f; val aY = height - aSize - 4f
        val mc = Minecraft.getInstance(); val player = mc.player
        var avatarTex: Identifier? = null
        val discord = ModuleManager.get("Discord RPC")
        if (discord != null && discord.isEnabled) {
            val dm = discord as? so.aporia.module.impl.misc.DiscordRPCModule
            dm?.loadAvatarOnRenderThread(); avatarTex = dm?.avatarId
        }
        r.drawRectBlurred(aX - 2, aY - 2, aSize + 4 + r.getTextWidth(Fonts.BOLD, player?.name?.string ?: "Player", 11f) + 10, aSize + 4, 6f, ColorUtil.rgba(0, 0, 0, 140), 3f)
        if (avatarTex != null) r.drawImageCropped(aX, aY, aSize, aSize, avatarTex, aSize / 2f, 0f, 0f, 1f, 1f)
        else if (player != null) {
            val skinId = player.skin.body.texturePath()
            if (skinId != null) { r.drawImageCropped(aX, aY, aSize, aSize, skinId, aSize / 2f, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f); r.drawImageCropped(aX, aY, aSize, aSize, skinId, aSize / 2f, 40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f) }
            else r.drawRectBlurred(aX, aY, aSize, aSize, aSize / 2f, th.guiSettingValue)
        } else r.drawRectBlurred(aX, aY, aSize, aSize, aSize / 2f, th.guiSettingValue)
        if (player != null) r.drawText(Fonts.BOLD, player.name.string, aX + aSize + 6, aY + (aSize - 11f) / 2f - 1f, 11f, 0xFFFFFFFF.toInt())

        // ── Panel background ──
        val bg = bgImage
        if (bg != null) r.drawImage(px, py, panelW, panelH, bg, 6f)
        else r.drawRectBlurred(px, py, panelW, panelH, 6f, th.guiBackground, 3f)

        // ── Left: Categories ──
        var cy = py + pad
        for ((i, cat) in categories.withIndex()) {
            val over = mx >= px + pad && mx < px + catW && my >= cy && my < cy + catH
            val sel = i == selCategory
            if (over || sel) r.drawRectBlurred(px + pad, cy, catW - pad * 2, catH, 3f,
                if (sel) th.guiTitleBg else ColorUtil.rgba(255, 255, 255, 12))
            r.drawText(Fonts.REGULAR, cat.icon.toString(), px + pad + 4, cy + (catH - 12f) / 2f - 1f, 12f,
                if (sel) 0xFFFFFFFF.toInt() else th.guiDisabledDot)
            val label = LocaleManager.INSTANCE.get("category.${cat.name.lowercase()}") ?: cat.name
            r.drawText(Fonts.REGULAR, label, px + pad + 20, cy + (catH - 10f) / 2f - 1f, 10f,
                if (sel) 0xFFFFFFFF.toInt() else th.guiSettingValue)
            if (sel) r.drawRectBlurred(px + pad + catW - pad * 2 - 3, cy + 3, 2f, catH - 6, 1f, th.guiEnabledDot)
            cy += catH
        }

        // ── Center: Modules ──
        val mLeft = px + catW; val mTop = py + pad; val mBot = py + panelH - pad
        var my2 = mTop + scrollY
        val mods = ModuleManager.getByCategory(categories[selCategory])
        for ((i, mod) in mods.withIndex()) {
            if (my2 + modH < mTop || my2 > mBot) { my2 += modH; continue }
            val over = mx >= mLeft && mx < mLeft + modW && my >= my2 && my < my2 + modH
            val en = mod.isEnabled; val selMod = mod === selectedModule
            if (over || selMod) r.drawRectBlurred(mLeft, my2, modW, modH, 2f,
                if (selMod) th.guiTitleBg else ColorUtil.rgba(255, 255, 255, 12))
            if (en) r.drawRectBlurred(mLeft + 2, my2 + 2, 2f, modH - 4, 1f, th.guiEnabledDot)
            r.drawText(Fonts.REGULAR, mod.name, mLeft + 8, my2 + (modH - 10f) / 2f - 1f, 10f,
                if (en) 0xFFFFFFFF.toInt() else th.guiDisabledDot)
            if (selMod) r.drawText(Fonts.REGULAR, "\u25C0", mLeft + modW - 12, my2 + (modH - 9f) / 2f - 1f, 9f, ColorUtil.rgba(180, 180, 200, 200))
            my2 += modH
        }

        // ── Right: Settings ──
        val sLeft = px + catW + modW; val sTop = py + pad; val sWidth = setW - pad; val sBot = py + panelH - pad
        val selMod = selectedModule
        if (selMod != null) {
            r.drawRectBlurred(sLeft, sTop, sWidth, modH, 2f, th.guiTitleBg)
            r.drawText(Fonts.BOLD, selMod.name, sLeft + 4, sTop + (modH - 11f) / 2f - 1f, 11f, 0xFFFFFFFF.toInt())

            var sy3 = sTop + modH + pad + settingsScrollY
            var lastCat = ""
            for (s in getSettings(selMod)) {
                if (s.category != lastCat) {
                    lastCat = s.category
                    if (lastCat.isNotEmpty()) {
                        val sh = catTitleH
                        if (sy3 + sh < sTop || sy3 > sBot) { sy3 += sh; continue }
                        val exp = catExpanded.getOrPut(lastCat) { true }
                        val over = mx >= sLeft && mx < sLeft + sWidth && my >= sy3 && my < sy3 + sh
                        if (over) r.drawRectBlurred(sLeft, sy3, sWidth, sh, 0f, ColorUtil.rgba(255, 255, 255, 10))
                        r.drawText(Fonts.BOLD, "${if (exp) "\u25BC" else "\u25B6"} $lastCat",
                            sLeft + 8, sy3 + (sh - 10f) / 2f - 1f, 10f, th.guiSettingText)
                        sy3 += sh
                        if (!exp) continue
                    }
                }
                val sh = settingHeight(s)
                if (sy3 + sh < sTop || sy3 > sBot) { sy3 += sh; continue }
                val sov = mx >= sLeft && mx < sLeft + sWidth && my >= sy3 && my < sy3 + sh
                if (sov) r.drawRectBlurred(sLeft, sy3, sWidth, sh, 0f, ColorUtil.rgba(255, 255, 255, 6))
                drawSetting(r, s, sy3, th, sLeft, sWidth, sov, mx, my)
                sy3 += sh
            }
        }

        // ── Browser button (bottom-left of GUI) ──
        val bbSize = 26f; val bbX = px + pad + 4; val bbY = py + panelH - pad - bbSize - 4
        val bbOver = mx >= bbX && mx < bbX + bbSize + 60 && my >= bbY && my < bbY + bbSize
        r.drawRectBlurred(bbX, bbY, bbSize + 56, bbSize, 5f,
            if (bbOver) ColorUtil.rgba(80, 80, 160, 200) else ColorUtil.rgba(40, 40, 80, 180))
        r.drawRectBlurred(bbX + 5, bbY + 5, 5f, bbSize - 10, 2.5f, th.guiEnabledDot)
        r.drawText(Fonts.REGULAR, "Browser", bbX + 16, bbY + (bbSize - 10f) / 2f - 1f, 10f, 0xFFFFFFFF.toInt())
        r.flush()

        // ── Bind hint ──
        val bm = bindMod ?: return
        val hint = LocaleManager.INSTANCE.get("gui.presskey") ?: "Press any key..."
        val hs2 = 12f; val hw2 = r.getTextWidth(Fonts.BOLD, hint, hs2)
        val hx = (width - hw2) / 2f
        r.drawRectBlurred(hx - 6f, 5f, hw2 + 12f, hs2 + 4f, 5f, ColorUtil.rgba(0, 0, 0, 160))
        r.drawText(Fonts.BOLD, hint, hx, 6f, hs2, 0xFFFFAA00.toInt())
        r.flush()
    }

    // ── Setting helpers ──

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
                } catch (_: Exception) {
                    java.lang.System.err.println("[ClickGui] category reflection failed for ${m.name}.${f.name}")
                }
            }
            list.add(s)
        }
        return list
    }

    private fun settingHeight(s: Setting<*>): Float = when {
        s == dropSetting && (s is SelectSetting || s is MultiSelectSetting) -> {
            val opts = if (s is SelectSetting) s.getOptions() else (s as MultiSelectSetting).getOptions()
            setH + 4f + opts.size * dh + 3f
        }
        s is NumberSetting || s is RangeSetting -> setH + 8f
        else -> setH
    }

    private fun drawSetting(r: AporiaRenderer, s: Setting<*>, y: Float, th: Theme, sx: Float, sw: Float, sov: Boolean, mx: Int, my: Int) {
        val pw = sw - 14
        when (s) {
            is BooleanSetting -> {
                val vt = if (s.isEnabled) "ON" else "OFF"
                val vc = if (s.isEnabled) 0xFF64FF64.toInt() else th.guiSettingValue
                val vw = r.getTextWidth(Fonts.REGULAR, vt, 8f)
                r.drawText(Fonts.REGULAR, s.name, sx + 8, y + (setH - 9f) / 2f - 1f, 9f, th.guiSettingText)
                r.drawText(Fonts.REGULAR, vt, sx + sw - 6 - vw, y + (setH - 8f) / 2f - 1f, 8f, vc)
            }
            is NumberSetting -> {
                val frac = ((s.get() - s.min) / (s.max - s.min)).toFloat().coerceIn(0f, 1f)
                r.drawText(Fonts.REGULAR, s.name, sx + 8, y + 1, 9f, th.guiSettingText)
                val txt = String.format("%.1f", s.get())
                val tw = r.getTextWidth(Fonts.REGULAR, txt, 7f)
                val by = y + setH / 2f + 4
                r.drawRectBlurred(sx + 10, by, pw, 2f, 1f, ColorUtil.rgba(255, 255, 255, 30))
                r.drawRectBlurred(sx + 10, by, pw * frac, 2f, 1f, th.guiSettingValue)
                r.drawText(Fonts.REGULAR, txt, sx + 10 + pw * frac - tw / 2f, by + 3f, 7f, th.guiSettingValue)
            }
            is RangeSetting -> {
                val frac = ((s.get() - s.min) / (s.max - s.min)).toFloat().coerceIn(0f, 1f)
                r.drawText(Fonts.REGULAR, s.name, sx + 8, y + 1, 9f, th.guiSettingText)
                val txt = String.format("%.1f", s.get())
                val tw = r.getTextWidth(Fonts.REGULAR, txt, 7f)
                val by = y + setH / 2f + 4
                r.drawRectBlurred(sx + 10, by, pw, 2f, 1f, ColorUtil.rgba(255, 255, 255, 30))
                r.drawRectBlurred(sx + 10, by, pw * frac, 2f, 1f, th.guiSettingValue)
                r.drawText(Fonts.REGULAR, txt, sx + 10 + pw * frac - tw / 2f, by + 3f, 7f, th.guiSettingValue)
            }
            is SelectSetting -> {
                val opts = s.getOptions()
                r.drawText(Fonts.REGULAR, s.name, sx + 8, y + (setH - 9f) / 2f - 1f, 9f, th.guiSettingText)
                val disp = s.get() + " \u25BC"
                val dw = r.getTextWidth(Fonts.REGULAR, disp, 8f)
                r.drawText(Fonts.REGULAR, disp, sx + sw - 8 - dw, y + (setH - 8f) / 2f - 1f, 8f, th.guiSettingValue)
                if (s == dropSetting) {
                    val dy = y + setH + 2
                    for (oi in opts.indices) {
                        val oy = dy + oi * dh; val sel = s.getSelectedIndex() == oi
                        val oh = mx >= sx + 8 && mx < sx + sw - 8 && my >= oy && my < oy + dh
                        if (oh) r.drawRectBlurred(sx + 8, oy, pw, dh, 0f, ColorUtil.rgba(255, 255, 255, 20))
                        r.drawText(Fonts.REGULAR, opts[oi], sx + 12, oy + (dh - 8f) / 2f - 1f, 8f, if (sel) 0xFFFFFFFF.toInt() else th.guiSettingValue)
                        val dx = sx + sw - 16; val dy2 = oy + dh / 2f
                        r.drawCircle(dx, dy2, 3f, if (sel) th.guiSettingValue else ColorUtil.rgba(255, 255, 255, 60))
                        if (sel) r.drawCircle(dx, dy2, 1.5f, 0xFFFFFFFF.toInt())
                    }
                }
            }
            is MultiSelectSetting -> {
                val opts = s.getOptions()
                r.drawText(Fonts.REGULAR, s.name, sx + 8, y + (setH - 9f) / 2f - 1f, 9f, th.guiSettingText)
                val sum = s.getSelected().joinToString(", ").ifEmpty { "-" }
                val disp = sum + " \u25BC"
                val dw = r.getTextWidth(Fonts.REGULAR, disp, 8f)
                r.drawText(Fonts.REGULAR, disp, sx + sw - 8 - dw, y + (setH - 8f) / 2f - 1f, 8f, th.guiSettingValue)
                if (s == dropSetting) {
                    val dy = y + setH + 2
                    for (oi in opts.indices) {
                        val oy = dy + oi * dh; val sel = s.isSelected(opts[oi])
                        val oh = mx >= sx + 8 && mx < sx + sw - 8 && my >= oy && my < oy + dh
                        if (oh) r.drawRectBlurred(sx + 8, oy, pw, dh, 0f, ColorUtil.rgba(255, 255, 255, 20))
                        r.drawText(Fonts.REGULAR, opts[oi], sx + 12, oy + (dh - 8f) / 2f - 1f, 8f, if (sel) 0xFFFFFFFF.toInt() else th.guiSettingValue)
                        val bx = sx + sw - 18; val by2 = oy + dh / 2f - 2.5f
                        r.drawRectBlurred(bx, by2, 6f, 6f, 1f, if (sel) th.guiSettingValue else ColorUtil.rgba(255, 255, 255, 60))
                        if (sel) { r.drawLine(bx, by2 + 2.5f, bx + 1.5f, by2 + 4f, 1f, 0xFFFFFFFF.toInt()); r.drawLine(bx + 1.5f, by2 + 4f, bx + 4f, by2 + 1f, 1f, 0xFFFFFFFF.toInt()) }
                    }
                }
            }
            is BindSetting -> s.render(r, sx.toInt(), y.toInt(), sw.toInt(), s == bindSet)
            is TextSetting -> {
                val v = s.get(); val txt = if (v.length > 10) v.substring(0, 8) + ".." else v
                val tw = r.getTextWidth(Fonts.REGULAR, txt, 8f)
                r.drawText(Fonts.REGULAR, s.name, sx + 8, y + (setH - 9f) / 2f - 1f, 9f, th.guiSettingText)
                r.drawText(Fonts.REGULAR, txt, sx + sw - 8 - tw, y + (setH - 8f) / 2f - 1f, 8f, th.guiSettingValue)
            }
            is ButtonSetting -> {
                val bw = r.getTextWidth(Fonts.REGULAR, s.name, 8f) + 12
                val bx = sx + sw - 8 - bw
                r.drawRectBlurred(bx, y + 1, bw, setH - 3, 2f, ColorUtil.rgba(255, 255, 255, 30))
                r.drawText(Fonts.REGULAR, s.name, bx + 4, y + (setH - 8f) / 2f - 1f, 8f, th.guiSettingValue)
            }
            else -> {
                val txt = s.value?.toString() ?: ""
                val tw = r.getTextWidth(Fonts.REGULAR, txt, 8f)
                r.drawText(Fonts.REGULAR, txt, sx + sw - 8 - tw, y + (setH - 8f) / 2f - 1f, 8f, th.guiSettingValue)
            }
        }
    }

    // ── Mouse ──
    override fun mouseClicked(e: MouseButtonEvent, b: Boolean): Boolean {
        val mx = e.x(); val my = e.y(); val btn = e.button()

        if (bindMod != null) {
            val vk = if (btn in 0..7) 500 + btn else btn
            bindSet?.setKey(vk); bindMod!!.keybind = vk; bindSet = null; bindMod = null
            return true
        }

        // Browser button
        val bbSize = 26f; val bbX = px + pad + 4; val bbY = py + panelH - pad - bbSize - 4
        if (mx >= bbX && mx < bbX + bbSize + 60 && my >= bbY && my < bbY + bbSize) {
            Minecraft.getInstance().setScreen(WebviewScreen("https://google.com")); return true
        }

        // Category click
        var cy = py + pad
        for ((i, _) in categories.withIndex()) {
            if (mx >= px + pad && mx < px + catW && my >= cy && my < cy + catH) {
                selCategory = i; selectedModule = null; scrollY = 0f; dropSetting = null; settingsScrollY = 0f; return true
            }
            cy += catH
        }

        // Module click
        val mLeft = px + catW; val mTop = py + pad
        val mods = ModuleManager.getByCategory(categories[selCategory])
        var my2 = mTop + scrollY
        for ((i, mod) in mods.withIndex()) {
            if (my2 + modH < mTop || my2 > py + panelH - pad) { my2 += modH; continue }
            if (mx >= mLeft && mx < mLeft + modW && my >= my2 && my < my2 + modH) {
                when (btn) {
                    0 -> { mod.toggle() }
                    1 -> { selectedModule = if (selectedModule === mod) null else mod; dropSetting = null; settingsScrollY = 0f }
                    2 -> { val bs = getSettings(mod).find { it is BindSetting } as? BindSetting; if (bs != null) { bindSet = bs; bindMod = mod } }
                }
                return true
            }
            my2 += modH
        }

        // Settings click (including category headers)
        val selMod = selectedModule
        if (selMod != null) {
            val sLeft = px + catW + modW; val sTop = py + pad; val sWidth = setW - pad
            var sy3 = sTop + modH + pad + settingsScrollY
            var lastCat = ""
            for (s in getSettings(selMod)) {
                if (s.category != lastCat) {
                    lastCat = s.category
                    if (lastCat.isNotEmpty()) {
                        val sh = catTitleH
                        if (my >= sy3 && my < sy3 + sh) {
                            val cur = catExpanded.getOrDefault(lastCat, true)
                            catExpanded[lastCat] = !cur
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
                                val rel = my - sy3 - setH - 2; val idx = (rel / dh).toInt()
                                if (idx in opts.indices) { s.setSelectedIndex(idx); dropSetting = null } else dropSetting = null
                                return true
                            } else { dropSetting = s; return true }
                        }
                        is MultiSelectSetting -> {
                            val opts = s.getOptions(); if (opts.isEmpty()) return true
                            if (s == dropSetting) {
                                val rel = my - sy3 - setH - 2; val idx = (rel / dh).toInt()
                                if (idx in opts.indices) s.toggle(opts[idx]); else dropSetting = null
                                return true
                            } else { dropSetting = s; return true }
                        }
                        is ButtonSetting -> { s.click(); return true }
                        else -> return false
                    }
                }
                sy3 += sh
            }
        }
        dropSetting = null
        return super.mouseClicked(e, b)
    }

    override fun mouseDragged(e: MouseButtonEvent, ddx: Double, ddy: Double): Boolean {
        val mx = e.x(); val my = e.y()
        val info = dragSlider ?: return super.mouseDragged(e, ddx, ddy)
        if (e.button() != 0) return true
        val (mod, si) = info
        val sets = getSettings(mod); val s = sets.getOrNull(si) ?: run { dragSlider = null; return true }
        if (s !is NumberSetting && s !is RangeSetting) { dragSlider = null; return true }
        val sLeft = px + catW + modW; val sWidth = setW - pad
        val frac = ((mx.toFloat() - sLeft - 10) / (sWidth - 20)).coerceIn(0f, 1f)
        when (s) { is NumberSetting -> s.setValue(s.min + (s.max - s.min) * frac); is RangeSetting -> s.setValue(s.min + (s.max - s.min) * frac) }
        return true
    }

    override fun mouseReleased(e: MouseButtonEvent): Boolean { dragSlider = null; return super.mouseReleased(e) }

    override fun mouseScrolled(mx: Double, my: Double, dX: Double, dY: Double): Boolean {
        val mLeft = px + catW
        val sLeft = px + catW + modW; val sWidth = setW - pad
        // Module scroll (center column)
        if (mx >= mLeft && mx < mLeft + modW && my >= py && my < py + panelH) {
            scrollY = minOf(scrollY + (dY * 30).toFloat(), 0f)
            return true
        }
        // Settings scroll (right column)
        if (mx >= sLeft && mx < sLeft + sWidth && my >= py && my < py + panelH) {
            val selMod = selectedModule
            if (selMod != null) {
                val totalH = getTotalSettingsHeight(selMod)
                val visibleH = panelH - modH - pad * 2
                val maxScroll = -(totalH - visibleH).coerceAtLeast(0f)
                settingsScrollY = (settingsScrollY + (dY * 30).toFloat()).coerceIn(maxScroll, 0f)
            }
            return true
        }
        return false
    }

    private fun getTotalSettingsHeight(m: Module): Float {
        var h = 0f
        var lastCat = ""
        for (s in getSettings(m)) {
            if (s.category != lastCat) {
                lastCat = s.category
                if (lastCat.isNotEmpty()) h += catTitleH
            }
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
    override fun onClose() { ThemeManager.INSTANCE.saveAll(); Minecraft.getInstance().setScreen(null) }

    data class DragInfo(val mod: Module, val si: Int)

    companion object {
        @JvmStatic
        fun settingCategory(name: String, hierarchy: Int) {
            SettingCategoryManager.push(name, hierarchy)
        }

        @JvmStatic
        fun settingCategoryEnd() {
            SettingCategoryManager.pop()
        }
    }
}
