package ru.render.anim;

public class Animation {
    private long start;
    private double duration;
    private double fromValue;
    private double toValue;
    private double value;
    private double prevValue;
    private Easing easing = Easings.LINEAR;
    private Runnable finishAction;

    public Animation run(double valueTo, double duration) {
        return this.run(valueTo, duration, Easings.LINEAR, false);
    }

    public Animation run(double valueTo, double duration, Easing easing) {
        return this.run(valueTo, duration, easing, false);
    }

    public Animation run(double valueTo, double duration, boolean safe) {
        return this.run(valueTo, duration, Easings.LINEAR, safe);
    }

    public Animation run(double valueTo, double duration, Easing easing, boolean safe) {
        if (this.check(safe, valueTo)) {
            return this;
        }
        
        this.easing = easing;
        this.duration = duration * 1000.0;
        this.start = System.currentTimeMillis();
        this.fromValue = this.value;
        this.toValue = valueTo;
        
        return this;
    }

    public boolean update() {
        this.prevValue = this.value;
        boolean alive = this.isAlive();
        
        if (alive) {
            this.value = this.interpolate(this.fromValue, this.toValue, this.easing.ease(this.calculatePart()));
        } else {
            this.start = 0L;
            this.value = this.toValue;
            if (this.finishAction != null) {
                this.finishAction.run();
                this.finishAction = null;
            }
        }

        return alive;
    }

    public boolean isAlive() {
        return !this.isFinished();
    }

    public boolean isFinished() {
        return this.calculatePart() >= 1.0;
    }

    public double calculatePart() {
        return (System.currentTimeMillis() - this.start) / this.duration;
    }

    public boolean check(boolean safe, double valueTo) {
        return safe && this.isAlive() && (valueTo == this.fromValue || valueTo == this.toValue || valueTo == this.value);
    }

    public double interpolate(double start, double end, double pct) {
        return start + (end - start) * pct;
    }

    public Animation onFinished(Runnable action) {
        this.finishAction = action;
        return this;
    }

    public float get() {
        return (float) this.value;
    }

    public float getPrev() {
        return (float) this.prevValue;
    }

    public void set(double value) {
        this.run(value, 0.0);
        this.update();
        this.value = value;
    }

    public long getStart() {
        return this.start;
    }

    public double getDuration() {
        return this.duration;
    }

    public double getFromValue() {
        return this.fromValue;
    }

    public double getToValue() {
        return this.toValue;
    }

    public double getValue() {
        return this.value;
    }

    public double getPrevValue() {
        return this.prevValue;
    }

    public Easing getEasing() {
        return this.easing;
    }

    public Runnable getFinishAction() {
        return this.finishAction;
    }
}
