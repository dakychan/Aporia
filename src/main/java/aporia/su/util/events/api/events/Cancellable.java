package aporia.su.util.events.api.events;

public interface Cancellable {

    boolean isCancelled();

    void cancel();

}