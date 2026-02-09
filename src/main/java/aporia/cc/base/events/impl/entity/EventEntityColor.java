package aporia.cc.base.events.impl.entity;

import lombok.*;
import aporia.cc.base.events.callables.EventCancellable;
@Getter
@Setter
@AllArgsConstructor
public class EventEntityColor extends EventCancellable {
    private int color;
}

