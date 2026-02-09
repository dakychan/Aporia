package zenith.zov.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import zenith.zov.Zenith;
import zenith.zov.base.autobuy.item.ItemBuy;
import zenith.zov.base.events.impl.input.EventKey;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.base.events.impl.server.EventPacket;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.ButtonSetting;
import zenith.zov.client.modules.api.setting.impl.ModeSetting;
import zenith.zov.client.screens.autobuy.items.AutoInventoryBuyScreen;
import zenith.zov.client.screens.autobuy.items.AutoInventoryItem;
import zenith.zov.utility.game.other.MessageUtil;
import zenith.zov.utility.game.server.AutoBuyUtil;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.math.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static zenith.zov.client.screens.autobuy.items.AutoInventoryBuyScreen.createItem;

@ModuleAnnotation(name = "AutoSbor", category = Category.MISC, description = "Автоматическим образом закупает инвентарь")
public final class AutoSbor extends Module {
    public static final AutoSbor INSTANCE = new AutoSbor();
    @Getter
    private ArrayList<AutoInventoryItem> zakup = new ArrayList<>();
    private ModeSetting mode = new ModeSetting("Мод");
    private ModeSetting.Value funtimeMode = new ModeSetting.Value(mode,"FunTime");
    private ModeSetting.Value hollyworldMode = new ModeSetting.Value(mode,"HollyWorld").select();
    private final ButtonSetting executeButton = new ButtonSetting("Открыть меню", () -> {
        mc.setScreen(new AutoInventoryBuyScreen(getLib(), zakup));
    });

    private int index = 0;

    private AutoSbor() {

    }

    private List<String> funtimeBypass = new ArrayList<>();
    private final Timer timerForAhCommand = new Timer();
    private final Timer timerForWaitBuy = new Timer();
    private final Timer timerForChangeStadia = new Timer();
    private int stadia = 0;
    private int sumTrati = 0;
    private int buyCount = 0;
    private int lastTrati = 0;
    private int lastCount = 0;
    private int baseInvCount = -1;
    @EventTarget
    public void update(EventUpdate event) {

        if(index>=zakup.size()) {
            this.toggle();
            return;
        }



        AutoInventoryItem currentItem = zakup.get(index);

        if (currentItem == null) {
            this.toggle();
            return;
        }

        if (funtimeMode.isSelected()) {
            if (mc.currentScreen == null) {
                if (timerForAhCommand.finished(500)) {
                    mc.getNetworkHandler().sendChatCommand("ah search " + currentItem.getItemBuy().getSearchName());
                    mc.player.setPitch((float) MathUtil.getRandom(-90, 90));
                    mc.player.setYaw((float) (mc.player.getYaw() + MathUtil.getRandom(-90, 90)));
                    timerForAhCommand.reset();
                }
                return;
            }
            if (!startSendUpdate.finished(400)) {
                return;
            }

            if (AutoBuyUtil.isWaitBuy(mc.player.currentScreenHandler)) {
                if (timerForWaitBuy.finished(400)) {
                    click(10, 0, SlotActionType.PICKUP);
                    timerForWaitBuy.reset();
                }
                return;
            }

            if (stadia == 0) {

                if (baseInvCount == -1) {
                    baseInvCount = getCountInInventory(currentItem);
                }

                for (int i = 0; i < 44; i++) {
                    Slot slot = mc.player.currentScreenHandler.getSlot(i);
                    ItemStack stack = slot.getStack();
                    if (currentItem.isBuy(stack)) {
                        this.funtimeBypass.add(AutoBuyUtil.getTagFuntimeNotTempElements(stack));
                    }
                }
                click(49, 0, SlotActionType.PICKUP);
                stadia = 1;
                timerForChangeStadia.reset();
                return;
            }

            if (stadia == 1 && timerForChangeStadia.finished(500)) {

                int gained = getCountInInventory(currentItem) - baseInvCount;
                if (gained < buyCount) {

                    buyCount = Math.max(0, buyCount - lastCount);
                    sumTrati = Math.max(0, sumTrati - lastTrati);
                }

                Slot goodSlot = null;
                double bestUnitPrice = Double.MAX_VALUE;
                int countBuy = currentItem.getCountBuy();

                for (int i = 0; i < 44; i++) {
                    Slot slot = mc.player.currentScreenHandler.getSlot(i);
                    ItemStack stack = slot.getStack();

                    if (funtimeBypass.contains(AutoBuyUtil.getTagFuntimeNotTempElements(stack))) {
                        int price = AutoBuyUtil.getPrice(stack);
                        int countItem = stack.getCount();


                        if (countItem > Math.max(1, countBuy) - buyCount || price > currentItem.getMaxSumBuy() - sumTrati) {
                            continue;
                        }

                        double unitPrice = (double) price / countItem;
                        if (unitPrice < bestUnitPrice) {
                            bestUnitPrice = unitPrice;
                            goodSlot = slot;
                        }
                    }
                }

                if (goodSlot != null) {
                    int price = AutoBuyUtil.getPrice(goodSlot.getStack());
                    lastTrati = price;
                    lastCount = goodSlot.getStack().getCount();

                    sumTrati += price;
                    buyCount += lastCount;

                    timerForChangeStadia.reset();
                    click(goodSlot.id, 0, SlotActionType.PICKUP_ALL);
                    MessageUtil.displayMessage(MessageUtil.LogLevel.INFO,
                            "Пробуем " + currentItem.getItemBuy().getSearchName());
                } else {

                    int totalGained = Math.max(0, getCountInInventory(currentItem) - baseInvCount);
                    boolean success = totalGained > 0;
                    MessageUtil.displayMessage(success ? MessageUtil.LogLevel.INFO : MessageUtil.LogLevel.WARN,
                            (success ? "Куплено: +" : "Не куплено: +") + totalGained + " " + currentItem.getItemBuy().getSearchName());

                    sumTrati = 0;
                    stadia = 0;
                    buyCount = 0;
                    lastCount = 0;
                    lastTrati = 0;
                    baseInvCount = -1;
                    funtimeBypass.clear();
                    mc.player.closeScreen();
                    timerForAhCommand.reset();
                    index++;
                }
            }
        }else {

        }

    }
    Timer startSendUpdate = new Timer();
    @EventTarget
    public void packet(EventPacket event) {
        if(event.isReceive()){
            if(event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket screenHandlerSlotUpdateS2CPacket||event.getPacket() instanceof OpenScreenS2CPacket){
                startSendUpdate.reset();
            }
        }
        if(event.getPacket() instanceof ClickSlotC2SPacket clickSlotC2SPacket){
          if (clickSlotC2SPacket.slot() == 50) {
              click(48,0,SlotActionType.PICKUP);
          }
        }
    }
    public int getCountInInventory(AutoInventoryItem item) {
        int count = 0;
        for (ItemStack slot : mc.player.getInventory().getMainStacks()){
            if((!slot.isEmpty() &&item.isBuy(slot)) ){
                count+=slot.getCount();
            }
        }
        return count;
    }
    @Override
    public void onEnable() {
        if(mc.player==null){
            this.setEnabled(false);
            return;
        }

        if(mc.currentScreen!=null){
            mc.player.closeHandledScreen();
        }
        timerForAhCommand.reset();
        super.onEnable();
        MessageUtil.displayInfo("ВРЕМЕННО НЕ РАБОТАЕТ");
        this.toggle();
    }

