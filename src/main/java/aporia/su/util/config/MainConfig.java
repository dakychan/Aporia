package aporia.su.util.config;

import aporia.su.Initialization;
import aporia.su.modules.module.ModuleRepository;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.setting.Setting;
import aporia.su.modules.module.setting.implement.*;
import aporia.su.util.config.impl.loggers.autosaver.ConfigAutoSaver;
import aporia.su.util.config.impl.player.autobuyconfig.AutoBuyConfig;
import aporia.su.util.config.impl.loggers.consolelogger.Logger;
import com.google.gson.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MainConfig {
    public static class ConfigSerializer {

        private static final Gson GSON = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        public String serialize() {
            JsonObject root = new JsonObject();
            JsonObject modulesJson = new JsonObject();

            ModuleRepository repository = getModuleRepository();
            if (repository != null) {
                for (ModuleStructure module : repository.modules()) {
                    JsonObject moduleJson = serializeModule(module);
                    modulesJson.add(module.getName(), moduleJson);
                }
            }

            root.add("modules", modulesJson);
            root.add("autobuy", serializeAutoBuy());
            root.addProperty("version", "0.4");
            root.addProperty("timestamp", System.currentTimeMillis());
            root.addProperty("client", "Aporia.cc");

            return GSON.toJson(root);
        }

        private JsonObject serializeAutoBuy() {
            JsonObject autoBuyJson = new JsonObject();
            AutoBuyConfig autoBuyConfig = AutoBuyConfig.getInstance();

            autoBuyJson.addProperty("globalEnabled", autoBuyConfig.isGlobalEnabled());

            JsonObject itemsJson = new JsonObject();
            Map<String, AutoBuyConfig.ItemConfig> allItems = autoBuyConfig.getAllItemConfigs();

            for (Map.Entry<String, AutoBuyConfig.ItemConfig> entry : allItems.entrySet()) {
                JsonObject itemJson = new JsonObject();
                AutoBuyConfig.ItemConfig itemConfig = entry.getValue();
                itemJson.addProperty("enabled", itemConfig.isEnabled());
                itemJson.addProperty("buyBelow", itemConfig.getBuyBelow());
                itemJson.addProperty("minQuantity", itemConfig.getMinQuantity());
                itemsJson.add(entry.getKey(), itemJson);
            }

            autoBuyJson.add("items", itemsJson);
            return autoBuyJson;
        }

        private JsonObject serializeModule(ModuleStructure module) {
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("enabled", module.isState());
            moduleJson.addProperty("key", module.getKey());
            moduleJson.addProperty("type", module.getType());
            moduleJson.addProperty("favorite", module.isFavorite());

            JsonObject settingsJson = new JsonObject();
            for (Setting setting : module.settings()) {
                JsonElement element = serializeSetting(setting);
                if (element != null) {
                    settingsJson.add(setting.getName(), element);
                }
            }
            moduleJson.add("settings", settingsJson);

            return moduleJson;
        }

        private JsonElement serializeSetting(Setting setting) {
            if (setting instanceof BooleanSetting boolSetting) {
                return new JsonPrimitive(boolSetting.isValue());
            }
            if (setting instanceof SliderSettings sliderSetting) {
                return new JsonPrimitive(sliderSetting.getValue());
            }
            if (setting instanceof BindSetting bindSetting) {
                JsonObject bindJson = new JsonObject();
                bindJson.addProperty("key", bindSetting.getKey());
                bindJson.addProperty("type", bindSetting.getType());
                return bindJson;
            }
            if (setting instanceof TextSetting textSetting) {
                return new JsonPrimitive(textSetting.getText() != null ? textSetting.getText() : "");
            }
            if (setting instanceof SelectSetting selectSetting) {
                return new JsonPrimitive(selectSetting.getSelected());
            }
            if (setting instanceof ColorSetting colorSetting) {
                JsonObject colorJson = new JsonObject();
                colorJson.addProperty("hue", colorSetting.getHue());
                colorJson.addProperty("saturation", colorSetting.getSaturation());
                colorJson.addProperty("brightness", colorSetting.getBrightness());
                colorJson.addProperty("alpha", colorSetting.getAlpha());
                return colorJson;
            }
            if (setting instanceof MultiSelectSetting multiSetting) {
                JsonArray array = new JsonArray();
                for (String value : multiSetting.getSelected()) {
                    array.add(value);
                }
                return array;
            }
            if (setting instanceof GroupSetting groupSetting) {
                JsonObject groupJson = new JsonObject();
                groupJson.addProperty("value", groupSetting.isValue());
                JsonObject subSettingsJson = new JsonObject();
                for (Setting subSetting : groupSetting.getSubSettings()) {
                    JsonElement element = serializeSetting(subSetting);
                    if (element != null) {
                        subSettingsJson.add(subSetting.getName(), element);
                    }
                }
                groupJson.add("subSettings", subSettingsJson);
                return groupJson;
            }
            return null;
        }

        public void deserialize(String json) {
            try {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();

                if (root.has("modules")) {
                    JsonObject modulesJson = root.getAsJsonObject("modules");
                    ModuleRepository repository = getModuleRepository();
                    if (repository != null) {
                        for (ModuleStructure module : repository.modules()) {
                            if (modulesJson.has(module.getName())) {
                                deserializeModule(module, modulesJson.getAsJsonObject(module.getName()));
                            }
                        }
                    }
                }

                if (root.has("autobuy")) {
                    deserializeAutoBuy(root.getAsJsonObject("autobuy"));
                }
            } catch (JsonSyntaxException e) {
                Logger.error("AutoConfiguration: JSON syntax error!");
            }
        }

        private void deserializeAutoBuy(JsonObject autoBuyJson) {
            AutoBuyConfig autoBuyConfig = AutoBuyConfig.getInstance();

            if (autoBuyJson.has("globalEnabled")) {
                autoBuyConfig.setGlobalEnabled(autoBuyJson.get("globalEnabled").getAsBoolean());
            }

            if (autoBuyJson.has("items")) {
                JsonObject itemsJson = autoBuyJson.getAsJsonObject("items");
                for (Map.Entry<String, JsonElement> entry : itemsJson.entrySet()) {
                    String itemName = entry.getKey();
                    JsonObject itemJson = entry.getValue().getAsJsonObject();

                    boolean enabled = itemJson.has("enabled") && itemJson.get("enabled").getAsBoolean();
                    int buyBelow = itemJson.has("buyBelow") ? itemJson.get("buyBelow").getAsInt() : 1000;
                    int minQuantity = itemJson.has("minQuantity") ? itemJson.get("minQuantity").getAsInt() : 1;

                    AutoBuyConfig.ItemConfig itemConfig = new AutoBuyConfig.ItemConfig(enabled, buyBelow, minQuantity);
                    autoBuyConfig.setItemConfig(itemName, itemConfig);
                }
            }
        }

        private void deserializeModule(ModuleStructure module, JsonObject moduleJson) {
            if (moduleJson.has("enabled")) {
                boolean enabled = moduleJson.get("enabled").getAsBoolean();
                if (enabled) {
                    module.setState(true);
                }
            }
            if (moduleJson.has("key")) {
                module.setKey(moduleJson.get("key").getAsInt());
            }
            if (moduleJson.has("type")) {
                module.setType(moduleJson.get("type").getAsInt());
            }
            if (moduleJson.has("favorite")) {
                module.setFavorite(moduleJson.get("favorite").getAsBoolean());
            }
            if (moduleJson.has("settings")) {
                JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                for (Setting setting : module.settings()) {
                    if (settingsJson.has(setting.getName())) {
                        deserializeSetting(setting, settingsJson.get(setting.getName()));
                    }
                }
            }
        }

        private void deserializeSetting(Setting setting, JsonElement element) {
            try {
                if (setting instanceof BooleanSetting boolSetting) {
                    boolSetting.setValue(element.getAsBoolean());
                } else if (setting instanceof SliderSettings sliderSetting) {
                    sliderSetting.setValue((float) element.getAsDouble());
                } else if (setting instanceof BindSetting bindSetting) {
                    if (element.isJsonObject()) {
                        JsonObject bindJson = element.getAsJsonObject();
                        if (bindJson.has("key")) {
                            bindSetting.setKey(bindJson.get("key").getAsInt());
                        }
                        if (bindJson.has("type")) {
                            bindSetting.setType(bindJson.get("type").getAsInt());
                        }
                    } else {
                        bindSetting.setKey(element.getAsInt());
                    }
                } else if (setting instanceof TextSetting textSetting) {
                    textSetting.setText(element.getAsString());
                } else if (setting instanceof SelectSetting selectSetting) {
                    selectSetting.setSelected(element.getAsString());
                } else if (setting instanceof ColorSetting colorSetting) {
                    if (element.isJsonObject()) {
                        JsonObject colorJson = element.getAsJsonObject();
                        if (colorJson.has("hue")) {
                            colorSetting.setHue(colorJson.get("hue").getAsFloat());
                        }
                        if (colorJson.has("saturation")) {
                            colorSetting.setSaturation(colorJson.get("saturation").getAsFloat());
                        }
                        if (colorJson.has("brightness")) {
                            colorSetting.setBrightness(colorJson.get("brightness").getAsFloat());
                        }
                        if (colorJson.has("alpha")) {
                            colorSetting.setAlpha(colorJson.get("alpha").getAsFloat());
                        }
                    } else {
                        colorSetting.setColor(element.getAsInt());
                    }
                } else if (setting instanceof MultiSelectSetting multiSetting) {
                    if (element.isJsonArray()) {
                        JsonArray array = element.getAsJsonArray();
                        List<String> selected = new ArrayList<>();
                        for (JsonElement e : array) {
                            selected.add(e.getAsString());
                        }
                        multiSetting.setSelected(selected);
                    }
                } else if (setting instanceof GroupSetting groupSetting) {
                    if (element.isJsonObject()) {
                        JsonObject groupJson = element.getAsJsonObject();
                        if (groupJson.has("value")) {
                            groupSetting.setValue(groupJson.get("value").getAsBoolean());
                        }
                        if (groupJson.has("subSettings")) {
                            JsonObject subSettingsJson = groupJson.getAsJsonObject("subSettings");
                            for (Setting subSetting : groupSetting.getSubSettings()) {
                                if (subSettingsJson.has(subSetting.getName())) {
                                    deserializeSetting(subSetting, subSettingsJson.get(subSetting.getName()));
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        private ModuleRepository getModuleRepository() {
            Initialization instance = Initialization.getInstance();
            if (instance != null && instance.getManager() != null) {
                return instance.getManager().getModuleRepository();
            }
            return null;
        }
    }

    /**
     *  © 2026 Copyright Aporia.cc 2.0
     *        All Rights Reserved ®
     */

    public static class ConfigPath {

        private static final String ROOT_DIR = "Aporia";
        private static final String CONFIG_DIR = "configs";
        private static final String AUTO_DIR = "autocfg";
        private static final String CONFIG_FILE = "autoconfig.json";

        private static Path runDirectory;

        public static void init() {
            runDirectory = Paths.get("").toAbsolutePath();
        }

        public static Path getConfigDirectory() {
            return runDirectory.resolve(ROOT_DIR).resolve(CONFIG_DIR).resolve(AUTO_DIR);
        }

        public static Path getConfigFile() {
            return getConfigDirectory().resolve(CONFIG_FILE);
        }
    }

    /**
     *  © 2026 Copyright Aporia.cc 2.0
     *        All Rights Reserved ®
     */

    public static class ConfigFileHandler {

        private final ReentrantReadWriteLock lock;

        public ConfigFileHandler() {
            this.lock = new ReentrantReadWriteLock();
        }

        public void createDirectories() {
            try {
                Files.createDirectories(ConfigPath.getConfigDirectory());
            } catch (IOException e) {
                Logger.error("AutoConfiguration: Failed to create directories!");
            }
        }

        public boolean write(String content) {
            lock.writeLock().lock();
            try {
                Path configFile = ConfigPath.getConfigFile();
                Path tempFile = configFile.resolveSibling(configFile.getFileName() + ".tmp");

                Files.writeString(tempFile, content, StandardCharsets.UTF_8);
                Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

                return true;
            } catch (IOException e) {
                Logger.error("AutoConfiguration: Write failed! " + e.getMessage());
                return false;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public String read() {
            lock.readLock().lock();
            try {
                Path configFile = ConfigPath.getConfigFile();

                if (!Files.exists(configFile)) {
                    return null;
                }

                return Files.readString(configFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                Logger.error("AutoConfiguration: Read failed! " + e.getMessage());
                return null;
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean exists() {
            return Files.exists(ConfigPath.getConfigFile());
        }
    }

    /**
     *  © 2026 Copyright Aporia.cc 2.0
     *        All Rights Reserved ®
     */

    public static class ConfigSystem {

        private static ConfigSystem instance;

        private final ConfigSerializer serializer;
        private final ConfigFileHandler fileHandler;
        private final ConfigAutoSaver autoSaver;
        private final AtomicBoolean initialized;
        private final AtomicBoolean saving;

        public ConfigSystem() {
            instance = this;
            this.serializer = new ConfigSerializer();
            this.fileHandler = new ConfigFileHandler();
            this.autoSaver = new ConfigAutoSaver(this::save);
            this.initialized = new AtomicBoolean(false);
            this.saving = new AtomicBoolean(false);
        }

        public static ConfigSystem getInstance() {
            return instance;
        }

        public void init() {
            if (initialized.compareAndSet(false, true)) {
                ConfigPath.init();
                fileHandler.createDirectories();
                load();
                autoSaver.start();
                registerShutdownHook();
                Logger.success("AutoConfiguration: System initialized!");
            }
        }

        private void registerShutdownHook() {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                Logger.info("AutoConfiguration: Shutdown detected, saving...");
                shutdown();
            }, "Aporia-ConfigShutdown"));
        }

        public void save() {
            if (!initialized.get()) {
                return;
            }
            if (!saving.compareAndSet(false, true)) {
                return;
            }
            try {
                String data = serializer.serialize();
                boolean success = fileHandler.write(data);
                if (success) {
                    Logger.success("AutoConfiguration: autoconfig.json saved successfully!");
                } else {
                    Logger.error("AutoConfiguration: autoconfig.json save failed!");
                }
            } catch (Exception e) {
                Logger.error("AutoConfiguration: Save error! " + e.getMessage());
            } finally {
                saving.set(false);
            }
        }

        public CompletableFuture<Void> saveAsync() {
            return CompletableFuture.runAsync(this::save);
        }

        public void load() {
            if (!fileHandler.exists()) {
                Logger.info("AutoConfiguration: No config found, creating new...");
                save();
                return;
            }
            try {
                String data = fileHandler.read();
                if (data != null && !data.isEmpty()) {
                    serializer.deserialize(data);
                    Logger.success("AutoConfiguration: autoconfig.json loaded successfully!");
                }
            } catch (Exception e) {
                Logger.error("AutoConfiguration: Load error! " + e.getMessage());
            }
        }

        public void shutdown() {
            if (!initialized.get()) {
                return;
            }
            autoSaver.shutdown();
            save();
            Logger.success("AutoConfiguration: Shutdown complete!");
        }

        public void reload() {
            load();
            Logger.success("AutoConfiguration: Config reloaded!");
        }

        public boolean isInitialized() {
            return initialized.get();
        }

        public boolean isSaving() {
            return saving.get();
        }

        public ConfigAutoSaver getAutoSaver() {
            return autoSaver;
        }
    }
}
