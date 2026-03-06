package aporia.su.util.files.impl;

import aporia.cc.OsManager;
import aporia.su.util.files.FilesManager;
import aporia.su.util.helper.Logger;
import aporia.su.util.user.repository.way.Way;
import aporia.su.util.user.repository.way.WayRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Конфиг для вейпоинтов.
 */
public class WayConfig {
    private static WayConfig instance;
    private static final String CONFIG_NAME = "waypoints";
    private static final Path CONFIG_DIR = OsManager.mainDirectory.resolve("configs");

    private WayConfig() {}

    public static WayConfig getInstance() {
        if (instance == null) {
            instance = new WayConfig();
        }
        return instance;
    }

    public void save() {
        try {
            JsonArray array = new JsonArray();
            for (Way way : WayRepository.getInstance().getWayList()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", way.name());
                obj.addProperty("x", way.pos().getX());
                obj.addProperty("y", way.pos().getY());
                obj.addProperty("z", way.pos().getZ());
                obj.addProperty("server", way.server());
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
            Logger.error("WayConfig: Save failed! " + e.getMessage());
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
            List<Way> ways = new ArrayList<>();
            array.forEach(element -> {
                JsonObject obj = element.getAsJsonObject();
                String name = obj.get("name").getAsString();
                int x = obj.get("x").getAsInt();
                int y = obj.get("y").getAsInt();
                int z = obj.get("z").getAsInt();
                String server = obj.get("server").getAsString();
                ways.add(new Way(name, new BlockPos(x, y, z), server));
            });
            WayRepository.getInstance().setWays(ways);
            
            Logger.success("WayConfig loaded!");
        } catch (Exception e) {
            Logger.error("WayConfig: Load failed! " + e.getMessage());
        }
    }
}
