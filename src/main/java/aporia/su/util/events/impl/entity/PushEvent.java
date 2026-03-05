package aporia.su.util.events.impl.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import aporia.su.util.events.api.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
public class PushEvent extends EventCancellable {
    private Type type;

    public enum Type {
        COLLISION, BLOCK, WATER
    }
}
