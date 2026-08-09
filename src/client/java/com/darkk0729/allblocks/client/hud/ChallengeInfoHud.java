package com.darkk0729.allblocks.client.hud;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

public final class ChallengeInfoHud {
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath("allblocks", "challenge_info_hud");

    private static final int COLOR_DAY = 0xFFFFD85A;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM = 0xFFBDBDBD;

    private ChallengeInfoHud() {
    }

    public static void register() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.BOSS_BAR,
                HUD_ID,
                ChallengeInfoHud::render
        );
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null) {
            return;
        }

        if (!ChallengeManager.shouldShowHud()) {
            return;
        }

        drawLeftInfo(graphics, client);
    }

    private static void drawLeftInfo(GuiGraphicsExtractor graphics, Minecraft client) {
        BlockPos pos = client.player.blockPosition();

        String dayText = ChallengeManager.getCurrentDay() + "일차";
        String timerText = "타이머 : " + ChallengeManager.getFormattedElapsedTime();
        String xyzText = "좌표 : " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        String biomeText = "생물 균계 : " + getBiomeName(client, pos);

        int x = 8;
        int y = 8;

        drawScaledText(graphics, client, dayText, x, y, COLOR_DAY, true, 2.0F);

        graphics.text(client.font, timerText, x, y + 25, COLOR_TEXT, true);
        graphics.text(client.font, xyzText, x, y + 39, COLOR_TEXT, true);
        graphics.text(client.font, biomeText, x, y + 53, COLOR_TEXT, true);
    }

    private static String getBiomeName(Minecraft client, BlockPos pos) {
        try {
            var biomeKeyOptional = client.level.getBiome(pos).unwrapKey();

            if (biomeKeyOptional.isEmpty()) {
                return "Unknown";
            }

            return prettifyBiomeId(biomeKeyOptional.get().toString());
        } catch (Exception ignored) {
            return "Unknown";
        }
    }

    private static String prettifyBiomeId(String id) {
        if (id == null || id.isBlank()) {
            return "Unknown";
        }

        String path = id.trim();

        int slashIndex = path.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex + 1 < path.length()) {
            path = path.substring(slashIndex + 1).trim();
        }

        if (path.endsWith("]")) {
            path = path.substring(0, path.length() - 1).trim();
        }

        int namespaceSplit = path.indexOf(':');
        if (namespaceSplit >= 0 && namespaceSplit + 1 < path.length()) {
            path = path.substring(namespaceSplit + 1);
        }

        String[] parts = path.split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));

            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.isEmpty() ? "Unknown" : builder.toString();
    }

    private static void drawScaledText(
            GuiGraphicsExtractor graphics,
            Minecraft client,
            String text,
            int x,
            int y,
            int color,
            boolean shadow,
            float scale
    ) {
        var matrices = graphics.pose();

        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);

        graphics.text(client.font, text, 0, 0, color, shadow);

        matrices.popMatrix();
    }
}