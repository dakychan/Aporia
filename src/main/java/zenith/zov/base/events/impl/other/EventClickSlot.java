package zenith.zov.base.events.impl.other;

import com.darkmagician6.eventapi.events.callables.EventCancellable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.screen.slot.SlotActionType;

@Getter
@Setter
@AllArgsConstructor

public class EventClickSlot extends EventCancellable {
    private int windowId, slotId, button;
    private SlotActionType actionType;
}
