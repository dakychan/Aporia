package zenith.zov.utility.game.other;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import zenith.zov.Zenith;
import zenith.zov.utility.interfaces.IMinecraft;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

import java.awt.*;

@UtilityClass
public class MessageUtil implements IMinecraft {

    public static final Text PREFIX = Text.of("[ %s ] ".formatted(Zenith.NAME))
            .copy()
            .append(Text.of("★"))
            .getWithStyle(Style.EMPTY.withColor(ColorRGBA.BLUE.getRGB()))
            .getFirst();

    public void displayMessage(LogLevel level, String message) {
        if (mc.player == null) return;
        Text icon = Text.of("[ %s ] ".formatted(Zenith.NAME))
                .copy()
                .append(Text.of("★"))
                .getWithStyle(Style.EMPTY.withColor(Zenith.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB()))
                .getFirst();
        Text styledMessage = Text.of(message).copy().getWithStyle(getLevelStyle(level)).getFirst();
        mc.player.sendMessage(        icon.copy().append(" ").append(styledMessage), false);
    }

    public void displayWarning(String message) {
        displayMessage(LogLevel.WARN, message);
    }

    public void displayError(String message) {
        displayMessage(LogLevel.ERROR, message);
    }

    public void displayInfo(String message) {

        displayMessage(LogLevel.INFO, message);
    }

    private Style getLevelStyle(LogLevel level) {
        return Style.EMPTY.withColor(level.getColor().getRGB());
    }

    @Getter
    @RequiredArgsConstructor
    public enum LogLevel {
        WARN("Warning", new Color(247, 206, 59)),
        ERROR("Error", new Color(242, 79, 68)),
        INFO("Info", new Color(87, 126, 255));

        private final String level;
        private final Color color;

    }
}
