package zenith.zov.base.events.impl.input;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class EventMouseRotation extends EventCancellable {
    float cursorDeltaX, cursorDeltaY;
}
