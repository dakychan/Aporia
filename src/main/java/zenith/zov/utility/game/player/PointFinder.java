package zenith.zov.utility.game.player;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import zenith.zov.utility.game.player.rotation.Rotation;
import zenith.zov.utility.game.player.rotation.RotationDelta;
import zenith.zov.utility.game.player.rotation.RotationUtil;
import zenith.zov.utility.interfaces.IMinecraft;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Getter
public class PointFinder implements IMinecraft {
    // Чем меньше — тем меньше фпс потому что виноват Большой
    private static final double MIN_GRID_SPACING = 0.15;
    private static final int MAX_STEPS_XZ = 14;
    private static final int STEPS_Y = 10;

    private final Random random = new SecureRandom();
    private Vec3d offset = Vec3d.ZERO;

    public Pair<Vec3d, Box> computeVector(LivingEntity entity,
                                          float maxDistance,
                                          Rotation initialRotation,
                                          Vec3d velocity,
                                          boolean ignoreWalls) {
        Pair<List<Vec3d>, Box> candidatePoints = generateCandidatePoints(entity, maxDistance, ignoreWalls);


        List<Vec3d> suitable = filterSuitable(candidatePoints.getLeft(), maxDistance);


        Vec3d bestPoint = findValidCenterOrNearest(suitable, maxDistance, ignoreWalls);
        if (bestPoint == null) {
            bestPoint = findValidCenterOrNearest(candidatePoints.getLeft(), maxDistance, ignoreWalls);
        }

        if (bestPoint == null) {
            bestPoint = findBestVectorByDistance(candidatePoints.getLeft());
        }

        updateOffset(velocity);
        Vec3d result = (bestPoint == null ? entity.getEyePos() : bestPoint).add(offset);
        return new Pair<>(result, candidatePoints.getRight());
    }

    public Pair<List<Vec3d>, Box> generateCandidatePoints(LivingEntity entity,
                                                          float maxDistance,
                                                          boolean ignoreWalls) {
        Box box = entity.getBoundingBox();
        Vec3d eye = mc.player.getEyePos();

        double lenX = box.getLengthX();
        double lenY = box.getLengthY();
        double lenZ = box.getLengthZ();

        int stepsX = computeSteps(lenX);
        int stepsZ = computeSteps(lenZ);

        int ySteps = Math.max(2, STEPS_Y);
        double stepY = lenY / (ySteps - 1);

        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;

        double stepX = stepsX <= 1 ? 0 : lenX / (stepsX - 1);
        double stepZ = stepsZ <= 1 ? 0 : lenZ / (stepsZ - 1);

        List<Vec3d> list = new ArrayList<>(ySteps * stepsX * stepsZ);

        for (int iy = 0; iy < ySteps; iy++) {
            double y = minY + iy * stepY;

            for (int ix = 0; ix < stepsX; ix++) {
                double x = minX + ix * stepX;

                for (int iz = 0; iz < stepsZ; iz++) {
                    double z = minZ + iz * stepZ;

                    Vec3d p = new Vec3d(x, y, z);
                    if (isValidPoint(eye, p, maxDistance, ignoreWalls)) {
                        list.add(p);
                    }
                }
            }
        }

        return new Pair<>(list, box);
    }

