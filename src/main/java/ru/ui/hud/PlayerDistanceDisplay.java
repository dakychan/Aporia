package ru.ui.hud;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import ru.render.MsdfTextRenderer;
import ru.util.math.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayerDistanceDisplay {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    
    public PlayerDistanceDisplay(int x, int y, int width, int height) {
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
        
        textRenderer.drawText(x + 10, y + 10, 14, "Player Distances", RenderColor.WHITE);
        
        Map<AbstractClientPlayer, Double> distances = MathUtils.calculateDistancesToPlayers(player);
        
        if (distances.isEmpty()) {
            textRenderer.drawText(x + 10, y + 35, 12, "No players nearby", RenderColor.of(150, 150, 150, 200));
            return;
        }
        
        List<Map.Entry<AbstractClientPlayer, Double>> sortedDistances = new ArrayList<>(distances.entrySet());
        sortedDistances.sort(Map.Entry.comparingByValue());
        
        int textY = y + 35;
        int maxPlayers = Math.min(5, sortedDistances.size());
        
        for (int i = 0; i < maxPlayers; i++) {
            Map.Entry<AbstractClientPlayer, Double> entry = sortedDistances.get(i);
            AbstractClientPlayer otherPlayer = entry.getKey();
            double distance = entry.getValue();
            
            String playerName = otherPlayer.getName().getString();
            if (playerName.length() > 12) {
                playerName = playerName.substring(0, 12) + "...";
            }
            
            String distanceText = String.format("%s: %.1f blocks", playerName, distance);
            
            RenderColor color;
            if (distance < 20) {
                color = RenderColor.of(100, 255, 100, 255);
            } else if (distance < 50) {
                color = RenderColor.of(255, 255, 100, 255);
            } else {
                color = RenderColor.of(255, 100, 100, 255);
            }
            
            textRenderer.drawText(x + 10, textY, 12, distanceText, color);
            textY += 18;
        }
        
        if (sortedDistances.size() > 5) {
            textRenderer.drawText(x + 10, textY, 11, 
                String.format("+ %d more players", sortedDistances.size() - 5), 
                RenderColor.of(150, 150, 150, 200));
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
