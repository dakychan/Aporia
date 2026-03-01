package aporia.su.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import aporia.su.events.api.EventHandler;
import aporia.su.events.impl.ModuleToggleEvent;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.SelectSetting;
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.util.Instance;
import aporia.su.util.sounds.SoundManager;

public class ClientSounds extends ModuleStructure {

    public static ClientSounds getInstance() {
        return Instance.get(ClientSounds.class);
    }

    private final SelectSetting soundType = new SelectSetting("Тип звука", "Select sound type")
            .value("New", "Old")
            .selected("New");

    private final SliderSettings volume = new SliderSettings("Громкость", "Set volume")
            .range(0.1f, 2.0f)
            .setValue(1.0f);

    public ClientSounds() {
        super("ClientSounds", ModuleCategory.MISC);
        settings(soundType, volume);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onModuleToggle(ModuleToggleEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getModule() == this) return;

        playToggleSound(event.isEnabled());
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void playToggleSound(boolean enabled) {
        float vol = volume.getValue();

        if (enabled) {
            if (soundType.isSelected("New")) {
                SoundManager.playSound(SoundManager.MODULE_ENABLE, vol, 1);
            } else {
                SoundManager.playSound(SoundManager.ON, vol, 1);
            }
        } else {
            if (soundType.isSelected("New")) {
                SoundManager.playSound(SoundManager.MODULE_DISABLE, vol, 1);
            } else {
                SoundManager.playSound(SoundManager.OFF, vol, 1);
            }
        }
    }
}