package ru.input.impl.bind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import ru.input.api.KeyboardKeys;
import ru.input.api.bind.Keybind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KeybindManager {
    private static KeybindManager instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "aporia";
    private static final String KEYBINDS_FILE = "keybinds.json";
    
    private final Map<String, Keybind> keybinds = new ConcurrentHashMap<>();
    
    private final Map<Integer, List<Keybind>> keyCodeToKeybinds = new ConcurrentHashMap<>();
    
    private KeybindManager() {
    }

    public static KeybindManager getInstance() {
        if (instance == null) {
            synchronized (KeybindManager.class) {
                if (instance == null) {
                    instance = new KeybindManager();
                }
            }
        }
        return instance;
    }

    public void registerKeybind(Keybind keybind) {
        if (keybind == null) {
            throw new IllegalArgumentException("Keybind cannot be null");
        }
        if (keybind.getId() == null) {
            throw new IllegalArgumentException("Keybind ID cannot be null");
        }
        Keybind oldKeybind = keybinds.get(keybind.getId());
        if (oldKeybind != null) {
            removeFromKeyCodeMap(oldKeybind);
        }
        keybinds.put(keybind.getId(), keybind);
        keyCodeToKeybinds
            .computeIfAbsent(keybind.getKeyCode(), k -> new ArrayList<>())
            .add(keybind);
    }

    public boolean updateKeybind(String id, int newKeyCode) {
        Keybind keybind = keybinds.get(id);
        if (keybind == null) {
            return false;
        }
        
        removeFromKeyCodeMap(keybind);
        
        keybind.setKeyCode(newKeyCode);
        
        keyCodeToKeybinds
            .computeIfAbsent(newKeyCode, k -> new ArrayList<>())
            .add(keybind);
        
        saveKeybinds();
        
        return true;
    }

    public void handleKeyPress(int keyCode) {
        List<Keybind> binds = keyCodeToKeybinds.get(keyCode);
        if (binds != null) {
            List<Keybind> bindsCopy = new ArrayList<>(binds);
            for (Keybind bind : bindsCopy) {
                try {
                    bind.execute();
                } catch (Exception e) {
                    ru.files.Logger.INSTANCE.error("Error executing keybind " + bind.getId() + ": " + e.getMessage(), e);
                }
            }
        }
    }

    public Keybind getKeybind(String id) {
        return keybinds.get(id);
    }

    public List<Keybind> getKeybindsByKeyCode(int keyCode) {
        List<Keybind> binds = keyCodeToKeybinds.get(keyCode);
        return binds != null ? Collections.unmodifiableList(new ArrayList<>(binds)) : Collections.emptyList();
    }

    public Collection<Keybind> getAllKeybinds() {
        return Collections.unmodifiableCollection(keybinds.values());
    }

    public boolean unregisterKeybind(String id) {
        Keybind keybind = keybinds.remove(id);
        if (keybind != null) {
            removeFromKeyCodeMap(keybind);
            return true;
        }
        return false;
    }

    public void clear() {
        keybinds.clear();
        keyCodeToKeybinds.clear();
    }

    public int getKeybindCount() {
        return keybinds.size();
    }

    public boolean hasKeybind(String id) {
        return keybinds.containsKey(id);
    }

    private void removeFromKeyCodeMap(Keybind keybind) {
        List<Keybind> binds = keyCodeToKeybinds.get(keybind.getKeyCode());
        if (binds != null) {
            binds.remove(keybind);
            if (binds.isEmpty()) {
                keyCodeToKeybinds.remove(keybind.getKeyCode());
            }
        }
    }
    
    public void saveKeybinds() {
        try {
            Path configDir = getConfigDirectory();
            if (configDir == null) {
                ru.files.Logger.INSTANCE.warn("Config directory not available (test environment?)");
                return;
            }
            
            Files.createDirectories(configDir);
            
            Path keybindsFile = configDir.resolve(KEYBINDS_FILE);
            
            JsonObject root = new JsonObject();
            JsonArray keybindsArray = new JsonArray();
            
            for (Keybind keybind : keybinds.values()) {
                JsonObject keybindObj = new JsonObject();
                keybindObj.addProperty("id", keybind.getId());
                keybindObj.addProperty("keyCode", keybind.getKeyCode());
                keybindObj.addProperty("keyName", KeyboardKeys.getKeyName(keybind.getKeyCode()));
                keybindsArray.add(keybindObj);
            }
            
            root.add("keybinds", keybindsArray);
            
            String json = GSON.toJson(root);
            Files.writeString(keybindsFile, json);
        } catch (IOException e) {
            ru.files.Logger.INSTANCE.error("Failed to save keybinds: " + e.getMessage(), e);
        }
    }
    
    public void loadKeybinds() {
        try {
            Path configDir = getConfigDirectory();
            if (configDir == null) {
                ru.files.Logger.INSTANCE.warn("Config directory not available (test environment?)");
                return;
            }
            
            Path keybindsFile = configDir.resolve(KEYBINDS_FILE);
            
            if (!Files.exists(keybindsFile)) {
                return;
            }
            
            String json = Files.readString(keybindsFile);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            
            if (root == null || !root.has("keybinds")) {
                return;
            }
            
            JsonArray keybindsArray = root.getAsJsonArray("keybinds");
            Map<String, Integer> loadedKeybinds = new HashMap<>();
            
            for (int i = 0; i < keybindsArray.size(); i++) {
                JsonObject keybindObj = keybindsArray.get(i).getAsJsonObject();
                String id = keybindObj.get("id").getAsString();
                int keyCode = keybindObj.get("keyCode").getAsInt();
                loadedKeybinds.put(id, keyCode);
            }
            
            for (Map.Entry<String, Integer> entry : loadedKeybinds.entrySet()) {
                String id = entry.getKey();
                int keyCode = entry.getValue();
                
                if (keybinds.containsKey(id)) {
                    updateKeybindInternal(id, keyCode);
                }
            }
        } catch (IOException e) {
            ru.files.Logger.INSTANCE.error("Failed to load keybinds: " + e.getMessage(), e);
        } catch (Exception e) {
            ru.files.Logger.INSTANCE.error("Corrupted keybinds file, using defaults: " + e.getMessage(), e);
        }
    }
    
    private Path getConfigDirectory() {
        try {
            return net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve(CONFIG_DIR);
        } catch (Exception e) {
            return null;
        }
    }
    
    private boolean updateKeybindInternal(String id, int newKeyCode) {
        Keybind keybind = keybinds.get(id);
        if (keybind == null) {
            return false;
        }
        
        removeFromKeyCodeMap(keybind);
        
        keybind.setKeyCode(newKeyCode);
        
        keyCodeToKeybinds
            .computeIfAbsent(newKeyCode, k -> new ArrayList<>())
            .add(keybind);
        
        return true;
    }
}
