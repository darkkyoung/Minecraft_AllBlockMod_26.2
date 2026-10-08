package com.darkk0729.allblocks.command;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import com.darkk0729.allblocks.challenge.ChallengeDifficulty;
import com.darkk0729.allblocks.challenge.ChallengeState;
import com.darkk0729.allblocks.challenge.ChallengeTimeLimitType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
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

    public static void showGameSettings(
            ServerPlayer player,
            ChallengeDifficulty difficulty,
            ChallengeTimeLimitType timeLimitType,
            long timeLimitTicks,
            boolean timeLimitManual,
            int progressEventIntervalPercent,
            boolean progressEventManual,
            int dayRaidIntervalDays,
            boolean dayRaidManual,
            boolean resetWorldTime
    ) {
        MessageSender sender =
                player::sendSystemMessage;

        ChallengeDifficulty safeDifficulty =
                difficulty == null
                        ? ChallengeDifficulty.NORMAL
                        : difficulty;

        ChallengeTimeLimitType safeTimeLimitType =
                timeLimitType == null
                        ? ChallengeTimeLimitType.IN_GAME_TIME
                        : timeLimitType;

        sender.send(separator());
        sender.send(
                Component.literal("[ 게임 설정 ]")
                        .withStyle(
                                ChatFormatting.WHITE,
                                ChatFormatting.BOLD
                        )
        );
        sender.send(Component.literal(""));
        sender.send(
                plain(
                        "아래 항목을 클릭해서 설정을 변경할 수 있습니다."
                )
        );
        sender.send(Component.literal(""));

        sender.send(sectionTitle("난이도"));
        sender.send(
                plain(
                        "이벤트와 레이드의 종류 및 강도를 설정합니다."
                )
        );
        sender.send(
                row(
                        selectable(
                                "[쉬움]",
                                safeDifficulty
                                        == ChallengeDifficulty.EASY,
                                "/올블록 @난이도 쉬움"
                        ),
                        selectable(
                                "[보통]",
                                safeDifficulty
                                        == ChallengeDifficulty.NORMAL,
                                "/올블록 @난이도 보통"
                        ),
                        selectable(
                                "[어려움]",
                                safeDifficulty
                                        == ChallengeDifficulty.HARD,
                                "/올블록 @난이도 어려움"
                        )
                )
        );

        sender.send(Component.literal(""));
        sender.send(sectionTitle("시간 제한"));
        sender.send(
                plain(
                        "챌린지를 완료해야 하는 제한 시간을 설정합니다."
                )
        );
        sender.send(
                row(
                        selectable(
                                "[인게임 시간]",
                                safeTimeLimitType
                                        == ChallengeTimeLimitType.IN_GAME_TIME,
                                "/올블록 @설정 시간종류 인게임"
                        ),
                        selectable(
                                "[실제 시간]",
                                safeTimeLimitType
                                        == ChallengeTimeLimitType.PLAY_TIME,
                                "/올블록 @설정 시간종류 실제"
                        ),
                        selectable(
                                "[없음]",
                                safeTimeLimitType
                                        == ChallengeTimeLimitType.NONE,
                                "/올블록 @설정 시간종류 없음"
                        )
                )
        );

        if (safeTimeLimitType
                != ChallengeTimeLimitType.NONE) {
            boolean inGame =
                    safeTimeLimitType
                            == ChallengeTimeLimitType.IN_GAME_TIME;

            long unitTicks =
                    inGame
                            ? ChallengeState.TICKS_PER_DAY
                            : ChallengeState.TICKS_PER_SECOND
                            * 60L;

            long amount =
                    Math.max(
                            1L,
                            timeLimitTicks / unitTicks
                    );

            if (inGame) {
                sender.send(
                        row(
                                selectable(
                                        "[25일]",
                                        !timeLimitManual
                                                && amount == 25L,
                                        "/올블록 @설정 시간값 25"
                                ),
                                selectable(
                                        "[50일]",
                                        !timeLimitManual
                                                && amount == 50L,
                                        "/올블록 @설정 시간값 50"
                                ),
                                selectable(
                                        "[75일]",
                                        !timeLimitManual
                                                && amount == 75L,
                                        "/올블록 @설정 시간값 75"
                                ),
                                selectable(
                                        "[100일]",
                                        !timeLimitManual
                                                && amount == 100L,
                                        "/올블록 @설정 시간값 100"
                                ),
                                selectable(
                                        "[수동]",
                                        timeLimitManual,
                                        "/올블록 @설정 시간수동"
                                )
                        )
                );
            } else {
                sender.send(
                        row(
                                selectable(
                                        "[60분]",
                                        !timeLimitManual
                                                && amount == 60L,
                                        "/올블록 @설정 시간값 60"
                                ),
                                selectable(
                                        "[100분]",
                                        !timeLimitManual
                                                && amount == 100L,
                                        "/올블록 @설정 시간값 100"
                                ),
                                selectable(
                                        "[180분]",
                                        !timeLimitManual
                                                && amount == 180L,
                                        "/올블록 @설정 시간값 180"
                                ),
                                selectable(
                                        "[360분]",
                                        !timeLimitManual
                                                && amount == 360L,
                                        "/올블록 @설정 시간값 360"
                                ),
                                selectable(
                                        "[수동]",
                                        timeLimitManual,
                                        "/올블록 @설정 시간수동"
                                )
                        )
                );
            }

            if (timeLimitManual) {
                sender.send(
                        manualRow(
                                amount
                                        + (inGame
                                        ? "일"
                                        : "분"),
                                "시간조정"
                        )
                );
            }
        }

        sender.send(Component.literal(""));
        sender.send(sectionTitle("진행률 이벤트"));

        if (safeDifficulty
                == ChallengeDifficulty.EASY) {
            sender.send(
                    plain(
                            "쉬움 난이도에서는 진행률 이벤트가 발생하지 않습니다."
                    )
            );
            sender.send(
                    row(
                            disabledOption(
                                    "[없음]",
                                    true
                            ),
                            disabledOption(
                                    "[5%]",
                                    false
                            ),
                            disabledOption(
                                    "[10%]",
                                    false
                            ),
                            disabledOption(
                                    "[20%]",
                                    false
                            ),
                            disabledOption(
                                    "[수동]",
                                    false
                            )
                    )
            );
        } else {
            sender.send(
                    plain(
                            "블록 수집 진행률이 일정 비율에 도달할 때마다 이벤트가 발생합니다."
                    )
            );
            sender.send(
                    row(
                            selectable(
                                    "[없음]",
                                    !progressEventManual
                                            && progressEventIntervalPercent
                                            == 0,
                                    "/올블록 @설정 진행률값 0"
                            ),
                            selectable(
                                    "[5%]",
                                    !progressEventManual
                                            && progressEventIntervalPercent
                                            == 5,
                                    "/올블록 @설정 진행률값 5"
                            ),
                            selectable(
                                    "[10%]",
                                    !progressEventManual
                                            && progressEventIntervalPercent
                                            == 10,
                                    "/올블록 @설정 진행률값 10"
                            ),
                            selectable(
                                    "[20%]",
                                    !progressEventManual
                                            && progressEventIntervalPercent
                                            == 20,
                                    "/올블록 @설정 진행률값 20"
                            ),
                            selectable(
                                    "[수동]",
                                    progressEventManual,
                                    "/올블록 @설정 진행률수동"
                            )
                    )
            );

            if (progressEventManual) {
                sender.send(
                        manualRow(
                                progressEventIntervalPercent
                                        + "%",
                                "진행률조정"
                        )
                );
            }
        }

        sender.send(Component.literal(""));
        sender.send(sectionTitle("Day 이벤트"));

        if (safeDifficulty
                == ChallengeDifficulty.EASY) {
            sender.send(
                    plain(
                            "쉬움 난이도에서는 Day 레이드 이벤트가 발생하지 않습니다."
                    )
            );
            sender.send(
                    row(
                            disabledOption(
                                    "[없음]",
                                    true
                            ),
                            disabledOption(
                                    "[5일]",
                                    false
                            ),
                            disabledOption(
                                    "[10일]",
                                    false
                            ),
                            disabledOption(
                                    "[20일]",
                                    false
                            ),
                            disabledOption(
                                    "[수동]",
                                    false
                            )
                    )
            );
        } else {
            sender.send(
                    plain(
                            "챌린지 시작 후 일정 인게임 Day마다 레이드 이벤트가 발생합니다."
                    )
            );
            sender.send(
                    row(
                            selectable(
                                    "[없음]",
                                    !dayRaidManual
                                            && dayRaidIntervalDays
                                            == 0,
                                    "/올블록 @설정 데이값 0"
                            ),
                            selectable(
                                    "[5일]",
                                    !dayRaidManual
                                            && dayRaidIntervalDays
                                            == 5,
                                    "/올블록 @설정 데이값 5"
                            ),
                            selectable(
                                    "[10일]",
                                    !dayRaidManual
                                            && dayRaidIntervalDays
                                            == 10,
                                    "/올블록 @설정 데이값 10"
                            ),
                            selectable(
                                    "[20일]",
                                    !dayRaidManual
                                            && dayRaidIntervalDays
                                            == 20,
                                    "/올블록 @설정 데이값 20"
                            ),
                            selectable(
                                    "[수동]",
                                    dayRaidManual,
                                    "/올블록 @설정 데이수동"
                            )
                    )
            );

            if (dayRaidManual) {
                sender.send(
                        manualRow(
                                dayRaidIntervalDays
                                        + "일",
                                "데이조정"
                        )
                );
            }
        }

        sender.send(Component.literal(""));
        sender.send(sectionTitle("월드 시간"));
        sender.send(
                plain(
                        "챌린지 시작 시 월드 시간을 유지하거나 Day 1 아침으로 초기화합니다."
                )
        );
        sender.send(
                row(
                        selectable(
                                "[현재 시간 유지]",
                                !resetWorldTime,
                                "/올블록 @설정 월드 유지"
                        ),
                        selectable(
                                "[Day 1 초기화]",
                                resetWorldTime,
                                "/올블록 @설정 월드 초기화"
                        )
                )
        );

        sender.send(Component.literal(""));
        sender.send(
                row(
                        primaryActionButton(
                                "[설정 완료]",
                                "/올블록 @설정 완료"
                        ),
                        primaryActionButton(
                                "[돌아가기]",
                                "/올블록 @설정 돌아가기"
                        )
                )
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

    private static MutableComponent sectionTitle(
            String text
    ) {
        return Component.literal(text)
                .withStyle(
                        ChatFormatting.WHITE,
                        ChatFormatting.BOLD
                );
    }

    private static MutableComponent plain(
            String text
    ) {
        return Component.literal(text)
                .withStyle(ChatFormatting.WHITE);
    }

    private static MutableComponent selectable(
            String text,
            boolean selected,
            String command
    ) {
        return clickable(
                text,
                selected
                        ? ChatFormatting.GREEN
                        : ChatFormatting.GRAY,
                command
        );
    }

    private static MutableComponent disabledOption(
            String text,
            boolean selected
    ) {
        return Component.literal(text)
                .withStyle(
                        selected
                                ? ChatFormatting.GREEN
                                : ChatFormatting.GRAY,
                        ChatFormatting.BOLD
                );
    }

    private static MutableComponent actionButton(
            String text,
            String command
    ) {
        return clickable(
                text,
                ChatFormatting.WHITE,
                command
        );
    }

    private static MutableComponent manualValue(
            String text
    ) {
        return Component.literal(text)
                .withStyle(
                        ChatFormatting.YELLOW,
                        ChatFormatting.BOLD
                );
    }

    private static MutableComponent primaryActionButton(
            String text,
            String command
    ) {
        return Component.literal(text)
                .withStyle(style -> style
                        .withColor(
                                TextColor.fromRgb(
                                        0xB6FF66
                                )
                        )
                        .withBold(true)
                        .withClickEvent(
                                new ClickEvent.RunCommand(
                                        command
                                )
                        )
                );
    }

    private static MutableComponent row(
            MutableComponent... components
    ) {
        MutableComponent line =
                Component.literal("");

        for (int i = 0;
             i < components.length;
             i++) {
            if (i > 0) {
                line.append(
                        Component.literal(" ")
                );
            }

            line.append(components[i]);
        }

        return line;
    }

    private static MutableComponent manualRow(
            String valueText,
            String actionName
    ) {
        return row(
                actionButton(
                        "[-10]",
                        "/올블록 @설정 "
                                + actionName
                                + " -10"
                ),
                actionButton(
                        "[-5]",
                        "/올블록 @설정 "
                                + actionName
                                + " -5"
                ),
                actionButton(
                        "[-1]",
                        "/올블록 @설정 "
                                + actionName
                                + " -1"
                ),
                manualValue(valueText),
                actionButton(
                        "[+1]",
                        "/올블록 @설정 "
                                + actionName
                                + " 1"
                ),
                actionButton(
                        "[+5]",
                        "/올블록 @설정 "
                                + actionName
                                + " 5"
                ),
                actionButton(
                        "[+10]",
                        "/올블록 @설정 "
                                + actionName
                                + " 10"
                )
        );
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
