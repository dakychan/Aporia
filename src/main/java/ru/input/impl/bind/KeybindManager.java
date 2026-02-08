package ru.input.impl.bind;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.input.api.bind.Keybind;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton manager for the keybind system.
 * 
 * Responsibilities:
 * - Register keybinds with unique IDs
 * - Update keybinds (rebinding to different keys)
 * - Handle key press events and trigger bound actions
 * - Maintain efficient lookup structures (by ID and by key code)
 * 
 * The manager uses two storage maps:
 * - keybinds: Maps keybind ID to Keybind object for lookup and updates
 * - keyCodeToKeybinds: Maps key code to list of Keybinds for efficient key press handling
 * 
 * Thread-safe for concurrent access from event handlers.
 * 
 * Requirements: 3.3, 3.4, 3.5
 */
@Environment(EnvType.CLIENT)
public class KeybindManager {
    private static KeybindManager instance;
    
    // Map from keybind ID to Keybind object
    private final Map<String, Keybind> keybinds = new ConcurrentHashMap<>();
    
    // Map from key code to list of Keybinds bound to that key
    private final Map<Integer, List<Keybind>> keyCodeToKeybinds = new ConcurrentHashMap<>();
    
    private KeybindManager() {
        // Private constructor for singleton
    }
    
    /**
     * Gets the singleton instance of KeybindManager.
     * 
     * @return The KeybindManager instance
     */
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
    
    /**
     * Registers a new keybind.
     * 
     * If a keybind with the same ID already exists, it will be replaced.
     * The keybind is added to both storage maps for efficient lookup.
     * 
     * @param keybind The keybind to register
     * @throws IllegalArgumentException if keybind or its ID is null
     */
    public void registerKeybind(Keybind keybind) {
        if (keybind == null) {
            throw new IllegalArgumentException("Keybind cannot be null");
        }
        if (keybind.getId() == null) {
            throw new IllegalArgumentException("Keybind ID cannot be null");
        }
        
        // Remove old keybind if it exists
        Keybind oldKeybind = keybinds.get(keybind.getId());
        if (oldKeybind != null) {
            removeFromKeyCodeMap(oldKeybind);
        }
        
        // Add to ID map
        keybinds.put(keybind.getId(), keybind);
        
        // Add to key code map
        keyCodeToKeybinds
            .computeIfAbsent(keybind.getKeyCode(), k -> new ArrayList<>())
            .add(keybind);
    }
    
    /**
     * Updates a keybind to use a new key code (rebinding).
     * 
     * This method:
     * 1. Finds the keybind by ID
     * 2. Removes it from the old key code mapping
     * 3. Updates the keybind's key code
     * 4. Adds it to the new key code mapping
     * 
     * @param id The ID of the keybind to update
     * @param newKeyCode The new key code to bind to
     * @return true if the keybind was found and updated, false otherwise
     */
    public boolean updateKeybind(String id, int newKeyCode) {
        Keybind keybind = keybinds.get(id);
        if (keybind == null) {
            return false;
        }
        
        // Remove from old key code mapping
        removeFromKeyCodeMap(keybind);
        
        // Update key code
        keybind.setKeyCode(newKeyCode);
        
        // Add to new key code mapping
        keyCodeToKeybinds
            .computeIfAbsent(newKeyCode, k -> new ArrayList<>())
            .add(keybind);
        
        return true;
    }
    
    /**
     * Handles a key press event by executing all keybinds bound to that key.
     * 
     * This method is called by KeybindListener when a KeyPressEvent is fired.
     * All keybinds associated with the given key code will have their actions executed.
     * 
     * @param keyCode The GLFW key code that was pressed
     */
    public void handleKeyPress(int keyCode) {
        List<Keybind> binds = keyCodeToKeybinds.get(keyCode);
        if (binds != null) {
            // Create a copy to avoid concurrent modification if actions modify keybinds
            List<Keybind> bindsCopy = new ArrayList<>(binds);
            for (Keybind bind : bindsCopy) {
                try {
                    bind.execute();
                } catch (Exception e) {
                    System.err.println("Error executing keybind " + bind.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Gets a keybind by its ID.
     * 
     * @param id The keybind ID
     * @return The keybind, or null if not found
     */
    public Keybind getKeybind(String id) {
        return keybinds.get(id);
    }
    
    /**
     * Gets all keybinds bound to a specific key code.
     * 
     * @param keyCode The key code
     * @return An unmodifiable list of keybinds, or an empty list if none are bound
     */
    public List<Keybind> getKeybindsByKeyCode(int keyCode) {
        List<Keybind> binds = keyCodeToKeybinds.get(keyCode);
        return binds != null ? Collections.unmodifiableList(new ArrayList<>(binds)) : Collections.emptyList();
    }
    
    /**
     * Gets all registered keybinds.
     * 
     * @return An unmodifiable collection of all keybinds
     */
    public Collection<Keybind> getAllKeybinds() {
        return Collections.unmodifiableCollection(keybinds.values());
    }
    
    /**
     * Unregisters a keybind by its ID.
     * 
     * @param id The ID of the keybind to remove
     * @return true if the keybind was found and removed, false otherwise
     */
    public boolean unregisterKeybind(String id) {
        Keybind keybind = keybinds.remove(id);
        if (keybind != null) {
            removeFromKeyCodeMap(keybind);
            return true;
        }
        return false;
    }
    
    /**
     * Clears all registered keybinds.
     * Useful for testing or reloading keybinds from file.
     */
    public void clear() {
        keybinds.clear();
        keyCodeToKeybinds.clear();
    }
    
    /**
     * Gets the number of registered keybinds.
     * 
     * @return The number of keybinds
     */
    public int getKeybindCount() {
        return keybinds.size();
    }
    
    /**
     * Checks if a keybind with the given ID exists.
     * 
     * @param id The keybind ID
     * @return true if the keybind exists, false otherwise
     */
    public boolean hasKeybind(String id) {
        return keybinds.containsKey(id);
    }
    
    /**
     * Helper method to remove a keybind from the key code map.
     * 
     * @param keybind The keybind to remove
     */
    private void removeFromKeyCodeMap(Keybind keybind) {
        List<Keybind> binds = keyCodeToKeybinds.get(keybind.getKeyCode());
        if (binds != null) {
            binds.remove(keybind);
            // Clean up empty lists
            if (binds.isEmpty()) {
                keyCodeToKeybinds.remove(keybind.getKeyCode());
            }
        }
    }
}
