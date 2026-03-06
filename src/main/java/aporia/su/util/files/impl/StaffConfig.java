package aporia.su.util.files.impl;

import aporia.cc.OsManager;
import aporia.su.util.files.FilesManager;
import aporia.su.util.helper.Logger;
import aporia.su.util.user.repository.staff.StaffUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Конфиг для списка стаффа серверов.
 */
public class StaffConfig {
    private static StaffConfig instance;
    private static final String CONFIG_NAME = "staff";
    private static final Path CONFIG_DIR = OsManager.mainDirectory.resolve("configs");

    private StaffConfig() {}

    public static StaffConfig getInstance() {
        if (instance == null) {
            instance = new StaffConfig();
        }
        return instance;
    }

    public void save() {
        try {
            JsonArray array = new JsonArray();
            for (String name : StaffUtils.getStaffNames()) {
                array.add(name);
            }
            
            FilesManager.createFile(
                CONFIG_DIR,
                FilesManager.FileFormat.APR,
                CONFIG_NAME,
                array.toString(),
                FilesManager.CheckMode.ALWAYS
            );
        } catch (Exception e) {
            Logger.error("StaffConfig: Save failed! " + e.getMessage());
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
            List<String> names = new ArrayList<>();
            array.forEach(element -> names.add(element.getAsString()));
            StaffUtils.setStaff(names);
            
            Logger.success("StaffConfig loaded!");
        } catch (Exception e) {
            Logger.error("StaffConfig: Load failed! " + e.getMessage());
        }
    }
}