    @Override
    public void onDisable() {
        index = 0;
        sumTrati = 0;
        stadia = 0;
        funtimeBypass.clear();
        timerForAhCommand.reset();
        buyCount = 0;
        super.onDisable();
    }

    @Override
    public JsonObject save() {
        JsonObject object = super.save();
        JsonObject propertiesObject = new JsonObject();

        for (AutoInventoryItem autoInventoryItem: this.zakup) {
            propertiesObject.add(autoInventoryItem.getItemBuy().getSearchName()+autoInventoryItem.getItemBuy().getDisplayName()+autoInventoryItem.getItemBuy().getCategory().name(), autoInventoryItem.save());
        }

        object.add("AutoInventoryItems", propertiesObject);
        return object;
    }

    @Override
    public void load(JsonObject object) {
        super.load(object);
        this.zakup.clear();

        if (object.has("AutoInventoryItems") && object.get("AutoInventoryItems").isJsonObject()) {
            JsonObject propertiesObject = object.getAsJsonObject("AutoInventoryItems");

            for (Map.Entry<String, JsonElement> entry : propertiesObject.entrySet()) {
                String key = entry.getKey();
                JsonObject itemJson = entry.getValue().getAsJsonObject();


                ItemBuy itemBuy = findItemBuyByKey(key);
                if (itemBuy == null) {

                    continue;
                }

                AutoInventoryItem autoInventoryItem = (createItem(itemBuy));
                autoInventoryItem.load(itemJson);
                this.zakup.add(autoInventoryItem);
            }
        }
    }

    private void click(int slotId, int buttonId,SlotActionType slotActionType) {
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slotId, buttonId,slotActionType , mc.player);

    }
    private ItemBuy findItemBuyByKey(String key) {

        for (ItemBuy item : getLib()) {
            String generatedKey = item.getSearchName() + item.getDisplayName() + item.getCategory().name();
            if (generatedKey.equals(key)) {
                return item;
            }
        }
        return null;
    }
    private ArrayList<ItemBuy> getLib(){
        ArrayList<ItemBuy> allItemBuys = new ArrayList<>(funtimeMode.isSelected()?Zenith.getInstance().getAutoBuyManager().getFuntime():Zenith.getInstance().getAutoBuyManager().getHollyworld());
        allItemBuys.addAll(Zenith.getInstance().getAutoBuyManager().getVanilla());
        return allItemBuys;
    }
    @EventTarget
    public void onKey(EventKey event) {
        this.toggle();
    }
}
