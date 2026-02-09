package zenith.zov.base.events.impl.input;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;


@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventHotBarScroll extends EventCancellable {
    private double horizontal, vertical;
}
