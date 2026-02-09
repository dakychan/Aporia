package aporia.cc.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import aporia.cc.base.events.impl.render.EventRender3D;
import aporia.cc.base.events.impl.server.EventPacket;
import aporia.cc.client.modules.api.Category;
import aporia.cc.client.modules.api.Module;
import aporia.cc.client.modules.api.ModuleAnnotation;
import aporia.cc.client.modules.api.setting.impl.NumberSetting;
import aporia.cc.utility.math.Timer;
import aporia.cc.utility.render.level.Render3DUtil;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
@ModuleAnnotation(name = "BaseFinder",description = "ищет базы",category = Category.MISC)
public final class BaseFinder extends Module {
    private final NumberSetting range = new NumberSetting("Радиус", 80, 1, 128, 2);
    private final NumberSetting time = new NumberSetting("Таймер", 4, 0, 100, 5);

    private final Timer timerUtil = new Timer();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean isStarted = true;

    public static BaseFinder INSTANCE = new BaseFinder();
    private BaseFinder() {

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

        if (timerUtil.finished(time.getCurrent() * 1000)  && isStarted) {
            executor.submit(this::scan);
            timerUtil.reset();

        }



    }

    @EventTarget
    public void packer(EventPacket eventPacket) {

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

                    if (!(block instanceof AirBlock) ) {
                        blocks.add(pos);
                    }


                }
            }
        }
        isStarted = true;
    }

}

