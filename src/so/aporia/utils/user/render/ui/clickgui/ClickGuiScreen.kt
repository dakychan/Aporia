package so.aporia.utils.user.render.ui.clickgui

import so.aporia.utils.imports.*
import net.minecraft.resources.Identifier
import so.aporia.utils.user.render.core.AporiaRenderer
import aporia.webview.WebviewScreen
import com.chaos.annotation.Obfuscate
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import so.aporia.module.Category
import so.aporia.utils.assets.AssetManager
import so.aporia.module.Module
import so.aporia.module.ModuleManager
import so.aporia.module.impl.misc.ClickGui
import so.aporia.module.settings.*

import so.aporia.utils.user.render.animation.Animator
import so.aporia.utils.user.render.animation.Easing
import so.aporia.utils.user.render.animation.SpringSimulator
import so.aporia.utils.user.render.animation.TypeAnim
import so.aporia.utils.user.render.font.Fonts
import so.aporia.utils.user.render.theme.ThemeManager
import so.aporia.utils.user.render.theme.ThemeManager.Theme
import org.lwjgl.glfw.GLFW
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.impl.KeyInputEvent
import so.aporia.utils.events.impl.MouseClickEvent
import com.chaos.annotation.ChaosNative
@Obfuscate
@OnlyIn(Dist.CLIENT)
@ChaosNative
class ClickGuiScreen(private val clickGui: ClickGui) : Screen(Component.literal("ClickGui")) {

    private enum class Tab(val localeKey: String) {
        AVATAR("gui.tab.avatar"),
        QUESTS("gui.tab.quests"),
        BROWSER("gui.tab.browser"),
        SETTINGS("gui.tab.settings");

        val label: String get() = locale.get(localeKey)
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
    private var editingSlider: SliderSetting? = null
    private var dragColorSetting = false
    private var dragColorHue = false
    private var dragColorAlpha = false
    private var dragColorSV = false
    private var dragColorSettingRef: ColorSetting? = null
    private val bindTypeAnim = TypeAnim(60, 120)

    private var minimized = false
    private var maximized = false
    private var showOtherPanel = false
    private var windowX = 0f
    private var windowY = 0f
    private var windowDragX = 0f
    private var windowDragY = 0f
    private var draggingWindow = false

    private val catExpanded = mutableMapOf<String, Boolean>()
    private var questScrollY: Float = 0f

    private var pickerBoundsY: Float = 0f

    private val openAnim = Animator(220, Easing::sineOut)
    private var closing = false
    private var lastModuleSwitch = 0L
    private val moduleSwitchAnim = Animator(250, Easing::cubicOut)
    private val dropAnim = Animator(180, Easing::cubicOut)
    private var lastDropTime = 0L
    private val categoryAnim = Animator(220, Easing::cubicOut)
    private var prevCategory = -1

    private val hoverSprings = mutableMapOf<Int, SpringSimulator>()
    private val selectionSpring = SpringSimulator(200f, 18f, 0f)
    private var lastSelCategory = 0

    private var logoTextureView: Any? = null
    private var logoId: Identifier? = null
    private val categoryIcons = mutableMapOf<Category, Identifier?>()
    private val catTypeAnims = Category.values().associateWith { TypeAnim(60, 120) }.toMutableMap()
    private val catAnimStarted = Category.values().associateWith { false }.toMutableMap()
    private val moduleTypeAnims = mutableMapOf<String, TypeAnim>()
    private val moduleAnimStarted = mutableMapOf<String, Boolean>()
    private val settingTypeAnims = mutableMapOf<String, TypeAnim>()
    private val settingAnimStarted = mutableMapOf<String, Boolean>()
    private var waveStartMs = 0L
    private val waveDelayPerItem = 200L

    init {
        openAnim.snapTo(1f)
    }

