package zenith.zov.base.events.impl.render;

import com.darkmagician6.eventapi.events.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.UIContext;

@Getter
@RequiredArgsConstructor
public class EventRenderScreen implements Event {

    private final UIContext context;


}