package aporia.su.mixin.renderer;

import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import aporia.su.events.api.EventManager;
import aporia.su.events.impl.ChunkOcclusionEvent;

@Mixin(ChunkOcclusionDataBuilder.class)
public abstract class ChunkOcclusionDataBuilderMixin {

    @Inject(method = "markClosed", at = @At("HEAD"), cancellable = true)
    private void onMarkClosed(BlockPos pos, CallbackInfo info) {
        ChunkOcclusionEvent event = ChunkOcclusionEvent.get();
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            info.cancel();
        }
    }
}