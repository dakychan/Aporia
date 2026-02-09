package zenith.zov.client.hud.elements.draggable;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2f;
import zenith.zov.Zenith;
import zenith.zov.client.modules.impl.render.Interface;
import zenith.zov.utility.interfaces.IMinecraft;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

public abstract class DraggableHudElement implements IMinecraft {
    @Getter
    private final String name;

    public void tick() {
    }

    public enum Align {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    @Getter
    protected float x, y, width, height;

    private float windowWidth, windowHeight;
    protected float newX = -1, newY = -1;

    private Align align = Align.TOP_LEFT;
    private float offsetX = 0f, offsetY = 0f;

    public DraggableHudElement(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        this.name = name;
        this.x = initialX;
        this.y = initialY;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.align = align;

    }

    public abstract void render(CustomDrawContext ctx);

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    protected void drawBorder(CustomDrawContext ctx) {
        float borderThickness = 5.5f;
        float borderRadius = 6f;
        ColorRGBA borderColor = new ColorRGBA(179, 145, 255, 255);

        ctx.drawRoundedBorder(x, y, width, height, borderThickness, BorderRadius.all(borderRadius), borderColor);
    }

    public void set(CustomDrawContext ctx, float x, float y, Interface dragManager,float widthScreen,float heightScreen) {

        Vector2f nerest = dragManager.getNearest(x, y);
        SheetCode x0 = new SheetCode(nerest.x, 0);
        SheetCode y0 = new SheetCode(nerest.y, 0);

        Vector2f nerest2 = dragManager.getNearest(x + width, y + height);
        SheetCode x1 = new SheetCode(nerest2.x, -width);
        SheetCode y1 = new SheetCode(nerest2.y, -height);

        Vector2f nerest3 = dragManager.getNearest(x + width / 2, y + height / 2);
        SheetCode x2 = new SheetCode(nerest3.x, -width / 2);
        SheetCode y2 = new SheetCode(nerest3.y, -height / 2);

        this.x = x;
        this.y = y;
        this.windowWidth = widthScreen;
        this.windowHeight = heightScreen;

        SheetCode x3 = getValue(x0, x1, x2);
        SheetCode y3 = getValue(y0, y1, y2);
        renderXLine(ctx, x3);
        renderYLine(ctx, y3);

        update(widthScreen, heightScreen);

    }

    private SheetCode getValue(SheetCode value, SheetCode value2, SheetCode value3) {
        if (value.pos != -1) {
            return value;
        }
        if (value2.pos != -1) {
            return value2;
        }
        return value3;
    }

    protected void renderYLine(CustomDrawContext ctx, SheetCode nearest) {
        if (nearest.pos == -1) {
            this.newY = nearest.pos;
            return;
        }

        ctx.drawRoundedRect(0, nearest.pos, ctx.getScaledWindowWidth(), 1, BorderRadius.ZERO,
                Zenith.getInstance().getThemeManager().getCurrentTheme().getColor().mulAlpha(1));

        this.newY = nearest.pos + nearest.offset;
    }

    protected void renderXLine(CustomDrawContext ctx, SheetCode nearest) {
        if (nearest.pos == -1) {
            this.newX = nearest.pos;
            return;
        }

        ctx.drawRoundedRect(nearest.pos, 0, 1, ctx.getScaledWindowHeight(), BorderRadius.ZERO,
                Zenith.getInstance().getThemeManager().getCurrentTheme().getColor().mulAlpha(1));

        this.newX = nearest.pos + nearest.offset;
    }

    public void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void windowResized(float newWindowWidth, float newWindowHeight) {
        if (newWindowHeight <= 0 || newWindowWidth <= 0) return;

        //if (windowWidth != newWindowWidth || windowHeight != newWindowHeight) {
        float baseX = alignToX(align, newWindowWidth);
        float baseY = alignToY(align, newWindowHeight);

        this.x = baseX + offsetX;
        this.y = baseY + offsetY;
        this.windowWidth = newWindowWidth;
        this.windowHeight = newWindowHeight;

        update(newWindowWidth, newWindowHeight);
        // }
    }

