package zenith.zov.base.events.impl.player;


import net.minecraft.util.math.Vec3d;
import zenith.zov.base.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;



@AllArgsConstructor
@Getter
@Setter
public class EventMove extends EventCancellable {
    private Vec3d movePos;
}
