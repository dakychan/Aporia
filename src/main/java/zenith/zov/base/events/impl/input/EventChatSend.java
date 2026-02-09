package zenith.zov.base.events.impl.input;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import zenith.zov.base.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
public final class EventChatSend extends EventCancellable {
    @Setter
    private String message;
}