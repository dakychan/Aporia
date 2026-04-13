/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.settings;

import java.util.function.Supplier;

/** Clickable button setting. */
public class ButtonSetting extends Setting<Void> {

    private final Runnable action;

    public ButtonSetting(String name, String description, Runnable action) {
        this(name, description, action, null);
    }

    public ButtonSetting(String name, String description, Runnable action, Supplier<Boolean> visible) {
        super(name, description, null, visible);
        this.action = action;
    }

    public void click() {
        if (action != null) action.run();
    }
}
