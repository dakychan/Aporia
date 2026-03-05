package aporia.su.util.user.render.animations;

public interface AnimationCalculation {
    default double calculation(double value) {
        return 0;
    }
}