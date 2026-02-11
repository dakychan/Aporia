package ru.ui.hud;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import ru.render.MsdfTextRenderer;
import ru.util.math.MathUtils;

import java.util.List;

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

    public void render(GuiGraphics context, LocalPlayer player, MsdfTextRenderer textRenderer) {
        if (player == null || textRenderer == null) {
            return;
        }
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        plugin.bindMainFramebuffer(true);

        renderRect(x, y, width, height, 5, RenderColor.of(35, 35, 45, 180));
        
        textRenderer.drawText(x + 10, y + 10, 14, "Elytra Flight", RenderColor.WHITE);
        
        if (!player.isFlyingVehicle()) {
            textRenderer.drawText(x + 10, y + 35, 12, "Not flying", RenderColor.of(150, 150, 150, 200));
            return;
        }

        Vec3 position = player.position();
        Vec3 velocity = player.getDeltaMovement();
        float pitch = player.getXRot();
        float yaw = player.getYRot();

        List<Vec3> trajectory = MathUtils.calculateElytraTrajectory(position, velocity, pitch, yaw);
        double maxDistance = MathUtils.calculateElytraMaxDistance(position, velocity);

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

        if (!trajectory.isEmpty()) {
            renderTrajectory(context, trajectory);
        }
    }

    public void renderTrajectory(GuiGraphics context, List<Vec3> trajectory) {
        if (trajectory == null || trajectory.isEmpty()) {
            return;
        }

        int graphX = x + 10;
        int graphY = y + height - 40;
        int graphWidth = width - 20;
        int graphHeight = 30;

        renderRect(graphX, graphY, graphWidth, graphHeight, 3, RenderColor.of(20, 20, 25, 200));

        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;
        for (Vec3 point : trajectory) {
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }

        double yRange = maxY - minY;
        if (yRange < 0.1) {
            yRange = 1.0;
        }
        
        for (int i = 0; i < trajectory.size() - 1; i++) {
            Vec3 point = trajectory.get(i);
            
            float pointX = graphX + (i * graphWidth / (float) trajectory.size());
            
            float normalizedY = (float) ((point.y - minY) / yRange);
            float pointY = graphY + graphHeight - (normalizedY * graphHeight);
            
            float progress = i / (float) trajectory.size();
            int r = (int) (progress * 255);
            int g = (int) ((1 - progress) * 255);
            
            renderRect((int) pointX, (int) pointY, 2, 2, 1, RenderColor.of(r, g, 50, 255));
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
