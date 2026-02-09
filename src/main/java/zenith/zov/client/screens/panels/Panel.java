package zenith.zov.client.screens.panels;

import java.util.ArrayList;
import java.util.List;

import zenith.zov.Zenith;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.screens.panels.components.ModuleComponent;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.render.display.ScrollHandler;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.Gradient;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

public class Panel {
    private float x;
    private float y;
    private float width;
    private float height;
    private float contentHeight;
    private final Category category;
    private final List<ModuleComponent> functions = new ArrayList<>();
    private final ScrollHandler scrollHandler = new ScrollHandler();
    private boolean hovered;

    public Panel(Category category) {
        this.category = category;
        for (Module module : Zenith.getInstance().getModuleManager().getModules()) {
            if (module.getCategory() != category) {
                continue;
            }
            functions.add(new ModuleComponent(module));
        }
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public List<ModuleComponent> getFunctions() {
        return functions;
    }

    public void render(UIContext ctx, float mouseX, float mouseY, float alpha) {
        hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        
        scrollHandler.update();
        float scrollOffset = (float) scrollHandler.getValue();
        
        drawPanelBackground(ctx, alpha);
        
        // Включаем scissor для обрезки контента
        ctx.enableScissor((int) x, (int) (y + 28), (int) (x + width), (int) (y + height - 4));
        drawFunctions(ctx, mouseX, mouseY, alpha, scrollOffset);
        ctx.disableScissor();
        
        // Обновляем максимальный скролл
        float maxScroll = Math.max(0, contentHeight - (height - 36));
        scrollHandler.setMax(maxScroll);
    }
    
    public boolean isHovered() {
        return hovered;
    }
    
    public void scroll(double amount) {
        scrollHandler.scroll(amount);
    }

    public void mouseClicked(double mouseX, double mouseY, MouseButton button) {
        for (ModuleComponent functionComponent : functions) {
            functionComponent.mouseClicked(mouseX, mouseY, button);
        }
    }

    public void mouseReleased(double mouseX, double mouseY, MouseButton button) {
        for (ModuleComponent functionComponent : functions) {
            functionComponent.mouseReleased(mouseX, mouseY, button);
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ModuleComponent functionComponent : functions) {
            functionComponent.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public boolean charTyped(char chr, int modifiers) {
        boolean handled = false;
        for (ModuleComponent functionComponent : functions) {
            handled |= functionComponent.charTyped(chr, modifiers);
        }
        return handled;
    }

    private void drawFunctions(UIContext ctx, float mouseX, float mouseY, float alpha, float scrollOffset) {
        float offset = 2f;
        for (ModuleComponent component : functions) {
            component.setX(x + 4.5f);
            component.setY(y + 28 + offset - scrollOffset);
            component.setWidth(width - 9f);
            component.render(ctx, mouseX, mouseY, alpha);
            offset += component.getHeight() + 2f;
        }
        contentHeight = offset;
    }

    private void drawPanelBackground(UIContext ctx, float alpha) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        float offset = 2f;
        for (ModuleComponent component : functions) {
            offset += component.getHeight() + 2f;
        }
        height = offset + 34f;

        // Тень панели
        DrawUtil.drawShadow(ctx.getMatrices(), x - 4, y - 4, width + 8, height + 8, 
                16f, BorderRadius.all(10), new ColorRGBA(0, 0, 0, (int)(60 * alpha)));

        // Основной градиент фона
        Gradient gradient = Gradient.of(
                theme.getForegroundLight().mulAlpha(alpha),
                theme.getForegroundLight().mulAlpha(alpha),
                theme.getForegroundColor().mulAlpha(alpha),
                theme.getForegroundColor().mulAlpha(alpha)
        );
        ctx.drawRoundedRect(x, y, width, height, BorderRadius.all(8), gradient);

        // Заголовок панели
        float headerHeight = 24f;
        Gradient headerGradient = Gradient.of(
                theme.getColor().mulAlpha(alpha * 0.9f),
                theme.getColor().darker(0.2f).mulAlpha(alpha * 0.9f),
                theme.getColor().darker(0.2f).mulAlpha(alpha * 0.9f),
                theme.getColor().mulAlpha(alpha * 0.9f)
        );
        ctx.drawRoundedRect(x, y, width, headerHeight, BorderRadius.top(8, 8), headerGradient);

        // Название категории
        Font font = Fonts.SEMIBOLD.getFont(8f);
        ColorRGBA titleColor = theme.getWhite().mulAlpha(alpha);
        String title = category.getName();
        float titleX = x + width / 2f - font.width(title) / 2f;
        ctx.drawText(font, title, titleX, y + 7, titleColor);

        // Разделительная линия под заголовком
        ctx.drawRoundedRect(x + 8, y + headerHeight, width - 16, 1f, BorderRadius.all(0.5f), 
                theme.getForegroundDark().mulAlpha(alpha * 0.5f));
    }
}
