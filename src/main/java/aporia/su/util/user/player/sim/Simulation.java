package aporia.su.util.user.player.sim;

import net.minecraft.util.math.Vec3d;

public interface Simulation {
    Vec3d pos();

    void tick();
}