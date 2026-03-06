package aporia.su.util.files.impl;

import aporia.cc.OsManager;
import aporia.su.util.files.FilesManager;
import aporia.su.util.helper.Logger;
import aporia.su.util.user.chat.command.CommandManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;

import java.nio.file.Path;

/**
 * Конфиг для префикса команд.
 */
public class PrefixConfig {
    private static PrefixConfig instance;
    private static final String CONFIG_NAME = "prefix";
    private static final Path CONFIG_DIR = OsManager.mainDirectory.resolve("configs");

    @Getter
    private String prefix = ".";

    private PrefixConfig() {}

    public static PrefixConfig getInstance() {
        if (instance == null) {
            instance = new PrefixConfig();
        }
        return instance;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        if (CommandManager.getInstance() != null) {
            CommandManager.getInstance().setPrefix(prefix);
        }
    }

    public void setPrefixAndSave(String prefix) {
        setPrefix(prefix);
        save();
    }

    public void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("prefix", prefix);
            
            FilesManager.createFile(
                CONFIG_DIR,
                FilesManager.FileFormat.APR,
                CONFIG_NAME,
                obj.toString(),
                FilesManager.CheckMode.ALWAYS
            );
        } catch (Exception e) {
            Logger.error("PrefixConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            Path configPath = FilesManager.getFilePath(CONFIG_DIR, CONFIG_NAME, FilesManager.FileFormat.APR);
            
            if (!FilesManager.exists(configPath)) {
                return;
            }
            
            String json = FilesManager.readFile(configPath);
            if (json == null || json.isEmpty()) {
                return;
            }
            
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("prefix")) {
                String loadedPrefix = obj.get("prefix").getAsString();
                if (!loadedPrefix.isEmpty()) {
                    this.prefix = loadedPrefix;
                }
            }
            
            Logger.success("PrefixConfig loaded!");
        } catch (Exception e) {
            Logger.error("PrefixConfig: Load failed! " + e.getMessage());
        }
    }
}
