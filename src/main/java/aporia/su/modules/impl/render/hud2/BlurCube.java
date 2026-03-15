package aporia.su.modules.impl.render.hud2;

import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.BooleanSetting;
import aporia.su.modules.module.setting.implement.ColorSetting;
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.util.events.api.EventHandler;
import aporia.su.util.events.impl.ui.DrawEvent;
import aporia.su.util.user.render.Render2D;

public class BlurCube extends ModuleStructure {

    SliderSettings blurRadius = new SliderSettings("Радиус блюра", "Сила размытия")
            .setValue(8).range(1, 20);

    SliderSettings posX = new SliderSettings("X", "Позиция по горизонтали")
            .setValue(10).range(0, 1920);

    SliderSettings posY = new SliderSettings("Y", "Позиция по вертикали")
            .setValue(10).range(0, 1080);

    SliderSettings width = new SliderSettings("Ширина", "Ширина блока")
            .setValue(200).range(10, 1000);

    SliderSettings height = new SliderSettings("Высота", "Высота блока")
            .setValue(100).range(10, 500);

    SliderSettings cornerRadius = new SliderSettings("Скругление", "Радиус скругления углов")
            .setValue(8).range(0, 30);

    BooleanSetting enableTint = new BooleanSetting("Оттенок", "Включить цветной оттенок")
            .setValue(false);

    ColorSetting tintColor = new ColorSetting("Цвет оттенка", "Цвет оттенка блюра")
            .value(0xFFFFFFFF)
            .visible(enableTint::isValue);

    SliderSettings tintOpacity = new SliderSettings("Прозрачность оттенка", "Прозрачность цветного оттенка")
            .setValue(80).range(0, 255)
            .visible(enableTint::isValue);

    public BlurCube() {
        super("BlurCube", "Отрисовывает blur-прямоугольник на экране", ModuleCategory.RENDER);
        settings(blurRadius, posX, posY, width, height, cornerRadius, enableTint, tintColor, tintOpacity);
    }

    @EventHandler
    public void onDraw(DrawEvent event) {
        if (!isState()) return;
        if (mc.player == null || mc.world == null) return;

        int tint = enableTint.isValue()
                ? (tintOpacity.getInt() << 24) | (tintColor.getColor() & 0x00FFFFFF)
                : 0x00000000;

        Render2D.blur(
                posX.getValue(),
                posY.getValue(),
                width.getValue(),
                height.getValue(),
                blurRadius.getValue(),
                cornerRadius.getValue(),
                tint
        );
    }
}
