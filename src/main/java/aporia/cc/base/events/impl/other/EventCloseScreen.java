package aporia.cc.base.events.impl.other;

import lombok.AllArgsConstructor;
import net.minecraft.client.gui.screen.Screen;
import aporia.cc.base.events.callables.EventCancellable;
@AllArgsConstructor
public class EventCloseScreen extends EventCancellable {
   private final Screen screen;
}

