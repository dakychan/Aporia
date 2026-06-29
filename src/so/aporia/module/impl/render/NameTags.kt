package so.aporia.module.impl.render

import so.aporia.utils.imports.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import so.aporia.utils.user.render.core.AporiaRenderer
import net.minecraft.util.Mth
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f
import org.joml.Vector4f
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.NumberSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.RenderHudEvent
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.user.render.font.Fonts
import java.util.UUID

class NameTags : Module("NameTags", Category.VISUAL) {

    companion object {
        @JvmField var active = false
    }

    private val flashMap = mutableMapOf<UUID, FlashState>()
    private val prevHealthMap = mutableMapOf<UUID, Float>()

    val showSelf = BooleanSetting("Self", "Show own name tag", false)
    val showEnchants = BooleanSetting("Enchantments", "Show key enchantments", true)
    val showHealth = BooleanSetting("Health", "Show player health", true)
    val showArmor = BooleanSetting("Armor", "Show armor items", true)
    val showItem = BooleanSetting("HandItem", "Show held item icon", true)
    val scale = NumberSetting("Scale", "Tag scale", 1.0, 0.3, 2.0, 0.1)

    override fun onEnable() {
        active = true
        bus.register(this)
    }

    override fun onDisable() {
        active = false
        bus.unregister(this)
        flashMap.clear()
        prevHealthMap.clear()
    }

    @EventHandler
    fun onTick(e: TickEvent) {
        if (mc.level == null || mc.player == null) return
        val now = System.currentTimeMillis()
        for (entity in mc.level!!.entitiesForRendering()) {
            if (entity !is Player) continue
            if (entity == mc.player && !showSelf.isEnabled) continue
            val uuid = entity.uuid
            val currentHealth = entity.health + entity.absorptionAmount
            val prev = prevHealthMap[uuid]
            if (prev != null) {
                val diff = currentHealth - prev
                if (diff < -0.01f) {
                    val state = flashMap.getOrPut(uuid) { FlashState() }
                    state.hurtTime = now
                    state.lastHurtHealth = currentHealth
                } else if (diff > 0.01f) {
                    val state = flashMap.getOrPut(uuid) { FlashState() }
                    state.healTime = now
                    state.lastHealAmount = diff
                }
            }
            prevHealthMap[uuid] = currentHealth
        }
    }

    private val vp = Matrix4f()
    private val clip = Vector4f()

    private fun shouldRender(entity: Player, camera: net.minecraft.world.entity.Entity): Boolean {
        if (entity.name.string.isBlank()) return false
        if (entity == camera) return showSelf.isEnabled
        return true
    }

