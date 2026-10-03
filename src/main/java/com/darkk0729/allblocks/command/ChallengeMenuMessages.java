package com.darkk0729.allblocks.command;

import com.darkk0729.allblocks.challenge.ChallengeManager;
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

    public static void showModeMenu(ServerPlayer player) {
        sendModeMenu(player::sendSystemMessage);
    }

    public static void showRaceModeMenu(ServerPlayer player) {
        sendRaceModeMenu(player::sendSystemMessage);
    }

    public static void showSingleDifficultyMenu(ServerPlayer player) {
        sendDifficultyMenu(
                player::sendSystemMessage,
                "싱글",
                ChatFormatting.GREEN
        );
    }

    public static void showCoopDifficultyMenu(ServerPlayer player) {
        sendDifficultyMenu(
                player::sendSystemMessage,
                "협동",
                ChatFormatting.AQUA
        );
    }

    public static void showTeamRaceDifficultyMenu(ServerPlayer player) {
        sendDifficultyMenu(
                player::sendSystemMessage,
                "팀 레이스",
                ChatFormatting.BLUE
        );
    }

    public static void showBlockRaceDifficultyMenu(ServerPlayer player) {
        sendDifficultyMenu(
                player::sendSystemMessage,
                "블록 레이스",
                ChatFormatting.RED
        );
    }

    public static void showTimeLimitMenu(ServerPlayer player) {
        MessageSender sender = player::sendSystemMessage;

        sender.send(separator());
        sender.send(
                Component.literal("[ 시간 제한 설정 ]")
                        .withStyle(
                                ChatFormatting.GOLD,
                                ChatFormatting.BOLD
                        )
        );
        sender.send(Component.literal(""));
        sender.send(
                Component.literal(
                        "기본 설정은 인게임 시간 100일입니다."
                ).withStyle(ChatFormatting.WHITE)
        );
        sender.send(
                Component.literal(
                        "제한 시간을 직접 설정하려면 채팅에 '120분', '50일'과 같이 입력해주세요."
                ).withStyle(ChatFormatting.WHITE)
        );
        sender.send(
                Component.literal(
                        "시간 제한을 사용하지 않으려면 '없음'을 입력해주세요."
                ).withStyle(ChatFormatting.WHITE)
        );
        sender.send(Component.literal(""));
        sender.send(
                clickable(
                        "[기본 설정: 100일]",
                        ChatFormatting.GREEN,
                        "/올블록 @시간 기본"
                )
        );
        sender.send(separator());
    }

    public static void showWorldTimeMenu(ServerPlayer player) {
        MessageSender sender = player::sendSystemMessage;

        sender.send(separator());
        sender.send(
                Component.literal("[ 월드 시간 설정 ]")
                        .withStyle(
                                ChatFormatting.GOLD,
                                ChatFormatting.BOLD
                        )
        );
        sender.send(Component.literal(""));
        sender.send(
                Component.literal(
                        "챌린지 시작 시 현재 월드 시간을 초기화하시겠습니까?"
                ).withStyle(ChatFormatting.WHITE)
        );
        sender.send(Component.literal(""));
        sender.send(
                clickable(
                        "[현재 시간 유지]",
                        ChatFormatting.GREEN,
                        "/올블록 @월드 유지"
                ).append(
                        Component.literal(
                                " 현재 월드의 시간을 그대로 유지합니다."
                        ).withStyle(ChatFormatting.WHITE)
                )
        );
        sender.send(
                clickable(
                        "[시간 초기화]",
                        ChatFormatting.YELLOW,
                        "/올블록 @월드 초기화"
                ).append(
                        Component.literal(
                                " 오버월드 시간을 Day 1 아침으로 초기화합니다."
                        ).withStyle(ChatFormatting.WHITE)
                )
        );
        sender.send(Component.literal(""));
        sender.send(
                Component.literal(
                        "기존 월드에서 플레이 중이라면 현재 시간 유지를 권장합니다."
                ).withStyle(ChatFormatting.GRAY)
        );
        sender.send(separator());
    }

    public static void showRunningMenu(ServerPlayer player) {
        MessageSender sender = player::sendSystemMessage;

        sender.send(separator());
        sender.send(
                Component.literal("[ 올블록 챌린지 ]")
                        .withStyle(
                                ChatFormatting.GOLD,
                                ChatFormatting.BOLD
                        )
        );
        sender.send(Component.literal(""));
        sender.send(
                Component.literal(
                        "현재 챌린지가 진행 중입니다."
                ).withStyle(ChatFormatting.WHITE)
        );
        sender.send(
                Component.literal(
                        "모드: "
                                + ChallengeManager.getMode().getDisplayName()
                ).withStyle(ChatFormatting.GRAY)
        );
        sender.send(
                Component.literal(
                        "난이도: "
                                + ChallengeManager.getDifficulty().getDisplayName()
                ).withStyle(ChatFormatting.GRAY)
        );
        sender.send(
                Component.literal(
                        "시간 제한: "
                                + ChallengeManager.getTimeLimitDisplayText()
                ).withStyle(ChatFormatting.GRAY)
        );
        sender.send(
                Component.literal(
                        "Day: "
                                + ChallengeManager.getDisplayedDay()
                ).withStyle(ChatFormatting.GRAY)
        );
        sender.send(Component.literal(""));
        sender.send(
                clickable(
                        "[챌린지 종료]",
                        ChatFormatting.RED,
                        "/올블록 @종료 확인"
                )
        );
        sender.send(separator());
    }

    public static void showStopConfirm(ServerPlayer player) {
        MessageSender sender = player::sendSystemMessage;

        sender.send(separator());
        sender.send(
                Component.literal("[ 챌린지 종료 ]")
                        .withStyle(
                                ChatFormatting.RED,
                                ChatFormatting.BOLD
                        )
        );
        sender.send(Component.literal(""));
        sender.send(
                Component.literal(
                        "정말 현재 챌린지를 종료하시겠습니까?"
                ).withStyle(ChatFormatting.WHITE)
        );
        sender.send(Component.literal(""));
        sender.send(
                clickable(
                        "[종료하기]",
                        ChatFormatting.RED,
                        "/올블록 @종료 실행"
                ).append(Component.literal("   "))
                        .append(
                                clickable(
                                        "[돌아가기]",
                                        ChatFormatting.GREEN,
                                        "/올블록 @종료 취소"
                                )
                        )
        );
        sender.send(separator());
    }

    private static void sendWelcome(MessageSender sender) {
        sender.send(separator());
        sender.send(
                Component.literal("[ 올블록 챌린지 ]")
                        .withStyle(
                                ChatFormatting.GOLD,
                                ChatFormatting.BOLD
                        )
        );
        sender.send(
                Component.literal("환영합니다!")
                        .withStyle(ChatFormatting.WHITE)
        );
        sender.send(
                Component.literal(
                        "[올블록 챌린지]는 마인크래프트 내 모든 블록을 모아 도감을 완성하는 모드입니다."
                ).withStyle(ChatFormatting.WHITE)
        );
        sender.send(
                Component.literal(
                        "싱글과 멀티 플레이 모두 가능하며, 추후 API를 통한 시청자 참여 기능도 지원할 예정입니다."
                ).withStyle(ChatFormatting.WHITE)
        );
        sender.send(Component.literal(""));
        sender.send(
                Component.literal(
                        "싱글: 혼자서 모든 블록을 모읍니다."
                ).withStyle(ChatFormatting.WHITE)
        );
        sender.send(
                Component.literal(
                        "협동: 모든 플레이어가 하나의 도감을 함께 완성합니다."
                ).withStyle(ChatFormatting.WHITE)
        );
        sender.send(
                Component.literal(
                        "경쟁: 팀 또는 개인 단위로 블록 소유권과 수집량을 겨루어 승자를 정합니다."
                ).withStyle(ChatFormatting.WHITE)
        );
        sender.send(Component.literal(""));
        sender.send(
                Component.literal("   ")
                        .append(
                                clickable(
                                        "[올블록 챌린지 시작하기]",
                                        ChatFormatting.GREEN,
                                        "/올블록 @시작"
                                )
                        )
        );
        sender.send(separator());
    }

    private static void sendModeMenu(MessageSender sender) {
        sender.send(separator());
        sender.send(
                Component.literal("[ 모드 선택 ]")
                        .withStyle(
                                ChatFormatting.GOLD,
                                ChatFormatting.BOLD
                        )
        );
        sender.send(Component.literal(""));
        sender.send(
                clickable(
                        "[싱글]",
                        ChatFormatting.GREEN,
                        "/올블록 @모드 싱글"
                ).append(
                        Component.literal(
                                " 혼자서 모든 블록을 모아 도감을 완성합니다."
                        ).withStyle(ChatFormatting.WHITE)
                )
        );
        sender.send(Component.literal(""));
        sender.send(
                clickable(
                        "[협동]",
                        ChatFormatting.AQUA,
                        "/올블록 @모드 협동"
                ).append(
                        Component.literal(
                                " 모든 플레이어가 하나의 도감을 함께 완성합니다."
                        ).withStyle(ChatFormatting.WHITE)
                )
        );
        sender.send(Component.literal(""));
        sender.send(
                clickable(
                        "[경쟁]",
                        ChatFormatting.RED,
                        "/올블록 @모드 경쟁"
                ).append(
                        Component.literal(
                                " 팀 레이스 또는 개인 블록 레이스로 경쟁합니다."
                        ).withStyle(ChatFormatting.WHITE)
                )
        );
        sender.send(separator());
    }

    private static void sendRaceModeMenu(MessageSender sender) {
        sender.send(separator());
        sender.send(
                Component.literal("[ 경쟁 모드 선택 ]")
                        .withStyle(
                                ChatFormatting.GOLD,
                                ChatFormatting.BOLD
                        )
        );
        sender.send(Component.literal(""));
        sender.send(
                clickable(
                        "[팀 레이스]",
                        ChatFormatting.BLUE,
                        "/올블록 @경쟁 팀레이스"
                ).append(
                        Component.literal(
                                " 두 팀이 블록 소유권을 두고 경쟁합니다."
                        ).withStyle(ChatFormatting.WHITE)
                )
        );
        sender.send(Component.literal(""));
        sender.send(
                clickable(
                        "[블록 레이스]",
                        ChatFormatting.RED,
                        "/올블록 @경쟁 블록레이스"
                ).append(
                        Component.literal(
                                " 각 플레이어가 개인 수집량을 겨룹니다."
                        ).withStyle(ChatFormatting.WHITE)
                )
        );
        sender.send(separator());
    }

    private static void sendDifficultyMenu(
            MessageSender sender,
            String modeName,
            ChatFormatting titleColor
    ) {
        sender.send(separator());
        sender.send(
                Component.literal(
                        "[ " + modeName + " 난이도 선택 ]"
                ).withStyle(
                        titleColor,
                        ChatFormatting.BOLD
                )
        );
        sender.send(Component.literal(""));
        sender.send(
                clickable(
                        "[쉬움]",
                        ChatFormatting.GREEN,
                        "/올블록 @난이도 쉬움"
                ).append(
                        Component.literal(
                                " 진행률 이벤트와 Day 이벤트 없이 플레이합니다."
                        ).withStyle(ChatFormatting.WHITE)
                )
        );
        sender.send(
                clickable(
                        "[보통]",
                        ChatFormatting.YELLOW,
                        "/올블록 @난이도 보통"
                ).append(
                        Component.literal(
                                " 기본 진행률 이벤트와 Day 이벤트가 적용됩니다."
                        ).withStyle(ChatFormatting.WHITE)
                )
        );
        sender.send(
                clickable(
                        "[어려움]",
                        ChatFormatting.RED,
                        "/올블록 @난이도 어려움"
                ).append(
                        Component.literal(
                                " 강화된 진행률 이벤트와 Day 이벤트가 적용됩니다."
                        ).withStyle(ChatFormatting.WHITE)
                )
        );
        sender.send(separator());
    }

    private static MutableComponent clickable(
            String text,
            ChatFormatting color,
            String command
    ) {
        return Component.literal(text)
                .withStyle(style -> style
                        .withColor(color)
                        .withBold(true)
                        .withClickEvent(
                                new ClickEvent.RunCommand(command)
                        )
                );
    }

    private static MutableComponent separator() {
        return Component.literal(
                "━━━━━━━━━━━━━━━━━━━━"
        ).withStyle(ChatFormatting.DARK_GRAY);
    }

    @FunctionalInterface
    private interface MessageSender {
        void send(Component component);
    }
}
