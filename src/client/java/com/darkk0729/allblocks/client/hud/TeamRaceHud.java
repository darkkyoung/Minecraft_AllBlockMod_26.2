package com.darkk0729.allblocks.client.hud;

import com.darkk0729.allblocks.client.data.ClientChallengeStateCache;
import com.darkk0729.allblocks.challenge.ChallengeDifficulty;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class TeamRaceHud {
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_Y = 12;
    private static final int BOSS_BAR_ROW_HEIGHT = 19;
    private static final int FINAL_DAY_BOSS_BAR_OFFSET = 31;

    private static final int COLOR_BLUE = 0xFF3C6FFF;
    private static final int COLOR_RED = 0xFFFF4A4A;
    private static final int COLOR_EMPTY = 0xFF262626;
    private static final int COLOR_BORDER = 0xFFB8B8B8;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_DIM = 0xFFBDBDBD;
    private static final int COLOR_BACKGROUND = 0x99000000;

    private TeamRaceHud() {
    }

    public static void register() {
        HudElementRegistry.replaceElement(
                VanillaHudElements.BOSS_BAR,
                original -> (graphics, deltaTracker) -> {
                    Minecraft client = Minecraft.getInstance();

                    if (!shouldRenderTeamRace(client)) {
                        original.extractRenderState(graphics, deltaTracker);
                        return;
                    }

                    drawCompetitionBar(graphics, client);
                    drawRanking(graphics, client);

                    int vanillaOffset =
                            isFinalDay()
                                    ? FINAL_DAY_BOSS_BAR_OFFSET
                                    : BOSS_BAR_ROW_HEIGHT;

                    var matrices = graphics.pose();
                    matrices.pushMatrix();
                    matrices.translate(0, vanillaOffset);
                    original.extractRenderState(graphics, deltaTracker);
                    matrices.popMatrix();
                }
        );
    }

    private static boolean shouldRenderTeamRace(Minecraft client) {
        return client.player != null
                && client.level != null
                && ClientChallengeStateCache.shouldShowHud()
                && ClientChallengeStateCache.isTeamRace();
    }

    private static void drawCompetitionBar(
            GuiGraphicsExtractor graphics,
            Minecraft client
    ) {
        int blueScore =
                ClientChallengeStateCache.getTeamScore("BLUE");

        int redScore =
                ClientChallengeStateCache.getTeamScore("RED");

        int total =
                Math.max(
                        1,
                        ClientChallengeStateCache.getTotalTargetCount()
                );

        String playerTeam =
                ClientChallengeStateCache.getPlayerTeam(
                        client.player.getUUID().toString()
                );

        boolean redPerspective =
                "RED".equals(playerTeam);

        int ownScore =
                redPerspective ? redScore : blueScore;

        int enemyScore =
                redPerspective ? blueScore : redScore;

        int ownColor =
                redPerspective ? COLOR_RED : COLOR_BLUE;

        int enemyColor =
                redPerspective ? COLOR_BLUE : COLOR_RED;

        String ownName =
                redPerspective ? "레드팀" : "블루팀";

        String enemyName =
                redPerspective ? "블루팀" : "레드팀";

        int remaining =
                Math.max(
                        0,
                        total - blueScore - redScore
                );

        int screenWidth =
                client.getWindow().getGuiScaledWidth();

        int x =
                (screenWidth - BAR_WIDTH) / 2;

        int ownWidth =
                (int) Math.floor(
                        BAR_WIDTH
                                * (ownScore / (double) total)
                );

        int enemyWidth =
                (int) Math.floor(
                        BAR_WIDTH
                                * (enemyScore / (double) total)
                );

        ownWidth =
                Math.max(
                        0,
                        Math.min(BAR_WIDTH, ownWidth)
                );

        enemyWidth =
                Math.max(
                        0,
                        Math.min(
                                BAR_WIDTH - ownWidth,
                                enemyWidth
                        )
                );

        boolean finalDay =
                isFinalDay();

        int finalDayAccent =
                getFinalDayAccentColor();

        int emptyColor =
                finalDay
                        ? getFinalDayEmptyColor()
                        : COLOR_EMPTY;

        int borderColor =
                finalDay
                        ? finalDayAccent
                        : COLOR_BORDER;

        graphics.fill(
                x,
                BAR_Y,
                x + BAR_WIDTH,
                BAR_Y + BAR_HEIGHT,
                emptyColor
        );

        if (ownWidth > 0) {
            graphics.fill(
                    x,
                    BAR_Y,
                    x + ownWidth,
                    BAR_Y + BAR_HEIGHT,
                    ownColor
            );
        }

        if (enemyWidth > 0) {
            graphics.fill(
                    x + BAR_WIDTH - enemyWidth,
                    BAR_Y,
                    x + BAR_WIDTH,
                    BAR_Y + BAR_HEIGHT,
                    enemyColor
            );
        }

        graphics.outline(
                x - 1,
                BAR_Y - 1,
                BAR_WIDTH + 2,
                BAR_HEIGHT + 2,
                borderColor
        );

        String leftText =
                ownName + " " + ownScore;

        String centerText =
                "미획득 " + remaining;

        String rightText =
                enemyScore + " " + enemyName;

        int textY = 2;

        graphics.text(
                client.font,
                leftText,
                x,
                textY,
                ownColor,
                true
        );

        int centerWidth =
                client.font.width(centerText);

        graphics.text(
                client.font,
                centerText,
                x + (BAR_WIDTH - centerWidth) / 2,
                textY,
                COLOR_DIM,
                true
        );

        int rightWidth =
                client.font.width(rightText);

        graphics.text(
                client.font,
                rightText,
                x + BAR_WIDTH - rightWidth,
                textY,
                enemyColor,
                true
        );

        if (finalDay) {
            String timerText =
                    "최종일 | 남은 시간 "
                            + formatFinalDayRemainingTime();

            int timerWidth =
                    client.font.width(timerText);

            graphics.text(
                    client.font,
                    timerText,
                    x + (BAR_WIDTH - timerWidth) / 2,
                    BAR_Y + BAR_HEIGHT + 4,
                    finalDayAccent,
                    true
            );
        }
    }

    private static boolean isFinalDay() {
        return ClientChallengeStateCache.isRunning()
                && ClientChallengeStateCache.getCurrentDay() == 100
                && ClientChallengeStateCache.getDifficulty()
                        != ChallengeDifficulty.EASY;
    }

    private static int getFinalDayAccentColor() {
        long remainingSeconds =
                getFinalDayRemainingSeconds();

        if (remainingSeconds <= 60L) {
            return 0xFFFF5555;
        }

        if (remainingSeconds <= 300L) {
            return 0xFFFFFF55;
        }

        return 0xFF55FF55;
    }

    private static int getFinalDayEmptyColor() {
        long remainingSeconds =
                getFinalDayRemainingSeconds();

        if (remainingSeconds <= 60L) {
            return 0xFF3A1818;
        }

        if (remainingSeconds <= 300L) {
            return 0xFF3A3518;
        }

        return 0xFF183A18;
    }

    private static long getFinalDayRemainingSeconds() {
        long ticks =
                Math.max(
                        0L,
                        ClientChallengeStateCache.getFinalDayRemainingTicks()
                );

        return (ticks + 19L) / 20L;
    }

    private static String formatFinalDayRemainingTime() {
        long totalSeconds =
                getFinalDayRemainingSeconds();

        long minutes =
                totalSeconds / 60L;

        long seconds =
                totalSeconds % 60L;

        return String.format(
                "%02d:%02d",
                minutes,
                seconds
        );
    }

    private static void drawRanking(
            GuiGraphicsExtractor graphics,
            Minecraft client
    ) {
        int blueScore =
                ClientChallengeStateCache.getTeamScore("BLUE");

        int redScore =
                ClientChallengeStateCache.getTeamScore("RED");

        String title = "올블록 순위";
        String blueLine = "블루팀 " + blueScore;
        String redLine = "레드팀 " + redScore;

        int paddingX = 6;

        int contentWidth =
                Math.max(
                        client.font.width(title),
                        Math.max(
                                client.font.width(blueLine),
                                client.font.width(redLine)
                        )
                );

        int boxWidth = contentWidth + paddingX * 2;
        int boxHeight = 49;

        int x = 8;

        int screenHeight =
                client.getWindow().getGuiScaledHeight();

        int y =
                screenHeight / 2 - boxHeight / 2;

        graphics.fill(
                x,
                y,
                x + boxWidth,
                y + boxHeight,
                COLOR_BACKGROUND
        );

        graphics.text(
                client.font,
                title,
                x + paddingX,
                y + 6,
                COLOR_TEXT,
                true
        );

        if (blueScore >= redScore) {
            drawTeamRankingLine(
                    graphics,
                    client,
                    "블루팀",
                    blueScore,
                    x + paddingX,
                    y + 21,
                    COLOR_BLUE
            );

            drawTeamRankingLine(
                    graphics,
                    client,
                    "레드팀",
                    redScore,
                    x + paddingX,
                    y + 34,
                    COLOR_RED
            );
        } else {
            drawTeamRankingLine(
                    graphics,
                    client,
                    "레드팀",
                    redScore,
                    x + paddingX,
                    y + 21,
                    COLOR_RED
            );

            drawTeamRankingLine(
                    graphics,
                    client,
                    "블루팀",
                    blueScore,
                    x + paddingX,
                    y + 34,
                    COLOR_BLUE
            );
        }
    }

    private static void drawTeamRankingLine(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            String teamName,
            int score,
            int x,
            int y,
            int color
    ) {
        graphics.text(
                client.font,
                teamName + " " + score,
                x,
                y,
                color,
                true
        );
    }
}
