package aporia.su.util.events.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.player.PlayerEntity;
import aporia.su.util.events.api.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JumpEvent extends EventCancellable {
    PlayerEntity player;
}
