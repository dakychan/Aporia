package so.aporia.module.impl.render

import com.chaos.annotation.Obfuscate
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.entity.player.AvatarRenderer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.NumberSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.WorldRenderEvent
import so.aporia.utils.user.logger.Logger
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.core.RenderFilter
import java.util.UUID

@Obfuscate
class PlayerESP : Module("PlayerESP", Category.VISUAL) {

    private val range = NumberSetting("Range", "Render distance (blocks)", 48.0, 8.0, 128.0, 8.0)
    private val glowIntensity = NumberSetting("Glow", "Edge glow intensity", 1.0, 0.0, 3.0, 0.1)
    private val rimPower = NumberSetting("Rim", "Rim light power", 1.0, 0.0, 5.0, 0.1)
    private val fresnelPower = NumberSetting("Fresnel", "Fresnel edge power", 2.0, 0.5, 8.0, 0.5)
    private val pulseSpeed = NumberSetting("Pulse", "Pulse animation speed", 0.5, 0.0, 3.0, 0.1)
    private val color = NumberSetting("Color", "Glow color (ARGB)", 0x3355FF.toDouble(), 0.0, 0xFFFFFF.toDouble(), 1.0)
    private val alpha = NumberSetting("Alpha", "Glow opacity", 1.0, 0.0, 1.0, 0.05)

    val showSelf = BooleanSetting("Show Self", "Show on own player", false)
    private val showInvisible = BooleanSetting("Show Invisible", "Show invisible players", false)

    private var renderState: AvatarRenderState? = null
    private var playerModel: PlayerModel? = null

    private val glowCache = mutableMapOf<UUID, CachedGlow>()
    private var debugDrawn = false

    override fun onEnable() {
        Logger.info("PlayerESP enabled")
        EventBus.register(this)
        RenderFilter.setEntityFilter { e -> e is Player && shouldSkipVanilla(e) }
    }

    override fun onDisable() {
        Logger.info("PlayerESP disabled")
        EventBus.unregister(this)
        RenderFilter.clearEntityFilter()
        renderState = null
        playerModel = null
        glowCache.clear()
    }

    private fun shouldSkipVanilla(player: Player): Boolean {
        val mc = Minecraft.getInstance()
        if (mc.level == null || mc.player == null) return false
        if (player == mc.player && !showSelf.isEnabled) return false
        if (player.isInvisible && !showInvisible.isEnabled) return false
        return mc.player!!.distanceTo(player) <= range.getFloat()
    }

