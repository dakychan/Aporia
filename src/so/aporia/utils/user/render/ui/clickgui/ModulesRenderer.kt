package so.aporia.utils.user.render.ui.clickgui

import so.aporia.utils.imports.*
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import so.aporia.utils.user.render.animation.SpringSimulator
import so.aporia.utils.user.render.animation.TypeAnim
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.ModuleManager
import so.aporia.module.settings.*
import so.aporia.module.impl.render.clickgui.ThemeManagerModule.Theme
import org.lwjgl.opengl.GL11

/**
 * Renders categories, module list, script modules, settings panel, quests, settings tab.
 * Also handles clicks on categories, modules, and settings.
 */
object ModulesRenderer {

    // ─── Categories ─────────────────────────────────────

    fun drawCategories(r: AporiaRenderer, th: Theme, screen: ClickGuiScreen, mx: Float, my: Float) {
        val cy = screen.contentY
        val categories = Category.values().toList()
        val now = System.currentTimeMillis()

        val catLeft = screen.px + ClickGuiScreen.PAD - 2f - 9f
        var catY = cy + 10f + 2f

        val catClipH = screen.py + screen.panelH - ClickGuiScreen.PAD - cy
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        val scScale = mc.window.guiScale.toFloat()
        val scClipY = ((mc.window.guiScaledHeight - (screen.py + screen.panelH - ClickGuiScreen.PAD)) * scScale).toInt()
        GL11.glScissor((screen.px * scScale).toInt(), scClipY,
            (screen.catW * scScale).toInt().coerceAtLeast(0),
            (catClipH * scScale).toInt().coerceAtLeast(0))

        for ((i, cat) in categories.withIndex()) {
            val ta = screen.catTypeAnims[cat] ?: continue
            val started = screen.catAnimStarted[cat] ?: false
            val elapsed = now - screen.waveStartMs
            if (!started && elapsed >= i * screen.waveDelayPerItem) {
                ta.setTarget(locale.get("category.${cat.name.lowercase()}"))
                screen.catAnimStarted[cat] = true
            }
            val label = if (started) ta.update() else ""

            val over = mx >= catLeft && mx < catLeft + screen.catW && my >= catY && my < catY + ClickGuiScreen.CAT_H
            val sel = i == screen.selCategory

            val springKey = 1000 + i
            val spring = screen.hoverSprings.getOrPut(springKey) { SpringSimulator(200f, 16f, 0f) }
            spring.setTarget(if (over || sel) 1f else 0f)
            spring.update(0.016f)
            val hoverFrac = spring.value()

            val catCenterX = catLeft + (screen.catW - ClickGuiScreen.PAD * 2) / 2f
            val iconSize = 12f
            val iconId = screen.categoryIcons[cat]

            if (sel) {
                r.drawBg(screen, catLeft, catY, screen.catW - ClickGuiScreen.PAD * 2, ClickGuiScreen.CAT_H, 3f, th.guiTitleBg)
            } else if (over || hoverFrac > 0.01f) {
                val bgAlpha = ((12 * hoverFrac).toInt().coerceIn(0, 255))
                r.drawBg(screen, catLeft, catY, screen.catW - ClickGuiScreen.PAD * 2, ClickGuiScreen.CAT_H, 3f,
                    colorUtil.rgba(255, 255, 255, bgAlpha))
            }
            if (sel) {
                val selColor = th.guiEnabledDot
                r.drawBg(screen, catLeft + screen.catW - ClickGuiScreen.PAD - 1f, catY + 3f, 2f, ClickGuiScreen.CAT_H - 6f, 1f,
                    colorUtil.rgba(selColor shr 16 and 0xFF, selColor shr 8 and 0xFF, selColor and 0xFF, selColor shr 24 and 0xFF))
            }

            if (iconId != null && mc.textureManager.getTexture(iconId) != null) {
                r.depth(1f)
                r.drawImage(catCenterX - iconSize / 2f, catY + (ClickGuiScreen.CAT_H - iconSize) / 2f, iconSize, iconSize, iconId)
                r.depth(0f)
            }
            val labelX = catCenterX + iconSize / 2f + 4f
            r.drawText(Fonts.REGULAR, label, labelX, catY + (ClickGuiScreen.CAT_H - 10f) / 2f - 1f, 10f,
                if (sel) 0xFFFFFFFF.toInt() else th.guiDisabledDot)
            catY += ClickGuiScreen.CAT_H
        }

        // "Other" button
        val btnW = screen.catW - ClickGuiScreen.PAD * 2
        val btnH = ClickGuiScreen.AVATAR_SIZE + 4f
        val btnX = catLeft
        val btnY = screen.py + screen.panelH - ClickGuiScreen.PAD - btnH
        r.drawText(Fonts.BOLD, locale.get("gui.other"),
            btnX + 8f, btnY + (btnH - 11f) / 2f - 1f, 11f, 0xFFAAAAAA.toInt())
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    // ─── Module List ─────────────────────────────────────

    fun drawModuleList(r: AporiaRenderer, th: Theme, screen: ClickGuiScreen, mx: Float, my: Float, cy: Float, categories: List<Category>) {
        screen.catFluid.update(0.016f)
        val fluidRunning = screen.catFluid.isRunning()
        val mLeft = screen.px + screen.catW
        val mTop = cy
        val mWidth = screen.modW
        val catAnim = screen.categoryAnim.value()
        val slideOff = (1f - catAnim) * 16f
        val mods = ModuleManager.getByCategory(categories[screen.selCategory])

        val modClipH = screen.py + screen.panelH - ClickGuiScreen.PAD - cy
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        val scScale = mc.window.guiScale.toFloat()
        val scModClipY = ((mc.window.guiScaledHeight - (screen.py + screen.panelH - ClickGuiScreen.PAD)) * scScale).toInt()
        GL11.glScissor((mLeft * scScale).toInt(), scModClipY,
            (mWidth * scScale).toInt().coerceAtLeast(0),
            (modClipH * scScale).toInt().coerceAtLeast(0))
        val altDown = org.lwjgl.glfw.GLFW.glfwGetKey(mc.window.handle(), org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT) == 1
        val now = System.currentTimeMillis()

        val dstBaseX = mLeft
        val dstBaseY = cy + screen.scrollY

        for ((mi, mod) in mods.withIndex()) {
            val dstX = dstBaseX
            val dstY = dstBaseY + mi * ClickGuiScreen.MOD_H

            val pos = if (fluidRunning) screen.catFluid.getPos(mi, dstX, dstY) else floatArrayOf(dstX, dstY)
            val drawX = pos[0] + slideOff
            val drawY = pos[1]
            val sc = if (fluidRunning) screen.catFluid.getScale(mi) else 1f

            if (drawY + ClickGuiScreen.MOD_H * sc < cy || drawY > screen.py + screen.panelH - ClickGuiScreen.PAD) continue

            val over = mx >= mLeft && mx < mLeft + mWidth && my >= drawY && my < drawY + ClickGuiScreen.MOD_H
            val en = mod.isEnabled
            val selMod = mod === screen.selectedModule

            val springKey = 3000 + mi
            val spring = screen.hoverSprings.getOrPut(springKey) { SpringSimulator(200f, 16f, 0f) }
            spring.setTarget(if (over || selMod) 1f else 0f)
            spring.update(0.016f)
            val hoverFrac = spring.value()

            val modKey = "module:${mod.name}"
            val modAnim = screen.moduleTypeAnims.getOrPut(modKey) { TypeAnim(35, 60) }
            val modStarted = screen.moduleAnimStarted.getOrPut(modKey) { false }
            val modElapsed = now - screen.waveStartMs
            val modDelay = (categories.size + mi) * screen.waveDelayPerItem
            val modName = if (!modStarted) {
                if (modElapsed >= modDelay) {
                    modAnim.setTarget(mod.name)
                    screen.moduleAnimStarted[modKey] = true
                }
                modAnim.update()
            } else modAnim.update()

            if (sc < 0.999f) {
                GL11.glPushMatrix()
                val cx = drawX + mWidth / 2f
                val cy2 = drawY + ClickGuiScreen.MOD_H / 2f
                GL11.glTranslatef(cx, cy2, 0f)
                GL11.glScalef(sc, sc, 1f)
                GL11.glTranslatef(-cx, -cy2, 0f)
            }

            r.drawBg(screen, drawX, drawY, mWidth, ClickGuiScreen.MOD_H, 2f, colorUtil.rgba(0, 0, 0, if (en) 90 else 55))
            if (selMod) {
                r.drawBg(screen, drawX, drawY, mWidth, ClickGuiScreen.MOD_H, 2f, th.guiTitleBg)
            } else if (over || hoverFrac > 0.01f) {
                val hlAlpha = ((12 * hoverFrac).toInt().coerceIn(0, 255))
                r.drawBg(screen, drawX, drawY, mWidth, ClickGuiScreen.MOD_H, 2f, colorUtil.rgba(255, 255, 255, hlAlpha))
            }
            if (en) r.drawBg(screen, drawX + 2f, drawY + 2f, 2f, ClickGuiScreen.MOD_H - 4f, 1f, th.guiEnabledDot)
            r.drawText(Fonts.REGULAR, modName, drawX + 8f, drawY + (ClickGuiScreen.MOD_H - 10f) / 2f - 1f, 10f,
                if (en) 0xFFFFFFFF.toInt() else (screen.theme?.guiDisabledDot ?: 0))
            if (altDown && mod.keybind != -1) {
                val keyName = so.aporia.utils.user.input.KeyCodeMap.getName(mod.keybind)
                val kw = r.getTextWidth(Fonts.REGULAR, keyName, 7f)
                r.drawText(Fonts.REGULAR, keyName, drawX + mWidth - kw - 4f, drawY + (ClickGuiScreen.MOD_H - 7f) / 2f, 7f, 0xFFAAAAAA.toInt())
            }
            if (selMod) r.drawText(Fonts.REGULAR, "\u25C0", drawX + mWidth - 12f, drawY + (ClickGuiScreen.MOD_H - 9f) / 2f - 1f, 9f, colorUtil.rgba(180, 180, 200, 200))

            if (sc < 0.999f) {
                GL11.glPopMatrix()
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    // ─── Script Module List ──────────────────────────────

    fun drawScriptModuleList(r: AporiaRenderer, th: Theme, screen: ClickGuiScreen, mx: Float, my: Float, cy: Float, categories: List<Category>) {
        val mLeft = screen.px + screen.catW
        val mTop = cy
        val mods = ModuleManager.getScriptModules().filter { it.category == categories[screen.selCategory] }
        var my2 = mTop + screen.scrollY
        for ((mi, mod) in mods.withIndex()) {
            if (my2 + ClickGuiScreen.MOD_H < mTop || my2 > screen.py + screen.panelH - ClickGuiScreen.PAD) { my2 += ClickGuiScreen.MOD_H; continue }
            val over = mx >= mLeft && mx < mLeft + screen.modW && my >= my2 && my < my2 + ClickGuiScreen.MOD_H
            val en = mod.isEnabled
            val selMod = mod === screen.selectedModule

            val springKey = 5000 + mods.indexOf(mod)
            val spring = screen.hoverSprings.getOrPut(springKey) { SpringSimulator(200f, 16f, 0f) }
            spring.setTarget(if (over || selMod) 1f else 0f)
            spring.update(0.016f)
            val hoverFrac = spring.value()

            r.drawBg(screen, mLeft, my2, screen.modW, ClickGuiScreen.MOD_H, 2f, colorUtil.rgba(0, 0, 0, if (en) 90 else 55))
            if (selMod) {
                r.drawBg(screen, mLeft, my2, screen.modW, ClickGuiScreen.MOD_H, 2f, th.guiTitleBg)
            } else if (over || hoverFrac > 0.01f) {
                val hlAlpha = ((12 * hoverFrac).toInt().coerceIn(0, 255))
                r.drawBg(screen, mLeft, my2, screen.modW, ClickGuiScreen.MOD_H, 2f, colorUtil.rgba(255, 255, 255, hlAlpha))
            }
            if (en) r.drawBg(screen, mLeft + 2f, my2 + 2f, 2f, ClickGuiScreen.MOD_H - 4f, 1f, th.guiEnabledDot)
            r.drawText(Fonts.REGULAR, mod.name, mLeft + 8f, my2 + (ClickGuiScreen.MOD_H - 10f) / 2f - 1f, 10f,
                if (en) 0xFFFFFFFF.toInt() else th.guiDisabledDot)
            if (selMod) r.drawText(Fonts.REGULAR, "\u25C0", mLeft + screen.modW - 12f, my2 + (ClickGuiScreen.MOD_H - 9f) / 2f - 1f, 9f, colorUtil.rgba(180, 180, 200, 200))
            my2 += ClickGuiScreen.MOD_H
        }
    }

    // ─── Settings Panel ──────────────────────────────────

    fun drawSettingsPanel(r: AporiaRenderer, th: Theme, screen: ClickGuiScreen, mx: Float, my: Float, cy: Float) {
        val sLeft = screen.px + screen.catW + screen.modW
        val sTop = cy
        val sWidth = screen.setW - ClickGuiScreen.PAD
        val selMod = screen.selectedModule ?: return

        val msAnim = screen.moduleSwitchAnim.value()
        val titleSlide = (1f - msAnim) * 10f
        r.drawBg(screen, sLeft, sTop, sWidth, ClickGuiScreen.MOD_H, 2f, th.guiTitleBg)
        val setNow = System.currentTimeMillis()
        val stDelay = (Category.values().size + ModuleManager.getByCategory(Category.values()[screen.selCategory]).size) * screen.waveDelayPerItem
        val stKey = "setting_title:${selMod.name}"
        val stAnim = screen.settingTypeAnims.getOrPut(stKey) { TypeAnim(35, 60) }
        val stStarted = screen.settingAnimStarted.getOrPut(stKey) { false }
        val stElapsed = setNow - screen.waveStartMs
        val modTitle = if (!stStarted) {
            if (stElapsed >= stDelay) {
                stAnim.setTarget(selMod.name)
                screen.settingAnimStarted[stKey] = true
            }
            stAnim.update()
        } else stAnim.update()
        r.drawText(Fonts.BOLD, modTitle, sLeft + 4f + titleSlide, sTop + (ClickGuiScreen.MOD_H - 11f) / 2f - 1f, 11f, colorUtil.rgba(255, 255, 255, (180 + 75 * msAnim).toInt()))

        var sy3 = sTop + ClickGuiScreen.MOD_H + ClickGuiScreen.PAD + screen.settingsScrollY
        var lastCat = ""
        for (s in selMod.settings) {
            if (s.category != lastCat) {
                lastCat = s.category
                if (lastCat.isNotEmpty()) {
                    val sh = ClickGuiScreen.CAT_TITLE_H
                    if (sy3 + sh < sTop || sy3 > screen.py + screen.panelH - ClickGuiScreen.PAD) { sy3 += sh; continue }
                    val exp = screen.catExpanded.getOrPut(lastCat) { true }
                    val over = mx >= sLeft && mx < sLeft + sWidth && my >= sy3 && my < sy3 + sh
                    if (over) r.drawBg(screen, sLeft, sy3, sWidth, sh, 0f, colorUtil.rgba(255, 255, 255, 10))
                    r.drawText(Fonts.BOLD, "${if (exp) "\u25BC" else "\u25B6"} $lastCat",
                        sLeft + 8f, sy3 + (sh - 10f) / 2f - 1f, 10f, th.guiSettingText)
                    sy3 += sh
                    if (!exp) continue
                }
            }
            val sh = s.displayHeight(s == screen.dropSetting)
            if (sy3 + sh < sTop || sy3 > screen.py + screen.panelH - ClickGuiScreen.PAD) { sy3 += sh; continue }
            val sov = mx >= sLeft && mx < sLeft + sWidth && my >= sy3 && my < sy3 + sh
            if (sov) r.drawBg(screen, sLeft, sy3, sWidth, sh, 0f, colorUtil.rgba(255, 255, 255, 6))
            if (s == screen.dropSetting && s is ColorSetting) screen.pickerBoundsY = sy3 + 14f + 4f
            s.draw(r, sLeft, sy3, sWidth, th, mx, my, s == screen.dropSetting)
            if (s == screen.dropSetting && screen.dropAnim.isPlaying()) {
                val da = screen.dropAnim.value()
                r.drawRect(sLeft, sy3, sWidth, sh, 0f, colorUtil.rgba(255, 255, 255, (14 * da).toInt()))
            }
            sy3 += sh
        }

        // Info panel
        val infoX = sLeft + sWidth + ClickGuiScreen.PAD
        val infoW = (screen.panelW - (sLeft + sWidth + ClickGuiScreen.PAD) - screen.px).coerceAtLeast(0f)
        if (infoW > 20f) {
            val infoY = sTop
            val infoH = screen.panelH - ClickGuiScreen.TOP_BAR_H - ClickGuiScreen.PAD * 2
            r.drawBg(screen, infoX, infoY, infoW, infoH, 4f, colorUtil.rgba(0, 0, 0, 60))
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

    // ─── Quests ──────────────────────────────────────────

    fun drawQuestsContent(r: AporiaRenderer, th: Theme, screen: ClickGuiScreen) {
        val cy = screen.contentY
        val margin = ClickGuiScreen.PAD * 2f

        val titleX = screen.px + margin + 24f + 8f
        r.drawText(Fonts.BOLD, locale.get("gui.quests.title"), titleX, cy + 2f, 14f, 0xFFFFFFFF.toInt())

        val listX = screen.px + margin
        val listY = cy + 26f
        val listW = screen.panelW - margin * 2f
        val cardH = 50f
        val gap = 6f
        val visibleH = screen.panelH - (listY - screen.py) - ClickGuiScreen.PAD

        val total = QuestManager.getAll().size
        val done = QuestManager.getAll().count { it.completed }
        val stats = "$done / $total completed"
        val sw = r.getTextWidth(Fonts.REGULAR, stats, 9f)
        r.drawText(Fonts.REGULAR, stats, listX + listW - sw, titleX + 4f, 9f, th.guiSettingValue)

        val scrollOffset = screen.questScrollY
        val totalH = total * (cardH + gap)
        var qy = listY + scrollOffset

        for (q in QuestManager.getAll()) {
            if (qy + cardH < listY || qy > listY + visibleH) { qy += cardH + gap; continue }
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
        r.drawBg(null, x, y, w, h, 4f, colorUtil.rgba(0, 0, 0, 90))
        r.drawRect(x, y, 3f, h, 1.5f, if (q.completed) th.guiEnabledDot else th.guiSettingValue)
        val title = q.type.displayName
        r.drawText(Fonts.BOLD, title, x + 10f, y + 6f, 11f, 0xFFFFFFFF.toInt())
        r.drawText(Fonts.REGULAR, q.description, x + 10f, y + 22f, 9f, if (q.completed) colorUtil.rgba(140, 220, 140, 230) else th.guiSettingText)
        val frac = q.percent
        val barX = x + 10f; val barY = y + h - 12f; val barW = w - 20f
        r.drawBg(null, barX, barY, barW, 4f, 2f, colorUtil.rgba(255, 255, 255, 30))
        r.drawBg(null, barX, barY, barW * frac, 4f, 2f, if (q.completed) th.guiEnabledDot else th.guiSettingValue)
        val progText = "${q.progress} / ${q.target}"
        val pw = r.getTextWidth(Fonts.REGULAR, progText, 8f)
        r.drawText(Fonts.REGULAR, progText, x + w - 10f - pw, y + 6f, 8f, th.guiSettingValue)
        q.reward?.let { reward ->
            r.drawText(Fonts.REGULAR, "\u2605 $reward", x + 10f + r.getTextWidth(Fonts.REGULAR, q.description, 9f) + 8f, y + 22f, 9f, 0xFFFFD700.toInt())
        }
    }

    // ─── Settings Tab ────────────────────────────────────

    fun drawSettingsContent(r: AporiaRenderer, th: Theme, screen: ClickGuiScreen, mx: Float, my: Float) {
        val cy = screen.contentY
        val sWidth = screen.panelW - ClickGuiScreen.PAD * 2

        r.drawText(Fonts.BOLD, locale.get("gui.tab.settings_title"), screen.px + ClickGuiScreen.PAD, cy, 12f, 0xFFFFFFFF.toInt())

        var sy = cy + 20f
        for (s in screen.clickGui.settings) {
            val sh = s.displayHeight(s == screen.dropSetting)
            val sov = mx >= screen.px + ClickGuiScreen.PAD && mx < screen.px + ClickGuiScreen.PAD + sWidth && my >= sy && my < sy + sh
            if (sov) r.drawBg(screen, screen.px + ClickGuiScreen.PAD, sy, sWidth, sh, 2f, colorUtil.rgba(255, 255, 255, 6))
            if (s == screen.dropSetting && s is ColorSetting) screen.pickerBoundsY = sy + 14f + 4f
            s.draw(r, screen.px + ClickGuiScreen.PAD, sy, sWidth, th, mx, my, s == screen.dropSetting)
            if (s == screen.dropSetting && screen.dropAnim.isPlaying()) {
                val da = screen.dropAnim.value()
                r.drawRect(screen.px + ClickGuiScreen.PAD, sy, sWidth, sh, 0f, colorUtil.rgba(255, 255, 255, (14 * da).toInt()))
            }
            sy += sh
        }
    }

    // ─── Click Handling ──────────────────────────────────

    fun handleAvatarClick(mx: Float, my: Float, btn: Int, screen: ClickGuiScreen): Boolean {
        val cy = screen.contentY
        val catLeft = screen.px + ClickGuiScreen.PAD - 2f - 9f
        var catY = cy + 10f + 2f
        val categories = Category.values().toList()
        for ((i, _) in categories.withIndex()) {
            if (mx >= catLeft && mx < catLeft + screen.catW && my >= catY && my < catY + ClickGuiScreen.CAT_H) {
                if (i != screen.selCategory) {
                    screen.selCategory = i
                    screen.lastSelCategory = i
                    screen.categoryAnim.reset(); screen.categoryAnim.play()
                    screen.selectionSpring.snap(i.toFloat())
                    screen.catFluid.setSource(catLeft + (screen.catW - ClickGuiScreen.PAD * 2) / 2f, catY + ClickGuiScreen.CAT_H / 2f)
                    screen.catFluid.reset()
                    screen.catFluid.trigger()
                }
                screen.selectedModule = null; screen.scrollY = 0f; screen.dropSetting = null; screen.settingsScrollY = 0f; return true
            }
            catY += ClickGuiScreen.CAT_H
        }

        // "Other" button
        val btnW = screen.catW - ClickGuiScreen.PAD * 2
        val btnH = ClickGuiScreen.AVATAR_SIZE + 4f
        val btnX = screen.px + ClickGuiScreen.PAD
        val btnY = screen.py + screen.panelH - ClickGuiScreen.PAD - btnH
        if (mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH) {
            screen.showOtherPanel = !screen.showOtherPanel
            screen.otherPanelSpring.setTarget(if (screen.showOtherPanel) 1f else 0f)
            return true
        }

        // Module list click
        val mLeft = screen.px + screen.catW; val mTop = cy
        var my2 = mTop + screen.scrollY
        val mods = if (screen.showOtherPanel)
            ModuleManager.getScriptModules().filter { it.category == categories[screen.selCategory] }
        else
            ModuleManager.getByCategory(categories[screen.selCategory])
        for (mod in mods) {
            if (my2 + ClickGuiScreen.MOD_H < mTop || my2 > screen.py + screen.panelH - ClickGuiScreen.PAD) { my2 += ClickGuiScreen.MOD_H; continue }
            if (mx >= mLeft && mx < mLeft + screen.modW && my >= my2 && my < my2 + ClickGuiScreen.MOD_H) {
                when (btn) {
                    0 -> mod.toggle()
                    1 -> {
                        screen.selectedModule = mod
                        screen.settingsScrollY = 0f; screen.dropSetting = null
                        screen.moduleSwitchAnim.reset(); screen.moduleSwitchAnim.play()
                    }
                    2 -> { screen.bindMod = mod; screen.bindSet = null }
                }
                return true
            }
            my2 += ClickGuiScreen.MOD_H
        }

        // Settings panel click
        val sLeft = screen.px + screen.catW + screen.modW
        val sTop = cy
        val sWidth = screen.setW - ClickGuiScreen.PAD
        val selMod = screen.selectedModule ?: return false

        var sy3 = sTop + ClickGuiScreen.MOD_H + ClickGuiScreen.PAD + screen.settingsScrollY
        var lastCat = ""
        for (s in selMod.settings) {
            if (s.category != lastCat) {
                lastCat = s.category
                if (lastCat.isNotEmpty()) {
                    val sh = ClickGuiScreen.CAT_TITLE_H
                    if (my >= sy3 && my < sy3 + sh) {
                        screen.catExpanded[lastCat] = !screen.catExpanded.getOrDefault(lastCat, true)
                        return true
                    }
                    sy3 += sh
                    if (!screen.catExpanded.getOrDefault(lastCat, true)) continue
                }
            }
            val sh = s.displayHeight(s == screen.dropSetting)
            if (my >= sy3 && my < sy3 + sh) {
                handleSettingClick(s, mx, my, sy3, sLeft, sWidth, screen)
                return true
            }
            sy3 += sh
        }

        return false
    }

    fun handleSettingsTabClick(mx: Float, my: Float, screen: ClickGuiScreen): Boolean {
        val sWidth = screen.panelW - ClickGuiScreen.PAD * 2
        var sy = screen.contentY + 20f
        for (s in screen.clickGui.settings) {
            val sh = s.displayHeight(s == screen.dropSetting)
            if (my >= sy && my < sy + sh) {
                handleSettingClick(s, mx, my, sy, screen.px + ClickGuiScreen.PAD, sWidth, screen)
                return true
            }
            sy += sh
        }
        return false
    }

    fun handleSettingClick(s: Setting<*>, mx: Float, my: Float, sy: Float, sLeft: Float, sWidth: Float, screen: ClickGuiScreen) {
        when (s) {
            is BooleanSetting -> { s.toggle() }
            is BindSetting -> { screen.bindSet = s; screen.bindMod = if (screen.activeTab == ClickGuiScreen.Tab.SETTINGS) screen.clickGui else screen.selectedModule }
            is SliderSetting -> {
                val vb = s.valueBounds
                if (mx >= vb[0] && mx < vb[0] + vb[2] && my >= vb[1] && my < vb[1] + vb[3]) {
                    s.editing = true
                    s.editBuffer = java.lang.String.format(java.util.Locale.US, "%.1f", s.get())
                    screen.editingSlider = s
                } else {
                    val mod = if (screen.activeTab == ClickGuiScreen.Tab.SETTINGS) screen.clickGui else screen.selectedModule
                    val sets = if (screen.activeTab == ClickGuiScreen.Tab.SETTINGS) screen.clickGui.settings else screen.selectedModule!!.settings
                    val frac = ((mx - sLeft - 10f) / (sWidth - 14f)).coerceIn(0f, 1f)
                    val v = s.min + (s.max - s.min) * frac
                    val stepped = kotlin.math.round(v / s.step) * s.step
                    s.setValue(stepped.coerceIn(s.min, s.max))
                    screen.dragSlider = DragInfo(mod!!, sets.indexOf(s))
                }
            }
            is SelectSetting -> handleSelectClick(s, mx, my, sy, sWidth, sLeft, screen)
            is MultiSelectSetting -> handleMultiSelectClick(s, mx, my, sy, sWidth, sLeft, screen)
            is ButtonSetting -> { s.click() }
            is ColorSetting -> handleColorPickerClick(s, mx, my, sy, sLeft, sWidth, screen)
        }
    }

    private fun handleSelectClick(s: SelectSetting, mx: Float, my: Float, sy: Float, sw: Float, sl: Float, screen: ClickGuiScreen) {
        val opts = s.getOptions(); if (opts.isEmpty()) return
        if (s == screen.dropSetting) {
            val rel = my - sy - 14f - 2f; val idx = (rel / ClickGuiScreen.DH).toInt()
            if (idx in opts.indices) { s.setSelectedIndex(idx); screen.dropSetting = null }
            else { if (mx < sl + sw * 0.4f) screen.dropSetting = null }
        } else {
            if (mx < sl + sw * 0.4f) screen.dropSetting = s
        }
    }

    private fun handleMultiSelectClick(s: MultiSelectSetting, mx: Float, my: Float, sy: Float, sw: Float, sl: Float, screen: ClickGuiScreen) {
        val opts = s.getOptions(); if (opts.isEmpty()) return
        if (s == screen.dropSetting) {
            val rel = my - sy - 14f - 2f; val idx = (rel / ClickGuiScreen.DH).toInt()
            if (idx in opts.indices) s.toggle(opts[idx])
            else { if (mx < sl + sw * 0.4f) screen.dropSetting = null }
        } else {
            if (mx < sl + sw * 0.4f) screen.dropSetting = s
        }
    }

    private fun handleColorPickerClick(s: ColorSetting, mx: Float, my: Float, sy: Float, sLeft: Float, sWidth: Float, screen: ClickGuiScreen) {
        if (s == screen.dropSetting) {
            val pickerY = sy + 14f + 4f
            val padX = 8f; val pX = sLeft + padX; val svSize = 60f; val gap = 4f
            val hueX = pX + svSize + gap; val hueW = 8f
            val alphaY = pickerY + svSize + gap
            val pW = (sWidth - padX * 2f).coerceIn(100f, 160f)

            when {
                mx in pX..(pX + svSize) && my in pickerY..(pickerY + svSize) -> {
                    val sat = ((mx - pX) / svSize).coerceIn(0f, 1f)
                    val v = (1f - (my - pickerY) / svSize).coerceIn(0f, 1f)
                    s.setHSV(s.getHue(), sat, v, s.getA())
                    screen.dragColorSetting = true; screen.dragColorSV = true; screen.dragColorSettingRef = s
                }
                mx in hueX..(hueX + hueW) && my in pickerY..(pickerY + svSize) -> {
                    val h = ((my - pickerY) / svSize).coerceIn(0f, 1f)
                    s.setHSV(h, s.getSaturation(), s.getValue(), s.getA())
                    screen.dragColorHue = true; screen.dragColorSettingRef = s
                }
                mx in pX..(pX + pW) && my in alphaY..(alphaY + 12f) -> {
                    val a = ((mx - pX) / pW).coerceIn(0f, 1f)
                    s.setA((a * 255).toInt())
                    screen.dragColorAlpha = true; screen.dragColorSettingRef = s
                }
                else -> { screen.dropSetting = null }
            }
        } else {
            screen.dropSetting = s
        }
    }

    // ─── Helpers ─────────────────────────────────────────

    fun getTotalSettingsHeight(m: Module, screen: ClickGuiScreen): Float {
        var h = 0f; var lastCat = ""
        for (s in m.settings) {
            if (s.category != lastCat) { lastCat = s.category; if (lastCat.isNotEmpty()) h += ClickGuiScreen.CAT_TITLE_H }
            h += s.displayHeight(s == screen.dropSetting)
        }
        return h
    }

    private fun wrapText(text: String, maxWidth: Float, fontSize: Float, r: AporiaRenderer): List<String> {
        if (text.isEmpty()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val test = if (current.isEmpty()) word else "$current $word"
            if (r.getTextWidth(Fonts.REGULAR, test, fontSize) > maxWidth && current.isNotEmpty()) {
                lines.add(current); current = word
            } else { current = test }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }
}
