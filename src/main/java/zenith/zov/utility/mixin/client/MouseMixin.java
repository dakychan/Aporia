package zenith.zov.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zenith.zov.base.events.impl.input.EventKey;
import zenith.zov.base.events.impl.input.EventMouse;
import zenith.zov.base.events.impl.input.EventHotBarScroll;
import zenith.zov.base.events.impl.input.EventMouseRotation;

import static zenith.zov.utility.interfaces.IMinecraft.mc;

@Mixin(Mouse.class)
public class MouseMixin {

    @Shadow private int activeButton;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {

        if (button != GLFW.GLFW_KEY_UNKNOWN && window == mc.getWindow().getHandle()) {
            EventManager.call(new EventKey( action, button));
            EventManager.call(new EventMouse(button, action));
        }
    }

    @Inject(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"), cancellable = true)
    public void onMouseScrollHook(long window, double horizontal, double vertical, CallbackInfo ci) {
        EventHotBarScroll event = new EventHotBarScroll(horizontal, vertical);
        EventManager.call(event);
        if (event.isCancelled()) ci.cancel();
    }

    @Redirect(method = "tick",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Mouse;isCursorLocked()Z"))
    public boolean onIsCursorLocked(Mouse instance) {

        return instance.isCursorLocked() ||isAnim();
    }
    @WrapWithCondition(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"), require = 1, allow = 1)
    private boolean modifyMouseRotationInput(ClientPlayerEntity instance, double cursorDeltaX, double cursorDeltaY) {
        EventMouseRotation event = new EventMouseRotation((float) cursorDeltaX, (float) cursorDeltaY);
        EventManager.call(event);
        if (event.isCancelled()) return false;
        instance.changeLookDirection(event.getCursorDeltaX(), event.getCursorDeltaY());
        return false;
    }
    @Unique
    private boolean isAnim(){
        Screen screen = MinecraftClient.getInstance().currentScreen;
//        if(screen instanceof InventoryScreen inventoryScreen){
//            return inventoryScreen.isAnimationClose();
//        }
//        if(screen instanceof ContainerScreen containerScreen){
//            return  containerScreen.isAnimationClose();
//        }
//        if(screen instanceof MenuScreen menuScreen){
//            return menuScreen.isClosing();
//        }
        return false;
    }
}
