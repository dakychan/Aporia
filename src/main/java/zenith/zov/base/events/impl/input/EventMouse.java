package zenith.zov.base.events.impl.input;

import com.darkmagician6.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class EventMouse implements Event {
    private final int button, action;
}