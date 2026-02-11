package ru.ui.clickgui;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
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
import ru.render.RectRenderer;
import ru.util.Lang;

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
    private Module expandedModule = null;
    private final Map<Module, Float> settingsAnimations = new HashMap<>();
    
    private final Map<Module.Category, CategoryPanel> categoryPanels = new HashMap<>();
    private CategoryPanel draggingPanel = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    
    private static final int CATEGORY_WIDTH = 175;
    private static final int CATEGORY_SPACING = 15;
    private static final int MODULE_HEIGHT = 32;
    private static final int MODULE_SPACING = 8;
    private static final int HEADER_HEIGHT = 38;
    private static final int SETTINGS_HEIGHT = 100;
    private static final float ANIMATION_SPEED = 0.15f;

    public ClickGuiScreen(int width, int height) {
        super(Text.literal("Click GUI"));
        initFont();
        Lang.load(); // Загружаем переводы
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
            CategoryPanel panel = new CategoryPanel(category, categoryX, startY, CATEGORY_WIDTH, 385);
            categoryPanels.put(category, panel);
            categoryX += CATEGORY_WIDTH + CATEGORY_SPACING;
        }
    }
    
    private boolean panelsPositioned = false;
    
    private void centerPanels(int fbWidth, int fbHeight) {
        if (panelsPositioned) return;
        
        Module.Category[] categories = Module.Category.values();
        int totalWidth = (CATEGORY_WIDTH * categories.length) + (CATEGORY_SPACING * (categories.length - 1));
        int panelHeight = 385;

        int startX = (fbWidth - totalWidth) / 2;
        int startY = (fbHeight - panelHeight) / 2;
        int categoryX = startX;
        
        for (Module.Category category : categories) {
            CategoryPanel panel = categoryPanels.get(category);
            if (panel != null) {
                panel.setPosition(categoryX, startY);
                categoryX += CATEGORY_WIDTH + CATEGORY_SPACING;
            }
        }
        
        panelsPositioned = true;
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        if (!initialized) return;

        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        plugin.bindMainFramebuffer(true);

        int fbWidth = plugin.getMainFramebufferWidth();
        int fbHeight = plugin.getMainFramebufferHeight();

        double scale = (double) fbWidth / this.width;
        int fbMouseX = (int)(mouseX * scale);
        int fbMouseY = (int)(mouseY * scale);

        centerPanels(fbWidth, fbHeight);
        
        hoveredModule = null;

        updateSettingsAnimations();

        for (CategoryPanel panel : categoryPanels.values()) {
            renderCategory(panel, fbMouseX, fbMouseY);
        }
        
        // Рендерим тултип с описанием модуля над панелями
        if (hoveredModule != null) {
            renderModuleTooltip(hoveredModule, fbMouseX, fbMouseY);
        }
    }
    
    private void renderCategory(CategoryPanel panel, int mouseX, int mouseY) {
        int x = panel.getX();
        int y = panel.getY();
        int width = panel.getWidth();
        int height = panel.getHeight();

        renderRect(x, y, width, height, 8, RenderColor.of(20, 20, 25, 230));

        renderRect(x, y, width, HEADER_HEIGHT, 8, RenderColor.of(30, 30, 38, 255));

        if (textRenderer != null) {
            textRenderer.drawText(x + 10, y + 21, 16, 
                panel.getCategory().getDisplayName(), RenderColor.WHITE);
        }

        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(panel.getCategory());
        int moduleY = y + HEADER_HEIGHT + 5;
        
        for (Module module : modules) {
            if (moduleY + MODULE_HEIGHT > y + height - 5) {
                break;
            }

            float settingsProgress = settingsAnimations.getOrDefault(module, 0f);
            int settingsHeight = (int)(SETTINGS_HEIGHT * settingsProgress);
            
            renderModule(module, x + 5, moduleY, width - 10, 
                MODULE_HEIGHT, mouseX, mouseY);

            if (settingsProgress > 0.01f) {
                renderModuleSettings(module, x + 5, moduleY + MODULE_HEIGHT, 
                    width - 10, settingsHeight, settingsProgress);
            }
            
            moduleY += MODULE_HEIGHT + MODULE_SPACING + settingsHeight;
        }
    }
    
    private void renderModule(Module module, int x, int y, int width, int height, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        boolean isEnabled = module.isEnabled();
        boolean isExpanded = module == expandedModule;
        
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

        float radius = isExpanded && settingsAnimations.getOrDefault(module, 0f) > 0.01f ? 5 : 5;
        renderRect(x, y, width, height, radius, bgColor);

        if (textRenderer != null) {
            textRenderer.drawText(x + 8, y + 20, 15, module.getName(), textColor);
        }

        if (textRenderer != null && isExpanded) {
            String arrow = settingsAnimations.getOrDefault(module, 0f) > 0.5f ? "▼" : "▶";
            float arrowWidth = textRenderer.measureWidth(arrow, 12);
            textRenderer.drawText(x + width - arrowWidth - 8, y + 19, 12, arrow, textColor);
        }
    }
    
    private void renderModuleSettings(Module module, int x, int y, int width, int height, float alpha) {
        if (height <= 0) return;

        int alphaValue = (int)(200 * alpha);
        RenderColor bgColor = RenderColor.of(35, 35, 45, alphaValue);
        renderRect(x, y, width, height, 5, bgColor);

        if (textRenderer != null && alpha > 0.3f) {
            textRenderer.drawText(x + 10, y + 15, 13, 
                "Настройки модуля", RenderColor.of(150, 150, 160, (int)(255 * alpha)));
            textRenderer.drawText(x + 10, y + 35, 11, 
                module.getDescription(), RenderColor.of(120, 120, 130, (int)(255 * alpha)));
        }
    }
    
    private void updateSettingsAnimations() {
        for (Module module : ModuleManager.getInstance().getModules()) {
            float current = settingsAnimations.getOrDefault(module, 0f);
            float target = (module == expandedModule) ? 1f : 0f;
            
            if (Math.abs(current - target) > 0.01f) {
                float newValue = current + (target - current) * ANIMATION_SPEED;
                settingsAnimations.put(module, newValue);
            } else {
                settingsAnimations.put(module, target);
            }
        }
    }
    
    private void renderModuleTooltip(Module module, int mouseX, int mouseY) {
        if (textRenderer == null) return;
        
        String moduleName = module.getName();
        String description = Lang.getModuleDescription(moduleName);
        
        // Если описание не найдено, используем дефолтное
        if (description.equals("module." + moduleName.toLowerCase() + ".description")) {
            description = module.getDescription();
        }
        
        // Формируем строку: "ModuleName - описание"
        String fullText = moduleName + " - " + description;
        
        // Позиционируем текст по центру экрана, чуть ниже верха
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        int fbWidth = plugin.getMainFramebufferWidth();
        
        // Измеряем ширину для центрирования (БОЛЬШИЕ размеры)
        float nameWidth = textRenderer.measureWidth(moduleName, 24);
        float dashWidth = textRenderer.measureWidth(" - ", 20);
        float descWidth = textRenderer.measureWidth(description, 20);
        float totalWidth = nameWidth + dashWidth + descWidth;
        
        int tooltipX = (int)((fbWidth - totalWidth) / 2);
        int tooltipY = 50; // Чуть ниже верха
        
        // Рендерим название модуля (жирным - большой размер 24)
        textRenderer.drawText(tooltipX, tooltipY, 24, 
            moduleName, RenderColor.WHITE);
        
        // Рендерим тире и описание (размер 20)
        float offsetX = tooltipX + nameWidth;
        textRenderer.drawText(offsetX, tooltipY, 20, 
            " - " + description, RenderColor.of(200, 200, 210, 255));
    }

    private void renderRect(float x, float y, float w, float h, float radius, RenderColor color) {
        RectRenderer.drawRoundedRect(x, y, w, h, radius, color);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (input.getKeycode() == 256) {
            this.close();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        double scale = (double) plugin.getMainFramebufferWidth() / this.width;
        
        int mouseX = (int)(click.x() * scale);
        int mouseY = (int)(click.y() * scale);
        int button = click.button();
        
        boolean shiftPressed = GLFW.glfwGetKey(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                               GLFW.glfwGetKey(this.client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        
        if (button == 0) {
            for (CategoryPanel panel : categoryPanels.values()) {
                if (panel.isHeaderHovered(mouseX, mouseY)) {
                    draggingPanel = panel;
                    dragOffsetX = mouseX - panel.getX();
                    dragOffsetY = mouseY - panel.getY();
                    panel.setDragging(true);
                    return true;
                }
            }

            for (CategoryPanel panel : categoryPanels.values()) {
                Module module = panel.getHoveredModule(mouseX, mouseY);
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
                Module module = panel.getHoveredModule(mouseX, mouseY);
                if (module != null) {
                    if (expandedModule == module) {
                        expandedModule = null;
                    } else {
                        expandedModule = module;
                    }
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        if (click.button() == 0 && draggingPanel != null) {
            draggingPanel.setDragging(false);
            draggingPanel = null;
            savePanelPositions();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double offsetX, double offsetY) {
        if (draggingPanel != null && click.button() == 0) {
            MinecraftPlugin plugin = MinecraftPlugin.getInstance();
            double scale = (double) plugin.getMainFramebufferWidth() / this.width;
            
            int mouseX = (int)(click.x() * scale);
            int mouseY = (int)(click.y() * scale);
            
            draggingPanel.setPosition(
                mouseX - dragOffsetX,
                mouseY - dragOffsetY
            );
            return true;
        }
        return false;
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
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
