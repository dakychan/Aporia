package aporia.cc.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import aporia.cc.base.events.impl.render.EventFog;
import aporia.cc.utility.render.display.base.color.ColorUtil;

@Mixin(FogRenderer.class)
public class BackGroundRendererMixin {

    @Unique
    private static final ThreadLocal<EventFog> FOG_EVENT = new ThreadLocal<>();

    @Inject(method = "applyFog", at = @At("HEAD"))
    private void onApplyFogHead(Camera camera, int fogType, boolean thickFog, RenderTickCounter tickCounter, float viewDistance, ClientWorld world, CallbackInfoReturnable<Vector4f> cir) {
        EventFog event = new EventFog();
        EventManager.call(event);
        if (event.isCancelled()) {
            FOG_EVENT.set(event);
        } else {
            FOG_EVENT.remove();
        }
    }

    @ModifyArgs(method = "applyFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void modifyFogArgs(Args args) {
        EventFog event = FOG_EVENT.get();
        if (event == null || !event.isCancelled()) {
            return;
        }

        int color = event.getColor();
        Vector4f fogColor = (Vector4f) args.get(2);
        fogColor.set(ColorUtil.redf(color), ColorUtil.greenf(color), ColorUtil.bluef(color), ColorUtil.alphaf(color));

        float start = 2.0F;
        float end = event.getDistance();
        args.set(3, start);
        args.set(4, end);
        args.set(5, start);
        args.set(6, end);
    }

    @Inject(method = "applyFog", at = @At("RETURN"), cancellable = true)
    private void onApplyFogReturn(Camera camera, int fogType, boolean thickFog, RenderTickCounter tickCounter, float viewDistance, ClientWorld world, CallbackInfoReturnable<Vector4f> cir) {
        EventFog event = FOG_EVENT.get();
        if (event != null && event.isCancelled()) {
            int color = event.getColor();
            cir.setReturnValue(new Vector4f(ColorUtil.redf(color), ColorUtil.greenf(color), ColorUtil.bluef(color), ColorUtil.alphaf(color)));
        }
        FOG_EVENT.remove();
    }
}

