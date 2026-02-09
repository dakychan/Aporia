package zenith.zov.base.events.impl.server;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.packet.Packet;
import zenith.zov.base.events.callables.EventCancellable;

@Getter
@Setter
@AllArgsConstructor
public class EventPacket extends EventCancellable {
    private final Action action;
    private Packet<?> packet;

    public boolean isSent() {
        return this.getAction() == Action.SENT;
    }

    public boolean isReceive() {
        return this.getAction() == Action.RECEIVE;
    }

    public enum Action {
        SENT, RECEIVE
    }
}
