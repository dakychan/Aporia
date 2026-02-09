package zenith.zov.utility.mixin.client.render;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zenith.zov.base.events.impl.other.EventWindowResize;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "onWindowSizeChanged",at = @At(value = "TAIL"))
    private void onWindowSizeChanged(long window,int width,int height,CallbackInfo ci) {
        EventManager.call(new EventWindowResize());
    }

}
