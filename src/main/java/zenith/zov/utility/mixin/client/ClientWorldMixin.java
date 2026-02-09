package zenith.zov.utility.mixin.client;

import com.darkmagician6.eventapi.EventManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zenith.zov.base.events.impl.other.EventSpawnEntity;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "addEntity",at = @At(value = "RETURN"))
    public void injectAddEntity(Entity entity, CallbackInfo ci) {
        EventSpawnEntity eventSpawnEntity = new EventSpawnEntity(entity);
        EventManager.call(eventSpawnEntity);
    }
}
