package so.aporia.utils.user.render.ui.clickgui

import so.aporia.utils.imports.*
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import so.aporia.module.impl.render.clickgui.ThemeManagerModule.Theme

/**
 * Renders the top bar: logo, tab buttons, close/maximize/minimize controls.
 */
object TopBarRenderer {

    fun draw(r: AporiaRenderer, th: Theme, screen: ClickGuiScreen, mx: Float, my: Float) {
        val bx = screen.px; val by = screen.py; val bw = screen.panelW

        r.drawRect(bx, by, bw, ClickGuiScreen.TOP_BAR_H, ClickGuiScreen.CORNER_RADIUS, colorUtil.rgba(0, 0, 0, 60), cornerMask = 3)
        r.drawRect(bx, by + ClickGuiScreen.TOP_BAR_H - 1f, bw, 1f, 0f, th.guiSeparator)

        val logoSize = 16f; val logoPad = 4f
        val logoY = by + (ClickGuiScreen.TOP_BAR_H - logoSize) / 2f
        if (screen.logoId != null) {
            r.drawLogo(bx + logoPad, logoY, logoSize, logoSize, screen.logoId!!, System.currentTimeMillis())
        }

        val leftTabs = listOf(ClickGuiScreen.Tab.BROWSER, ClickGuiScreen.Tab.SETTINGS)
        var tabX = bx + ClickGuiScreen.PAD + logoSize + logoPad * 2
        for (tab in leftTabs) {
            val tw = r.getTextWidth(Fonts.BOLD, tab.label, 10f) + 16f
            val over = mx >= tabX && mx < tabX + tw && my >= by && my < by + ClickGuiScreen.TOP_BAR_H
            val active = tab == screen.activeTab
            if (over || active) {
                r.drawBg(screen, tabX, by + 2f, tw, ClickGuiScreen.TOP_BAR_H - 4f, 3f,
                    if (active) th.guiTitleBg else colorUtil.rgba(255, 255, 255, 12))
            }
            r.drawText(Fonts.BOLD, tab.label, tabX + 8f, by + (ClickGuiScreen.TOP_BAR_H - 10f) / 2f - 1f, 10f,
                if (active) 0xFFFFFFFF.toInt() else th.guiDisabledDot)
            tabX += tw + ClickGuiScreen.TAB_PAD
        }

        val ctrlY = by + (ClickGuiScreen.TOP_BAR_H - 12f) / 2f
        val ctrlR = 6f
        val closeX = bx + bw - ClickGuiScreen.PAD - ctrlR * 2 - 4f
        val maxX = closeX - ctrlR * 2 - 6f
        val minX = maxX - ctrlR * 2 - 6f

        // Avatar + Quests combined button
        var rightX = minX - ClickGuiScreen.TAB_PAD
        val avatarH = ClickGuiScreen.TOP_BAR_H - 6f
        val questW = r.getTextWidth(Fonts.BOLD, ClickGuiScreen.Tab.QUESTS.label, 10f) + 12f
        val combinedW = avatarH + 4f + questW + 4f
        rightX -= combinedW

        val isAvOrQs = screen.activeTab == ClickGuiScreen.Tab.AVATAR || screen.activeTab == ClickGuiScreen.Tab.QUESTS
        r.drawBg(screen, rightX, by + 2f, combinedW, ClickGuiScreen.TOP_BAR_H - 4f, 3f,
            if (isAvOrQs) th.guiTitleBg else colorUtil.rgba(255, 255, 255, 12))

        val skinId = mc.player?.skin?.body?.texturePath()
        if (skinId != null) {
            r.drawImageCropped(rightX + 2f, by + 2f, combinedW - 4f, ClickGuiScreen.TOP_BAR_H - 4f,
                skinId, (combinedW - 4f) / 2f, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f)
        } else {
            r.drawCircle(rightX + combinedW / 2f, by + ClickGuiScreen.TOP_BAR_H / 2f, (ClickGuiScreen.TOP_BAR_H - 6f) / 2f,
                if (isAvOrQs) th.guiTitleBg else colorUtil.rgba(255, 255, 255, 40))
        }

        // Close
        val closeOver = mx >= closeX && mx < closeX + ctrlR * 2 + 4f && my >= ctrlY && my < ctrlY + ctrlR * 2 + 4f
        r.drawCircle(closeX + ctrlR, ctrlY + ctrlR, ctrlR, if (closeOver) 0xFFFF5555.toInt() else 0xFFFF6666.toInt())
        if (closeOver) {
            r.drawLine(closeX + ctrlR - 2f, ctrlY + ctrlR - 2f, closeX + ctrlR + 2f, ctrlY + ctrlR + 2f, 1.5f, 0xFFFFFFFF.toInt())
            r.drawLine(closeX + ctrlR + 2f, ctrlY + ctrlR - 2f, closeX + ctrlR - 2f, ctrlY + ctrlR + 2f, 1.5f, 0xFFFFFFFF.toInt())
        }

        // Maximize
        val maxOver = mx >= maxX && mx < maxX + ctrlR * 2 + 4f && my >= ctrlY && my < ctrlY + ctrlR * 2 + 4f
        r.drawCircle(maxX + ctrlR, ctrlY + ctrlR, ctrlR, if (maxOver) 0xFFFFDD55.toInt() else 0xFFFFDD66.toInt())
        if (maxOver) {
            r.drawStroke(maxX + 2f, ctrlY + 2f, ctrlR * 2 - 4f, ctrlR * 2 - 4f, 1f, 1.2f, 0, 0f, 0xFFFFFFFF.toInt())
        }

        // Minimize
        val minOver = mx >= minX && mx < minX + ctrlR * 2 + 4f && my >= ctrlY && my < ctrlY + ctrlR * 2 + 4f
        r.drawCircle(minX + ctrlR, ctrlY + ctrlR, ctrlR, if (minOver) 0xFF66DD66.toInt() else 0xFF66EE66.toInt())
        if (minOver) {
            r.drawLine(minX + 2f, ctrlY + ctrlR, minX + ctrlR * 2 - 2f, ctrlY + ctrlR, 1.5f, 0xFFFFFFFF.toInt())
        }
    }

