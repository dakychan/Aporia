package so.aporia.module.impl.render

import so.aporia.utils.imports.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import so.aporia.utils.user.render.core.AporiaRenderer
import net.minecraft.util.Mth
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.joml.Vector4f
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.SliderSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.RenderHudEvent
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.user.render.animation.SpringSimulator
import so.aporia.utils.user.render.font.Fonts
import so.aporia.utils.user.whois.WhoIs
import java.util.UUID
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.AtlasIds
import com.chaos.annotation.ChaosNative

@ChaosNative
class NameTags : Module("NameTags", Category.VISUAL) {

    companion object {
        @JvmField var active = false
    }

    private val flashMap = mutableMapOf<UUID, FlashState>()
    private val prevHealthMap = mutableMapOf<UUID, Float>()
    private val smoothPositions = mutableMapOf<UUID, SmoothPos>()
    private val filteredPos = mutableMapOf<UUID, FilterState>()

    val showSelf = BooleanSetting("Self", "Show own name tag", false)
    val showEnchants = BooleanSetting("Enchantments", "Show key enchantments", true)
    val showHealth = BooleanSetting("Health", "Show player health", true)
    val showArmor = BooleanSetting("Armor", "Show armor items", true)
    val showItem = SelectSetting("HandItem", "Show held item icons")
        .value("Both", "Main Hand", "Off Hand", "None")
        .selected("Both")
    val scale = SliderSetting("Scale", "Tag scale", 1.0, 0.3, 2.0, 0.1)
    val smoothMovement = BooleanSetting("Smooth", "Smooth tag movement (spring interpolation)", true)

    override val settings = listOf(showSelf, showEnchants, showHealth, showArmor, showItem, scale, smoothMovement)

    override fun onEnable() {
        active = true
        bus.register(this)
    }