    public void update(float widthScreen, float heightScreen) {
        if (this.x < 0) {
            this.x = 0;
        }
        if (this.y < 0) {
            this.y = 0;
        }

        if (this.x + width > widthScreen) {
            this.x = widthScreen - width;
        }
        if (this.y + height > heightScreen) {
            this.y = heightScreen - height;
        }
    }

    public void release() {
        if (newX != -1) this.x = newX;
        if (newY != -1) this.y = newY;

        Align newAlign = determineAlign(this.x, this.y, windowWidth, windowHeight);
        float baseX = alignToX(newAlign, windowWidth);
        float baseY = alignToY(newAlign, windowHeight);

        this.align = newAlign;
        this.offsetX = this.x - baseX;
        this.offsetY = this.y - baseY;
    }

    private Align determineAlign(float x, float y, float screenWidth, float screenHeight) {
        boolean left = x + width / 2f < screenWidth / 3f;
        boolean right = x + width / 2f > screenWidth * 2f / 3f;
        boolean centerX = !left && !right;

        boolean top = y + height / 2f < screenHeight / 3f;
        boolean bottom = y + height / 2f > screenHeight * 2f / 3f;
        boolean centerY = !top && !bottom;

        if (top) {
            if (left) return Align.TOP_LEFT;
            if (centerX) return Align.TOP_CENTER;
            return Align.TOP_RIGHT;
        } else if (centerY) {
            if (left) return Align.CENTER_LEFT;
            if (centerX) return Align.CENTER;
            return Align.CENTER_RIGHT;
        } else {
            if (left) return Align.BOTTOM_LEFT;
            if (centerX) return Align.BOTTOM_CENTER;
            return Align.BOTTOM_RIGHT;
        }
    }

    private float alignToX(Align align, float screenWidth) {
        return switch (align) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> 0;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> screenWidth / 2f - width / 2f;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> screenWidth - width;
        };
    }

    private float alignToY(Align align, float screenHeight) {
        return switch (align) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> screenHeight / 2f - height / 2f;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - height;
        };
    }

    public JsonObject save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("x", x);
        obj.addProperty("y", y);
        obj.addProperty("width", width);
        obj.addProperty("height", height);
        obj.addProperty("windowWidth", windowWidth);
        obj.addProperty("windowHeight", windowHeight);
        obj.addProperty("offsetX", offsetX);
        obj.addProperty("offsetY", offsetY);
        obj.addProperty("align", align.name());

        return obj;
    }

    public void load(JsonObject obj) {
        if (obj.has("x")) this.x = obj.get("x").getAsFloat();
        if (obj.has("y")) this.y = obj.get("y").getAsFloat();
        if (obj.has("width")) this.width = obj.get("width").getAsFloat();
        if (obj.has("height")) this.height = obj.get("height").getAsFloat();
        if (obj.has("windowWidth")) this.windowWidth = obj.get("windowWidth").getAsFloat();
        if (obj.has("windowHeight")) this.windowHeight = obj.get("windowHeight").getAsFloat();
        if (obj.has("offsetX")) this.offsetX = obj.get("offsetX").getAsFloat();
        if (obj.has("offsetY")) this.offsetY = obj.get("offsetY").getAsFloat();
        if (obj.has("align")) {
            try {
                this.align = Align.valueOf(obj.get("align").getAsString());
            } catch (IllegalArgumentException ignored) {
                this.align = Align.TOP_LEFT;
            }
        }
    }


    @Getter
    @Setter
    protected class SheetCode {
        private float pos;
        private float offset;

        public SheetCode(float pos, float offset) {
            this.pos = pos;
            this.offset = offset;
        }
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

}
