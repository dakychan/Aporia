package zenith.zov.client.screens.autobuy.items;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.item.Items;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.autobuy.item.ItemBuy;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.modules.impl.misc.AutoSbor;
import zenith.zov.client.screens.autobuy.UiItemBuy;
import zenith.zov.utility.game.other.render.CustomScreen;
import zenith.zov.utility.game.other.MouseButton;
import zenith.zov.utility.render.display.ScrollHandler;
import zenith.zov.utility.render.display.TextBox;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.UIContext;

import java.util.ArrayList;
import java.util.List;

public class AutoInventoryBuyScreen extends CustomScreen {

    private final Animation animationScale = new Animation(300, Easing.QUAD_IN_OUT);


    private final TextBox searchBox = new TextBox(new Vector2f(0, 0),
            zenith.zov.base.font.Fonts.MEDIUM.getFont(7), "Search...", 200);


    private final List<UiItemBuy> libraryItems = new ArrayList<>();

    private final List<UiItemBuy> inventoryItems = new ArrayList<>();


    private UiItemBuy draggedItem = null;
    private boolean draggedFromLibrary = false;
    private float dragOffsetX, dragOffsetY;

    private final ScrollHandler scrollHandler = new ScrollHandler();
    private boolean draggingScrollbar = false;
    private float scrollClickOffset = 0f;

    private UiItemBuy currentUiItemBuy = null;

    public AutoInventoryBuyScreen(List<ItemBuy> lib, List<AutoInventoryItem> items) {

        lib
                .forEach(itemBuy -> libraryItems.add(new UiItemBuy(createItem(itemBuy))));
        items
                .forEach(itemBuy -> inventoryItems.add(new UiItemBuy(itemBuy)));
        this.currentUiItemBuy = inventoryItems.stream().filter(e->e.getItemBuy().isSelected()).findAny().orElse(null);
    }

    public static AutoInventoryItem createItem(ItemBuy itemBuy) {
        if(itemBuy.getCategory()== ItemBuy.Category.ANY &&itemBuy.getItemStack().getItem()== Items.ELYTRA){
            return new AutoInventoryElytra(itemBuy);
        }else {
            return new AutoInventoryItem(itemBuy);
        }
    }
    @Override
    public void render(UIContext ctx, float mouseX, float mouseY) {
        scrollHandler.update();
        float sizeItem = 20;
        float widthLibrary = 8 + (sizeItem + 4) * 4 + 8-4;
        float widthSettings = (9 * (sizeItem + 4)) + 8;
        float totalWidth = widthLibrary + widthSettings;
        float height = 200;
        float x = (mc.getWindow().getScaledWidth() - totalWidth) / 2;
        float y = (mc.getWindow().getScaledHeight() - height) / 2;

        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        ctx.drawRoundedRect(x, y, totalWidth, height, BorderRadius.all(8), theme.getBackgroundColor());


        ctx.drawRoundedRect(x + 8, y + 8, (sizeItem + 4) * 4 -4, 20, BorderRadius.all(4), theme.getForegroundLight());

        searchBox.render(ctx, x + 8 + 8, y + 8 + 8, theme.getWhite(), theme.getGray());
        searchBox.setWidth(sizeItem * 4);
        searchBox.setMaxLength(15);

        String query = searchBox.getText().toLowerCase();
        List<UiItemBuy> filteredLibrary = libraryItems.stream()
                .filter(item -> item.getItemBuy().getItemBuy().getSearchName().toLowerCase().contains(query))
                .toList();

        renderLibrary(ctx, filteredLibrary, x + 8, y + 8 + 8 + 20, sizeItem);

        renderInventory(ctx, x + widthLibrary, y + height - 8 - 4 * (sizeItem + 4), sizeItem);


        if (currentUiItemBuy != null) {
            currentUiItemBuy.renderSettings(ctx, mouseX, mouseY, x + widthLibrary, y + 8, sizeItem);
        }


        if (draggedItem != null) {
            draggedItem.renderSlotBar(ctx, mouseX - dragOffsetX, mouseY - dragOffsetY, sizeItem, BorderRadius.all(4));
        }

    }

    private void renderLibrary(UIContext ctx, List<UiItemBuy> items, float startX, float startY, float sizeItem) {
        int totalRows = (int) Math.ceil(items.size() / 4.0);


        float rowHeight = sizeItem + 4;


        float contentHeight = totalRows * rowHeight;
        scrollHandler.setMax(Math.max(0, contentHeight - 160));
        float scrollOffset = (float) scrollHandler.getValue();

        ctx.enableScissor((int) startX, (int) startY, (int) (startX + (sizeItem + 4) * 4), (int) (startY + 165));
        int col = 0, row = 0;
        for (UiItemBuy itemBuy : items) {
            float slotX = startX + col * (sizeItem + 4);
            float slotY = startY + row * (sizeItem + 4) - scrollOffset;

            itemBuy.setBounds(slotX, slotY, sizeItem, sizeItem);
            if (itemBuy != draggedItem || !draggedFromLibrary) {
                itemBuy.renderSlotBar(ctx, slotX, slotY, sizeItem, BorderRadius.all(4));
            }
            if (++col >= 4) {
                col = 0;
                row++;
            }
        }
        ctx.disableScissor();
    }

