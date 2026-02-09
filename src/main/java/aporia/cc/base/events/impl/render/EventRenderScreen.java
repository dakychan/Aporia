package aporia.cc.base.events.impl.render;

import com.darkmagician6.eventapi.events.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import aporia.cc.utility.render.display.base.UIContext;

@Getter
@RequiredArgsConstructor
public class EventRenderScreen implements Event {

    private final UIContext context;


}