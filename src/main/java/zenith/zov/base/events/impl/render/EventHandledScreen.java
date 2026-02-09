package zenith.zov.base.events.impl.render;

import com.darkmagician6.eventapi.events.Event;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;

@Getter
@AllArgsConstructor
public class EventHandledScreen implements Event {
    private final DrawContext drawContext;
    private final Slot slotHover;
    private final int backgroundWidth, backgroundHeight;
}
