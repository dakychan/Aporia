package aporia.cc.base.events.impl.render;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import aporia.cc.base.events.callables.EventCancellable;
import aporia.cc.utility.game.player.rotation.Rotation;


@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventCamera extends EventCancellable {
    boolean cameraClip;
    float distance;
    Rotation angle;
}

