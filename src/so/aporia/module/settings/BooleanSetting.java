/*
 * Copyright (c) 2025-2026 BEVoid Project
 * Distributed under the BEVoid Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.settings;

import java.util.function.Supplier;

/**
 * Boolean (checkbox) setting.
 */
public class BooleanSetting extends Setting<Boolean> {
    
    public BooleanSetting(String name, String description, boolean defaultValue) {
        this(name, description, defaultValue, null);
    }
    
    public BooleanSetting(String name, String description, boolean defaultValue, Supplier<Boolean> visible) {
        super(name, description, defaultValue, visible);
    }
    
    public void toggle() { value = !value; }
    public boolean isEnabled() { return value; }
}
