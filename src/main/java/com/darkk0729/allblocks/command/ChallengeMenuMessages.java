package com.darkk0729.allblocks.command;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class ChallengeMenuMessages {
    private ChallengeMenuMessages() {
    }

    public static void showWelcome(CommandSourceStack source) {
        sendWelcome(line -> source.sendSuccess(() -> line, false));
    }

    public static void showWelcome(ServerPlayer player) {
        sendWelcome(player::sendSystemMessage);
    }

    public static void showModeMenu(CommandSourceStack source) {
        sendModeMenu(line -> source.sendSuccess(() -> line, false));
    }

    public static void showModeMenu(ServerPlayer player) {
        sendModeMenu(player::sendSystemMessage);
    }

    public static void showSingleDifficultyMenu(CommandSourceStack source) {
        sendSingleDifficultyMenu(line -> source.sendSuccess(() -> line, false));
    }

    public static void showCoopDifficultyMenu(CommandSourceStack source) {
        sendCoopDifficultyMenu(line -> source.sendSuccess(() -> line, false));
    }

    public static void showRaceDifficultyMenu(CommandSourceStack source) {
        sendRaceDifficultyMenu(line -> source.sendSuccess(() -> line, false));
    }

    public static void showCoopComingSoon(CommandSourceStack source) {
        source.sendSuccess(() -> prefix()
                .append(Component.literal("협동 모드는 아직 개발 중입니다. 지금은 싱글 모드만 사용할 수 있습니다.")
                        .withStyle(ChatFormatting.WHITE)), false);
    }

    public static void showRaceComingSoon(CommandSourceStack source) {
        source.sendSuccess(() -> prefix()
                .append(Component.literal("경쟁 모드는 아직 개발 중입니다. 지금은 싱글 모드만 사용할 수 있습니다.")
                        .withStyle(ChatFormatting.WHITE
                        )), false);
    }

    private static void sendWelcome(MessageSender sender) {
        sender.send(separator());
        sender.send(Component.literal("[ 올블록 챌린지 ]")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        sender.send(Component.literal("환영합니다!")
                .withStyle(ChatFormatting.WHITE));
        sender.send(Component.literal("이 모드는 마인크래프트에서 수집 가능한 모든 블록을 모아")
                .withStyle(ChatFormatting.WHITE));
        sender.send(Component.literal("도감을 완성하는 챌린지 모드입니다.")
                .withStyle(ChatFormatting.WHITE));

        sender.send(Component.literal(""));
        sender.send(Component.literal("싱글: 혼자서 모든 블록을 모읍니다.")
                .withStyle(ChatFormatting.WHITE));
        sender.send(Component.literal("협동: 모든 플레이어가 하나의 도감을 함께 완성합니다.")
                .withStyle(ChatFormatting.WHITE));
        sender.send(Component.literal("경쟁: 가장 많은 블록을 모은 플레이어가 승리합니다.")
                .withStyle(ChatFormatting.WHITE));

        sender.send(Component.literal(""));
        sender.send(Component.literal("   ")
                .append(clickable(
                        "[올블록 챌린지 시작하기]",
                        ChatFormatting.GREEN,
                        "/allblocks menu modes"
                )));

        sender.send(separator());
    }

    private static void sendModeMenu(MessageSender sender) {
        sender.send(separator());
        sender.send(Component.literal("[ 모드 선택 ]")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        sender.send(Component.literal(""));
        sender.send(clickable("[싱글]", ChatFormatting.GREEN, "/allblocks menu single")
                .append(Component.literal(" 혼자서 모든 블록을 모아 도감을 완성합니다.")
                        .withStyle(ChatFormatting.WHITE)));

        sender.send(clickable("[협동]", ChatFormatting.AQUA, "/allblocks menu coop")
                .append(Component.literal(" 모든 플레이어가 하나의 도감을 함께 완성합니다.")
                        .withStyle(ChatFormatting.WHITE)));

        sender.send(clickable("[경쟁]", ChatFormatting.RED, "/allblocks menu race")
                .append(Component.literal(" 블록을 가장 많이 모은 1등 플레이어를 선발합니다.")
                        .withStyle(ChatFormatting.WHITE)));

        sender.send(separator());
    }

    private static void sendSingleDifficultyMenu(MessageSender sender) {
        sender.send(separator());
        sender.send(Component.literal("[ 싱글 난이도 선택 ]")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        sender.send(Component.literal(""));
        sender.send(clickable("[쉬움]", ChatFormatting.GREEN, "/allblocks start single easy")
                .append(Component.literal(" 이벤트와 100일 제한 없이 평화롭게 모든 블록을 모읍니다.")
                        .withStyle(ChatFormatting.WHITE)));

        sender.send(clickable("[보통]", ChatFormatting.YELLOW, "/allblocks start single normal")
                .append(Component.literal(" 이벤트는 없지만 100일 제한이 있습니다.")
                        .withStyle(ChatFormatting.WHITE)));

        sender.send(clickable("[어려움]", ChatFormatting.RED, "/allblocks start single hard")
                .append(Component.literal(" 진행률 이벤트와 Day 이벤트가 발생합니다.")
                        .withStyle(ChatFormatting.WHITE)));

        sender.send(separator());
    }

    private static void sendCoopDifficultyMenu(MessageSender sender) {
        sender.send(separator());
        sender.send(Component.literal("[ 협동 난이도 선택 ]")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        sender.send(Component.literal("모든 플레이어가 하나의 도감을 함께 완성합니다.")
                .withStyle(ChatFormatting.WHITE));
        sender.send(Component.literal("멀티 동기화 구현 후 사용할 수 있습니다.")
                .withStyle(ChatFormatting.DARK_GRAY));

        sender.send(Component.literal(""));
        sender.send(clickable("[쉬움]", ChatFormatting.GREEN, "/allblocks menu comingsoon coop")
                .append(Component.literal(" 이벤트 없이 함께 도감을 완성합니다.")
                        .withStyle(ChatFormatting.WHITE)));

        sender.send(clickable("[보통]", ChatFormatting.YELLOW, "/allblocks menu comingsoon coop")
                .append(Component.literal(" 약한 이벤트가 발생합니다.")
                        .withStyle(ChatFormatting.WHITE)));

        sender.send(clickable("[어려움]", ChatFormatting.RED, "/allblocks menu comingsoon coop")
                .append(Component.literal(" 강력한 이벤트가 발생합니다.")
                        .withStyle(ChatFormatting.WHITE)));

        sender.send(separator());
    }

    private static void sendRaceDifficultyMenu(MessageSender sender) {
        sender.send(separator());
        sender.send(Component.literal("[ 경쟁 난이도 선택 ]")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        sender.send(Component.literal("각 플레이어가 블록 수집량을 겨룹니다.")
                .withStyle(ChatFormatting.WHITE));
        sender.send(Component.literal("멀티 경쟁 시스템 구현 후 사용할 수 있습니다.")
                .withStyle(ChatFormatting.DARK_GRAY));

        sender.send(Component.literal(""));
        sender.send(clickable("[쉬움]", ChatFormatting.GREEN, "/allblocks menu comingsoon race")
                .append(Component.literal(" 이벤트 없이 순수하게 수집량을 겨룹니다.")
                        .withStyle(ChatFormatting.WHITE)));

        sender.send(clickable("[보통]", ChatFormatting.YELLOW, "/allblocks menu comingsoon race")
                .append(Component.literal(" 경쟁용 보통 이벤트가 발생합니다.")
                        .withStyle(ChatFormatting.WHITE)));

        sender.send(clickable("[어려움]", ChatFormatting.RED, "/allblocks menu comingsoon race")
                .append(Component.literal(" 차등 이벤트가 포함된 고난도 경쟁 모드입니다.")
                        .withStyle(ChatFormatting.WHITE)));

        sender.send(separator());
    }

    private static MutableComponent clickable(String text, ChatFormatting color, String command) {
        return Component.literal(text).withStyle(style -> style
                .withColor(color)
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand(command))
        );
    }

    private static MutableComponent separator() {
        return Component.literal("━━━━━━━━━━━━━━━━━━━━")
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    private static MutableComponent prefix() {
        return Component.literal("[올블록 챌린지] ")
                .withStyle(ChatFormatting.GOLD);
    }

    @FunctionalInterface
    private interface MessageSender {
        void send(Component component);
    }
}