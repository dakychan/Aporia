package so.aporia.utils.user.render.ui.clickgui

import so.aporia.utils.imports.*
import net.minecraft.resources.Identifier
import so.aporia.utils.user.render.core.AporiaRenderer
import com.chaos.annotation.Obfuscate
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.ModuleManager
import so.aporia.module.impl.misc.ClickGui
import so.aporia.module.settings.*
import so.aporia.utils.user.render.animation.*
import so.aporia.utils.user.render.font.Fonts
import so.aporia.module.impl.render.clickgui.ThemeManagerModule
import so.aporia.module.impl.render.clickgui.ThemeManagerModule.Theme
import org.lwjgl.glfw.GLFW
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.impl.KeyInputEvent
import so.aporia.utils.events.impl.MouseClickEvent
import com.chaos.annotation.ChaosNative

@Obfuscate
@OnlyIn(Dist.CLIENT)
@ChaosNative
class ClickGuiScreen(val clickGui: ClickGui) : Screen(Component.literal("ClickGui")) {

    enum class Tab(val localeKey: String) {
        AVATAR("gui.tab.avatar"), QUESTS("gui.tab.quests"),
        BROWSER("gui.tab.browser"), SETTINGS("gui.tab.settings");
        val label: String get() = locale.get(localeKey)
    }

    companion object {
        const val TOP_BAR_H = 24f; const val CORNER_RADIUS = 6f; const val PAD = 6f
        const val CAT_W_RATIO = 0.18f; const val MOD_W_RATIO = 0.37f
        const val CAT_H = 22f; const val MOD_H = 18f; const val CAT_TITLE_H = 18f
        const val DH = 9.8f; const val TAB_PAD = 4f; const val AVATAR_SIZE = 28f

        @JvmStatic fun settingCategory(name: String, hierarchy: Int) { SettingCategoryManager.push(name, hierarchy) }
        @JvmStatic fun settingCategoryEnd() { SettingCategoryManager.pop() }
    }

    // ─── State (all accessible to renderers) ─────────────
    var activeTab = Tab.AVATAR; internal set
    var selCategory = 0; internal set
    var selectedModule: Module? = null; internal set
    var scrollY = 0f; internal set
    var scrollVelocity = 0f; internal set
    var settingsScrollY = 0f; internal set
    var settingsScrollVelocity = 0f; internal set
    var bindSet: BindSetting? = null; internal set
    var bindMod: Module? = null; internal set
    var dragSlider: DragInfo? = null; internal set
    var dropSetting: Setting<*>? = null; internal set
    var editingSlider: SliderSetting? = null; internal set
    var dragColorSetting = false; internal set
    var dragColorHue = false; internal set
    var dragColorAlpha = false; internal set
    var dragColorSV = false; internal set
    var dragColorSettingRef: ColorSetting? = null; internal set
    val bindTypeAnim = TypeAnim(35, 60)

    var minimized = false; internal set
    var maximized = false; internal set
    var showOtherPanel = false; internal set
    val otherPanelSpring = SpringSimulator.snappy(0f)
    var windowX = 0f; internal set
    var windowY = 0f; internal set
    var windowDragX = 0f; internal set
    var windowDragY = 0f; internal set
    var draggingWindow = false; internal set

    val catExpanded = mutableMapOf<String, Boolean>()
    var questScrollY = 0f; internal set

    var pickerBoundsY = 0f; internal set

    val openAnim = Animator(220, Easing::sineOut)
    var closing = false; internal set
    val moduleSwitchAnim = Animator(250, Easing::cubicOut)
    val dropAnim = Animator(180, Easing::cubicOut)
    val categoryAnim = Animator(220, Easing::cubicOut)

    val hoverSprings = mutableMapOf<Int, SpringSimulator>()
    val selectionSpring = SpringSimulator(200f, 18f, 0f)
    var lastSelCategory = 0; internal set
    val catFluid = FluidAnim.suck()

    var logoId: Identifier? = null; internal set
    private var logoTextureView: Any? = null
    val categoryIcons = mutableMapOf<Category, Identifier?>()
    val catTypeAnims = Category.values().associateWith { TypeAnim(35, 60) }.toMutableMap()
    val catAnimStarted = Category.values().associateWith { false }.toMutableMap()
    val moduleTypeAnims = mutableMapOf<String, TypeAnim>()
    val moduleAnimStarted = mutableMapOf<String, Boolean>()
    val settingTypeAnims = mutableMapOf<String, TypeAnim>()
    val settingAnimStarted = mutableMapOf<String, Boolean>()
    var waveStartMs = 0L; internal set
    val waveDelayPerItem = 120L

