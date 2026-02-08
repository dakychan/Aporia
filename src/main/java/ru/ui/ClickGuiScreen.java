package ru.ui;

import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.module.Module;
import ru.module.ModuleManager;
import ru.render.MsdfFont;
import ru.render.MsdfTextRenderer;

import java.util.List;

/**
 * Modern ClickGUI with column-based category layout.
 * Each category is a column with modules displayed as rounded rectangles.
 */
public class ClickGuiScreen extends Screen {
    private static MsdfFont font;
    private static MsdfTextRenderer textRenderer;
    private static boolean initialized = false;

    private int guiX;
    private int guiY;
    private final int guiWidth;
    private final int guiHeight;
    
    private boolean isDragging = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;
    
    private Module hoveredModule = null;
    
    // Layout constants
    private static final int CATEGORY_WIDTH = 140;
    private static final int CATEGORY_SPACING = 10;
    private static final int MODULE_HEIGHT = 28;
    private static final int MODULE_SPACING = 6;
    private static final int PADDING = 15;
    private static final int HEADER_HEIGHT = 35;

    public ClickGuiScreen(int width, int height) {
        super(Text.literal("Click GUI"));
        
        // Calculate GUI size based on categories
        Module.Category[] categories = Module.Category.values();
        this.guiWidth = PADDING * 2 + (CATEGORY_WIDTH * categories.length) + (CATEGORY_SPACING * (categories.length - 1));
        this.guiHeight = 500;
        this.guiX = (width - this.guiWidth) / 2;
        this.guiY = (height - this.guiHeight) / 2;
        
        initFont();
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

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        if (!initialized) return;
        MinecraftPlugin.getInstance().bindMainFramebuffer(true);
        // Render main GUI background
        renderRect(guiX, guiY, guiWidth, guiHeight, 8, RenderColor.of(25, 25, 30, 240));
        
        // Render header
        renderRect(guiX, guiY, guiWidth, HEADER_HEIGHT, 8, RenderColor.of(35, 35, 45, 255));
        
        // Render title
        if (textRenderer != null) {
            textRenderer.drawText(guiX + PADDING, guiY + 10, 16, "Aporia Client", RenderColor.WHITE);
        }
        
        // Render categories
        Module.Category[] categories = Module.Category.values();
        int categoryX = guiX + PADDING;
        int categoryY = guiY + HEADER_HEIGHT + PADDING;
        int categoryHeight = guiHeight - HEADER_HEIGHT - PADDING * 2;
        
        hoveredModule = null;
        
        for (Module.Category category : categories) {
            renderCategory(category, categoryX, categoryY, CATEGORY_WIDTH, categoryHeight, mouseX, mouseY);
            categoryX += CATEGORY_WIDTH + CATEGORY_SPACING;
        }
    }
    
    private void renderCategory(Module.Category category, int x, int y, int width, int height, int mouseX, int mouseY) {
        // Render category background
        renderRect(x, y, width, height, 6, RenderColor.of(30, 30, 38, 200));
        
        // Render category header
        renderRect(x, y, width, HEADER_HEIGHT - 5, 6, RenderColor.of(40, 40, 50, 255));
        
        // Render category name
        if (textRenderer != null) {
            textRenderer.drawText(x + 10, y + 8, 14, category.getDisplayName(), RenderColor.WHITE);
        }
        
        // Render modules
        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
        int moduleY = y + HEADER_HEIGHT;
        
        for (Module module : modules) {
            if (moduleY + MODULE_HEIGHT > y + height - 5) {
                break; // Don't render modules that would overflow
            }
            
            renderModule(module, x + 5, moduleY, width - 10, MODULE_HEIGHT, mouseX, mouseY);
            moduleY += MODULE_HEIGHT + MODULE_SPACING;
        }
    }
    
    private void renderModule(Module module, int x, int y, int width, int height, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        boolean isEnabled = module.isEnabled();
        
        if (isHovered) {
            hoveredModule = module;
        }
        
        // Determine colors
        RenderColor bgColor;
        RenderColor textColor;
        
        if (isEnabled) {
            bgColor = isHovered ? RenderColor.of(70, 130, 255, 255) : RenderColor.of(60, 120, 245, 230);
            textColor = RenderColor.WHITE;
        } else {
            bgColor = isHovered ? RenderColor.of(50, 50, 60, 200) : RenderColor.of(40, 40, 50, 180);
            textColor = RenderColor.of(180, 180, 190, 255);
        }
        
        // Render module background
        renderRect(x, y, width, height, 4, bgColor);
        
        // Render module name
        if (textRenderer != null) {
            textRenderer.drawText(x + 8, y + 8, 12, module.getName(), textColor);
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
        if (button != 0) return false;
        
        // Check if title bar was clicked for dragging
        if (x >= guiX && x <= guiX + guiWidth && y >= guiY && y <= guiY + HEADER_HEIGHT) {
            isDragging = true;
            dragOffsetX = x - guiX;
            dragOffsetY = y - guiY;
            return true;
        }
        
        // Check if a module was clicked
        if (hoveredModule != null) {
            hoveredModule.toggle();
            return true;
        }
        
        return false;
    }

    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return false;
    }

    public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        if (isDragging) {
            guiX = (int) (x - dragOffsetX);
            guiY = (int) (y - dragOffsetY);
            return true;
        }
        return false;
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        // Override to prevent default background blur
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
}
