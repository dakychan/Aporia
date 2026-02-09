package aporia.cc.utility.render.display.base;

import aporia.cc.utility.math.MathUtil;

public record Rect(float x, float y, float width, float height) {
    public boolean contains(double mx, double my) {
        return MathUtil.isHovered(mx,my,x,y,width,height);
    }
}