    @EventHandler
    fun onWorldRender(e: WorldRenderEvent) {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val self = mc.player ?: return
        val pt = e.partialTick
        val camPos = mc.gameRenderer.mainCamera.position()
        val maxRange = range.getFloat()
        val poseStack = e.poseStack
        val dispatcher = mc.entityRenderDispatcher

        val rgb = color.getInt()
        val cr = ((rgb shr 16) and 0xFF) / 255f
        val cg = ((rgb shr 8) and 0xFF) / 255f
        val cb = (rgb and 0xFF) / 255f

        val now = System.currentTimeMillis()

        for (entity in level.entitiesForRendering()) {
            if (entity !is Player) continue
            if (entity == self && !showSelf.isEnabled) continue
            if (entity.isInvisible && !showInvisible.isEnabled) continue

            val dist = camPos.distanceTo(entity.position())
            if (dist > maxRange) continue

            if (entity !is AbstractClientPlayer) continue

            val avatarRenderer = dispatcher.getPlayerRenderer(entity) ?: continue

            val pos = entity.position()
            val uuid = entity.uuid
            val cached = glowCache[uuid]

            val distFade = maxOf(0.15f, 1.0f - (dist / maxRange).toFloat())

            if (cached != null && !cached.isStale(now, pos.x, pos.y, pos.z)) {
                AporiaRenderer.INSTANCE.drawPlayerGlowSubmit(
                    cached.vertexData, cached.vertexCount,
                    cr, cg, cb, alpha.getFloat() * distFade,
                    glowIntensity.getFloat(), rimPower.getFloat(),
                    fresnelPower.getFloat(), pulseSpeed.getFloat()
                )
                continue
            }

            if (renderState == null) renderState = avatarRenderer.createRenderState()
            avatarRenderer.extractRenderState(entity, renderState!!, pt)
            playerModel = avatarRenderer.getModel() as PlayerModel
            playerModel!!.setupAnim(renderState!!)

            val px = Mth.lerp(pt.toDouble(), entity.xo, entity.x).toFloat()
            val py = Mth.lerp(pt.toDouble(), entity.yo, entity.y).toFloat()
            val pz = Mth.lerp(pt.toDouble(), entity.zo, entity.z).toFloat()

            poseStack.pushPose()
            poseStack.translate(px.toDouble(), py.toDouble(), pz.toDouble())

            val sc = renderState!!.scale
            poseStack.scale(sc, sc, sc)
            if (!renderState!!.hasPose(Pose.SLEEPING)) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - renderState!!.bodyRot))
            }
            poseStack.scale(-1.0f, -1.0f, 1.0f)
            if (renderState!!.isBaby) {
                poseStack.scale(0.5f, 0.5f, 0.5f)
            }
            poseStack.scale(0.9375f, 0.9375f, 0.9375f)
            poseStack.translate(0.0, -1.501, 0.0)

            val ca = alpha.getFloat() * distFade

            val fresh = buildVertexData(poseStack, playerModel!!)
            poseStack.popPose()

            if (fresh == null) continue

            AporiaRenderer.INSTANCE.drawPlayerGlowSubmit(
                fresh.vertexData, fresh.vertexCount,
                cr, cg, cb, ca,
                glowIntensity.getFloat(), rimPower.getFloat(),
                fresnelPower.getFloat(), pulseSpeed.getFloat()
            )

            fresh.lastUpdateMs = now
            fresh.lastX = pos.x
            fresh.lastY = pos.y
            fresh.lastZ = pos.z
            glowCache[uuid] = fresh
        }

        // DEBUG: one-shot test quad to verify entityGlowPipeline works
        if (!debugDrawn) {
            AporiaRenderer.INSTANCE.drawTestQuad(1f, 0f, 0f, 1f, 1f, 1f, 2f, 0.5f)
            debugDrawn = true
            Logger.info("PlayerESP: test quad drawn")
        }
    }

    private fun buildVertexData(poseStack: PoseStack, model: PlayerModel): CachedGlow? {
        val bb = ByteBufferBuilder(262144)
        val buf = BufferBuilder(bb, VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY)
        model.renderToBuffer(poseStack, buf, 15728880, OverlayTexture.NO_OVERLAY, -1)
        val mesh = buf.build() ?: run { bb.close(); return null }
        val totalBytes = mesh.vertexBuffer().remaining()
        val vertexCount = mesh.drawState().vertexCount()
        val cg = CachedGlow()
        cg.vertexData = ByteArray(totalBytes)
        cg.vertexCount = vertexCount
        mesh.vertexBuffer().get(cg.vertexData)
        mesh.close()
        bb.close()
        return cg
    }

    private class CachedGlow {
        var vertexData: ByteArray = ByteArray(0)
        var vertexCount = 0
        var lastUpdateMs = 0L
        var lastX = 0.0
        var lastY = 0.0
        var lastZ = 0.0

        fun isStale(now: Long, x: Double, y: Double, z: Double): Boolean {
            if (now - lastUpdateMs > CACHE_TTL_MS) return true
            val dx = x - lastX
            val dy = y - lastY
            val dz = z - lastZ
            return dx * dx + dy * dy + dz * dz > 1.0
        }

        companion object {
            private const val CACHE_TTL_MS = 150L
        }
    }
}
