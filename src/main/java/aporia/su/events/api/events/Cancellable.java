package aporia.su.events.api.events;

public interface Cancellable {

    boolean isCancelled();

    void cancel();

}