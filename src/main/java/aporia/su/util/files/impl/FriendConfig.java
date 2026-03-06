package aporia.su.util.files.impl;

import aporia.cc.OsManager;
import aporia.su.util.files.FilesManager;
import aporia.su.util.helper.Logger;
import aporia.su.util.user.repository.friend.FriendUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Конфиг для списка друзей.
 */
public class FriendConfig {
    private static FriendConfig instance;
    private static final String CONFIG_NAME = "friends";
    private static final Path CONFIG_DIR = OsManager.mainDirectory.resolve("configs");

    private FriendConfig() {}

    public static FriendConfig getInstance() {
        if (instance == null) {
            instance = new FriendConfig();
        }
        return instance;
    }

    public void save() {
        try {
            JsonArray array = new JsonArray();
            for (String name : FriendUtils.getFriendNames()) {
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
            Logger.error("FriendConfig: Save failed! " + e.getMessage());
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
            FriendUtils.setFriends(names);
            
            Logger.success("FriendConfig loaded!");
        } catch (Exception e) {
            Logger.error("FriendConfig: Load failed! " + e.getMessage());
        }
    }
}
