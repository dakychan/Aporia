package zenith.zov.base.events.impl.other;

import com.darkmagician6.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.entity.Entity;


@Getter
@AllArgsConstructor
public class EventSpawnEntity implements Event {
    private Entity entity;

}