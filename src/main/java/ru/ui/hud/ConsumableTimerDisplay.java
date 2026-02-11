package ru.ui.hud;

import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import ru.render.MsdfTextRenderer;
import ru.util.MathUtils;

/**
 * Display component for showing consumable item usage time.
 * Shows usage time and a visual progress bar.
 * 
 * Requirements: 9.3, 8.5
 */
public class ConsumableTimerDisplay {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    
    public ConsumableTimerDisplay(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Renders the consumable timer display showing usage time and progress.
     * 
     * @param context Draw context
     * @param player The client player
     * @param textRenderer Text renderer for drawing text
     */
    public void render(GuiGraphics context, LocalPlayer player, MsdfTextRenderer textRenderer) {
        if (player == null || textRenderer == null) {
            return;
        }
        
        // Render background panel
        renderRect(x, y, width, height, 5, RenderColor.of(35, 35, 45, 180));
        
        // Render title
        textRenderer.drawText(x + 10, y + 10, 14, "Consumable Timer", RenderColor.WHITE);
        
        // Check if player is using an item
        if (!player.isUsingItem()) {
            textRenderer.drawText(x + 10, y + 35, 12, "Not consuming", RenderColor.of(150, 150, 150, 200));
            return;
        }
        
        // Get active item and consumption data
        ItemStack activeItem = player.getActiveItem();
        int remainingTime = MathUtils.getRemainingTime(player);
        float progress = MathUtils.getConsumptionProgress(player);
        
        // Get item name
        String itemName = activeItem.getItemName().getString();
        if (itemName.length() > 15) {
            itemName = itemName.substring(0, 15) + "...";
        }
        
        // Display item name
        int textY = y + 35;
        textRenderer.drawText(x + 10, textY, 12, itemName, RenderColor.of(200, 200, 200, 255));
        
        // Display remaining time
        textY += 18;
        float remainingSeconds = remainingTime / 20.0f; // Convert ticks to seconds
        textRenderer.drawText(x + 10, textY, 12, 
            String.format("Time left: %.1fs", remainingSeconds), 
            RenderColor.of(200, 200, 200, 255));
        
        // Display progress percentage
        textY += 18;
        textRenderer.drawText(x + 10, textY, 12, 
            String.format("Progress: %.0f%%", progress * 100), 
            RenderColor.of(200, 200, 200, 255));
        
        // Render progress bar
        renderProgressBar(context, progress);
    }
    
    /**
     * Renders a visual progress bar showing consumption progress.
     * 
     * @param context Draw context
     * @param progress Progress from 0.0 to 1.0
     */
    public void renderProgressBar(GuiGraphics context, float progress) {
        if (progress < 0) progress = 0;
        if (progress > 1) progress = 1;
        
        int barX = x + 10;
        int barY = y + height - 25;
        int barWidth = width - 20;
        int barHeight = 15;
        
        // Background bar
        renderRect(barX, barY, barWidth, barHeight, 3, RenderColor.of(20, 20, 25, 200));
        
        // Progress fill
        int fillWidth = (int) (barWidth * progress);
        if (fillWidth > 0) {
            // Color gradient from red (start) to green (complete)
            int r = (int) ((1 - progress) * 255);
            int g = (int) (progress * 255);
            
            renderRect(barX, barY, fillWidth, barHeight, 3, RenderColor.of(r, g, 50, 255));
        }
    }
    
    /**
     * Helper method to render a rounded rectangle using CometRenderer.
     */
    private void renderRect(int x, int y, int w, int h, float radius, RenderColor color) {
        new RoundedRectDrawer()
                .rectSized(x, y, w, h, radius, RectColors.oneColor(color))
                .build()
                .tryDraw()
                .close();
    }
}
