package aporia.su.util.events.impl.ui;

import aporia.su.util.events.api.events.callables.EventCancellable;

public class ChunkOcclusionEvent extends EventCancellable {
    private static final ChunkOcclusionEvent INSTANCE = new ChunkOcclusionEvent();

    public static ChunkOcclusionEvent get() {
        INSTANCE.setCancelled(false);
        return INSTANCE;
    }
}