    // ─── Layout ──────────────────────────────────────────
    val panelW get() = if (maximized) width.toFloat() else (width * 0.72f).coerceIn(500f, 900f)
    val panelH get() = if (maximized) height.toFloat() else (height * 0.72f).coerceIn(360f, 680f)
    val px get() = if (maximized) 0f else windowX
    val py get() = if (maximized) 0f else windowY
    val contentY get() = py + TOP_BAR_H + PAD
    val catW get() = panelW * CAT_W_RATIO
    val modW get() = panelW * MOD_W_RATIO
    val setW get() = panelW - catW - modW

    internal val useBlur: Boolean get() = clickGui.guiMode.getSelectedIndex() == 1
    val theme: Theme? get() = ThemeManagerModule.activeTheme()

    init {
        openAnim.snapTo(1f)
        selCategory = clickGui.savedCategory.get().toInt().coerceIn(0, Category.values().size - 1)
        scrollY = clickGui.savedScroll.get().toFloat()
        settingsScrollY = clickGui.savedSettingsScroll.get().toFloat()
        lastSelCategory = selCategory
    }

    override fun init() {
        val sw = width.toFloat(); val sh = height.toFloat()
        val pw = (sw * 0.72f).coerceIn(500f, 900f)
        windowX = (sw - pw) / 2f
        windowY = (sh - (sh * 0.72f).coerceIn(360f, 680f)) / 2f
        initCategoryIcons(); initLogo()
        waveStartMs = System.currentTimeMillis()
        for (cat in Category.values()) {
            catTypeAnims[cat]?.snap(locale.get("category.${cat.name.lowercase()}"))
            catAnimStarted[cat] = false
        }
        moduleTypeAnims.clear(); moduleAnimStarted.clear()
        settingTypeAnims.clear(); settingAnimStarted.clear()
    }

    private fun initCategoryIcons() {
        categoryIcons.clear()
        for (cat in Category.values()) {
            val path = so.aporia.utils.assets.AssetManager.getResourcePath(cat.texture)
            if (path != null) { categoryIcons[cat] = r.loadImage(path) }
        }
    }

    private fun initLogo() {
        logoId = null; logoTextureView = null
        val logoPath = so.aporia.utils.assets.AssetManager.getResourcePath(
            Identifier.fromNamespaceAndPath("aporia", "texture/gui/settings.png"))
        if (logoPath != null) {
            val id = r.loadImage(logoPath)
            if (id != null) { logoId = id; logoTextureView = mc.textureManager.getTexture(id) }
        }
    }

    // ─── Render ──────────────────────────────────────────

    override fun extractRenderState(g: GuiGraphicsExtractor, mx: Int, my: Int, d: Float) {
        ClickGuiScreenRenderer.render(r, this, mx, my)
    }

    // ─── Mouse ───────────────────────────────────────────

    override fun mouseClicked(e: MouseButtonEvent, b: Boolean): Boolean {
        val mx = e.x().toFloat(); val my = e.y().toFloat(); val btn = e.button()

        // Commit slider edit
        editingSlider?.let { s ->
            s.editBuffer?.toDoubleOrNull()?.let { parsed ->
                s.setValue(parsed.coerceIn(s.min, s.max).let { v -> kotlin.math.round(v / s.step) * s.step }.coerceIn(s.min, s.max))
            }
            s.editing = false
        }
        editingSlider = null

        EventBus.post(MouseClickEvent(mx.toDouble(), my.toDouble(), btn, MouseClickEvent.Action.PRESS))

        if (bindMod != null) {
            if (btn in 0..2) return true
            val vk = if (btn in 0..7) 500 + btn else btn
            bindSet?.setKey(vk); bindMod!!.keybind = vk; bindSet = null; bindMod = null
            return true
        }

        if (TopBarRenderer.handleClick(mx, my, this)) return true
        if (activeTab == Tab.SETTINGS) return ModulesRenderer.handleSettingsTabClick(mx, my, this)
        if (activeTab == Tab.AVATAR) return ModulesRenderer.handleAvatarClick(mx, my, btn, this)

        return super.mouseClicked(e, b)
    }

