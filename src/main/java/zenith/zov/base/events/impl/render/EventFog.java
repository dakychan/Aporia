package zenith.zov.base.events.impl.render;


import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;


@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFog extends EventCancellable {
    float distance;
    int color;
}
