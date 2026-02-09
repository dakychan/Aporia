package zenith.zov.base.theme;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import lombok.Getter;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;

import zenith.zov.base.animations.types.ColorCycleRGBA;
import zenith.zov.base.events.impl.render.EventHudRender;
import zenith.zov.utility.render.display.base.Gradient;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.base.color.ColorUtil;

import java.util.Arrays;
import java.util.List;

@Getter
public class ThemeManager {
    private Theme prevTheme;
    private Theme currentTheme = Theme.DARK;
    private final Animation animation;
    private final ColorCycleRGBA colorCycleIcon;


    public ThemeManager() {
        EventManager.register(this);
        animation = new Animation(200, 1, Easing.LINEAR);
        {
            animation.setDone(true);
        }
        colorCycleIcon =new ColorCycleRGBA(List.of(getCurrentTheme().getColor(),getCurrentTheme().getColor(),getCurrentTheme().getColor(),getCurrentTheme().getColor()),500);
      }

    public void switchTheme(Theme theme) {
        if (animation.isDone()) {
            prevTheme = currentTheme;
            currentTheme = theme;
            int index = themes.indexOf(theme);
            if (index != -1) {
                themeIndex = index;
            }

            animation.reset();
        }
    }

    private final List<Theme> themes = Arrays.asList(Theme.DARK, Theme.LIGHT, Theme.CUSTOM_THEME);
    private int themeIndex = 0;

    public void switchTheme() {
        if (animation.isDone()) {
            prevTheme = currentTheme;
            themeIndex = (themeIndex + 1) % themes.size();
            currentTheme = themes.get(themeIndex);
            animation.reset();
        }
    }

    public void switchThemeByName(String name) {
        if (name == null) return;

        Theme theme;
        switch (name) {
            case "Dark":
                theme = Theme.DARK;
                break;
            case "Light":
                theme = Theme.LIGHT;
                break;
            case "Custom":
                theme = Theme.CUSTOM_THEME;
                break;
            default:
                return;
        }

        switchTheme(theme);
    }

    @EventTarget
    public void eventRender(EventHudRender event) {

        setColorCycle(colorCycleIcon.getBaseColors(),getCurrentTheme().getColor(),getCurrentTheme().getColor(),getCurrentTheme().getColor(),getCurrentTheme().getColor());
        colorCycleIcon.update();



        animation.update(1);
    }
    private void setColorCycle(List<ColorRGBA> colorCycle,ColorRGBA one,ColorRGBA two,ColorRGBA three,ColorRGBA four) {
        colorCycle.set(0,one);
        colorCycle.set(1,two);
        colorCycle.set(2,three);
        colorCycle.set(3,four);
    }
    public Theme getCurrentTheme() {
        return animation.isDone() ? currentTheme : prevTheme.interpolateTheme(currentTheme, animation.getValue());
    }

    public boolean is(Theme theme) {
        return currentTheme == theme;
    }
    public Gradient getClientColor() {
       return Gradient.of(getClientColor(0),getClientColor(90),getClientColor(180),getClientColor(270));
    }
    public ColorRGBA getClientColor(int index) {
       return ColorUtil.lerp(4, index, getCurrentTheme().getColor(), getCurrentTheme().getSecondColor());

    }
}
