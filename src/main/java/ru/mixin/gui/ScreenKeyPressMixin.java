package ru.mixin.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.module.ModuleManager;
import ru.ui.clickgui.ClickGuiScreen;
import ru.ui.notify.NotifyRenderer;

@Mixin(InGameHud.class)
public class ScreenKeyPressMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onHudRender(net.minecraft.client.gui.DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        
        ModuleManager.getInstance().onTick();
        
        NotifyRenderer.render(context);
        
        if (mc.currentScreen == null) {
            if (org.lwjgl.glfw.GLFW.glfwGetKey(mc.getWindow().getHandle(), 96) == org.lwjgl.glfw.GLFW.GLFW_PRESS) {
                mc.setScreen(new ClickGuiScreen(mc.getWindow().getWidth(), mc.getWindow().getHeight()));
            }
        }
    }
}
