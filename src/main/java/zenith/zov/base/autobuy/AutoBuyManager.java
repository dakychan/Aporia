package zenith.zov.base.autobuy;

import lombok.Getter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import zenith.zov.base.autobuy.enchantes.Enchant;
import zenith.zov.base.autobuy.enchantes.container.EnchantContainer;
import zenith.zov.base.autobuy.item.EnchantItemBuy;
import zenith.zov.base.autobuy.item.ItemBuy;
import zenith.zov.base.autobuy.item.NbtItemBuy;
import zenith.zov.base.autobuy.item.SkinItemBuy;

import java.awt.*;
import java.util.*;
import java.util.List;

@Getter
public class AutoBuyManager {

    private ArrayList<ItemBuy> vanilla = new ArrayList<>();

    private ArrayList<ItemBuy> funtime = new ArrayList<>();
    private ArrayList<ItemBuy> hollyworld = new ArrayList<>();

    public AutoBuyManager() {
        {
            List<Enchant> crusherHelmetEnchants = EnchantContainer.parse("[EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:protection, level=5], EnchantVanilla [checked=minecraft:fire_protection, level=5], EnchantVanilla [checked=minecraft:projectile_protection, level=5], EnchantVanilla [checked=minecraft:respiration, level=3], EnchantVanilla [checked=minecraft:mending, level=1], EnchantVanilla [checked=minecraft:blast_protection, level=5], EnchantVanilla [checked=minecraft:aqua_affinity, level=1]]");
            ;

            EnchantItemBuy crusherHELMET = new EnchantItemBuy(Items.NETHERITE_HELMET.getDefaultStack(), "Шлем крушителя", ItemBuy.Category.FUNTIME);
            crusherHelmetEnchants.forEach(crusherHELMET::addEnchant);

            List<Enchant> crusherChestplateEnchants = EnchantContainer.parse("[EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:projectile_protection, level=5], EnchantVanilla [checked=minecraft:protection, level=5], EnchantVanilla [checked=minecraft:fire_protection, level=5], EnchantVanilla [checked=minecraft:blast_protection, level=5], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy crusherCHESTPLATE = new EnchantItemBuy(Items.NETHERITE_CHESTPLATE.getDefaultStack(), "Нагрудник Крушителя", ItemBuy.Category.FUNTIME);
            crusherChestplateEnchants.forEach(crusherCHESTPLATE::addEnchant);


            EnchantItemBuy crusherLEGGINGS = new EnchantItemBuy(Items.NETHERITE_LEGGINGS.getDefaultStack(), "Поножи Крушителя", ItemBuy.Category.FUNTIME);
            crusherChestplateEnchants.forEach(crusherLEGGINGS::addEnchant);

            List<Enchant> crusherBootsEnchants = EnchantContainer.parse("[EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:soul_speed, level=3], EnchantVanilla [checked=minecraft:protection, level=5], EnchantVanilla [checked=minecraft:fire_protection, level=5], EnchantVanilla [checked=minecraft:depth_strider, level=3], EnchantVanilla [checked=minecraft:feather_falling, level=4], EnchantVanilla [checked=minecraft:projectile_protection, level=5], EnchantVanilla [checked=minecraft:blast_protection, level=5], EnchantVanilla [checked=minecraft:mending, level=1]]");

            EnchantItemBuy crusherBoots = new EnchantItemBuy(Items.NETHERITE_BOOTS.getDefaultStack(), "Ботинки Крушителя", ItemBuy.Category.FUNTIME);
            crusherBootsEnchants.forEach(crusherBoots::addEnchant);


            List<Enchant> crusherSwordEnchants = EnchantContainer.parse("[EnchantCustom [checked=oxidation, level=2], EnchantCustom [checked=detection, level=3], EnchantCustom [checked=poison, level=3], EnchantCustom [checked=vampirism, level=2], EnchantCustom [checked=skilled, level=3], EnchantVanilla [checked=minecraft:looting, level=5], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:fire_aspect, level=2], EnchantVanilla [checked=minecraft:sweeping_edge, level=3], EnchantVanilla [checked=minecraft:smite, level=7], EnchantVanilla [checked=minecraft:sharpness, level=7], EnchantVanilla [checked=minecraft:bane_of_arthropods, level=7], EnchantVanilla [checked=minecraft:mending, level=1]]");

            EnchantItemBuy crusherSword = new EnchantItemBuy(Items.NETHERITE_SWORD.getDefaultStack(), "Меч Крушителя", ItemBuy.Category.FUNTIME);
            crusherSwordEnchants.forEach(crusherSword::addEnchant);

            List<Enchant> crusherPickaxeEnchants = EnchantContainer.parse("[EnchantCustom [checked=skilled, level=3], EnchantCustom [checked=smelting, level=1], EnchantCustom [checked=magnet, level=1], EnchantCustom [checked=pinger, level=1], EnchantCustom [checked=web, level=1], EnchantCustom [checked=buldozing, level=2], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:efficiency, level=10], EnchantVanilla [checked=minecraft:mending, level=1], EnchantVanilla [checked=minecraft:fortune, level=5]]");
            EnchantItemBuy crusherPickaxe = new EnchantItemBuy(Items.NETHERITE_PICKAXE.getDefaultStack(), "Кирка Крушителя", ItemBuy.Category.FUNTIME);
            crusherPickaxeEnchants.forEach(crusherPickaxe::addEnchant);

            List<Enchant> crusherCrossbowEnchants = EnchantContainer.parse("[EnchantVanilla [checked=minecraft:unbreaking, level=3], EnchantVanilla [checked=minecraft:mending, level=1], EnchantVanilla [checked=minecraft:multishot, level=1], EnchantVanilla [checked=minecraft:piercing, level=5], EnchantVanilla [checked=minecraft:quick_charge, level=3]]");
            EnchantItemBuy crusherCrossbow = new EnchantItemBuy(Items.CROSSBOW.getDefaultStack(), "Арбалет Крушителя", ItemBuy.Category.FUNTIME);
            crusherCrossbowEnchants.forEach(crusherCrossbow::addEnchant);

            List<Enchant> crusherTridentEnchants = EnchantContainer.parse("[EnchantCustom [checked=detection, level=3], EnchantCustom [checked=poison, level=3], EnchantCustom [checked=demolishing, level=1], EnchantCustom [checked=returning, level=1], EnchantCustom [checked=oxidation, level=2], EnchantCustom [checked=pulling, level=2], EnchantCustom [checked=stupor, level=3], EnchantCustom [checked=vampirism, level=2], EnchantCustom [checked=skilled, level=3], EnchantCustom [checked=scout, level=3], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:fire_aspect, level=2], EnchantVanilla [checked=minecraft:loyalty, level=3], EnchantVanilla [checked=minecraft:impaling, level=5], EnchantVanilla [checked=minecraft:channeling, level=1], EnchantVanilla [checked=minecraft:sharpness, level=7], EnchantVanilla [checked=minecraft:mending, level=1]]");

            EnchantItemBuy crusherTrident = new EnchantItemBuy(Items.TRIDENT.getDefaultStack(), "Трезубец Крушителя", ItemBuy.Category.FUNTIME);
            crusherTridentEnchants.forEach(crusherTrident::addEnchant);
            ItemStack medikStak = Items.SPLASH_POTION.getDefaultStack();
            medikStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(214, 0, 191).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy medik = new NbtItemBuy(medikStak, "Зелье Медика", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:2b,duration:900,id:\"minecraft:health_boost\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:2b,duration:900,id:\"minecraft:regeneration\",show_icon:1b,show_particles:1b}]");

            ItemStack pobedilkaStak = Items.SPLASH_POTION.getDefaultStack();
            pobedilkaStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(11, 188, 4).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy pobedilka = new NbtItemBuy(pobedilkaStak, "Зелье Победителя", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:1b,duration:3600,id:\"minecraft:health_boost\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:18000,id:\"minecraft:invisibility\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:1b,duration:1200,id:\"minecraft:regeneration\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:1200,id:\"minecraft:resistance\",show_icon:1b,show_particles:1b}]");

            ItemStack agentStak = Items.SPLASH_POTION.getDefaultStack();
            agentStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(246, 250, 78).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy agent = new NbtItemBuy(agentStak, "Зелье Агента", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:0b,duration:18000,id:\"minecraft:fire_resistance\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:3600,id:\"minecraft:haste\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:18000,id:\"minecraft:invisibility\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:2b,duration:18000,id:\"minecraft:speed\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:2b,duration:6000,id:\"minecraft:strength\",show_icon:1b,show_particles:1b}]");

            ItemStack killerStak = Items.SPLASH_POTION.getDefaultStack();
            killerStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(135, 0, 0).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy killer = new NbtItemBuy(killerStak, "Зелье Киллера", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:0b,duration:3600,id:\"minecraft:resistance\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:3b,duration:1800,id:\"minecraft:strength\",show_icon:1b,show_particles:1b}]");

            ItemStack kislotaStak = Items.SPLASH_POTION.getDefaultStack();
            kislotaStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(164, 252, 76).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy kislota = new NbtItemBuy(kislotaStak, "Серная кислота", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:1b,duration:1000,id:\"minecraft:poison\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:3b,duration:1800,id:\"minecraft:slowness\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:2b,duration:1800,id:\"minecraft:weakness\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:4b,duration:600,id:\"minecraft:wither\",show_icon:1b,show_particles:1b}]");
            //  NbtItemBuy silka3 = new NbtItemBuy(Items.POTION.getDefaultStack(), "Зелье силы","Зелье силы", ItemBuy.Category.FUNTIME,"custom_potion_effects","[{ambient:1b,amplifier:2b,duration:3600,id:\"minecraft:strength\",show_icon:1b,show_particles:1b},{ambient:1b,amplifier:2b,duration:3600,id:\"minecraft:speed\",show_icon:1b,show_particles:1b}]");

            ItemStack regenkaStak = Items.SPLASH_POTION.getDefaultStack();
            regenkaStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(243, 59, 128).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy regenka = new NbtItemBuy(regenkaStak, "Исцел", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:1b,amplifier:1b,duration:1,id:\"minecraft:instant_health\",show_icon:1b,show_particles:1b},{ambient:1b,amplifier:0b,duration:600,id:\"minecraft:regeneration\",show_icon:1b,show_particles:1b}]");

            ItemStack otrigStak = Items.SPLASH_POTION.getDefaultStack();
            otrigStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(new Color(16737792).getRGB()), List.of(), Optional.empty()
            ));
            NbtItemBuy otrig = new NbtItemBuy(otrigStak, "Отрыжка", ItemBuy.Category.FUNTIME, "CustomPotionColor", "16737792");

            ItemStack ice_arrowStak = Items.TIPPED_ARROW.getDefaultStack();
            ice_arrowStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(65535), List.of(), Optional.empty()
            ));
            NbtItemBuy ice_arrow = new NbtItemBuy(ice_arrowStak, "Ледяная стрела", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:50b,duration:100,id:\"minecraft:slowness\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:120,id:\"minecraft:weakness\",show_icon:1b,show_particles:1b}]");
            ItemStack gowno_arrowStak = Items.TIPPED_ARROW.getDefaultStack();
            gowno_arrowStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                    Optional.of(Potions.SWIFTNESS), Optional.of(0), List.of(), Optional.empty()
            ));

            NbtItemBuy gowno_arrow = new NbtItemBuy(gowno_arrowStak, "Проклятая стрела", ItemBuy.Category.FUNTIME, "custom_potion_effects", "[{ambient:0b,amplifier:0b,duration:40,id:\"minecraft:blindness\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:100,id:\"minecraft:nausea\",show_icon:1b,show_particles:1b},{ambient:0b,amplifier:0b,duration:200,id:\"minecraft:weakness\",show_icon:1b,show_particles:1b}]");


            NbtItemBuy plast = new NbtItemBuy(Items.DRIED_KELP.getDefaultStack(), "Пласт", ItemBuy.Category.FUNTIME, "stratum", "1b");
            NbtItemBuy trapka = new NbtItemBuy(Items.NETHERITE_SCRAP.getDefaultStack(), "Трапка", ItemBuy.Category.FUNTIME, "trap", "1b");
            NbtItemBuy desoritation = new NbtItemBuy(Items.ENDER_EYE.getDefaultStack(), "Дезориентация", ItemBuy.Category.FUNTIME, "desorientation", "1b");
            NbtItemBuy yawka = new NbtItemBuy(Items.SUGAR.getDefaultStack(), "Явная пыль", ItemBuy.Category.FUNTIME, "sheerdust", "1b");
            NbtItemBuy bogAura = new NbtItemBuy(Items.PHANTOM_MEMBRANE.getDefaultStack(), "Божья аура", ItemBuy.Category.FUNTIME, "godsaura", "1b");

            SkinItemBuy andromed = new SkinItemBuy("Сфера Андромеды", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NTEwODQzNywKICAicHJvZmlsZUlkIiA6ICIzMjNiYjlkYzkwZWU0Nzk5YjUxYzE3NjRmZDRhNjI3OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJOcGllIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQ0ZmZlM2YzNThmMjA5YmFkOGZmZjRkYzQ4MjQ1ZDliYWYwYTAzMWIzYzFlZTZiNzU4NDYwYTMzOWIxNTE5ZTIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==");
            SkinItemBuy pandora = new SkinItemBuy("Сфера Пандоры", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NTY2NTExNCwKICAicHJvZmlsZUlkIiA6ICJkNzJlNGJjZDIyZGI0NjQ4OTUxNTc0M2UyYTRmMWFjMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJhdnZheSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS84ZTUxZTY1ZWI0MDUyNzcyMzgyYzllNTA3YTU0YmRlZDQzZTM5Zjc1NWI1ZGRmNTViM2YzOTQ0M2NlZDQ2N2Y0IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=");
            SkinItemBuy titan = new SkinItemBuy("Сфера Титана", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM1NDQ1NTE5MiwKICAicHJvZmlsZUlkIiA6ICJkOTcwYzEzZTM4YWI0NzlhOTY1OGM1ZDQ1MjZkMTM0YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJDcmltcHlMYWNlODUxMjciLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlOTY5ODQ1OGI3ODQxYzk2YWU0ZjI0ZWM4NGFlMDE3MjQxMDA2NDFjNTY0ZTJhN2IxODVmNDA2ZThlZDIzIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=");
            SkinItemBuy appolon = new SkinItemBuy("Сфера Аполлона", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NjYyNTM0NywKICAicHJvZmlsZUlkIiA6ICJhMjk1ODZmYmU1ZDk0Nzk2OWZjOGQ4ZGE0NzlhNDNlZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJMZXZlMjQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQxMTdiNjAxOGZlZjBkNTE1NjcyMTczZTNiMjZlNjYwZDY1MWU1ODc2YmE2ZDAzZTUzNDIyNzBjNDliZWM4MCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9");
            SkinItemBuy astrei = new SkinItemBuy("Сфера Астрея", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NTA2MjQwNywKICAicHJvZmlsZUlkIiA6ICJlMzcxMWU2Y2E0ZmY0NzA4YjY5ZjhiNGZlYzNhZjdhMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJNckJ1cnN0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzFhNWFhZGQ1MmE1ZmFiOTcwODgxNDUxYWRmNTZmYmI0OTNhMzU4NTZlYTk2ZjU0ZTMyZWVhNjYyZDc4N2VkMjAiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==");
            SkinItemBuy sirius = new SkinItemBuy("Сфера Осириса", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NjY2Mzg3NiwKICAicHJvZmlsZUlkIiA6ICI3NGEwMzQxNWY1OTI0ZTA4YjMyMGM2MmU1NGE3ZjJhYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJNZXp6aXIiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDgxMzYzNWJkODZiMTcxYmJlMTQzYWQ3MWUwOTAyMjkyNjQ5Y2IzYWI4NDQwZWQwMGY4NWNhNmNhMzgyOTkzNiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9");
            SkinItemBuy himera = new SkinItemBuy("Сфера Химеры", ItemBuy.Category.FUNTIME, "ewogICJ0aW1lc3RhbXAiIDogMTcxNzM2NjE4MTEwOSwKICAicHJvZmlsZUlkIiA6ICJiNzRiMGQzNTBkNTk0NTU4YmYyYjBlMDJlYmE4NjE4NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJCcmFuZG9uYnBtMjg0IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzlmYWJlZWQ0MjRiMjUyYTg5NDVhNjQ0MmI0NjJkNWYzMTQ3MDFhODE2ZGEyZDBhNjljY2RmY2ZkNzQ2ZTU4OGUiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==");

            NbtItemBuy grani = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Грани", ItemBuy.Category.FUNTIME, "AttributeModifiers", "[{Amount:-4.0d,AttributeName:\"minecraft:generic.max_health\",Name:\"максимальное здоровье\",Operation:0,Slot:\"offhand\",UUID:[I;-1243469000,-221950864,-1992446485,-2021417908]},{Amount:0.15d,AttributeName:\"minecraft:generic.movement_speed\",Name:\"скорость\",Operation:1,Slot:\"offhand\",UUID:[I;421185474,-1675802507,-1302866561,493632362]},{Amount:3.0d,AttributeName:\"minecraft:generic.attack_damage\",Name:\"сила\",Operation:0,Slot:\"offhand\",UUID:[I;1519226946,1096501307,-1211766788,1772678932]}]");

            NbtItemBuy dedal = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Дедала", ItemBuy.Category.FUNTIME, "AttributeModifiers", "[{Amount:-4.0d,AttributeName:\"minecraft:generic.max_health\",Name:\"максимальное здоровье\",Operation:0,Slot:\"offhand\",UUID:[I;359257458,1778207792,-1338766924,848576712]},{Amount:5.0d,AttributeName:\"minecraft:generic.attack_damage\",Name:\"сила\",Operation:0,Slot:\"offhand\",UUID:[I;-1723975978,-172144109,-1549361344,1422474545]}]");

            NbtItemBuy triton = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Тритона", ItemBuy.Category.FUNTIME, "AttributeModifiers", "[{Amount:2.0d,AttributeName:\"minecraft:generic.max_health\",Name:\"максимальное здоровье\",Operation:0,Slot:\"offhand\",UUID:[I;1523385351,-602324415,-1760945561,-270836422]},{Amount:-2.0d,AttributeName:\"minecraft:generic.armor_toughness\",Name:\"твёрдость брони\",Operation:0,Slot:\"offhand\",UUID:[I;1017626540,670256083,-1935748919,-786940866]},{Amount:2.0d,AttributeName:\"minecraft:generic.armor\",Name:\"броня\",Operation:0,Slot:\"offhand\",UUID:[I;1409389356,857884230,-1271577328,-932163262]}]");
            NbtItemBuy garmon = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Гармонии", ItemBuy.Category.FUNTIME, "AttributeModifiers", "[{Amount:2.0d,AttributeName:\"minecraft:generic.max_health\",Name:\"максимальное здоровье\",Operation:0,Slot:\"offhand\",UUID:[I;1253805553,182601342,-1805352146,-1397322752]},{Amount:2.0d,AttributeName:\"minecraft:generic.attack_damage\",Name:\"сила\",Operation:0,Slot:\"offhand\",UUID:[I;1648014473,711805426,-1962672765,-537567144]},{Amount:2.0d,AttributeName:\"minecraft:generic.armor\",Name:\"броня\",Operation:0,Slot:\"offhand\",UUID:[I;-432853915,2131774585,-1617568671,-621826558]}]");

            NbtItemBuy fenix = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Феникса", ItemBuy.Category.FUNTIME, "AttributeModifiers", "[{Amount:6.0d,AttributeName:\"minecraft:generic.max_health\",Name:\"максимальное здоровье\",Operation:0,Slot:\"offhand\",UUID:[I;587791282,-2058138192,-2013554380,1529824133]},{Amount:0.1d,AttributeName:\"minecraft:generic.attack_speed\",Name:\"скорость атаки\",Operation:1,Slot:\"offhand\",UUID:[I;233538602,1321813146,-1704635325,1580943681]}]");

            NbtItemBuy ehidna = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Ехидны", ItemBuy.Category.FUNTIME, "AttributeModifiers", "[{Amount:-4.0d,AttributeName:\"minecraft:generic.max_health\",Name:\"максимальное здоровье\",Operation:0,Slot:\"offhand\",UUID:[I;951469257,944390782,-1639200535,1882164017]},{Amount:6.0d,AttributeName:\"minecraft:generic.attack_damage\",Name:\"сила\",Operation:0,Slot:\"offhand\",UUID:[I;955551735,1646086503,-2027411701,279678977]},{Amount:-2.0d,AttributeName:\"minecraft:generic.armor_toughness\",Name:\"твёрдость брони\",Operation:0,Slot:\"offhand\",UUID:[I;1301352305,-411349527,-1975888927,1331290909]},{Amount:-2.0d,AttributeName:\"minecraft:generic.armor\",Name:\"броня\",Operation:0,Slot:\"offhand\",UUID:[I;-1591608852,-1821816734,-1777269705,-1095126461]}]");

            NbtItemBuy krush = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Крушителя", ItemBuy.Category.FUNTIME, "AttributeModifiers", "[{Amount:4.0d,AttributeName:\"minecraft:generic.max_health\",Name:\"максимальное здоровье\",Operation:0,Slot:\"offhand\",UUID:[I;-2029680951,-1707392264,-1958707995,-389772071]},{Amount:3.0d,AttributeName:\"minecraft:generic.attack_damage\",Name:\"сила\",Operation:0,Slot:\"offhand\",UUID:[I;430632003,-1617212859,-1216331408,-1833541566]},{Amount:2.0d,AttributeName:\"minecraft:generic.armor_toughness\",Name:\"твёрдость брони\",Operation:0,Slot:\"offhand\",UUID:[I;-1348659832,-1401469259,-1365362331,376666792]},{Amount:2.0d,AttributeName:\"minecraft:generic.armor\",Name:\"броня\",Operation:0,Slot:\"offhand\",UUID:[I;897201681,1928088706,-1491637188,-648449433]}]");
            NbtItemBuy karatel = new NbtItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Талисман Карателя", ItemBuy.Category.FUNTIME, "AttributeModifiers", "[{Amount:7.0d,AttributeName:\"minecraft:generic.attack_damage\",Name:\"сила\",Operation:0,Slot:\"offhand\",UUID:[I;-1651993925,-1873589036,-2067416271,-818067434]},{Amount:-4.0d,AttributeName:\"minecraft:generic.max_health\",Name:\"максимальное здоровье\",Operation:0,Slot:\"offhand\",UUID:[I;-473822740,-120697814,-1795206788,-16471115]},{Amount:0.1d,AttributeName:\"minecraft:generic.movement_speed\",Name:\"скорость\",Operation:1,Slot:\"offhand\",UUID:[I;1026697411,-1939651121,-2079096398,-1055446047]}]");

            funtime.add(crusherHELMET);
            funtime.add(crusherCHESTPLATE);
            funtime.add(crusherLEGGINGS);
            funtime.add(crusherBoots);
            funtime.add(crusherSword);
            funtime.add(crusherPickaxe);
            funtime.add(crusherCrossbow);
            funtime.add(crusherTrident);

            funtime.add(medik);
            funtime.add(pobedilka);
            funtime.add(agent);
            funtime.add(killer);
            funtime.add(kislota);
            funtime.add(regenka);
            funtime.add(ice_arrow);
            funtime.add(gowno_arrow);
            funtime.add(otrig);

            funtime.add(plast);
            funtime.add(trapka);
            funtime.add(desoritation);
            funtime.add(yawka);
            funtime.add(bogAura);

            funtime.add(andromed);
            funtime.add(pandora);
            funtime.add(titan);
            funtime.add(appolon);
            funtime.add(astrei);
            funtime.add(sirius);
            funtime.add(himera);

            funtime.add(grani);
            funtime.add(dedal);
            funtime.add(triton);
            funtime.add(garmon);
            funtime.add(fenix);
            funtime.add(ehidna);
            funtime.add(krush);
            funtime.add(karatel);
        }

        vanilla.add(new ItemBuy(Items.ENCHANTED_GOLDEN_APPLE.getDefaultStack(), "Зачарованное золотое яблоко", ItemBuy.Category.ANY));
        vanilla.add(new ItemBuy(Items.GOLDEN_APPLE.getDefaultStack(), "Золотое яблоко", ItemBuy.Category.ANY));
        vanilla.add(new ItemBuy(Items.ENDER_PEARL.getDefaultStack(), "Эндер жемчуг", ItemBuy.Category.ANY));
        vanilla.add(new ItemBuy(Items.CHORUS_FRUIT.getDefaultStack(), "Плод хоруса", ItemBuy.Category.ANY));
        vanilla.add(new ItemBuy(Items.TOTEM_OF_UNDYING.getDefaultStack(), "Тотем бессмертия", ItemBuy.Category.ANY));
        vanilla.add(new ItemBuy(Items.EXPERIENCE_BOTTLE.getDefaultStack(), "Пузырёк опыта", ItemBuy.Category.ANY));
        vanilla.add(new ItemBuy(Items.ELYTRA.getDefaultStack(), "Элитры", ItemBuy.Category.ANY));

        {

            List<Enchant> eternityHelmetEnchants = EnchantContainer.parse("[EnchantCustom [checked=enchantments:impenetrable-enchant-custom, level=1], EnchantCustom [checked=minecraft:aqua_affinity, level=1], EnchantCustom [checked=minecraft:blast_protection, level=5], EnchantCustom [checked=minecraft:fire_protection, level=5], EnchantCustom [checked=minecraft:mending, level=1], EnchantCustom [checked=minecraft:projectile_protection, level=5], EnchantCustom [checked=minecraft:protection, level=5], EnchantCustom [checked=minecraft:respiration, level=3], EnchantCustom [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:aqua_affinity, level=1], EnchantVanilla [checked=minecraft:blast_protection, level=5], EnchantVanilla [checked=minecraft:fire_protection, level=5], EnchantVanilla [checked=minecraft:respiration, level=3], EnchantVanilla [checked=minecraft:projectile_protection, level=5], EnchantVanilla [checked=minecraft:protection, level=5], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy eternityHELMET = new EnchantItemBuy(Items.NETHERITE_HELMET.getDefaultStack(), "Шлем eternity", ItemBuy.Category.HOLLYWORLD);
            eternityHelmetEnchants.forEach(eternityHELMET::addEnchant);

            List<Enchant> eternityChestplateEnchants = EnchantContainer.parse("[EnchantCustom [checked=enchantments:impenetrable-enchant-custom, level=1], EnchantCustom [checked=minecraft:blast_protection, level=5], EnchantCustom [checked=minecraft:fire_protection, level=5], EnchantCustom [checked=minecraft:mending, level=1], EnchantCustom [checked=minecraft:projectile_protection, level=5], EnchantCustom [checked=minecraft:protection, level=5], EnchantCustom [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:projectile_protection, level=5], EnchantVanilla [checked=minecraft:protection, level=5], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:blast_protection, level=5], EnchantVanilla [checked=minecraft:fire_protection, level=5], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy eternityCHESTPLATE = new EnchantItemBuy(Items.NETHERITE_CHESTPLATE.getDefaultStack(), "Нагрудник eternity", ItemBuy.Category.HOLLYWORLD);
            eternityChestplateEnchants.forEach(eternityCHESTPLATE::addEnchant);


            EnchantItemBuy eternityLEGGINGS = new EnchantItemBuy(Items.NETHERITE_LEGGINGS.getDefaultStack(), "Штаны eternity", ItemBuy.Category.HOLLYWORLD);
            eternityChestplateEnchants.forEach(eternityLEGGINGS::addEnchant);

            List<Enchant> eternityBootsEnchants = EnchantContainer.parse("[EnchantCustom [checked=minecraft:blast_protection, level=5], EnchantCustom [checked=minecraft:depth_strider, level=3], EnchantCustom [checked=minecraft:feather_falling, level=4], EnchantCustom [checked=minecraft:fire_protection, level=5], EnchantCustom [checked=minecraft:mending, level=1], EnchantCustom [checked=minecraft:projectile_protection, level=5], EnchantCustom [checked=minecraft:protection, level=5], EnchantCustom [checked=minecraft:soul_speed, level=3], EnchantCustom [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:depth_strider, level=3], EnchantVanilla [checked=minecraft:blast_protection, level=5], EnchantVanilla [checked=minecraft:fire_protection, level=5], EnchantVanilla [checked=minecraft:projectile_protection, level=5], EnchantVanilla [checked=minecraft:protection, level=5], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:soul_speed, level=3], EnchantVanilla [checked=minecraft:feather_falling, level=4], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy eternityBoots = new EnchantItemBuy(Items.NETHERITE_BOOTS.getDefaultStack(), "Ботинки eternity", ItemBuy.Category.HOLLYWORLD);
            eternityBootsEnchants.forEach(eternityBoots::addEnchant);


            List<Enchant> eternitySwordEnchants = EnchantContainer.parse("[EnchantCustom [checked=enchantments:critical-enchant-custom, level=2], EnchantCustom [checked=enchantments:destroyer-enchant-custom, level=2], EnchantCustom [checked=enchantments:rich-enchant-custom, level=1], EnchantCustom [checked=minecraft:bane_of_arthropods, level=7], EnchantCustom [checked=minecraft:fire_aspect, level=2], EnchantCustom [checked=minecraft:looting, level=5], EnchantCustom [checked=minecraft:mending, level=1], EnchantCustom [checked=minecraft:sharpness, level=7], EnchantCustom [checked=minecraft:smite, level=7], EnchantCustom [checked=minecraft:sweeping, level=3], EnchantCustom [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:sweeping_edge, level=3], EnchantVanilla [checked=minecraft:bane_of_arthropods, level=7], EnchantVanilla [checked=minecraft:looting, level=5], EnchantVanilla [checked=minecraft:sharpness, level=7], EnchantVanilla [checked=minecraft:fire_aspect, level=2], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:smite, level=7], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy eternitySword = new EnchantItemBuy(Items.NETHERITE_SWORD.getDefaultStack(), "Меч eternity", ItemBuy.Category.HOLLYWORLD);
            eternitySwordEnchants.forEach(eternitySword::addEnchant);

            List<Enchant> eternityPickaxeEnchants = EnchantContainer.parse("[EnchantCustom [checked=enchantments:drill-enchant-custom, level=2], EnchantCustom [checked=enchantments:exp-enchant-custom, level=3], EnchantCustom [checked=enchantments:filter-enchant-custom, level=1], EnchantCustom [checked=enchantments:foundry-enchant-custom, level=1], EnchantCustom [checked=enchantments:internal-enchant-custom, level=1], EnchantCustom [checked=enchantments:magnet-enchant-custom, level=1], EnchantCustom [checked=minecraft:efficiency, level=10], EnchantCustom [checked=minecraft:fortune, level=5], EnchantCustom [checked=minecraft:mending, level=1], EnchantCustom [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:efficiency, level=10], EnchantVanilla [checked=minecraft:fortune, level=5], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy eternityPickaxe = new EnchantItemBuy(Items.NETHERITE_PICKAXE.getDefaultStack(), "Кирка eternity", ItemBuy.Category.HOLLYWORLD);
            eternityPickaxeEnchants.forEach(eternityPickaxe::addEnchant);

            List<Enchant> eternityCrossbowEnchants = EnchantContainer.parse("[EnchantCustom [checked=enchantments:stun-enchant-custom, level=2], EnchantCustom [checked=minecraft:multishot, level=1], EnchantCustom [checked=minecraft:piercing, level=5], EnchantCustom [checked=minecraft:quick_charge, level=3], EnchantCustom [checked=minecraft:unbreaking, level=3], EnchantVanilla [checked=minecraft:multishot, level=1], EnchantVanilla [checked=minecraft:piercing, level=5], EnchantVanilla [checked=minecraft:quick_charge, level=3], EnchantVanilla [checked=minecraft:unbreaking, level=3]]");
            EnchantItemBuy eternityCrossbow = new EnchantItemBuy(Items.CROSSBOW.getDefaultStack(), "Арбалет eternity", ItemBuy.Category.HOLLYWORLD);
            eternityCrossbowEnchants.forEach(eternityCrossbow::addEnchant);

            List<Enchant> eternityTridentEnchants = EnchantContainer.parse("[EnchantCustom [checked=minecraft:impaling, level=5], EnchantCustom [checked=minecraft:looting, level=5], EnchantCustom [checked=minecraft:loyalty, level=3], EnchantCustom [checked=minecraft:mending, level=1], EnchantCustom [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:loyalty, level=3], EnchantVanilla [checked=minecraft:looting, level=5], EnchantVanilla [checked=minecraft:impaling, level=5], EnchantVanilla [checked=minecraft:unbreaking, level=5], EnchantVanilla [checked=minecraft:mending, level=1]]");
            EnchantItemBuy eternityTrident = new EnchantItemBuy(Items.TRIDENT.getDefaultStack(), "Громовержец", ItemBuy.Category.HOLLYWORLD);
            eternityTridentEnchants.forEach(eternityTrident::addEnchant);





            SkinItemBuy cerberusSphere = new SkinItemBuy("Сфера Цербера", ItemBuy.Category.HOLLYWORLD, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjA5NWE3ZmQ5MGRhYTFiYmU3MDY5MDg5NzQwZTA1ZDBiZmM2NjI5NmVlM2M0MGVlNzFhNGUwYTY2MTZiMmJiYyJ9fX0=");
            SkinItemBuy fleshSphere = new SkinItemBuy("Сфера Флеша", ItemBuy.Category.HOLLYWORLD, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzc0MDBlYTE5ZGJkODRmNzVjMzlhZDY4MjNhYzRlZjc4NmYzOWY0OGZjNmY4NDYwMjM2NmFjMjliODM3NDIyIn19fQ==");
            SkinItemBuy imortaliti = new SkinItemBuy("Сфера Имморталити", ItemBuy.Category.HOLLYWORLD, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODNlZDRjZTIzOTMzZTY2ZTA0ZGYxNjA3MDY0NGY3NTk5ZWViNTUzMDdmN2VhZmU4ZDkyZjQwZmIzNTIwODYzYyJ9fX0=");
             ItemStack golubSphere = new SkinItemBuy("", ItemBuy.Category.HOLLYWORLD, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGM5MzY1NjQyYzZlZGRjZmVkZjViNWUxNGUyYmM3MTI1N2Q5ZTRhMzM2M2QxMjNjNmYzM2M1NWNhZmJmNmQifX19").getItemStack();
            NbtItemBuy damageSphere = new NbtItemBuy(golubSphere,"Сфера на урон 3", ItemBuy.Category.HOLLYWORLD, "sphereEffect","{\"lvl\":3,\"nbtName\":\"hms-damage\"}");
            NbtItemBuy speedSphere = new NbtItemBuy(golubSphere,"Сфера на Скорость 3", ItemBuy.Category.HOLLYWORLD, "sphereEffect","{\"lvl\":3,\"nbtName\":\"hms-speed\"}");
            NbtItemBuy eternitySphere = new NbtItemBuy(golubSphere,"Сфера eternity", ItemBuy.Category.HOLLYWORLD,"sphereEffect","{\"lvl\":2,\"nbtName\":\"hms-speed\"},{\"lvl\":2,\"nbtName\":\"hms-armor\"},{\"lvl\":2,\"nbtName\":\"hms-damage\"}");

            NbtItemBuy trapka = new NbtItemBuy(Items.POPPED_CHORUS_FRUIT.getDefaultStack(), "Трапка", ItemBuy.Category.HOLLYWORLD, "pyrotechnic-item", "ALTERNATIVE_TRAP");
            NbtItemBuy explosionTrapka = new NbtItemBuy(Items.PRISMARINE_SHARD.getDefaultStack(), "Взрывная Трапка", ItemBuy.Category.HOLLYWORLD, "pyrotechnic-item", "EXPLOSIVE_TRAP");
            NbtItemBuy stan = new NbtItemBuy(Items.NETHER_STAR.getDefaultStack(), "Стан", ItemBuy.Category.HOLLYWORLD, "pyrotechnic-item", "STUN_STAR");
            NbtItemBuy explosionBum = new NbtItemBuy(Items.FIRE_CHARGE.getDefaultStack(), "Взрывная штучка", ItemBuy.Category.HOLLYWORLD, "kringeItems", "ExplosiveStuff");
         //   NbtItemBuy bogAura = new NbtItemBuy(Items.PHANTOM_MEMBRANE.getDefaultStack(), "Божья аура", ItemBuy.Category.HOLLYWORLD, "godsaura", "1b");

            hollyworld.add(eternityHELMET);
            hollyworld.add(eternityCHESTPLATE);
            hollyworld.add(eternityLEGGINGS);
            hollyworld.add(eternityBoots);
            hollyworld.add(eternitySword);
            hollyworld.add(eternityPickaxe);
            hollyworld.add(eternityCrossbow);
            hollyworld.add(eternityTrident);
            hollyworld.add(cerberusSphere);
            hollyworld.add(fleshSphere);
            hollyworld.add(imortaliti);
            hollyworld.add(damageSphere);
            hollyworld.add(speedSphere);
            hollyworld.add(eternitySphere);
            hollyworld.add(trapka);
            hollyworld.add(explosionTrapka);
            hollyworld.add(stan);
            hollyworld.add(explosionBum);
//            hollyworld.add(bogAura);


        }


    }


}
