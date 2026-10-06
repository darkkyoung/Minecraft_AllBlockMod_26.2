package com.darkk0729.allblocks.command;

import com.darkk0729.allblocks.challenge.BlockRaceSetupManager;
import com.darkk0729.allblocks.challenge.ChallengeDifficulty;
import com.darkk0729.allblocks.challenge.ChallengeManager;
import com.darkk0729.allblocks.challenge.ChallengeMode;
import com.darkk0729.allblocks.challenge.ChallengeSetupManager;
import com.darkk0729.allblocks.challenge.ChallengeTimeLimitType;
import com.darkk0729.allblocks.challenge.TeamRaceSetupManager;
import com.darkk0729.allblocks.event.ChallengeEventManager;
import com.darkk0729.allblocks.event.DayRaidManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class AllBlocksCommands {
    private AllBlocksCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        dispatcher.register(
                                Commands.literal("올블록")
                                        .executes(context ->
                                                showRoot(
                                                        context.getSource()
                                                )
                                        )
                                        .then(
                                                Commands.literal("디버그")
                                                        .requires(
                                                                Commands.hasPermission(
                                                                        Commands.LEVEL_GAMEMASTERS
                                                                )
                                                        )
                                                        .executes(context ->
                                                                showDebugMenu(
                                                                        context.getSource()
                                                                )
                                                        )
                                                        .then(
                                                                Commands.literal("레이드")
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "day",
                                                                                                IntegerArgumentType.integer(10, 90)
                                                                                        )
                                                                                        .executes(context -> {
                                                                                            int day =
                                                                                                    IntegerArgumentType.getInteger(
                                                                                                            context,
                                                                                                            "day"
                                                                                                    );

                                                                                            if (day % 10 != 0) {
                                                                                                context.getSource()
                                                                                                        .sendFailure(
                                                                                                                Component.literal(
                                                                                                                        "[올블록 디버그] 레이드 일차는 10, 20, 30, ..., 90 중 하나여야 합니다."
                                                                                                                )
                                                                                                        );
                                                                                                return 0;
                                                                                            }

                                                                                            DayRaidManager.startDebugRaid(
                                                                                                    context.getSource().getServer(),
                                                                                                    day
                                                                                            );
                                                                                            return 1;
                                                                                        })
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.literal("일차")
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "day",
                                                                                                IntegerArgumentType.integer(1, 10000)
                                                                                        )
                                                                                        .executes(context -> {
                                                                                            ChallengeManager.debugSetDay(
                                                                                                    context.getSource().getServer(),
                                                                                                    IntegerArgumentType.getInteger(
                                                                                                            context,
                                                                                                            "day"
                                                                                                    )
                                                                                            );
                                                                                            return 1;
                                                                                        })
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.literal("수집")
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "count",
                                                                                                IntegerArgumentType.integer(1, 2000)
                                                                                        )
                                                                                        .executes(context -> {
                                                                                            ServerPlayer player =
                                                                                                    context.getSource()
                                                                                                            .getPlayerOrException();

                                                                                            ChallengeManager.debugCollectBlocks(
                                                                                                    context.getSource().getServer(),
                                                                                                    player,
                                                                                                    IntegerArgumentType.getInteger(
                                                                                                            context,
                                                                                                            "count"
                                                                                                    )
                                                                                            );
                                                                                            return 1;
                                                                                        })
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.literal("진행률")
                                                                        .then(
                                                                                Commands.argument(
                                                                                                "percent",
                                                                                                IntegerArgumentType.integer(10, 100)
                                                                                        )
                                                                                        .executes(context -> {
                                                                                            ChallengeEventManager.startDebugProgressEvent(
                                                                                                    context.getSource().getServer(),
                                                                                                    IntegerArgumentType.getInteger(
                                                                                                            context,
                                                                                                            "percent"
                                                                                                    )
                                                                                            );
                                                                                            return 1;
                                                                                        })
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.argument(
                                                                "internal",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(context ->
                                                                handleInternalAction(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "internal"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
        );
    }

    private static int showDebugMenu(
            CommandSourceStack source
    ) {
        source.sendSuccess(
                () -> Component.literal(
                        "━━━━━━━━━━━━━━━━━━━━"
                ),
                false
        );
        source.sendSuccess(
                () -> Component.literal(
                        "[ 올블록 디버그 ]"
                ),
                false
        );
        source.sendSuccess(
                () -> Component.literal(
                        "/올블록 디버그 수집 <개수>  - 블록 강제 수집"
                ),
                false
        );
        source.sendSuccess(
                () -> Component.literal(
                        "/올블록 디버그 진행률 <10~100>  - 진행률 이벤트 강제 실행"
                ),
                false
        );
        source.sendSuccess(
                () -> Component.literal(
                        "/올블록 디버그 일차 <일차>  - 챌린지 일차 이동"
                ),
                false
        );
        source.sendSuccess(
                () -> Component.literal(
                        "/올블록 디버그 레이드 <10~90>  - Day 레이드 강제 실행"
                ),
                false
        );
        source.sendSuccess(
                () -> Component.literal(
                        "━━━━━━━━━━━━━━━━━━━━"
                ),
                false
        );
        return 1;
    }

    private static int showRoot(
            CommandSourceStack source
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        if (ChallengeManager.isRunning()) {
            ChallengeMenuMessages.showRunningMenu(player);
            return 1;
        }

        ChallengeMenuMessages.showWelcome(player);
        return 1;
    }

    private static Integer parseIntegerActionValue(
            String action,
            String prefix
    ) {
        if (action == null
                || prefix == null
                || !action.startsWith(prefix)) {
            return null;
        }

        try {
            return Integer.parseInt(
                    action.substring(
                            prefix.length()
                    ).trim()
            );
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int handleInternalAction(
            CommandSourceStack source,
            String rawAction
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String action = rawAction == null
                ? ""
                : rawAction.trim();

        if (action.equals("@시작")) {
            return ChallengeSetupManager.begin(
                    source.getServer(),
                    player
            ) ? 1 : 0;
        }

        if (action.equals("@모드 싱글")) {
            return ChallengeSetupManager.selectMode(
                    player,
                    ChallengeMode.SOLO
            ) ? 1 : 0;
        }

        if (action.equals("@모드 협동")) {
            return ChallengeSetupManager.selectMode(
                    player,
                    ChallengeMode.CO_OP
            ) ? 1 : 0;
        }

        if (action.equals("@모드 경쟁")) {
            return ChallengeSetupManager.showRaceModeMenu(
                    player
            ) ? 1 : 0;
        }

        if (action.equals("@경쟁 팀레이스")) {
            return ChallengeSetupManager.selectMode(
                    player,
                    ChallengeMode.TEAM_RACE
            ) ? 1 : 0;
        }

        if (action.equals("@경쟁 블록레이스")) {
            return ChallengeSetupManager.selectMode(
                    player,
                    ChallengeMode.BLOCK_RACE
            ) ? 1 : 0;
        }

        if (action.startsWith("@난이도 ")) {
            ChallengeDifficulty difficulty =
                    switch (action.substring("@난이도 ".length())) {
                        case "쉬움" -> ChallengeDifficulty.EASY;
                        case "보통" -> ChallengeDifficulty.NORMAL;
                        case "어려움" -> ChallengeDifficulty.HARD;
                        default -> null;
                    };

            return difficulty != null
                    && ChallengeSetupManager.selectDifficulty(
                            player,
                            difficulty
                    ) ? 1 : 0;
        }

        if (action.startsWith("@설정 시간종류 ")) {
            ChallengeTimeLimitType type =
                    switch (action.substring(
                            "@설정 시간종류 ".length()
                    )) {
                        case "인게임" ->
                                ChallengeTimeLimitType.IN_GAME_TIME;
                        case "실제" ->
                                ChallengeTimeLimitType.PLAY_TIME;
                        case "없음" ->
                                ChallengeTimeLimitType.NONE;
                        default -> null;
                    };

            return type != null
                    && ChallengeSetupManager
                    .selectTimeLimitType(
                            player,
                            type
                    ) ? 1 : 0;
        }

        if (action.startsWith("@설정 시간값 ")) {
            Integer value =
                    parseIntegerActionValue(
                            action,
                            "@설정 시간값 "
                    );

            return value != null
                    && ChallengeSetupManager
                    .selectTimeLimitPreset(
                            player,
                            value
                    ) ? 1 : 0;
        }

        if (action.equals("@설정 시간수동")) {
            return ChallengeSetupManager
                    .selectTimeLimitManual(
                            player
                    ) ? 1 : 0;
        }

        if (action.startsWith("@설정 시간조정 ")) {
            Integer delta =
                    parseIntegerActionValue(
                            action,
                            "@설정 시간조정 "
                    );

            return delta != null
                    && ChallengeSetupManager
                    .adjustTimeLimit(
                            player,
                            delta
                    ) ? 1 : 0;
        }

        if (action.startsWith("@설정 진행률값 ")) {
            Integer value =
                    parseIntegerActionValue(
                            action,
                            "@설정 진행률값 "
                    );

            return value != null
                    && ChallengeSetupManager
                    .selectProgressEventInterval(
                            player,
                            value
                    ) ? 1 : 0;
        }

        if (action.equals("@설정 진행률수동")) {
            return ChallengeSetupManager
                    .selectProgressEventManual(
                            player
                    ) ? 1 : 0;
        }

        if (action.startsWith("@설정 진행률조정 ")) {
            Integer delta =
                    parseIntegerActionValue(
                            action,
                            "@설정 진행률조정 "
                    );

            return delta != null
                    && ChallengeSetupManager
                    .adjustProgressEventInterval(
                            player,
                            delta
                    ) ? 1 : 0;
        }

        if (action.startsWith("@설정 데이값 ")) {
            Integer value =
                    parseIntegerActionValue(
                            action,
                            "@설정 데이값 "
                    );

            return value != null
                    && ChallengeSetupManager
                    .selectDayRaidInterval(
                            player,
                            value
                    ) ? 1 : 0;
        }

        if (action.equals("@설정 데이수동")) {
            return ChallengeSetupManager
                    .selectDayRaidManual(
                            player
                    ) ? 1 : 0;
        }

        if (action.startsWith("@설정 데이조정 ")) {
            Integer delta =
                    parseIntegerActionValue(
                            action,
                            "@설정 데이조정 "
                    );

            return delta != null
                    && ChallengeSetupManager
                    .adjustDayRaidInterval(
                            player,
                            delta
                    ) ? 1 : 0;
        }

        if (action.equals("@설정 월드 유지")) {
            return ChallengeSetupManager.selectWorldTime(
                    player,
                    false
            ) ? 1 : 0;
        }

        if (action.equals("@설정 월드 초기화")) {
            return ChallengeSetupManager.selectWorldTime(
                    player,
                    true
            ) ? 1 : 0;
        }

        if (action.equals("@설정 완료")) {
            return ChallengeSetupManager.finishSettings(
                    source.getServer(),
                    player
            ) ? 1 : 0;
        }

        if (action.equals("@종료 확인")) {
            if (!ChallengeManager.isRunning()) {
                ChallengeMenuMessages.showWelcome(player);
                return 0;
            }

            ChallengeMenuMessages.showStopConfirm(player);
            return 1;
        }

        if (action.equals("@종료 취소")) {
            if (ChallengeManager.isRunning()) {
                ChallengeMenuMessages.showRunningMenu(player);
            } else {
                ChallengeMenuMessages.showWelcome(player);
            }
            return 1;
        }

        if (action.equals("@종료 실행")) {
            if (!ChallengeManager.isRunning()
                    && !ChallengeManager.shouldShowHud()
                    && !TeamRaceSetupManager.isActive()
                    && !BlockRaceSetupManager.isActive()) {
                player.sendSystemMessage(
                        Component.literal(
                                "[올블록 챌린지] 종료할 챌린지가 없습니다."
                        )
                );
                return 0;
            }

            ChallengeManager.stop(source.getServer());
            player.sendSystemMessage(
                    Component.literal(
                            "[올블록 챌린지] 챌린지를 종료했습니다."
                    )
            );
            return 1;
        }

        if (action.equals("@팀 재추첨메뉴")) {
            TeamRaceSetupManager.showRerollMenu(player);
            return 1;
        }

        if (action.equals("@팀 랜덤")) {
            return TeamRaceSetupManager.randomizeAgain(
                    source.getServer(),
                    player
            ) ? 1 : 0;
        }

        if (action.equals("@팀 수동")) {
            return TeamRaceSetupManager.beginManual(
                    source.getServer(),
                    player
            ) ? 1 : 0;
        }

        if (action.startsWith("@팀 순환 ")) {
            return TeamRaceSetupManager.cycleManualTeam(
                    source.getServer(),
                    player,
                    action.substring("@팀 순환 ".length())
            ) ? 1 : 0;
        }

        if (action.equals("@팀 확정")) {
            return TeamRaceSetupManager.confirm(
                    source.getServer(),
                    player
            ) ? 1 : 0;
        }

        source.sendFailure(
                Component.literal(
                        "[올블록 챌린지] 알 수 없는 메뉴 동작입니다."
                )
        );
        return 0;
    }
}
