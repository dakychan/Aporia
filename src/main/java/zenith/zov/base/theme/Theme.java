package zenith.zov.base.theme;

import lombok.Getter;
import lombok.Setter;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

@Getter
@Setter
public class Theme {
    private final String name;
    private final String icon;

    private final Defaults defaults;

    
    private ColorRGBA color;
    private ColorRGBA secondColor;
    private ColorRGBA friendColor;

    private ColorRGBA gray;
    private ColorRGBA grayLight;
    private ColorRGBA foregroundLight;
    private ColorRGBA whiteGray;
    private ColorRGBA foregroundGray;
    private ColorRGBA foregroundLightStroke;
    private ColorRGBA foregroundColor;
    private ColorRGBA foregroundStroke;
    private ColorRGBA foregroundDark;
    private ColorRGBA white;
    private ColorRGBA backgroundColor;

    
    private boolean glow;
    private boolean blur;
    private boolean corners;

    public Theme(String name, String icon, ThemeBuilder builder) {
        this.name = name;
        this.icon = icon;

        this.color = builder.color;
        this.secondColor = builder.secondColor;
        this.friendColor = builder.friendColor;

        this.gray = builder.gray;
        this.grayLight = builder.grayLight;
        this.foregroundLight = builder.foregroundLight;
        this.whiteGray = builder.whiteGray;
        this.foregroundGray = builder.foregroundGray;
        this.foregroundLightStroke = builder.foregroundLightStroke;
        this.foregroundColor = builder.foreground;
        this.foregroundStroke = builder.foregroundStroke;
        this.foregroundDark = builder.foregroundDark;
        this.white = builder.white;
        this.backgroundColor = builder.background;

        this.glow = builder.glow;
        this.blur = builder.blur;
        this.corners = builder.corners;

        this.defaults = new Defaults(
                builder.color,
                builder.secondColor,
                builder.friendColor,
                builder.friendSecond,
                builder.gray,
                builder.grayLight,
                builder.foregroundLight,
                builder.whiteGray,
                builder.foregroundGray,
                builder.foregroundLightStroke,
                builder.foreground,
                builder.foregroundStroke,
                builder.foregroundDark,
                builder.white,
                builder.background,
                builder.glow,
                builder.blur,
                builder.corners
        );
    }

    public void reset() {
        this.color = defaults.color;
        this.secondColor = defaults.secondColor;
        this.friendColor = defaults.friendColor;

        this.gray = defaults.gray;
        this.grayLight = defaults.grayLight;
        this.foregroundLight = defaults.foregroundLight;
        this.whiteGray = defaults.whiteGray;
        this.foregroundGray = defaults.foregroundGray;
        this.foregroundLightStroke = defaults.foregroundLightStroke;
        this.foregroundColor = defaults.foreground;
        this.foregroundStroke = defaults.foregroundStroke;
        this.foregroundDark = defaults.foregroundDark;
        this.white = defaults.white;
        this.backgroundColor = defaults.background;

        this.glow = defaults.glow;
        this.blur = defaults.blur;
        this.corners = defaults.corners;
    }

    public Theme interpolateTheme(Theme other, float delta) {
        return ThemeBuilder.builder()
                .color(this.color.mix(other.getColor(), delta))
                .secondColor(this.secondColor.mix(other.getSecondColor(), delta))
                .friendColor(this.friendColor.mix(other.getFriendColor(), delta))
                .gray(this.gray.mix(other.getGray(), delta))
                .grayLight(this.grayLight.mix(other.getGrayLight(), delta))
                .foregroundLight(this.foregroundLight.mix(other.getForegroundLight(), delta))
                .whiteGray(this.whiteGray.mix(other.getWhiteGray(), delta))
                .foregroundGray(this.foregroundGray.mix(other.getForegroundGray(), delta))
                .foregroundLightStroke(this.foregroundLightStroke.mix(other.getForegroundLightStroke(), delta))
                .foreground(this.foregroundColor.mix(other.getForegroundColor(), delta))
                .foregroundStroke(this.foregroundStroke.mix(other.getForegroundStroke(), delta))
                .foregroundDark(this.foregroundDark.mix(other.getForegroundDark(), delta))
                .white(this.white.mix(other.getWhite(), delta))
                .background(this.backgroundColor.mix(other.getBackgroundColor(), delta))
                
                .glow(this.glow)
                .blur(this.blur)
                .corners(this.corners)
                .build(other.name, other.icon);
    }

    public static class ThemeBuilder {
        private ColorRGBA color;
        private ColorRGBA secondColor;
        private ColorRGBA friendColor;
        private ColorRGBA friendSecond;
        private ColorRGBA gray;
        private ColorRGBA grayLight;
        private ColorRGBA foregroundLight;
        private ColorRGBA whiteGray;
        private ColorRGBA foregroundGray;
        private ColorRGBA foregroundLightStroke;
        private ColorRGBA foreground;
        private ColorRGBA foregroundStroke;
        private ColorRGBA foregroundDark;
        private ColorRGBA white;
        private ColorRGBA background;

        private boolean glow = false;
        private boolean blur = false;
        private boolean corners = false;

        public static ThemeBuilder builder() {
            return new ThemeBuilder();
        }

        public ThemeBuilder color(ColorRGBA color) {
            this.color = color;
            return this;
        }

        public ThemeBuilder secondColor(ColorRGBA secondColor) {
            this.secondColor = secondColor;
            return this;
        }

        public ThemeBuilder friendColor(ColorRGBA friendColor) {
            this.friendColor = friendColor;
            return this;
        }

        public ThemeBuilder friendSecond(ColorRGBA friendSecond) {
            this.friendSecond = friendSecond;
            return this;
        }

        public ThemeBuilder gray(ColorRGBA gray) {
            this.gray = gray;
            return this;
        }

