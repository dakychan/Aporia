package zenith.zov.base.animations.types;

import lombok.Getter;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.utility.render.display.base.Gradient;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.List;

public class ColorCycleRGBA {
    @Getter
    private List<ColorRGBA> baseColors; // исходные для анимки
    private final List<ColorRGBA> colors;     // текущие
    private int index = 0;
    private final Animation animation;
    private static final int TL = 0;
    private static final int BL = 1;
    private static final int TR = 2;
    private static final int BR = 3;

    public ColorCycleRGBA(List<ColorRGBA> initialColors, long durationMs) {
        if (initialColors.size() != 4)
            throw new IllegalArgumentException("надо 4 цвета - Gradient.of thow expetion");

        this.baseColors = new ArrayList<>(initialColors);
        this.colors = new ArrayList<>(initialColors);
        this.animation = new Animation(durationMs,Easing.LINEAR);
    }

    public  void update(){
        float t = animation.getValue();
        animation.setDuration(1000);
        ColorRGBA color = baseColors.get(0).mulAlpha(0);
        if(index == 0){

            colors.set(TL, baseColors.get(TL).mix(color,t));
            colors.set(BL, color.mix(baseColors.get(BL),t));
        }
        if(index == 1){

            colors.set(TR, baseColors.get(TR).mix(color,t));
            colors.set(TL, color.mix(baseColors.get(TL),t));
        }
        if(index == 2){

            colors.set(BR, baseColors.get(BR).mix(color,t));
            colors.set(TR, color.mix(baseColors.get(TR),t));
        }
        if(index == 3){

            colors.set(BL, baseColors.get(BL).mix(color,t));
            colors.set(BR, color.mix(baseColors.get(BR),t));
        }
        if(animation.getValue()==1){
            animation.reset();
          index++;
            if(index>=colors.size()){
                index = 0;
            }

        }

        animation.update(1);

    }

    public Gradient toGradient() {

        return Gradient.of(colors.get(TL),colors.get(BL),colors.get(TR),colors.get(BR));
    }


}
