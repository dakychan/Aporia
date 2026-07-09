package so.aporia.utils.user.player.rotation

import com.chaos.annotation.Obfuscate
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import so.aporia.utils.math.Angle
import so.aporia.utils.math.Prediction
import so.aporia.utils.user.render.animation.SpringSimulator
import java.util.Random
import kotlin.math.abs
import kotlin.math.hypot
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
object RotationUtil {

    enum class RotationMode {
        SMOOTH, SNAP, HVH, MATRIX, VULCAN, GRIM, NCP, INTAVE
    }

    private val mc = Minecraft.getInstance()
    private val RANDOM = Random()

    private var serverYaw = 0f
    private var serverPitch = 0f
    private var clientYaw = 0f
    private var clientPitch = 0f
    private var prevServerYaw = 0f
    private var prevServerPitch = 0f
    private var targetYaw = 0f
    private var targetPitch = 0f
    private var lastDeltaYaw = 0f
    private var lastDeltaPitch = 0f
    private var active = false
    private var gcdFix = true
    private var currentMode = RotationMode.SMOOTH
    private var tickCounter = 0
    private var f5OrbitYaw = 0f
    private var f5OrbitPitch = 0f
    private var lastDetached = false
    private var lastUpdateTime = 0L

    private val clientSpringYaw = SpringSimulator(120f, 18f, 0f)
    private val clientSpringPitch = SpringSimulator(120f, 18f, 0f)
    private var lastClientSpringUpdate = 0L

    private fun smoothClientCamera() {
        val now = System.currentTimeMillis()
        val dt = (now - lastClientSpringUpdate).coerceIn(1L, 50L) / 1000f
        lastClientSpringUpdate = now
        clientSpringYaw.setTarget(serverYaw)
        clientSpringPitch.setTarget(serverPitch)
        clientSpringYaw.update(dt)
        clientSpringPitch.update(dt)
        clientYaw = Mth.wrapDegrees(clientSpringYaw.value())
        clientPitch = clientSpringPitch.value().coerceIn(-90f, 90f)
    }

    @JvmStatic fun sync() {
        val p = mc.player ?: return
        serverYaw = p.yRot
        serverPitch = p.xRot
        clientYaw = serverYaw
        clientPitch = serverPitch
        prevServerYaw = serverYaw
        prevServerPitch = serverPitch
        lastDeltaYaw = 0f
        lastDeltaPitch = 0f
        f5OrbitYaw = 0f
        f5OrbitPitch = 0f
        lastDetached = false
        tickCounter = 0
        clientSpringYaw.snap(serverYaw)
        clientSpringPitch.snap(serverPitch)
        lastClientSpringUpdate = System.currentTimeMillis()
        active = true
    }

    @JvmStatic fun setMode(mode: RotationMode) { currentMode = mode }
    @JvmStatic fun getMode(): RotationMode = currentMode

    @JvmStatic fun update(target: Entity, speed: Float) {
        val p = mc.player ?: run { active = false; return }
        active = true
        lastUpdateTime = System.currentTimeMillis()
        val eyes = p.getEyePosition(1.0f)
        val pos = Prediction.predicted(target)
        val diff = pos.subtract(eyes)
        val (calcYaw, calcPitch) = Angle.calculateFromDiff(diff).let {
            it[0] to it[1].coerceIn(-90f, 90f)
        }
        targetYaw = calcYaw
        targetPitch = calcPitch
        prevServerYaw = serverYaw
        prevServerPitch = serverPitch
        tickCounter++
        when (currentMode) {
            RotationMode.HVH -> applyHvh(calcYaw, calcPitch)
            RotationMode.SNAP -> applySnap(calcYaw, calcPitch, speed)
            RotationMode.MATRIX -> applyMatrix(calcYaw, calcPitch, speed)
            RotationMode.VULCAN -> applyVulcan(calcYaw, calcPitch, speed)
            RotationMode.GRIM -> applyGrim(calcYaw, calcPitch, speed)
            RotationMode.NCP -> applyNcp(calcYaw, calcPitch, speed)
            RotationMode.INTAVE -> applyIntave(calcYaw, calcPitch, speed)
            else -> applySmooth(calcYaw, calcPitch, speed)
        }
        smoothClientCamera()
    }

