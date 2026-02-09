package zenith.zov.utility.game.player.rotation;

import lombok.experimental.UtilityClass;
import net.minecraft.util.math.Vec3d;
import zenith.zov.utility.interfaces.IMinecraft;

import static java.lang.Math.hypot;
import static java.lang.Math.toDegrees;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

@UtilityClass
public class RotationUtil implements IMinecraft {
    public Rotation getClientRotation() {
        return new Rotation(mc.player.getYaw(), mc.player.getPitch());
    }
    public Rotation fromVec3d(Vec3d vector) {
        return new Rotation((float) wrapDegrees(toDegrees(Math.atan2(vector.z, vector.x)) - 90), (float) wrapDegrees(toDegrees(-Math.atan2(vector.y, hypot(vector.x, vector.z)))));
    }
    public Rotation calculateAngle(Vec3d to) {
        return fromVec3d(to.subtract(mc.player.getEyePos()));
    }

}
