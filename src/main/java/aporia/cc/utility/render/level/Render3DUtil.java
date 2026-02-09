package aporia.cc.utility.render.level;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import aporia.cc.utility.interfaces.IMinecraft;
import aporia.cc.utility.math.ProjectionUtil;
import aporia.cc.utility.render.display.RenderLayerUtil;
import aporia.cc.utility.render.display.base.color.ColorUtil;


import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class Render3DUtil implements IMinecraft {
    private final List<ShapeOutline> SHAPE_OUTLINES = new ArrayList<>();
    private final List<ShapeBoxes> SHAPE_BOXES = new ArrayList<>();
    public final List<Line> LINE_DEPTH = new ArrayList<>();
    public final List<Line> LINE = new ArrayList<>();
    public final List<Quad> QUAD_DEPTH = new ArrayList<>();
    public final List<Quad> QUAD = new ArrayList<>();
    private Tessellator tessellator = Tessellator.getInstance();
    @Setter
    @Getter
    private Matrix4f lastProjMat = new Matrix4f(), lastModMat = new Matrix4f(), lastWorldSpaceMatrix = new Matrix4f();
    private final Identifier captureId = Identifier.of("textures/capture.png"), bloom = Identifier.of("textures/bloom.png");

    public void onEventRender3D(MatrixStack matrix) {

        MatrixStack.Entry entry = matrix.peek();
        if (!QUAD.isEmpty()) {
            RenderLayer quadLayer = RenderLayerUtil.positionColor();
            BufferBuilder buffer = RenderLayerUtil.begin(quadLayer);
            QUAD.forEach(quad -> vertexQuad(entry, buffer, quad.x, quad.y, quad.w, quad.z, quad.color));
            RenderLayerUtil.draw(quadLayer, buffer);
            QUAD.clear();
        }
        if (!LINE.isEmpty()) {
            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
            Set<Float> widths = LINE.stream().map(line -> line.width).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderLayer lineLayer = RenderLayerUtil.lines();
            widths.forEach(width -> {
                RenderSystem.lineWidth(width);
                BufferBuilder buffer = RenderLayerUtil.begin(lineLayer);
                LINE.stream().filter(line -> line.width == width).forEach(line -> vertexLine(matrix, buffer, line.start, line.end, line.colorStart, line.colorEnd));
                RenderLayerUtil.draw(lineLayer, buffer);
            });
            LINE.clear();
            GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        }
        if (!LINE_DEPTH.isEmpty()) {
            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
            Set<Float> widths = LINE_DEPTH.stream().map(line -> line.width).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderLayer lineLayer = RenderLayerUtil.linesDepthNoWrite();
            widths.forEach(width -> {
                RenderSystem.lineWidth(width);
                BufferBuilder buffer = RenderLayerUtil.begin(lineLayer);
                LINE_DEPTH.stream().filter(line -> line.width == width).forEach(line -> vertexLine(matrix, buffer, line.start, line.end, line.colorStart, line.colorEnd));
                RenderLayerUtil.draw(lineLayer, buffer);
            });
            LINE_DEPTH.clear();
            GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        }
        if (!QUAD_DEPTH.isEmpty()) {
            RenderLayer quadLayer = RenderLayerUtil.positionColorDepth();
            BufferBuilder buffer = RenderLayerUtil.begin(quadLayer);
            QUAD_DEPTH.forEach(quad -> vertexQuad(entry, buffer, quad.x, quad.y, quad.w, quad.z, quad.color));
            RenderLayerUtil.draw(quadLayer, buffer);
            QUAD_DEPTH.clear();
        }
    }

    public void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width) {
        drawShape(blockPos, voxelShape, color, width, true, false);
    }

    public void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        if (ProjectionUtil.canSee(voxelShape.getBoundingBox().offset(blockPos))) SHAPE_BOXES.stream().filter(boxes -> boxes.shape.equals(voxelShape))
                .findFirst().ifPresentOrElse(shapeBoxes -> shapeBoxes.boxes.forEach(box -> drawBox(box.offset(blockPos), color, width, true, fill, depth)),
                        () -> SHAPE_BOXES.add(new ShapeBoxes(voxelShape, voxelShape.getBoundingBoxes())));
    }

    public void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        Vec3d vec3d = Vec3d.of(blockPos);
        if (ProjectionUtil.canSee(voxelShape.getBoundingBox().offset(vec3d))) {
            List<Box> voxelBoxes = voxelShape.getBoundingBoxes();
            SHAPE_OUTLINES.stream().filter(shapeOutline -> shapeOutline.boxes.equals(voxelBoxes))
                    .findFirst().ifPresentOrElse(shapeOutline -> {
                        shapeOutline.boxes.forEach(box -> drawBox(box.offset(vec3d), color, width, false, fill, depth));
                        shapeOutline.lines.forEach(line -> drawLine(line.start.add(vec3d), line.end.add(vec3d), color, width, depth));
                    }, () -> {
                        List<Line> lines = new ArrayList<>();
                        voxelShape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> lines.add(new Line(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0)));
                        SHAPE_OUTLINES.add(new ShapeOutline(voxelShape, lines, voxelShape.getBoundingBoxes()));
                    });
        }
    }

    public void drawBox(Box box, int color, float width) {
        drawBox(box, color, width, true, true, false);
    }

    public void drawBox(Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        box = box.expand(1e-3);
        if (ProjectionUtil.canSee(box)) {
            double x1 = box.minX;
            double y1 = box.minY;
            double z1 = box.minZ;
            double x2 = box.maxX;
            double y2 = box.maxY;
            double z2 = box.maxZ;

            if (fill) {
                int fillColor = ColorUtil.multAlpha(color, 0.1f);
                drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, depth);
                drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, depth);
                drawQuad(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, depth);
                drawQuad(new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, depth);
                drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, depth);
                drawQuad(new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, depth);
            }

            if (line) {
                drawLine(x1, y1, z1, x2, y1, z1, color, width, depth);
                drawLine(x2, y1, z1, x2, y1, z2, color, width, depth);
                drawLine(x2, y1, z2, x1, y1, z2, color, width, depth);
                drawLine(x1, y1, z2, x1, y1, z1, color, width, depth);
                drawLine(x1, y1, z2, x1, y2, z2, color, width, depth);
                drawLine(x1, y1, z1, x1, y2, z1, color, width, depth);
                drawLine(x2, y1, z2, x2, y2, z2, color, width, depth);
                drawLine(x2, y1, z1, x2, y2, z1, color, width, depth);
                drawLine(x1, y2, z1, x2, y2, z1, color, width, depth);
                drawLine(x2, y2, z1, x2, y2, z2, color, width, depth);
                drawLine(x2, y2, z2, x1, y2, z2, color, width, depth);
                drawLine(x1, y2, z2, x1, y2, z1, color, width, depth);
            }
        }
    }

    public void vertexLine(@NotNull MatrixStack matrices, @NotNull VertexConsumer buffer, Vec3d start, Vec3d end, int lineColor) {
        vertexLine(matrices, buffer, start.toVector3f(), end.toVector3f(), lineColor, lineColor);
    }

    public void vertexLine(@NotNull MatrixStack matrices, @NotNull VertexConsumer buffer, Vec3d start, Vec3d end, int startColor, int endColor) {
        vertexLine(matrices, buffer, start.toVector3f(), end.toVector3f(), startColor, endColor);
    }

    public void vertexLine(@NotNull MatrixStack matrices, @NotNull VertexConsumer buffer, Vector3f start, Vector3f end, int startColor, int endColor) {
        matrices.push();
        MatrixStack.Entry entry = matrices.peek();

        Vector3f vec = getNormal(start.x, start.y, start.z, end.x, end.y, end.z);
        buffer.vertex(entry, start).color(startColor).normal(entry, vec.x(), vec.y(), vec.z());
        buffer.vertex(entry, end).color(endColor).normal(entry, vec.x(), vec.y(), vec.z());
        matrices.pop();
    }

    public void vertexQuad(@NotNull MatrixStack.Entry entry, @NotNull VertexConsumer buffer, Vec3d vec1, Vec3d vec2, Vec3d vec3, Vec3d vec4, int color) {
        vertexQuad(entry, buffer, vec1.toVector3f(), vec2.toVector3f(), vec3.toVector3f(), vec4.toVector3f(), color);
    }

    public void vertexQuad(@NotNull MatrixStack.Entry entry, @NotNull VertexConsumer buffer, Vector3f vec1, Vector3f vec2, Vector3f vec3, Vector3f vec4, int color) {
        buffer.vertex(entry, vec1).color(color);
        buffer.vertex(entry, vec2).color(color);
        buffer.vertex(entry, vec3).color(color);
        buffer.vertex(entry, vec4).color(color);
    }

    public @NotNull Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);
        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }

    private float espValue = 1f,espSpeed = 1f,  prevEspValue, prevCircleStep, circleStep;
    private boolean flipSpeed;

    public void updateTargetEsp() {
        prevEspValue = espValue;
        espValue += espSpeed;
        if (espSpeed > 25) flipSpeed = true;
        if (espSpeed < -25) flipSpeed = false;
        espSpeed = flipSpeed ? espSpeed - 0.5f : espSpeed + 0.5f;

        prevCircleStep = circleStep;
        circleStep += 0.15f;
    }

    public void drawLine(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float width, boolean depth) {
        drawLine(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), color, width, depth);
    }

    public void drawLine(Vec3d start, Vec3d end, int color, float width, boolean depth) {
        drawLine(start, end, color, color, width, depth);
    }

    public void drawLine(Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
        Line line = new Line(start.subtract(cameraPos), end.subtract(cameraPos), colorStart, colorEnd, width);
        if (depth) LINE_DEPTH.add(line);
        else LINE.add(line);
    }

    public void drawQuad(Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        Vec3d cameraPos = mc.getEntityRenderDispatcher().camera.getPos();
        Quad quad = new Quad(x.subtract(cameraPos), y.subtract(cameraPos), w.subtract(cameraPos), z.subtract(cameraPos), color);
        if (depth) QUAD_DEPTH.add(quad);
        else QUAD.add(quad);
    }

    public static float getTickDelta() {
        return mc.getRenderTickCounter().getTickProgress(false);
    }

    public record Line(Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {}
    public record Quad(Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color) {}
    public record ShapeBoxes(VoxelShape shape, List<Box> boxes) {}
    public record ShapeOutline(VoxelShape shape, List<Line> lines, List<Box> boxes) {}
}
