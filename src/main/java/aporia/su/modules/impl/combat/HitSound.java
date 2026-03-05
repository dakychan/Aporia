package aporia.su.modules.impl.combat;

import anidumpproject.api.annotation.Native;
import net.minecraft.entity.LivingEntity;
import aporia.su.util.events.api.EventHandler;
import aporia.su.util.events.impl.AttackEvent;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.SelectSetting;
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.util.Instance;
import aporia.su.util.user.render.sounds.SoundManager;

import java.util.concurrent.ThreadLocalRandom;

public class HitSound extends ModuleStructure {

    public static HitSound getInstance() {
        return Instance.get(HitSound.class);
    }

    private final SelectSetting soundType = new SelectSetting("Тип звука", "Select sound type")
            .value("Moan", "Metallic", "Crime")
            .selected("Moan");

    private final SliderSettings volume = new SliderSettings("Громкость", "Set volume")
            .range(0.1f, 2.0f)
            .setValue(1.0f);

    public HitSound() {
        super("HitSound", ModuleCategory.COMBAT);
        settings(soundType, volume);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onAttack(AttackEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!(event.getTarget() instanceof LivingEntity)) return;

        playSelectedSound();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void playSelectedSound() {
        float vol = volume.getValue();

        if (soundType.isSelected("Moan")) {
            playRandomMoan(vol, 1);
        }

        if (soundType.isSelected("Metallic")) {
            SoundManager.playSound(SoundManager.METALLIC, vol, 1);
        }

        if (soundType.isSelected("Crime")) {
            SoundManager.playSound(SoundManager.CRIME, vol, 1);
        }
    }

    private void playRandomMoan(float volume, float pitch) {
        int random = ThreadLocalRandom.current().nextInt(4);
        switch (random) {
            case 0 -> SoundManager.playSound(SoundManager.MOAN1, volume, pitch);
            case 1 -> SoundManager.playSound(SoundManager.MOAN2, volume, pitch);
            case 2 -> SoundManager.playSound(SoundManager.MOAN3, volume, pitch);
            case 3 -> SoundManager.playSound(SoundManager.MOAN4, volume, pitch);
        }
    }
}