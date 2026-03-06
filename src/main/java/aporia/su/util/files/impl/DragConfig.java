package aporia.su.util.files.impl;

import aporia.cc.OsManager;
import aporia.su.Initialization;
import aporia.su.util.files.FilesManager;
import aporia.su.util.helper.Logger;
import aporia.su.util.user.render.screens.hud.api.HudElement;
import aporia.su.util.user.render.screens.hud.api.HudManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Path;

/**
 * Конфиг для позиций HUD элементов.
 */
public class DragConfig {
    private static DragConfig instance;
    private static final String CONFIG_NAME = "draggables";
    private static final Path CONFIG_DIR = OsManager.mainDirectory.resolve("configs");

    private DragConfig() {}

    public static DragConfig getInstance() {
        if (instance == null) {
            instance = new DragConfig();
        }
        return instance;
    }

    public void save() {
        try {
            HudManager hudManager = getHudManager();
            if (hudManager == null || !hudManager.isInitialized()) {
                return;
            }

            JsonObject root = new JsonObject();
            for (HudElement element : hudManager.getElements()) {
                JsonObject elementJson = new JsonObject();
                elementJson.addProperty("x", element.getX());
                elementJson.addProperty("y", element.getY());
                elementJson.addProperty("width", element.getWidth());
                elementJson.addProperty("height", element.getHeight());
                root.add(element.getName(), elementJson);
            }
            
            FilesManager.createFile(
                CONFIG_DIR,
                FilesManager.FileFormat.APR,
                CONFIG_NAME,
                root.toString(),
                FilesManager.CheckMode.ALWAYS
            );
            
            Logger.success("DragConfig saved!");
        } catch (Exception e) {
            Logger.error("DragConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            Path configPath = FilesManager.getFilePath(CONFIG_DIR, CONFIG_NAME, FilesManager.FileFormat.APR);
            
            if (!FilesManager.exists(configPath)) {
                Logger.info("DragConfig: No config found, using defaults.");
                return;
            }

            HudManager hudManager = getHudManager();
            if (hudManager == null) {
                Logger.error("DragConfig: HudManager is null, cannot load.");
                return;
            }

            String json = FilesManager.readFile(configPath);
            if (json == null || json.trim().isEmpty()) {
                Logger.error("DragConfig: Config file is empty.");
                return;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            for (HudElement element : hudManager.getElements()) {
                if (root.has(element.getName())) {
                    JsonObject elementJson = root.getAsJsonObject(element.getName());
                    if (elementJson.has("x")) {
                        element.setX(elementJson.get("x").getAsInt());
                    }
                    if (elementJson.has("y")) {
                        element.setY(elementJson.get("y").getAsInt());
                    }
                    if (elementJson.has("width")) {
                        element.setWidth(elementJson.get("width").getAsInt());
                    }
                    if (elementJson.has("height")) {
                        element.setHeight(elementJson.get("height").getAsInt());
                    }
                }
            }
            
            Logger.success("DragConfig loaded!");
        } catch (Exception e) {
            Logger.error("DragConfig: Load failed! " + e.getMessage());
        }
    }

    private HudManager getHudManager() {
        if (Initialization.getInstance() == null) return null;
        if (Initialization.getInstance().getManager() == null) return null;
        return Initialization.getInstance().getManager().getHudManager();
    }
}
