package ru.ui;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.module.Module;
import ru.module.ModuleManager;
import ru.render.BlurRenderer;
import ru.render.MsdfFont;
import ru.render.MsdfTextRenderer;

import java.util.List;

/**
 * Modern ClickGUI with column-based category layout.
 * Each category is a separate column with modules displayed as rounded rectangles.
 */
public class ClickGuiScreen extends Screen {
    private static MsdfFont font;
    private static MsdfTextRenderer textRenderer;
    private static boolean initialized = false;
    private static BlurRenderer blurRenderer;
    
    private Module hoveredModule = null;
    
    // Layout constants
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
        
        // Bind framebuffer ONCE at the start
        MinecraftPlugin.getInstance().bindMainFramebuffer(true);
        
        // Get actual window dimensions
        int windowWidth = this.client.getWindow().getScaledWidth();
        int windowHeight = this.client.getWindow().getScaledHeight();
        
        // Apply blur to background
        if (blurRenderer != null) {
            blurRenderer.applyBlur(windowWidth, windowHeight);
        }
        
        // Render categories as separate columns (no main background)
        Module.Category[] categories = Module.Category.values();
        int totalWidth = (CATEGORY_WIDTH * categories.length) + (CATEGORY_SPACING * (categories.length - 1));
        int startX = (windowWidth - totalWidth) / 2;
        int startY = 50;
        
        hoveredModule = null;
        
        int categoryX = startX;
        for (Module.Category category : categories) {
            renderCategory(category, categoryX, startY, CATEGORY_WIDTH, 400, mouseX, mouseY);
            categoryX += CATEGORY_WIDTH + CATEGORY_SPACING;
        }
    }
    
    private void renderCategory(Module.Category category, int x, int y, int width, int height, int mouseX, int mouseY) {
        // Render category background
        renderRect(x, y, width, height, 8, RenderColor.of(20, 20, 25, 230));
        
        // Render category header
        renderRect(x, y, width, HEADER_HEIGHT, 8, RenderColor.of(30, 30, 38, 255));
        
        // Render category name
        if (textRenderer != null) {
            textRenderer.drawText(x + 10, y + 8, 13, category.getDisplayName(), RenderColor.WHITE);
        }
        
        // Render modules
        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
        int moduleY = y + HEADER_HEIGHT + 5;
        
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
        renderRect(x, y, width, height, 5, bgColor);
        
        // Render module name
        if (textRenderer != null) {
            textRenderer.drawText(x + 8, y + 8, 11, module.getName(), textColor);
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
        
        // Check if a module was clicked
        if (hoveredModule != null) {
            hoveredModule.toggle();
            return true;
        }
        
        return false;
    }

    public boolean mouseReleased(double x, double y, int button) {
        return false;
    }

    public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
        return false;
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
}
