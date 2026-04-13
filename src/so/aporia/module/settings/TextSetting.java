/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.settings;

import java.util.function.Supplier;

/** Text input setting. */
public class TextSetting extends Setting<String> {

    private boolean focused = false;

    public TextSetting(String name, String description, String defaultValue) {
        this(name, description, defaultValue, null);
    }

    public TextSetting(String name, String description, String defaultValue, Supplier<Boolean> visible) {
        super(name, description, defaultValue, visible);
    }

    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) { this.focused = focused; }

    public void appendChar(char c) { value += c; }
    public void backspace() {
        if (!value.isEmpty()) value = value.substring(0, value.length() - 1);
    }
}
