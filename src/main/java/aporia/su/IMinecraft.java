package aporia.su;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.Window;
import aporia.su.util.render.draw.DrawEngine;
import aporia.su.util.render.draw.DrawEngineImpl;

/**
 *  © 2026 Copyright Aporia.cc 2.0
 *        All Rights Reserved ®
 */

public interface IMinecraft {
    MinecraftClient mc = MinecraftClient.getInstance();
    Window window = MinecraftClient.getInstance().getWindow();
    Tessellator tessellator = Tessellator.getInstance();
    RenderTickCounter tickCounter = mc.getRenderTickCounter();
    DrawEngine drawEngine = new DrawEngineImpl();
}
