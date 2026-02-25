package cc.apr.input.impl.bind;

import aporia.cc.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import cc.apr.input.api.KeyboardKeys;
import cc.apr.input.api.bind.Keybind;

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
      } else if (keybind.getId() == null) {
         throw new IllegalArgumentException("Keybind ID cannot be null");
      } else {
         Keybind oldKeybind = this.keybinds.get(keybind.getId());
         if (oldKeybind != null) {
            this.removeFromKeyCodeMap(oldKeybind);
         }

         this.keybinds.put(keybind.getId(), keybind);
         this.keyCodeToKeybinds.computeIfAbsent(keybind.getKeyCode(), k -> new ArrayList<>()).add(keybind);
      }
   }

   public boolean updateKeybind(String id, int newKeyCode) {
      Keybind keybind = this.keybinds.get(id);
      if (keybind == null) {
         return false;
      } else {
         this.removeFromKeyCodeMap(keybind);
         keybind.setKeyCode(newKeyCode);
         this.keyCodeToKeybinds.computeIfAbsent(newKeyCode, k -> new ArrayList<>()).add(keybind);
         this.saveKeybinds();
         return true;
      }
   }

   public void handleKeyPress(int keyCode) {
      List<Keybind> binds = this.keyCodeToKeybinds.get(keyCode);
      if (binds != null) {
         for (Keybind bind : new ArrayList<>(binds)) {
            try {
               bind.execute();
            } catch (Exception var7) {
               Logger.INSTANCE.error("Error executing keybind " + bind.getId() + ": " + var7.getMessage(), var7);
            }
         }
      }
   }

   public Keybind getKeybind(String id) {
      return this.keybinds.get(id);
   }

   public List<Keybind> getKeybindsByKeyCode(int keyCode) {
      List<Keybind> binds = this.keyCodeToKeybinds.get(keyCode);
      return binds != null ? Collections.unmodifiableList(new ArrayList<>(binds)) : Collections.emptyList();
   }

   public Collection<Keybind> getAllKeybinds() {
      return Collections.unmodifiableCollection(this.keybinds.values());
   }

   public boolean unregisterKeybind(String id) {
      Keybind keybind = this.keybinds.remove(id);
      if (keybind != null) {
         this.removeFromKeyCodeMap(keybind);
         return true;
      } else {
         return false;
      }
   }

   public void clear() {
      this.keybinds.clear();
      this.keyCodeToKeybinds.clear();
   }

   public int getKeybindCount() {
      return this.keybinds.size();
   }

   public boolean hasKeybind(String id) {
      return this.keybinds.containsKey(id);
   }

   private void removeFromKeyCodeMap(Keybind keybind) {
      List<Keybind> binds = this.keyCodeToKeybinds.get(keybind.getKeyCode());
      if (binds != null) {
         binds.remove(keybind);
         if (binds.isEmpty()) {
            this.keyCodeToKeybinds.remove(keybind.getKeyCode());
         }
      }
   }

   public void saveKeybinds() {
      try {
         Path configDir = this.getConfigDirectory();
         if (configDir == null) {
            Logger.INSTANCE.warn("Config directory not available (test environment?)");
            return;
         }

         Files.createDirectories(configDir);
         Path keybindsFile = configDir.resolve("keybinds.json");
         JsonObject root = new JsonObject();
         JsonArray keybindsArray = new JsonArray();

         for (Keybind keybind : this.keybinds.values()) {
            JsonObject keybindObj = new JsonObject();
            keybindObj.addProperty("id", keybind.getId());
            keybindObj.addProperty("keyCode", keybind.getKeyCode());
            keybindObj.addProperty("keyName", KeyboardKeys.getKeyName(keybind.getKeyCode()));
            keybindsArray.add(keybindObj);
         }

         root.add("keybinds", keybindsArray);
         String json = GSON.toJson(root);
         Files.writeString(keybindsFile, json);
      } catch (IOException var8) {
         Logger.INSTANCE.error("Failed to save keybinds: " + var8.getMessage(), var8);
      }
   }

   public void loadKeybinds() {
      try {
         Path configDir = this.getConfigDirectory();
         if (configDir == null) {
            Logger.INSTANCE.warn("Config directory not available (test environment?)");
            return;
         }

         Path keybindsFile = configDir.resolve("keybinds.json");
         if (!Files.exists(keybindsFile)) {
            return;
         }

         String json = Files.readString(keybindsFile);
         JsonObject root = (JsonObject)GSON.fromJson(json, JsonObject.class);
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

         for (Entry<String, Integer> entry : loadedKeybinds.entrySet()) {
            String id = entry.getKey();
            int keyCode = entry.getValue();
            if (this.keybinds.containsKey(id)) {
               this.updateKeybindInternal(id, keyCode);
            }
         }
      } catch (IOException var11) {
         Logger.INSTANCE.error("Failed to load keybinds: " + var11.getMessage(), var11);
      } catch (Exception var12) {
         Logger.INSTANCE.error("Corrupted keybinds file, using defaults: " + var12.getMessage(), var12);
      }
   }

   private Path getConfigDirectory() {
      try {
         return Minecraft.getInstance().gameDirectory.toPath().resolve("aporia");
      } catch (Exception var2) {
         return null;
      }
   }

   private boolean updateKeybindInternal(String id, int newKeyCode) {
      Keybind keybind = this.keybinds.get(id);
      if (keybind == null) {
         return false;
      } else {
         this.removeFromKeyCodeMap(keybind);
         keybind.setKeyCode(newKeyCode);
         this.keyCodeToKeybinds.computeIfAbsent(newKeyCode, k -> new ArrayList<>()).add(keybind);
         return true;
      }
   }
}