    private void renderInventory(UIContext ctx, float startX, float startY, float sizeItem) {
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        int col = 0, row = 0;

        for (int slotIndex = 0; slotIndex < 9 * 4; slotIndex++) {
            float slotX = startX + col * (sizeItem + 4);
            float slotY = startY + row * (sizeItem + 4);

            ctx.drawRoundedRect(slotX, slotY, sizeItem, sizeItem, BorderRadius.all(4), theme.getForegroundGray());

            for (UiItemBuy itemBuy : inventoryItems) {
                if (itemBuy.getItemBuy().getSlotId() == (slotIndex) && itemBuy != draggedItem) {
                    itemBuy.setBounds(slotX, slotY, sizeItem, sizeItem);
                    itemBuy.renderSlotBar(ctx, slotX, slotY, sizeItem, BorderRadius.all(4));
                }
            }

            if (++col >= 9) {
                col = 0;
                row++;
            }
        }
    }

    private boolean isSlotOccupied(int slotIndex) {
        for (UiItemBuy item : inventoryItems) {
            if (item.getItemBuy().getSlotId() == (slotIndex)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {

        searchBox.onMouseClicked(mouseX, mouseY, button);
        float y = (mc.getWindow().getScaledHeight() - 200) / 2f;
        if(mouseY>y+ 8 + 8 + 20 && mouseY <y + 8 + 8 + 20+160){
            List<UiItemBuy> filteredLibrary = libraryItems.stream()
                    .filter(item -> item.getItemBuy().getItemBuy().getSearchName().toLowerCase().contains(searchBox.getText().toLowerCase()))
                    .toList();

            for (UiItemBuy itemBuy : filteredLibrary) {
                if (itemBuy.getBounds() != null && itemBuy.getBounds().contains(mouseX, mouseY)) {
                    if (button == MouseButton.LEFT) {
                        draggedItem = itemBuy;
                        draggedFromLibrary = true;
                        dragOffsetX = (float) mouseX - itemBuy.getBounds().x();
                        dragOffsetY = (float) mouseY - itemBuy.getBounds().y();
                        return;
                    }
                }
            }
        }
        for (UiItemBuy itemBuy : inventoryItems) {
            if (itemBuy.getBounds() != null && itemBuy.getBounds().contains(mouseX, mouseY)) {
                if (button == MouseButton.LEFT) {
                    draggedItem = itemBuy;
                    draggedFromLibrary = false;
                    dragOffsetX = (float) mouseX - itemBuy.getBounds().x();
                    dragOffsetY = (float) mouseY - itemBuy.getBounds().y();
                    return;
                } else if (button == MouseButton.RIGHT) {
                    if (currentUiItemBuy != null) {
                        currentUiItemBuy.getItemBuy().setSelected(false);
                    }
                    currentUiItemBuy = itemBuy;
                    currentUiItemBuy.getItemBuy().setSelected(true);
                    return;
                }
            }
        }


        if (currentUiItemBuy != null) {
            currentUiItemBuy.onMouseClicked(mouseX, mouseY, button, true);
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        if (draggedItem != null && button == MouseButton.LEFT) {
            float sizeItem = 20;
            float widthLibrary = 8 + (sizeItem + 4) * 4 + 8;
            float height = 200;
            float x = (mc.getWindow().getScaledWidth() - (widthLibrary + (9 * (sizeItem + 4)) + 8)) / 2;
            float y = (mc.getWindow().getScaledHeight() - height) / 2;

            float slotStartX = x + widthLibrary;
            float slotStartY = y + height - 8 - 4 * (sizeItem + 4);

            int col = 0, row = 0;
            boolean placed = false;

            for (int slotIndex = 0; slotIndex < 9 * 4; slotIndex++) {
                float slotX = slotStartX + col * (sizeItem + 4);
                float slotY = slotStartY + row * (sizeItem + 4);

                if (mouseX >= slotX && mouseX <= slotX + sizeItem &&
                        mouseY >= slotY && mouseY <= slotY + sizeItem) {

                    if (!isSlotOccupied(slotIndex)) {
                        if (draggedFromLibrary) {
                            AutoInventoryItem newItem = draggedItem.getItemBuy().copy();

                            newItem.setSlotId(slotIndex);
                            AutoSbor.INSTANCE.getZakup().add(newItem);
                            UiItemBuy uiItemBuy = new UiItemBuy(newItem);

                            inventoryItems.add(uiItemBuy);
                            if (currentUiItemBuy != null) {
                                currentUiItemBuy.getItemBuy().setSelected(false);
                            }
                            currentUiItemBuy = uiItemBuy;
                            currentUiItemBuy.getItemBuy().setSelected(true);
                        } else {

                            draggedItem.getItemBuy().setSlotId(slotIndex);
                        }
                        placed = true;
                    }
                    break;
                }
                if (++col >= 9) {
                    col = 0;
                    row++;
                }
            }


            if (!placed && !draggedFromLibrary) {
                AutoSbor.INSTANCE.getZakup().remove(draggedItem.getItemBuy());
                inventoryItems.remove(draggedItem);
            }

            draggedItem = null;
        }
        if (currentUiItemBuy != null) {
            this.currentUiItemBuy.onMouseReleased(mouseX, mouseY, button);
        }

    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        searchBox.charTyped(codePoint, modifiers);
        if (currentUiItemBuy != null) {
            currentUiItemBuy.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollHandler.scroll(verticalAmount * 10);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        searchBox.keyPressed(keyCode, scanCode, modifiers);
        if (currentUiItemBuy != null) {
            currentUiItemBuy.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }
}
