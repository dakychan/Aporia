package aporia.cc.utility.render.display.base;

import lombok.Getter;
import aporia.cc.utility.render.display.base.color.ColorRGBA;

import java.util.List;

@Getter
public class Gradient {

    protected final ColorRGBA topLeftColor;
    protected final ColorRGBA bottomLeftColor;
    protected final ColorRGBA topRightColor;
    protected final ColorRGBA bottomRightColor;
    protected Gradient(ColorRGBA topLeftColor, ColorRGBA bottomLeftColor,
                       ColorRGBA topRightColor, ColorRGBA bottomRightColor) {
        this.topLeftColor = topLeftColor;
        this.bottomLeftColor = bottomLeftColor;
        this.topRightColor = topRightColor;
        this.bottomRightColor = bottomRightColor;
    }

    public static Gradient of(
            ColorRGBA topLeftColor, ColorRGBA bottomLeftColor,
            ColorRGBA topRightColor, ColorRGBA bottomRightColor) {
        return new Gradient(topLeftColor, bottomLeftColor, topRightColor, bottomRightColor);
    }
    public static Gradient of(List<ColorRGBA> colors) {
        return new Gradient(colors.get(0), colors.get(1) ,  colors.get(2),colors.get(3));
    }

    /**
     * Метод для поворота градиента.
     */
    public Gradient rotate() {
        return this; // ничего не делаем для обычного 4х цветного градиента
    };
    public Gradient mulAlpha(float alphaMultiplier) {
        return new Gradient(topLeftColor.mulAlpha(alphaMultiplier),bottomLeftColor.mulAlpha(alphaMultiplier),topRightColor.mulAlpha(alphaMultiplier),bottomRightColor.mulAlpha(alphaMultiplier));
    }
}

