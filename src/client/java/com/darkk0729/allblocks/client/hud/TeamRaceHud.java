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
    private static final int COLOR_TEXT = 0xFFFFFFFF;
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

        drawRanking(graphics, client);
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