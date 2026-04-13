/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.events.impl;

/** Fired when the mouse wheel is scrolled. */
public final class MouseScrollEvent {

    private final double x, y;
    private final double deltaX, deltaY;
    private boolean cancelled;

    public MouseScrollEvent(double x, double y, double deltaX, double deltaY) {
        this.x = x; this.y = y; this.deltaX = deltaX; this.deltaY = deltaY;
    }

    public double x()      { return x; }
    public double y()      { return y; }
    public double deltaX() { return deltaX; }
    /** Positive = scroll up, negative = scroll down. */
    public double deltaY() { return deltaY; }

    public void cancel()         { cancelled = true; }
    public boolean isCancelled() { return cancelled; }
}
