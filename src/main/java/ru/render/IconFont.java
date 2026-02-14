package ru.render;

import ru.module.Module;

import java.util.HashMap;
import java.util.Map;

public class IconFont {
    private static MsdfFont iconFont;
    private static MsdfTextRenderer iconRenderer;
    private static boolean initialized = false;
    
    private static final Map<Module.Category, String> CATEGORY_ICONS = new HashMap<>();
    
    static {
        CATEGORY_ICONS.put(Module.Category.COMBAT, "0");
        CATEGORY_ICONS.put(Module.Category.MOVEMENT, "C");
        CATEGORY_ICONS.put(Module.Category.VISUALS, "B");
        CATEGORY_ICONS.put(Module.Category.PLAYER, "2");
        CATEGORY_ICONS.put(Module.Category.MISC, "G");
    }
    
    public static void init() {
        if (initialized) return;
        try {
            iconFont = new MsdfFont(
                "assets/aporia/fonts/icons.json",
                "assets/aporia/fonts/icons.png"
            );
            iconRenderer = new MsdfTextRenderer(iconFont);
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static MsdfTextRenderer getRenderer() {
        if (!initialized) {
            init();
        }
        return iconRenderer;
    }
    
    public static String getIcon(Module.Category category) {
        return CATEGORY_ICONS.getOrDefault(category, "?");
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
