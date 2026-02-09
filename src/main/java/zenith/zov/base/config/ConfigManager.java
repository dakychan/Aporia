package zenith.zov.base.config;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.Getter;
import zenith.zov.Zenith;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.client.hud.elements.component.MusicInfoComponent;
import zenith.zov.utility.crypt.CryptUtility;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class ConfigManager {
    public static final File configDirectory = new File(Zenith.DIRECTORY, "configs");
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final String shifr = "config";

    public ConfigManager() {

        configDirectory.mkdirs();
        loadConfig("current_config");
        EventManager.register(this);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                saveConfig("current_config");
            } catch (Exception e) {
            }
        }, 5, 5, TimeUnit.MINUTES);
    }


    public boolean loadConfig(String configName) {

        if (configName == null)
            return false;
        Config config = findConfig(configName);
        if (config == null)
            return false;
        try (BufferedReader reader = new BufferedReader(new FileReader(config.getFile()))) {
            JsonParser parser = new JsonParser();
            String encryptedDataBase64 = reader.readLine();

            byte[] encryptedData = Base64.getDecoder().decode(encryptedDataBase64);
            byte[] decryptedData = CryptUtility.decryptData(encryptedData, shifr);

            String json = new String(decryptedData, StandardCharsets.UTF_8);

            JsonObject object = (JsonObject) parser.parse(json);

            config.load(object);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean saveConfig(String configName) {
        try {
            if (configName == null)
                return false;
            Config config;
            if ((config = findConfig(configName)) == null) {
                config = new Config(configName);
            }

            String contentPrettyPrint = new GsonBuilder().setPrettyPrinting().create().toJson(config.save());

            contentPrettyPrint = Base64.getEncoder().encodeToString(CryptUtility.encryptData(contentPrettyPrint.getBytes(), shifr));
            try {
                FileWriter writer = new FileWriter(config.getFile());
                writer.write(contentPrettyPrint);
                writer.close();
                return true;
            } catch (IOException e) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public Config findConfig(String configName) {
        if (configName == null) return null;


        if (new File(configDirectory, configName + "." + Zenith.NAME.toLowerCase()).exists())
            return new Config(configName);

        return null;
    }

    public List<String> configNames() {
        File[] files = configDirectory.listFiles();
        List<String> names = new ArrayList<>();
        for (File file : files) {
            names.add(file.getName());
        }
        return names;
    }

    public boolean deleteConfig(String configName) {
        if (configName == null)
            return false;
        Config config;
        if ((config = findConfig(configName)) != null) {
            final File f = config.getFile();

            return f.exists() && f.delete();
        }
        return false;
    }

    public void save() {
        this.scheduler.shutdown();
        saveConfig("current_config");

    }
}
