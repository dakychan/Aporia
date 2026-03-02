package aporia.su;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import aporia.su.manager.Manager;

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