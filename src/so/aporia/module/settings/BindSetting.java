/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.settings;

import java.util.function.Supplier;

/**
 * Keybind setting - binds a key to an action.
 */
public class BindSetting extends Setting<Integer> {
    
    public BindSetting(String name, String description) {
        this(name, description, -1, null);
    }
    
    public BindSetting(String name, String description, int defaultKey) {
        this(name, description, defaultKey, null);
    }
    
    public BindSetting(String name, String description, int defaultKey, Supplier<Boolean> visible) {
        super(name, description, defaultKey, visible);
    }
    
    public int getKey() { return value; }
    public void setKey(int key) { this.value = key; }
    public boolean isBound() { return value != -1; }
}
