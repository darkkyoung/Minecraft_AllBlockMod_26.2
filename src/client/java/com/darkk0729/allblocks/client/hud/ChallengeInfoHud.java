package com.darkk0729.allblocks.client.hud;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import com.darkk0729.allblocks.challenge.ChallengeDifficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

public final class ChallengeInfoHud {
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath("allblocks", "challenge_info_hud");

    private static final int COLOR_DAY = 0xFFFFD85A;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM = 0xFFBDBDBD;

    private static final int COLOR_DIFFICULTY_EASY = 0xFF55FF55;
    private static final int COLOR_DIFFICULTY_NORMAL = 0xFFFFFF55;
    private static final int COLOR_DIFFICULTY_HARD = 0xFFFF5555;

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
        drawCodexShortcutHint(graphics, client);
    }

    private static void drawLeftInfo(GuiGraphicsExtractor graphics, Minecraft client) {
        BlockPos pos = client.player.blockPosition();

        String dayText = ChallengeManager.getCurrentDay() + 1 + "일차";
        String timerText = "타이머 : " + ChallengeManager.getFormattedElapsedTime();
        String xyzText = "좌표 : " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        String biomeText = "생물 군계 : " + getBiomeName(client, pos);
        String difficultyText = "난이도 : " + ChallengeManager.getDifficulty().getDisplayName();

        int x = 8;
        int y = 8;

        drawScaledText(graphics, client, dayText, x, y, COLOR_DAY, true, 2.0F);

        graphics.text(client.font, timerText, x, y + 27, COLOR_TEXT, true);
        graphics.text(client.font, xyzText, x, y + 41, COLOR_TEXT, true);
        graphics.text(client.font, biomeText, x, y + 55, COLOR_TEXT, true);
        graphics.text(client.font, difficultyText, x, y + 69, getDifficultyColor(), true);
    }

    private static void drawCodexShortcutHint(
            GuiGraphicsExtractor graphics,
            Minecraft client
    ) {
        int size = 20;

        int hotbarRight =
                client.getWindow().getGuiScaledWidth() / 2 + 91;

        int x = hotbarRight + 6;
        int y = client.getWindow().getGuiScaledHeight() - 22;

        // 인벤토리 슬롯 같은 어두운 배경
        graphics.fill(
                x,
                y,
                x + size,
                y + size,
                0x88000000
        );

        graphics.outline(
                x,
                y,
                size,
                size,
                0xFF9A9A9A
        );

        // 책 아이콘
        ItemStack bookStack = new ItemStack(Items.BOOK);

        graphics.item(
                bookStack,
                x + 2,
                y + 2
        );

        // 우하단 B 키 표시
        graphics.fill(
                x + 11,
                y + 10,
                x + 20,
                y + 20,
                0xCC171717
        );

        graphics.text(
                client.font,
                "B",
                x + 13,
                y + 11,
                0xFFFFFFFF,
                true
        );
    }


    private static int getDifficultyColor() {
        ChallengeDifficulty difficulty = ChallengeManager.getDifficulty();

        if (difficulty == null) {
            return COLOR_DIFFICULTY_HARD;
        }

        return switch (difficulty) {
            case EASY -> COLOR_DIFFICULTY_EASY;
            case NORMAL -> COLOR_DIFFICULTY_NORMAL;
            case HARD -> COLOR_DIFFICULTY_HARD;
        };
    }

    private static String getBiomeName(
            Minecraft client,
            BlockPos pos
    ) {
        try {
            var biomeKeyOptional =
                    client.level
                            .getBiome(pos)
                            .unwrapKey();

            if (biomeKeyOptional.isEmpty()) {
                return "알 수 없음";
            }

            var biomeId =
                    biomeKeyOptional
                            .get()
                            .identifier();

            String translationKey =
                    biomeId.toLanguageKey("biome");

            // 번역이 존재하면 현재 게임 언어 기준으로 반환
            if (Language.getInstance().has(translationKey)) {
                return Component
                        .translatable(translationKey)
                        .getString();
            }

            // 모드 생물 군계 등에 번역이 없는 경우 fallback
            return prettifyBiomeId(
                    biomeId.toString()
            );

        } catch (Exception ignored) {
            return "알 수 없음";
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