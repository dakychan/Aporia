package so.aporia.utils.user.render.animation;

/**
 * Per-message physics animation: alpha fade + horizontal slide-in.
 * Both driven by real spring oscillators — call {@link #tick()} each frame.
 */
public final class MessageAnim {

    private static final float ALPHA_K = 160f, ALPHA_D = 24f;
    private static final float SLIDE_K = 340f, SLIDE_D = 22f;

    /** Pixels to slide in from the left on first appear. */
    public static final float SLIDE_INITIAL = 12f;

    public final SpringSimulator alpha;
    public final SpringSimulator slideX;

    private long lastMs = System.currentTimeMillis();

    public MessageAnim(float initialAlpha) {
        alpha  = new SpringSimulator(ALPHA_K, ALPHA_D, initialAlpha);
        slideX = new SpringSimulator(SLIDE_K, SLIDE_D, initialAlpha > 0.5f ? 0f : SLIDE_INITIAL);
        alpha.setTarget(initialAlpha);
        slideX.setTarget(0f);
    }

    /** Advance springs by real elapsed time. Call once per render frame. */
    public void tick() {
        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastMs) / 1000f, 0.05f);
        lastMs = now;
        alpha.update(dt);
        slideX.update(dt);
    }

    public void setAlphaTarget(float t) { alpha.setTarget(t); }

    public float alpha()  { return Math.max(0f, Math.min(1f, alpha.value())); }
    public float slideX() { return slideX.value(); }

    /** True once faded out and settled — safe to evict from cache. */
    public boolean isDead() {
        return alpha.getTarget() < 0.01f && alpha.isSettled();
    }
}
