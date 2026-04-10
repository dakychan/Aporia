/*
 * Copyright (c) 2025-2026 BEVoid Project
 * Distributed under the BEVoid Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.events.impl;

/** Fired when a mouse button is pressed or released. */
public final class MouseClickEvent {

    public enum Action { PRESS, RELEASE }

    private final double x, y;
    private final int button;
    private final Action action;
    private boolean cancelled;

    public MouseClickEvent(double x, double y, int button, Action action) {
        this.x = x; this.y = y; this.button = button; this.action = action;
    }

    public double x()       { return x; }
    public double y()       { return y; }
    public int button()     { return button; }
    public Action action()  { return action; }

    public void cancel()         { cancelled = true; }
    public boolean isCancelled() { return cancelled; }
}
