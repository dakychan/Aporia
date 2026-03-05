package aporia.su.util.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import aporia.su.util.events.api.events.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent implements Event {
    byte type;
}
