package zenith.zov.base.events.impl.render;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import zenith.zov.base.events.callables.EventCancellable;


@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFov extends EventCancellable {
    int fov;
}
