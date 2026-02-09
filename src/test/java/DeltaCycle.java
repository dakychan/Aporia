import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;

import java.util.Arrays;

public class DeltaCycle {

    private final float[] deltas = new float[4];
    private int index = 0;
    private Animation animation;

    public DeltaCycle(long durationMs) {
        this.animation = new Animation(durationMs,Easing.LINEAR);
        Arrays.fill(deltas, 0f);
    }

    public void update() {
        float t = (float) animation.getValue();


        int from = index;
        int to = (index + 1) % deltas.length;

        deltas[from] = 1f - t;
        deltas[to] = t;

        if (t >= 1.0f) {
            index = (index + 1) % deltas.length;
            animation.reset();
        }

        animation.update(1);
    }

    public float[] getDeltas() {
        return deltas.clone();
    }
}
