package zenith.zov.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zenith.zov.Zenith;
import zenith.zov.base.events.impl.input.EventSetScreen;
import zenith.zov.base.events.impl.other.EventWindowResize;

import static zenith.zov.utility.interfaces.IMinecraft.mc;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    @Final
    private Window window;

    @Shadow
    public abstract Window getWindow();

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient$1;<init>(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/RunArgs;)V"))
    public void init(RunArgs args, CallbackInfo ci) {
        Zenith.getInstance().init();
    }
    @Inject(method = "onResolutionChanged", at = @At("TAIL"))
    private void captureResize(CallbackInfo ci) {

    }
    @ModifyVariable(
            method = "setScreen(Lnet/minecraft/client/gui/screen/Screen;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Screen mixin$modifySetScreenArg(Screen original) {

        EventSetScreen event = new EventSetScreen(original);

        EventManager.call(event);
        return event.getScreen();
    }
}