    @JvmStatic fun updateRotationOnly(yaw: Float, pitch: Float, speed: Float) {
        active = true
        lastUpdateTime = System.currentTimeMillis()
        prevServerYaw = serverYaw
        prevServerPitch = serverPitch
        targetYaw = yaw
        targetPitch = pitch
        tickCounter++
        when (currentMode) {
            RotationMode.HVH -> applyHvh(yaw, pitch)
            RotationMode.SNAP -> applySnap(yaw, pitch, speed)
            RotationMode.MATRIX -> applyMatrix(yaw, pitch, speed)
            RotationMode.VULCAN -> applyVulcan(yaw, pitch, speed)
            RotationMode.GRIM -> applyGrim(yaw, pitch, speed)
            RotationMode.NCP -> applyNcp(yaw, pitch, speed)
            RotationMode.INTAVE -> applyIntave(yaw, pitch, speed)
            else -> applySmooth(yaw, pitch, speed)
        }
        smoothClientCamera()
    }

    // ─── helpers ──────────────────────────────────────────────

    private fun degsPerTick(speed: Float): Float = maxOf(speed / 20.0f, 1.0f)

    private fun gauss(intensity: Float): Float = RANDOM.nextGaussian().toFloat() * intensity

    private fun sanitize(v: Float): Float = (v * 1000000f).let { kotlin.math.round(it) / 1000000f }

    private fun gcdFix(angle: Float, prev: Float): Float {
        val sens = mc.options.sensitivity().get().toFloat() * 0.6f + 0.2f
        val gcd = sens * sens * sens * 8.0f * 0.15f
        val delta = angle - prev
        val steps = kotlin.math.round(delta / gcd.toDouble())
        return prev + (steps * gcd).toFloat()
    }

    // ─── SMOOTH ───────────────────────────────────────────────

    private fun applySmooth(yaw: Float, pitch: Float, speed: Float) {
        val maxDelta = degsPerTick(speed)
        var dy = Mth.wrapDegrees(yaw - serverYaw)
        var dp = Mth.wrapDegrees(pitch - serverPitch)
        val left = hypot(
            abs(Mth.wrapDegrees(yaw - serverYaw)).toDouble(),
            abs(Mth.wrapDegrees(pitch - serverPitch)).toDouble()
        ).toFloat()
        if (left < maxDelta) {
            val t = left / maxDelta
            dy *= t; dp *= t
        } else {
            dy = dy.coerceIn(-maxDelta, maxDelta)
            dp = dp.coerceIn(-maxDelta, maxDelta)
        }
        serverYaw += dy
        serverPitch += dp
        serverPitch = serverPitch.coerceIn(-90f, 90f)
        if (gcdFix) { serverYaw = gcdFix(serverYaw, prevServerYaw); serverPitch = gcdFix(serverPitch, prevServerPitch) }
        lastDeltaYaw = serverYaw - prevServerYaw
        lastDeltaPitch = serverPitch - prevServerPitch
        mc.player?.apply { setYRot(serverYaw); xRot = serverPitch }
    }

    // ─── SNAP ─────────────────────────────────────────────────

    private fun applySnap(yaw: Float, pitch: Float, speed: Float) {
        val maxDelta = degsPerTick(speed)
        val dy = Mth.wrapDegrees(yaw - serverYaw)
        val dp = Mth.wrapDegrees(pitch - serverPitch)
        if (abs(dy) > maxDelta || abs(dp) > maxDelta) {
            applySmooth(yaw, pitch, speed); return
        }
        serverYaw = gcdFix(yaw, prevServerYaw)
        serverPitch = gcdFix(pitch, prevServerPitch)
        lastDeltaYaw = serverYaw - prevServerYaw
        lastDeltaPitch = serverPitch - prevServerPitch
        mc.player?.apply { setYRot(serverYaw); xRot = serverPitch }
    }

    // ─── HVH ──────────────────────────────────────────────────

    private fun applyHvh(yaw: Float, pitch: Float) {
        val dy = Mth.wrapDegrees(yaw - serverYaw)
        val dp = Mth.wrapDegrees(pitch - serverPitch)
        val jitterYaw = if (tickCounter % 2 == 0) 120f else -120f
        val noisePitch = (kotlin.math.sin(tickCounter * 0.4) * 3f + gauss(1.5f)).toFloat()
        serverYaw += dy + jitterYaw
        serverPitch = (serverPitch + dp + noisePitch).coerceIn(-90f, 90f)
        lastDeltaYaw = serverYaw - prevServerYaw
        lastDeltaPitch = serverPitch - prevServerPitch
        mc.player?.apply { setYRot(serverYaw); xRot = serverPitch }
    }

    // ─── MATRIX ───────────────────────────────────────────────

