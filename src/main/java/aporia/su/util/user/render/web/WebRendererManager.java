package aporia.su.util.user.render.web;

import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;

public final class WebRendererManager {

    public static final WebRendererManager INSTANCE = new WebRendererManager();

    private volatile WebRendererEngine engine;

    private WebRendererManager() {}

    public void initialize() {
        this.engine = pickEngine();
    }

    public WebRendererInstance create(WebRendererConfig config) {
        ensureInitialized();
        return engine.create(config);
    }

    public void tick() {
        if (engine != null) engine.tick();
    }

    public void shutdown() {
        if (engine != null) engine.shutdown();
    }

    private WebRendererEngine pickEngine() {
        WebRendererEngine candidate = tryEngine(McefWebRendererEngine::new);
        if (candidate != null && !(candidate instanceof NoOpWebRendererEngine)) return candidate;
        return NoOpWebRendererEngine.INSTANCE;
    }

    private WebRendererEngine tryEngine(Supplier<WebRendererEngine> supplier) {
        try {
            WebRendererEngine candidate = supplier.get();
            if (candidate != null && candidate.isAvailable()) return candidate;
        } catch (Throwable ignored) {}
        return null;
    }

    private void ensureInitialized() {
        if (engine == null) {
            synchronized (this) {
                if (engine == null) initialize();
            }
        }
    }

    public boolean hasRealEngine() {
        ensureInitialized();
        return !(engine instanceof NoOpWebRendererEngine);
    }

    public boolean isMcefPresent() {
        return FabricLoader.getInstance().isModLoaded("mcef");
    }
}
