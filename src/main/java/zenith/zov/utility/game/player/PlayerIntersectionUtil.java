package zenith.zov.utility.game.player;

import lombok.experimental.UtilityClass;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import zenith.zov.Zenith;
import zenith.zov.client.modules.api.setting.impl.KeySetting;
import zenith.zov.utility.game.player.rotation.Rotation;
import zenith.zov.utility.game.player.rotation.RotationUtil;
import zenith.zov.utility.interfaces.IClient;
import zenith.zov.utility.render.display.base.color.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@UtilityClass
public class PlayerIntersectionUtil implements IClient {
    public void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        mc.interactionManager.sendSequencedPacket(mc.world, packetCreator);
    }

    public void startFallFlying() {
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        mc.player.startGliding();
    }

    public void sendPacketWithOutEvent(Packet<?> packet) {
        mc.getNetworkHandler().getConnection().send(packet, null);
    }

  //  public String getHealthString(LivingEntity entity) {
//        return getHealthString(getHealth(entity));
//    }
//
//    public String getHealthString(float hp) {
//        return String.format("%.1f", hp).replace(",",".").replace(".0","");
//    }

//    public float getHealth(LivingEntity entity) {
//
//        float hp = entity.getHealth() + entity.getAbsorptionAmount();
//        if (entity instanceof PlayerEntity player) switch (ServerUtil.server) {
//            case "FunTime", "ReallyWorld" -> {
//                ScoreboardObjective scoreBoard = player.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
//                if (scoreBoard != null) {
//                    MutableText text2 = ReadableScoreboardScore.getFormattedScore(player.getScoreboard().getScore(player, scoreBoard), scoreBoard.getNumberFormatOr(StyledNumberFormat.EMPTY));
//                    try {
//                        hp = Float.parseFloat(ColorUtility.removeFormatting(text2.getString()));
//                    } catch (NumberFormatException ignored) {}
//                }
//            }
//        }
//        return MathHelper.clamp(hp,0,entity.getMaxHealth());
//    }

    public List<BlockPos> getCube(BlockPos center, float radius) {
        return getCube(center, radius,radius,true);
    }

    public List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY) {
        return getCube(center,radiusXZ,radiusY,true);
    }

    public List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY, boolean down) {
        List<BlockPos> positions = new ArrayList<>();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();
        int posY = down ? centerY - (int) radiusY : centerY;

        for (int x = centerX - (int) radiusXZ; x <= centerX + radiusXZ; x++) {
            for (int z = centerZ - (int) radiusXZ; z <= centerZ + radiusXZ; z++) {
                for (int y = posY; y <= centerY + radiusY; y++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }

        return positions;
    }

    public List<BlockPos> getCube(BlockPos start, BlockPos end) {
        List<BlockPos> positions = new ArrayList<>();

        for (int x = start.getX(); x <= end.getX(); x++) {
            for (int z = start.getZ(); z <= end.getZ(); z++) {
                for (int y = start.getY(); y <= end.getY(); y++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }

        return positions;
    }

    public InputUtil.Type getKeyType(int key) {
        return key < 8 ? InputUtil.Type.MOUSE : InputUtil.Type.KEYSYM;
    }

    public Stream<Entity> streamEntities() {
        return StreamSupport.stream(mc.world.getEntities().spliterator(), false);
    }

    public boolean canChangeIntoPose(EntityPose pose) {
        return mc.player.getWorld().isSpaceEmpty(mc.player, mc.player.getDimensions(pose).getBoxAt(mc.player.getPos()).contract(1.0E-7));
    }

    public boolean isPotionActive(RegistryEntry<StatusEffect> statusEffect) {
        return mc.player.getActiveStatusEffects().containsKey(statusEffect);
    }

    public boolean isPlayerInBlock(Block block) {
        return isBoxInBlock(mc.player.getBoundingBox().expand(-1e-3), block);
    }

    public boolean isBoxInBlock(Box box, Block block) {
        return isBox(box,pos -> mc.world.getBlockState(pos).getBlock().equals(block));
    }

    public boolean isBoxInBlocks(Box box, List<Block> blocks) {
        return isBox(box,pos -> blocks.contains(mc.world.getBlockState(pos).getBlock()));
    }

    public boolean isBox(Box box, Predicate<BlockPos> pos) {
        return BlockPos.stream(box).anyMatch(pos);
    }



    public boolean isKey(InputUtil.Key key) {
        return isKey(key.getCategory(), key.getCode());
    }
    public boolean isKey(KeySetting setting) {
        int key = setting.getKeyCode();
        return mc.currentScreen == null && setting.isVisible() && isKey(getKeyType(key), key);
    }
    public boolean isKey(InputUtil.Type type, int keyCode) {
        if (keyCode != -1) switch (type) {
            case InputUtil.Type.KEYSYM: return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == 1;
            case InputUtil.Type.MOUSE: return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == 1;
        }
        return false;
    }

    public boolean isAir(BlockPos blockPos) {
       return isAir(mc.world.getBlockState(blockPos));
    }

    public boolean isAir(BlockState state) {
        return state.isAir() || state.getBlock().equals(Blocks.CAVE_AIR) || state.getBlock().equals(Blocks.VOID_AIR);
    }

    public boolean isChat(Screen screen) {return screen instanceof ChatScreen;}
    public boolean nullCheck() {return mc.player == null || mc.world == null;}
    public void useItem(Hand hand) {
        useItem(hand, rotationManager.getCurrentRotation());
    }

    public void useItem(Hand hand, Rotation angle) {
        sendSequencedPacket(i -> new PlayerInteractItemC2SPacket(hand, i, angle.getYaw(), angle.getPitch()));
    }

    public float getHealth(LivingEntity entity) {
        float hp = entity.getHealth() + entity.getAbsorptionAmount();
        if (entity instanceof PlayerEntity player) switch (Zenith.getInstance().getServerHandler().getServer()) {
            case "FunTime", "ReallyWorld" -> {
                ScoreboardObjective scoreBoard = player.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
                if (scoreBoard != null) {
                    MutableText text2 = ReadableScoreboardScore.getFormattedScore(player.getScoreboard().getScore(player, scoreBoard), scoreBoard.getNumberFormatOr(StyledNumberFormat.EMPTY));
                    try {

                        hp = Float.parseFloat(ColorUtil.removeFormatting(text2.getString()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return MathHelper.clamp(hp,0,entity.getMaxHealth());
    }

    public String getHealthString(LivingEntity entity) {
        return getHealthString(getHealth(entity));
    }

    public String getHealthString(float hp) {
        return String.format("%.1f", hp).replace(",",".").replace(".0","");
    }
}
