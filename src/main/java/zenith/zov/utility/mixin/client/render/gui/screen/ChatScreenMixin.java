package zenith.zov.utility.mixin.client.render.gui.screen;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zenith.zov.Zenith;
import zenith.zov.utility.interfaces.IMinecraft;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen implements IMinecraft {

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "sendMessage(Ljava/lang/String;Z)V", at = @At("HEAD"), cancellable = false)
    private void onSendMessage(String text, boolean addToHistory, CallbackInfo ci) {

//        if (Zenith.getInstance().getCommandManager().dispatch(text)) {
//            mc.inGameHud.getChatHud().addToMessageHistory(text);
//            ci.cancel();
//        }
    }
}
