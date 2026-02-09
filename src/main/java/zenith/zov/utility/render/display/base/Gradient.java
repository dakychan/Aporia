package zenith.zov.utility.render.display.base;

import lombok.Getter;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

import java.util.List;

@Getter
public class Gradient {

    protected final ColorRGBA topLeftColor;
    protected final ColorRGBA bottomLeftColor;
    protected final ColorRGBA topRightColor;
    protected final ColorRGBA bottomRightColor;
                        //1- 0; //2- //3-4 //4 - 1
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
    //targetColors.set(0, baseColors.get(2)); // TL = TR
    //        targetColors.set(2, baseColors.get(3)); // TR = BR
    //        targetColors.set(3, baseColors.get(1)); // BR = BL
    //        targetColors.set(1, baseColors.get(0)); // BL = TL
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