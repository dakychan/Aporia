package zenith.zov.client.modules.api.setting.impl;



import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import zenith.zov.client.modules.api.setting.Setting;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Supplier;

public class ItemSelectSetting extends Setting {
    public void setItemsById(List<String> itemsById) {

        this.itemsById = itemsById;
    }

    private List<String> itemsById;
    public ItemSelectSetting(String name,List<String> itemsById) {
        this(name,itemsById, ()->true);
    }
    public ItemSelectSetting(String name,List<String> itemsById,Supplier<Boolean> visible) {
        super(name);
        this.itemsById = itemsById;
    }


    public List<String> getItemsById() {
        return itemsById;
    }

    public void add(String s) {
        itemsById.add(s);
    }

    public void remove(String s) {
        itemsById.remove(s);
    }

    public boolean contains(String s) {
        return itemsById.contains(s);
    }

    public void add(Block b) {
        add(b.getTranslationKey().replace("block.minecraft.", ""));
    }

    public void add(Item i) {
        add(i.getTranslationKey().replace("item.minecraft.", ""));
    }

    public void remove(Block b) {

        remove(b.getTranslationKey().replace("block.minecraft.", ""));
    }

    public void remove(Item i) {
        remove(i.getTranslationKey().replace("item.minecraft.", ""));
    }

    public boolean contains(Block b) {
        return contains(b.getTranslationKey().replace("block.minecraft.", ""));
    }

    public boolean contains(Item i) {
        return contains(i.getTranslationKey().replace("item.minecraft.", ""));
    }

    public void clear() {
        itemsById.clear();
    }

    private final static Gson gson = new Gson();
    @Override
    public void safe(JsonObject propertiesObject) {
        propertiesObject.add(String.valueOf(name), gson.toJsonTree(this.getItemsById()));


    }

    @Override
    public void load(JsonObject propertiesObject) {
        Type listType = new TypeToken<List<String>>() {}.getType();
        JsonElement jsonElement = propertiesObject.get(String.valueOf(name));
        if (jsonElement != null && jsonElement.isJsonArray()) {
            List<String> list = gson.fromJson(jsonElement, listType);

            this.setItemsById(list);
        }
    }
}
