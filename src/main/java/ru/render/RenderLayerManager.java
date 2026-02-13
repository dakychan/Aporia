package ru.render;

import ru.ui.hud.HudComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RenderLayerManager {
    private final List<HudComponent> components;
    private int nextZIndex;

    public RenderLayerManager() {
        this.components = new ArrayList<>();
        this.nextZIndex = 0;
    }

    public void bringToFront(HudComponent component) {
        if (components.contains(component)) {
            nextZIndex++;
            component.setZIndex(nextZIndex);
        }
    }

    public void addComponent(HudComponent component) {
        if (!components.contains(component)) {
            component.setZIndex(nextZIndex++);
            components.add(component);
        }
    }

    public void removeComponent(HudComponent component) {
        components.remove(component);
    }

    public List<HudComponent> getComponentsInRenderOrder() {
        List<HudComponent> sorted = new ArrayList<>(components);
        sorted.sort(Comparator.comparingInt(HudComponent::getZIndex));
        return sorted;
    }

    public List<HudComponent> getComponents() {
        return new ArrayList<>(components);
    }

    public void clear() {
        components.clear();
        nextZIndex = 0;
    }
}
