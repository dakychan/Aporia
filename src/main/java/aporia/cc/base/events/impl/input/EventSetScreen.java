package aporia.cc.base.events.impl.input;

import com.darkmagician6.eventapi.events.Event;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.gui.screen.Screen;

@AllArgsConstructor
@Data
public class EventSetScreen implements Event {
    private Screen screen;
}
