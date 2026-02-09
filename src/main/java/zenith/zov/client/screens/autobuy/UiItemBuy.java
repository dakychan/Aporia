package zenith.zov.client.screens.autobuy;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.item.ItemStack;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.api.setting.impl.NumberSetting;
import zenith.zov.client.screens.autobuy.items.AutoInventoryItem;
import zenith.zov.client.screens.autobuy.items.ExtendAutoInventoryItem;
import zenith.zov.client.screens.menu.settings.api.MenuSetting;
import zenith.zov.client.screens.menu.settings.impl.MenuSliderSetting;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.render.display.TextBox;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.Rect;
import zenith.zov.utility.render.display.base.UIContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

import java.util.Locale;

public class UiItemBuy {
    @Getter
    private final AutoInventoryItem itemBuy;
    private final TextBox sumTextBox;
    @Getter
    @Setter
    private Rect bounds;
    private final Animation enableAnimation = new Animation(200, Easing.QUAD_IN_OUT);
    private final MenuSliderSetting menuSliderSetting;
    public UiItemBuy(AutoInventoryItem itemBuy) {
        this.itemBuy = itemBuy;
        sumTextBox = new TextBox(new Vector2f(0, 0), Fonts.MEDIUM.getFont(7), "Сумма", 50);
        sumTextBox.setCharFilter(TextBox.CharFilter.NUMBERS_ONLY);
        sumTextBox.setMaxLength(11);
        sumTextBox.setText(String.valueOf(itemBuy.getMaxSumBuy()));
        sumTextBox.setCursor(sumTextBox.getText().length());
        menuSliderSetting = new MenuSliderSetting(new NumberSetting("Количество",1,1,itemBuy.getItemBuy().getItemStack().getMaxCount(),1,(oldValue, newValue) -> itemBuy.setCountBuy((int) newValue)));
    }
    public void renderSlotBar(CustomDrawContext ctx, float x, float y, float itemSize,BorderRadius borderRadius){
        bounds = new Rect(x+0.1f, y+0.1f, itemSize-0.1f*2, itemSize-0.1f*2);
        renderSlot(ctx,x,y,itemSize,borderRadius);

    }
    private void renderSlot(CustomDrawContext ctx, float x, float y, float itemSize,BorderRadius borderRadius) {
        enableAnimation.update(itemBuy.isSelected());
        Font countFont = Fonts.MEDIUM.getFont(6);
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        ItemStack stack = itemBuy.getItemBuy().getItemStack();

        ColorRGBA slotColor = itemBuy.isSelected() ? theme.getColor() : theme.getForegroundGray();
        float round = 4f;

        ctx.drawRoundedRect(x, y, itemSize, itemSize, borderRadius, slotColor);

        if (!stack.isEmpty()) {
            ctx.pushMatrix();
            ctx.getMatrices().translate(x + (itemSize - 12.8f) / 2f, y + (itemSize - 12.8f) / 2f);
            ctx.getMatrices().scale(0.8f, 0.8f);

            ctx.drawItem(stack, 0, 0);

            ctx.popMatrix();
            if (itemBuy.getCountBuy() > 1) {
                String countText = "x" + String.valueOf((int) (itemBuy.getCountBuy()));
                float countWidth = countFont.width(countText);
                float countX = x + itemSize - countWidth - 0.5f;
                float countY = y + itemSize - countFont.height() - 2;

                ctx.drawText(countFont, countText, countX, countY, theme.getWhite());
            }
        }

    }

    public void renderSettings(UIContext ctx,float mouseX,float mouseY, float x, float y, float itemSize) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        renderSlot(ctx, x, y, itemSize,BorderRadius.all(4));
        Font textFont = Fonts.MEDIUM.getFont(7);
        ctx.drawRoundedRect(x+20+8,y,200-20-8,itemSize,BorderRadius.all(4),theme.getForegroundLight());

        ctx.drawText(textFont,itemBuy.getItemBuy().getDisplayName(),x+20+8+((200-20-8)- textFont.width(itemBuy.getItemBuy().getDisplayName()))/2,y+8,theme.getColor());

        ctx.drawRoundedRect(x+200-65,y+20+8,65,itemSize,BorderRadius.all(4),theme.getForegroundLight());
        sumTextBox.render(ctx,x+200-65+8,y+20+8+8,theme.getColor(),theme.getGray());
        sumTextBox.setWidth(50);
        ctx.drawRoundedRect(x,y+20+8,200-65-8,itemSize,BorderRadius.all(4),theme.getForegroundLight());

        ctx.drawText(textFont,"Купить этот предмет на сумму до:",x+8,y+20+8+8,theme.getWhite());
        menuSliderSetting.render(ctx,mouseX,mouseY,x,y+20+8+20+8,200,1,1,theme.getColor(),theme.getWhite(),theme.getWhiteGray(),theme);

        if(!sumTextBox.isEmpty()){
            String cleanText = sumTextBox.getText()
                    .replaceAll(",", "");
             sumTextBox.setText(String.format(Locale.US,"%,d", Long.parseLong(cleanText)));
            this.itemBuy.setMaxSumBuy(Long.parseLong(cleanText));

        }

        if(itemBuy instanceof ExtendAutoInventoryItem extendAutoInventoryItem){
            System.out.println("YES");

            float settingX = x+200+8+8+8+8;

            float settingY = y;

            float settingWidth = 150;
            ctx.drawRoundedRect(settingX,settingY-8,settingWidth,200,BorderRadius.all(8),theme.getForegroundColor());
            ctx.drawText(Fonts.MEDIUM.getFont(7),"Доп. Настройки",settingX+50,settingY,theme.getWhite());
            settingY+=20;
            for (MenuSetting menuSetting :extendAutoInventoryItem.getEnchants()){
                menuSetting.render(ctx,mouseX,mouseY,settingX,settingY,settingWidth,1,1,theme.getColor(),theme.getWhite(),theme.getWhiteGray(),theme);
                settingY+=menuSetting.getHeight()+16;
            }
        }
    }

    public boolean onMouseClicked(double mouseX, double mouseY, MouseButton button,boolean selected) {

            if(bounds!=null&&bounds.contains(mouseX, mouseY)) {
                if(button == MouseButton.LEFT) {
                    itemBuy.toggleSelected();
                }
                return true;
            }

        if(selected){
            sumTextBox.onMouseClicked(mouseX, mouseY, button);
            menuSliderSetting.onMouseClicked(mouseX, mouseY, button);
            if(itemBuy instanceof ExtendAutoInventoryItem extendAutoInventoryItem){

                for (MenuSetting menuSetting :extendAutoInventoryItem.getEnchants()){
                    menuSetting.onMouseClicked(mouseX, mouseY, button);
                }
            }
        }

        return false;
    }
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        menuSliderSetting.onMouseReleased(mouseX, mouseY, button);
        if(itemBuy instanceof ExtendAutoInventoryItem extendAutoInventoryItem){

            for (MenuSetting menuSetting :extendAutoInventoryItem.getEnchants()){
                menuSetting.onMouseReleased(mouseX, mouseY, button);
            }
        }
    }
    public boolean charTyped(char codePoint, int modifiers) {

        return sumTextBox.charTyped(codePoint, modifiers);
    }


    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return sumTextBox.keyPressed(keyCode, scanCode, modifiers);
    }
    public String getName(){
        return itemBuy.getItemBuy().getDisplayName();
    }
    public void setBounds(float x, float y, float width, float height) {
        bounds = new Rect(x, y, width, height);
    }
}

