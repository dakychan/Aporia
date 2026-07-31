package so.aporia.module.impl.render.hud

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.Items
import net.minecraft.world.item.ItemStack
import so.aporia.module.impl.render.Beautifully
import so.aporia.utils.imports.*
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import com.chaos.annotation.ChaosNative

@ChaosNative
object HotBar {

    @JvmField var active = false
    @JvmField var posX = 0f
    @JvmField var posY = 0f
    @JvmField var userMoved = false
    @JvmField var lastW = 0f
    @JvmField var lastH = 0f

    private const val SLOT_SIZE = 16f
    private const val SLOT_GAP = 1f
    private const val FS = 8f
    private const val MARGIN = 3f

    @JvmStatic
    fun render(r: AporiaRenderer, gfx: GuiGraphicsExtractor, sw: Int, sh: Int) {
        val p = mc.player ?: run { active = false; return }
        active = true

        val blur = Beautifully.isBlurEnabled() && Beautifully.isFeatureEnabled("HotBar Blur")
        val cx = sw / 2f

        val totalSlotsW = 9 * SLOT_SIZE + 8 * SLOT_GAP

        // Labels with icons
        val hp = (p.health + p.absorptionAmount).toInt()
        val food = p.foodData.foodLevel
        val lv = p.experienceLevel

        val iconFS = 10f
        val iconSize = 10f
        val gapIconText = 3f

        val hpStr = "$hp"
        val lvStr = if (lv > 0) "$lv" else ""
        val foodStr = "$food"

        val hpIconW = iconSize + gapIconText + r.getTextWidth(Fonts.BOLD, hpStr, FS)
        val lvW = if (lvStr.isNotEmpty()) iconSize + gapIconText + r.getTextWidth(Fonts.BOLD, lvStr, FS) else 0f
        val foodW = iconSize + gapIconText + r.getTextWidth(Fonts.BOLD, foodStr, FS)
        val pad = HudStyle.PAD

        // Panel width based on slots (labels auto-position within).
        val w = totalSlotsW + pad * 2
        val h = SLOT_SIZE + FS + 6f + pad * 2

        if (!userMoved) {
            posX = cx - w / 2f
            posY = sh - h - MARGIN
        }
        clampToScreen(w, h, sw, sh)
        val px = posX
        val py = posY
        lastW = w; lastH = h

        HudStyle.panel(r, px, py, w, h, HudStyle.RADIUS, blur)

        val cx2 = px + w / 2f

        // ── Labels row (above slots) ──
        val labelY = py + pad

        val hpItem = ItemStack(Items.APPLE)
        val xpItem = ItemStack(Items.EXPERIENCE_BOTTLE)
        val foodItem = ItemStack(Items.COOKED_BEEF)

        // HP left-aligned
        gfx.item(p, hpItem, (px + pad).toInt(), labelY.toInt(), 0)
        r.drawText(Fonts.BOLD, hpStr, px + pad + iconSize + gapIconText, labelY, FS, colorUtil.rgba(200, 60, 60, 220))

        // Lvl centered
        if (lvStr.isNotEmpty()) {
            val lvIconX = cx2 - lvW / 2f
            gfx.item(p, xpItem, lvIconX.toInt(), labelY.toInt(), 0)
            r.drawText(Fonts.BOLD, lvStr, lvIconX + iconSize + gapIconText, labelY, FS, colorUtil.rgba(180, 255, 100, 220))
        }

        // Food right-aligned
        val foodIconX = px + w - pad - foodW
        gfx.item(p, foodItem, foodIconX.toInt(), labelY.toInt(), 0)
        r.drawText(Fonts.BOLD, foodStr, foodIconX + iconSize + gapIconText, labelY, FS, colorUtil.rgba(160, 100, 40, 220))

        // ── Slots (bottom row) ──
        val slotY = py + pad + FS + 6f
        val slotStartX = cx2 - totalSlotsW / 2f

        for (i in 0 until 9) {
            val sx = slotStartX + i * (SLOT_SIZE + SLOT_GAP)
            val sel = i == p.inventory.selectedSlot

            val slotBg = if (sel) colorUtil.rgba(255, 255, 255, 40) else colorUtil.rgba(0, 0, 0, 50)
            r.drawRect(sx, slotY, SLOT_SIZE, SLOT_SIZE, 3f, slotBg)
            if (sel) {
                r.drawStroke(sx, slotY, SLOT_SIZE, SLOT_SIZE, 3f, 1.5f, 0, 0f, colorUtil.rgba(255, 255, 255, 100))
            }

            val item = p.inventory.getItem(i)

            // Number inside slot top-left, only if slot is empty
            if (item.isEmpty) {
                val numStr = (i + 1).toString()
                val numW = r.getTextWidth(Fonts.BOLD, numStr, 6f)
                r.drawText(Fonts.BOLD, numStr, sx + 1.5f, slotY + 1f, 6f,
                    if (sel) colorUtil.rgba(255, 255, 255, 160) else colorUtil.rgba(255, 255, 255, 80))
            }

            if (!item.isEmpty) {
                val itemX = sx.toInt() + 1
                val itemY = slotY.toInt() + 1
                gfx.item(p, item, itemX, itemY, i * 37 + 7)
                gfx.itemDecorations(mc.font, item, itemX, itemY)
            }
        }
    }

    private fun clampToScreen(w: Float, h: Float, sw: Int, sh: Int) {
        posX = posX.coerceIn(0f, (sw - w).coerceAtLeast(0f))
        posY = posY.coerceIn(0f, (sh - h).coerceAtLeast(0f))
    }
}
