package zenith.zov.client.screens.menu.settings.impl.popup;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.item.Items;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.api.setting.impl.ItemSelectSetting;
import zenith.zov.client.screens.menu.settings.api.MenuPopupSetting;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.render.display.ScrollHandler;
import zenith.zov.utility.render.display.TextBox;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.ChangeRect;
import zenith.zov.utility.render.display.base.Rect;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

public class MenuItemPopupSetting extends MenuPopupSetting {
    private final TextBox searchBox;
    private final ItemSelectSetting setting;
    private final ScrollHandler scrollHandler = new ScrollHandler();
    private boolean rebornSort = false;
    private Map<Block,Rect> itemBounds = new HashMap<>();
    public MenuItemPopupSetting( ItemSelectSetting setting,ChangeRect bounds) {

        super(bounds);
        searchBox = new TextBox(new Vector2f(0, 0), Fonts.MEDIUM.getFont(7), "Search...", 78);
        animationScale.update(1);
        this.setting = setting;

    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float alphas, Theme theme) {
        animationScale.update();
        alphas = 1;
        float x = bounds.getX();
        float y = bounds.getY();
        float width = bounds.getWidth();
        float height = bounds.getHeight()-20-4;

        ctx.pushMatrix();
        ctx.getMatrices().translate(bounds.getX(),bounds.getY()+bounds.getHeight()/2);
        ctx.getMatrices().scale(animationScale.getValue(),animationScale.getValue());
        ctx.getMatrices().translate(-bounds.getX(),-(bounds.getY()+bounds.getHeight()/2));

        ctx.drawRoundedRect(bounds.getX(),bounds.getY(),bounds.getWidth(),height, BorderRadius.all(4),theme.getForegroundColor().mulAlpha(alphas));
        ctx.drawRoundedRect(bounds.getX(),bounds.getY(),bounds.getWidth(),18, BorderRadius.top(4,4),theme.getForegroundLight().mulAlpha(alphas));
        Font itemFont = Fonts.MEDIUM.getFont(7);
        Font iconFont = Fonts.ICONS.getFont(7);

        ctx.drawText(itemFont,setting.getName(),x+ 8+11.2f+3,y+7.55f,theme.getWhite());
        ctx.pushMatrix();
        ctx.getMatrices().translate(x+8, y+(20-11.2f)/2f);
        ctx.getMatrices().scale(0.7f,0.7f);
        ctx.drawItem(Items.TOTEM_OF_UNDYING.getDefaultStack(), 0, 0);
        ctx.popMatrix();
        float sortSize = 14;
        ctx.drawRoundedRect(x+width-sortSize-8,y+3,sortSize,sortSize,BorderRadius.all(2),theme.getForegroundGray().mulAlpha(alphas));
        ctx.drawText(iconFont,"W", x +width- 8-sortSize+(sortSize-iconFont.width("W"))/2+1, y +6.6f, theme.getColor());

        List<Block> sortedList = searchBox.isEmpty()&& rebornSort ? getAllBlocks().toList():getAllBlocks()
                .sorted((o1, o2) -> {
                    if(searchBox.isEmpty()){
                        boolean containsInSetting1 = this.setting.contains(o1);
                        boolean containsInSetting2 = this.setting.contains(o2);
                        return Boolean.compare(!containsInSetting1, !containsInSetting2);
                    }
                    String query = searchBox.getText().toLowerCase().trim();
                    String name1 = o1.getTranslationKey()
                            .replaceFirst("^block\\.minecraft\\.", "")
                            .replaceAll("_", " ");
                    String name2 = o2.getTranslationKey()
                            .replaceFirst("^block\\.minecraft\\.", "")
                            .replaceAll("_", " ");
                    boolean matchesSearch1 = name1
                            .toLowerCase()
                            .contains(query);
                    boolean matchesSearch2 = name2
                            .toLowerCase()
                            .contains(query);

                    return Boolean.compare(!matchesSearch1, !matchesSearch2);

                })
                .toList();


        float contentHeight = sortedList.size() * 20f;
        scrollHandler.setMax(Math.max(0, contentHeight - height));
        scrollHandler.update();
        int padding = 4;
        float itemY = padding+18+y - (float) scrollHandler.getValue();
        float itemX = x;
        float itemWidth = width;

        ColorRGBA textColor = theme.getWhite().mulAlpha(alphas);
        ColorRGBA bgColor = theme.getColor().mulAlpha(alphas);
        itemBounds.clear();
        ColorRGBA graySlotColor = theme.getForegroundColor();
        ColorRGBA themeSlotColor = theme.getForegroundLight();
        ctx.enableScissor((int) x, (int) y+18+padding, (int) (x + width), (int) (y + height-padding));
        int i = 0;
        for (Block item : sortedList) {

            if (item == Blocks.AIR) continue;
            i++;
            if (itemY < y) {
                itemY += 20;
                continue;
            }
            boolean selected = setting.contains(item);
            Rect rect = new Rect(itemX, itemY, itemWidth, 20);
            ctx.drawRoundedRect(rect.x(), rect.y(), rect.width(), rect.height() , BorderRadius.ZERO,selected?bgColor: i%2==0?graySlotColor:themeSlotColor);

            itemBounds.put(item, rect);
            String name = item.getTranslationKey()
                    .replaceFirst("^block\\.minecraft\\.", "")
                    .replaceAll("_", " ");
            name = name.substring(0, 1).toUpperCase() + name.substring(1);

            ctx.pushMatrix();
            ctx.getMatrices().translate(itemX+8, itemY+(20-11.2f)/2f);
            ctx.getMatrices().scale(0.7f,0.7f);
            ctx.drawItem(item.asItem().getDefaultStack(), 0, 0);
            ctx.popMatrix();
            ctx.drawText(Fonts.BOLD.getFont(8),".", itemX + 8+11.2f+3, itemY +5, theme.getWhiteGray());

            ctx.drawText(itemFont, name, itemX + 8+11.2f+8, itemY+7.55f, selected ? textColor : theme.getGrayLight());

            itemY += 20;

            if (itemY > y + height) {
                break;
            }
        }

        ctx.disableScissor();
        ctx.enableScissor((int)x, (int)(y + height + 4), (int)(x + width), (int)(y + height + 24));
        ctx.drawRoundedRect(x, y + height + 4, width, 20, BorderRadius.all(4), theme.getForegroundColor().mulAlpha(alphas));

        searchBox.setWidth(width-20);
        searchBox.render(ctx,x+8,y+height+4+8,theme.getWhite().mulAlpha(alphas),theme.getGray().mulAlpha(alphas));
        searchBox.setMaxLength(35);
        ctx.disableScissor();
        ctx.popMatrix();

    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return searchBox.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return searchBox.charTyped(chr, modifiers);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        searchBox.onMouseClicked(mouseX, mouseY, button);
        float x = bounds.getX();
        float y = bounds.getY();
        float width = bounds.getWidth();
        float height = bounds.getHeight();
        if(mouseY>y+18){
            for (Map.Entry<Block, Rect> entry : itemBounds.entrySet()) {

                if (entry.getValue().contains(mouseX, mouseY)) {

                    if (setting.contains(entry.getKey())) {
                        setting.remove(entry.getKey());
                    } else {
                        setting.add(entry.getKey());
                    }
                    return;
                }
            }
        }

        if(MathUtil.isHovered(mouseX,mouseY,x+width-8-16,y+3,16,16)){
            rebornSort=!rebornSort;
            scrollHandler.setTargetValue(0);
        }

    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollHandler.scroll(verticalAmount);
        return true;
    }
    @Override
    public float getWidth() {
        return 0;
    }

    @Override
    public float getHeight() {
        return 0;
    }

    @Override
    public boolean isVisible() {
        return true;
    }
    public static Stream<Block> getAllBlocks() {
        return Stream.of(Blocks.class.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> Modifier.isPublic(field.getModifiers()))
                .filter(field -> Block.class.isAssignableFrom(field.getType()))
                .map(field -> {
                    try {
                        return (Block) field.get(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof MenuItemPopupSetting that) {
            return setting == that.setting;
        }
        return false;
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(setting);
    }
}