    fun handleClick(mx: Float, my: Float, screen: ClickGuiScreen): Boolean {
        val bx = screen.px; val by = screen.py; val bw = screen.panelW

        // Start window drag
        if (mx >= bx && mx < bx + bw && my >= by && my < by + ClickGuiScreen.TOP_BAR_H) {
            screen.draggingWindow = true
            screen.windowDragX = mx - screen.px
            screen.windowDragY = my - screen.py
        }

        val ctrlY = by + (ClickGuiScreen.TOP_BAR_H - 12f) / 2f
        val ctrlR = 6f
        val closeX = bx + bw - ClickGuiScreen.PAD - ctrlR * 2 - 4f
        val maxX = closeX - ctrlR * 2 - 6f
        val minX = maxX - ctrlR * 2 - 6f

        if (mx >= closeX && mx < closeX + ctrlR * 2 + 4f && my >= ctrlY && my < ctrlY + ctrlR * 2 + 4f) {
            screen.onClose(); return true
        }
        if (mx >= maxX && mx < maxX + ctrlR * 2 + 4f && my >= ctrlY && my < ctrlY + ctrlR * 2 + 4f) {
            screen.maximized = !screen.maximized
            if (screen.maximized) { screen.windowX = screen.px; screen.windowY = screen.py }
            return true
        }
        if (mx >= minX && mx < minX + ctrlR * 2 + 4f && my >= ctrlY && my < ctrlY + ctrlR * 2 + 4f) {
            screen.minimized = true
            screen.onClose()
            return true
        }

        // Left tabs (Browser, Settings)
        val leftTabs = listOf(ClickGuiScreen.Tab.BROWSER, ClickGuiScreen.Tab.SETTINGS)
        var tabX = bx + ClickGuiScreen.PAD
        for (tab in leftTabs) {
            val tw = r.getTextWidth(Fonts.BOLD, tab.label, 10f) + 16f
            if (mx >= tabX && mx < tabX + tw && my >= by && my < by + ClickGuiScreen.TOP_BAR_H) {
                if (tab == ClickGuiScreen.Tab.BROWSER) {
                    mc.gui.setScreen(aporia.webview.WebviewScreen("https://google.com"))
                } else {
                    screen.activeTab = tab
                    screen.selCategory = 0; screen.categoryAnim.reset(); screen.categoryAnim.play()
                    screen.selectedModule = null; screen.scrollY = 0f; screen.dropSetting = null; screen.settingsScrollY = 0f
                }
                return true
            }
            tabX += tw + ClickGuiScreen.TAB_PAD
        }

        // Avatar + Quests toggle
        val avatarH = ClickGuiScreen.TOP_BAR_H - 6f
        val questW = r.getTextWidth(Fonts.BOLD, ClickGuiScreen.Tab.QUESTS.label, 10f) + 12f
        val combinedW = avatarH + 4f + questW + 4f
        val combinedX = minX - ClickGuiScreen.TAB_PAD - combinedW
        if (mx >= combinedX && mx < combinedX + combinedW && my >= by && my < by + ClickGuiScreen.TOP_BAR_H) {
            screen.activeTab = if (screen.activeTab == ClickGuiScreen.Tab.AVATAR) ClickGuiScreen.Tab.QUESTS else ClickGuiScreen.Tab.AVATAR
            screen.selCategory = 0; screen.selectedModule = null; screen.scrollY = 0f; screen.dropSetting = null; screen.settingsScrollY = 0f
            return true
        }

        return false
    }
}
