package aporia.su.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import aporia.su.events.api.events.Event;

@Getter
@AllArgsConstructor
public class RotationUpdateEvent implements Event {
    byte type;
}
