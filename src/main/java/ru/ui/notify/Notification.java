package ru.ui.notify;

import ru.render.Animation;
import ru.render.EasingFunction;

public class Notification {
    private final String message;
    private final NotificationType type;
    private final long createdTime;
    private final long duration;
    private final Animation slideAnimation;
    private final Animation progressAnimation;
    private boolean dismissed = false;
    private static final long SLIDE_DURATION_MS = 300;

    public Notification(String message, NotificationType type, long duration) {
        this.message = message;
        this.type = type;
        this.createdTime = System.currentTimeMillis();
        this.duration = duration;
        
        this.slideAnimation = new Animation(SLIDE_DURATION_MS / 1000.0f);
        this.progressAnimation = new Animation(duration / 1000.0f);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdTime >= duration;
    }

    public float getProgress() {
        long elapsed = System.currentTimeMillis() - createdTime;
        float normalizedTime = Math.min((float) elapsed / duration, 1.0f);
        return EasingFunction.LINEAR.apply(normalizedTime);
    }

    public float getSlideOffset() {
        long elapsed = System.currentTimeMillis() - createdTime;
        float normalizedTime = Math.min((float) elapsed / SLIDE_DURATION_MS, 1.0f);
        return EasingFunction.EASE_IN_OUT_CUBIC.apply(normalizedTime);
    }

    public void dismiss() {
        dismissed = true;
    }

    public boolean isDismissed() {
        return dismissed;
    }

    public String getMessage() {
        return message;
    }

    public NotificationType getType() {
        return type;
    }
}
