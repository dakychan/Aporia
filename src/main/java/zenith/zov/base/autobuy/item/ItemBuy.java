package zenith.zov.base.autobuy.item;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import zenith.zov.utility.interfaces.IClient;


@Getter
public class ItemBuy implements IClient {
    protected ItemStack itemStack;
    protected final String displayName;
    protected final String searchName;
    protected final Category category;
    public ItemBuy(ItemStack itemStack, String searchName, Category category) {
        this.itemStack = itemStack;
        this.displayName = searchName;
        this.searchName = searchName;
        this.category = category;

    }

    public ItemBuy(ItemStack itemStack, String displayName, String searchName, Category category) {
        this.itemStack = itemStack;
        this.displayName = displayName;
        this.searchName = searchName;
        this.category = category;
    }

    public boolean isBuy(ItemStack stack) {

        return stack != null && stack.getItem() == itemStack.getItem();
    }


    public enum Category{
        FUNTIME,
        HOLLYWORLD,
        ANY
    }
}
