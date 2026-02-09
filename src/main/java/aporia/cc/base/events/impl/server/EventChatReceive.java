package aporia.cc.base.events.impl.server;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.text.Text;
import aporia.cc.base.events.callables.EventCancellable;


@Getter
@Setter
@AllArgsConstructor
public class EventChatReceive extends EventCancellable {

    private Text message;


}