        public ThemeBuilder grayLight(ColorRGBA grayLight) {
            this.grayLight = grayLight;
            return this;
        }

        public ThemeBuilder foregroundLight(ColorRGBA foregroundLight) {
            this.foregroundLight = foregroundLight;
            return this;
        }

        public ThemeBuilder whiteGray(ColorRGBA whiteGray) {
            this.whiteGray = whiteGray;
            return this;
        }

        public ThemeBuilder foregroundGray(ColorRGBA foregroundGray) {
            this.foregroundGray = foregroundGray;
            return this;
        }

        public ThemeBuilder foregroundLightStroke(ColorRGBA foregroundLightStroke) {
            this.foregroundLightStroke = foregroundLightStroke;
            return this;
        }

        public ThemeBuilder foreground(ColorRGBA foreground) {
            this.foreground = foreground;
            return this;
        }

        public ThemeBuilder foregroundStroke(ColorRGBA foregroundStroke) {
            this.foregroundStroke = foregroundStroke;
            return this;
        }

        public ThemeBuilder foregroundDark(ColorRGBA foregroundDark) {
            this.foregroundDark = foregroundDark;
            return this;
        }

        public ThemeBuilder white(ColorRGBA white) {
            this.white = white;
            return this;
        }

        public ThemeBuilder background(ColorRGBA background) {
            this.background = background;
            return this;
        }

        public ThemeBuilder glow(boolean glow) {
            this.glow = glow;
            return this;
        }

        public ThemeBuilder blur(boolean blur) {
            this.blur = blur;
            return this;
        }

        public ThemeBuilder corners(boolean corners) {
            this.corners = corners;
            return this;
        }

        public Theme build(String name, String icon) {
            return new Theme(name, icon, this);
        }
    }

    public static final Theme DARK = ThemeBuilder.builder()
            .color(new ColorRGBA(181, 162, 255))
            .secondColor(new ColorRGBA(255, 203, 162))
            .friendColor(new ColorRGBA(181, 162, 255))
            .friendSecond(new ColorRGBA(255, 203, 162))
            .gray(new ColorRGBA(88, 87, 93))
            .grayLight(new ColorRGBA(128, 127, 133))
            .foregroundLight(new ColorRGBA(32, 31, 37))
            .whiteGray(new ColorRGBA(68, 67, 73))
            .foregroundGray(new ColorRGBA(48, 47, 53))
            .foregroundLightStroke(new ColorRGBA(38, 37, 43))
            .foreground(new ColorRGBA(28, 27, 33))
            .foregroundStroke(new ColorRGBA(35, 34, 40))
            .foregroundDark(new ColorRGBA(25, 24, 30))
            .white(new ColorRGBA(255, 255, 255))
            .background(new ColorRGBA(23, 22, 28))
            .glow(false)
            .blur(false)
            .corners(false)
            .build("Dark","8");

    public static final Theme LIGHT = ThemeBuilder.builder()
            .color(new ColorRGBA(123, 93, 234))
            .secondColor(new ColorRGBA(255, 192, 121))
            .friendColor(new ColorRGBA(123, 93, 234))
            .friendSecond(new ColorRGBA(255, 192, 121))
            .gray(new ColorRGBA(138, 137, 143))
            .grayLight(new ColorRGBA(148, 147, 153))
            .foregroundLight(new ColorRGBA(236, 236, 236))
            .whiteGray(new ColorRGBA(178, 177, 183))
            .foregroundGray(new ColorRGBA(188, 187, 193))
            .foregroundLightStroke(new ColorRGBA(229, 229, 229))
            .foreground(new ColorRGBA(246, 246, 246))
            .foregroundStroke(new ColorRGBA(229, 229, 229))
            .foregroundDark(new ColorRGBA(251, 251, 251))
            .white(new ColorRGBA(23, 22, 28))
            .background(new ColorRGBA(255, 255, 255))
            .glow(false)
            .blur(false)
            .corners(false)
            .build("Light","T");

    public static final Theme CUSTOM_THEME = ThemeBuilder.builder()
            .color(new ColorRGBA(181, 162, 255))
            .secondColor(new ColorRGBA(255, 203, 162))
            .friendColor(new ColorRGBA(181, 162, 255))
            .friendSecond(new ColorRGBA(255, 203, 162))
            .gray(new ColorRGBA(88, 87, 93))
            .grayLight(new ColorRGBA(128, 127, 133))
            .foregroundLight(new ColorRGBA(32, 31, 37))
            .whiteGray(new ColorRGBA(68, 67, 73))
            .foregroundGray(new ColorRGBA(48, 47, 53))
            .foregroundLightStroke(new ColorRGBA(38, 37, 43))
            .foreground(new ColorRGBA(28, 27, 33))
            .foregroundStroke(new ColorRGBA(35, 34, 40))
            .foregroundDark(new ColorRGBA(25, 24, 30))
            .white(new ColorRGBA(255, 255, 255))
            .background(new ColorRGBA(23, 22, 28))
            .glow(false)
            .blur(false)
            .corners(false)
            .build("Custom","F");

    private record Defaults(
            ColorRGBA color,
            ColorRGBA secondColor,
            ColorRGBA friendColor,
            ColorRGBA friendSecond,
            ColorRGBA gray,
            ColorRGBA grayLight,
            ColorRGBA foregroundLight,
            ColorRGBA whiteGray,
            ColorRGBA foregroundGray,
            ColorRGBA foregroundLightStroke,
            ColorRGBA foreground,
            ColorRGBA foregroundStroke,
            ColorRGBA foregroundDark,
            ColorRGBA white,
            ColorRGBA background,
            boolean glow,
            boolean blur,
            boolean corners
    ) {}
}
