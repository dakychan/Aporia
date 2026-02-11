package ru.mixin.chat;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.module.ModuleManager;
import ru.module.impl.misc.AutoFlyMe;

@Mixin(ChatComponent.class)
public class ChatMessageMixin {

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD")
    )
    private void onChatMessage(Component message, CallbackInfo ci) {
        ModuleManager.getInstance().getModules().stream()
                .filter(module -> module instanceof AutoFlyMe && module.isEnabled())
                .forEach(module -> ((AutoFlyMe) module).onChatMessage(message));
    }
}