package aporia.su.modules.impl.player;

import anidumpproject.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import aporia.su.util.events.api.EventHandler;
import aporia.su.util.events.impl.TickEvent;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.MultiSelectSetting;
import aporia.su.modules.module.setting.implement.SelectSetting;
import aporia.su.modules.module.setting.implement.SliderSettings;
import aporia.su.util.user.player.inventory.InventoryUtils;
import aporia.su.util.user.player.timer.StopWatch;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChestStealer extends ModuleStructure {
    StopWatch stopWatch = new StopWatch();

    SelectSetting modeSetting = new SelectSetting("Тип", "Выбирает тип стила")
            .value("FunTime", "WhiteList", "Default").selected("FunTime");

    SliderSettings delaySetting = new SliderSettings("Задержка", "Задержка между кликами по слоту")
            .setValue(100).range(0, 1000)
            .visible(() -> modeSetting.isSelected("WhiteList") || modeSetting.isSelected("Default"));

    MultiSelectSetting itemSettings = new MultiSelectSetting("Предметы", "Выберите предметы, которые вор будет подбирать")
            .value("Player Head", "Totem Of Undying", "Elytra", "Netherite Sword",
                    "Netherite Helmet", "Netherite ChestPlate", "Netherite Leggings",
                    "Netherite Boots", "Netherite Ingot", "Netherite Scrap")
            .visible(() -> modeSetting.isSelected("WhiteList"));

    public ChestStealer() {
        super("ChestStealer", "Chest Stealer", ModuleCategory.PLAYER);
        settings(modeSetting, delaySetting, itemSettings);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null) return;

        switch (modeSetting.getSelected()) {
            case "FunTime" -> handleFunTimeMode();
            case "WhiteList", "Default" -> handleDefaultMode();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleFunTimeMode() {
        if (mc.currentScreen instanceof GenericContainerScreen sh
                && sh.getTitle().getString().toLowerCase().contains("мистический")
                && !mc.player.getItemCooldownManager().isCoolingDown(Items.GUNPOWDER.getDefaultStack())) {

            sh.getScreenHandler().slots.stream()
                    .filter(s -> s.hasStack()
                            && !s.inventory.equals(mc.player.getInventory())
                            && stopWatch.every(150))
                    .forEach(s -> InventoryUtils.click(s.id, 0, SlotActionType.QUICK_MOVE));
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleDefaultMode() {
        if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler sh) {
            sh.slots.forEach(s -> {
                if (s.hasStack()
                        && !s.inventory.equals(mc.player.getInventory())
                        && (modeSetting.isSelected("Default") || whiteList(s.getStack().getItem()))
                        && stopWatch.every(delaySetting.getValue())) {
                    InventoryUtils.click(s.id, 0, SlotActionType.QUICK_MOVE);
                }
            });
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean whiteList(Item item) {
        return itemSettings.getSelected().toString().toLowerCase()
                .contains(item.toString().toLowerCase().replace("_", ""));
    }
}