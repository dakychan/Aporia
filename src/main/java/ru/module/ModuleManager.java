package ru.module;

import ru.module.impl.TestChat;

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
        registerModule(new TestChat());
        
        // Add more dummy modules for testing GUI
        registerDummyModules();
    }
    
    private void registerModule(Module module) {
        modules.add(module);
        modulesByCategory.get(module.getCategory()).add(module);
    }
    
    private void registerDummyModules() {
        // Combat
        registerModule(new DummyModule("AntiBot", Module.Category.COMBAT));
        registerModule(new DummyModule("Aura", Module.Category.COMBAT));
        registerModule(new DummyModule("AutoCrystal", Module.Category.COMBAT));
        registerModule(new DummyModule("Criticals", Module.Category.COMBAT));
        
        // Movement
        registerModule(new DummyModule("Abilities Fly", Module.Category.MOVEMENT));
        registerModule(new DummyModule("Air Jump", Module.Category.MOVEMENT));
        registerModule(new DummyModule("Blink", Module.Category.MOVEMENT));
        registerModule(new DummyModule("Elytra Fly", Module.Category.MOVEMENT));
        
        // Visuals
        registerModule(new DummyModule("ClickGui", Module.Category.VISUALS));
        registerModule(new DummyModule("Cross Hair", Module.Category.VISUALS));
        registerModule(new DummyModule("Entity ESP", Module.Category.VISUALS));
        registerModule(new DummyModule("No Render", Module.Category.VISUALS));
        
        // Player
        registerModule(new DummyModule("Auto Armor", Module.Category.PLAYER));
        registerModule(new DummyModule("Auto Tool", Module.Category.PLAYER));
        registerModule(new DummyModule("FastBreak", Module.Category.PLAYER));
        registerModule(new DummyModule("No Delay", Module.Category.PLAYER));
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
    
    // Dummy module for testing
    private static class DummyModule extends Module {
        public DummyModule(String name, Category category) {
            super(name, category);
        }
        
        @Override
        public void onEnable() {}
        
        @Override
        public void onDisable() {}
        
        @Override
        public void onTick() {}
    }
}