    override fun onDisable() {
        active = false
        bus.unregister(this)
        flashMap.clear()
        prevHealthMap.clear()
        smoothPositions.clear()
        filteredPos.clear()
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

    private val viewClip = Vector4f()

    private fun shouldRender(entity: Player): Boolean {
        if (entity.name.string.isBlank()) return false
        if (fonts.getTextWidth(Fonts.BOLD, entity.name.string, 16f) < 1f) return false
        return true
    }

    private fun shouldRenderAny(entity: Player, camera: net.minecraft.world.entity.Entity): Boolean {
        if (!shouldRender(entity)) return false
        if (entity == camera && mc.options.cameraType.isFirstPerson()) return showSelf.isEnabled
        return true
    }

    private fun hasArmor(player: Player): Boolean {
        if (!showArmor.isEnabled) return false
        for (slot in listOf(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            if (!player.getItemBySlot(slot).isEmpty) return true
        }
        return false
    }

    @EventHandler
    fun onRenderHud(e: RenderHudEvent) {
        val camera = mc.cameraEntity ?: return
        val level = mc.level ?: return
        if (mc.player == null) return

        val gfx = e.graphics
        val pt = e.partialTick

        val camPos = AporiaRenderer.savedCameraPos ?: mc.gameRenderer.mainCamera().position()

        val sw = mc.window.guiScaledWidth
        val sh = mc.window.guiScaledHeight

        val renderable = level.entitiesForRendering()
            .filterIsInstance<Player>()
            .filter { shouldRenderAny(it, camera) }
            .filter { camera.distanceTo(it) <= 64.0 }
            .mapNotNull { entity ->
                val layout = computeLayout(entity, camPos, pt, sw, sh)
                if (layout != null) entity to layout else null
            }

        gfx.nextStratum()
        val pose = gfx.pose()
        pose.pushMatrix()

        // Phase 1: blurred backgrounds
        for ((entity, layout) in renderable) {
            val (s, left, top, totalW, totalH) = layout

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

            // Aporia badge background
            if (WhoIs.isAperiaUser(entity.uuid)) {
                val badgeText = "Aporia.cc user"
                val badgeW = fonts.getTextWidth(Fonts.REGULAR, badgeText, s * 10f) + s * 8f
                val badgeH = s * 14f
                val badgeX = nbX + (nameBubbleW - badgeW) / 2f
                val badgeY = if (hasArmor(entity)) {
                    top - s * 4f
                } else {
                    nbY - badgeH - s * 2f
                }
                r.drawRectBlurred(badgeX, badgeY, badgeW, badgeH, s * 4f, colorUtil.rgba(128, 0, 255, 200), 3f, 10)
            }

            val mainHand = if (showItem.isSelected("Both") || showItem.isSelected("Main Hand")) entity.mainHandItem else null
            val offHand = if (showItem.isSelected("Both") || showItem.isSelected("Off Hand")) entity.offhandItem else null
            if (mainHand != null || offHand != null) {
                var itemOffset = 0f
                if (mainHand != null && !mainHand.isEmpty) {
                    val ibW = s * 22f; val ibH = s * 22f
                    r.drawRectBlurred(nbX + nameBubbleW + s * 4f + itemOffset, top + totalH - ibH - s * 4f, ibW, ibH, s * 6f, colorUtil.rgba(25, 25, 35, 200), 3f, 15)
                    itemOffset += ibW + s * 4f
                }
                if (offHand != null && !offHand.isEmpty) {
                    val ibW = s * 22f; val ibH = s * 22f
                    r.drawRectBlurred(nbX + nameBubbleW + s * 4f + itemOffset, top + totalH - ibH - s * 4f, ibW, ibH, s * 6f, colorUtil.rgba(25, 25, 35, 200), 3f, 15)
                }
            }
        }

        r.flush()

        // Phase 2: item icons
        for ((entity, layout) in renderable) {
            val (s, left, top, totalW, totalH) = layout

            if (showArmor.isEnabled) renderArmorItems(entity, left, top, totalW, s)

            val mainHand = if (showItem.isSelected("Both") || showItem.isSelected("Main Hand")) entity.mainHandItem else null
            val offHand = if (showItem.isSelected("Both") || showItem.isSelected("Off Hand")) entity.offhandItem else null
            if (mainHand != null || offHand != null) {
                var itemOffset = 0f
                if (mainHand != null && !mainHand.isEmpty) {
                    val itemBubbleW = s * 22f
                    val ix = left + s * 4f + (if (showHealth.isEnabled) s * 32f + s * 4f else 0f) + getCenterNameWidth(entity, s) + s * 4f + itemOffset + (itemBubbleW - s * 16f) / 2f
                    val iy = top + totalH - s * 4f - s * 22f + (s * 22f - s * 16f) / 2f
                    renderItemIcon(mainHand, ix.toInt(), iy.toInt(), (s * 16f).toInt())
                    itemOffset += itemBubbleW + s * 4f
                }
                if (offHand != null && !offHand.isEmpty) {
                    val itemBubbleW = s * 22f
                    val ix = left + s * 4f + (if (showHealth.isEnabled) s * 32f + s * 4f else 0f) + getCenterNameWidth(entity, s) + s * 4f + itemOffset + (itemBubbleW - s * 16f) / 2f
                    val iy = top + totalH - s * 4f - s * 22f + (s * 22f - s * 16f) / 2f
                    renderItemIcon(offHand, ix.toInt(), iy.toInt(), (s * 16f).toInt())
                }
            }
        }

        gfx.nextStratum()

        // Phase 3: text
        for ((entity, layout) in renderable) {
            val (s, left, top, _, totalH) = layout

            val nameW = fonts.getTextWidth(Fonts.BOLD, entity.name.string, s * 16f)
            val nbX = if (showHealth.isEnabled) left + s * 4f + s * 32f + s * 4f else left + s * 4f
            val nbY = top + totalH - s * 4f - s * 22f
            fonts.drawText(Fonts.BOLD, entity.name.string,
                nbX + (getCenterNameWidth(entity, s) - nameW) / 2f,
                nbY + (s * 22f - s * 16f) / 2f - 1f, s * 16f, -0x1)

            // Aporia badge text
            if (WhoIs.isAperiaUser(entity.uuid)) {
                val badgeText = "Aporia.cc user"
                val badgeW = fonts.getTextWidth(Fonts.REGULAR, badgeText, s * 10f)
                val badgeX = nbX + (getCenterNameWidth(entity, s) - badgeW) / 2f
                val badgeY = if (hasArmor(entity)) {
                    top + s * 2f
                } else {
                    nbY - s * 12f
                }
                fonts.drawText(Fonts.REGULAR, badgeText, badgeX, badgeY, s * 10f, 0xFF00FF.toInt())
            }

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
                val hpText = java.lang.String.format("%.0f", health)
                val hpW = fonts.getTextWidth(Fonts.BOLD, hpText, s * 14f)
                fonts.drawText(Fonts.BOLD, hpText, hbX + (s * 32f - hpW) / 2f, hbY + (s * 22f - s * 14f) / 2f - 1f, s * 14f, heartColor)
            }
        }
        pose.popMatrix()
    }

    private fun computeLayout(entity: Player, camPos: net.minecraft.world.phys.Vec3, pt: Float, sw: Int, sh: Int): FiveFold? {
        val dist = camPos.distanceTo(entity.position())
        val px = Mth.lerp(pt.toDouble(), entity.xo, entity.x).toFloat() - camPos.x.toFloat()
        val py = Mth.lerp(pt.toDouble(), entity.yo, entity.y).toFloat() - camPos.y.toFloat()
        val pz = Mth.lerp(pt.toDouble(), entity.zo, entity.z).toFloat() - camPos.z.toFloat()
        val distScale = maxOf(0.5, 1.0 - dist / 64.0).toFloat()
        val s = scale.getFloat() * distScale
        val headY = py + entity.eyeHeight + 0.15f
        val sx = projectX(px, headY, pz)
        val sy = projectY(px, headY, pz)
        if (sx.isNaN() || sy.isNaN()) return null
        if (sx < -300 || sx > sw + 300 || sy < -300 || sy > sh + 300) return null

        val finalX: Float
        val finalY: Float
        if (smoothMovement.isEnabled) {
            val uuid = entity.uuid
            val smooth = smoothPositions.getOrPut(uuid) { SmoothPos(sx, sy) }
            smooth.update(sx, sy)
            finalX = smooth.sx
            finalY = smooth.sy
        } else {
            val uuid = entity.uuid
            val now = System.currentTimeMillis()
            val state = filteredPos.getOrPut(uuid) { FilterState(sx, sy, now) }
            val dt = ((now - state.lastTime).coerceIn(1L, 200L) / 1000f)
            state.lastTime = now
            val speed = 25f
            val decay = Math.exp((-dt * speed).toDouble()).toFloat()
            val alpha = 1f - decay
            val fx = sx * alpha + state.x * decay
            val fy = sy * alpha + state.y * decay
            state.x = fx
            state.y = fy
            finalX = fx
            finalY = fy
        }

        val totalW = getTotalWidth(entity, s); val totalH = getTotalHeight(entity, s)
        return FiveFold(s, finalX - totalW / 2f, finalY - totalH, totalW, totalH)
    }

    private data class FiveFold(val s: Float, val left: Float, val top: Float, val totalW: Float, val totalH: Float)

    private class SmoothPos {
        var sx: Float
        var sy: Float
        private val springX = SpringSimulator(80f, 12f, 0f)
        private val springY = SpringSimulator(80f, 12f, 0f)
        private var lastUpdate = System.currentTimeMillis()

        constructor(x: Float, y: Float) {
            sx = x; sy = y
            springX.snap(x); springY.snap(y)
        }

        fun update(targetX: Float, targetY: Float) {
            val now = System.currentTimeMillis()
            val dt = (now - lastUpdate).coerceIn(1L, 50L) / 1000f
            lastUpdate = now
            springX.setTarget(targetX)
            springY.setTarget(targetY)
            springX.update(dt)
            springY.update(dt)
            sx = springX.value()
            sy = springY.value()
            val maxDev = 40f
            sx = sx.coerceIn(targetX - maxDev, targetX + maxDev)
            sy = sy.coerceIn(targetY - maxDev, targetY + maxDev)
        }
    }

    private fun renderArmorItems(player: Player, left: Float, top: Float, totalW: Float, s: Float) {
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
            renderItemIcon(armor, ax.toInt(), ay.toInt(), (s * 14f).toInt())
            idx++
        }
    }

