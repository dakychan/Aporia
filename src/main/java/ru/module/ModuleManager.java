package ru.module;

import ru.input.api.bind.Keybind;
import ru.input.impl.bind.KeybindManager;
import ru.module.impl.visuals.ClickGui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {
    private static ModuleManager instance;
    private final List<Module> modules;
    private final Map<Module.Category, List<Module>> modulesByCategory;

    private ModuleManager() {
        modules = new ArrayList<>();
        modulesByCategory = new HashMap<>();

        for (Module.Category category : Module.Category.values()) {
            modulesByCategory.put(category, new ArrayList<>());
        }

        registerModules();
        registerModuleKeybinds();
        KeybindManager.getInstance().loadKeybinds();
        loadConfig();
    }
    
    private void loadConfig() {
        try {
            ru.files.FilesManager filesManager = ru.Aporia.getFilesManager();
            if (filesManager != null) {
                Map<String, ru.files.ModuleConfig> configs = filesManager.loadConfig();
                if (configs != null) {
                    applyConfig(configs);
                }
            }
        } catch (Exception e) {
            ru.files.Logger.INSTANCE.error("Failed to load config: " + e.getMessage(), e);
        }
    }
    
    private void applyConfig(Map<String, ru.files.ModuleConfig> configs) {
        for (Module module : modules) {
            ru.files.ModuleConfig config = configs.get(module.getName());
            if (config != null) {
                if (config.getEnabled() != module.isEnabled()) {
                    module.setEnabled(config.getEnabled());
                }
                
                for (Module.Setting<?> setting : module.getSettings()) {
                    String value = config.getSettings().get(setting.getName());
                    if (value != null) {
                        applySetting(setting, value);
                    }
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void applySetting(Module.Setting<?> setting, String value) {
        try {
            if (setting instanceof Module.BooleanSetting) {
                ((Module.Setting<Boolean>) setting).setValue(Boolean.parseBoolean(value));
            } else if (setting instanceof Module.NumberSetting) {
                ((Module.Setting<Double>) setting).setValue(Double.parseDouble(value));
            } else if (setting instanceof Module.StringSetting || setting instanceof Module.ModeSetting) {
                ((Module.Setting<String>) setting).setValue(value);
            } else if (setting instanceof ru.module.impl.visuals.Interface.MultiSetting) {
                String[] items = value.split(",");
                java.util.List<String> list = new java.util.ArrayList<>();
                for (String item : items) {
                    String trimmed = item.trim();
                    if (!trimmed.isEmpty()) {
                        list.add(trimmed);
                    }
                }
                ((ru.module.impl.visuals.Interface.MultiSetting) setting).getValue().clear();
                ((ru.module.impl.visuals.Interface.MultiSetting) setting).getValue().addAll(list);
            }
        } catch (Exception e) {
            ru.files.Logger.INSTANCE.error("Failed to apply setting " + setting.getName() + ": " + e.getMessage(), e);
        }
    }
    
    private void registerModule(Module module) {
        modules.add(module);
        modulesByCategory.get(module.getCategory()).add(module);
    }
    
    private void registerModules() {
        // Visuals
        registerModule(new ClickGui());
        registerModule(new ru.module.impl.visuals.Interface());
        
        // Movement
        registerModule(new ru.module.impl.movement.AutoSprint());
        
        // Misc
        registerModule(new ru.module.impl.misc.AutoFlyMe());
        registerModule(new ru.module.impl.misc.BetterChat());
        registerModule(new ru.module.impl.misc.DiscordRPC());
    }
    
    public static ModuleManager getInstance() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }
    
    public List<Module> getModules() {
        return modules;
    }
    
    public Module getModuleByName(String name) {
        for (Module module : modules) {
            if (module.getName().equals(name)) {
                return module;
            }
        }
        return null;
    }
    
    public List<Module> getModulesByCategory(Module.Category category) {
        return modulesByCategory.get(category);
    }
    
    public void onTick() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }
    
    public void saveConfig() {
        try {
            ru.files.FilesManager filesManager = ru.Aporia.getFilesManager();
            if (filesManager != null) {
                Map<String, ru.files.ModuleConfig> configs = new HashMap<>();
                
                Map<String, ru.files.ModuleConfig> existingConfigs = filesManager.loadConfig();
                if (existingConfigs != null && existingConfigs.containsKey("ClickGui")) {
                    configs.put("ClickGui", existingConfigs.get("ClickGui"));
                }
                
                for (Module module : modules) {
                    if (module.getName().equals("ClickGui")) {
                        continue;
                    }
                    
                    Map<String, String> settings = new HashMap<>();
                    
                    for (Module.Setting<?> setting : module.getSettings()) {
                        String value = getSettingValue(setting);
                        if (value != null) {
                            settings.put(setting.getName(), value);
                        }
                    }
                    
                    configs.put(module.getName(), new ru.files.ModuleConfig(module.isEnabled(), settings));
                }
                
                filesManager.saveConfig(configs);
            }
        } catch (Exception e) {
            ru.files.Logger.INSTANCE.error("Failed to save config: " + e.getMessage(), e);
        }
    }
    
    private String getSettingValue(Module.Setting<?> setting) {
        try {
            if (setting instanceof Module.BooleanSetting) {
                return String.valueOf(((Module.BooleanSetting) setting).getValue());
            } else if (setting instanceof Module.NumberSetting) {
                return String.valueOf(((Module.NumberSetting) setting).getValue());
            } else if (setting instanceof Module.StringSetting) {
                return ((Module.StringSetting) setting).getValue();
            } else if (setting instanceof Module.ModeSetting) {
                return ((Module.ModeSetting) setting).getValue();
            } else if (setting instanceof ru.module.impl.visuals.Interface.MultiSetting) {
                java.util.List<String> values = ((ru.module.impl.visuals.Interface.MultiSetting) setting).getValue();
                return String.join(",", values);
            }
        } catch (Exception e) {
            ru.files.Logger.INSTANCE.error("Failed to get setting value for " + setting.getName() + ": " + e.getMessage(), e);
        }
        return null;
    }
    
    private void registerModuleKeybinds() {
        KeybindManager manager = KeybindManager.getInstance();
        
        for (Module module : modules) {
            String keybindId = "module." + module.getName().toLowerCase() + ".toggle";
            int defaultKey = module.getDefaultBind();
            Keybind keybind = new Keybind(keybindId, defaultKey, module::toggle);
            manager.registerKeybind(keybind);
        }
    }
}
