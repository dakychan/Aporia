package ru.mixin.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.module.ModuleManager;
import ru.ui.clickgui.ClickGuiScreen;
import ru.ui.notify.NotifyRenderer;

@Mixin(Gui.class)
public class ScreenKeyPressMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onHudRender(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        ModuleManager.getInstance().onTick();
        NotifyRenderer.render(context);
        if (mc.screen == null) {
            if (GLFW.glfwGetKey(mc.getWindow().handle(), 96) == GLFW.GLFW_PRESS) {
                mc.setScreen(new ClickGuiScreen(mc.getWindow().getWidth(), mc.getWindow().getHeight()));
            }
        }
    }
}
