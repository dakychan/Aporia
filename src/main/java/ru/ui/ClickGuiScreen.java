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
        
        // Get REAL window dimensions (not scaled)
        int windowWidth = MinecraftPlugin.getInstance().getMainFramebufferWidth();
        int windowHeight = MinecraftPlugin.getInstance().getMainFramebufferHeight();
        
        // Calculate scale factor and reduce by 1.5x
        float baseScale = (float) this.client.getWindow().getScaleFactor();
        float guiScale = baseScale / 1.5f;
        
        // Scale mouse coordinates properly
        int scaledMouseX = (int)(mouseX * baseScale);
        int scaledMouseY = (int)(mouseY * baseScale);
        
        // Render categories as separate columns (no main background)
        Module.Category[] categories = Module.Category.values();
        int totalWidth = (int)((CATEGORY_WIDTH * categories.length) + (CATEGORY_SPACING * (categories.length - 1)) * guiScale);
        int startX = (windowWidth - totalWidth) / 2;
        int startY = (int)(50 * guiScale);
        
        hoveredModule = null;
        
        int categoryX = startX;
        for (Module.Category category : categories) {
            renderCategory(category, categoryX, startY, (int)(CATEGORY_WIDTH * guiScale), (int)(400 * guiScale), 
                          scaledMouseX, scaledMouseY, guiScale);
            categoryX += (int)((CATEGORY_WIDTH + CATEGORY_SPACING) * guiScale);
        }
    }
    
    private void renderCategory(Module.Category category, int x, int y, int width, int height, int mouseX, int mouseY, float scale) {
        // Render category background
        renderRect(x, y, width, height, 8 * scale, RenderColor.of(20, 20, 25, 230));
        
        // Render category header
        renderRect(x, y, width, (int)(HEADER_HEIGHT * scale), 8 * scale, RenderColor.of(30, 30, 38, 255));
        
        // Render category name (text 3px lower)
        if (textRenderer != null) {
            textRenderer.drawText(x + (int)(10 * scale), y + (int)(11 * scale), 13 * scale, category.getDisplayName(), RenderColor.WHITE);
        }
        
        // Render modules
        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
        int moduleY = y + (int)(HEADER_HEIGHT * scale) + (int)(5 * scale);
        
        for (Module module : modules) {
            if (moduleY + (int)(MODULE_HEIGHT * scale) > y + height - (int)(5 * scale)) {
                break; // Don't render modules that would overflow
            }
            
            renderModule(module, x + (int)(5 * scale), moduleY, width - (int)(10 * scale), (int)(MODULE_HEIGHT * scale), mouseX, mouseY, scale);
            moduleY += (int)((MODULE_HEIGHT + MODULE_SPACING) * scale);
        }
    }
    
    private void renderModule(Module module, int x, int y, int width, int height, int mouseX, int mouseY, float scale) {
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
        renderRect(x, y, width, height, 5 * scale, bgColor);
        
        // Render module name (text 3px lower)
        if (textRenderer != null) {
            textRenderer.drawText(x + (int)(8 * scale), y + (int)(11 * scale), 11 * scale, module.getName(), textColor);
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
