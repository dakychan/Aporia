package zenith.zov.client.screens.menu;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.impl.render.Interface;
import zenith.zov.client.screens.menu.elements.api.AbstractMenuElement;
import zenith.zov.client.screens.menu.elements.impl.MenuModuleElement;
import zenith.zov.client.screens.menu.elements.impl.MenuThemeElement;
import zenith.zov.client.screens.menu.panels.HeaderPanel;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.screens.menu.panels.SidebarPanel;
import zenith.zov.client.screens.menu.settings.api.MenuPopupSetting;
import zenith.zov.utility.render.display.ScrollHandler;
import zenith.zov.utility.game.other.render.CustomScreen;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.render.display.TextBox;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.*;

public class MenuScreen extends CustomScreen {

    private Category selectedCategory = Category.COMBAT;
    private Category realSelectedCategory = Category.COMBAT;
    private float boxX, boxY;
    @Getter
    @Setter
    private int columns = 1;
    private float boxWidth = 522;
    private float boxHeight = 316;

    private boolean dragging;
    private float dragOffsetX, dragOffsetY;

    private final Animation sidebarAnimation = new Animation(300, 0f, Easing.CUBIC_IN_OUT);
    private boolean isSidebarExpanded;

    private final Animation animationClose = new Animation(300, 0f, Easing.BAKEK_SIZE);
    private boolean initialized;

    private TextBox searchField;

    private final ScrollHandler scrollHandler = new ScrollHandler();

    @Getter
    @Setter
    private boolean closing = false;
    private SidebarPanel sidebarPanel;
    private HeaderPanel headerPanel;

    private int scaledScissorX = 0;
    private int scaledScissorY = 0;
    private int scaledScissorEndX = 2000;
    private int scaledScissorEndY = 2000;

    private final Animation animationColums;
    private final Animation animationScrollHeight;
    private final Animation animationChangeCategory;
    private boolean draggingScrollbar = false;
    private float scrollClickOffset = 0;

    private Set<MenuPopupSetting> popupSettings = new HashSet<>();
    List<AbstractMenuElement> modules = new ArrayList<>();

    public MenuScreen() {
        animationColums = new Animation(300, columns == 3 ? 1 : 0, Easing.CUBIC_IN_OUT);
        animationChangeCategory = new Animation(150, 1, Easing.CUBIC_IN_OUT);
        animationScrollHeight = new Animation(150, 1, Easing.QUAD_IN_OUT);
    }

    public void initialize() {
        modules.addAll(Zenith.getInstance().getModuleManager().getModules().stream().map(MenuModuleElement::new).toList());
        modules.add(new MenuThemeElement(Theme.DARK));
        modules.add(new MenuThemeElement(Theme.LIGHT));
        modules.add(new MenuThemeElement(Theme.CUSTOM_THEME));

    }

