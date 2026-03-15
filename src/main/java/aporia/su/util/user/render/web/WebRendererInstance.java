package aporia.su.util.user.render.web;

import net.minecraft.client.gui.DrawContext;

public interface WebRendererInstance {

    void render(DrawContext drawContext, int x, int y, int width, int height, float tickDelta);

    void resize(int width, int height);

    void loadUrl(String url);

    void mouseMove(double x, double y);

    void mouseButton(double x, double y, int button, boolean pressed, int modifiers);

    void scroll(double xDelta, double yDelta);

    void keyEvent(int keyCode, int scanCode, int modifiers, boolean pressed);

    void charTyped(int codePoint);

    void setFocused(boolean focused);

    void close();
}
