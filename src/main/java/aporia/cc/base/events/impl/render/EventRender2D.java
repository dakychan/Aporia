package aporia.cc.base.events.impl.render;

import com.darkmagician6.eventapi.events.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import aporia.cc.utility.render.display.base.CustomDrawContext;

@Getter
@RequiredArgsConstructor
public class EventRender2D implements Event {

    private final CustomDrawContext context;
    private final float tickDelta;

}

