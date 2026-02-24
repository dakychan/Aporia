package ru.module;

import aporia.cc.Logger;
import aporia.cc.files.ModuleConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.files.FilesManager;
import ru.input.api.bind.Keybind;
import ru.input.impl.bind.KeybindManager;
import ru.module.impl.misc.AutoFlyMe;
import ru.module.impl.misc.BetterChat;
import ru.module.impl.misc.DiscordRPC;
import ru.module.impl.movement.AutoSprint;
import ru.module.impl.visuals.BlurTest;
import ru.module.impl.visuals.ClickGui;
import ru.module.impl.visuals.Interface;

public class ModuleManager {
   private static ModuleManager instance;
   private final List<Module> modules = new ArrayList<>();
   private final Map<Module.Category, List<Module>> modulesByCategory = new HashMap<>();

   private ModuleManager() {
      for (Module.Category category : Module.Category.values()) {
         this.modulesByCategory.put(category, new ArrayList<>());
      }

      this.registerModules();
      this.registerModuleKeybinds();
      KeybindManager.getInstance().loadKeybinds();
      this.loadConfig();
   }

   private void loadConfig() {
      try {
         FilesManager filesManager = ru.Aporia.getFilesManager();
         if (filesManager != null) {
            Map<String, ModuleConfig> configs = filesManager.loadConfig();
            if (configs != null) {
               this.applyConfig(configs);
            }
         }
      } catch (Exception var3) {
         Logger.INSTANCE.error("Failed to load config: " + var3.getMessage(), var3);
      }
   }

   private void applyConfig(Map<String, ModuleConfig> configs) {
      for (Module module : this.modules) {
         ModuleConfig config = configs.get(module.getName());
         if (config != null) {
            if (config.getEnabled() != module.isEnabled()) {
               module.setEnabled(config.getEnabled());
            }

            for (Module.Setting<?> setting : module.getSettings()) {
               String value = (String)config.getSettings().get(setting.getName());
               if (value != null) {
                  this.applySetting(setting, value);
               }
            }
         }
      }
   }

   private void applySetting(Module.Setting<?> setting, String value) {
      try {
         if (setting instanceof Module.BooleanSetting) {
            ((Module.Setting<Boolean>)setting).setValue(Boolean.parseBoolean(value));
         } else if (setting instanceof Module.NumberSetting) {
            ((Module.Setting<Double>)setting).setValue(Double.parseDouble(value));
         } else if (setting instanceof Module.StringSetting || setting instanceof Module.ModeSetting) {
            ((Module.Setting<String>)setting).setValue(value);
         } else if (setting instanceof Interface.MultiSetting) {
            String[] items = value.split(",");
            List<String> list = new ArrayList<>();

            for (String item : items) {
               String trimmed = item.trim();
               if (!trimmed.isEmpty()) {
                  list.add(trimmed);
               }
            }

            ((Interface.MultiSetting)setting).getValue().clear();
            ((Interface.MultiSetting)setting).getValue().addAll(list);
         }
      } catch (Exception var10) {
         Logger.INSTANCE.error("Failed to apply setting " + setting.getName() + ": " + var10.getMessage(), var10);
      }
   }

   private void registerModule(Module module) {
      this.modules.add(module);
      this.modulesByCategory.get(module.getCategory()).add(module);
   }

   private void registerModules() {
      this.registerModule(new ClickGui());
      this.registerModule(new Interface());
      this.registerModule(new BlurTest());
      this.registerModule(new AutoSprint());
      this.registerModule(new AutoFlyMe());
      this.registerModule(new BetterChat());
      this.registerModule(new DiscordRPC());
   }

   public static ModuleManager getInstance() {
      if (instance == null) {
         instance = new ModuleManager();
      }

      return instance;
   }

   public List<Module> getModules() {
      return this.modules;
   }

   public Module getModuleByName(String name) {
      for (Module module : this.modules) {
         if (module.getName().equals(name)) {
            return module;
         }
      }

      return null;
   }

   public List<Module> getModulesByCategory(Module.Category category) {
      return this.modulesByCategory.get(category);
   }

   public void onTick() {
      for (Module module : this.modules) {
         if (module.isEnabled()) {
            module.onTick();
         }
      }
   }

   public void saveConfig() {
      try {
         FilesManager filesManager = ru.Aporia.getFilesManager();
         if (filesManager != null) {
            Map<String, ModuleConfig> configs = new HashMap<>();
            Map<String, ModuleConfig> existingConfigs = filesManager.loadConfig();
            if (existingConfigs != null && existingConfigs.containsKey("ClickGui")) {
               configs.put("ClickGui", existingConfigs.get("ClickGui"));
            }

            for (Module module : this.modules) {
               if (!module.getName().equals("ClickGui")) {
                  Map<String, String> settings = new HashMap<>();

                  for (Module.Setting<?> setting : module.getSettings()) {
                     String value = this.getSettingValue(setting);
                     if (value != null) {
                        settings.put(setting.getName(), value);
                     }
                  }

                  configs.put(module.getName(), new ModuleConfig(module.isEnabled(), settings));
               }
            }

            filesManager.saveConfig(configs);
         }
      } catch (Exception var10) {
         Logger.INSTANCE.error("Failed to save config: " + var10.getMessage(), var10);
      }
   }

   private String getSettingValue(Module.Setting<?> setting) {
      try {
         if (setting instanceof Module.BooleanSetting) {
            return String.valueOf(((Module.BooleanSetting)setting).getValue());
         }

         if (setting instanceof Module.NumberSetting) {
            return String.valueOf(((Module.NumberSetting)setting).getValue());
         }

         if (setting instanceof Module.StringSetting) {
            return ((Module.StringSetting)setting).getValue();
         }

         if (setting instanceof Module.ModeSetting) {
            return ((Module.ModeSetting)setting).getValue();
         }

         if (setting instanceof Interface.MultiSetting) {
            List<String> values = ((Interface.MultiSetting)setting).getValue();
            return String.join(",", values);
         }
      } catch (Exception var3) {
         Logger.INSTANCE.error("Failed to get setting value for " + setting.getName() + ": " + var3.getMessage(), var3);
      }

      return null;
   }

   private void registerModuleKeybinds() {
      KeybindManager manager = KeybindManager.getInstance();

      for (Module module : this.modules) {
         String keybindId = "module." + module.getName().toLowerCase() + ".toggle";
         int defaultKey = module.getDefaultBind();
         Keybind keybind = new Keybind(keybindId, defaultKey, module::toggle);
         manager.registerKeybind(keybind);
      }
   }
}
