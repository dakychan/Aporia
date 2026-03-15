package aporia.su.util.user.render.web;

public interface WebRendererEngine {

    boolean isAvailable();

    WebRendererInstance create(WebRendererConfig config);

    default void tick() {
    }

    default void shutdown() {
    }
}
