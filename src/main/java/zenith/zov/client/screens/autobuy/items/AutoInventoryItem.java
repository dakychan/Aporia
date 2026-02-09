package zenith.zov.client.screens.autobuy.items;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import zenith.zov.base.autobuy.item.ItemBuy;

@Getter
@Setter
public class AutoInventoryItem {


    private final ItemBuy itemBuy;
    private long maxSumBuy = 0;
    private int countBuy = 1;
    private boolean selected = false;
    private int slotId;
    public AutoInventoryItem(ItemBuy itemBuy) {
        this.itemBuy = itemBuy;
    }

    public void toggleSelected() {
        selected = !selected;
    }


    public AutoInventoryItem copy() {
        AutoInventoryItem copy = new AutoInventoryItem(this.itemBuy);
        copy.maxSumBuy = this.maxSumBuy;
        copy.countBuy = this.countBuy;
        copy.selected = this.selected;

        return copy;
    }
    public JsonObject save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("maxSumBuy", maxSumBuy);
        obj.addProperty("countBuy", countBuy);
        obj.addProperty("selected", selected);
        obj.addProperty("slotId", slotId);
        return obj;
    }

    public void load(JsonObject obj) {
        if (obj.has("maxSumBuy")) maxSumBuy = obj.get("maxSumBuy").getAsLong();
        if (obj.has("countBuy")) countBuy = obj.get("countBuy").getAsInt();
        if (obj.has("selected")) selected = obj.get("selected").getAsBoolean();
        if (obj.has("slotId")) slotId = obj.get("slotId").getAsInt();
    }
    public boolean isBuy(ItemStack stack) {
        return itemBuy.isBuy(stack);
    }

}
