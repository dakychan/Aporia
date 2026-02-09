package zenith.zov.base.events.impl.player;

import com.darkmagician6.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.entity.Entity;

@Data

@AllArgsConstructor
public class EventEntityHitBox implements Event {
    private Entity entity;
    private float size;
}