    companion object {
        const val TOP_BAR_H = 24f
        const val CORNER_RADIUS = 6f
        const val PAD = 6f
        const val CAT_W_RATIO = 0.18f
        const val MOD_W_RATIO = 0.37f
        const val CAT_H = 22f
        const val MOD_H = 18f
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

    private val useBlur: Boolean get() = clickGui.guiMode.getSelectedIndex() == 1

    private fun AporiaRenderer.drawBg(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Int, blurStrength: Float = 4f) {
        if (useBlur) drawRectBlurred(x, y, w, h, radius, color, blurStrength)
        else drawRect(x, y, w, h, radius, color)
    }

    init {
        selCategory = clickGui.savedCategory.get().toInt().coerceIn(0, Category.values().size - 1)
        scrollY = clickGui.savedScroll.get().toFloat()
        settingsScrollY = clickGui.savedSettingsScroll.get().toFloat()
        lastSelCategory = selCategory
    }

    override fun init() {
        val sw = width.toFloat()
        val sh = height.toFloat()
        val pw = (sw * 0.72f).coerceIn(500f, 900f)
        windowX = (sw - pw) / 2f
        windowY = (sh - (sh * 0.72f).coerceIn(360f, 680f)) / 2f
        initCategoryIcons()
        initLogo()
        waveStartMs = System.currentTimeMillis()
        for (cat in Category.values()) {
            catTypeAnims[cat]?.snap(locale.get("category.${cat.name.lowercase()}"))
            catAnimStarted[cat] = false
        }
        moduleTypeAnims.clear()
        moduleAnimStarted.clear()
        settingTypeAnims.clear()
        settingAnimStarted.clear()
    }

    private fun initCategoryIcons() {
        categoryIcons.clear()
        for (cat in Category.values()) {
            val path = so.aporia.utils.assets.AssetManager.getResourcePath(cat.texture)
            if (path != null) {
                val id = r.loadImage(path)
                categoryIcons[cat] = id
            }
        }
    }

    private fun initLogo() {
        logoId = null
        logoTextureView = null
        val logoPath = so.aporia.utils.assets.AssetManager.getResourcePath(
            Identifier.fromNamespaceAndPath("aporia", "texture/gui/settings.png"))
        if (logoPath != null) {
            val id = r.loadImage(logoPath)
            if (id != null) {
                logoId = id
                val tex = mc.textureManager.getTexture(id)
                if (tex != null) {
                    logoTextureView = tex
                }
            }
        }
    }

    private fun getLogoTextureView(): Any? {
        if (logoTextureView == null && logoId != null) {
            val tex = mc.textureManager.getTexture(logoId!!)
            if (tex != null) logoTextureView = tex
        }
        return logoTextureView
    }

    override fun render(g: GuiGraphics, mx: Int, my: Int, d: Float) {
        openAnim.update()
        moduleSwitchAnim.update()
        dropAnim.update()
        categoryAnim.update()
        selectionSpring.update(0.016f)
        selectionSpring.setTarget(selCategory.toFloat())

        if (closing && openAnim.isFinished()) {
            onCloseReal()
            return
        }

        val th = theme ?: return
        val fmx = mx.toFloat()
        val fmy = my.toFloat()

        drawWindow(r, th)
        drawTopBar(r, th, fmx, fmy)
        drawContent(r, th, fmx, fmy)

        r.flush()

        if (closing) {
            onCloseReal()
            return
        }

        val bm = bindMod ?: return
        if (bindTypeAnim.getTarget() != locale.get("gui.bindprompt")) {
            bindTypeAnim.setTarget(locale.get("gui.bindprompt"))
        }
        val hint = bindTypeAnim.update()
        val hs = 12f
        val hw = r.getTextWidth(Fonts.BOLD, hint, hs)
        val hx = (width - hw) / 2f
        r.drawBg(hx - 6f, 5f, hw + 12f, hs + 4f, 5f, colorUtil.rgba(0, 0, 0, 160))
        r.drawText(Fonts.BOLD, hint, hx, 6f, hs, 0xFFFFAA00.toInt())
        r.flush()
    }

    private fun drawWindow(r: AporiaRenderer, th: Theme) {
        r.drawBg(px, py, panelW, panelH, CORNER_RADIUS, th.guiBackground, 3f)
    }

    private fun drawTopBar(r: AporiaRenderer, th: Theme, mx: Float, my: Float) {
        val bx = px
        val by = py
        val bw = panelW

        r.drawRect(bx, by, bw, TOP_BAR_H, CORNER_RADIUS, colorUtil.rgba(0, 0, 0, 60))
        r.drawRect(bx, by + TOP_BAR_H - 1f, bw, 1f, 0f, th.guiSeparator)

        val logoSize = 16f
        val logoPad = 4f
        val logoY = by + (TOP_BAR_H - logoSize) / 2f
        if (logoId != null) {
            r.drawLogo(bx + logoPad, logoY, logoSize, logoSize, logoId!!, System.currentTimeMillis())
        }

        val leftTabs = listOf(Tab.BROWSER, Tab.SETTINGS)

        var tabX = bx + PAD + logoSize + logoPad * 2
        for (tab in leftTabs) {
            val tw = r.getTextWidth(Fonts.BOLD, tab.label, 10f) + 16f
            val over = mx >= tabX && mx < tabX + tw && my >= by && my < by + TOP_BAR_H
            val active = tab == activeTab
            if (over || active) {
                r.drawBg(tabX, by + 2f, tw, TOP_BAR_H - 4f, 3f,
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
        r.drawBg(rightX, by + 2f, combinedW, TOP_BAR_H - 4f, 3f,
            if (isAvOrQs) th.guiTitleBg else colorUtil.rgba(255, 255, 255, 12))

        val skinId = mc.player?.skin?.body?.texturePath()
        if (skinId != null) {
            r.drawImageCropped(rightX + 2f, by + 2f, combinedW - 4f, TOP_BAR_H - 4f,
                skinId, (combinedW - 4f) / 2f, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f)
        } else {
            r.drawCircle(rightX + combinedW / 2f, by + TOP_BAR_H / 2f, (TOP_BAR_H - 6f) / 2f,
                if (isAvOrQs) th.guiTitleBg else colorUtil.rgba(255, 255, 255, 40))
        }

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
        val now = System.currentTimeMillis()

        val catLeft = px + PAD - 2f
        var catY = cy + 10f
        for ((i, cat) in categories.withIndex()) {
            val ta = catTypeAnims[cat] ?: continue
            val started = catAnimStarted[cat] ?: false
            val elapsed = now - waveStartMs
            if (!started && elapsed >= i * waveDelayPerItem) {
                ta.setTarget(locale.get("category.${cat.name.lowercase()}"))
                catAnimStarted[cat] = true
            }
            val label = if (started) ta.update() else ""

            val over = mx >= catLeft && mx < catLeft + catW && my >= catY && my < catY + CAT_H
            val sel = i == selCategory

            val springKey = 1000 + i
            val spring = hoverSprings.getOrPut(springKey) { SpringSimulator(200f, 16f, 0f) }
            spring.setTarget(if (over || sel) 1f else 0f)
            spring.update(0.016f)
            val hoverFrac = spring.value()

            val catCenterX = catLeft + (catW - PAD * 2) / 2f
            val iconSize = 12f
            val iconId = categoryIcons[cat]

            if (sel) {
                r.drawBg(catLeft, catY, catW - PAD * 2, CAT_H, 3f, th.guiTitleBg)
            } else if (over || hoverFrac > 0.01f) {
                val bgAlpha = ((12 * hoverFrac).toInt().coerceIn(0, 255))
                r.drawBg(catLeft, catY, catW - PAD * 2, CAT_H, 3f,
                    colorUtil.rgba(255, 255, 255, bgAlpha))
            }
            if (sel) {
                val selColor = th.guiEnabledDot
                r.drawBg(catLeft + catW - PAD - 1f, catY + 3f, 2f, CAT_H - 6f, 1f,
                    colorUtil.rgba(selColor shr 16 and 0xFF, selColor shr 8 and 0xFF, selColor and 0xFF, selColor shr 24 and 0xFF))
            }

            if (iconId != null && mc.textureManager.getTexture(iconId) != null) {
                r.depth(1f)
                r.drawImage(catCenterX - iconSize / 2f, catY + (CAT_H - iconSize) / 2f, iconSize, iconSize, iconId)
                r.depth(0f)
            }
            val labelX = catCenterX + iconSize / 2f + 4f
            r.drawText(Fonts.REGULAR, label, labelX, catY + (CAT_H - 10f) / 2f - 1f, 10f,
                if (sel) 0xFFFFFFFF.toInt() else colorUtil.lerp(theme.guiSettingValue, 0xFFFFFFFF.toInt(), hoverFrac))
            catY += CAT_H
        }

        val btnW = catW - PAD * 2
        val btnH = AVATAR_SIZE + 4f
        val btnX = catLeft
        val btnY = py + panelH - PAD - btnH
        val overBtn = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH
        r.drawBg(btnX, btnY, btnW, btnH, 4f,
            if (overBtn) th.guiTitleBg else colorUtil.rgba(0, 0, 0, 100))
        r.drawText(Fonts.BOLD, locale.get("gui.other"),
            btnX + 8f, btnY + (btnH - 11f) / 2f - 1f, 11f, 0xFFFFFFFF.toInt())
        r.drawText(Fonts.REGULAR, "\u25B6", btnX + btnW - 16f, btnY + (btnH - 10f) / 2f - 1f, 10f, th.guiSettingValue)

        if (showOtherPanel) {
            drawOtherPanel(r, th, mx, my)
        } else {
            drawModuleList(r, th, mx, my, cy, categories)
            drawSettingsPanel(r, th, mx, my, cy)
        }
    }

    private fun drawOtherPanel(r: AporiaRenderer, th: Theme, mx: Float, my: Float) {
        val cy = contentY
        val label = locale.get("gui.other.title")
        r.drawText(Fonts.BOLD, label, px + PAD, cy, 14f, 0xFFFFFFFF.toInt())
    }

    private fun drawModuleList(r: AporiaRenderer, th: Theme, mx: Float, my: Float, cy: Float, categories: List<Category>) {
        val mLeft = px + catW
        val mTop = cy
        val catAnim = categoryAnim.value()
        val slideOff = (1f - catAnim) * 16f
        var my2 = mTop + scrollY
        val mods = ModuleManager.getByCategory(categories[selCategory])
        val altDown = org.lwjgl.glfw.GLFW.glfwGetKey(mc.window.handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT) == 1
        val now = System.currentTimeMillis()
        for ((mi, mod) in mods.withIndex()) {
            if (my2 + MOD_H < mTop || my2 > py + panelH - PAD) { my2 += MOD_H; continue }
            val over = mx >= mLeft && mx < mLeft + modW && my >= my2 && my < my2 + MOD_H
            val en = mod.isEnabled
            val selMod = mod === selectedModule

            // Module hover spring
            val springKey = 3000 + mods.indexOf(mod)
            val spring = hoverSprings.getOrPut(springKey) { SpringSimulator(200f, 16f, 0f) }
            spring.setTarget(if (over || selMod) 1f else 0f)
            spring.update(0.016f)
            val hoverFrac = spring.value()

            val modKey = "module:${mod.name}"
            val modAnim = moduleTypeAnims.getOrPut(modKey) { TypeAnim(60, 120) }
            val modStarted = moduleAnimStarted.getOrPut(modKey) { false }
            val modElapsed = now - waveStartMs
            val modDelay = (categories.size + mi) * waveDelayPerItem
            val modName = if (!modStarted) {
                if (modElapsed >= modDelay) {
                    modAnim.setTarget(mod.name)
                    moduleAnimStarted[modKey] = true
                }
                modAnim.update()
            } else modAnim.update()

            r.drawBg(mLeft + slideOff, my2, modW, MOD_H, 2f, colorUtil.rgba(0, 0, 0, if (en) 70 else 40))
            if (selMod) {
                r.drawBg(mLeft + slideOff, my2, modW, MOD_H, 2f, th.guiTitleBg)
            } else if (over || hoverFrac > 0.01f) {
                val hlAlpha = ((12 * hoverFrac).toInt().coerceIn(0, 255))
                r.drawBg(mLeft + slideOff, my2, modW, MOD_H, 2f, colorUtil.rgba(255, 255, 255, hlAlpha))
            }
            if (en) r.drawBg(mLeft + slideOff + 2f, my2 + 2f, 2f, MOD_H - 4f, 1f, th.guiEnabledDot)
            r.drawText(Fonts.REGULAR, modName, mLeft + 8f + slideOff, my2 + (MOD_H - 10f) / 2f - 1f, 10f,
                if (en) 0xFFFFFFFF.toInt() else theme.guiDisabledDot)
            if (altDown && mod.keybind != -1) {
                val keyName = so.aporia.utils.user.input.KeyCodeMap.getName(mod.keybind)
                val kw = r.getTextWidth(Fonts.REGULAR, keyName, 7f)
                r.drawText(Fonts.REGULAR, keyName, mLeft + modW - kw - 4f + slideOff, my2 + (MOD_H - 7f) / 2f, 7f, 0xFFAAAAAA.toInt())
            }
            if (selMod) r.drawText(Fonts.REGULAR, "\u25C0", mLeft + modW - 12f + slideOff, my2 + (MOD_H - 9f) / 2f - 1f, 9f, colorUtil.rgba(180, 180, 200, 200))
            my2 += MOD_H
        }
    }

    private fun drawSettingsPanel(r: AporiaRenderer, th: Theme, mx: Float, my: Float, cy: Float) {
        val sLeft = px + catW + modW
        val sTop = cy
        val sWidth = setW - PAD
        val selMod = selectedModule ?: return

        val msAnim = moduleSwitchAnim.value()
        val titleSlide = (1f - msAnim) * 10f
        r.drawBg(sLeft, sTop, sWidth, MOD_H, 2f, th.guiTitleBg)
        val setNow = System.currentTimeMillis()
        val stDelay = (Category.values().size + ModuleManager.getByCategory(Category.values()[selCategory]).size) * waveDelayPerItem
        val stKey = "setting_title:${selMod.name}"
        val stAnim = settingTypeAnims.getOrPut(stKey) { TypeAnim(60, 120) }
        val stStarted = settingAnimStarted.getOrPut(stKey) { false }
        val stElapsed = setNow - waveStartMs
        val modTitle = if (!stStarted) {
            if (stElapsed >= stDelay) {
                stAnim.setTarget(selMod.name)
                settingAnimStarted[stKey] = true
            }
            stAnim.update()
        } else stAnim.update()
        r.drawText(Fonts.BOLD, modTitle, sLeft + 4f + titleSlide, sTop + (MOD_H - 11f) / 2f - 1f, 11f, colorUtil.rgba(255, 255, 255, (180 + 75 * msAnim).toInt()))

        var sy3 = sTop + MOD_H + PAD + settingsScrollY
        var lastCat = ""
        for (s in selMod.settings) {
            if (s.category != lastCat) {
                lastCat = s.category
                if (lastCat.isNotEmpty()) {
                    val sh = CAT_TITLE_H
                    if (sy3 + sh < sTop || sy3 > py + panelH - PAD) { sy3 += sh; continue }
                    val exp = catExpanded.getOrPut(lastCat) { true }
                    val over = mx >= sLeft && mx < sLeft + sWidth && my >= sy3 && my < sy3 + sh
                    if (over) r.drawBg(sLeft, sy3, sWidth, sh, 0f, colorUtil.rgba(255, 255, 255, 10))
                    r.drawText(Fonts.BOLD, "${if (exp) "\u25BC" else "\u25B6"} $lastCat",
                        sLeft + 8f, sy3 + (sh - 10f) / 2f - 1f, 10f, th.guiSettingText)
                    sy3 += sh
                    if (!exp) continue
                }
            }
            val sh = s.displayHeight(s == dropSetting)
            if (sy3 + sh < sTop || sy3 > py + panelH - PAD) { sy3 += sh; continue }
            val sov = mx >= sLeft && mx < sLeft + sWidth && my >= sy3 && my < sy3 + sh
            if (sov) r.drawBg(sLeft, sy3, sWidth, sh, 0f, colorUtil.rgba(255, 255, 255, 6))
            if (s == dropSetting && s is ColorSetting) pickerBoundsY = sy3 + 14f + 4f
            s.draw(r, sLeft, sy3, sWidth, th, mx, my, s == dropSetting)
            if (s == dropSetting && dropAnim.isPlaying()) {
                val da = dropAnim.value()
                r.drawRect(sLeft, sy3, sWidth, sh, 0f, colorUtil.rgba(255, 255, 255, (14 * da).toInt()))
            }
            sy3 += sh
        }

        val infoX = sLeft + sWidth + PAD
        val infoW = (panelW - (sLeft + sWidth + PAD) - px).coerceAtLeast(0f)
        if (infoW > 20f) {
            val infoY = sTop
            val infoH = panelH - TOP_BAR_H - PAD * 2
            r.drawBg(infoX, infoY, infoW, infoH, 4f, colorUtil.rgba(0, 0, 0, 60))
            r.drawText(Fonts.BOLD, locale.get("gui.info.title"), infoX + 6f, infoY + 6f, 10f, 0xFFFFFFFF.toInt())
            val modDesc = locale.get("module.${selMod.name.lowercase().replace(" ", "_")}.desc")
            val descLines = wrapText(modDesc, infoW - 12f, 8f, r)
            var descY = infoY + 20f
            for (line in descLines) {
                r.drawText(Fonts.REGULAR, line, infoX + 6f, descY, 8f, th.guiSettingText)
                descY += 10f
            }
        }
    }

    private fun drawQuestsContent(r: AporiaRenderer, th: Theme) {
        val cy = contentY
        val margin = PAD * 2f
        val aSize = 24f
        val titleX = px + margin + aSize + 8f

        val title = locale.get("gui.quests.title")
        r.drawText(Fonts.BOLD, title, titleX, cy + 2f, 14f, 0xFFFFFFFF.toInt())

        val listX = px + margin
        val listY = cy + 26f
        val listW = panelW - margin * 2f
        val cardH = 50f
        val gap = 6f
        val visibleH = panelH - (listY - py) - PAD

        val total = QuestManager.getAll().size
        val done = QuestManager.getAll().count { it.completed }
        val stats = "$done / $total completed"
        val sw = r.getTextWidth(Fonts.REGULAR, stats, 9f)
        r.drawText(Fonts.REGULAR, stats, listX + listW - sw, titleX + 4f, 9f, th.guiSettingValue)

        val scrollOffset = questScrollY
        val totalH = total * (cardH + gap)
        var qy = listY + scrollOffset

        for (q in QuestManager.getAll()) {
            val top = qy
            val bottom = qy + cardH
            if (bottom < listY || top > listY + visibleH) { qy += cardH + gap; continue }
            drawQuestCard(r, th, q, listX, qy, listW, cardH)
            qy += cardH + gap
        }

        if (totalH > visibleH) {
            val trackX = listX + listW - 3f
            val trackY = listY
            val trackH = visibleH
            r.drawRect(trackX, trackY, 2f, trackH, 1f, colorUtil.rgba(255, 255, 255, 30))
            val thumbH = (trackH * (visibleH / totalH)).coerceAtLeast(20f)
            val range = (totalH - visibleH).coerceAtLeast(1f)
            val thumbY = trackY + ((-scrollOffset / range) * (trackH - thumbH))
            r.drawRect(trackX, thumbY, 2f, thumbH, 1f, th.guiSettingValue)
        }
    }

    private fun drawQuestCard(r: AporiaRenderer, th: Theme, q: QuestManager.Quest, x: Float, y: Float, w: Float, h: Float) {
        r.drawBg(x, y, w, h, 4f, colorUtil.rgba(0, 0, 0, 90))
        r.drawRect(x, y, 3f, h, 1.5f, if (q.completed) th.guiEnabledDot else th.guiSettingValue)
        val title = q.type.displayName
        r.drawText(Fonts.BOLD, title, x + 10f, y + 6f, 11f, 0xFFFFFFFF.toInt())
        r.drawText(Fonts.REGULAR, q.description, x + 10f, y + 22f, 9f, if (q.completed) colorUtil.rgba(140, 220, 140, 230) else th.guiSettingText)
        val frac = q.percent
        val barX = x + 10f
        val barY = y + h - 12f
        val barW = w - 20f
        r.drawBg(barX, barY, barW, 4f, 2f, colorUtil.rgba(255, 255, 255, 30))
        r.drawBg(barX, barY, barW * frac, 4f, 2f, if (q.completed) th.guiEnabledDot else th.guiSettingValue)
        val progText = "${q.progress} / ${q.target}"
        val pw = r.getTextWidth(Fonts.REGULAR, progText, 8f)
        r.drawText(Fonts.REGULAR, progText, x + w - 10f - pw, y + 6f, 8f, th.guiSettingValue)
        q.reward?.let { reward ->
            r.drawText(Fonts.REGULAR, "\u2605 $reward", x + 10f + r.getTextWidth(Fonts.REGULAR, q.description, 9f) + 8f, y + 22f, 9f, 0xFFFFD700.toInt())
        }
    }

    private fun drawSettingsContent(r: AporiaRenderer, th: Theme, mx: Float, my: Float) {
        val cy = contentY
        val sWidth = panelW - PAD * 2

        r.drawText(Fonts.BOLD, locale.get("gui.tab.settings_title"), px + PAD, cy, 12f, 0xFFFFFFFF.toInt())

        var sy = cy + 20f
        for (s in clickGui.settings) {
            val sh = s.displayHeight(s == dropSetting)
            val sov = mx >= px + PAD && mx < px + PAD + sWidth && my >= sy && my < sy + sh
            if (sov) r.drawBg(px + PAD, sy, sWidth, sh, 2f, colorUtil.rgba(255, 255, 255, 6))
            if (s == dropSetting && s is ColorSetting) pickerBoundsY = sy + 14f + 4f
            s.draw(r, px + PAD, sy, sWidth, th, mx, my, s == dropSetting)
            if (s == dropSetting && dropAnim.isPlaying()) {
                val da = dropAnim.value()
                r.drawRect(px + PAD, sy, sWidth, sh, 0f, colorUtil.rgba(255, 255, 255, (14 * da).toInt()))
            }
            sy += sh
        }
    }

    override fun mouseClicked(e: MouseButtonEvent, b: Boolean): Boolean {
        val mx = e.x().toFloat()
        val my = e.y().toFloat()
        val btn = e.button()

        val slider = editingSlider
        if (slider != null) {
            val raw = slider.editBuffer
            val parsed = if (raw != null) raw.toDoubleOrNull() else null
            if (parsed != null) {
                val clamped = parsed.coerceIn(slider.min, slider.max)
                slider.setValue(clamped)
                if (Math.abs(parsed - clamped) > 0.0001) {
                    slider.editBuffer = java.lang.String.format("%.1f", clamped)
                }
            }
            slider.editing = false
        }
        editingSlider = null

        EventBus.post(MouseClickEvent(mx.toDouble(), my.toDouble(), btn, MouseClickEvent.Action.PRESS))

        if (bindMod != null) {
            if (btn in 0..2) return true
            val vk = if (btn in 0..7) 500 + btn else btn
            bindSet?.setKey(vk); bindMod!!.keybind = vk; bindSet = null; bindMod = null
            return true
        }

        if (handleTopBarClick(mx, my)) return true
        if (activeTab == Tab.SETTINGS) return handleSettingsTabClick(mx, my)
        if (activeTab == Tab.AVATAR) return handleAvatarClick(mx, my, btn)

        return super.mouseClicked(e, b)
    }

    private fun handleTopBarClick(mx: Float, my: Float): Boolean {
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
        var tabX = bx + PAD
        for (tab in leftTabs) {
            val tw = r.getTextWidth(Fonts.BOLD, tab.label, 10f) + 16f
            if (mx >= tabX && mx < tabX + tw && my >= by && my < by + TOP_BAR_H) {
                if (tab == Tab.BROWSER) {
                    mc.setScreen(WebviewScreen("https://google.com"))
                } else {
                    activeTab = tab
                    selCategory = 0; categoryAnim.reset(); categoryAnim.play()
                    selectedModule = null; scrollY = 0f; dropSetting = null; settingsScrollY = 0f
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

        return false
    }

    private fun handleSettingsTabClick(mx: Float, my: Float): Boolean {
        val sWidth = panelW - PAD * 2
        var sy = contentY + 20f
        for (s in clickGui.settings) {
            val sh = s.displayHeight(s == dropSetting)
            if (my >= sy && my < sy + sh) {
                handleSettingClick(s, mx, my, sy, px + PAD, sWidth)
                return true
            }
            sy += sh
        }
        return false
    }

    private fun handleAvatarClick(mx: Float, my: Float, btn: Int = 0): Boolean {
        val cy = contentY
        val catLeft = px + PAD - 2f
        var catY = cy + 10f
        val categories = Category.values().toList()
        for ((i, _) in categories.withIndex()) {
            if (mx >= catLeft && mx < catLeft + catW && my >= catY && my < catY + CAT_H) {
                if (i != selCategory) {
                    selCategory = i
                    lastSelCategory = i
                    categoryAnim.reset(); categoryAnim.play()
                    selectionSpring.snap(i.toFloat())
                }
                selectedModule = null; scrollY = 0f; dropSetting = null; settingsScrollY = 0f; return true
            }
            catY += CAT_H
        }

        val btnW2 = catW - PAD * 2
        val btnH2 = AVATAR_SIZE + 4f
        val btnX2 = px + PAD
        val btnY2 = py + panelH - PAD - btnH2
        if (mx >= btnX2 && mx < btnX2 + btnW2 && my >= btnY2 && my < btnY2 + btnH2) {
            showOtherPanel = !showOtherPanel
            return true
        }

        if (showOtherPanel) return false

        val mLeft = px + catW; val mTop = cy
        var my2 = mTop + scrollY
        val mods = ModuleManager.getByCategory(categories[selCategory])
        for (mod in mods) {
            if (my2 + MOD_H < mTop || my2 > py + panelH - PAD) { my2 += MOD_H; continue }
            if (mx >= mLeft && mx < mLeft + modW && my >= my2 && my < my2 + MOD_H) {
                when (btn) {
                    0 -> mod.toggle()
                    1 -> {
                        selectedModule = mod
                        settingsScrollY = 0f; dropSetting = null
                        moduleSwitchAnim.reset(); moduleSwitchAnim.play()
                    }
                    2 -> { bindMod = mod; bindSet = null }
                }
                return true
            }
            my2 += MOD_H
        }

        val sLeft = px + catW + modW
        val sTop = cy
        val sWidth = setW - PAD
        val selMod = selectedModule ?: return false

        var sy3 = sTop + MOD_H + PAD + settingsScrollY
        var lastCat = ""
        for (s in selMod.settings) {
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
            val sh = s.displayHeight(s == dropSetting)
            if (my >= sy3 && my < sy3 + sh) {
                handleSettingClick(s, mx, my, sy3, sLeft, sWidth)
                return true
            }
            sy3 += sh
        }

        return false
    }

    private fun handleSettingClick(s: Setting<*>, mx: Float, my: Float, sy: Float, sLeft: Float, sWidth: Float) {
        when (s) {
            is BooleanSetting -> { s.toggle() }
            is BindSetting -> { bindSet = s; bindMod = if (activeTab == Tab.SETTINGS) clickGui else selectedModule }
            is SliderSetting -> {
                val vb = s.valueBounds
                if (mx >= vb[0] && mx < vb[0] + vb[2] && my >= vb[1] && my < vb[1] + vb[3]) {
                    s.editing = true
                    s.editBuffer = java.lang.String.format("%.1f", s.get())
                    editingSlider = s
                } else {
                    val mod = if (activeTab == Tab.SETTINGS) clickGui else selectedModule
                    val sets = if (activeTab == Tab.SETTINGS) clickGui.settings else selectedModule!!.settings
                    dragSlider = DragInfo(mod!!, sets.indexOf(s))
                }
            }
            is SelectSetting -> handleSelectClick(s, mx, my, sy, sWidth, sLeft)
            is MultiSelectSetting -> handleMultiSelectClick(s, mx, my, sy, sWidth, sLeft)
            is ButtonSetting -> { s.click() }
            is ColorSetting -> handleColorPickerClick(s, mx, my, sy, sLeft, sWidth)
        }
    }

    private fun handleSelectClick(s: SelectSetting, mx: Float, my: Float, sy: Float, sw: Float, sl: Float) {
        val opts = s.getOptions(); if (opts.isEmpty()) return
        if (s == dropSetting) {
            val rel = my - sy - 14f - 2f; val idx = (rel / DH).toInt()
            if (idx in opts.indices) {
                s.setSelectedIndex(idx)
                dropSetting = null
            } else {
                if (mx < sl + sw * 0.4f) dropSetting = null
            }
        } else {
            if (mx < sl + sw * 0.4f) dropSetting = s
        }
    }

    private fun handleMultiSelectClick(s: MultiSelectSetting, mx: Float, my: Float, sy: Float, sw: Float, sl: Float) {
        val opts = s.getOptions(); if (opts.isEmpty()) return
        if (s == dropSetting) {
            val rel = my - sy - 14f - 2f; val idx = (rel / DH).toInt()
            if (idx in opts.indices) s.toggle(opts[idx]); else {
                if (mx < sl + sw * 0.4f) dropSetting = null
            }
        } else {
            if (mx < sl + sw * 0.4f) dropSetting = s
        }
    }

    private fun handleColorPickerClick(s: ColorSetting, mx: Float, my: Float, sy: Float, sLeft: Float, sWidth: Float) {
        if (s == dropSetting) {
            val pickerY = sy + 14f + 4f
            val padX = 8f
            val pX = sLeft + padX
            val svSize = 60f
            val gap = 4f
            val hueX = pX + svSize + gap
            val hueW = 8f
            val alphaY = pickerY + svSize + gap
            val pW = (sWidth - padX * 2f).coerceIn(100f, 160f)

            when {
                mx in pX..(pX + svSize) && my in pickerY..(pickerY + svSize) -> {
                    val sat = ((mx - pX) / svSize).coerceIn(0f, 1f)
                    val v = (1f - (my - pickerY) / svSize).coerceIn(0f, 1f)
                    s.setHSV(s.getHue(), sat, v, s.getA())
                    dragColorSetting = true
                    dragColorSV = true
                    dragColorSettingRef = s
                }
                mx in hueX..(hueX + hueW) && my in pickerY..(pickerY + svSize) -> {
                    val h = ((my - pickerY) / svSize).coerceIn(0f, 1f)
                    s.setHSV(h, s.getSaturation(), s.getValue(), s.getA())
                    dragColorHue = true
                    dragColorSettingRef = s
                }
                mx in pX..(pX + pW) && my in alphaY..(alphaY + 12f) -> {
                    val a = ((mx - pX) / pW).coerceIn(0f, 1f)
                    s.setA((a * 255).toInt())
                    dragColorAlpha = true
                    dragColorSettingRef = s
                }
                else -> { dropSetting = null }
            }
        } else {
            dropSetting = s
        }
    }

    override fun mouseReleased(e: MouseButtonEvent): Boolean {
        draggingWindow = false
        dragSlider = null
        dragColorSetting = false
        dragColorHue = false
        dragColorAlpha = false
        dragColorSV = false
        dragColorSettingRef = null
        return super.mouseReleased(e)
    }

    override fun mouseDragged(e: MouseButtonEvent, ddx: Double, ddy: Double): Boolean {
        val mx = e.x().toFloat(); val my = e.y().toFloat()

        if (draggingWindow && !maximized) {
            windowX = (mx - windowDragX).coerceIn(0f, width - panelW)
            windowY = (my - windowDragY).coerceIn(0f, height - panelH)
            return true
        }

        val ref = dragColorSettingRef
        if (ref != null) {
            val padX = 8f
            val pickerX = if (activeTab == Tab.SETTINGS) px + PAD + padX else px + catW + modW + padX
            val sw = if (activeTab == Tab.SETTINGS) panelW - PAD * 2 else setW - PAD - padX * 2f
            val pickerY = pickerBoundsY
            val svSize = 60f
            val barH = 12f
            val gap = 4f
            val hueX = pickerX + svSize + gap
            val hueW = 8f
            val alphaY = pickerY + svSize + gap

            if (dragColorSV) {
                val sat = ((mx - pickerX) / svSize).coerceIn(0f, 1f)
                val v = (1f - (my - pickerY) / svSize).coerceIn(0f, 1f)
                ref.setHSV(ref.getHue(), sat, v, ref.getA())
                return true
            }
            if (dragColorHue) {
                val h = ((my - pickerY) / svSize).coerceIn(0f, 1f)
                ref.setHSV(h, ref.getSaturation(), ref.getValue(), ref.getA())
                return true
            }
            if (dragColorAlpha) {
                val a = ((mx - pickerX) / sw.coerceAtMost(160f)).coerceIn(0f, 1f)
                ref.setA((a * 255).toInt())
                return true
            }
        }

        val info = dragSlider ?: return super.mouseDragged(e, ddx, ddy)
        if (e.button() != 0) return true
        val (mod, si) = info
        val sets = mod.settings; val s = sets.getOrNull(si) ?: run { dragSlider = null; return true }
        if (s !is SliderSetting) { dragSlider = null; return true }
        val sLeft = if (activeTab == Tab.SETTINGS) px + PAD else px + catW + modW
        val sWidth = if (activeTab == Tab.SETTINGS) panelW - PAD * 2 else setW - PAD
        val frac = ((mx - sLeft - 10f) / (sWidth - 20f)).coerceIn(0f, 1f)
        s.setValue(s.min + (s.max - s.min) * frac)
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
        for (s in m.settings) {
            if (s.category != lastCat) { lastCat = s.category; if (lastCat.isNotEmpty()) h += CAT_TITLE_H }
            h += s.displayHeight(s == dropSetting)
        }
        return h
    }

    override fun keyPressed(e: KeyEvent): Boolean {
        EventBus.post(KeyInputEvent(e.key(), e.scancode(), e.modifiers(), KeyInputEvent.Action.PRESS))

        if (editingSlider != null) {
            val s = editingSlider ?: return true
            val orig = java.lang.String.format("%.1f", s.get())
            when (e.key()) {
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    val buf = s.editBuffer ?: return true
                    val parsed = buf.toDoubleOrNull()
                    if (parsed != null) {
                        val clamped = parsed.coerceIn(s.min, s.max)
                        s.setValue(clamped)
                        if (Math.abs(parsed - clamped) > 0.0001) {
                            s.editBuffer = java.lang.String.format("%.1f", clamped)
                        }
                    }
                    s.editing = false; editingSlider = null
                }
                GLFW.GLFW_KEY_ESCAPE -> {
                    s.editing = false; editingSlider = null
                }
                GLFW.GLFW_KEY_BACKSPACE -> {
                    val buf = s.editBuffer ?: return true
                    if (buf.isNotEmpty()) s.editBuffer = buf.dropLast(1)
                }
                GLFW.GLFW_KEY_MINUS -> {
                    val buf = s.editBuffer ?: return true
                    if (buf == orig) s.editBuffer = "-"
                    else if (buf.isEmpty()) s.editBuffer = "-"
                    else if (buf == "-") {} else if ('-' !in buf) s.editBuffer = "-$buf"
                }
                GLFW.GLFW_KEY_PERIOD -> {
                    val buf = s.editBuffer ?: return true
                    if (buf == orig) s.editBuffer = "."
                    else if ('.' !in buf) s.editBuffer = buf + "."
                }
                in GLFW.GLFW_KEY_0..GLFW.GLFW_KEY_9 -> {
                    val buf = s.editBuffer ?: return true
                    val digit = '0' + (e.key() - GLFW.GLFW_KEY_0)
                    s.editBuffer = if (buf == orig) "$digit" else buf + digit
                }
                in GLFW.GLFW_KEY_KP_0..GLFW.GLFW_KEY_KP_9 -> {
                    val buf = s.editBuffer ?: return true
                    val digit = '0' + (e.key() - GLFW.GLFW_KEY_KP_0)
                    s.editBuffer = if (buf == orig) "$digit" else buf + digit
                }
            }
            return true
        }

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
    override fun onClose() {
        if (!closing) {
            closing = true
            openAnim.reverse()
            return
        }
        onCloseReal()
    }

    private fun onCloseReal() {
        ThemeManager.INSTANCE.saveAll()
        clickGui.savedCategory.setValue(selCategory.toDouble())
        clickGui.savedScroll.setValue(scrollY.toDouble())
        clickGui.savedSettingsScroll.setValue(settingsScrollY.toDouble())
        mc.setScreen(null)
    }

    private fun wrapText(text: String, maxWidth: Float, fontSize: Float, r: AporiaRenderer): List<String> {
        if (text.isEmpty()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val test = if (current.isEmpty()) word else "$current $word"
            if (r.getTextWidth(Fonts.REGULAR, test, fontSize) > maxWidth && current.isNotEmpty()) {
                lines.add(current)
                current = word
            } else {
                current = test
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }

    data class DragInfo(val mod: Module, val si: Int)
}