    private fun renderItemIcon(stack: ItemStack, x: Int, y: Int, size: Int) {
        if (stack.isEmpty) return
        val itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()) ?: return
        val spriteId = itemId.withPrefix("item/")
        val atlas = try { mc.atlasManager.getAtlasOrThrow(AtlasIds.ITEMS) } catch (e: Exception) { return }
        val sprite = atlas.getSprite(spriteId)
        if (sprite === atlas.missingSprite()) return
        r.drawImageCropped(x.toFloat(), y.toFloat(), size.toFloat(), size.toFloat(),
            sprite.atlasLocation(), 0f, sprite.u0, sprite.v0, sprite.u1, sprite.v1)
    }

    private fun getCenterNameWidth(player: Player, s: Float): Float {
        return fonts.getTextWidth(Fonts.BOLD, player.name.string, s * 16f) + s * 16f
    }

    private fun getTotalWidth(player: Player, s: Float): Float {
        var w = s * 8f
        if (showHealth.isEnabled) w += s * 32f + s * 4f
        w += getCenterNameWidth(player, s)
        if (showItem.isSelected("Both") || showItem.isSelected("Main Hand")) {
            if (!player.mainHandItem.isEmpty) w += s * 22f + s * 4f
        }
        if (showItem.isSelected("Both") || showItem.isSelected("Off Hand")) {
            if (!player.offhandItem.isEmpty) w += s * 22f + s * 4f
        }
        return w
    }

    private fun getTotalHeight(player: Player, s: Float): Float {
        var h = s * 4f
        h += s * 22f
        h += s * 4f

        if (hasArmor(player)) h += s * 6f + s * 18f + s * 4f

        return h
    }

    private fun projectX(x: Float, y: Float, z: Float): Float {
        viewClip.set(x, y, z, 1.0f)
        AporiaRenderer.worldViewMatrix.transform(viewClip)
        AporiaRenderer.worldProjMatrix.transform(viewClip)
        if (viewClip.w <= 0) return Float.NaN
        val ndcX = viewClip.x / viewClip.w
        return (ndcX * 0.5f + 0.5f) * mc.window.guiScaledWidth
    }

    private fun projectY(x: Float, y: Float, z: Float): Float {
        viewClip.set(x, y, z, 1.0f)
        AporiaRenderer.worldViewMatrix.transform(viewClip)
        AporiaRenderer.worldProjMatrix.transform(viewClip)
        if (viewClip.w <= 0) return Float.NaN
        val ndcY = viewClip.y / viewClip.w
        return (1.0f - (ndcY * 0.5f + 0.5f)) * mc.window.guiScaledHeight
    }

    private class FilterState(x: Float, y: Float, lastTime: Long) {
        var x = x
        var y = y
        var lastTime = lastTime
    }

    private class FlashState {
        var hurtTime = 0L
        var healTime = 0L
        var lastHurtHealth = 0f
        var lastHealAmount = 0f
    }
}
