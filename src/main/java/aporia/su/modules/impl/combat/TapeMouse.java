package aporia.su.modules.impl.combat;

import anidumpproject.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.Hand;
import aporia.su.events.api.EventHandler;
import aporia.su.events.impl.TickEvent;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.SelectSetting;
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.util.timer.StopWatch;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TapeMouse extends ModuleStructure {

    final SelectSetting modeClick = new SelectSetting("Тип", "Тип клика")
            .value("Левая кнопка", "Правая кнопка")
            .selected("Левая кнопка");

    final SliderSettings delayForClick = new SliderSettings("Задержка", "Задержка между кликами")
            .range(1.0f, 15.0f).setValue(1.0f);

    final StopWatch delay = new StopWatch();

    public TapeMouse() {
        super("TapeMouse", "Tape Mouse", ModuleCategory.COMBAT);
        settings(modeClick, delayForClick);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        long delayMs = (long) (delayForClick.getValue() * 300.0f);

        if (!delay.finished(delayMs)) return;

        performClick();
        delay.reset();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void performClick() {
        if (modeClick.isSelected("Левая кнопка")) {
            leftClick();
        } else {
            rightClick();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void leftClick() {
        if (mc.interactionManager == null) return;

        if (mc.targetedEntity != null) {
            mc.interactionManager.attackEntity(mc.player, mc.targetedEntity);
            mc.player.swingHand(Hand.MAIN_HAND);
        } else if (mc.crosshairTarget != null) {
            mc.doAttack();
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void rightClick() {
        if (mc.interactionManager == null) return;
        mc.doItemUse();
    }
}