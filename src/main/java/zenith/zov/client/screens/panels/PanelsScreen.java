package zenith.zov.client.screens.panels;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.screens.menu.settings.api.MenuPopupSetting;
import zenith.zov.client.screens.panels.components.ModuleComponent;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.game.other.render.CustomScreen;
import zenith.zov.utility.render.display.ScrollHandler;
import zenith.zov.utility.render.display.base.UIContext;

public class PanelsScreen extends CustomScreen {
    private final List<Panel> panels = new ArrayList<>();
    private final PanelsThemePanel themePanel = new PanelsThemePanel();
    private final ScrollHandler scrollHandlerY = new ScrollHandler();
    private final ScrollHandler scrollHandlerX = new ScrollHandler();
    private final Animation closeAnimation = new Animation(300, 0f, Easing.BAKEK_SIZE);
    private final Set<MenuPopupSetting> popupSettings = new HashSet<>();
    private boolean closing;

    public PanelsScreen() {
        for (Category category : Category.values()) {
            if (category == Category.THEMES) {
                continue;
            }
            panels.add(new Panel(category));
        }
    }

    public void renderTop(UIContext ctx, float mouseX, float mouseY) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        float progress = closeAnimation.update(closing ? 0f : 1f);
        progress = Math.min(Math.max(progress, 0f), 1f);

        themePanel.render(ctx, mouseX, mouseY, progress);

        scrollHandlerY.update();
        float scrollOffset = (float) scrollHandlerY.getValue();

        ctx.pushMatrix();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float scale = 0.85f + 0.15f * progress;

        ctx.getMatrices().translate(centerX, centerY);
        ctx.getMatrices().scale(scale, scale);
        ctx.getMatrices().translate(-centerX, -centerY);

        float panelWidth = 105f;
        float panelGap = 8f;
        float totalWidth = panels.size() * panelWidth + (panels.size() - 1) * panelGap;
        float startX = centerX - totalWidth / 2f;
        float baseY = centerY - 100f + scrollOffset;
        float maxHeight = 0f;

        for (int i = 0; i < panels.size(); i++) {
            Panel panel = panels.get(i);
            panel.setWidth(panelWidth);
            panel.setX(startX + i * (panelWidth + panelGap));
            panel.setY(baseY);
            panel.render(ctx, mouseX, mouseY, progress);
            maxHeight = Math.max(maxHeight, panel.getHeight());
        }

        for (Panel panel : panels) {
            for (ModuleComponent component : panel.getFunctions()) {
                component.renderDescription(ctx, mouseX, mouseY, progress);
            }
        }

        List<MenuPopupSetting> removes = new ArrayList<>();
        for (MenuPopupSetting setting : popupSettings) {
            setting.render(ctx, mouseX, mouseY, progress, theme);
            if (setting.getAnimationScale().getValue() == 0f) {
                removes.add(setting);
            }
        }
        popupSettings.removeAll(removes);

        float maxScroll = Math.max(0, maxHeight - (height - 120f));
        scrollHandlerY.setMax(maxScroll);
        ctx.popMatrix();
    }

    public void addPopupSetting(MenuPopupSetting setting) {
        popupSettings.remove(setting);
        popupSettings.add(setting);
    }

    public boolean isFinish() {
        return closing && closeAnimation.getValue() == 0f;
    }

    @Override
    protected void init() {
        closing = false;
        closeAnimation.setValue(0f);
        scrollHandlerY.setTargetValue(0);
        scrollHandlerY.setValue(0);
        super.init();
    }

    @Override
    public void removed() {
        closing = true;
        super.removed();
    }

    @Override
    public void tick() {
        if (closing && closeAnimation.getValue() == 0f) {
            this.close();
        }
    }

    @Override
    public void render(UIContext context, float mouseX, float mouseY) {
        renderTop(context, mouseX, mouseY);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (!popupSettings.isEmpty()) {
            for (MenuPopupSetting setting : popupSettings) {
                if (setting.getBounds().contains(mouseX, mouseY)) {
                    setting.onMouseClicked(mouseX, mouseY, button);
                    return;
                }
                setting.getAnimationScale().update(0);
            }
        }

        if (closing) {
            return;
        }

        themePanel.mouseClicked(mouseX, mouseY, button);
        for (Panel panel : panels) {
            panel.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        for (Panel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, button);
        }
        for (MenuPopupSetting setting : popupSettings) {
            setting.onMouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!popupSettings.isEmpty()) {
            for (MenuPopupSetting setting : popupSettings) {
                if (setting.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                    return true;
                }
            }
        }
        
        // Скролл для панели под курсором
        for (Panel panel : panels) {
            if (panel.isHovered()) {
                panel.scroll(verticalAmount);
                return true;
            }
        }
        
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (MenuPopupSetting setting : popupSettings) {
            if (setting.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        for (Panel panel : panels) {
            panel.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !closing) {
            closing = true;
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        boolean handled = false;
        for (MenuPopupSetting setting : popupSettings) {
            handled |= setting.charTyped(chr, modifiers);
        }
        for (Panel panel : panels) {
            handled |= panel.charTyped(chr, modifiers);
        }
        return handled || super.charTyped(chr, modifiers);
    }
}

