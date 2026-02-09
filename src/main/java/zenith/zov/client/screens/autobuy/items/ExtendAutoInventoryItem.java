package zenith.zov.client.screens.autobuy.items;

import zenith.zov.base.autobuy.item.ItemBuy;
import zenith.zov.client.screens.menu.settings.api.MenuSetting;

import java.util.List;

public abstract class ExtendAutoInventoryItem extends AutoInventoryItem {
    public ExtendAutoInventoryItem(ItemBuy itemBuy) {
        super(itemBuy);
    }
    public abstract List<MenuSetting> getEnchants();

}
