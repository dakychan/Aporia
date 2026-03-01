package aporia.su.util.animations;

@FunctionalInterface
public interface Easing {
    double ease(double value);
}