package ru.ui.clickgui;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.input.impl.bind.KeybindManager;
import ru.module.Module;
import ru.module.ModuleManager;
import ru.render.BlurRenderer;
import ru.render.MsdfFont;
import ru.render.MsdfTextRenderer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGuiScreen extends Screen {
    private static MsdfFont font;
    private static MsdfTextRenderer textRenderer;
    private static boolean initialized = false;
    private static BlurRenderer blurRenderer;
    
    private Module hoveredModule = null;
    
    private final Map<Module.Category, CategoryPanel> categoryPanels = new HashMap<>();
    private CategoryPanel draggingPanel = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    
    private static final int CATEGORY_WIDTH = 140;
    private static final int CATEGORY_SPACING = 10;
    private static final int MODULE_HEIGHT = 28;
    private static final int MODULE_SPACING = 6;
    private static final int HEADER_HEIGHT = 30;

    public ClickGuiScreen(int width, int height) {
        super(Text.literal("Click GUI"));
        initFont();
        if (blurRenderer == null) {
            blurRenderer = new BlurRenderer();
        }
        initializeCategoryPanels();
        loadPanelPositions();
    }

    private static void initFont() {
        if (initialized) return;
        try {
            font = new MsdfFont(
                "assets/aporia/fonts/Inter_Medium.json",
                "assets/aporia/fonts/Inter_Medium.png"
            );
            textRenderer = new MsdfTextRenderer(font);
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void initializeCategoryPanels() {
        Module.Category[] categories = Module.Category.values();
        int startX = 100;
        int startY = 50;
        int categoryX = startX;
        
        for (Module.Category category : categories) {
            CategoryPanel panel = new CategoryPanel(category, categoryX, startY, CATEGORY_WIDTH, 400);
            categoryPanels.put(category, panel);
            categoryX += CATEGORY_WIDTH + CATEGORY_SPACING;
        }
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        if (!initialized) return;
        
        MinecraftPlugin.getInstance().bindMainFramebuffer(true);
        
        int windowWidth = MinecraftPlugin.getInstance().getMainFramebufferWidth();
        int windowHeight = MinecraftPlugin.getInstance().getMainFramebufferHeight();
        
        float baseScale = (float) this.client.getWindow().getScaleFactor();
        
        int scaledMouseX = (int)(mouseX * baseScale);
        int scaledMouseY = (int)(mouseY * baseScale);
        
        hoveredModule = null;
        
        for (CategoryPanel panel : categoryPanels.values()) {
            renderCategory(panel, scaledMouseX, scaledMouseY, 1.0f);
        }
    }
    
    private void renderCategory(CategoryPanel panel, int mouseX, int mouseY, float scale) {
        int x = panel.getX();
        int y = panel.getY();
        int width = panel.getWidth();
        int height = panel.getHeight();
        
        renderRect(x, y, width, height, 8, RenderColor.of(20, 20, 25, 230));
        
        renderRect(x, y, width, HEADER_HEIGHT, 8, RenderColor.of(30, 30, 38, 255));
        
        if (textRenderer != null) {
            textRenderer.drawText(x + 10, y + 11, 13, 
                panel.getCategory().getDisplayName(), RenderColor.WHITE);
        }
        
        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(panel.getCategory());
        int moduleY = y + HEADER_HEIGHT + 5;
        
        for (Module module : modules) {
            if (moduleY + MODULE_HEIGHT > y + height - 5) {
                break;
            }
            
            renderModule(module, x + 5, moduleY, width - 10, 
                MODULE_HEIGHT, mouseX, mouseY, scale);
            moduleY += MODULE_HEIGHT + MODULE_SPACING;
        }
    }
    
    private void renderModule(Module module, int x, int y, int width, int height, int mouseX, int mouseY, float scale) {
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        boolean isEnabled = module.isEnabled();
        
        if (isHovered) {
            hoveredModule = module;
        }
        
        RenderColor bgColor;
        RenderColor textColor;
        
        if (isEnabled) {
            bgColor = isHovered ? RenderColor.of(70, 130, 255, 255) : RenderColor.of(60, 120, 245, 230);
            textColor = RenderColor.WHITE;
        } else {
            bgColor = isHovered ? RenderColor.of(50, 50, 60, 200) : RenderColor.of(40, 40, 50, 180);
            textColor = RenderColor.of(180, 180, 190, 255);
        }
        
        renderRect(x, y, width, height, 5, bgColor);
        
        if (textRenderer != null) {
            textRenderer.drawText(x + 8, y + 11, 11, module.getName(), textColor);
        }
    }

    private void renderRect(int x, int y, int w, int h, float radius, RenderColor color) {
        new RoundedRectDrawer()
                .rectSized(x, y, w, h, radius, RectColors.oneColor(color))
                .build()
                .tryDraw()
                .close();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC key
            this.close();
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double x, double y, int button) {
        float baseScale = (float) this.client.getWindow().getScaleFactor();
        int scaledMouseX = (int)(x * baseScale);
        int scaledMouseY = (int)(y * baseScale);
        
        boolean shiftPressed = GLFW.glfwGetKey(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                               GLFW.glfwGetKey(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        
        if (button == 0) {
            for (CategoryPanel panel : categoryPanels.values()) {
                if (panel.isHeaderHovered(scaledMouseX, scaledMouseY)) {
                    draggingPanel = panel;
                    dragOffsetX = scaledMouseX - panel.getX();
                    dragOffsetY = scaledMouseY - panel.getY();
                    panel.setDragging(true);
                    return true;
                }
            }
            
            for (CategoryPanel panel : categoryPanels.values()) {
                Module module = panel.getHoveredModule(scaledMouseX, scaledMouseY);
                if (module != null) {
                    if (shiftPressed) {
                        KeybindUI.startBinding(module.getName(), keyCode -> {
                            String keybindId = "module." + module.getName().toLowerCase() + ".toggle";
                            KeybindManager.getInstance().updateKeybind(keybindId, keyCode);
                        });
                    } else {
                        module.toggle();
                    }
                    return true;
                }
            }
        } else if (button == 1) {
            for (CategoryPanel panel : categoryPanels.values()) {
                Module module = panel.getHoveredModule(scaledMouseX, scaledMouseY);
                if (module != null) {
                    openModuleSettings(module);
                    return true;
                }
            }
        }
        
        return false;
    }

    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && draggingPanel != null) {
            draggingPanel.setDragging(false);
            draggingPanel = null;
            savePanelPositions();
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        if (draggingPanel != null && button == 0) {
            float baseScale = (float) this.client.getWindow().getScaleFactor();
            int scaledMouseX = (int)(x * baseScale);
            int scaledMouseY = (int)(y * baseScale);
            
            draggingPanel.setPosition(
                scaledMouseX - dragOffsetX,
                scaledMouseY - dragOffsetY
            );
            return true;
        }
        return false;
    }

    private void openModuleSettings(Module module) {
        // TODO: Implement settings panel
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        // Override to prevent default background
    }

    @Override
    public void close() {
        super.close();
    }

    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    public boolean shouldPause() {
        return false;
    }
    
    private void savePanelPositions() {
        try {
            File configDir = new File("config/aporia");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            
            File configFile = new File(configDir, "gui_positions.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            
            JsonObject root = new JsonObject();
            JsonArray panelsArray = new JsonArray();
            
            for (CategoryPanel panel : categoryPanels.values()) {
                JsonObject panelObj = new JsonObject();
                panelObj.addProperty("category", panel.getCategory().name());
                panelObj.addProperty("x", panel.getX());
                panelObj.addProperty("y", panel.getY());
                panelsArray.add(panelObj);
            }
            
            root.add("panels", panelsArray);
            
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(root, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadPanelPositions() {
        try {
            File configFile = new File("config/aporia/gui_positions.json");
            if (!configFile.exists()) {
                return;
            }
            
            Gson gson = new Gson();
            try (FileReader reader = new FileReader(configFile)) {
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                if (root == null || !root.has("panels")) {
                    return;
                }
                
                JsonArray panelsArray = root.getAsJsonArray("panels");
                for (int i = 0; i < panelsArray.size(); i++) {
                    JsonObject panelObj = panelsArray.get(i).getAsJsonObject();
                    String categoryName = panelObj.get("category").getAsString();
                    int x = panelObj.get("x").getAsInt();
                    int y = panelObj.get("y").getAsInt();
                    
                    try {
                        Module.Category category = Module.Category.valueOf(categoryName);
                        CategoryPanel panel = categoryPanels.get(category);
                        if (panel != null) {
                            panel.setPosition(x, y);
                        }
                    } catch (IllegalArgumentException e) {
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    static class CategoryPanel {
        private final Module.Category category;
        private int x;
        private int y;
        private final int width;
        private int height;
        private boolean dragging;
        
        public CategoryPanel(Module.Category category, int x, int y, int width, int height) {
            this.category = category;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.dragging = false;
        }
        
        public boolean isHeaderHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width &&
                   mouseY >= y && mouseY <= y + HEADER_HEIGHT;
        }
        
        public Module getHoveredModule(int mouseX, int mouseY) {
            if (!isInBounds(mouseX, mouseY)) return null;
            
            List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
            int moduleY = y + HEADER_HEIGHT + 5;
            int moduleX = x + 5;
            int moduleWidth = width - 10;
            
            for (Module module : modules) {
                if (mouseX >= moduleX && mouseX <= moduleX + moduleWidth &&
                    mouseY >= moduleY && mouseY <= moduleY + MODULE_HEIGHT) {
                    return module;
                }
                moduleY += MODULE_HEIGHT + MODULE_SPACING;
            }
            
            return null;
        }
        
        public boolean isInBounds(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width &&
                   mouseY >= y && mouseY <= y + height;
        }
        
        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        public Module.Category getCategory() {
            return category;
        }
        
        public int getX() {
            return x;
        }
        
        public int getY() {
            return y;
        }
        
        public int getWidth() {
            return width;
        }
        
        public int getHeight() {
            return height;
        }
        
        public boolean isDragging() {
            return dragging;
        }
        
        public void setDragging(boolean dragging) {
            this.dragging = dragging;
        }
    }
}
