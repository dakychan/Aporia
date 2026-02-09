package zenith.zov.utility.game.player.rotation;


import net.minecraft.util.math.Vec2f;

public class RotationDelta {
    private final float deltaYaw;
    private final float deltaPitch;

    public RotationDelta(float deltaYaw, float deltaPitch) {
        this.deltaYaw = deltaYaw;
        this.deltaPitch = deltaPitch;
    }

    public float length() {
        return (float) Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
    }


    public float getDeltaYaw() { return deltaYaw; }
    public float getDeltaPitch() { return deltaPitch; }

    public Vec2f toVec2f() {
        return new Vec2f(deltaYaw, deltaPitch);
    }
    public boolean isInRange(float delta){
        return isInRange(delta, delta);
    }
    public boolean isInRange(float maxDeltaYaw, float maxDeltaPitch) {
        return Math.abs(deltaYaw) < maxDeltaYaw && Math.abs(deltaPitch) < maxDeltaPitch;
    }

}