package aporia.su.util.files.impl;

import aporia.cc.OsManager;
import aporia.su.util.files.FilesManager;
import aporia.su.util.helper.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Конфиг для автопокупки предметов.
 */
public class AutoBuyConfig {
    private static AutoBuyConfig instance;
    private static final String CONFIG_NAME = "autobuy";
    private static final Path CONFIG_DIR = OsManager.mainDirectory.resolve("configs").resolve("autobuy");
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    @Getter
    private ConfigData data = new ConfigData();

    @Getter
    @Setter
    public static class ItemConfig {
        private boolean enabled = false;
        private int buyBelow = 1000;
        private int minQuantity = 1;

        public ItemConfig() {}

        public ItemConfig(boolean enabled, int buyBelow, int minQuantity) {
            this.enabled = enabled;
            this.buyBelow = buyBelow;
            this.minQuantity = minQuantity;
        }
    }

    @Getter
    @Setter
    public static class ConfigData {
        private boolean globalEnabled = false;
        private Map<String, ItemConfig> items = new HashMap<>();
    }

    private AutoBuyConfig() {
        load();
    }

    public static AutoBuyConfig getInstance() {
        if (instance == null) {
            instance = new AutoBuyConfig();
        }
        return instance;
    }

    public void load() {
        try {
            Path configPath = FilesManager.getFilePath(CONFIG_DIR, CONFIG_NAME, FilesManager.FileFormat.APR);
            
            if (FilesManager.exists(configPath)) {
                String json = FilesManager.readFile(configPath);
                if (json != null && !json.isEmpty()) {
                    ConfigData loaded = gson.fromJson(json, ConfigData.class);
                    if (loaded != null) {
                        this.data = loaded;
                        if (this.data.getItems() == null) {
                            this.data.setItems(new HashMap<>());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("AutoBuyConfig: Load failed! " + e.getMessage());
        }
    }

    public void save() {
        try {
            String json = gson.toJson(data);
            FilesManager.createFile(
                CONFIG_DIR,
                FilesManager.FileFormat.APR,
                CONFIG_NAME,
                json,
                FilesManager.CheckMode.ALWAYS
            );
        } catch (Exception e) {
            Logger.error("AutoBuyConfig: Save failed! " + e.getMessage());
        }
    }

    public void reset() {
        data = new ConfigData();
        try {
            Path configPath = FilesManager.getFilePath(CONFIG_DIR, CONFIG_NAME, FilesManager.FileFormat.APR);
            if (FilesManager.exists(configPath)) {
                FilesManager.deleteFile(configPath);
            }
        } catch (Exception e) {
            Logger.error("AutoBuyConfig: Reset failed! " + e.getMessage());
        }
        save();
    }

    public boolean isGlobalEnabled() {
        return data.isGlobalEnabled();
    }

    public void setGlobalEnabled(boolean enabled) {
        data.setGlobalEnabled(enabled);
    }

    public void setGlobalEnabledAndSave(boolean enabled) {
        data.setGlobalEnabled(enabled);
        save();
    }

    public ItemConfig getItemConfig(String itemName) {
        return data.getItems().computeIfAbsent(itemName, k -> new ItemConfig());
    }

    public ItemConfig getItemConfigOrNull(String itemName) {
        return data.getItems().get(itemName);
    }

    public void setItemConfig(String itemName, ItemConfig config) {
        data.getItems().put(itemName, config);
    }

    public void setItemConfigAndSave(String itemName, ItemConfig config) {
        data.getItems().put(itemName, config);
        save();
    }

    public void setItemEnabled(String itemName, boolean enabled) {
        ItemConfig config = getItemConfig(itemName);
        config.setEnabled(enabled);
    }

    public void setItemEnabledAndSave(String itemName, boolean enabled) {
        ItemConfig config = getItemConfig(itemName);
        config.setEnabled(enabled);
        save();
    }

    public void setItemBuyBelow(String itemName, int buyBelow) {
        ItemConfig config = getItemConfig(itemName);
        config.setBuyBelow(buyBelow);
    }

    public void setItemBuyBelowAndSave(String itemName, int buyBelow) {
        ItemConfig config = getItemConfig(itemName);
        config.setBuyBelow(buyBelow);
        save();
    }

    public void setItemMinQuantity(String itemName, int minQuantity) {
        ItemConfig config = getItemConfig(itemName);
        config.setMinQuantity(minQuantity);
    }

    public void setItemMinQuantityAndSave(String itemName, int minQuantity) {
        ItemConfig config = getItemConfig(itemName);
        config.setMinQuantity(minQuantity);
        save();
    }

    public boolean isItemEnabled(String itemName) {
        ItemConfig config = getItemConfigOrNull(itemName);
        return config != null && config.isEnabled();
    }

    public int getItemBuyBelow(String itemName) {
        return getItemConfig(itemName).getBuyBelow();
    }

    public int getItemMinQuantity(String itemName) {
        return getItemConfig(itemName).getMinQuantity();
    }

    public boolean hasItemConfig(String itemName) {
        return data.getItems().containsKey(itemName);
    }

    public void loadItemSettings(String itemName, int defaultPrice) {
        if (!hasItemConfig(itemName)) {
            ItemConfig config = new ItemConfig(false, defaultPrice, 1);
            data.getItems().put(itemName, config);
        }
    }

    public Map<String, ItemConfig> getAllItemConfigs() {
        return new HashMap<>(data.getItems());
    }
}