    @EventHandler
    fun onRenderHud(e: RenderHudEvent) {
        val camera = mc.cameraEntity ?: return
        val level = mc.level ?: return
        if (mc.player == null) return

        val gfx = e.graphics
        val pt = e.partialTick

        vp.set(AporiaRenderer.worldProjMatrix).mul(AporiaRenderer.worldViewMatrix)
        val camPos = mc.gameRenderer.mainCamera.position()

        val sw = mc.window.guiScaledWidth
        val sh = mc.window.guiScaledHeight

        val renderable = level.entitiesForRendering()
            .filterIsInstance<Player>()
            .filter { shouldRender(it, camera) }
            .filter { camera.distanceTo(it) <= 64.0 }

        // Phase 1: blurred backgrounds
        for (entity in renderable) {
            val (s, left, top, totalW, totalH) = computeLayout(entity, camPos, pt, sw, sh) ?: continue

            if (showHealth.isEnabled) {
                val hbW = s * 32; val hbH = s * 22
                r.drawRectBlurred(left + s * 4, top + totalH - hbH - s * 4, hbW, hbH, s * 6f, colorUtil.rgba(30, 30, 40, 200), 3f, 15)
            }

            val nameW = fonts.getTextWidth(Fonts.BOLD, entity.name.string, s * 16f) + s * 16f
            val nameBubbleW = if (showHealth.isEnabled) nameW else totalW - s * 8f
            val nbH = s * 22f
            val nbX = if (showHealth.isEnabled) left + s * 4f + s * 32f + s * 4f else left + s * 4f
            val nbY = top + totalH - nbH - s * 4f
            r.drawRectBlurred(nbX, nbY, nameBubbleW, nbH, s * 6f, colorUtil.rgba(25, 25, 35, 200), 3f, 15)

            if (showItem.isEnabled) {
                val hand = entity.mainHandItem
                if (!hand.isEmpty) {
                    val ibW = s * 22f; val ibH = s * 22f
                    r.drawRectBlurred(nbX + nameBubbleW + s * 4f, top + totalH - ibH - s * 4f, ibW, ibH, s * 6f, colorUtil.rgba(25, 25, 35, 200), 3f, 15)
                }
            }
        }

        r.flush()

        // Phase 2: item icons
        for (entity in renderable) {
            val (s, left, top, totalW, totalH) = computeLayout(entity, camPos, pt, sw, sh) ?: continue

            if (showArmor.isEnabled) renderArmorItems(gfx, entity, left, top, totalW, s)

            if (showItem.isEnabled) {
                val hand = entity.mainHandItem
                if (!hand.isEmpty) {
                    val itemBubbleW = s * 22f
                    val ix = left + s * 4f + (if (showHealth.isEnabled) s * 32f + s * 4f else 0f) + getCenterNameWidth(entity, s) + s * 4f + (itemBubbleW - s * 16f) / 2f
                    val iy = top + totalH - s * 4f - s * 22f + (s * 22f - s * 16f) / 2f
                    renderItemIcon(gfx, hand, ix.toInt(), iy.toInt(), (s * 16f).toInt())
                }
            }
        }

        // Phase 3: text
        for (entity in renderable) {
            val (s, left, top, _, totalH) = computeLayout(entity, camPos, pt, sw, sh) ?: continue

            val nameW = fonts.getTextWidth(Fonts.BOLD, entity.name.string, s * 16f)
            val nbX = if (showHealth.isEnabled) left + s * 4f + s * 32f + s * 4f else left + s * 4f
            val nbY = top + totalH - s * 4f - s * 22f
            fonts.drawText(Fonts.BOLD, entity.name.string,
                nbX + (getCenterNameWidth(entity, s) - nameW) / 2f,
                nbY + (s * 22f - s * 16f) / 2f - 1f, s * 16f, -0x1)

            if (showHealth.isEnabled) {
                val health = entity.health + entity.absorptionAmount
                val flash = flashMap[entity.uuid]
                var hurtFlash = 0f; var healFlash = 0f
                val now = System.currentTimeMillis()
                if (flash != null) {
                    val dt = (now - flash.hurtTime) / 400f
                    if (dt < 1f) hurtFlash = 1f - dt
                    val dt2 = (now - flash.healTime) / 400f
                    if (dt2 < 1f) healFlash = 1f - dt2
                }
                var heartColor = if (health > 10) -0x00AA00AB else if (health > 5) -0xAB else -0xAAAB
                if (hurtFlash > 0.01f) heartColor = colorUtil.lerp(heartColor, -0xCCCD, hurtFlash)
                else if (healFlash > 0.01f) heartColor = colorUtil.lerp(heartColor, -0x22CD, healFlash)
                val hbX = left + s * 4f; val hbY = top + totalH - s * 4f - s * 22f
                val hpText = "%.0f".format(health)
                val hpW = fonts.getTextWidth(Fonts.BOLD, hpText, s * 14f)
                fonts.drawText(Fonts.BOLD, hpText, hbX + (s * 32f - hpW) / 2f, hbY + (s * 22f - s * 14f) / 2f - 1f, s * 14f, heartColor)
            }
        }
    }

