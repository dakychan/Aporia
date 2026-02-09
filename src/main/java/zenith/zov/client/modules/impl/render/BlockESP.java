package zenith.zov.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import zenith.zov.base.events.impl.render.EventRender3D;
import zenith.zov.base.events.impl.server.EventPacket;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.ItemSelectSetting;
import zenith.zov.client.modules.api.setting.impl.ModeSetting;
import zenith.zov.client.modules.api.setting.impl.NumberSetting;
import zenith.zov.utility.math.Timer;
import zenith.zov.utility.render.level.Render3DUtil;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ModuleAnnotation(name = "BlockESP", category = Category.RENDER, description = "Подсвечивает блоки")
public final class BlockESP extends Module {


    private volatile List<BlockPos> blockPos = new ArrayList<>();
    private final ItemSelectSetting itemSelectSetting = new ItemSelectSetting("Блоки", new ArrayList<>());
    private ModeSetting mode = new ModeSetting("Мод");
    private ModeSetting.Value onlyUpdate = new ModeSetting.Value(mode,"Обновление").select();
    private ModeSetting.Value all = new ModeSetting.Value(mode,"Сканировать");

    private final NumberSetting range = new NumberSetting("Радиус", 80, 1, 128, 2,all::isSelected);
    private final NumberSetting time = new NumberSetting("Таймер", 4, 0, 100, 5,all::isSelected);

    private final Timer timerUtil = new Timer();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean isStarted = true;

    public static BlockESP INSTANCE = new BlockESP();
    private BlockESP() {
        itemSelectSetting.add(Blocks.DIAMOND_ORE);
    }
    @Override
    public void onEnable() {

        timerUtil.setMillis(0);
        isStarted = true;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        isStarted = false;
        super.onDisable();
    }

    @EventTarget
    public void render(EventRender3D eventRender3D) {

        if (timerUtil.finished(time.getCurrent() * 1000) && !onlyUpdate.isSelected() && isStarted) {
            executor.submit(this::scan);
            timerUtil.reset();

        }

        for (BlockPos pos : blockPos) {

            Block block = mc.world.getBlockState(pos).getBlock();


            if (itemSelectSetting.contains(block)) {
                if ((block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)) {
                    drawBox(pos, Color.cyan.getRGB());
                } else if ((block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE)) {
                    drawBox(pos, 0xFFFFD700);
                } else if (block == Blocks.NETHER_GOLD_ORE) {
                    drawBox(pos, 0xFFFFD700);
                } else if ((block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE)) {
                    drawBox(pos, 0xFF00FF4D);
                } // (0, 255, 77)


                else if ((block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE)) {
                    drawBox(pos, 0xFFD5D5D5);
                } // (213, 213, 213)


                else if ((block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE)) {
                    drawBox(pos, 0xFFFF0000);
                } // (255, 0, 0)


                else if (block == Blocks.ANCIENT_DEBRIS) {
                    drawBox(pos, 0xFFFFFFFF);
                } // (255, 255, 255)}
                else {
                    MapColor color = block.getDefaultMapColor();
                    int rgb =new Color(color.color).getRGB();

                    drawBox(pos,rgb);

                }


            }

        }


    }

    @EventTarget
    public void packer(EventPacket eventPacket) {
        if (eventPacket.isReceive()) {
            if (eventPacket.getPacket() instanceof ChunkDeltaUpdateS2CPacket chunkDelta)
                chunkDelta.visitUpdates((pos, state) -> {
                    if(itemSelectSetting.contains(state.getBlock())) {
                        blockPos.add(pos);
                    }
                });
            if (eventPacket.getPacket() instanceof BlockUpdateS2CPacket chunkDelta) {

                if (itemSelectSetting.contains(chunkDelta.getState ().getBlock())){
                    blockPos.add(chunkDelta.getPos());
                }
            }

        }
    }

    public void drawBox(BlockPos blockPos, int start) {
        Render3DUtil.drawBox(new Box(blockPos),start, 1);
    }

    private void scan() {

        ArrayList<BlockPos> blocks = new ArrayList<>();
        int startX = (int) Math.floor(mc.player.getX() - range.getCurrent());
        int endX = (int) Math.ceil(mc.player.getX() + range.getCurrent());
        int startY = mc.world.getBottomY() + 1;
        int endY = mc.world.getTopYInclusive();
        int startZ = (int) Math.floor(mc.player.getZ() - range.getCurrent());
        int endZ = (int) Math.ceil(mc.player.getZ() + range.getCurrent());

        for (int x = startX; x <= endX; x++) {

            for (int y = startY; y <= endY; y++) {

                for (int z = startZ; z <= endZ; z++) {
                    if (!isStarted) return;
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();

                    if (!(block instanceof AirBlock) && itemSelectSetting.contains(block)) {
                        blocks.add(pos);
                    }


                }
            }
        }
        this.blockPos = blocks;
        isStarted = true;
    }


}
