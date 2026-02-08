package ru.event.impl;

import ru.event.api.Event;

/**
 * Событие клика мыши
 */
public class MouseClickEvent extends Event {
    public final double x;
    public final double y;
    public final int button;

    public MouseClickEvent(double x, double y, int button) {
        this.x = x;
        this.y = y;
        this.button = button;
    }
}
