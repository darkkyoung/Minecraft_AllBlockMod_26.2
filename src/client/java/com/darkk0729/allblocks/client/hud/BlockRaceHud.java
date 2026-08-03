package com.darkk0729.allblocks.client.hud;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class BlockRaceHud {
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath("allblocks", "codex_progress_hud");

    private static final Identifier PROGRESS_BAR_TEXTURE =
            Identifier.fromNamespaceAndPath("allblocks", "textures/gui/codex_progress_bar.png");

    private static final int TEXTURE_WIDTH = 1750;
    private static final int TEXTURE_HEIGHT = 273;

    private static final int HUD_WIDTH = 210;
    private static final int HUD_HEIGHT = 45;

    private static final int HUD_Y = 0;

    private static final int FILL_X = 22;
    private static final int FILL_Y = 16;
    private static final int FILL_WIDTH = 166;
    private static final int FILL_HEIGHT = 4;

    private static final int COLOR_PROGRESS_GREEN = 0xFF00A020;
    private static final int COLOR_PROGRESS_DARK_GREEN = 0xFF005014;

    private BlockRaceHud() {
    }

    public static void register() {
        /*
         * 도감 HUD를 바닐라 보스바보다 먼저 그린다.
         * 평상시에는 도감 HUD가 보이고,
         * 엔더 드래곤/위더 보스바가 있으면 바닐라 보스바가 그 위에 덮인다.
         */
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.BOSS_BAR,
                HUD_ID,
                BlockRaceHud::render
        );
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            return;
        }

        if (!ChallengeManager.shouldShowHud()) {
            return;
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();

        int x = (screenWidth - HUD_WIDTH) / 2;
        int y = HUD_Y;

        drawFrame(graphics, x, y);
        drawProgressFill(graphics, x, y);
    }

    private static void drawFrame(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                PROGRESS_BAR_TEXTURE,
                x,
                y,
                0.0F,
                0.0F,
                HUD_WIDTH,
                HUD_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );
    }

    private static void drawProgressFill(GuiGraphicsExtractor graphics, int x, int y) {
        int total = Math.max(1, ChallengeManager.getTotalTargetCount());
        int collected = Math.max(0, ChallengeManager.getCollectedCount());

        int progressWidth;

        if (collected <= 0) {
            progressWidth = 0;
        } else {
            progressWidth = Math.max(1, Math.min(
                    FILL_WIDTH,
                    (int) Math.round((collected * FILL_WIDTH) / (double) total)
            ));
        }

        if (progressWidth <= 0) {
            return;
        }

        int fillX = x + FILL_X;
        int fillY = y + FILL_Y;

        graphics.fill(
                fillX,
                fillY,
                fillX + progressWidth,
                fillY + FILL_HEIGHT,
                COLOR_PROGRESS_GREEN
        );

        graphics.fill(
                fillX,
                fillY + FILL_HEIGHT - 2,
                fillX + progressWidth,
                fillY + FILL_HEIGHT,
                COLOR_PROGRESS_DARK_GREEN
        );
    }
}