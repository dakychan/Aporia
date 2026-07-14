package so.aporia.utils.user.render.animation

import com.chaos.annotation.ChaosNative

@ChaosNative
class FluidAnim(
    private var stiffness: Float = 300f,
    private var damping: Float = 22f,
    private var staggerMs: Long = 35L
) {
    private data class ItemState(
        val posSpring: SpringSimulator,
        val scaleSpring: SpringSimulator,
        var started: Boolean = false
    )

    private val items = mutableMapOf<Int, ItemState>()
    private var srcX = 0f; private var srcY = 0f
    private var startMs = 0L; private var running = false

    fun setSource(x: Float, y: Float) { srcX = x; srcY = y }

    fun trigger() {
        startMs = System.currentTimeMillis()
        running = true
        for ((_, s) in items) { s.started = false; s.posSpring.snap(0f); s.scaleSpring.snap(0f) }
    }

    fun reverse() {
        for ((_, s) in items) { s.posSpring.setTarget(0f); s.scaleSpring.setTarget(0f) }
    }

    fun reset() { running = false; items.clear() }

    fun update(dt: Float) {
        if (!running) return
        val now = System.currentTimeMillis()
        for ((idx, s) in items) {
            val delay = idx * staggerMs
            if (!s.started && now - startMs > delay) {
                s.posSpring.setTarget(1f)
                s.scaleSpring.setTarget(1f)
                s.started = true
            }
            s.posSpring.update(dt)
            s.scaleSpring.update(dt)
        }
    }

    fun isRunning(): Boolean {
        if (!running) return false
        return items.values.any { !it.posSpring.isSettled() || !it.scaleSpring.isSettled() }
    }

    fun getPos(idx: Int, dstX: Float, dstY: Float): FloatArray {
        val s = items.getOrPut(idx) {
            ItemState(
                SpringSimulator(stiffness, damping, 0f),
                SpringSimulator(stiffness * 0.85f, damping * 0.85f, 0f)
            )
        }
        val t = s.posSpring.value()
        return floatArrayOf(
            srcX + (dstX - srcX) * t,
            srcY + (dstY - srcY) * t
        )
    }

    fun getScale(idx: Int): Float {
        val s = items.getOrPut(idx) {
            ItemState(
                SpringSimulator(stiffness, damping, 0f),
                SpringSimulator(stiffness * 0.85f, damping * 0.85f, 0f)
            )
        }
        return 0.2f + 0.8f * s.scaleSpring.value()
    }

    companion object {
        @JvmStatic fun suck(): FluidAnim = FluidAnim(320f, 24f, 30L)
        @JvmStatic fun fanOut(): FluidAnim = FluidAnim(260f, 18f, 40L)
    }
}
