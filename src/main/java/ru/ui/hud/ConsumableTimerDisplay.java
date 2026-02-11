package ru.ui.hud;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import ru.render.MsdfTextRenderer;
import ru.util.math.MathUtils;

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

    public void render(GuiGraphics context, LocalPlayer player, MsdfTextRenderer textRenderer) {
        if (player == null || textRenderer == null) {
            return;
        }
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        plugin.bindMainFramebuffer(true);

        renderRect(x, y, width, height, 5, RenderColor.of(35, 35, 45, 180));

        textRenderer.drawText(x + 10, y + 10, 14, "Consumable Timer", RenderColor.WHITE);

        if (!player.isUsingItem()) {
            textRenderer.drawText(x + 10, y + 35, 12, "Not consuming", RenderColor.of(150, 150, 150, 200));
            return;
        }

        ItemStack activeItem = player.getActiveItem();
        int remainingTime = MathUtils.getRemainingTime(player);
        float progress = MathUtils.getConsumptionProgress(player);

        String itemName = activeItem.getItemName().getString();
        if (itemName.length() > 15) {
            itemName = itemName.substring(0, 15) + "...";
        }

        int textY = y + 35;
        textRenderer.drawText(x + 10, textY, 12, itemName, RenderColor.of(200, 200, 200, 255));

        textY += 18;
        float remainingSeconds = remainingTime / 20.0f;
        textRenderer.drawText(x + 10, textY, 12, 
            String.format("Time left: %.1fs", remainingSeconds), 
            RenderColor.of(200, 200, 200, 255));

        textY += 18;
        textRenderer.drawText(x + 10, textY, 12, 
            String.format("Progress: %.0f%%", progress * 100), 
            RenderColor.of(200, 200, 200, 255));

        renderProgressBar(context, progress);
    }

    public void renderProgressBar(GuiGraphics context, float progress) {
        if (progress < 0) progress = 0;
        if (progress > 1) progress = 1;
        
        int barX = x + 10;
        int barY = y + height - 25;
        int barWidth = width - 20;
        int barHeight = 15;

        renderRect(barX, barY, barWidth, barHeight, 3, RenderColor.of(20, 20, 25, 200));

        int fillWidth = (int) (barWidth * progress);
        if (fillWidth > 0) {

            int r = (int) ((1 - progress) * 255);
            int g = (int) (progress * 255);
            
            renderRect(barX, barY, fillWidth, barHeight, 3, RenderColor.of(r, g, 50, 255));
        }
    }

    private void renderRect(int x, int y, int w, int h, float radius, RenderColor color) {
        new RoundedRectDrawer()
                .rectSized(x, y, w, h, radius, RectColors.oneColor(color))
                .build()
                .tryDraw()
                .close();
    }
}
