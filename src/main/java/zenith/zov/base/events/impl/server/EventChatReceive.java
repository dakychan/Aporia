package zenith.zov.base.events.impl.server;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.component.Component;
import net.minecraft.text.Text;
import zenith.zov.base.events.callables.EventCancellable;
import zenith.zov.utility.render.display.base.CustomDrawContext;


@Getter
@Setter
@AllArgsConstructor
public class EventChatReceive extends EventCancellable {

    private Text message;


}