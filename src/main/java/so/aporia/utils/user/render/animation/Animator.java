package so.aporia.utils.user.render.animation;

import java.util.function.Function;

/**
 * Time-based animator — drives a value from 0→1 over a duration using any easing.
 * <p>
 * Usage:
 * <pre>{@code
 * Animator anim = new Animator(300, Easing::elasticOut);
 * anim.play();
 * // each frame:
 * anim.update();
 * float v = anim.value(); // 0→1
 * }</pre>
 */
public final class Animator {

    public enum Direction { FORWARD, BACKWARD }
    public enum State     { IDLE, PLAYING, FINISHED }

    private final long durationMs;
    private final Function<Float, Float> easing;

    private long  startMs   = 0;
    private float startVal  = 0f;
    private float endVal    = 1f;
    private Direction direction = Direction.FORWARD;
    private State state        = State.IDLE;
    private float currentValue = 0f;

    public Animator(long durationMs, Function<Float, Float> easing) {
        this.durationMs = durationMs;
        this.easing     = easing;
    }

    /** Plays forward from current position to 1. */
    public void play() {
        startMs   = System.currentTimeMillis();
        startVal  = currentValue;
        endVal    = 1f;
        direction = Direction.FORWARD;
        state     = State.PLAYING;
    }

    /** Plays backward from current position to 0. */
    public void reverse() {
        startMs   = System.currentTimeMillis();
        startVal  = currentValue;
        endVal    = 0f;
        direction = Direction.BACKWARD;
        state     = State.PLAYING;
    }

    /** Plays forward if idle/finished, reverses if playing forward. */
    public void toggle() {
        if(direction == Direction.FORWARD && state == State.PLAYING) reverse();
        else play();
    }

    /** Updates the animator and returns the current eased value. */
    public float update() {
        if(state != State.PLAYING) return currentValue;
        float t = (float)(System.currentTimeMillis() - startMs) / durationMs;
        if(t >= 1f) {
            currentValue = endVal;
            state = State.FINISHED;
            return currentValue;
        }
        float eased = easing.apply(t);
        currentValue = startVal + (endVal - startVal) * eased;
        return currentValue;
    }

    public float value()          { return currentValue; }
    public State state()          { return state; }
    public boolean isPlaying()    { return state == State.PLAYING; }
    public boolean isFinished()   { return state == State.FINISHED; }
    public void    reset()        { state = State.IDLE; currentValue = 0f; }
    public void    snapTo(float v){ currentValue = v; state = State.IDLE; }

    /* --- Factory presets --- */

    public static Animator fadeIn (long ms) { return new Animator(ms, Easing::sineOut); }
    public static Animator fadeOut(long ms) { return new Animator(ms, Easing::sineIn);  }
    public static Animator pop    (long ms) { return new Animator(ms, Easing::elasticOut); }
    public static Animator slide  (long ms) { return new Animator(ms, Easing::cubicOut);   }
    public static Animator bounce (long ms) { return new Animator(ms, Easing::bounceOut);  }
    public static Animator spring (long ms) { return new Animator(ms, t -> Easing.springCritical(t, 8f)); }
}
