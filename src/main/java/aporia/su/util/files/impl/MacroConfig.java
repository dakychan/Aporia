package aporia.su.util.files.impl;

import aporia.cc.OsManager;
import aporia.su.util.files.FilesManager;
import aporia.su.util.helper.Logger;
import aporia.su.util.user.repository.macro.Macro;
import aporia.su.util.user.repository.macro.MacroRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Конфиг для макросов.
 */
public class MacroConfig {
    private static MacroConfig instance;
    private static final String CONFIG_NAME = "macros";
    private static final Path CONFIG_DIR = OsManager.mainDirectory.resolve("configs");

    private MacroConfig() {
        load();
    }

    public static MacroConfig getInstance() {
        if (instance == null) {
            instance = new MacroConfig();
        }
        return instance;
    }

    public void save() {
        try {
            JsonArray array = new JsonArray();
            for (Macro macro : MacroRepository.getInstance().getMacroList()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", macro.name());
                obj.addProperty("message", macro.message());
                obj.addProperty("key", macro.key());
                array.add(obj);
            }
            
            FilesManager.createFile(
                CONFIG_DIR,
                FilesManager.FileFormat.APR,
                CONFIG_NAME,
                array.toString(),
                FilesManager.CheckMode.ALWAYS
            );
        } catch (Exception e) {
            Logger.error("MacroConfig: Save failed! " + e.getMessage());
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
            
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            List<Macro> macros = new ArrayList<>();
            array.forEach(element -> {
                JsonObject obj = element.getAsJsonObject();
                String name = obj.get("name").getAsString();
                String message = obj.get("message").getAsString();
                int key = obj.get("key").getAsInt();
                macros.add(new Macro(name, message, key));
            });
            MacroRepository.getInstance().setMacros(macros);
            Logger.success("MacroConfig loaded!");
        } catch (Exception e) {
            Logger.error("MacroConfig: Load failed! " + e.getMessage());
        }
    }
}