    public boolean hasValidPoint(LivingEntity entity,
                                 float maxDistance,
                                 boolean ignoreWalls) {
        Box box = entity.getBoundingBox();
        Vec3d eye = mc.player.getEyePos();

        double lenX = box.getLengthX();
        double lenY = box.getLengthY();
        double lenZ = box.getLengthZ();

        int stepsX = computeSteps(lenX);
        int stepsZ = computeSteps(lenZ);

        int ySteps = Math.max(2, STEPS_Y);
        double stepY = lenY / (ySteps - 1);

        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;

        double stepX = stepsX <= 1 ? 0 : lenX / (stepsX - 1);
        double stepZ = stepsZ <= 1 ? 0 : lenZ / (stepsZ - 1);

        for (int iy = 0; iy < ySteps; iy++) {
            double y = minY + iy * stepY;

            for (int ix = 0; ix < stepsX; ix++) {
                double x = minX + ix * stepX;

                for (int iz = 0; iz < stepsZ; iz++) {
                    double z = minZ + iz * stepZ;

                    Vec3d p = new Vec3d(x, y, z);
                    if (isValidPoint(eye, p, maxDistance, ignoreWalls)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isValidPoint(Vec3d startPoint,
                                 Vec3d endPoint,
                                 float maxDistance,
                                 boolean ignoreWalls) {
        if (startPoint.squaredDistanceTo(endPoint) > (double) maxDistance * maxDistance) {
            return false;
        }
        if (ignoreWalls) return true;

        var hit = RaytracingUtil.raycast(startPoint, endPoint, RaycastContext.ShapeType.COLLIDER);
        return hit.getType() != HitResult.Type.BLOCK;
    }


    private Vec3d findValidCenterOrNearest(List<Vec3d> points,
                                           float maxDistance,
                                           boolean ignoreWalls) {
        if (points == null || points.isEmpty()) return null;

        Vec3d eye = mc.player.getEyePos();


        Vec3d centroid = computeCentroid(points);


        if (isValidPoint(eye, centroid, maxDistance, ignoreWalls)) {
            return centroid;
        }

       return points.stream()
                .filter(p -> isValidPoint(eye, p, maxDistance, ignoreWalls))
                .min(Comparator.comparingDouble(p -> p.squaredDistanceTo(centroid)))
                .orElse(null);
    }

      private List<Vec3d> filterSuitable(List<Vec3d> points, float maxDistance) {
        if (points == null || points.isEmpty()) return List.of();
        Vec3d eye = mc.player.getEyePos();
        double tight = Math.max(0.0, maxDistance - 0.3);
        double tightSq = tight * tight;
        List<Vec3d> suitable = new ArrayList<>();
        for (Vec3d p : points) {
            if (eye.squaredDistanceTo(p) < tightSq) {
                suitable.add(p);
            }
        }
        return suitable;
    }

    private Vec3d computeCentroid(List<Vec3d> points) {
        double sx = 0, sy = 0, sz = 0;
        int n = points.size();
        for (Vec3d p : points) {
            sx += p.x;
            sy += p.y;
            sz += p.z;
        }
        return new Vec3d(sx / n, sy / n, sz / n);
    }

    private Vec3d findBestVectorByDistance(List<Vec3d> candidatePoints) {
        if (candidatePoints == null || candidatePoints.isEmpty()) return null;
        Vec3d eye = mc.player.getEyePos();
        return candidatePoints.stream()
                .min(Comparator.comparingDouble(p -> p.squaredDistanceTo(eye)))
                .orElse(null);
    }

    private void updateOffset(Vec3d velocity) {
        offset = offset.add(
                        random.nextGaussian(),
                        random.nextGaussian(),
                        random.nextGaussian())
                .multiply(velocity);
    }

    @SuppressWarnings("unused")
    private double calculateRotationDifference(Vec3d startPoint, Vec3d endPoint, Rotation initialRotation) {
        if (initialRotation == null) return Double.POSITIVE_INFINITY;
        var targetRotation = RotationUtil.fromVec3d(endPoint.subtract(startPoint));
        RotationDelta delta = initialRotation.rotationDeltaTo(targetRotation);
        return Math.hypot(delta.getDeltaYaw(), delta.getDeltaPitch());
    }

    private int computeSteps(double length) {
        if (length <= 0) return 1;
        int bySpacing = (int) Math.ceil(length / MIN_GRID_SPACING) + 1;
        int steps = Math.min(bySpacing, MAX_STEPS_XZ);
        return Math.max(2, steps);
    }
}
