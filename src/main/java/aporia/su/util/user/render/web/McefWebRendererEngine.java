package aporia.su.util.user.render.web;

import java.lang.reflect.Method;
import java.util.Optional;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class McefWebRendererEngine implements WebRendererEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger("Aporia-WebRenderer");

    private final Variant variant;
    private final Method createBrowserStatic;
    private final boolean hasCinemamodBrowser;
    private final boolean hasCinemamodRenderer;
    private final Object legacyApi;
    private final Method legacyCreateBrowser;
    private final Method legacyTick;

    McefWebRendererEngine() {
        if (!FabricLoader.getInstance().isModLoaded("mcef")) {
            throw new IllegalStateException("MCEF mod is not loaded");
        }
        Optional<Class<?>> modernEntry = ReflectionSupport.findFirstClass(
                "com.cinemamod.mcef.MCEF",
                "net.ccbluex.mcef.MCEF");
        if (modernEntry.isPresent()) {
            variant = Variant.CINEMAMOD;
            hasCinemamodBrowser = ReflectionSupport.findFirstClass(
                    "com.cinemamod.mcef.MCEFBrowser",
                    "net.ccbluex.mcef.MCEFBrowser").isPresent();
            hasCinemamodRenderer = ReflectionSupport.findFirstClass(
                    "com.cinemamod.mcef.MCEFRenderer",
                    "net.ccbluex.mcef.MCEFRenderer").isPresent();
            createBrowserStatic = ReflectionSupport.findMethod(modernEntry.get(), "createBrowser", String.class, boolean.class)
                    .orElseGet(() -> ReflectionSupport.findMethod(modernEntry.get(), "createBrowser", String.class, boolean.class, int.class, int.class)
                            .orElse(null));
            legacyApi = null; legacyCreateBrowser = null; legacyTick = null;
            return;
        }
        Optional<Class<?>> legacyProxy = ReflectionSupport.findFirstClass("net.montoyo.mcef.client.ClientProxy", "net.montoyo.mcef.MCEF");
        if (legacyProxy.isPresent()) {
            variant = Variant.LEGACY;
            Object apiCandidate = null; Method createCandidate = null; Method tickCandidate = null;
            try {
                Method getInstance = legacyProxy.get().getMethod("getInstance");
                Object proxyInstance = getInstance.invoke(null);
                Method getApi = proxyInstance.getClass().getMethod("getAPI");
                apiCandidate = getApi.invoke(proxyInstance);
                if (apiCandidate != null) {
                    Class<?> apiClass = apiCandidate.getClass();
                    createCandidate = ReflectionSupport.findMethod(apiClass, "createBrowser", String.class, boolean.class).orElse(null);
                    if (createCandidate == null) createCandidate = ReflectionSupport.findMethodByParams(apiClass, "createBrowser", 1, 4).orElse(null);
                    tickCandidate = ReflectionSupport.findMethod(apiClass, "onTick", int.class).orElse(null);
                }
            } catch (ReflectiveOperationException ex) { LOGGER.warn("Failed to init legacy MCEF API", ex); }
            legacyApi = apiCandidate; legacyCreateBrowser = createCandidate; legacyTick = tickCandidate;
            createBrowserStatic = null; hasCinemamodBrowser = false; hasCinemamodRenderer = false;
            return;
        }
        variant = Variant.NONE;
        createBrowserStatic = null; hasCinemamodBrowser = false; hasCinemamodRenderer = false;
        legacyApi = null; legacyCreateBrowser = null; legacyTick = null;
    }

    @Override
    public boolean isAvailable() {
        return switch (variant) {
            case CINEMAMOD -> createBrowserStatic != null && hasCinemamodBrowser && hasCinemamodRenderer;
            case LEGACY -> legacyApi != null && legacyCreateBrowser != null;
            default -> false;
        };
    }

    @Override
    public WebRendererInstance create(WebRendererConfig config) {
        return switch (variant) {
            case CINEMAMOD -> createCinemamodInstance(config);
            default -> NoOpWebRendererEngine.INSTANCE.create(config);
        };
    }

    @Override
    public void tick() {
        if (variant == Variant.LEGACY && legacyApi != null && legacyTick != null) {
            try { legacyTick.invoke(legacyApi, 0); } catch (ReflectiveOperationException ex) { LOGGER.debug("Legacy MCEF tick failed", ex); }
        }
    }

    private WebRendererInstance createCinemamodInstance(WebRendererConfig config) {
        if (createBrowserStatic == null) return NoOpWebRendererEngine.INSTANCE.create(config);
        Object browser = null;
        try {
            if (createBrowserStatic.getParameterCount() == 2) {
                browser = createBrowserStatic.invoke(null, config.initialUrl(), config.transparent());
            } else if (createBrowserStatic.getParameterCount() >= 4) {
                browser = createBrowserStatic.invoke(null, config.initialUrl(), config.transparent(), 0, 0);
            }
        } catch (ReflectiveOperationException ex) { LOGGER.error("Failed to create MCEF browser", ex); }
        if (browser == null) return NoOpWebRendererEngine.INSTANCE.create(config);
        return new CinemamodBrowserInstance(browser, config);
    }

    private enum Variant { CINEMAMOD, LEGACY, NONE }

    private static final class CinemamodBrowserInstance implements WebRendererInstance {
        private final Object browser;
        private final WebRendererConfig config;
        private final Method resize, loadUrl, close, focus;
        private final Method mouseMove, mousePress, mouseRelease, mouseWheel;
        private final Method keyPress, keyRelease, keyTyped;
        private final Object renderer;
        private final Method rendererTextureId;
        private int lastWidth = -1, lastHeight = -1;

        private CinemamodBrowserInstance(Object browser, WebRendererConfig config) {
            this.browser = browser; this.config = config;
            Class<?> bc = browser.getClass();
            this.resize = ReflectionSupport.findMethod(bc, "resize", int.class, int.class).orElse(null);
            this.loadUrl = ReflectionSupport.findMethod(bc, "loadURL", String.class)
                    .orElseGet(() -> ReflectionSupport.findMethod(bc, "loadUrl", String.class).orElse(null));
            this.close = ReflectionSupport.findMethodByParams(bc, "close", 0, 0).orElse(null);
            this.focus = ReflectionSupport.findMethod(bc, "setFocus", boolean.class).orElse(null);
            this.mouseMove = ReflectionSupport.findMethod(bc, "sendMouseMove", int.class, int.class).orElse(null);
            this.mousePress = ReflectionSupport.findMethod(bc, "sendMousePress", int.class, int.class, int.class).orElse(null);
            this.mouseRelease = ReflectionSupport.findMethod(bc, "sendMouseRelease", int.class, int.class, int.class).orElse(null);
            this.mouseWheel = ReflectionSupport.findMethod(bc, "sendMouseWheel", int.class, int.class, double.class, int.class).orElse(null);
            this.keyPress = ReflectionSupport.findMethod(bc, "sendKeyPress", int.class, long.class, int.class).orElse(null);
            this.keyRelease = ReflectionSupport.findMethod(bc, "sendKeyRelease", int.class, long.class, int.class).orElse(null);
            this.keyTyped = ReflectionSupport.findMethod(bc, "sendKeyTyped", char.class, int.class).orElse(null);
            Object rendererHandle = null; Method textureMethod = null;
            Method getRenderer = ReflectionSupport.findMethodByParams(bc, "getRenderer", 0, 0).orElse(null);
            if (getRenderer != null) {
                try { rendererHandle = getRenderer.invoke(browser); } catch (ReflectiveOperationException ignored) {}
            }
            if (rendererHandle != null) {
                textureMethod = ReflectionSupport.findMethodByParams(rendererHandle.getClass(), "getTextureID", 0, 0).orElse(null);
                Method init = ReflectionSupport.findMethodByParams(rendererHandle.getClass(), "initialize", 0, 0).orElse(null);
                if (init != null) { try { init.invoke(rendererHandle); } catch (ReflectiveOperationException ignored) {} }
            }
            this.renderer = rendererHandle; this.rendererTextureId = textureMethod;
            if (this.loadUrl != null) { try { this.loadUrl.invoke(browser, config.initialUrl()); } catch (ReflectiveOperationException ignored) {} }
        }

        @Override
        public void render(DrawContext drawContext, int x, int y, int width, int height, float tickDelta) {
            if (renderer == null || rendererTextureId == null) {
                NoOpWebRendererEngine.INSTANCE.create(config).render(drawContext, x, y, width, height, tickDelta);
                return;
            }
            ensureSize(width, height);
            int textureId = 0;
            try {
                Object tex = rendererTextureId.invoke(renderer);
                if (tex instanceof Number n) textureId = n.intValue();
            } catch (ReflectiveOperationException ignored) {}
            if (textureId == 0) return;

            aporia.su.util.user.render.Render2D.drawFramebufferTexture(textureId, x, y, width, height, 1f, 1f, 1f, 1f);
        }

        @Override public void resize(int w, int h) { ensureSize(w, h); }
        @Override public void loadUrl(String url) { if (loadUrl == null) return; try { loadUrl.invoke(browser, url); } catch (ReflectiveOperationException ignored) {} }
        @Override public void mouseMove(double x, double y) { if (mouseMove == null) return; try { mouseMove.invoke(browser, scale(x), scale(y)); } catch (ReflectiveOperationException ignored) {} }
        @Override public void mouseButton(double x, double y, int button, boolean pressed, int modifiers) {
            Method t = pressed ? mousePress : mouseRelease; if (t == null) return;
            try { t.invoke(browser, scale(x), scale(y), button); } catch (ReflectiveOperationException ignored) {}
        }
        @Override public void scroll(double xDelta, double yDelta) { if (mouseWheel == null) return; try { mouseWheel.invoke(browser, scale(0), scale(0), -yDelta, 0); } catch (ReflectiveOperationException ignored) {} }
        @Override public void keyEvent(int keyCode, int scanCode, int modifiers, boolean pressed) {
            Method t = pressed ? keyPress : keyRelease; if (t == null) return;
            try { t.invoke(browser, keyCode, (long) scanCode, modifiers); } catch (ReflectiveOperationException ignored) {}
        }
        @Override public void charTyped(int codePoint) { if (keyTyped == null) return; try { keyTyped.invoke(browser, (char) codePoint, 0); } catch (ReflectiveOperationException ignored) {} }
        @Override public void setFocused(boolean focused) { if (focus == null) return; try { focus.invoke(browser, focused); } catch (ReflectiveOperationException ignored) {} }
        @Override public void close() { if (close == null) return; try { close.invoke(browser); } catch (ReflectiveOperationException ignored) {} }

        private void ensureSize(int w, int h) {
            if (resize == null || w <= 0 || h <= 0 || (w == lastWidth && h == lastHeight)) return;
            lastWidth = w; lastHeight = h;
            try { resize.invoke(browser, scale(w), scale(h)); } catch (ReflectiveOperationException ignored) {}
        }
        private int scale(double v) { return Math.max(1, (int) Math.round(v * MinecraftClient.getInstance().getWindow().getScaleFactor())); }
        private int scale(int v) { return scale((double) v); }
    }
}
