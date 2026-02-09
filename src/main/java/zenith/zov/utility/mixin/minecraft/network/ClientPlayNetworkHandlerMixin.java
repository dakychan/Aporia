package zenith.zov.utility.mixin.minecraft.network;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zenith.zov.Zenith;


@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void sendChatMessageHook(@NotNull String message, CallbackInfo ci) {



        if (message.startsWith(Zenith.getInstance().getCommandManager().getPrefix())) {
            try {
                Zenith.getInstance().getCommandManager().getDispatcher().execute(
                        message.substring(Zenith.getInstance().getCommandManager().getPrefix().length()),
                        Zenith.getInstance().getCommandManager().getSource()
                );
            } catch (CommandSyntaxException ignored) {}

            ci.cancel();
        }
    }
}