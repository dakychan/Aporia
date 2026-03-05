package aporia.su.util.config.impl.player.friend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import aporia.su.util.config.impl.loggers.consolelogger.Logger;
import aporia.su.util.user.repository.friend.FriendUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FriendConfig {
    private static FriendConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;

    private FriendConfig() {
        Path configDir = Paths.get("Aporia", "configs");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        configPath = configDir.resolve("friends.json");
    }

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
            Files.writeString(configPath, gson.toJson(array), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("FriendConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            if (!Files.exists(configPath)) {
                return;
            }
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            List<String> names = new ArrayList<>();
            array.forEach(element -> names.add(element.getAsString()));
            FriendUtils.setFriends(names);
            Logger.success("FriendConfig: friends.json loaded successfully!");
        } catch (Exception e) {
            Logger.error("FriendConfig: Load failed! " + e.getMessage());
        }
    }
}