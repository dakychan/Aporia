package zenith.zov.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import zenith.zov.base.events.impl.input.EventKey;
import zenith.zov.base.events.impl.input.EventHotBarScroll;
import zenith.zov.base.events.impl.input.EventMouseRotation;
import zenith.zov.base.events.impl.render.*;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.KeySetting;
import zenith.zov.client.modules.api.setting.impl.MultiBooleanSetting;
import zenith.zov.client.modules.api.setting.impl.NumberSetting;
import zenith.zov.utility.game.player.PlayerIntersectionUtil;
import zenith.zov.utility.game.player.rotation.Rotation;
import zenith.zov.utility.math.MathUtil;

import java.util.List;

@ModuleAnnotation(
        name = "CameraTweaks",
        description = "Настройки камеры",
        category = Category.MISC
)
public class CameraTweaks extends Module {
    public static final CameraTweaks INSTANCE = new CameraTweaks();
    private float fov = 110, smoothFov = 30, lastChangedFov = 30;
    private Perspective perspective;
    private Rotation angle;

    private final MultiBooleanSetting multiSetting = MultiBooleanSetting.create(
            "Настройки",
            List.of("Соотношение сторон", "Клип камеры", "Дистанция камеры")
    );

    private final NumberSetting ratioSetting =
            new NumberSetting("Соотношение сторон", 1f, 0.1f, 2.0f, 0.1f,
                    () -> multiSetting.isEnable(0));

    private final NumberSetting distanceSetting =
            new NumberSetting("Дистанция камеры", 3.0F, 2.0F, 5.0F, 0.5f,
                    () -> multiSetting.isEnable(2));

    private final KeySetting zoomSetting = new KeySetting("Зум");
    private final KeySetting freeLookSetting = new KeySetting("Свободный взгляд");

    private CameraTweaks() {}

    @EventTarget
    public void onKey(EventKey e) {
        if (e.is(zoomSetting.getKeyCode())) {
            fov = Math.min(lastChangedFov, mc.options.getFov().getValue() - 20);
        }
        if (e.isKeyReleased(zoomSetting.getKeyCode(), true)) {
            lastChangedFov = fov;
            fov = mc.options.getFov().getValue();
        }
        if (e.isKeyDown(freeLookSetting.getKeyCode())) {
            perspective = mc.options.getPerspective();
        }
    }

    @EventTarget
    public void onHotBarScroll(EventHotBarScroll e) {
        if (PlayerIntersectionUtil.isKey(zoomSetting)) {
            fov = (int) MathHelper.clamp(fov - e.getVertical() * 10, 10, mc.options.getFov().getValue());
            e.setCancelled(true);
        }
    }

    @EventTarget
    public void onFov(EventFov e) {
        if (PlayerIntersectionUtil.isKey(freeLookSetting)) {
            if (mc.options.getPerspective().isFirstPerson())
                mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        } else if (perspective != null) {
            mc.options.setPerspective(perspective);
            perspective = null;
        }
        if (zoomSetting.isVisible()) {
            e.setFov((int) MathHelper.clamp(
                    (smoothFov = MathUtil.interpolateSmooth(1.6, smoothFov, fov)) + 1,
                    10,
                    mc.options.getFov().getValue()
            ));
            e.cancel();
        }
    }

    @EventTarget
    public void onMouseRotation(EventMouseRotation e) {
        if (PlayerIntersectionUtil.isKey(freeLookSetting)) {
            angle = new Rotation(
                    angle.getYaw() + e.getCursorDeltaX() * 0.15F,
                    MathHelper.clamp(angle.getPitch() + e.getCursorDeltaY() * 0.15F, -90F, 90F)
            );
            e.setCancelled(true);
        } else {
            angle = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        }
    }

    @EventTarget
    public void onCamera(EventCamera e) {
        e.setCameraClip(multiSetting.isEnable(1));
        if (multiSetting.isEnable(2)) {
            e.setDistance(distanceSetting.getCurrent());
        }
        e.setAngle(angle);
        e.cancel();
    }

    @EventTarget
    public void onAspectRatio(EventAspectRatio e) {
        if (multiSetting.isEnable(0)) {
            e.setRatio(ratioSetting.getCurrent());
            e.setCancelled(true);
        }
    }
}
