package aporia.su.util.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import aporia.su.util.events.api.events.callables.EventCancellable;

@Getter
@Setter
@AllArgsConstructor
public class UsingItemEvent extends EventCancellable {
    byte type;
}
