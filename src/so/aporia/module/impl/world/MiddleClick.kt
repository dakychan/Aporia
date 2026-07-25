package so.aporia.module.impl.world
import com.chaos.annotation.Obfuscate
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector4f
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.MouseClickEvent
import so.aporia.utils.events.impl.RenderHudEvent
import so.aporia.utils.user.render.font.Fonts
import so.aporia.utils.imports.*
import java.util.HashMap
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class MiddleClick : Module("MiddleClick", Category.WORLD) {

    val autoFriend = BooleanSetting("AutoFriend",
        "Middle click on a player nametag to add/remove friend", true)

    private val projectedPositions = HashMap<Player, FloatArray>()

    override val settings = listOf(autoFriend)

    override fun onEnable() {
        bus.register(this)
    }

    override fun onDisable() {
        bus.unregister(this)
    }

    @EventHandler
    fun onMouseClick(e: MouseClickEvent) {
        if (!autoFriend.isEnabled) return
        if (e.action() != MouseClickEvent.Action.PRESS) return
        if (e.button() != 2) return

        val target = findTargetPlayerAtCrosshair()
        if (target != null) {
            val name = target.name.string
            if (fm.isFriend(name)) {
                fm.remove(name)
            } else {
                fm.add(name)
            }
        }
    }

    @EventHandler
    fun onRenderHud(e: RenderHudEvent) {
        if (!autoFriend.isEnabled) return
        projectedPositions.clear()

        val camera = mc.cameraEntity
        if (mc.level == null || mc.player == null || camera == null) return

        val camPos = mc.gameRenderer.mainCamera().position()
        val vp = mc.gameRenderer.mainCamera().getViewRotationProjectionMatrix(Matrix4f())
        val pt = e.partialTick()

        val mouseX = mc.mouseHandler.xpos() / mc.window.guiScale
        val mouseY = mc.mouseHandler.ypos() / mc.window.guiScale

        var hovered: Player? = null
        var hoverScrX = 0f
        var hoverScrY = 0f

        for (entity in mc.level!!.entitiesForRendering()) {
            if (entity !is Player) continue
            if (entity == camera) continue
            val dist = camera.distanceTo(entity)
            if (dist > 64.0) continue

            val px = Mth.lerp(pt.toDouble(), entity.xo, entity.x).toFloat() - camPos.x.toFloat()
            val py = Mth.lerp(pt.toDouble(), entity.yo, entity.y).toFloat() - camPos.y.toFloat()
            val pz = Mth.lerp(pt.toDouble(), entity.zo, entity.z).toFloat() - camPos.z.toFloat()
            val headY = py + entity.bbHeight + 0.35f

            val scrX = projectX(vp, px, headY, pz, mc)
            val scrY = projectY(vp, px, headY, pz, mc)
            if (scrX.isNaN() || scrY.isNaN()) continue

            val nameWidth = r.getTextWidth(Fonts.BOLD, entity.name.string, 12f)
            val zoneW = nameWidth + 20
            val zoneH = 24f

            if (mouseX >= scrX - zoneW / 2 && mouseX <= scrX + zoneW / 2
                && mouseY >= scrY - zoneH / 2 && mouseY <= scrY + zoneH / 2) {
                hovered = entity
                hoverScrX = scrX
                hoverScrY = scrY
                break
            }
        }

        if (hovered != null) {
            val displayName = hovered.name.string
            val nameWidth = r.getTextWidth(Fonts.BOLD, displayName, 12f)
            val zoneW = nameWidth + 20
            val zoneH = 24f

            val isFriend = fm.isFriend(displayName)
            val zoneColor = if (isFriend)
                colorUtil.rgba(100, 200, 255, 60)
            else
                colorUtil.rgba(100, 255, 100, 60)
            val borderColor = if (isFriend)
                colorUtil.rgba(100, 200, 255, 160)
            else
                colorUtil.rgba(100, 255, 100, 160)

            val zx = hoverScrX - zoneW / 2
            val zy = hoverScrY - zoneH / 2

            r.drawRect(zx, zy, zoneW, zoneH, 4f, zoneColor)
            r.drawStroke(zx, zy, zoneW, zoneH, 4f, 1f, 1, 0f, borderColor)

            val label = if (isFriend) "-" else "+"
            r.drawText(Fonts.BOLD, label, zx - 16, zy + (zoneH - 14f) / 2f - 1f, 14f,
                if (isFriend) colorUtil.rgba(100, 200, 255, 255) else colorUtil.rgba(100, 255, 100, 255))
        }
    }

    private fun findTargetPlayerAtCrosshair(): Player? {
        if (mc.hitResult != null && mc.hitResult!!.type == HitResult.Type.ENTITY) {
            val e = (mc.hitResult as EntityHitResult).entity
            if (e is Player) return e
        }
        return null
    }

    companion object {
        private fun projectX(vp: Matrix4f, x: Float, y: Float, z: Float, mc: Minecraft): Float {
            val clip = Vector4f(x, y, z, 1.0f).mul(vp)
            if (clip.w <= 0) return Float.NaN
            val ndcX = clip.x / clip.w
            return (ndcX * 0.5f + 0.5f) * mc.window.guiScaledWidth
        }

        private fun projectY(vp: Matrix4f, x: Float, y: Float, z: Float, mc: Minecraft): Float {
            val clip = Vector4f(x, y, z, 1.0f).mul(vp)
            if (clip.w <= 0) return Float.NaN
            val ndcY = clip.y / clip.w
            return (1.0f - (ndcY * 0.5f + 0.5f)) * mc.window.guiScaledHeight
        }
    }
}