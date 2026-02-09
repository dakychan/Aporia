package zenith.zov.client.modules.impl.misc;


import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import org.apache.commons.lang3.time.StopWatch;
import zenith.zov.Zenith;
import zenith.zov.base.events.impl.input.EventKey;
import zenith.zov.base.events.impl.player.EventRotate;
import zenith.zov.base.events.impl.render.EventRender3D;
import zenith.zov.base.rotation.RotationTarget;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.Setting;
import zenith.zov.client.modules.api.setting.impl.KeySetting;
import zenith.zov.client.modules.impl.render.Predictions;
import zenith.zov.utility.game.player.PlayerIntersectionUtil;
import zenith.zov.utility.game.player.PlayerInventoryComponent;
import zenith.zov.utility.game.player.PlayerInventoryUtil;
import zenith.zov.utility.game.player.SimulatedPlayer;
import zenith.zov.utility.game.player.rotation.Rotation;
import zenith.zov.utility.game.player.rotation.RotationUtil;
import zenith.zov.utility.math.Timer;
import zenith.zov.utility.other.BooleanSettable;

import java.util.ArrayList;
import java.util.List;


@ModuleAnnotation(name = "ClickAction",description = "Делает что то по бинду",category = Category.MISC)
public final class ClickAction extends Module {


    private final KeySetting friendBind = new KeySetting("Добавить друга");
    private final KeySetting expBind = new KeySetting("Пузырек опыта");

    private final List<KeyBind> keyBindings = new ArrayList<>();
    private final Timer timer = new Timer();
    public static final ClickAction INSTANCE = new ClickAction();
    private ClickAction() {

        keyBindings.add(new KeyBind(Items.ENDER_PEARL, new KeySetting("Эндер перл"), new BooleanSettable()));
        keyBindings.add(new KeyBind(Items.WIND_CHARGE, new KeySetting("Заряд ветра"), new BooleanSettable()));


    }

    @Override
    public List<Setting> getSettings() {
        ArrayList<Setting> settings = new ArrayList<>();
        settings.add(expBind);
        settings.add(friendBind);
        settings.addAll(keyBindings.stream().map(KeyBind::setting).toList());
        return settings;
    }

    @EventTarget
    public void onKey(EventKey e) {
        if (e.isKeyDown(friendBind.getKeyCode()) && mc.crosshairTarget instanceof EntityHitResult result && result.getEntity() instanceof PlayerEntity player) {
            if (Zenith.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())) Zenith.getInstance().getFriendManager().removeFriend(player.getGameProfile().getName());
            else Zenith.getInstance().getFriendManager().add(player.getGameProfile().getName());
        }
        keyBindings.stream().filter(bind -> e.isKeyDown(bind.setting.getKeyCode()) && PlayerInventoryUtil.getSlot(bind.item) != null).forEach(bind -> bind.draw.setValue(true));
        keyBindings.stream().filter(bind -> e.isKeyReleased(bind.setting.getKeyCode())).forEach(bind -> {
           PlayerInventoryUtil.swapAndUse(bind.item);

            bind.draw.setValue(false);
        });
        if(e.isKeyDown(expBind.getKeyCode())) {
            Slot slot = PlayerInventoryUtil.getSlot(Items.EXPERIENCE_BOTTLE);

            if (slot == null) {
                Zenith.getInstance().getNotifyManager().addNotification( "M", Text.of(Items.EXPERIENCE_BOTTLE.getName().copy().setStyle(Style.EMPTY.withColor(zenith.getThemeManager().getCurrentTheme().getColor().getRGB())).append(Text.of("не найден").copy().setStyle(Style.EMPTY.withColor(zenith.getThemeManager().getCurrentTheme().getWhite().getRGB())))));

                return;
            }
        }
    }

    @EventTarget
    public void onWorldRender(EventRender3D e) {
        Predictions.INSTANCE.drawPredictionInHand(e.getMatrix(), keyBindings.stream().filter(keyBind -> keyBind.draw.isValue()).map(keyBind -> keyBind.item.getDefaultStack()).toList());
    }
    private Slot saveSlot = null;
    @EventTarget
    public void onTick(EventRotate e) {
        boolean isMainHandItem = mc.player.getMainHandStack().getItem().equals(Items.EXPERIENCE_BOTTLE);
        if (PlayerIntersectionUtil.isKey(expBind) ) {
            Slot slot = PlayerInventoryUtil.getSlot(Items.EXPERIENCE_BOTTLE);


            SimulatedPlayer simulatedPlayer = SimulatedPlayer.simulateLocalPlayer(3);
            Rotation angle =new Rotation(mc.player.getYaw(), RotationUtil.calculateAngle(simulatedPlayer.boundingBox.getCenter()).getPitch());

            rotationManager.setRotation(new RotationTarget(angle,()->{
                return aimManager.rotate(aimManager.getInstantSetup(), angle);
            }, aimManager.getInstantSetup()),1,this);
            if (!isMainHandItem) {

                PlayerInventoryComponent.addTask(()->{
                    if(saveSlot==null) {
                        saveSlot = slot;
                   }
                    PlayerInventoryUtil.swapHand(slot, Hand.MAIN_HAND, false);
                    PlayerInventoryUtil.closeScreen(true);
                });

            } else if (timer.finished(70)&&rotationManager.getCurrentRotation().rotationDeltaTo(angle).isInRange(180,10) ) {
                PlayerIntersectionUtil.useItem(Hand.MAIN_HAND, angle);
                timer.reset();
            }
        }else {
            if(saveSlot!=null){

                PlayerInventoryComponent.addTask(()->{

                    if (!PlayerIntersectionUtil.isKey(expBind) ) {
                        PlayerInventoryUtil.swapHand(saveSlot, Hand.MAIN_HAND, false);
                        PlayerInventoryUtil.closeScreen(true);
                        saveSlot = null;
                    }
                });
            }
        }
    }

    public record KeyBind(Item item, KeySetting setting, BooleanSettable draw) {
    }
}