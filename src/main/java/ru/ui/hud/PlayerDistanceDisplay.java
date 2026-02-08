package ru.ui.hud;

import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import ru.render.MsdfTextRenderer;
import ru.util.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Display component for showing distances to other players.
 * Formats distances with player names.
 * 
 * Requirements: 9.2, 7.4
 */
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
    
    /**
     * Renders the player distance display showing distances to all nearby players.
     * 
     * @param context Draw context
     * @param player The local client player
     * @param textRenderer Text renderer for drawing text
     */
    public void render(DrawContext context, ClientPlayerEntity player, MsdfTextRenderer textRenderer) {
        if (player == null || textRenderer == null) {
            return;
        }
        
        // Render background panel
        renderRect(x, y, width, height, 5, RenderColor.of(35, 35, 45, 180));
        
        // Render title
        textRenderer.drawText(x + 10, y + 10, 14, "Player Distances", RenderColor.WHITE);
        
        // Calculate distances to all players
        Map<AbstractClientPlayerEntity, Double> distances = MathUtils.calculateDistancesToPlayers(player);
        
        if (distances.isEmpty()) {
            textRenderer.drawText(x + 10, y + 35, 12, "No players nearby", RenderColor.of(150, 150, 150, 200));
            return;
        }
        
        // Sort players by distance (closest first)
        List<Map.Entry<AbstractClientPlayerEntity, Double>> sortedDistances = new ArrayList<>(distances.entrySet());
        sortedDistances.sort(Map.Entry.comparingByValue());
        
        // Display up to 5 closest players
        int textY = y + 35;
        int maxPlayers = Math.min(5, sortedDistances.size());
        
        for (int i = 0; i < maxPlayers; i++) {
            Map.Entry<AbstractClientPlayerEntity, Double> entry = sortedDistances.get(i);
            AbstractClientPlayerEntity otherPlayer = entry.getKey();
            double distance = entry.getValue();
            
            // Get player name
            String playerName = otherPlayer.getName().getString();
            if (playerName.length() > 12) {
                playerName = playerName.substring(0, 12) + "...";
            }
            
            // Format distance text
            String distanceText = String.format("%s: %.1f blocks", playerName, distance);
            
            // Color based on distance (green = close, yellow = medium, red = far)
            RenderColor color;
            if (distance < 20) {
                color = RenderColor.of(100, 255, 100, 255); // Green
            } else if (distance < 50) {
                color = RenderColor.of(255, 255, 100, 255); // Yellow
            } else {
                color = RenderColor.of(255, 100, 100, 255); // Red
            }
            
            textRenderer.drawText(x + 10, textY, 12, distanceText, color);
            textY += 18;
        }
        
        // Show total player count if more than 5
        if (sortedDistances.size() > 5) {
            textRenderer.drawText(x + 10, textY, 11, 
                String.format("+ %d more players", sortedDistances.size() - 5), 
                RenderColor.of(150, 150, 150, 200));
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
