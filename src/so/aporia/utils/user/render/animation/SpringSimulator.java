package so.aporia.utils.user.render.animation;

/**
 * Real-time spring physics simulator.
 * <p>
 * Models a damped harmonic oscillator: {@code F = -k*x - d*v}.
 * Call {@link #update(float)} every frame with delta-time in seconds,
 * then read {@link #value()} and {@link #velocity()}.
 * <p>
 * Usage:
 * <pre>{@code
 * SpringSimulator spring = new SpringSimulator(200f, 20f, 0f);
 * spring.setTarget(1f);
 * // each frame:
 * spring.update(deltaSeconds);
 * float x = spring.value();
 * }</pre>
 */
public final class SpringSimulator {

    private float stiffness; /* k — spring constant */
    private float damping;   /* d — damping coefficient */
    private float mass;      /* m — mass (default 1) */

    private float position;
    private float velocity;
    private float target;

    /**
     * @param stiffness spring constant k (try 150–400 for snappy UI)
     * @param damping   damping d (try 15–30; critical = 2*sqrt(k*m))
     * @param initial   starting position
     */
    public SpringSimulator(float stiffness, float damping, float initial) {
        this.stiffness = stiffness;
        this.damping   = damping;
        this.mass      = 1f;
        this.position  = initial;
        this.target    = initial;
    }

    /** Advances the simulation by {@code dt} seconds. */
    public void update(float dt) {
        if(dt <= 0) return;
        /* Semi-implicit Euler integration — stable for large dt. */
        float force = -stiffness * (position - target) - damping * velocity;
        velocity += (force / mass) * dt;
        position += velocity * dt;
    }

    /** Convenience: update with milliseconds. */
    public void updateMs(long ms) { update(ms / 1000f); }

    public float value()    { return position; }
    public float velocity() { return velocity; }

    public void setTarget(float t)   { this.target   = t; }
    public void setPosition(float p) { this.position = p; this.velocity = 0; }
    public void snap(float p)        { this.position = p; this.target = p; this.velocity = 0; }

    public float getTarget()    { return target; }
    public boolean isSettled()  { return Math.abs(position - target) < 0.001f && Math.abs(velocity) < 0.001f; }

    /* --- Presets --- */

    /** Snappy UI spring — fast, slight overshoot. */
    public static SpringSimulator snappy(float initial) {
        return new SpringSimulator(300f, 22f, initial);
    }

    /** Bouncy spring — noticeable oscillation. */
    public static SpringSimulator bouncy(float initial) {
        return new SpringSimulator(200f, 10f, initial);
    }

    /** Smooth spring — no overshoot, gentle. */
    public static SpringSimulator smooth(float initial) {
        return new SpringSimulator(120f, 24f, initial);
    }

    /** Stiff spring — almost instant, tiny overshoot. */
    public static SpringSimulator stiff(float initial) {
        return new SpringSimulator(500f, 35f, initial);
    }

    /** Wobbly spring — lots of oscillation. */
    public static SpringSimulator wobbly(float initial) {
        return new SpringSimulator(180f, 8f, initial);
    }
}
