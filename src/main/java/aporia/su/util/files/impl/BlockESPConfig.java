package aporia.su.util.files.impl;

import aporia.cc.OsManager;
import aporia.su.util.files.FilesManager;
import aporia.su.util.helper.Logger;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Конфиг для BlockESP модуля.
 */
public class BlockESPConfig {
    private static BlockESPConfig instance;
    private static final String CONFIG_NAME = "blockesp";
    private static final Path CONFIG_DIR = OsManager.mainDirectory.resolve("configs");
    
    private final Set<String> blocks = new CopyOnWriteArraySet<>();

    private BlockESPConfig() {}

    public static BlockESPConfig getInstance() {
        if (instance == null) {
            instance = new BlockESPConfig();
        }
        return instance;
    }

    public Set<String> getBlocks() {
        return blocks;
    }

    public void addBlock(String block) {
        blocks.add(block);
    }

    public void addBlockAndSave(String block) {
        addBlock(block);
        save();
    }

    public boolean removeBlock(String block) {
        return blocks.remove(block);
    }

    public boolean removeBlockAndSave(String block) {
        boolean removed = removeBlock(block);
        if (removed) {
            save();
        }
        return removed;
    }

    public boolean hasBlock(String block) {
        return blocks.contains(block);
    }

    public void clear() {
        blocks.clear();
    }

    public void clearAndSave() {
        clear();
        save();
    }

    public int size() {
        return blocks.size();
    }

    public List<String> getBlockList() {
        return new ArrayList<>(blocks);
    }

    public void save() {
        try {
            JsonArray array = new JsonArray();
            for (String block : blocks) {
                array.add(block);
            }
            
            FilesManager.createFile(
                CONFIG_DIR,
                FilesManager.FileFormat.APR,
                CONFIG_NAME,
                array.toString(),
                FilesManager.CheckMode.ALWAYS
            );
        } catch (Exception e) {
            Logger.error("BlockESPConfig: Save failed! " + e.getMessage());
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
            blocks.clear();
            array.forEach(element -> blocks.add(element.getAsString()));
            
            Logger.success("BlockESPConfig loaded!");
        } catch (Exception e) {
            Logger.error("BlockESPConfig: Load failed! " + e.getMessage());
        }
    }
}
