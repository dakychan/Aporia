package aporia.cc.client.screens.autobuy.items;

import aporia.cc.base.autobuy.item.ItemBuy;
import aporia.cc.client.screens.menu.settings.api.MenuSetting;

import java.util.List;

public abstract class ExtendAutoInventoryItem extends AutoInventoryItem {
    public ExtendAutoInventoryItem(ItemBuy itemBuy) {
        super(itemBuy);
    }
    public abstract List<MenuSetting> getEnchants();

}

