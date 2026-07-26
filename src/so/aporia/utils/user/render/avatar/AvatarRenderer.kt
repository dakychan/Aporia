package so.aporia.utils.user.render.avatar

import so.aporia.utils.imports.*
import net.minecraft.resources.Identifier
import so.aporia.module.ModuleManager
import so.aporia.module.impl.misc.DiscordRPCModule
import so.aporia.utils.user.render.core.AporiaRenderer
import com.chaos.annotation.ChaosNative

/**
 * AvatarRenderer — single source of truth for the user's avatar icon.
 *
 * getAvatar() is the final output: it resolves the avatar with an auto-swap chain —
 *   getDiscordAvatar()  → reaches our Discord RPC module and checks for a loaded avatar; if none →
 *   getSkinFromMinecraft() → the Minecraft skin face; if none →
 *   the Aporia logo fallback.
 *
 * draw() renders the resolved avatar with the correct crop per source. drawCircle() renders any
 * arbitrary circular texture (e.g. song cover art) in the same style.
 */
@ChaosNative
object AvatarRenderer {

    enum class Mode { AUTO, LOGO, AVATAR, SKIN }
    enum class Kind { DISCORD, SKIN, LOGO }

    data class Avatar(val kind: Kind, val id: Identifier?)

    // ── final output ──

    fun getAvatar(mode: Mode = Mode.AUTO): Avatar = when (mode) {
        Mode.LOGO -> Avatar(Kind.LOGO, null)
        Mode.SKIN -> getSkinFromMinecraft()?.let { Avatar(Kind.SKIN, it) } ?: Avatar(Kind.LOGO, null)
        Mode.AVATAR -> getDiscordAvatar()?.let { Avatar(Kind.DISCORD, it) } ?: Avatar(Kind.LOGO, null)
        Mode.AUTO -> getDiscordAvatar()?.let { Avatar(Kind.DISCORD, it) }
            ?: getSkinFromMinecraft()?.let { Avatar(Kind.SKIN, it) }
            ?: Avatar(Kind.LOGO, null)
    }

    // ── sources ──

    /** Discord avatar from our Discord RPC module (null if the module is off or no avatar loaded). */
    fun getDiscordAvatar(): Identifier? {
        val m = ModuleManager.get("Discord RPC") ?: return null
        if (!m.isEnabled) return null
        val discord = m as? DiscordRPCModule ?: return null
        discord.loadAvatarOnRenderThread()
        return discord.avatarId
    }

    /** The Minecraft skin (face texture) of the local player. */
    fun getSkinFromMinecraft(): Identifier? = mc.player?.skin?.body?.texturePath()

    // ── rendering ──

    /** Resolves and draws the avatar as a rounded/circular icon. */
    fun draw(r: AporiaRenderer, x: Float, y: Float, size: Float, radius: Float, blur: Boolean, mode: Mode = Mode.AUTO) {
        val a = getAvatar(mode)
        when (a.kind) {
            Kind.DISCORD -> {
                background(r, x, y, size, radius, blur)
                r.drawImageCropped(x, y, size, size, a.id, radius, 0f, 0f, 1f, 1f)
            }
            Kind.SKIN -> {
                background(r, x, y, size, radius, blur)
                r.drawImageCropped(x, y, size, size, a.id, radius, 8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f)  // face
                r.drawImageCropped(x, y, size, size, a.id, radius, 40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f) // hat overlay
            }
            Kind.LOGO -> {
                r.drawRect(x, y, size, size, radius, colorUtil.rgba(60, 60, 80, 255))
                r.drawText("bold", "A", x + size / 2f - 5f, y + size / 2f - 6f, 12f, colorUtil.rgba(200, 200, 255, 255))
            }
        }
    }

    /** Draws an arbitrary circular texture (e.g. song cover art) in the avatar style. */
    fun drawCircle(r: AporiaRenderer, x: Float, y: Float, size: Float, radius: Float, id: Identifier, blur: Boolean) {
        background(r, x, y, size, radius, blur)
        r.drawImageCropped(x, y, size, size, id, radius, 0f, 0f, 1f, 1f)
    }

    private fun background(r: AporiaRenderer, x: Float, y: Float, size: Float, radius: Float, blur: Boolean) {
        if (blur) r.drawRectBlurred(x, y, size, size, radius, colorUtil.rgba(30, 30, 40, 200))
        else r.drawRect(x, y, size, size, radius, colorUtil.rgba(30, 30, 40, 200))
    }
}