    private fun applyMatrix(yaw: Float, pitch: Float, speed: Float) {
        val maxDelta = degsPerTick(speed) * 0.85f
        var dy = (Mth.wrapDegrees(yaw - serverYaw)).coerceIn(-maxDelta, maxDelta)
        var dp = (Mth.wrapDegrees(pitch - serverPitch)).coerceIn(-maxDelta * 0.8f, maxDelta * 0.8f)
        if (abs(dy) > 0.1f || abs(dp) > 0.1f) { dy += gauss(0.04f); dp += gauss(0.03f) }
        serverYaw += dy
        serverPitch += dp
        serverPitch = serverPitch.coerceIn(-90f, 90f)
        if (gcdFix) { serverYaw = gcdFix(serverYaw, prevServerYaw); serverPitch = gcdFix(serverPitch, prevServerPitch) }
        lastDeltaYaw = serverYaw - prevServerYaw
        lastDeltaPitch = serverPitch - prevServerPitch
        mc.player?.apply { setYRot(serverYaw); xRot = serverPitch }
    }

    // ─── VULCAN ───────────────────────────────────────────────

    private fun applyVulcan(yaw: Float, pitch: Float, speed: Float) {
        val maxDelta = maxOf(degsPerTick(speed) * 0.65f, 1.0f)
        var dy = (Mth.wrapDegrees(yaw - serverYaw)).coerceIn(-maxDelta, maxDelta)
        var dp = (Mth.wrapDegrees(pitch - serverPitch)).coerceIn(-maxDelta * 0.7f, maxDelta * 0.7f)

        if (abs(dy % 0.5f) < 0.001f && dy != 0f) dy += if (Math.random() > 0.5) 0.01f else -0.01f
        if (abs(dy) > 1.1f && abs(dp) < 0.01f) dp += (Math.random().toFloat() - 0.5f) * 0.1f
        if (abs(dy - lastDeltaYaw) < 0.001f) dy += (Math.random().toFloat() - 0.5f) * 0.01f
        if (abs(dp - lastDeltaPitch) < 0.001f) dp += (Math.random().toFloat() - 0.5f) * 0.01f
        if (abs(dy) > 1.0f && abs(dp) < 0.001f) dp = (Math.random().toFloat() - 0.5f) * 0.02f

        dy = sanitize(dy)
        dp = sanitize(dp)

        serverYaw += dy
        serverPitch += dp
        serverPitch = serverPitch.coerceIn(-90f, 90f)
        if (gcdFix) { serverYaw = gcdFix(serverYaw, prevServerYaw); serverPitch = gcdFix(serverPitch, prevServerPitch) }
        lastDeltaYaw = serverYaw - prevServerYaw
        lastDeltaPitch = serverPitch - prevServerPitch
        mc.player?.apply { setYRot(serverYaw); xRot = serverPitch }
    }

    // ─── GRIM ─────────────────────────────────────────────────

    private fun applyGrim(yaw: Float, pitch: Float, speed: Float) {
        var dy = Mth.wrapDegrees(yaw - serverYaw)
        var dp = Mth.wrapDegrees(pitch - serverPitch)

        if (abs(lastDeltaYaw) < 30f && abs(dy) > 320f) dy = sign(dy) * 320f

        serverYaw += dy
        serverPitch += dp
        serverPitch = serverPitch.coerceIn(-90f, 90f)
        if (gcdFix) { serverYaw = gcdFix(serverYaw, prevServerYaw); serverPitch = gcdFix(serverPitch, prevServerPitch) }
        lastDeltaYaw = serverYaw - prevServerYaw
        lastDeltaPitch = serverPitch - prevServerPitch
        mc.player?.apply { setYRot(serverYaw); xRot = serverPitch }
    }

    // ─── NCP ──────────────────────────────────────────────────

    private fun applyNcp(yaw: Float, pitch: Float, speed: Float) {
        val maxDelta = degsPerTick(speed)
        val dy = (Mth.wrapDegrees(yaw - serverYaw)).coerceIn(-maxDelta, maxDelta)
        val dp = (Mth.wrapDegrees(pitch - serverPitch)).coerceIn(-maxDelta, maxDelta)
        serverYaw += dy
        serverPitch += dp
        serverPitch = serverPitch.coerceIn(-90f, 90f)
        if (gcdFix) { serverYaw = gcdFix(serverYaw, prevServerYaw); serverPitch = gcdFix(serverPitch, prevServerPitch) }
        lastDeltaYaw = serverYaw - prevServerYaw
        lastDeltaPitch = serverPitch - prevServerPitch
        mc.player?.apply { setYRot(serverYaw); xRot = serverPitch }
    }

    // ─── INTAVE ───────────────────────────────────────────────

