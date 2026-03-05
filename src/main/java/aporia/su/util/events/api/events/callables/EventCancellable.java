package aporia.su.util.events.api.events.callables;

import lombok.Setter;
import aporia.su.util.events.api.events.Cancellable;
import aporia.su.util.events.api.events.Event;

public abstract class EventCancellable implements Event, Cancellable {

    @Setter
    private boolean cancelled;

    protected EventCancellable() {
    }


    @Override
    public boolean isCancelled() {
        return cancelled;
    }


    @Override
    public void cancel() {
        cancelled = true;
    }
}