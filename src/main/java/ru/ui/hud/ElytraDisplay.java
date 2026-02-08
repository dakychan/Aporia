package ru.ui.hud;

import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import ru.render.MsdfTextRenderer;
import ru.util.MathUtils;

import java.util.List;

/**
 * Display component for elytra flight trajectory visualization.
 * Shows trajectory data and visualizes the flight path.
 * 
 * Requirements: 9.1, 6.5
 */
public class ElytraDisplay {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    
    public ElytraDisplay(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Renders the elytra display showing trajectory data for the player.
     * 
     * @param context Draw context
     * @param player The client player
     * @param textRenderer Text renderer for drawing text
     */
    public void render(DrawContext context, ClientPlayerEntity player, MsdfTextRenderer textRenderer) {
        if (player == null || textRenderer == null) {
            return;
        }
        
        // Render background panel
        renderRect(x, y, width, height, 5, RenderColor.of(35, 35, 45, 180));
        
        // Render title
        textRenderer.drawText(x + 10, y + 10, 14, "Elytra Flight", RenderColor.WHITE);
        
        // Check if player is flying with elytra
        if (!player.isGliding()) {
            textRenderer.drawText(x + 10, y + 35, 12, "Not flying", RenderColor.of(150, 150, 150, 200));
            return;
        }
        
        // Get player position and velocity
        Vec3d position = player.getEntityPos();
        Vec3d velocity = player.getVelocity();
        float pitch = player.getPitch();
        float yaw = player.getYaw();
        
        // Calculate trajectory
        List<Vec3d> trajectory = MathUtils.calculateElytraTrajectory(position, velocity, pitch, yaw);
        double maxDistance = MathUtils.calculateElytraMaxDistance(position, velocity);
        
        // Display trajectory data
        int textY = y + 35;
        textRenderer.drawText(x + 10, textY, 12, 
            String.format("Distance: %.1f blocks", maxDistance), 
            RenderColor.of(200, 200, 200, 255));
        
        textY += 18;
        textRenderer.drawText(x + 10, textY, 12, 
            String.format("Velocity: %.2f m/s", velocity.length()), 
            RenderColor.of(200, 200, 200, 255));
        
        textY += 18;
        textRenderer.drawText(x + 10, textY, 12, 
            String.format("Points: %d", trajectory.size()), 
            RenderColor.of(200, 200, 200, 255));
        
        // Render trajectory visualization
        if (!trajectory.isEmpty()) {
            renderTrajectory(context, trajectory);
        }
    }
    
    /**
     * Renders a visual representation of the flight trajectory.
     * 
     * @param context Draw context
     * @param trajectory List of trajectory points
     */
    public void renderTrajectory(DrawContext context, List<Vec3d> trajectory) {
        if (trajectory == null || trajectory.isEmpty()) {
            return;
        }
        
        // Render a simplified trajectory visualization
        // We'll show a small graph at the bottom of the display
        int graphX = x + 10;
        int graphY = y + height - 40;
        int graphWidth = width - 20;
        int graphHeight = 30;
        
        // Background for graph
        renderRect(graphX, graphY, graphWidth, graphHeight, 3, RenderColor.of(20, 20, 25, 200));
        
        // Find min/max Y values for scaling
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        for (Vec3d point : trajectory) {
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }
        
        // Avoid division by zero
        double yRange = maxY - minY;
        if (yRange < 0.1) {
            yRange = 1.0;
        }
        
        // Draw trajectory line as a series of small rectangles
        for (int i = 0; i < trajectory.size() - 1; i++) {
            Vec3d point = trajectory.get(i);
            
            // Map trajectory index to X position
            float pointX = graphX + (i * graphWidth / (float) trajectory.size());
            
            // Map Y coordinate to graph height (inverted because screen Y increases downward)
            float normalizedY = (float) ((point.y - minY) / yRange);
            float pointY = graphY + graphHeight - (normalizedY * graphHeight);
            
            // Color gradient from green (start) to red (end)
            float progress = i / (float) trajectory.size();
            int r = (int) (progress * 255);
            int g = (int) ((1 - progress) * 255);
            
            renderRect((int) pointX, (int) pointY, 2, 2, 1, RenderColor.of(r, g, 50, 255));
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
