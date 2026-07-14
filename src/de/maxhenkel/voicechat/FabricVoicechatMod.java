package de.maxhenkel.voicechat;

public class FabricVoicechatMod extends Voicechat {

    private static boolean initialized;

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        onInitialize();
    }

    public static void onInitialize() {
        initialize();
    }
}