    override fun mouseReleased(e: MouseButtonEvent): Boolean {
        ClickGuiScreenRenderer.mouseReleased(this)
        return super.mouseReleased(e)
    }

    override fun mouseDragged(e: MouseButtonEvent, ddx: Double, ddy: Double): Boolean {
        return ClickGuiScreenRenderer.mouseDragged(e.x().toFloat(), e.y().toFloat(), this)
    }

    override fun mouseScrolled(mx: Double, my: Double, dX: Double, dY: Double): Boolean {
        return ClickGuiScreenRenderer.mouseScrolled(mx, my, dY, this)
    }

    // ─── Keyboard ────────────────────────────────────────

    override fun keyPressed(e: KeyEvent): Boolean {
        EventBus.post(KeyInputEvent(e.key(), e.scancode(), e.modifiers(), KeyInputEvent.Action.PRESS))

        editingSlider?.let { s ->
            val orig = java.lang.String.format(java.util.Locale.US, "%.1f", s.get())
            when (e.key()) {
                GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    s.editBuffer?.toDoubleOrNull()?.let { parsed ->
                        s.setValue(parsed.coerceIn(s.min, s.max).let { v -> kotlin.math.round(v / s.step) * s.step }.coerceIn(s.min, s.max))
                    }
                    s.editing = false; editingSlider = null
                }
                GLFW.GLFW_KEY_ESCAPE -> { s.editing = false; editingSlider = null }
                GLFW.GLFW_KEY_BACKSPACE -> { s.editBuffer = s.editBuffer?.dropLast(1) }
                GLFW.GLFW_KEY_MINUS -> {
                    val buf = s.editBuffer ?: return true
                    s.editBuffer = if (buf == orig) "-" else if (buf.isEmpty()) "-" else if (buf == "-") buf else if ('-' !in buf) "-$buf" else buf
                }
                GLFW.GLFW_KEY_PERIOD -> {
                    val buf = s.editBuffer ?: return true
                    s.editBuffer = if (buf == orig) "." else if ('.' !in buf) "$buf." else buf
                }
                in GLFW.GLFW_KEY_0..GLFW.GLFW_KEY_9 -> {
                    val buf = s.editBuffer ?: return true
                    val digit = '0' + (e.key() - GLFW.GLFW_KEY_0)
                    s.editBuffer = if (buf == orig) "$digit" else "$buf$digit"
                }
                in GLFW.GLFW_KEY_KP_0..GLFW.GLFW_KEY_KP_9 -> {
                    val buf = s.editBuffer ?: return true
                    val digit = '0' + (e.key() - GLFW.GLFW_KEY_KP_0)
                    s.editBuffer = if (buf == orig) "$digit" else "$buf$digit"
                }
            }
            return true
        }

        bindMod?.let {
            if (e.isEscape) { bindSet?.setKey(-1); it.keybind = -1 }
            else { bindSet?.setKey(e.scancode()); it.keybind = e.scancode() }
            bindSet = null; bindMod = null; return true
        }
        if (e.isEscape) { onClose(); return true }
        return super.keyPressed(e)
    }

    // ─── Lifecycle ───────────────────────────────────────

    override fun extractBackground(g: GuiGraphicsExtractor, mx: Int, my: Int, d: Float) {}
    override fun removed() {}
    override fun isPauseScreen() = false

    override fun onClose() {
        if (!closing) { closing = true; openAnim.reverse(); return }
        onCloseReal()
    }

    fun onCloseReal() {
        ThemeManagerModule.instance?.saveAll()
        clickGui.savedCategory.setValue(selCategory.toDouble())
        clickGui.savedScroll.setValue(scrollY.toDouble())
        clickGui.savedSettingsScroll.setValue(settingsScrollY.toDouble())
        mc.gui.setScreen(null)
    }
}

data class DragInfo(val mod: Module, val si: Int)

/** Draws a rectangle, optionally with blur if the screen's blur mode is active.
 *  cornerMask: bitmask for per-corner rounding (bit 0=TL, 1=TR, 2=BR, 3=BL, 15=all). */
fun AporiaRenderer.drawBg(screen: ClickGuiScreen?, x: Float, y: Float, w: Float, h: Float, radius: Float, color: Int, blurStrength: Float = 4f, cornerMask: Int = 15) {
    if (screen != null && screen.useBlur) drawRectBlurred(x, y, w, h, radius, color, blurStrength, cornerMask)
    else drawRect(x, y, w, h, radius, color, cornerMask)
}
