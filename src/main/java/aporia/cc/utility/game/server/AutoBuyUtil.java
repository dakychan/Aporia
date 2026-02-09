package aporia.cc.utility.game.server;

import lombok.experimental.UtilityClass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.MergedComponentMap;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
import aporia.cc.base.autobuy.enchantes.Enchant;
import aporia.cc.base.autobuy.enchantes.custom.EnchantCustom;
import aporia.cc.base.autobuy.enchantes.minecraft.EnchantVanilla;
import aporia.cc.utility.mixin.accessors.ItemStackAccessor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class AutoBuyUtil {

    private Pattern patternFuntime = Pattern.compile("\\$\\s*([0-9][\\d,]*)");;
    private Pattern patternHollyWorld = Pattern.compile("Цена:(?:.*?\\{\"text\":\"([\\d ]+)\")");

    public int getPrice(String nbt) {

        Matcher matcher = patternFuntime.matcher(nbt);

        if (matcher.find()) {

            String priceWithCommas = matcher.group(1);
            String price = priceWithCommas.replace(",", "");
            return Integer.parseInt(price);
        } else {
            matcher = patternHollyWorld.matcher(nbt);

            if (matcher.find()) {
                String amount = matcher.group(1).replaceAll(" ", "");
                return Integer.parseInt(amount);
            }

        }


        return Integer.MAX_VALUE;
    }

    public boolean checkDon(ItemStack itemStack) {
        return itemStack.getCustomName().getString().contains("★");
    }

    public int getPrice(ItemStack itemStack) {
        String nbt = getNBT(itemStack);
        return getPrice(nbt);

    }

    public String getNBT(ItemStack itemStack) {

        return getTag(itemStack).toString();
    }

    public String getKey(ItemStack itemStack) {
        System.out.println(getNBT(itemStack));
        NbtComponent customData = itemStack.get(DataComponentTypes.CUSTOM_DATA);

        if (customData != null) {

              System.out.println(customData.getNbt().getKeys());
            System.out.println(itemStack.getItem());
            if (customData.getNbt().contains("kringeItems")) {
                NbtElement customEnchants = customData.getNbt().get("kringeItems");
                MinecraftClient.getInstance().keyboard.setClipboard(customEnchants.toString());

                return customEnchants.toString();
            }

        }

        return "";
    }

    private final Map<ItemStack, NbtCompound> nbtCompoundMap = new HashMap<>();

    public NbtCompound getTag(ItemStack stack) {
        MergedComponentMap components = ((ItemStackAccessor) (Object) stack).getComponents();
        ComponentChanges changes = components.getChanges();
        World world = MinecraftClient.getInstance().world;
        if (world == null) return new NbtCompound();

        return nbtCompoundMap.computeIfAbsent(stack, itemStack -> (NbtCompound) ComponentChanges.CODEC
                .encodeStart(world.getRegistryManager().getOps(NbtOps.INSTANCE), changes)
                .getOrThrow());
    }

    public String getTagFuntimeNotTempElements(ItemStack stack) {
        MergedComponentMap components = ((ItemStackAccessor) (Object) stack).getComponents();
        ComponentChanges changes = components.getChanges();
        World world = MinecraftClient.getInstance().world;
        if (world == null) return "";

        return nbtCompoundMap.computeIfAbsent(stack, itemStack -> (NbtCompound) ComponentChanges.CODEC
                .encodeStart(world.getRegistryManager().getOps(NbtOps.INSTANCE), changes)
                .getOrThrow()).toString().replaceAll(",?\\s*PublicBukkitValues:\\{[^}]*\\}", "")
                .replaceAll(
                "'\\{[^']*Истeкaeт:[^']*\\}',?", "").replaceAll(",?UUID:\\[I;[-0-9]+,[-0-9]+,[-0-9]+,[-0-9]+]", "").replaceAll("minecraft:[0-9a-f\\-]{36}", "minecraft:UUID");
    }

    public ArrayList<Enchant> getEnchants(ItemStack stack) {
        ArrayList<Enchant> enchantsBuy = new ArrayList<>();
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtList customEnchants = customData.getNbt().getListOrEmpty("Enchantments");

            for (int i = 0; i < customEnchants.size(); i++) {
                NbtCompound ench = customEnchants.getCompoundOrEmpty(i);

                String type = ench.getString("id", "");
                int level = ench.getInt("lvl", 0);
                enchantsBuy.add(new EnchantCustom(type, type, level));
            }
        }
        ItemEnchantmentsComponent enchants = stack.getEnchantments();
        for (var entry : enchants.getEnchantmentEntries()) {
            String id = entry.getKey().getKey().get().getValue().toString();

            enchantsBuy.add(new EnchantVanilla(id, id, entry.getIntValue()));

        }
        return enchantsBuy;
    }

    public boolean isAuction(ScreenHandler handledScreen) {

        return (handledScreen.slots.size() == 90 && handledScreen.getSlot(49).getStack().getItem() == Items.NETHER_STAR) ;
    }
    public boolean isWaitBuy(ScreenHandler handledScreen) {
        return (handledScreen.slots.size() == 63 && handledScreen.getSlot(0).getStack().getItem() == Items.LIME_STAINED_GLASS_PANE);
    }
    public List<String> testBypass = new ArrayList<>();
    public static void test(int slotId) {

    }

}

