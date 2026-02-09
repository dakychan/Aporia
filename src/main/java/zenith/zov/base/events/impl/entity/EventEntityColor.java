package zenith.zov.base.events.impl.entity;

import lombok.*;
import zenith.zov.base.events.callables.EventCancellable;
@Getter
@Setter
@AllArgsConstructor
public class EventEntityColor extends EventCancellable {
    private int color;
}
