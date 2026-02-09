package aporia.cc.client.hud.elements.component;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import aporia.cc.base.animations.base.Animation;
import aporia.cc.base.animations.base.Easing;
import aporia.cc.client.hud.elements.draggable.DraggableHudElement;
import aporia.cc.client.modules.impl.render.Interface;
import aporia.cc.utility.render.display.base.CustomDrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerListComponent extends DraggableHudElement {
    private final Animation animation = new Animation(250, Easing.BAKEK_SIZE);

    public PlayerListComponent(String name) {
        super(name, 0, 0, 0, 0, 0, 0, null);
    }


    @Override
    protected void renderXLine(CustomDrawContext ctx, SheetCode nearest) {

    }

    @Override
    protected void renderYLine(CustomDrawContext ctx, SheetCode nearest) {

    }

    @Override
    public void set(float x, float y) {

    }

    @Override
    public void update(float widthScreen, float heightScreen) {

    }

    @Override
    public void release() {

    }

    @Override
    public void windowResized(float newWindowWidth, float newWindowHeight) {

    }

    @Override
    public void set(CustomDrawContext ctx, float x, float y, Interface dragManager, float widthScreen, float heightScreen) {
    }

    @Override
    public JsonObject save() {
        return new JsonObject();
    }

    @Override
    public void load(JsonObject obj) {

    }

    @Override
    public void render(CustomDrawContext ctx) {

        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective scoreboardObjective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.LIST);

        animation.update(!(!mc.options.playerListKey.isPressed() || mc.isInSingleplayer() && mc.player.networkHandler.getListedPlayerListEntries().size() <= 1 && scoreboardObjective == null));
        if (animation.getValue() != 0) {
            render(ctx, ctx.getScaledWindowWidth(), scoreboard, scoreboardObjective);
        }
        animation.setDuration(250);
        animation.setEasing(Easing.BAKEK_SIZE);
    }


    //deobfuscation by Chatgpt
    public void render(
            DrawContext context,
            int scaledWindowWidth,
            Scoreboard scoreboard,
            @Nullable ScoreboardObjective objective
    ) {
        float delta = animation.getValue();
        // Собираем игроков и подготавливаем контейнер под строки счёта/отображения
        List<PlayerListEntry> players = mc.inGameHud.getPlayerListHud().collectPlayerEntries();
        List<PlayerListHud.ScoreDisplayEntry> scoreEntries = new ArrayList<>(players.size());

        // Базовые измерения текста
        final int spaceWidth = mc.textRenderer.getWidth(" ");
        int nameColumnWidth = 0;   // максимальная ширина имени игрока
        int scoreColumnWidth = 0;  // дополнительная ширина под счёт/формат

        // Заполняем данные для каждой строки таблицы
        for (PlayerListEntry entry : players) {
            Text nameText = mc.inGameHud.getPlayerListHud().getPlayerName(entry);
            nameColumnWidth = Math.max(nameColumnWidth, mc.textRenderer.getWidth(nameText));

            int rawScore = 0;
            Text formattedScoreText = null;
            int formattedScoreWidth = 0;

            if (objective != null) {
                // Достаём счёт для текущего objective
                ScoreHolder holder = ScoreHolder.fromProfile(entry.getProfile());
                ReadableScoreboardScore readableScore = scoreboard.getScore(holder, objective);

                if (readableScore != null) {
                    rawScore = readableScore.getScore();
                }

                // Для не-сердечных рендеров форматируем число и учитываем ширину колонки
                if (objective.getRenderType() != ScoreboardCriterion.RenderType.HEARTS) {
                    NumberFormat fmt = objective.getNumberFormatOr(StyledNumberFormat.YELLOW);
                    formattedScoreText = ReadableScoreboardScore.getFormattedScore(readableScore, fmt);
                    formattedScoreWidth = mc.textRenderer.getWidth(formattedScoreText);

                    // если есть что рисовать — добавляем отступ + ширину
                    scoreColumnWidth = Math.max(scoreColumnWidth, formattedScoreWidth > 0 ? spaceWidth + formattedScoreWidth : 0);
                }
            }

            scoreEntries.add(new PlayerListHud.ScoreDisplayEntry(nameText, rawScore, formattedScoreText, formattedScoreWidth));
        }

        // Чистим кеш сердец от игроков, которых уже нет в списке
        if (!mc.inGameHud.getPlayerListHud().hearts.isEmpty()) {
            Set<UUID> visibleIds = players.stream()
                    .map(p -> p.getProfile().getId())
                    .collect(Collectors.toSet());
            mc.inGameHud.getPlayerListHud().hearts.keySet().removeIf(id -> !visibleIds.contains(id));
        }

        // Подсчёт колонок и строк (ограничение: максимум 20 строк в колонке)
        final int total = players.size();
        int rowsPerColumn = total;
        int columns = 1;
        while (rowsPerColumn > 20) {
            columns++;
            rowsPerColumn = (total + columns - 1) / columns;
        }

        // В одиночной игре или при зашифрованном соединении показываем мини-скин
        final boolean showHead = true||mc.isInSingleplayer() || mc.getNetworkHandler().getConnection().isEncrypted();

        // Ширина доп. колонки для счёта/сердец
        final int extraColumnWidth;
        if (objective != null) {
            extraColumnWidth = (objective.getRenderType() == ScoreboardCriterion.RenderType.HEARTS) ? 90 : scoreColumnWidth;
        } else {
            extraColumnWidth = 0;
        }

        // Ширина одной колонки с учётом отступов и ограничений окна
        int columnWidth = Math.min(
                columns * ((showHead ? 9 : 0) + nameColumnWidth + extraColumnWidth + 13),
                scaledWindowWidth - 50
        ) / columns;

        // Горизонтальные и вертикальные координаты начала отрисовки таблицы
        int leftX = scaledWindowWidth / 2 - (columnWidth * columns + (columns - 1) * 5) / 2;
        int yCursor = 10; // стартовый Y
        int contentWidth = columnWidth * columns + (columns - 1) * 5;

        // Заголовок (header), переносим на строки и расширяем contentWidth при необходимости
        List<OrderedText> headerLines = null;
        if (mc.inGameHud.getPlayerListHud().header != null) {
            headerLines = mc.textRenderer.wrapLines(mc.inGameHud.getPlayerListHud().header, scaledWindowWidth - 50);
            for (OrderedText line : headerLines) {
                contentWidth = Math.max(contentWidth, mc.textRenderer.getWidth(line));
            }
        }

        // Футер (footer), переносим на строки и расширяем contentWidth при необходимости
        List<OrderedText> footerLines = null;
        if (mc.inGameHud.getPlayerListHud().footer != null) {
            footerLines = mc.textRenderer.wrapLines(mc.inGameHud.getPlayerListHud().footer, scaledWindowWidth - 50);
            for (OrderedText line : footerLines) {
                contentWidth = Math.max(contentWidth, mc.textRenderer.getWidth(line));
            }
        }

        final int headerHeight = (headerLines != null) ? (headerLines.size() * 9) : 0;
        final int headerGap = (headerLines != null) ? 1 : 0;  // после хедера ты делаешь yCursor++
        final int tableHeight = rowsPerColumn * 9;
        final int footerGap = (footerLines != null) ? 1 : 0;  // перед футером ты делаешь +1
        final int footerHeight = (footerLines != null) ? (footerLines.size() * 9) : 0;

        final int totalHeight = headerHeight + headerGap + tableHeight + footerGap + footerHeight;

// ЦЕНТР СКЕЙЛА: по X — центр реального бокса (leftX + width/2),
// по Y — от стартового Y (10) + половина общей высоты
        final float centerX =scaledWindowWidth / 2 - 1 ;
        final float centerY = 10 + totalHeight / 2.0f;

// масштаб из delta (0..1). чутка клампим, чтобы не было 0
        final float scale = Math.max(0.0001f, delta);

// применяем матрицу
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX, centerY);
        matrices.scale(scale, scale);
        matrices.translate(-centerX, -centerY);


        // ⬆⬆ SCALE

        // Отрисовка заголовка (если есть)
        if (headerLines != null) {
            context.fill(
                    scaledWindowWidth / 2 - contentWidth / 2 - 1,
                    yCursor - 1,
                    scaledWindowWidth / 2 + contentWidth / 2 + 1,
                    yCursor + headerLines.size() * 9,
                    Integer.MIN_VALUE
            );

                for (OrderedText line : headerLines) {
                int lineWidth = mc.textRenderer.getWidth(line);
                context.drawTextWithShadow(mc.textRenderer, line, scaledWindowWidth / 2 - lineWidth / 2, yCursor, 0xFFFFFFFF);
                yCursor += 9;
            }
            yCursor++; // зазор после хедера
        }

        // Фон под таблицу игроков
        context.fill(
                scaledWindowWidth / 2 - contentWidth / 2 - 1,
                yCursor - 1,
                scaledWindowWidth / 2 + contentWidth / 2 + 1,
                yCursor + rowsPerColumn * 9,
                Integer.MIN_VALUE
        );

        final int rowBgColor =this.mc.options.getTextBackgroundColor(553648127);

        // Рендер строк
        for (int index = 0; index < total; index++) {
            int col = index / rowsPerColumn;
            int row = index % rowsPerColumn;

            int cellLeft = leftX + col * columnWidth + col * 5;
            int cellTop = yCursor + row * 9;

           context.fill(cellLeft, cellTop, cellLeft + columnWidth, cellTop + 8, rowBgColor);
            if (index >= players.size()) continue;

            PlayerListEntry ple = players.get(index);
            PlayerListHud.ScoreDisplayEntry sde = scoreEntries.get(index);
            GameProfile profile = ple.getProfile();

            int textX = cellLeft;

            if (showHead) {
                PlayerEntity playerEntity = mc.world.getPlayerByUuid(profile.getId());
                boolean flip = playerEntity != null && LivingEntityRenderer.shouldFlipUpsideDown(playerEntity);
                PlayerSkinDrawer.draw(
                        context,
                        ple.getSkinTextures().texture(),
                        cellLeft, cellTop, 8,
                        ple.shouldShowHat(),
                        flip,
                        -1
                );
                textX += 9;
            }

            int nameColor = (ple.getGameMode() == GameMode.SPECTATOR) ? 0x912D2D2F : Colors.WHITE;
            context.drawTextWithShadow(mc.textRenderer, sde.name(), textX, cellTop, nameColor);

            if (objective != null && ple.getGameMode() != GameMode.SPECTATOR) {
                int scoreLeft = textX + nameColumnWidth + 1;
                int scoreRight = scoreLeft + extraColumnWidth;
                if (scoreRight - scoreLeft > 5) {
                    mc.inGameHud.getPlayerListHud().renderScoreboardObjective(
                            objective, cellTop, sde, scoreLeft, scoreRight, profile.getId(), context
                    );
                }
            }

            mc.inGameHud.getPlayerListHud().renderLatencyIcon(
                    context, columnWidth, textX - (showHead ? 9 : 0), cellTop, ple
            );
        }

        // Футер
        if (footerLines != null) {
            yCursor += rowsPerColumn * 9 + 1;
            context.fill(
                    scaledWindowWidth / 2 - contentWidth / 2 - 1,
                    yCursor - 1,
                    scaledWindowWidth / 2 + contentWidth / 2 + 1,
                    yCursor + footerLines.size() * 9,
                    Integer.MIN_VALUE
            );
            for (OrderedText line : footerLines) {
                int lineWidth = mc.textRenderer.getWidth(line);
                context.drawTextWithShadow(mc.textRenderer, line, scaledWindowWidth / 2 - lineWidth / 2, yCursor, 0xFFFFFFFF);
                yCursor += 9;
            }
        }

        // Закрываем масштабирование
        matrices.popMatrix();
    }


}
