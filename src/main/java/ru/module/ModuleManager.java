package ru.module;

import ru.input.api.bind.Keybind;
import ru.input.impl.bind.KeybindManager;
import ru.module.impl.visuals.ClickGui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all modules in the client.
 */
public class ModuleManager {
    private static ModuleManager instance;
    private final List<Module> modules;
    private final Map<Module.Category, List<Module>> modulesByCategory;
    
    private ModuleManager() {
        modules = new ArrayList<>();
        modulesByCategory = new HashMap<>();
        
        // Initialize categories
        for (Module.Category category : Module.Category.values()) {
            modulesByCategory.put(category, new ArrayList<>());
        }
        
        // Register modules
        registerModules();
        
        // Register keybinds for all modules
        registerModuleKeybinds();
        
        // Load saved keybinds
        KeybindManager.getInstance().loadKeybinds();
    }
    
    private void registerModule(Module module) {
        modules.add(module);
        modulesByCategory.get(module.getCategory()).add(module);
    }
    
    private void registerModules() {
        // Visuals
        registerModule(new ClickGui());
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
