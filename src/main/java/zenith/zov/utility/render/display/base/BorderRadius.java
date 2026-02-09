package zenith.zov.utility.render.display.base;

import org.jetbrains.annotations.NotNull;

public record BorderRadius(float topLeftRadius, float topRightRadius, float bottomRightRadius, float bottomLeftRadius) {

    public static final BorderRadius ZERO = new BorderRadius(0f, 0f, 0f, 0f);

    public static BorderRadius all(float radius) {
        return new BorderRadius(radius, radius, radius, radius);
    }

    public static BorderRadius topLeft(float radius) {
        return new BorderRadius(radius, 0f, 0f, 0f);
    }

    public static BorderRadius topRight(float radius) {
        return new BorderRadius(0f, radius, 0f, 0f);
    }

    public static BorderRadius bottomRight(float radius) {
        return new BorderRadius(0f, 0f, radius, 0f);
    }

    public static BorderRadius bottomLeft(float radius) {
        return new BorderRadius(0f, 0f, 0f, radius);
    }

    public static BorderRadius top(float leftRadius, float rightRadius) {
        return new BorderRadius(leftRadius, rightRadius, 0f, 0f);
    }

    public static BorderRadius bottom(float leftRadius, float rightRadius) {
        return new BorderRadius(0f, 0f, rightRadius, leftRadius);
    }

    public static BorderRadius left(float topRadius, float bottomRadius) {
        return new BorderRadius(topRadius, 0f, 0f, bottomRadius);
    }

    public static BorderRadius right(float topRadius, float bottomRadius) {
        return new BorderRadius(0f, topRadius, bottomRadius, 0f);
    }

    @Override
    public @NotNull String toString() {
        return "BorderRadius{" +
                "topLeftRadius=" + topLeftRadius +
                ", topRightRadius=" + topRightRadius +
                ", bottomRightRadius=" + bottomRightRadius +
                ", bottomLeftRadius=" + bottomLeftRadius +
                '}';
    }
}