    private fun applyIntave(yaw: Float, pitch: Float, speed: Float) {
        val maxDelta = maxOf(degsPerTick(speed) * 0.7f, 1.0f)
        var dy = Mth.wrapDegrees(yaw - serverYaw)
        var dp = Mth.wrapDegrees(pitch - serverPitch)

        if (abs(lastDeltaYaw) < 9f && abs(dy) > 40f)
            dy = sign(dy) * minOf(abs(dy), maxDelta * 2f)

        dy = dy.coerceIn(-maxDelta, maxDelta)
        dp = dp.coerceIn(-maxDelta * 0.7f, maxDelta * 0.7f)

        if (abs(dy) > 100f) dy = sign(dy) * 100f

        dp += (Math.random().toFloat() - 0.5f) * 0.15f

        if (abs(dy) > 0.5f) dy += (Math.random().toFloat() - 0.5f) * 3.0f
        if (abs(dp) > 0.5f) dp += (Math.random().toFloat() - 0.5f) * 5.0f

        if (abs(dy) < 0.01f) dy = (Math.random().toFloat() - 0.5f) * 0.15f
        if (abs(dp) < 0.01f) dp = (Math.random().toFloat() - 0.5f) * 0.15f

        if (tickCounter % (50 + (Math.random() * 30).toInt()) == 0 && Math.random() < 0.2) {
            mc.player?.apply { setYRot(serverYaw); xRot = serverPitch }
            lastDeltaYaw = 0f; lastDeltaPitch = 0f; return
        }

        dy = sanitize(dy)
        dp = sanitize(dp)

        serverYaw += dy
        serverPitch += dp
        serverPitch = serverPitch.coerceIn(-90f, 90f)
        if (gcdFix) { serverYaw = gcdFix(serverYaw, prevServerYaw); serverPitch = gcdFix(serverPitch, prevServerPitch) }
        lastDeltaYaw = serverYaw - prevServerYaw
        lastDeltaPitch = serverPitch - prevServerPitch
        mc.player?.apply { setYRot(serverYaw); xRot = serverPitch }
    }

    // ─── public API ───────────────────────────────────────────

    @JvmStatic fun getServerYaw(): Float = serverYaw
    @JvmStatic fun getServerPitch(): Float = serverPitch
    @JvmStatic fun getClientYaw(): Float = clientYaw
    @JvmStatic fun getClientPitch(): Float = clientPitch
    @JvmStatic fun getF5OrbitYaw(): Float = f5OrbitYaw
    @JvmStatic fun getF5OrbitPitch(): Float = f5OrbitPitch
    @JvmStatic fun getTargetYaw(): Float = targetYaw
    @JvmStatic fun getTargetPitch(): Float = targetPitch

    @JvmStatic fun onCameraSetup(detached: Boolean) {
        if (detached && !lastDetached) { f5OrbitYaw = 0f; f5OrbitPitch = 0f }
        lastDetached = detached
    }

    @JvmStatic fun isActive(): Boolean {
        if (!active) return false
        if (System.currentTimeMillis() - lastUpdateTime > 150) { active = false; return false }
        return true
    }

    @JvmStatic fun reset() {
        active = false; tickCounter = 0
        lastDeltaYaw = 0f; lastDeltaPitch = 0f
        f5OrbitYaw = 0f; f5OrbitPitch = 0f
        lastDetached = false
    }

    @JvmStatic fun deactivate() { active = false; lastDetached = false }
    @JvmStatic fun setGcdFix(enabled: Boolean) { gcdFix = enabled }

    @JvmStatic fun turnClientCamera(dx: Double, dy: Double) {
        val sens = mc.options.sensitivity().get().toFloat() * 0.6f + 0.2f
        val factor = (sens * sens * sens * 8.0f * 0.15f).toDouble()
        clientYaw = (clientYaw + dx * factor).toFloat()
        clientPitch = (clientPitch + dy * factor).toFloat().coerceIn(-90f, 90f)
        f5OrbitYaw = (f5OrbitYaw + dx * factor).toFloat()
        f5OrbitPitch = (f5OrbitPitch + dy * factor).toFloat().coerceIn(-90f, 90f)
    }

    @JvmStatic fun isOnTarget(tolerance: Float): Boolean {
        if (!active) return false
        return abs(Mth.wrapDegrees(serverYaw - targetYaw)) <= tolerance
            && abs(serverPitch - targetPitch) <= tolerance
    }

    @JvmStatic fun getAngleToTarget(): Float {
        if (!active) return Float.MAX_VALUE
        return hypot(
            abs(Mth.wrapDegrees(serverYaw - targetYaw)).toDouble(),
            abs(serverPitch - targetPitch).toDouble()
        ).toFloat()
    }

    private fun sign(v: Float): Float = if (v > 0f) 1f else if (v < 0f) -1f else 0f
}
