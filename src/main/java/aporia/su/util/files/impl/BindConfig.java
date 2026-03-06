package aporia.su.util.files.impl;

import aporia.cc.OsManager;
import aporia.su.util.files.FilesManager;
import aporia.su.util.helper.Logger;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

/**
 * Конфиг для кейбинда открытия ClickGUI.
 */
public class BindConfig {
    private static BindConfig instance;
    private static final String CONFIG_NAME = "bind";
    private static final Path CONFIG_DIR = OsManager.mainDirectory.resolve("configs");

    @Getter
    private int bindKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private BindConfig() {
        load();
    }

    public static BindConfig getInstance() {
        if (instance == null) {
            instance = new BindConfig();
        }
        return instance;
    }

    public void setKey(int key) {
        this.bindKey = key;
    }

    public void setKeyAndSave(int key) {
        setKey(key);
        save();
    }

    public void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("bindKey", bindKey);
            
            FilesManager.createFile(
                CONFIG_DIR,
                FilesManager.FileFormat.APR,
                CONFIG_NAME,
                obj.toString(),
                FilesManager.CheckMode.ALWAYS
            );
        } catch (Exception e) {
            Logger.error("BindConfig: Save failed! " + e.getMessage());
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
            if (obj.has("bindKey")) {
                bindKey = obj.get("bindKey").getAsInt();
            }
            
            Logger.success("BindConfig loaded!");
        } catch (Exception e) {
            Logger.error("BindConfig: Load failed! " + e.getMessage());
        }
    }
}
