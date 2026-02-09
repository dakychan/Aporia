package zenith.zov.base.autobuy.item;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Optional;
import java.util.UUID;

public class SkinItemBuy extends ItemBuy {
    private final String skin;
    public SkinItemBuy(String name,Category category, String skin) {
        this(skin, name, name, category);
    }

    public SkinItemBuy(String skin, String displayName, String searchName, Category maxSumBuy) {

        super(Items.PLAYER_HEAD.getDefaultStack(), displayName, searchName, maxSumBuy);
        this.skin = skin;
        final ComponentChanges.Builder datacomponentpatch$builder = ComponentChanges.builder();
        Optional<String> emptyString = Optional.of("");
        Optional<UUID> emptyUUID = Optional.of(UUID.randomUUID());
        PropertyMap propertyMap = new PropertyMap();

        propertyMap.put("textures", new Property("textures", skin));
        ProfileComponent resolvableProfile = new ProfileComponent(emptyString, emptyUUID, propertyMap);
        datacomponentpatch$builder.add(DataComponentTypes.PROFILE, resolvableProfile);
        ItemStackArgument input = new ItemStackArgument(itemStack.getRegistryEntry(), datacomponentpatch$builder.build());
        try {
            this.itemStack = input.createStack(1, false);
        } catch (CommandSyntaxException e) {

        }

    }

    @Override
    public boolean isBuy(ItemStack stack) {
        if (!super.isBuy(stack)) {
            return false;
        }

        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null && customData.getNbt().contains("SkullOwner")) {
            return customData.getNbt().get("SkullOwner").toString().contains(this.skin);

        }
        return false;

    }
    
    public String getSkin() {
        return skin;
    }
  }
