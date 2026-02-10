package ru.mixin.chat;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.module.ModuleManager;
import ru.module.impl.misc.AutoFlyMe;

@Mixin(ChatHud.class)
public class ChatMessageMixin {
    
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void onChatMessage(Text message, CallbackInfo ci) {
        // Передаем сообщение в AutoFlyMe модуль
        ModuleManager.getInstance().getModules().stream()
            .filter(module -> module instanceof AutoFlyMe && module.isEnabled())
            .forEach(module -> ((AutoFlyMe) module).onChatMessage(message));
    }
}
