/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.settings;

import java.util.function.Supplier;

/**
 * Base class for all module settings.
 */
public abstract class Setting<T> {
    protected final String name;
    protected final String description;
    protected final Supplier<Boolean> visible;
    protected T value;
    
    protected Setting(String name, String description, T defaultValue, Supplier<Boolean> visible) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.visible = visible;
    }
    
    public String name() { return name; }
    public String description() { return description; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }
    public boolean isVisible() { return visible == null || visible.get(); }
}
