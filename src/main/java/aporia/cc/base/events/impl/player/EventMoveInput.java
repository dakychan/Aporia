package aporia.cc.base.events.impl.player;


import net.minecraft.util.PlayerInput;
import aporia.cc.base.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EventMoveInput extends EventCancellable {
    private PlayerInput input;
    private float forward, strafe;
}

