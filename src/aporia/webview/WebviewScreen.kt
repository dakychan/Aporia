package aporia.webview

import so.aporia.utils.imports.*
import aporia.webview.platform.WebviewWin32
import aporia.webview.render.WebviewDirectTexture
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class WebviewScreen(url: String) : Screen(Component.literal("Webview")) {

    companion object {
        private val WEBVIEW_TEX_ID = Identifier.fromNamespaceAndPath("aporia", "webview/content")
        private val webviewTex = WebviewDirectTexture()
    }

    private val webview: Webview
    private var webviewReady = false

    // Layout
    private val toolbarH = 32
    private val btnSize = 20
    private val urlH = 22
    private val btnPad = 4
    private val radius = 5f
    private val tabDir: Path = files.ROOT.resolve(".webview/tabs")

    // Buttons + state
    private val homeId = Identifier.fromNamespaceAndPath("aporia", "texture/webview/home.png")
    private val addTabId = Identifier.fromNamespaceAndPath("aporia", "texture/webview/dobavit.png")
    private val bookmarksId = Identifier.fromNamespaceAndPath("aporia", "texture/webview/zakladki.png")

    private val urlText = StringBuilder(url)
    private var urlFocused = false
    private var urlCursorPos = url.length
    private var blinkTicker = 0
    private val tabs = mutableListOf(Tab(url, ""))
    private var currentTab = 0

    private var homeBtn = Rect(); private var urlBar = Rect()
    private var bookmarksBtn = Rect(); private var addTabBtn = Rect(); private var closeBtn = Rect()

    init {
        mc.textureManager.register(WEBVIEW_TEX_ID, webviewTex)
        val w = mc.window.guiScaledWidth
        val h = mc.window.guiScaledHeight - toolbarH
        webview = WebviewFactory.create(url, w, h)
        loadTabs()
    }

    data class Tab(val url: String, val title: String)
    data class Rect(var x: Int = 0, var y: Int = 0, var w: Int = 0, var h: Int = 0) {
        fun contains(mx: Int, my: Int) = mx in x until x + w && my in y until y + h
    }

    private fun loadTabs() {
        try { if (Files.notExists(tabDir)) return
            Files.list(tabDir).filter { it.toString().endsWith(".tab") }.sorted().forEach { f ->
                val lines = Files.readAllLines(f)
                if (lines.size >= 2) tabs.add(Tab(lines[0], lines[1]))
            }
            if (tabs.isNotEmpty()) currentTab = 0
        } catch (e: Exception) { logger.warn("Failed to load tabs: ${e.message}") }
    }

    private fun saveTabs() {
        try {
            Files.createDirectories(tabDir)
            tabs.forEachIndexed { i, tab ->
                val f = tabDir.resolve("tab_$i.tab")
                Files.writeString(f, "${tab.url}\n${tab.title}", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            }
        } catch (e: Exception) { logger.warn("Failed to save tabs: ${e.message}") }
    }

    override fun init() {
        super.init()
        if (!webview.initialized) {
            webviewReady = webview.init()
            logger.info("Webview init: ready=$webviewReady size=${width}x${height - toolbarH}")
            if (webviewReady) {
                webview.resize(width, height - toolbarH)
                webview.navigate(webview.url)
            }
        } else {
            webview.resize(width, height - toolbarH)
        }
    }

    private fun navigate(to: String) {
        val uri = if (to.contains(".") || to.startsWith("http")) to else "https://www.google.com/search?q=$to"
        webview.navigate(uri)
        tabs[currentTab] = tabs[currentTab].copy(url = uri)
        urlText.clear().append(uri); urlCursorPos = uri.length; urlFocused = false
        saveTabs()
    }

    private fun goHome() { navigate("https://google.com") }

    override fun renderBackground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {}

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {

        // ── Background ──
        r.drawRectBlurred(0f, 0f, width.toFloat(), height.toFloat(), 0f, 0x88000000.toInt(), 1f)

        // ── Web content via GuiGraphics.blit (MCEF-compatible path) ──
        if (webviewReady && webview is WebviewWin32) {
            val win = webview as WebviewWin32
            win.tickCapture()
            if (win.isTextureReady()) {
                val tex = win.getTexture()
                val view = tex.getTextureView()
                if (view != null && !view.isClosed()) {
                    webviewTex.setView(view)
                    val texW = tex.getWidth()
                    val texH = tex.getHeight()
                    if (texW > 0 && texH > 0) {
                        graphics.blit(RenderPipelines.GUI_TEXTURED, WEBVIEW_TEX_ID,
                            0, toolbarH, 0f, 0f, width, height - toolbarH, texW, texH)
                    }
                }
            }
        } else if (webviewReady) {
            r.drawRectBlurred(0f, toolbarH.toFloat(), width.toFloat(), (height - toolbarH).toFloat(), 0f,
                colorUtil.rgba(20, 20, 40, 200), 2f)
            r.drawText("regular", "WebView (placeholder)", 20f, toolbarH + 30f, 12f, colorUtil.rgba(200, 200, 220, 180))
        } else {
            r.drawRectBlurred(0f, toolbarH.toFloat(), width.toFloat(), (height - toolbarH).toFloat(), 0f,
                colorUtil.rgba(10, 10, 20, 200), 2f)
            r.drawText("regular", "Loading...", 20f, toolbarH + 30f, 12f, colorUtil.rgba(200, 200, 220, 120))
        }

        // ── Toolbar ──
        r.drawRectBlurred(0f, 0f, width.toFloat(), toolbarH.toFloat(), 0f, colorUtil.rgba(0, 0, 0, 170), 2f)
        val textY = (toolbarH - 9) / 2f

        homeBtn.x = btnPad; homeBtn.y = (toolbarH - btnSize) / 2; homeBtn.w = btnSize; homeBtn.h = btnSize
        val homeHover = homeBtn.contains(mouseX, mouseY)
        r.drawRectBlurred(homeBtn.x.toFloat(), homeBtn.y.toFloat(), btnSize.toFloat(), btnSize.toFloat(), radius,
            if (homeHover) colorUtil.rgba(100, 100, 200, 200) else colorUtil.rgba(60, 60, 100, 180), 2f)
        r.drawImage(homeBtn.x + 2f, homeBtn.y + 2f, (btnSize - 4).toFloat(), (btnSize - 4).toFloat(), homeId)

        closeBtn.x = width - btnPad - btnSize; closeBtn.y = (toolbarH - btnSize) / 2; closeBtn.w = btnSize; closeBtn.h = btnSize
        val closeHover = closeBtn.contains(mouseX, mouseY)
        r.drawRectBlurred(closeBtn.x.toFloat(), closeBtn.y.toFloat(), btnSize.toFloat(), btnSize.toFloat(), radius,
            if (closeHover) colorUtil.rgba(200, 60, 60, 200) else colorUtil.rgba(100, 60, 60, 180), 2f)
        r.drawText("regular", "X", closeBtn.x + (btnSize - 5) / 2f, textY, 9f,
            if (closeHover) 0xFFFFFFFF.toInt() else colorUtil.rgba(255, 255, 255, 180))

        addTabBtn.x = closeBtn.x - btnSize - btnPad; addTabBtn.y = (toolbarH - btnSize) / 2; addTabBtn.w = btnSize; addTabBtn.h = btnSize
        val addHover = addTabBtn.contains(mouseX, mouseY)
        r.drawRectBlurred(addTabBtn.x.toFloat(), addTabBtn.y.toFloat(), btnSize.toFloat(), btnSize.toFloat(), radius,
            if (addHover) colorUtil.rgba(100, 200, 100, 200) else colorUtil.rgba(60, 100, 60, 180), 2f)
        r.drawImage(addTabBtn.x + 2f, addTabBtn.y + 2f, (btnSize - 4).toFloat(), (btnSize - 4).toFloat(), addTabId)

        bookmarksBtn.x = addTabBtn.x - btnSize - btnPad; bookmarksBtn.y = (toolbarH - btnSize) / 2; bookmarksBtn.w = btnSize; bookmarksBtn.h = btnSize
        val bkmkHover = bookmarksBtn.contains(mouseX, mouseY)
        r.drawRectBlurred(bookmarksBtn.x.toFloat(), bookmarksBtn.y.toFloat(), btnSize.toFloat(), btnSize.toFloat(), radius,
            if (bkmkHover) colorUtil.rgba(100, 100, 200, 200) else colorUtil.rgba(60, 60, 100, 180), 2f)
        r.drawImage(bookmarksBtn.x + 2f, bookmarksBtn.y + 2f, (btnSize - 4).toFloat(), (btnSize - 4).toFloat(), bookmarksId)

        // URL bar
        val leftEdge = homeBtn.x + homeBtn.w + btnPad * 3
        val rightEdge = bookmarksBtn.x - btnPad * 3
        urlBar.x = leftEdge; urlBar.y = (toolbarH - urlH) / 2
        urlBar.w = (rightEdge - leftEdge).coerceAtLeast(80); urlBar.h = urlH
        val urlHover = urlBar.contains(mouseX, mouseY)
        val urlBg = if (urlFocused) colorUtil.rgba(30, 30, 50, 220) else if (urlHover) colorUtil.rgba(50, 50, 80, 200) else colorUtil.rgba(40, 40, 70, 190)
        r.drawRectBlurred(urlBar.x.toFloat(), urlBar.y.toFloat(), urlBar.w.toFloat(), urlBar.h.toFloat(), radius, urlBg, 2f)

        val displayUrl = urlText.toString()
        val maxChars = urlBar.w / 6 - 1
        val shownUrl = if (displayUrl.length > maxChars) "\u2026${displayUrl.takeLast(maxChars - 1)}" else displayUrl
        r.drawText("regular", shownUrl, urlBar.x + 6f, textY, 9f, colorUtil.rgba(220, 220, 255, 220))

        if (urlFocused && (blinkTicker % 40 < 20)) {
            val cursorX = urlBar.x + 6f + minOf(urlCursorPos, shownUrl.length) * 6f
            r.drawRect(cursorX, urlBar.y + 4f, 1f, (urlH - 8).toFloat(), 0f, 0xFFFFFFFF.toInt())
        }
    }

    // ── Input: toolbar clicks handled here, content clicks forwarded to WebView2 ──

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        val mx = event.x().toInt(); val my = event.y().toInt()
        if (closeBtn.contains(mx, my)) { onClose(); return true }
        if (homeBtn.contains(mx, my)) { goHome(); return true }
        if (bookmarksBtn.contains(mx, my)) { return true }
        if (addTabBtn.contains(mx, my)) { tabs.add(Tab("about:blank", "")); currentTab = tabs.size - 1; navigate("about:blank"); saveTabs(); return true }
        if (urlBar.contains(mx, my)) { urlFocused = true; urlCursorPos = urlText.length; blinkTicker = 0; return true }
        if (my < toolbarH) { urlFocused = false; return true }
        // Content click → forward to WebView2
        urlFocused = false
        if (webviewReady) {
            val contentY = my - toolbarH
            if (contentY >= 0) {
                webview.sendMouseMove(mx, contentY)
                webview.sendMousePress(mx, contentY, event.button())
                webview.sendMouseRelease(mx, contentY, event.button())
            }
        }
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.y().toInt() < toolbarH) return true
        if (webviewReady) {
            val mx = event.x().toInt(); val my = event.y().toInt() - toolbarH
            webview.sendMouseRelease(mx, my, event.button())
        }
        return true
    }

    override fun mouseDragged(event: MouseButtonEvent, ddx: Double, ddy: Double): Boolean {
        if (webviewReady) {
            val mx = event.x().toInt(); val my = event.y().toInt() - toolbarH
            webview.sendMouseMove(mx, my)
        }
        return true
    }

    override fun mouseScrolled(mx: Double, my: Double, dX: Double, dY: Double): Boolean {
        if (my < toolbarH) return false
        if (webviewReady) webview.sendMouseWheel(dY)
        return true
    }

    override fun mouseMoved(mx: Double, my: Double) {
        if (webviewReady) {
            val cy = my.toInt() - toolbarH
            if (cy >= 0) webview.sendMouseMove(mx.toInt(), cy)
        }
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == 256) {
            if (urlFocused) { urlFocused = false; urlText.clear().append(tabs[currentTab].url); urlCursorPos = urlText.length; return true }
            onClose(); return true
        }
        if (urlFocused) {
            when (event.key()) {
                259 -> { if (urlCursorPos > 0) { urlText.deleteAt(urlCursorPos - 1); urlCursorPos-- } }
                257 -> { navigate(urlText.toString()) }
                263 -> { if (urlCursorPos > 0) urlCursorPos-- }
                262 -> { if (urlCursorPos < urlText.length) urlCursorPos++ }
                268 -> { urlCursorPos = 0 }
                269 -> { urlCursorPos = urlText.length }
            }
            blinkTicker = 0; return true
        }
        if (webviewReady) { webview.sendKeyPress(event.key(), event.scancode(), event.modifiers()); return true }
        return super.keyPressed(event)
    }

    override fun keyReleased(event: KeyEvent): Boolean {
        if (urlFocused) return true
        if (webviewReady) { webview.sendKeyRelease(event.key(), event.scancode(), event.modifiers()); return true }
        return super.keyReleased(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        val cp = event.codepoint()
        if (cp == 0) return true
        if (urlFocused) { urlText.insert(urlCursorPos, cp.toChar()); urlCursorPos++; blinkTicker = 0; return true }
        if (webviewReady) { webview.sendKeyTyped(cp.toChar(), event.modifiers()); return true }
        return super.charTyped(event)
    }

    override fun tick() { super.tick(); blinkTicker++ }

    override fun removed() {
        webview.close(); super.removed()
    }

    override fun onClose() {
        saveTabs(); super.onClose()
    }

    override fun shouldCloseOnEsc() = false
    override fun isPauseScreen() = false
}
