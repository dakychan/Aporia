package zenith.zov.base.events.impl.player;

import com.darkmagician6.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventDirection implements Event {
    private float yaw,pitch;
}
