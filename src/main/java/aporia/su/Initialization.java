package aporia.su;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import aporia.su.manager.Manager;
import aporia.su.util.mods.config.wave.HeartbeatManager;
import antidaunleak.api.UserProfile;
import antidaunleak.api.annotation.Native;

public class Initialization implements ClientModInitializer {

    @Getter
    private static Initialization instance;

    @Getter
    private Manager manager;

    @Override
    public void onInitializeClient() {

    }

    public void init() {
        instance = this;
        manager = new Manager();
        manager.init();
    }
}