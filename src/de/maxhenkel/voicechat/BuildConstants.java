package de.maxhenkel.voicechat;

public class BuildConstants {

    public static final int COMPATIBILITY_VERSION;
    public static final String MINECRAFT_VERSION = "1.21.11-2.6.18";
    public static final String MOD_COMPATIBLE_VERSION = "2.6.x";

    static {
        String compatibilityVersionString = "20";
        int compatibilityVersion;
        try {
            compatibilityVersion = Integer.parseInt(compatibilityVersionString);
        } catch (NumberFormatException e1) {
            try {
                compatibilityVersion = Integer.parseInt(System.getenv("COMPATIBILITY_VERSION"));
            } catch (NumberFormatException e2) {
                compatibilityVersion = -1;
            }
        }
        COMPATIBILITY_VERSION = compatibilityVersion;
    }

}
