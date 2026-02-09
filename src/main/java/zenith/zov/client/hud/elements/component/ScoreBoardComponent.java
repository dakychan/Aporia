package zenith.zov.client.hud.elements.component;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import zenith.zov.Zenith;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.hud.elements.draggable.DraggableHudElement;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.minecraft.client.gui.hud.InGameHud.SCOREBOARD_ENTRY_COMPARATOR;

public class ScoreBoardComponent extends DraggableHudElement {
    public ScoreBoardComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
    }

    @Override
    public void render(CustomDrawContext ctx) {
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective scoreboardObjective = null;
        Team team = scoreboard.getScoreHolderTeam(mc.player.getNameForScoreboard());
        if (team != null) {
            ScoreboardDisplaySlot scoreboardDisplaySlot = ScoreboardDisplaySlot.fromFormatting(team.getColor());
            if (scoreboardDisplaySlot != null) {
                scoreboardObjective = scoreboard.getObjectiveForSlot(scoreboardDisplaySlot);
            }
        }

        ScoreboardObjective scoreboardObjective2 = scoreboardObjective != null ? scoreboardObjective : scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (scoreboardObjective2 != null) {
            this.renderScoreboardSidebar(ctx, scoreboardObjective2);
        }
    }

    @Environment(EnvType.CLIENT)
    private static final class SidebarRow {
        private final Text name;
        private final Text score;
        private final int scoreWidth;

        private SidebarRow(Text name, Text score, int scoreWidth) {
            this.name = name;
            this.score = score;
            this.scoreWidth = scoreWidth;
        }

        public Text name()       { return name; }
        public Text score()      { return score; }
        public int scoreWidth()  { return scoreWidth; }
    }
    //deobfuscation by Chatgpt
    private void renderScoreboardSidebar(DrawContext ctx, ScoreboardObjective objective) {
        // == источники данных ==
        final Scoreboard sb = objective.getScoreboard();
        final TextRenderer tr = mc.textRenderer;
        final NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.RED);

        // == собираем строки (макс. 15, скрытые исключаем, сортируем как в исходнике) ==
        List<SidebarRow> rows = sb.getScoreboardEntries(objective).stream()
                .filter(score -> !score.hidden())
                .sorted(SCOREBOARD_ENTRY_COMPARATOR)
                .limit(15)
                .map(entry -> {
                    Team team = sb.getScoreHolderTeam(entry.owner());
                    Text name = Team.decorateName(team, entry.name());
                    Text scoreText = entry.formatted(numberFormat);
                    int scoreW = tr.getWidth(scoreText);
                    return new SidebarRow(name, scoreText, scoreW);
                })
                .collect(Collectors.toList());

        // == заголовок ==
        Text title = objective.getDisplayName();
        int titleWidth = tr.getWidth(title);

        // ширина ": " используется в расчёте общей ширины, как и в оригинале
        int colonWidth = tr.getWidth(": ");

        // максимальная ширина контента (имя + ": " + счёт) или ширина заголовка
        int contentWidth = titleWidth;
        for (SidebarRow row : rows) {
            int nameW = tr.getWidth(row.name());
            int rowW = nameW + (row.scoreWidth() > 0 ? colonWidth + row.scoreWidth() : 0);
            contentWidth = Math.max(contentWidth, rowW);
        }

        // == геометрия ==
        int count = rows.size();
        int totalHeight = count * 9;
        int headerBaseY = (int) y;
        // высота списка (по 9px на строку)
        int boxBottomY = (int) (headerBaseY+totalHeight)+9; // как в исходнике: o
        int rightMargin = 3;                                         // отступ от правого края (p)
        int xLeft  = (int) (x+2 ); // q
        int xRight = (int) (x+contentWidth + rightMargin +1);            // r (W - 1), как было
             // u
        this.width =+contentWidth + rightMargin+1 ;
        this.height = totalHeight+9;

        // фоновые цвета (как в оригинале)
        int bodyBg   =mc.options.getTextBackgroundColor(0.3F); // s
        int headerBg =mc.options.getTextBackgroundColor(0.4F); // t
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        if(ctx instanceof CustomDrawContext context){
            DrawUtil.drawBlurHud(ctx.getMatrices(),getX(),getY(),width,height,21,BorderRadius.all(4),ColorRGBA.WHITE);
            context.drawRoundedRect(getX(),headerBaseY,width,14, BorderRadius.top(4,4), theme.getForegroundLight());
            context.drawRoundedRect(getX(),headerBaseY+14,width,height-14, BorderRadius.bottom(4,4), theme.getForegroundColor());
            DrawUtil.drawRoundedCorner(ctx.getMatrices(),x,y,width, height,0.01f,20f,theme.getColor(),BorderRadius.all(4));

        }
        this.windowResized(ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
        // == фон заголовка и тела ==
        // полоса под заголовком (u - 10 .. u - 1), x: (xLeft - 2 .. xRight)
//        ctx.fill(xLeft - 2, headerBaseY , xRight, headerBaseY +14, headerBg);
//        // тело (u - 1 .. bottom)
//        ctx.fill(xLeft - 2, headerBaseY +14, xRight, boxBottomY, bodyBg);

        // == заголовок ==
        int titleX = xLeft + contentWidth / 2 - titleWidth / 2;      // центрируем в блоке
        ctx.drawText(tr, title, titleX, headerBaseY+3 , 0xFFFFFFFF, false);

        // == строки ==
        for (int i = 0; i < count; i++) {
            SidebarRow row = rows.get(i);
            int y = boxBottomY - (count - i) * 9; // w

            // имя — слева
            ctx.drawText(tr, row.name(), xLeft, y, 0xFFFFFFFF, false);

            // счёт — прижат к правой границе блока
        }
    }

}
