package aporia.su.util.user.render.web;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

final class NoOpWebRendererEngine implements WebRendererEngine {

    static final NoOpWebRendererEngine INSTANCE = new NoOpWebRendererEngine();

    private NoOpWebRendererEngine() {
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public WebRendererInstance create(WebRendererConfig config) {
        return new WebRendererInstance() {
            @Override
            public void render(DrawContext drawContext, int x, int y, int width, int height, float tickDelta) {
                drawContext.fill(x, y, x + width, y + height, 0xAA101010);
                drawContext.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                        Text.literal("MCEF not loaded"), x + 6, y + 6, 0xFFDC143C);
                drawContext.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
                        Text.literal("URL: " + config.initialUrl()), x + 6, y + 18, 0xFFD0D0D0);
            }

            @Override public void resize(int width, int height) {}
            @Override public void loadUrl(String url) {}
            @Override public void mouseMove(double x, double y) {}
            @Override public void mouseButton(double x, double y, int button, boolean pressed, int modifiers) {}
            @Override public void scroll(double xDelta, double yDelta) {}
            @Override public void keyEvent(int keyCode, int scanCode, int modifiers, boolean pressed) {}
            @Override public void charTyped(int codePoint) {}
            @Override public void setFocused(boolean focused) {}
            @Override public void close() {}
        };
    }
}
