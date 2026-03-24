package so.aporia.utils.user.render.animation;

import java.util.function.Function;

/**
 * Time-based animator — drives a value from 0→1 over a duration using any easing.
 * <p>
 * Аниматор на основе времени — изменяет значение от 0→1 в течение длительности используя easing.
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

    /**
     * Plays forward from current position to 1.
     * <p>
     * Воспроизводит вперёд от текущей позиции к 1.
     */
    public void play() {
        startMs   = System.currentTimeMillis();
        startVal  = currentValue;
        endVal    = 1f;
        direction = Direction.FORWARD;
        state     = State.PLAYING;
    }

    /**
     * Plays backward from current position to 0.
     * <p>
     * Воспроизводит назад от текущей позиции к 0.
     */
    public void reverse() {
        startMs   = System.currentTimeMillis();
        startVal  = currentValue;
        endVal    = 0f;
        direction = Direction.BACKWARD;
        state     = State.PLAYING;
    }

    /**
     * Plays forward if idle/finished, reverses if playing forward.
     * <p>
     * Воспроизводит вперёд если idle/finished, назад если играет вперёд.
     */
    public void toggle() {
        if(direction == Direction.FORWARD && state == State.PLAYING) reverse();
        else play();
    }

    /**
     * Updates the animator and returns the current eased value.
     * <p>
     * Обновляет аниматор и возвращает текущее eased значение.
     */
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

    /**
     * Resets to idle state.
     * <p>
     * Сбрасывает в состояние idle.
     */
    public void    reset()        { state = State.IDLE; currentValue = 0f; }

    /**
     * Snaps to specific value.
     * <p>
     * Устанавливает конкретное значение.
     */
    public void    snapTo(float v){ currentValue = v; state = State.IDLE; }

    /* --- Factory presets --- */

    /**
     * Fade in animation.
     * <p>
     * Анимация появления.
     */
    public static Animator fadeIn (long ms) { return new Animator(ms, Easing::sineOut); }

    /**
     * Fade out animation.
     * <p>
     * Анимация исчезновения.
     */
    public static Animator fadeOut(long ms) { return new Animator(ms, Easing::sineIn);  }

    /**
     * Pop animation.
     * <p>
     * Анимация всплывания.
     */
    public static Animator pop    (long ms) { return new Animator(ms, Easing::elasticOut); }

    /**
     * Slide animation.
     * <p>
     * Анимация скольжения.
     */
    public static Animator slide  (long ms) { return new Animator(ms, Easing::cubicOut);   }

    /**
     * Bounce animation.
     * <p>
     * Анимация прыжка.
     */
    public static Animator bounce (long ms) { return new Animator(ms, Easing::bounceOut);  }

    /**
     * Spring animation.
     * <p>
     * Пружинная анимация.
     */
    public static Animator spring (long ms) { return new Animator(ms, t -> Easing.springCritical(t, 8f)); }
}
