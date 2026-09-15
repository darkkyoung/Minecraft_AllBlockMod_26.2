package com.darkk0729.allblocks.client.hud;

import com.darkk0729.allblocks.client.data.ClientChallengeStateCache;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class TeamRaceHud {
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(
                    "allblocks",
                    "team_race_hud"
            );

    private static final int COLOR_BLUE = 0xFF3C6FFF;
    private static final int COLOR_RED = 0xFFFF4A4A;
    private static final int COLOR_EMPTY = 0xFF303030;
    private static final int COLOR_BORDER = 0xFFD0D0D0;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_DIM = 0xFFBBBBBB;
    private static final int COLOR_BACKGROUND = 0x99000000;

    private TeamRaceHud() {
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.BOSS_BAR,
                HUD_ID,
                TeamRaceHud::render
        );
    }

    private static void render(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker
    ) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null
                || client.level == null
                || !ClientChallengeStateCache.shouldShowHud()
                || !ClientChallengeStateCache.isTeamRace()) {
            return;
        }

        drawCompetitionBar(graphics, client);
        drawRanking(graphics, client);
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

        int screenWidth =
                client.getWindow().getGuiScaledWidth();

        int barWidth =
                Math.min(300, screenWidth - 80);

        int barHeight = 10;

        int x =
                (screenWidth - barWidth) / 2;

        int y = 12;

        int ownWidth =
                (int) Math.floor(
                        barWidth
                                * (ownScore / (double) total)
                );

        int enemyWidth =
                (int) Math.floor(
                        barWidth
                                * (enemyScore / (double) total)
                );

        ownWidth =
                Math.max(0, Math.min(barWidth, ownWidth));

        enemyWidth =
                Math.max(
                        0,
                        Math.min(
                                barWidth - ownWidth,
                                enemyWidth
                        )
                );

        graphics.fill(
                x,
                y,
                x + barWidth,
                y + barHeight,
                COLOR_EMPTY
        );

        if (ownWidth > 0) {
            graphics.fill(
                    x,
                    y,
                    x + ownWidth,
                    y + barHeight,
                    ownColor
            );
        }

        if (enemyWidth > 0) {
            graphics.fill(
                    x + barWidth - enemyWidth,
                    y,
                    x + barWidth,
                    y + barHeight,
                    enemyColor
            );
        }

        graphics.outline(
                x,
                y,
                barWidth,
                barHeight,
                COLOR_BORDER
        );

        String leftText =
                ownName + " " + ownScore;

        String rightText =
                enemyScore + " " + enemyName;

        graphics.text(
                client.font,
                leftText,
                x,
                y + 13,
                ownColor,
                true
        );

        int rightWidth =
                client.font.width(rightText);

        graphics.text(
                client.font,
                rightText,
                x + barWidth - rightWidth,
                y + 13,
                enemyColor,
                true
        );

        String remainingText =
                "미획득 "
                        + Math.max(
                        0,
                        total - blueScore - redScore
                );

        int remainingWidth =
                client.font.width(remainingText);

        graphics.text(
                client.font,
                remainingText,
                x + (barWidth - remainingWidth) / 2,
                y + 13,
                COLOR_DIM,
                true
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

        int boxWidth = 95;
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

        graphics.outline(
                x,
                y,
                boxWidth,
                boxHeight,
                0xFF777777
        );

        graphics.text(
                client.font,
                "올블록 순위",
                x + 7,
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
                    x + 7,
                    y + 21,
                    COLOR_BLUE
            );

            drawTeamRankingLine(
                    graphics,
                    client,
                    "레드팀",
                    redScore,
                    x + 7,
                    y + 34,
                    COLOR_RED
            );
        } else {
            drawTeamRankingLine(
                    graphics,
                    client,
                    "레드팀",
                    redScore,
                    x + 7,
                    y + 21,
                    COLOR_RED
            );

            drawTeamRankingLine(
                    graphics,
                    client,
                    "블루팀",
                    blueScore,
                    x + 7,
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