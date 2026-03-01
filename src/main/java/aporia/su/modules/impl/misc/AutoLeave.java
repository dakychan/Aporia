package aporia.su.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.Text;
import aporia.su.events.api.EventHandler;
import aporia.su.events.impl.TickEvent;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.MultiSelectSetting;
import aporia.su.modules.module.setting.implement.SelectSetting;
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.util.network.Network;
import aporia.su.util.repository.friend.FriendUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AutoLeave extends ModuleStructure {
    SelectSetting leaveType = new SelectSetting("Тип выхода", "Позволяет выбрать тип выхода")
            .value("Hub", "Main Menu").selected("Main Menu");

    MultiSelectSetting triggerSetting = new MultiSelectSetting("Триггеры", "Выберите, в каких случаях произойдет выход")
            .value("Players", "Staff").selected("Players", "Staff");

    SliderSettings distanceSetting = new SliderSettings("Максимальная дистанция", "Максимальная дистанция для активации авто-выхода")
            .setValue(10).range(5, 40).visible(() -> triggerSetting.isSelected("Players"));

    public AutoLeave() {
        super("AutoLeave", "Auto Leave", ModuleCategory.MISC);
        settings(leaveType, triggerSetting, distanceSetting);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (Network.isPvp()) return;

        if (triggerSetting.isSelected("Players"))
            mc.world.getPlayers().stream().filter(p -> mc.player.distanceTo(p) < distanceSetting.getValue() && mc.player != p && !FriendUtils.isFriend(p)).findFirst().ifPresent(p -> leave(p.getName().copy().append(" - Появился рядом " + mc.player.distanceTo(p) + "м")));
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    public void leave(Text text) {
        switch (leaveType.getSelected()) {
            case "Hub" -> {
                mc.getNetworkHandler().sendChatCommand("hub");
            }
            case "Main Menu" ->
                    mc.getNetworkHandler().getConnection().disconnect(Text.of("[Auto Leave] \n").copy().append(text));
        }
        setState(false);
    }
}