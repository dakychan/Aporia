package zenith.zov.client.modules.impl.render;

import net.minecraft.client.option.Perspective;
import net.minecraft.util.hit.HitResult;
import com.darkmagician6.eventapi.EventTarget;
import zenith.zov.base.events.impl.render.EventHudRender;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.BooleanSetting;
import zenith.zov.client.modules.api.setting.impl.NumberSetting;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.client.modules.impl.render.Menu;


// ЕБАНЫЕ ОНАНИСТЫ
@ModuleAnnotation(name = "Crosshair", category = Category.RENDER, description = "Кастомный прицел")
public final class Crosshair extends Module {
    public static final Crosshair INSTANCE = new Crosshair();
    
    private Crosshair() {
    }

    private final NumberSetting thickness = new NumberSetting("Толщина", 1.0f, 0.5f, 3.0f, 0.1f);
    private final NumberSetting length = new NumberSetting("Длина", 3.0f, 1.0f, 8.0f, 0.5f);
    private final NumberSetting gap = new NumberSetting("Разрыв", 2.0f, 0.0f, 5.0f, 0.5f);
    private final BooleanSetting dynamicGap = new BooleanSetting("Динамический разрыв", false);

    private final BooleanSetting useEntityColor = new BooleanSetting("Цвет при наведении", false);


    private final ColorRGBA entityColor = new ColorRGBA(255, 0, 0, 255);


    @EventTarget
    public void onRender(EventHudRender event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (mc.currentScreen != null || Menu.INSTANCE.isEnabled()) {
            return;
        }

        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) {
            return;
        }

        CustomDrawContext ctx = event.getContext();
        float x = mc.getWindow().getScaledWidth() / 2f;
        float y = mc.getWindow().getScaledHeight() / 2f;

        float currentGap = gap.getCurrent();
        if (dynamicGap.isEnabled()) {
            float cooldown = 1 - mc.player.getAttackCooldownProgress(0);
            currentGap += 8 * cooldown;
        }

        float currentThickness = thickness.getCurrent();
        float currentLength = length.getCurrent();

        ColorRGBA color = useEntityColor.isEnabled() && mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY 
            ? entityColor 
            : new ColorRGBA(255, 255, 255, 255);
        // вверхняя залупа
        drawLine(ctx, x - currentThickness / 2, y - currentGap - currentLength,
                currentThickness, currentLength, color);

        // нижняя залупа
        drawLine(ctx, x - currentThickness / 2, y + currentGap,
                currentThickness, currentLength, color);

        // левая залупа
        drawLine(ctx, x - currentGap - currentLength, y - currentThickness / 2,
                currentLength, currentThickness, color);

        // правая залупа
        drawLine(ctx, x + currentGap, y - currentThickness / 2,
                currentLength, currentThickness, color);
    }

    private void drawLine(CustomDrawContext ctx, float x, float y, float width, float height, ColorRGBA color) {
        ctx.drawRect(x, y, width, height, color);
    }
}
