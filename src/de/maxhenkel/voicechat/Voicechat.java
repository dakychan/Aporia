package de.maxhenkel.voicechat;

import de.maxhenkel.configbuilder.ConfigBuilder;
import de.maxhenkel.voicechat.command.VoicechatCommands;
import de.maxhenkel.voicechat.config.ServerConfig;
import de.maxhenkel.voicechat.config.Translations;
import de.maxhenkel.voicechat.intercompatibility.CommonCompatibilityManager;
import de.maxhenkel.voicechat.logging.Log4JVoicechatLogger;
import de.maxhenkel.voicechat.logging.VoicechatLogger;
import de.maxhenkel.voicechat.plugins.PluginManager;
import de.maxhenkel.voicechat.voice.server.ServerVoiceEvents;

import java.nio.file.Path;
import java.util.regex.Pattern;

public abstract class Voicechat {

    public static final String MODID = "voicechat";
    public static final String VERSION = "1.21.11-2.6.18"; //1.21.11-2.6.18 1.16.5-2.6.18
    public static final String MOD_NAME = "Simple Voice Chat";
    public static final VoicechatLogger LOGGER = new Log4JVoicechatLogger(MODID);
    public static ServerVoiceEvents SERVER;
    public static ServerConfig SERVER_CONFIG;
    public static Translations TRANSLATIONS;

    public static int COMPATIBILITY_VERSION = BuildConstants.COMPATIBILITY_VERSION;

    public static final int MAX_GROUP_NAME_LENGTH = 24;
    public static final Pattern GROUP_REGEX = Pattern.compile("^[^\\p{C}\\s][^\\p{C}]{0,23}$");

    public static void initialize() {
        if (debugMode()) {
            LOGGER.warn("Running in debug mode - Don't leave this enabled in production!");
        }

        LOGGER.info("Compatibility version {}", COMPATIBILITY_VERSION);

        initializeConfigs();

        CommonCompatibilityManager.INSTANCE.getNetManager().init();
        SERVER = new ServerVoiceEvents();
        initPlugins();
        registerCommands();
    }

    protected static void initPlugins() {
        PluginManager.instance().init();
    }

    protected static void registerCommands() {
        CommonCompatibilityManager.INSTANCE.onRegisterServerCommands(VoicechatCommands::register);
    }

    public static void initializeConfigs() {
        SERVER_CONFIG = ConfigBuilder.builder(ServerConfig::new).path(getVoicechatConfigFolderInternal().resolve("voicechat-server.properties")).build();
        TRANSLATIONS = ConfigBuilder.builder(Voicechat::createTranslations).path(getVoicechatConfigFolderInternal().resolve("translations.properties")).build();
    }

    protected static Translations createTranslations(ConfigBuilder builder) {
        return new Translations(builder);
    }

    public static boolean debugMode() {
        return CommonCompatibilityManager.INSTANCE.isDevEnvironment() || System.getProperty("voicechat.debug") != null;
    }

    protected static Path getVoicechatConfigFolderInternal() {
        return getVoicechatConfigFolder();
    }

    public static Path getVoicechatConfigFolder() {
        return getConfigFolder().resolve(MODID);
    }

    public static Path getConfigFolder() {
        return Path.of(".").resolve("config");
    }

}