    @Override
    protected void init() {
        closing = false;
        animationColums.setValue(columns == 3 ? 1 : 0);

        boxWidth = MathHelper.lerp(animationColums.getValue(), 461 + 4, 525 + 8);

        boxHeight = MathHelper.lerp(animationColums.getValue(), 282, 320);

        boxX = (this.width - boxWidth) / 2f;
        boxY = (this.height - boxHeight) / 2f;
        animationClose.setValue(0f);
        animationClose.update(1f);
        if (!initialized) {
            this.searchField = new TextBox(new Vector2f(boxX + boxWidth - 128 - 8, boxY + 8), Fonts.MEDIUM.getFont(7), "Search", 100);
            this.sidebarPanel = new SidebarPanel(
                    this.sidebarAnimation,
                    this.isSidebarExpanded,
                    category -> {

                        this.headerPanel.resetAnim(realSelectedCategory, category);
                        realSelectedCategory = category;

                        this.scrollHandler.setTargetValue(0.0F);
                        this.searchField.setSelectAll(true);
                        this.searchField.setSelected(true);
                        this.searchField.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, 0);
                        this.searchField.setSelected(false);
                    },
                    () -> {
                        this.isSidebarExpanded = !this.isSidebarExpanded;
                        this.sidebarAnimation.animateTo(this.isSidebarExpanded ? 1f : 0f);
                    }
            );

            this.headerPanel = new HeaderPanel(
                    this.searchField,
                    () -> this.columns = (this.columns % 3) + 1,
                    () -> Zenith.getInstance().getThemeManager().switchTheme()
            );
        }
        initialized = true;


    }

    @Override
    public void tick() {

        if (closing && animationClose.getValue() == 0.0F) {
            this.close();
        }
        super.tick();
    }

    @Override
    public void removed() {
        this.closing = true;
        super.removed();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        //  super.renderBackground(context, mouseX, mouseY, delta);
    }

    public boolean isFinish() {
        return animationClose.getValue() == 0.0F && closing;
    }

    public void renderTop(UIContext ctx, float mouseX, float mouseY) {
        if (!initialized) return;

        animationColums.update(columns == 3 ? 1 : 0);

        boxWidth = MathHelper.lerp(animationColums.getValue(), 461 + 4, 525 + 8);

        boxHeight = MathHelper.lerp(animationColums.getValue(), 282, 320);

        float progress = animationClose.update(closing ? 0.0F : 1.0F);

        progress = Math.min(Math.max(progress, 0), 1);
        float sidebarProgress = sidebarAnimation.update();
        float scale = 0.85f + 0.15f * progress;
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Matrix3x2fStack matrices = ctx.getMatrices();

        ctx.pushMatrix();

        float scaleX = boxX + boxWidth / 2f;
        float scaleY = boxY + boxHeight / 2f;

        matrices.translate(scaleX, scaleY);
        matrices.scale(scale, scale);
        matrices.translate(-scaleX, -scaleY);

        ColorRGBA primary = theme.getColor().mulAlpha(progress);
        ColorRGBA baseBg = theme.getBackgroundColor().mulAlpha(progress * 4); // Фон всей гуишки
        ColorRGBA selectedColor = theme.getWhite().mulAlpha(progress); // Цвет выбранного элемента и текста
        ColorRGBA textColor = theme.getWhite().mulAlpha(progress); // Цвет обычного текста

        // ФОН
        if (Interface.INSTANCE.isBlur() ) {
            DrawUtil.drawBlur(ctx.getMatrices(), boxX, boxY, boxWidth, boxHeight, 20 * progress * progress, BorderRadius.all(9), ColorRGBA.WHITE.mulAlpha(progress*2));
        }
        ctx.drawRoundedRect(boxX, boxY, boxWidth, boxHeight, BorderRadius.all(9), baseBg);
        float widthScroll = 2;
        // СЛАЙДБАР
        sidebarPanel.render(ctx, boxX, boxY, boxHeight, progress, theme, realSelectedCategory, primary, textColor, selectedColor);

        //НАХОЖДЕНИЕ
        float sidebarWidth = 30f + (88f - 30f) * sidebarProgress;
        float contentStartX = boxX + 8f + sidebarWidth + 8f;
        float sidebarY = boxY + 8f;
        float contentY = boxY + 22f + 8f + 8f;

        headerPanel.render(ctx, contentStartX, sidebarY, boxX, columns, boxWidth, progress, theme, realSelectedCategory);
        //рендер скролла
        float visibleHeight = boxHeight - (22f + 8f + 8f + 8f);
        float scrollProgress = scrollHandler.getMax() == 0 ? 0f : (float) (scrollHandler.getValue() / scrollHandler.getMax());
        float scrollHeight = Math.max(visibleHeight * (visibleHeight / (float) (visibleHeight + scrollHandler.getMax())), 20);
        scrollHeight = Math.min(visibleHeight, animationScrollHeight.update(scrollHeight));
        float denom = Math.max(1f, (visibleHeight - scrollHeight));
        float scrollY = contentY +denom * scrollProgress;
        scrollY = Math.min(contentY + visibleHeight, scrollY);
        ctx.drawRoundedRect(boxX + boxWidth - 8 - widthScroll, contentY, widthScroll, visibleHeight, BorderRadius.all(0.5f), theme.getForegroundColor().mulAlpha(progress));
        if(scrollY+scrollHeight>visibleHeight+contentY) {
            ctx.drawRoundedRect(boxX + boxWidth - 8 - widthScroll, contentY, widthScroll, (visibleHeight), BorderRadius.all(1f), theme.getForegroundStroke().mulAlpha(progress));

        }else ctx.drawRoundedRect(boxX + boxWidth - 8 - widthScroll, scrollY, widthScroll, (scrollHeight), BorderRadius.all(1f), theme.getForegroundStroke().mulAlpha(progress));


        //Рендер модулей
        float contentWidth = boxX + (columns == 3 ? 530 : 461) - contentStartX - 8f;


        this.scaledScissorX = (int) contentStartX;
        this.scaledScissorY = (int) ((int) boxY + (22f + 8f + 8f));
        this.scaledScissorEndX = (int) (boxX + boxWidth);
        this.scaledScissorEndY = (int) ((int) boxY + boxHeight);

        //-ScissorUtility.startScissor(scaledScissorX, scaledScissorY, scaledScissorWidth, scaledScissorHeight);

        ctx.enableScissor(this.scaledScissorX, this.scaledScissorY, this.scaledScissorEndX, this.scaledScissorEndY);
        animationChangeCategory.setEasing(Easing.QUAD_IN_OUT);

        renderModules(ctx, mouseX, mouseY, progress * animationChangeCategory.update(selectedCategory == realSelectedCategory ? 1 : 0), (int) contentStartX, contentWidth, (int) contentY);
        ctx.disableScissor();
        List<MenuPopupSetting> removes = new ArrayList<>();
        for (MenuPopupSetting setting : popupSettings) {

            setting.render(ctx, mouseX, mouseY, progress, theme);
            if (setting.getAnimationScale().getValue() == 0) {
                removes.add(setting);
            }
        }
        popupSettings.removeAll(removes);


        if (animationChangeCategory.getValue() == 0) {
            selectedCategory = realSelectedCategory;
        }
        //ScissorUtility.stopScissor();

        if (draggingScrollbar) {
            float scrollbarY = boxY + 22f + 8f + 8f;
            float newY = (float) mouseY - scrollbarY - scrollClickOffset;

            float scrollRatio = newY / denom;
            scrollHandler.setTargetValue(-(scrollRatio * scrollHandler.getMax()));

        }
        ctx.popMatrix();


    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {

        if (!popupSettings.isEmpty()) {
            for (MenuPopupSetting setting : popupSettings) {
                if (setting.getBounds().contains(mouseX, mouseY)) {
                    setting.onMouseClicked(mouseX, mouseY, button);
                    return;
                } else {
                    setting.getAnimationScale().update(0);

                }
            }
        }
        if (isClosing()) return;
        if (headerPanel.handleMouseClicked(mouseX, mouseY)) {
            if (headerPanel.searchBarBounds.contains(mouseX, mouseY)) {
                searchField.setSelected(true);
                ;
            }
            return;
        }

        if (sidebarPanel.handleMouseClicked(mouseX, mouseY)) {
            return;
        }

        if (searchField.isSelected()) {
            searchField.setSelected(false);
        }
        if (button.getButtonIndex() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (MathUtil.isHovered(mouseX, mouseY, boxX, boxY, boxWidth, 20)) {
                dragging = true;
                dragOffsetX = (float) mouseX - boxX;
                dragOffsetY = (float) mouseY - boxY;
                return;
            }
        }
        //чтобы все что обрезанно не нажималось
        if (!animationClose.isDone()) return;
        float scrollbarX = boxX + boxWidth - 8 - 2;
        float scrollbarY = boxY + 22f + 8f + 8f;
        float visibleHeight = boxHeight - (22f + 8f + 8f);
        if (button.getButtonIndex() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (MathUtil.isHovered(mouseX, mouseY, scrollbarX, scrollbarY, 2, visibleHeight)) {
                draggingScrollbar = true;

                return;
            }
        }
        if (!MathUtil.isHoveredByCords(mouseX, mouseY, scaledScissorX, scaledScissorY, scaledScissorEndX, scaledScissorEndY)) {
            return;
        }

        this.modules.stream()
                .filter(m -> searchField.isEmpty() ? m.getCategory() == selectedCategory : m.getName().toLowerCase().contains(searchField.getText().toLowerCase())).forEach(menuModule -> menuModule.onMouseClicked(mouseX, mouseY, button));


        super.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchField.isSelected()) {
            return searchField.charTyped(chr, modifiers);
        }
        for (MenuPopupSetting setting : popupSettings) {
            setting.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        for (MenuPopupSetting setting : popupSettings) {
            setting.onMouseReleased(mouseX, mouseY, button);
        }


        if (button.getButtonIndex() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = false;
            draggingScrollbar = false;
        }
        for (AbstractMenuElement module : modules) {
            module.onMouseReleased(mouseX, mouseY, button);
        }
        super.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean returnCheck = false;
        ;
        for (MenuPopupSetting setting : popupSettings) {
            if (setting.keyPressed(keyCode, scanCode, modifiers)) {
                searchField.setSelected(false);
                returnCheck = true;
            }
            ;
        }
        if (returnCheck) {
            return true;
        }
        //чтобы при отмене бинда или закрытия СЕРЧА ГУи -не закрывалась
        if (searchField.isSelected()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                searchField.setSelected(false);
                return true;
            }
            return searchField.keyPressed(keyCode, scanCode, modifiers);
        }
        boolean result = false;
        for (AbstractMenuElement module : modules) {
            if (module.keyPressed(keyCode, scanCode, modifiers)) {
                result = true;
            }
        }
        if (result) {
            return true;
        }
        // pfrhsnbt vty.irb
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !closing) {
            onMouseReleased(0, 0, MouseButton.LEFT);
            onMouseReleased(0, 0, MouseButton.RIGHT);
            onMouseReleased(0, 0, MouseButton.MIDDLE);
            for (MenuPopupSetting setting : popupSettings) {
                setting.getAnimationScale().setTargetValue(0);
            }
            closing = true;
        }


        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!popupSettings.isEmpty()) {
            for (MenuPopupSetting setting : popupSettings) {
                setting.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            }
            return true;
        }
        float visibleHeight = boxHeight - (22f + 8f + 8f);

        float baseStep = (float) Math.max(20f, Math.min(60f, (scrollHandler.getMax() / visibleHeight) * 10));


        scrollHandler.scroll(verticalAmount * baseStep / 8);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onMouseDragged(double mouseX, double mouseY, MouseButton button, double deltaX, double deltaY) {

        if (button.getButtonIndex() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (dragging) {
                boxX = (float) mouseX - dragOffsetX;
                boxY = (float) mouseY - dragOffsetY;
                return;
            }


        }
        for (AbstractMenuElement module : modules) {
            module.onMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        super.onMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void close() {
        ;
        super.close();
    }


    private void renderModules(UIContext ctx, float mouseX, float mouseY, float alpha, float contentStartX, float contentWidth, float startY) {

        List<AbstractMenuElement> modules = this.modules.stream()
                .filter(m -> searchField.isEmpty() ? m.getCategory() == selectedCategory : m.getName().toLowerCase().contains(searchField.getText().toLowerCase()))
                .sorted(Comparator.comparing(menuModule -> menuModule.getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();


        int columns = this.columns;
        float padding = 6f;
        float scrollbarWidth = 6f;
        float maxContentWidth = contentWidth - scrollbarWidth;
        float moduleWidth = (maxContentWidth - padding * (columns - 1)) / columns;

        Font font = Fonts.MEDIUM.getFont(7);
        double[] columnHeights = new double[columns];


        for (AbstractMenuElement module : modules) {
            int col = 0;
            for (int j = 1; j < columns; j++) {
                if (columnHeights[j] < columnHeights[col]) {
                    col = j;
                }
            }


            float x = contentStartX + col * (moduleWidth + padding);

            float y = (float) (startY + columnHeights[col] - scrollHandler.getValue());


            module.render(ctx, mouseX, mouseY, font, x, y, moduleWidth, alpha, col);

            columnHeights[col] += module.getHeight() + padding;

        }

        scrollHandler.update();
        double maxY = Arrays.stream(columnHeights).max().orElse(0);


        float visibleHeight = boxHeight - (22f + 8f + 8f);

        scrollHandler.setMax(Math.max(0, maxY - visibleHeight) + (maxY > visibleHeight ? 4 : 0));

    }

    public void addPopupMenuSetting(MenuPopupSetting setting) {
        popupSettings.add(setting);
    }

    public void removePopupMenuSetting(MenuPopupSetting setting) {
        popupSettings.remove(setting);
    }

    @Override
    public void render(UIContext context, float mouseX, float mouseY) {
        renderTop(context, mouseX, mouseY);
    }

}
