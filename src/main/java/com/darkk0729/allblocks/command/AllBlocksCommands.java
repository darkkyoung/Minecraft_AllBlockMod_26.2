package com.darkk0729.allblocks.command;

import com.darkk0729.allblocks.challenge.BlockRaceSetupManager;
import com.darkk0729.allblocks.challenge.ChallengeDifficulty;
import com.darkk0729.allblocks.challenge.ChallengeManager;
import com.darkk0729.allblocks.challenge.ChallengeMode;
import com.darkk0729.allblocks.challenge.ChallengeSetupManager;
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

        if (action.equals("@시간 기본")) {
            return ChallengeSetupManager.useDefaultTimeLimit(
                    player
            ) ? 1 : 0;
        }

        if (action.equals("@월드 유지")) {
            return ChallengeSetupManager.selectWorldTime(
                    source.getServer(),
                    player,
                    false
            ) ? 1 : 0;
        }

        if (action.equals("@월드 초기화")) {
            return ChallengeSetupManager.selectWorldTime(
                    source.getServer(),
                    player,
                    true
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
