package com.darkk0729.allblocks.client.hud;

import com.darkk0729.allblocks.challenge.TeamRaceTeam;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class TeamRevealHud {
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(
                    "allblocks",
                    "team_reveal_hud"
            );

    private static final long FAST_DURATION_MS = 3000L;
    private static final long SLOW_DURATION_MS = 4000L;
    private static final long HOLD_DURATION_MS = 2000L;
    private static final long TOTAL_REVEAL_MS =
            FAST_DURATION_MS + SLOW_DURATION_MS;

    private static final long[] SLOW_SWITCHES = {
            250L, 550L, 900L, 1300L,
            1800L, 2400L, 3100L, 4000L
    };

    private static boolean active = false;
    private static long startTimeMs = 0L;
    private static TeamRaceTeam finalTeam = TeamRaceTeam.NONE;

    private TeamRevealHud() {
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.BOSS_BAR,
                HUD_ID,
                TeamRevealHud::render
        );
    }

    public static void start(String finalTeamName) {
        TeamRaceTeam team =
                TeamRaceTeam.fromName(finalTeamName);

        if (!team.isAssigned()) {
            return;
        }

        finalTeam = team;
        startTimeMs = System.currentTimeMillis();
        active = true;
    }

    public static void clear() {
        active = false;
        finalTeam = TeamRaceTeam.NONE;
        startTimeMs = 0L;
    }

    private static void render(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker
    ) {
        if (!active) return;

        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            return;
        }

        long elapsed =
                System.currentTimeMillis() - startTimeMs;

        if (elapsed >= TOTAL_REVEAL_MS + HOLD_DURATION_MS) {
            clear();
            return;
        }

        TeamRaceTeam displayedTeam =
                getDisplayedTeam(elapsed);

        int width =
                client.getWindow().getGuiScaledWidth();

        int height =
                client.getWindow().getGuiScaledHeight();

        int centerX = width / 2;
        int centerY = height / 2;

        drawCenteredScaledText(
                graphics,
                client,
                "당신은",
                centerX,
                centerY - 55,
                0xFFFFFFFF,
                1.3F
        );

        int teamColor =
                displayedTeam == TeamRaceTeam.RED
                        ? 0xFFFF5555
                        : 0xFF5555FF;

        drawCenteredScaledText(
                graphics,
                client,
                displayedTeam.getDisplayName(),
                centerX,
                centerY - 22,
                teamColor,
                3.0F
        );

        drawCenteredScaledText(
                graphics,
                client,
                "입니다",
                centerX,
                centerY + 35,
                0xFFFFFFFF,
                1.3F
        );
    }

    private static TeamRaceTeam getDisplayedTeam(long elapsed) {
        if (elapsed >= TOTAL_REVEAL_MS) {
            return finalTeam;
        }

        int switchCount;

        if (elapsed < FAST_DURATION_MS) {
            switchCount = (int) (elapsed / 120L);
        } else {
            switchCount =
                    (int) (FAST_DURATION_MS / 120L);

            long slowElapsed =
                    elapsed - FAST_DURATION_MS;

            for (long switchTime : SLOW_SWITCHES) {
                if (slowElapsed >= switchTime) {
                    switchCount++;
                }
            }
        }

        return switchCount % 2 == 0
                ? TeamRaceTeam.BLUE
                : TeamRaceTeam.RED;
    }

    private static void drawCenteredScaledText(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            String text,
            int centerX,
            int y,
            int color,
            float scale
    ) {
        int textWidth =
                client.font.width(text);

        var matrices = graphics.pose();

        matrices.pushMatrix();
        matrices.translate(centerX, y);
        matrices.scale(scale, scale);

        graphics.text(
                client.font,
                text,
                -textWidth / 2,
                0,
                color,
                true
        );

        matrices.popMatrix();
    }
}