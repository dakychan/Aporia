package so.aporia.render.animation;

/**
 * A single animated float value driven by configurable physics.
 * <p>
 * Combines a {@link SpringSimulator} with optional easing on top.
 * Tracks its own time so you just call {@link #get()} each frame.
 * <p>
 * Usage:
 * <pre>{@code
 * PhysicsValue alpha = PhysicsValue.spring(0f);
 * alpha.setTarget(1f);
 * // each frame:
 * float a = alpha.get();
 * }</pre>
 */
public final class PhysicsValue {

    /** Underlying simulation mode. */
    public enum Mode {
        /** Damped harmonic oscillator. */
        SPRING,
        /** Exponential decay toward target (lerp each frame). */
        EXPONENTIAL,
        /** Instant snap — no animation. */
        INSTANT
    }

    private final Mode mode;
    private final SpringSimulator spring;
    private final float expSpeed; /* for EXPONENTIAL: fraction per ms */

    private float value;
    private float target;
    private long  lastMs;

    private PhysicsValue(Mode mode, float initial, SpringSimulator spring, float expSpeed) {
        this.mode     = mode;
        this.spring   = spring;
        this.expSpeed = expSpeed;
        this.value    = initial;
        this.target   = initial;
        this.lastMs   = System.currentTimeMillis();
    }

    /** Returns the current value, advancing the simulation. */
    public float get() {
        long now = System.currentTimeMillis();
        long dt  = now - lastMs;
        lastMs   = now;
        return advance(dt);
    }

    private float advance(long dtMs) {
        switch(mode) {
            case SPRING -> {
                spring.updateMs(dtMs);
                value = spring.value();
            }
            case EXPONENTIAL -> {
                float factor = 1f - (float)Math.pow(1f - expSpeed, dtMs);
                value += (target - value) * Math.min(1f, factor);
            }
            case INSTANT -> value = target;
        }
        return value;
    }

    public void setTarget(float t) {
        this.target = t;
        if(mode == Mode.SPRING) spring.setTarget(t);
    }

    public void snap(float v) {
        this.value  = v;
        this.target = v;
        if(mode == Mode.SPRING) spring.snap(v);
    }

    public float getTarget()   { return target; }
    public float peek()        { return value; }
    public boolean isSettled() {
        return mode == Mode.SPRING ? spring.isSettled()
             : Math.abs(value - target) < 0.001f;
    }

    /* --- Factories --- */

    public static PhysicsValue spring(float initial) {
        return new PhysicsValue(Mode.SPRING, initial, SpringSimulator.snappy(initial), 0);
    }
    public static PhysicsValue spring(float initial, SpringSimulator sim) {
        return new PhysicsValue(Mode.SPRING, initial, sim, 0);
    }
    /** Exponential lerp — {@code speed} is fraction closed per ms (try 0.01–0.05). */
    public static PhysicsValue exponential(float initial, float speed) {
        return new PhysicsValue(Mode.EXPONENTIAL, initial, null, speed);
    }
    public static PhysicsValue instant(float initial) {
        return new PhysicsValue(Mode.INSTANT, initial, null, 0);
    }
}
