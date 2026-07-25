package so.aporia.utils.user.render.ui.chat

import com.chaos.annotation.ChaosNative
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.components.CommandSuggestions
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import so.aporia.module.impl.render.Beautifully
import so.aporia.module.impl.render.clickgui.ThemeManagerModule
import so.aporia.module.settings.*
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.impl.KeyInputEvent
import so.aporia.utils.events.impl.MouseClickEvent
import so.aporia.utils.events.impl.MouseScrollEvent
import so.aporia.utils.files.impl.ChatFile
import so.aporia.utils.user.command.CommandManager
import so.aporia.utils.user.input.KeyboardLayout
import so.aporia.utils.user.logger.Logger
import so.aporia.utils.user.render.animation.*
import so.aporia.utils.user.render.color.ColorUtil
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.events.StateMachineEngine
import aporia.cc.PanicSystem
import net.minecraft.client.multiplayer.chat.GuiMessage

/**
 * Custom chat screen replacing vanilla ChatScreen.
 * Supports multiple chat windows, per-window prefix/suffix, message routing,
 * search, resize/drag, context menu and inline settings panel.
 */
@OnlyIn(Dist.CLIENT)
@ChaosNative
class ChatScreenBackendApi(initial: String?, isDraft: Boolean) : ChatScreen(
    if (initial != null && initial.length == 1 && initial[0].isLetter()) "" else (initial ?: ""),
    isDraft
) {

    // ── Window configuration ──

    class WinCfg(
        var name: String,
        var x: Int, var bottomY: Int, var w: Int, var h: Int,
        var draggable: Boolean = false,
        var searchOnOpen: Boolean = false,
        var showOnlyFilter: Boolean = false,
        var showOnlyServer: Boolean = false,
        var prefixEnabled: Boolean = true,
        var filterWords: String = "",
        var msgPrefix: String = "",
        var msgSuffix: String = "",
        var prefixTriggers: String = "",
        var selfColor: Int = 0xFFADD8E6.toInt(),
        var scrollOffset: Int = 0,
        var customTextEnabled: Boolean = false,
        var customTextPrefix: String = ""
    ) {
        val lines = ArrayDeque<GuiMessage.Line>(100)
        val nameAnim = TypeAnim(40, 100)
        val switchAnim = Animator(300, Easing::cubicOut)
        @Transient val editSettings = mutableListOf<Setting<*>>()

        init {
            nameAnim.snap(name)
            editSettings.addAll(listOf(
                TextSetting("Name", "Window name", name),
                TextSetting("Prefix", "Message prefix", msgPrefix),
                BooleanSetting("Prefix enabled", "Enable prefix", prefixEnabled),
                TextSetting("Prefix triggers", "Trigger chars", prefixTriggers),
                TextSetting("Suffix", "Message suffix", msgSuffix),
                TextSetting("Filter words", "Keyword filter", filterWords),
                BooleanSetting("Filter only", "Show filtered only", showOnlyFilter),
                BooleanSetting("Search on open", "Search when opened", searchOnOpen),
                BooleanSetting("Only server", "Server messages only", showOnlyServer),
                ColorSetting("Self color", "Own message color", selfColor),
                BooleanSetting("CustomText", "Rewrite name prefix", customTextEnabled),
                TextSetting("Custom prefix", "Custom text before name", customTextPrefix)
            ))
        }

        fun maxLines() = maxOf(1, (h - BOX_PAD * 2) / LINE_H)

        fun addLine(l: GuiMessage.Line) {
            lines.addFirst(l)
            while (lines.size > MAX_STORED) lines.removeLast()
        }

        fun clear() { lines.clear(); scrollOffset = 0 }

        fun syncToWinCfg() {
            editSettings.getOrNull(0)?.let { if (it is TextSetting) name = it.get() }
            editSettings.getOrNull(1)?.let { if (it is TextSetting) msgPrefix = it.get() }
            editSettings.getOrNull(2)?.let { if (it is BooleanSetting) prefixEnabled = it.isEnabled }
            editSettings.getOrNull(3)?.let { if (it is TextSetting) prefixTriggers = it.get() }
            editSettings.getOrNull(4)?.let { if (it is TextSetting) msgSuffix = it.get() }
            editSettings.getOrNull(5)?.let { if (it is TextSetting) filterWords = it.get() }
            editSettings.getOrNull(6)?.let { if (it is BooleanSetting) showOnlyFilter = it.isEnabled }
            editSettings.getOrNull(7)?.let { if (it is BooleanSetting) searchOnOpen = it.isEnabled }
            editSettings.getOrNull(8)?.let { if (it is BooleanSetting) showOnlyServer = it.isEnabled }
            editSettings.getOrNull(9)?.let { if (it is ColorSetting) selfColor = it.get() }
            editSettings.getOrNull(10)?.let { if (it is BooleanSetting) customTextEnabled = it.isEnabled }
            editSettings.getOrNull(11)?.let { if (it is TextSetting) customTextPrefix = it.get() }
        }

        fun syncFromWinCfg() {
            (editSettings.getOrNull(0) as? TextSetting)?.set(name)
            (editSettings.getOrNull(1) as? TextSetting)?.set(msgPrefix)
            (editSettings.getOrNull(2) as? BooleanSetting)?.set(prefixEnabled)
            (editSettings.getOrNull(3) as? TextSetting)?.set(prefixTriggers)
            (editSettings.getOrNull(4) as? TextSetting)?.set(msgSuffix)
            (editSettings.getOrNull(5) as? TextSetting)?.set(filterWords)
            (editSettings.getOrNull(6) as? BooleanSetting)?.set(showOnlyFilter)
            (editSettings.getOrNull(7) as? BooleanSetting)?.set(searchOnOpen)
            (editSettings.getOrNull(8) as? BooleanSetting)?.set(showOnlyServer)
            (editSettings.getOrNull(9) as? ColorSetting)?.set(selfColor)
            (editSettings.getOrNull(10) as? BooleanSetting)?.set(customTextEnabled)
            (editSettings.getOrNull(11) as? TextSetting)?.set(customTextPrefix)
        }

        companion object {
            private const val MAX_STORED = 200
            const val BOX_PAD = 4
            const val LINE_H = 10
            const val MAX_LINES = 10
            const val LEFT = 2
            const val BOTTOM_PAD = 2
            const val INPUT_H = 14
            const val RADIUS = 5
            const val GAP = 3
            const val TEXT_PAD = 8
            const val MIN_W = 80
            const val RESIZE_HIT = 6
            const val DRAG_GRIP = 8
        }
    }

    // ── Window manager ──

    object WinMgr {
        @JvmField val wins = mutableListOf<WinCfg>()
        @JvmField var active = 0

        init { wins.add(makeWin("Main", 0)) }

        fun get() = wins[active]

        fun add(name: String) {
            wins.add(makeWin(name, wins.size))
            active = wins.size - 1
        }

        fun remove(i: Int) {
            if (wins.size <= 1) return
            wins.removeAt(i)
            active = active.coerceIn(0, wins.size - 1)
        }

        @JvmStatic
        fun route(msg: GuiMessage, font: net.minecraft.client.gui.Font) {
            val full = msg.content().string.lowercase()
            val hasInteraction = booleanArrayOf(false)
            msg.content().visit({ style, _ ->
                if (style.clickEvent != null) hasInteraction[0] = true
                java.util.Optional.empty<Boolean>()
            }, net.minecraft.network.chat.Style.EMPTY)
            val isServerMessage = if (hasInteraction[0]) false else msg.tag() != null

            val claimants = wins.filter { w -> w.showOnlyFilter && w.filterWords.isNotEmpty() &&
                splitTokens(w.filterWords).any { matchesKw(full, it) }
            }

            var routed = false
            for (w in wins) {
                val isFiltered = w.showOnlyFilter && w.filterWords.isNotEmpty()
                val claimed = w in claimants
                if (isFiltered && !claimed) continue
                if (!isFiltered && claimants.isNotEmpty()) continue
                if (w.showOnlyServer && !isServerMessage) continue
                addMsgToWin(w, msg, font)
                routed = true
            }
            if (!routed && !isServerMessage && wins.isNotEmpty()) {
                addMsgToWin(wins[0], msg, font)
            }
        }

        private fun addMsgToWin(w: WinCfg, msg: GuiMessage, font: net.minecraft.client.gui.Font) {
            var content = msg.content()
            if (w.customTextEnabled && w.customTextPrefix.isNotEmpty()) {
                val p = net.minecraft.client.Minecraft.getInstance().player
                if (p != null) content = transformCustomText(content, w.customTextPrefix, p.name.string)
            }
            val splitW = maxOf(10, w.w - BOX_PAD * 2 - TEXT_PAD)
            val modifiedMsg = GuiMessage(msg.addedTime(), content, msg.signature(), msg.source(), msg.tag())
            val parts = modifiedMsg.splitLines(font, splitW)
            for (i in parts.indices) w.addLine(GuiMessage.Line(modifiedMsg, parts[i], i == parts.size - 1))
        }

        private fun transformCustomText(content: net.minecraft.network.chat.Component, customPrefix: String, playerName: String): net.minecraft.network.chat.Component {
            val plain = content.string
            val nameIdx = plain.lowercase().indexOf(playerName.lowercase())
            if (nameIdx <= 0) return content
            var sepIdx = plain.indexOf('>', nameIdx + playerName.length)
            if (sepIdx < 0) sepIdx = plain.indexOf(':', nameIdx + playerName.length)
            if (sepIdx < 0) sepIdx = nameIdx + playerName.length
            val after = plain.substring(sepIdx + 1)
            return net.minecraft.network.chat.Component.literal(customPrefix + playerName + ">" + after)
        }

        @JvmStatic
        fun rebuild(chat: ChatComponent, font: net.minecraft.client.gui.Font) {
            wins.forEach { it.clear() }
            val all = chat.allMessages
            for (i in all.indices.reversed()) route(all[i], font)
        }

        private fun matchesKw(text: String, kw: String): Boolean {
            if (kw.isEmpty()) return false
            val v = KeyboardLayout.both(kw.lowercase())
            return text.contains(v[0]) || text.contains(v[1])
        }

        private fun splitTokens(filterWords: String): List<String> =
            filterWords.split("[,\\s]+".toRegex()).filter { it.isNotBlank() }

        private fun makeWin(name: String, idx: Int): WinCfg {
            val defW = 320; val defH = WinCfg.MAX_LINES * WinCfg.LINE_H + WinCfg.BOX_PAD * 2
            return when (idx) {
                0 -> WinCfg(name, WinCfg.LEFT, WinCfg.BOTTOM_PAD + WinCfg.INPUT_H + WinCfg.GAP, defW, defH, false)
                else -> WinCfg(name, -1, -1, defW, defH, true)
            }
        }
    }

    // ── State machine ──

    enum class ScreenState { IDLE, SEARCH, EDIT, CONTEXT, SELECTING, SELECTED }
    private val screenState = StateMachineEngine(this, ScreenState::class, ScreenState.IDLE)

    // ── Context menu ──

    private class CtxMenu {
        var visible = false; var renderX = 0; var renderY = 0
        companion object {
            const val W = 160; const val ITEM = 14; const val PAD = 6
            val C_BG = ColorUtil.rgba(18, 18, 30, 235)
            val C_ITEM = ColorUtil.rgba(255, 255, 255, 200)
            val C_HOV = ColorUtil.rgba(80, 80, 160, 180)
            val C_SEP = ColorUtil.rgba(255, 255, 255, 40)
        }
        fun open(x: Int, y: Int, screenW: Int, screenH: Int) {
            visible = true
            val totalH = menuHeight(WinMgr.wins.size)
            renderX = if (x + W > screenW) x - W else x
            renderY = if (y + totalH > screenH) y - totalH else y
        }
        fun close() { visible = false }
        private fun menuHeight(wc: Int) = PAD * 2 + 3 * ITEM + if (wc > 0) 4 + wc * ITEM else 0
        fun itemAt(px: Double, py: Double, wc: Int): Int {
            if (!visible) return -1
            val totalH = menuHeight(wc)
            if (px < renderX || px > renderX + W || py < renderY || py > renderY + totalH) return -1
            return ((py - renderY - PAD).toInt() / ITEM).coerceIn(0, 2 + wc)
        }
        fun render(gfx: GuiGraphicsExtractor, font: net.minecraft.client.gui.Font, wc: Int) {
            if (!visible) return
            val r = AporiaRenderer
            r.drawRect(renderX.toFloat(), renderY.toFloat(), W.toFloat(), menuHeight(wc).toFloat(), 5f, C_BG)
            val labels = arrayOf("§fEdit", "§aAdd window", "§cDelete window")
            for (i in 0..2) r.drawText("regular", labels[i], (renderX + PAD).toFloat(), (renderY + PAD + i * ITEM + 3).toFloat(), 9f, C_ITEM)
            if (wc > 0) {
                val sy = renderY + PAD + 3 * ITEM
                r.drawRect((renderX + PAD).toFloat(), sy.toFloat(), (W - PAD * 2).toFloat(), 1f, 0f, C_SEP)
                for (i in 0 until wc) {
                    val iy = sy + 4 + i * ITEM
                    if (i == WinMgr.active) r.drawRect((renderX + 2).toFloat(), iy.toFloat(), (W - 4).toFloat(), ITEM.toFloat(), 0f, C_HOV)
                    r.drawText("regular", WinMgr.wins[i].name, (renderX + PAD).toFloat(), (iy + 3).toFloat(), 9f, C_ITEM)
                }
            }
        }
    }

    // ── Edit panel ──

    private class EditPanel {
        var visible = false; var bx = 0; var by = 0; var bw = 0; var bh = 0
        var scrollOffset = 0; var selectedField = -1
        companion object {
            const val ITEM = 16; const val PAD = 6; const val HDR_H = ITEM + PAD
            val C_BG = ColorUtil.rgba(10, 10, 24, 220)
            val C_HDR = ColorUtil.rgba(40, 40, 80, 240)
            val C_SEP = ColorUtil.rgba(255, 255, 255, 20)
            val C_SCROLL = ColorUtil.rgba(255, 255, 255, 30)
            val C_THUMB = ColorUtil.rgba(255, 255, 255, 100)
        }
        fun open() { visible = true; scrollOffset = 0; selectedField = -1 }
        fun close() { visible = false }
        fun setBox(x: Int, y: Int, w: Int, h: Int) { bx = x; by = y; bw = w; bh = h }
        fun visibleRows() = maxOf(1, (bh - HDR_H - PAD) / ITEM)
        fun scroll(delta: Int, fc: Int) { scrollOffset = (scrollOffset + delta).coerceIn(0, maxOf(0, fc - visibleRows())) }
        fun render(gfx: GuiGraphicsExtractor, font: net.minecraft.client.gui.Font, c: WinCfg, mx: Int, my: Int) {
            if (!visible) return
            val r = AporiaRenderer; val theme = ThemeManagerModule.activeTheme()
            gfx.nextStratum()
            r.drawRect(bx.toFloat(), by.toFloat(), bw.toFloat(), bh.toFloat(), RADIUS.toFloat(), C_BG)
            r.drawRect(bx.toFloat(), by.toFloat(), bw.toFloat(), HDR_H.toFloat(), RADIUS.toFloat(), C_HDR)
            r.drawText("bold", "§fSettings — §7${c.nameAnim.update()}", (bx + PAD).toFloat(), (by + PAD / 2 + 3).toFloat(), 11f, -0x1)
            val esc = "§7[Esc]"
            r.drawText("regular", esc, (bx + bw - r.getTextWidth("regular", esc, 9f) - PAD).toFloat(), (by + PAD / 2 + 3).toFloat(), 9f, -0x1)
            var contentY = by + HDR_H
            r.drawRect((bx + PAD).toFloat(), contentY.toFloat(), (bw - PAD * 2).toFloat(), 1f, 0f, C_SEP); contentY++
            val fc = c.editSettings.size; val rows = visibleRows()
            scrollOffset = scrollOffset.coerceIn(0, maxOf(0, fc - rows))
            gfx.enableScissor(bx, contentY, bx + bw, by + bh)
            for (i in 0 until minOf(rows, fc - scrollOffset)) {
                c.editSettings[i + scrollOffset].draw(r, (bx + 4f), (contentY + PAD / 2f + i * ITEM), (bw - 8f), theme, mx.toFloat(), my.toFloat(), false)
            }
            gfx.disableScissor()
            if (fc > rows) {
                val barH = bh - HDR_H - PAD; val thumbH = maxOf(10, barH * rows / fc)
                val thumbY = contentY + PAD / 2 + (barH - thumbH) * scrollOffset / (fc - rows)
                r.drawRect((bx + bw - 3).toFloat(), (contentY + PAD / 2).toFloat(), 2f, barH.toFloat(), 0f, C_SCROLL)
                r.drawRect((bx + bw - 3).toFloat(), thumbY.toFloat(), 2f, thumbH.toFloat(), 0f, C_THUMB)
            }
        }
        fun fieldAt(px: Double, py: Double, fc: Int): Int {
            if (!visible) return -1
            val contentY = by + HDR_H + 1 + PAD / 2
            if (px < bx || px > bx + bw || py < contentY || py > by + bh) return -1
            val fi = ((py - contentY).toInt() / ITEM) + scrollOffset
            return if (fi in 0 until fc) fi else -1
        }
        fun onScroll(px: Double, py: Double, dy: Double, fc: Int): Boolean {
            if (!visible || px < bx || px > bx + bw || py < by || py > by + bh) return false
            scroll(if (dy > 0) -1 else 1, fc); return true
        }
    }

    // ── Handle / state fields ──

    private enum class Handle { NONE, DRAG, TOP, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT }

    private var searchQuery = ""
    private lateinit var searchBox: EditBox
    private val ctx = CtxMenu()
    private val edit = EditPanel()
    private var editingField = -1
    private lateinit var fieldBox: EditBox
    private var dragging = Handle.NONE
    private var scrollDragging = false; private var scrollDragStartY = 0.0; private var scrollDragStartOffset = 0
    private var hoveredStyle: net.minecraft.network.chat.Style? = null; private var hoveredLineIdx = -1
    private var selAnchorKey = -1L; private var selActiveKey = -1L
    private var selAnchorX = 0f; private var selActiveX = 0f; private var localPlayerName = ""
    protected var historyPos = -1; protected lateinit var commandSuggestions: CommandSuggestions

    private fun cfg() = WinMgr.get()

    private fun barWidth(): Int {
        val v = if (::searchBox.isInitialized) input?.value ?: "" else ""
        return minOf(maxOf(font.width(v) + TEXT_PAD * 2, MIN_W), cfg().w)
    }

    private fun boxW() = cfg().w
    private fun boxH() = cfg().h

    private fun setScrollOffset(c: WinCfg, newOffset: Int) {
        c.scrollOffset = newOffset.coerceIn(0, maxOf(0, c.lines.size - c.maxLines()))
    }

    private fun renderMessages(gfx: GuiGraphicsExtractor, c: WinCfg, boxTopY: Int, textX: Int) {
        val total = c.lines.size; val maxL = c.maxLines()
        val maxS = maxOf(0, total - maxL)
        c.scrollOffset = c.scrollOffset.coerceIn(0, maxS)
        val count = minOf(maxL, total - c.scrollOffset)
        val bottomY = boxTopY + maxL * LINE_H - LINE_H
        val doSearch = screenState.isIn(ScreenState.SEARCH) && searchQuery.isNotEmpty()
        val selRange = selectedLineRange()
        if (localPlayerName.isEmpty() && minecraft.player != null) localPlayerName = minecraft.player!!.name.string.lowercase()
        var i = 0
        for (line in c.lines) {
            if (i >= c.scrollOffset + count) break
            if (i < c.scrollOffset) { i++; continue }
            val visIdx = i - c.scrollOffset
            val key = lineKey(line)
            val anim = ChatScreenRenderer.anims.getOrPut(key) { MessageAnim(0f) }
            anim.setAlphaTarget(1f); anim.tick()
            val lineY = bottomY - visIdx * LINE_H + anim.slideY().toInt()
            val tx = textX + anim.slideX().toInt()
            if (doSearch && matchesQuery(plainText(line.content()), searchQuery))
                AporiaRenderer.drawRect((c.x + 1).toFloat(), (lineY - 1).toFloat(), (c.w - 2).toFloat(), LINE_H.toFloat(), 2f, C_SEARCH_HL)
            if (i in selRange[0]..selRange[1]) {
                val lineW = font.width(plainText(line.content())).toFloat().toFloat()
                var hlLeft = 0f; var hlRight = lineW
                if (i == selRange[0] && i == selRange[1]) { hlLeft = minOf(selAnchorX, selActiveX); hlRight = maxOf(selAnchorX, selActiveX) }
                else if (i == selRange[0]) { hlRight = if (lineIndexForKey(selAnchorKey) == i) selAnchorX else selActiveX }
                else if (i == selRange[1]) { hlLeft = if (lineIndexForKey(selAnchorKey) == i) selAnchorX else selActiveX }
                val hlW = (hlRight - hlLeft).coerceAtLeast(0f)
                if (hlW > 0) AporiaRenderer.drawRect((c.x + 2 + hlLeft.toInt()).toFloat(), (lineY - 1).toFloat(), hlW.toInt().toFloat(), LINE_H.toFloat(), 2f, ColorUtil.rgba(70, 130, 180, 120))
            }
            var textColor = ColorUtil.rgba(255, 255, 255, (255 * anim.alpha()).toInt())
            if (localPlayerName.isNotEmpty() && plainText(line.content()).lowercase().contains(localPlayerName)) textColor = c.selfColor
            gfx.text(font, line.content(), tx, lineY, textColor, false)
            i++
        }
    }

    private fun renderMessagesPassive(gfx: GuiGraphicsExtractor, c: WinCfg, boxTopY: Int, textX: Int) {
        val maxL = c.maxLines(); val count = minOf(maxL, c.lines.size)
        val bottomY = boxTopY + maxL * LINE_H - LINE_H
        for (i in 0 until count) {
            gfx.text(font, c.lines.elementAt(i).content(), textX, bottomY - i * LINE_H, 0xAAFFFFFF.toInt(), false)
        }
    }

    private fun renderScrollBar(c: WinCfg, bx: Int, by: Int, bw: Int, bh: Int) {
        val total = c.lines.size; val maxL = c.maxLines(); val maxS = maxOf(0, total - maxL)
        if (maxS <= 0) return
        val barX = bx + bw - 4; val barY = by + BOX_PAD; val barH = bh - BOX_PAD * 2
        val thumbH = maxOf(10, barH * maxL / maxOf(1, total))
        val thumbY = barY + (barH - thumbH) * (maxS - c.scrollOffset) / maxOf(1, maxS)
        AporiaRenderer.drawRect(barX.toFloat(), barY.toFloat(), 2f, barH.toFloat(), 0f, C_SCROLL_BAR)
        AporiaRenderer.drawRect(barX.toFloat(), thumbY.toFloat(), 2f, thumbH.toFloat(), 0f, C_SCROLL_THUMB)
    }

    private fun resolvePosition(c: WinCfg, idx: Int) {
        if (c.x != -1) return
        val defW = c.w; val defH = c.h
        when (idx % 4) {
            1 -> { c.x = width - defW - LEFT; c.bottomY = BOTTOM_PAD + INPUT_H + GAP }
            2 -> { c.x = LEFT; c.bottomY = height - defH - GAP * 2 }
            3 -> { c.x = width - defW - LEFT; c.bottomY = height - defH - GAP * 2 }
            else -> { c.x = width / 2 - defW / 2; c.bottomY = height / 2 - defH / 2 }
        }
    }

    private fun boxX() = cfg().also { resolvePosition(it, WinMgr.active) }.x
    private fun boxY() = height - cfg().bottomY - cfg().h
    private fun inputY() = height - BOTTOM_PAD - INPUT_H

    private fun selectedLineRange(): IntArray {
        if (selAnchorKey == -1L) return intArrayOf(-1, -1)
        val a = lineIndexForKey(selAnchorKey); val b = lineIndexForKey(selActiveKey)
        return intArrayOf(minOf(a, b), maxOf(a, b))
    }

    private fun lineIndexForKey(key: Long): Int {
        val c = cfg(); var i = 0
        for (line in c.lines) { if (lineKey(line) == key) return i; i++ }
        return -1
    }

    private fun matchesQuery(text: String, query: String): Boolean =
        query.split("\\s+".toRegex()).all { text.lowercase().contains(it.lowercase()) }

    // ── Lifecycle ──

    override fun init() {
        historyPos = minecraft.gui.hud.chat.recentChat.size
        for (i in 1 until WinMgr.wins.size) resolvePosition(WinMgr.wins[i], i)
        val c = cfg(); val iy = inputY()
        input = EditBox(minecraft.fontFilterFishy, c.x + TEXT_PAD, iy + (INPUT_H - 9) / 2, c.w - TEXT_PAD * 2, 9, Component.translatable("chat.editBox")).apply {
            setMaxLength(256); isBordered = false; value = this@ChatScreenBackendApi.initial ?: ""
            setResponder {
                commandSuggestions.setAllowSuggestions(true)
                commandSuggestions.updateCommandInfo()
                isDraft = false
            }
            setCanLoseFocus(false)
        }
        addRenderableWidget(input)
        searchBox = EditBox(font, c.x + TEXT_PAD, 0, c.w - TEXT_PAD * 2, 9, Component.literal("")).apply {
            setMaxLength(64); isBordered = false; setResponder { searchQuery = it }; isVisible = false
        }
        addRenderableWidget(searchBox)
        fieldBox = EditBox(font, 0, 0, 100, 9, Component.literal("")).apply {
            setMaxLength(128); isBordered = false; isVisible = false
        }
        addRenderableWidget(fieldBox)
        commandSuggestions = CommandSuggestions(minecraft, this, input, font, false, false, 1, 10, true, -805306368).apply {
            setAllowHiding(false); setAllowSuggestions(false); updateCommandInfo()
        }
        try {
            val f = ChatScreen::class.java.getDeclaredField("commandSuggestions")
            f.isAccessible = true; f.set(this, commandSuggestions)
        } catch (_: Exception) {}
        WinMgr.rebuild(minecraft.gui.hud.chat, font)
    }

    override fun extractRenderState(gfx: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        renderWindowContent(gfx, cfg(), mouseX, mouseY, true)
        val bx = boxX(); val iy = inputY(); val bW = barWidth()
        AporiaRenderer.drawRect(bx.toFloat(), iy.toFloat(), bW.toFloat(), INPUT_H.toFloat(), RADIUS.toFloat(), C_INPUT_BG)
        gfx.enableScissor(bx, iy, bx + bW, iy + INPUT_H)
        input.extractRenderState(gfx, mouseX, mouseY, delta)
        gfx.disableScissor()
        if (input.value.isEmpty() && !screenState.isIn(ScreenState.SEARCH))
            AporiaRenderer.drawText("regular", "Message...", (bx + TEXT_PAD).toFloat(), (iy + (INPUT_H - 8) / 2f), 9f, C_HINT)
        commandSuggestions.extractRenderState(gfx, mouseX, mouseY)
    }

    private fun renderWindowContent(gfx: GuiGraphicsExtractor, c: WinCfg, mouseX: Int, mouseY: Int, isActive: Boolean) {
        resolvePosition(c, WinMgr.wins.indexOf(c))
        val bx = c.x; val by = height - c.bottomY - c.h; val bw = c.w; val bh = c.h
        if (isActive) edit.setBox(bx, by, bw, bh)
        val msgCount = minOf(c.lines.size, c.maxLines())
        val actualH = if (msgCount == 0) 0 else minOf(bh, msgCount * LINE_H + BOX_PAD * 2)
        val actualBy = by + bh - actualH
        if (actualH > 0) AporiaRenderer.drawRect(bx.toFloat(), actualBy.toFloat(), bw.toFloat(), actualH.toFloat(), RADIUS.toFloat(), C_CHAT_BG)
        gfx.enableScissor(bx, actualBy, bx + bw, by + bh)
        if (isActive) renderMessages(gfx, c, by + BOX_PAD, bx + BOX_PAD)
        else renderMessagesPassive(gfx, c, by + BOX_PAD, bx + BOX_PAD)
        gfx.disableScissor()
        renderScrollBar(c, bx, by, bw, bh)
        if (isActive && screenState.isIn(ScreenState.SEARCH)) {
            val sy = by - GAP * 2 - INPUT_H
            AporiaRenderer.drawRect(bx.toFloat(), sy.toFloat(), bw.toFloat(), INPUT_H.toFloat(), RADIUS.toFloat(), C_SEARCH_BG)
            if (searchQuery.isEmpty()) gfx.text(font, "§7Search...", bx + TEXT_PAD, sy + (INPUT_H - 8) / 2, 0xFFFFFFFF.toInt(), false)
            searchBox.x = bx + TEXT_PAD; searchBox.y = (sy + (INPUT_H - 9) / 2)
            searchBox.width = bw - TEXT_PAD * 2; searchBox.isVisible = true
            searchBox.extractRenderState(gfx, 0, 0, 0f)
        }
    }

    // ── Input handling ──

    override fun mouseMoved(mx: Double, my: Double) { updateHover(mx, my) }

    private fun updateHover(mx: Double, my: Double) {
        if (!screenState.isIn(ScreenState.IDLE)) { hoveredStyle = null; hoveredLineIdx = -1; return }
        val c = cfg(); val bx = boxX(); val by = boxY(); val bw = boxW(); val bh = boxH()
        if (mx < bx || mx > bx + bw || my < by || my > by + bh) { hoveredStyle = null; hoveredLineIdx = -1; return }
        val maxL = c.maxLines(); val total = c.lines.size; val count = minOf(maxL, total - c.scrollOffset)
        val bottomY = by + BOX_PAD + maxL * LINE_H - LINE_H
        var i = 0
        for (line in c.lines) {
            if (i >= c.scrollOffset + count) break
            if (i < c.scrollOffset) { i++; continue }
            val lineY = bottomY - (i - c.scrollOffset) * LINE_H
            if (my >= lineY && my < lineY + LINE_H) {
                hoveredLineIdx = i - c.scrollOffset
                hoveredStyle = styleAtX(line.content(), (mx - (bx + BOX_PAD)).toInt())
                return
            }
            i++
        }
        hoveredStyle = null; hoveredLineIdx = -1
    }

    private fun styleAtX(seq: net.minecraft.util.FormattedCharSequence, relX: Int): net.minecraft.network.chat.Style? {
        var found: net.minecraft.network.chat.Style? = null; var accW = 0f
        seq.accept { _, style, cp ->
            val cw = font.splitter.stringWidth(net.minecraft.util.FormattedCharSequence.forward(String(Character.toChars(cp)), style))
            if (accW + cw > relX) { found = style; false } else { accW += cw; true }
        }
        return found
    }

    override fun mouseClicked(e: MouseButtonEvent, b: Boolean): Boolean {
        val mx = e.x(); val my = e.y(); val btn = e.button()
        EventBus.post(MouseClickEvent(mx, my, btn, MouseClickEvent.Action.PRESS))
        if (editingField >= 0) { commitFieldEdit(); return true }
        if (edit.visible) {
            val c = cfg(); val f = edit.fieldAt(mx, my, c.editSettings.size)
            if (f >= 0) {
                val s = c.editSettings[f]
                when (s) { is BooleanSetting -> { s.toggle(); c.syncToWinCfg(); if (f == 6 || f == 8 || f == 10) WinMgr.rebuild(minecraft.gui.hud.chat, font) }; else -> startFieldEdit(f) }
                return true
            }
            cfg().syncToWinCfg(); edit.close(); return true
        }
        if (ctx.visible) {
            val item = ctx.itemAt(mx, my, WinMgr.wins.size)
            when { item == 0 -> { ctx.close(); edit.open(); return true }; item == 1 -> { ctx.close(); addWindow(); return true }; item == 2 -> { ctx.close(); removeWindow(); return true }; item >= 3 -> { WinMgr.active = item - 3; ctx.close(); return true } }
            ctx.close(); return true
        }
        if (btn == 1) {
            val bx = boxX(); val by = boxY(); val bw = boxW(); val bh = boxH()
            if (mx.toFloat() in bx.toFloat()..(bx + bw).toFloat() && my.toFloat() in by.toFloat()..(by + bh).toFloat()) { ctx.open(mx.toInt(), my.toInt(), width, height); return true }
            for (i in WinMgr.wins.indices) {
                if (i == WinMgr.active) continue
                val w = WinMgr.wins[i]; resolvePosition(w, i)
                val wx = w.x; val wy = height - w.bottomY - w.h; val ww = w.w; val wh = w.h
                if (mx.toFloat() in wx.toFloat()..(wx + ww).toFloat() && my.toFloat() in wy.toFloat()..(wy + wh).toFloat()) { WinMgr.active = i; ctx.open(mx.toInt(), my.toInt(), width, height); return true }
            }
        }
        val c = cfg(); val bx = boxX(); val by = boxY(); val bw = boxW(); val bh = boxH()
        if (btn == 0) {
            val total = c.lines.size; val maxL = c.maxLines(); val maxS = maxOf(0, total - maxL)
            if (maxS > 0) {
                val barX = bx + bw - 4; val barY = by + BOX_PAD; val barH = bh - BOX_PAD * 2
                val thumbH = maxOf(10, barH * maxL / maxOf(1, total))
                val thumbY = barY + (barH - thumbH) * c.scrollOffset / maxOf(1, maxS)
                if (mx.toFloat() in (barX - 2).toFloat()..(barX + 4).toFloat() && my.toFloat() in barY.toFloat()..(barY + barH).toFloat()) {
                    if (my.toFloat() in thumbY.toFloat()..(thumbY + thumbH).toFloat()) { scrollDragging = true; scrollDragStartY = my; scrollDragStartOffset = c.scrollOffset; return true }
                    setScrollOffset(c, ((1f - (my - barY) / barH) * maxS).toInt()); return true
                }
            }
        }
        val h = hitHandle(mx, my, c, bx, by, bw, bh)
        if (h != Handle.NONE && btn == 0) { dragging = h; return true }
        if (btn == 0 && hoveredStyle != null) {
            if (hoveredStyle!!.clickEvent != null) { defaultHandleGameClickEvent(hoveredStyle!!.clickEvent!!, minecraft, this); initial = input?.value ?: ""; return true }
            else if (hoveredStyle!!.insertion != null && minecraft.hasShiftDown()) { insertText(hoveredStyle!!.insertion!!, false); return true }
        }
        if (btn == 0) {
            val hr = hitLineChar(mx, my)
            if (hr != null) { selAnchorKey = hr.lineKey; selAnchorX = hr.x; selActiveKey = hr.lineKey; selActiveX = hr.x; screenState.transition(ScreenState.SELECTING); return true }
            else screenState.transition(ScreenState.IDLE)
        }
        return super.mouseClicked(e, b)
    }

    override fun mouseDragged(e: MouseButtonEvent, dx: Double, dy: Double): Boolean {
        if (dragging != Handle.NONE && e.button() == 0) {
            val c = cfg(); var widthChanged = false
            when (dragging) {
                Handle.DRAG -> { c.x = c.x.coerceIn(0, width - c.w); c.bottomY = (c.bottomY - dy.toInt()).coerceIn(0, height - c.h) }
                Handle.TOP -> c.h = maxOf(LINE_H * 2 + BOX_PAD * 2, c.h - dy.toInt())
                Handle.LEFT -> { val nw = maxOf(80, c.w - dx.toInt()); widthChanged = nw != c.w; c.x += c.w - nw; c.w = nw }
                Handle.RIGHT -> { val nw = maxOf(80, c.w + dx.toInt()); widthChanged = nw != c.w; c.w = nw }
                Handle.TOP_LEFT -> { c.h = maxOf(LINE_H * 2 + BOX_PAD * 2, c.h - dy.toInt()); val nw = maxOf(80, c.w - dx.toInt()); widthChanged = nw != c.w; c.x += c.w - nw; c.w = nw }
                Handle.TOP_RIGHT -> { c.h = maxOf(LINE_H * 2 + BOX_PAD * 2, c.h - dy.toInt()); val nw = maxOf(80, c.w + dx.toInt()); widthChanged = nw != c.w; c.w = nw }
                else -> {}
            }
            if (widthChanged) WinMgr.rebuild(minecraft.gui.hud.chat, font)
            return true
        }
        if (scrollDragging && e.button() == 0) {
            val c = cfg(); val total = c.lines.size; val maxL = c.maxLines(); val maxS = maxOf(0, total - maxL)
            if (maxS > 0) { val barH = boxH() - BOX_PAD * 2; setScrollOffset(c, scrollDragStartOffset + ((scrollDragStartY - e.y()) / barH * maxS).toInt()) }
            return true
        }
        if (screenState.isIn(ScreenState.SELECTING) && e.button() == 0) { val hr = hitLineChar(e.x(), e.y()); if (hr != null) { selActiveKey = hr.lineKey; selActiveX = hr.x }; return true }
        return super.mouseDragged(e, dx, dy)
    }

    override fun mouseReleased(e: MouseButtonEvent): Boolean {
        if (dragging != Handle.NONE) { dragging = Handle.NONE; return true }
        if (scrollDragging) { scrollDragging = false; return true }
        if (screenState.isIn(ScreenState.SELECTING) && e.button() == 0) {
            val hr = hitLineChar(e.x(), e.y()); if (hr != null) { selActiveKey = hr.lineKey; selActiveX = hr.x }
            val hasSel = selAnchorKey >= 0 && selActiveKey >= 0 && (selAnchorKey != selActiveKey || Math.abs(selActiveX - selAnchorX) > 0.5f)
            screenState.transition(if (hasSel) ScreenState.SELECTED else ScreenState.IDLE); return true
        }
        if (screenState.isIn(ScreenState.SELECTED) && e.button() == 0) { screenState.transition(ScreenState.IDLE); return true }
        return super.mouseReleased(e)
    }

    override fun mouseScrolled(mx: Double, my: Double, dx: Double, dy: Double): Boolean {
        EventBus.post(MouseScrollEvent(mx, my, dx, dy))
        if (edit.onScroll(mx, my, dy, cfg().editSettings.size)) return true
        if (commandSuggestions.mouseScrolled(dy)) return true
        val c = cfg(); val bx = boxX(); val by = boxY(); val bw = boxW(); val bh = boxH()
        if (mx.toFloat() in bx.toFloat()..(bx + bw).toFloat() && my.toFloat() in by.toFloat()..(by + bh).toFloat()) { setScrollOffset(c, c.scrollOffset + if (dy > 0) 1 else -1); return true }
        for (i in WinMgr.wins.indices) {
            if (i == WinMgr.active) continue
            val w = WinMgr.wins[i]; resolvePosition(w, i)
            val wx = w.x; val wy = height - w.bottomY - w.h; val ww = w.w; val wh = w.h
            if (mx.toFloat() in wx.toFloat()..(wx + ww).toFloat() && my.toFloat() in wy.toFloat()..(wy + wh).toFloat()) { setScrollOffset(w, w.scrollOffset + if (dy > 0) 1 else -1); return true }
        }
        return false
    }

    override fun keyPressed(e: KeyEvent): Boolean {
        EventBus.post(KeyInputEvent(e.key(), e.scancode(), e.modifiers(), KeyInputEvent.Action.PRESS))
        if (editingField >= 0) { when (e.key()) { 257 -> { commitFieldEdit(); return true }; 256 -> { cancelFieldEdit(); return true } }; return fieldBox.keyPressed(e) }
        if (edit.visible) { if (e.key() == 256) edit.close(); return true }
        if (e.key() == 70 && (e.modifiers() and 2) != 0) { screenState.transition(ScreenState.SEARCH); return true }
        if (screenState.isIn(ScreenState.SEARCH) && e.key() == 256) { screenState.transition(ScreenState.IDLE); return true }
        if (screenState.isIn(ScreenState.SEARCH)) return searchBox.keyPressed(e)
        val ctrl = (e.modifiers() and 2) != 0
        if (ctrl && e.key() == 67) { copySelection(); return true }
        if (ctrl && e.key() == 88) { cutSelection(); return true }
        return super.keyPressed(e)
    }

    override fun charTyped(e: CharacterEvent): Boolean {
        if (editingField >= 0) return fieldBox.charTyped(e)
        if (screenState.isIn(ScreenState.SEARCH)) return searchBox.charTyped(e)
        return super.charTyped(e)
    }

    // ── Helpers ──

    private fun copySelection() { val c = cfg(); val t = getSelectedText(c); if (t.isNotEmpty()) minecraft.keyboardHandler.setClipboard(t) }
    private fun cutSelection() { val c = cfg(); val t = getSelectedText(c); if (t.isEmpty()) return; copySelection(); val r = selectedLineRange(); if (r[0] >= 0) deleteLines(c, r[0], r[1]); screenState.transition(ScreenState.IDLE) }

    private fun getSelectedText(c: WinCfg): String {
        val r = selectedLineRange(); if (r[0] < 0) return ""
        val aIdx = lineIndexForKey(selAnchorKey); val sb = StringBuilder(); var idx = 0
        for (line in c.lines) {
            if (idx > r[1]) break
            if (idx >= r[0]) {
                if (sb.isNotEmpty()) sb.append("\n")
                val lineW = font.width(plainText(line.content())).toFloat().toFloat()
                val (selStart, selEnd) = when {
                    r[0] == r[1] -> minOf(selAnchorX, selActiveX) to maxOf(selAnchorX, selActiveX)
                    idx == r[0] -> 0f to (if (aIdx == idx) selAnchorX else selActiveX)
                    idx == r[1] -> (if (aIdx == idx) selAnchorX else selActiveX) to lineW
                    else -> 0f to lineW
                }
                val lineText = StringBuilder(); var acc = 0f
                line.content().accept { _, _, cp ->
                    val cw = font.width(net.minecraft.util.FormattedCharSequence.forward(String(Character.toChars(cp)), net.minecraft.network.chat.Style.EMPTY)).toFloat()
                    val rightEdge = acc + cw; if (rightEdge > selStart && acc < selEnd) lineText.appendCodePoint(cp); acc = rightEdge; true
                }
                sb.append(lineText)
            }
            idx++
        }
        return sb.toString()
    }

    private fun hitHandle(mx: Double, my: Double, c: WinCfg, bx: Int, by: Int, bw: Int, bh: Int): Handle {
        val onTop = my >= by - RESIZE_HIT && my <= by + RESIZE_HIT; val onLeft = mx >= bx - RESIZE_HIT && mx <= bx + RESIZE_HIT; val onRight = mx >= bx + bw - RESIZE_HIT && mx <= bx + bw + RESIZE_HIT; val inBox = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh
        return when { onTop && onLeft -> Handle.TOP_LEFT; onTop && onRight -> Handle.TOP_RIGHT; onTop && inBox -> Handle.TOP; onLeft && inBox -> Handle.LEFT; onRight && inBox -> Handle.RIGHT; c.draggable && inBox && !onTop && !onLeft && !onRight -> Handle.DRAG; else -> Handle.NONE }
    }

    private data class HitResult(val line: Int, val x: Float, val lineKey: Long)

    private fun hitLineChar(mx: Double, my: Double): HitResult? {
        val c = cfg(); val bx = boxX(); val by = boxY(); val bw = boxW(); val bh = boxH()
        if (mx < bx || mx > bx + bw || my < by || my > by + bh) return null
        val total = c.lines.size; val maxL = c.maxLines(); val count = minOf(maxL, total - c.scrollOffset); val bottomY = by + BOX_PAD + maxL * LINE_H - LINE_H
        var i = 0; for (line in c.lines) { if (i >= c.scrollOffset + count) break; if (i < c.scrollOffset) { i++; continue }
            val lineY = bottomY - (i - c.scrollOffset) * LINE_H
            if (my >= lineY && my < lineY + LINE_H) { val textX = boxX() + 2; return HitResult(i, minOf((mx - textX).toFloat().coerceAtLeast(0f), font.width(plainText(line.content())).toFloat()), lineKey(line)) }; i++ }; return null
    }

    private fun deleteLines(c: WinCfg, from: Int, to: Int) {
        if (from < 0 || to < from) return
        val all = ArrayList(c.lines); val removeCount = minOf(to - from + 1, all.size - from); repeat(removeCount) { all.removeAt(from) }
        c.lines.clear(); c.lines.addAll(all); c.scrollOffset = c.scrollOffset.coerceIn(0, c.maxLines())
        minecraft.gui.hud.chat?.let { chat ->
            val vanilla = ArrayList(chat.allMessages)
            val removeIdx = minOf(from, vanilla.size - 1)
            val removeUpTo = minOf(to, vanilla.size - 1)
            for (j in removeUpTo downTo removeIdx) if (j < vanilla.size) vanilla.removeAt(j)
        }
    }

    private fun addWindow() { WinMgr.add("Window ${WinMgr.wins.size + 1}"); resolvePosition(WinMgr.get(), WinMgr.active); trySave() }
    private fun removeWindow() { WinMgr.remove(WinMgr.active); trySave() }

    private fun startFieldEdit(field: Int) {
        editingField = field; val c = cfg(); val s = c.editSettings[field]
        fieldBox.value = when (s) { is TextSetting -> s.get(); is ColorSetting -> "#%06X".format(s.get() and 0xFFFFFF); else -> "" }
        fieldBox.isFocused = true; setFocused(fieldBox)
    }

    private fun commitFieldEdit() {
        val v = fieldBox.value.trim(); val c = cfg(); val s = c.editSettings[editingField]
        when (s) { is TextSetting -> { s.set(v); if (editingField == 5) WinMgr.rebuild(minecraft.gui.hud.chat, font) }; is ColorSetting -> try { s.set(v.replace("#", "").toLong(16).toInt() or (0xFF000000).toInt()) } catch (_: Exception) {} }
        c.syncToWinCfg(); cancelFieldEdit(); trySave()
    }

    private fun cancelFieldEdit() { editingField = -1; fieldBox.isVisible = false; fieldBox.isFocused = false; setFocused(input) }

    override fun extractBackground(gfx: GuiGraphicsExtractor, mx: Int, my: Int, d: Float) {}
    override fun isPauseScreen() = false
    override fun isAllowedInPortal() = true
    override fun onClose() { trySave(); super.onClose() }

    companion object { 
        const val LEFT = 2; const val BOTTOM_PAD = 2; const val INPUT_H = 14; const val RADIUS = 5
        const val GAP = 3; const val BOX_PAD = 4; const val LINE_H = 10; const val MAX_LINES = 10
        const val TEXT_PAD = 8; const val MIN_W = 80; const val RESIZE_HIT = 6; const val DRAG_GRIP = 8
        val C_INPUT_BG = ColorUtil.rgba(16, 16, 32, 180); val C_CHAT_BG = ColorUtil.rgba(0, 0, 0, 120)
        val C_HINT = ColorUtil.rgba(120, 120, 120, 200); val C_SEARCH_BG = ColorUtil.rgba(20, 20, 50, 180)
        val C_SEARCH_HL = ColorUtil.rgba(255, 200, 50, 150); val C_SCROLL_BAR = ColorUtil.rgba(255, 255, 255, 40)
        val C_SCROLL_THUMB = ColorUtil.rgba(255, 255, 255, 100)

        fun lineKey(line: GuiMessage.Line): Long = ((line.addedTime().toLong() shl 32) or (line.content().hashCode().toLong() and 0xFFFFFFFFL))
        fun plainText(seq: net.minecraft.util.FormattedCharSequence): String {
            val sb = StringBuilder()
            seq.accept { _, _, cp -> sb.appendCodePoint(cp); true }
            return sb.toString()
        }
        private fun trySave() { try { ChatFile.save() } catch (e: Exception) { Logger.error("ChatFile save failed: ${e.message}") } }
    }
}