    private fun computeLayout(entity: Player, camPos: net.minecraft.world.phys.Vec3, pt: Float, sw: Int, sh: Int): FiveFold? {
        val dist = camPos.distanceTo(entity.position())
        val px = Mth.lerp(pt.toDouble(), entity.xo, entity.x).toFloat() - camPos.x.toFloat()
        val py = Mth.lerp(pt.toDouble(), entity.yo, entity.y).toFloat() - camPos.y.toFloat()
        val pz = Mth.lerp(pt.toDouble(), entity.zo, entity.z).toFloat() - camPos.z.toFloat()
        val distScale = maxOf(0.5, 1.0 - dist / 64.0).toFloat()
        val s = scale.getFloat() * distScale
        val headY = py + entity.bbHeight + 0.15f
        val sx = projectX(vp, px, headY, pz, mc)
        val sy = projectY(vp, px, headY, pz, mc)
        if (sx.isNaN() || sy.isNaN()) return null
        if (sx < -300 || sx > sw + 300 || sy < -300 || sy > sh + 300) return null
        val totalW = getTotalWidth(entity, s); val totalH = getTotalHeight(entity, s)
        return FiveFold(s, sx - totalW / 2f, sy - totalH, totalW, totalH)
    }

    private data class FiveFold(val s: Float, val left: Float, val top: Float, val totalW: Float, val totalH: Float)

    private fun renderArmorItems(gfx: GuiGraphics, player: Player, left: Float, top: Float, totalW: Float, s: Float) {
        val slots = listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)
        val count = slots.count { !player.getItemBySlot(it).isEmpty }
        if (count == 0) return

        val armorY = top + s * 4f
        val armorTotalW = count * s * 14f + (count - 1) * s * 3f
        val armorStartX = left + (totalW - armorTotalW) / 2f

        r.drawRectBlurred(armorStartX - s * 4f, armorY - s * 2f, armorTotalW + s * 8f, s * 18f + s * 4f, s * 5f, colorUtil.rgba(16, 16, 24, 190), 3f, 15)

        var idx = 0
        for (slot in slots) {
            val armor = player.getItemBySlot(slot)
            if (armor.isEmpty) continue
            val ax = armorStartX + idx * (s * 14f + s * 3f)
            val ay = armorY + s * 2f
            renderItemIcon(gfx, armor, ax.toInt(), ay.toInt(), (s * 14f).toInt())
            idx++
        }
    }

    private fun renderItemIcon(gfx: GuiGraphics, stack: ItemStack, x: Int, y: Int, size: Int) {
        if (stack.isEmpty) return
        val pose = gfx.pose()
        pose.pushMatrix()
        val sc = size / 16f
        pose.translate(x.toFloat(), y.toFloat())
        pose.scale(sc, sc)
        gfx.renderItem(stack, 0, 0)
        pose.popMatrix()
    }

    private fun getCenterNameWidth(player: Player, s: Float): Float {
        return fonts.getTextWidth(Fonts.BOLD, player.name.string, s * 16f) + s * 16f
    }

    private fun getTotalWidth(player: Player, s: Float): Float {
        var w = s * 8f
        if (showHealth.isEnabled) w += s * 32f + s * 4f
        w += getCenterNameWidth(player, s)
        if (showItem.isEnabled) {
            val hand = player.mainHandItem
            if (!hand.isEmpty) w += s * 22f + s * 4f
        }
        return w
    }

    private fun getTotalHeight(player: Player, s: Float): Float {
        var h = s * 4f
        h += s * 22f
        h += s * 4f

        var hasArmor = false
        if (showArmor.isEnabled) {
            for (slot in listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                if (!player.getItemBySlot(slot).isEmpty) { hasArmor = true; break }
            }
        }
        if (hasArmor) h += s * 6f + s * 18f + s * 4f

        return h
    }

    private fun projectX(vp: Matrix4f, x: Float, y: Float, z: Float, mc: Minecraft): Float {
        clip.set(x, y, z, 1.0f).mul(vp)
        if (clip.w <= 0) return Float.NaN
        val ndcX = clip.x / clip.w
        return (ndcX * 0.5f + 0.5f) * mc.window.guiScaledWidth
    }

    private fun projectY(vp: Matrix4f, x: Float, y: Float, z: Float, mc: Minecraft): Float {
        clip.set(x, y, z, 1.0f).mul(vp)
        if (clip.w <= 0) return Float.NaN
        val ndcY = clip.y / clip.w
        return (1.0f - (ndcY * 0.5f + 0.5f)) * mc.window.guiScaledHeight
    }

    private class FlashState {
        var hurtTime = 0L
        var healTime = 0L
        var lastHurtHealth = 0f
        var lastHealAmount = 0f
    }
}
