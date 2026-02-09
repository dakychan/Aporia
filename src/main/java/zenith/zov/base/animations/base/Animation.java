package zenith.zov.base.animations.base;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Animation {

    private long duration;
    private float value;
    private Easing easing;
    private long startTime;
    private float startValue;
    private float targetValue;
    private boolean done;

    /**
     * Создает новую анимацию с указанными параметрами.
     *
     * @param duration     Продолжительность анимации в миллисекундах
     * @param initialValue Начальное значение
     * @param easing       Функция плавности для расчета промежуточных значений
     */
    public Animation(long duration, float initialValue, Easing easing) {
        this.duration = duration;
        this.easing = easing;
        this.value = initialValue;
        this.startValue = initialValue;
        this.targetValue = initialValue;
        this.done = true;
    }

    /**
     * Создает новую анимацию с начальным значением 0.0.
     *
     * @param duration Продолжительность анимации в миллисекундах
     * @param easing   Функция плавности для расчета промежуточных значений
     */
    public Animation(long duration, Easing easing) {
        this(duration, 0.0F, easing);
    }

    public void update(boolean bool) {
        update(bool ? 1 : 0);
    }

    /**
     * Обновляет анимацию и задает новое значение.
     * Запускает анимацию от текущего значения к новому значению.
     *
     * @param newValue Новое целевое значение для анимации
     */
    public float update(float newValue) {
        long currentTime = System.currentTimeMillis();

        if (newValue != targetValue) {
            startValue = value;
            targetValue = newValue;
            startTime = currentTime;
            done = false;
        }

        // Обновляем значение, если анимация активна
        long elapsed = currentTime - startTime;
        if (elapsed >= duration) {
            value = targetValue;
            done = true;
            return value;
        }

        float progress = (float) elapsed / duration;
        float easedProgress = easing.ease(progress, 0, 1, 1);
        value = startValue + (targetValue - startValue) * easedProgress;
        return value;
    }

    /**
     * Устанавливает значение мгновенно, без анимации.
     *
     * @param newValue Новое значение
     */
    public void setValue(float newValue) {
        this.value = newValue;
        this.startValue = newValue;
        this.targetValue = newValue;
        this.done = true;
    }


    /**
     * Сбрасывает анимацию до указанного начального значения.
     *
     * @param initialValue Значение, до которого нужно сбросить анимацию
     */
    public void reset(float initialValue) {
        this.value = initialValue;
        this.startValue = initialValue;
        this.targetValue = initialValue;
        this.done = true;
    }

    /**
     * Сбрасывает анимацию до нулевого значения.
     */
    public void reset() {
        reset(0.0F);
    }

    public void animateTo(float newTarget) {
        if (newTarget != this.targetValue) {
            this.startValue = this.value;
            this.targetValue = newTarget;
            this.startTime = System.currentTimeMillis();
            this.done = false;
        }
    }

    public float update() {
        return update(targetValue);
    }

    private boolean direction;
}