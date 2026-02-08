package ru.ui.notify;

import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;

public enum NotificationType {
    INFO(RenderColor.of(60, 120, 245, 230)),
    MODULE(RenderColor.of(80, 200, 120, 230)),
    ERROR(RenderColor.of(245, 80, 80, 230));

    private final RenderColor color;

    NotificationType(RenderColor color) {
        this.color = color;
    }

    public RenderColor getColor() {
        return color;
    }
}
