package com.darkk0729.allblocks.command;

import com.darkk0729.allblocks.challenge.ChallengeDifficulty;
import com.darkk0729.allblocks.challenge.ChallengeManager;
import com.darkk0729.allblocks.event.DayRaidManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AllBlocksCommands {
    private AllBlocksCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("allblocks")
                            .then(Commands.literal("menu")
                                    .executes(context -> {
                                        ChallengeMenuMessages.showWelcome(context.getSource());
                                        return 1;
                                    })
                                    .then(Commands.literal("modes")
                                            .executes(context -> {
                                                ChallengeMenuMessages.showModeMenu(context.getSource());
                                                return 1;
                                            })
                                    )
                                    .then(Commands.literal("single")
                                            .executes(context -> {
                                                ChallengeMenuMessages.showSingleDifficultyMenu(context.getSource());
                                                return 1;
                                            })
                                    )
                                    .then(Commands.literal("coop")
                                            .executes(context -> {
                                                ChallengeMenuMessages.showCoopDifficultyMenu(context.getSource());
                                                return 1;
                                            })
                                    )
                                    .then(Commands.literal("race")
                                            .executes(context -> {
                                                ChallengeMenuMessages.showRaceDifficultyMenu(context.getSource());
                                                return 1;
                                            })
                                    )
                                    .then(Commands.literal("comingsoon")
                                            .then(Commands.literal("coop")
                                                    .executes(context -> {
                                                        ChallengeMenuMessages.showCoopComingSoon(context.getSource());
                                                        return 1;
                                                    })
                                            )
                                            .then(Commands.literal("race")
                                                    .executes(context -> {
                                                        ChallengeMenuMessages.showRaceComingSoon(context.getSource());
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                            .then(Commands.literal("start")
                                    .executes(context -> showStartUsage(context.getSource()))
                                    .then(Commands.literal("single")
                                            .then(Commands.literal("easy")
                                                    .executes(context -> startSingle(
                                                            context.getSource(),
                                                            ChallengeDifficulty.EASY
                                                    )))
                                            .then(Commands.literal("normal")
                                                    .executes(context -> startSingle(
                                                            context.getSource(),
                                                            ChallengeDifficulty.NORMAL
                                                    )))
                                            .then(Commands.literal("hard")
                                                    .executes(context -> startSingle(
                                                            context.getSource(),
                                                            ChallengeDifficulty.HARD
                                                    )))
                                    )
                            )
                            .then(Commands.literal("stop")
                                    .executes(context -> stop(context.getSource())))
                            .then(Commands.literal("status")
                                    .executes(context -> status(context.getSource())))
                            .then(Commands.literal("progress")
                                    .executes(context -> progress(context.getSource())))
                            .then(Commands.literal("debug")
                                    .then(Commands.literal("raid")
                                            .then(Commands.argument("day", IntegerArgumentType.integer(10, 90))
                                                    .executes((CommandContext<CommandSourceStack> context) -> {
                                                        int day = IntegerArgumentType.getInteger(context, "day");

                                                        if (day % 10 != 0) {
                                                            context.getSource().sendSuccess(
                                                                    () -> Component.literal("[AllBlocks] Raid day must be 10, 20, 30, ..., 90."),
                                                                    false
                                                            );
                                                            return 0;
                                                        }

                                                        DayRaidManager.startDebugRaid(
                                                                context.getSource().getServer(),
                                                                day
                                                        );

                                                        context.getSource().sendSuccess(
                                                                () -> Component.literal("[AllBlocks] Debug raid requested: Day " + day),
                                                                false
                                                        );

                                                        return 1;
                                                    })
                                            )
                                    )
                                    .then(Commands.literal("day")
                                            .then(Commands.argument("day", IntegerArgumentType.integer(1, 100))
                                                    .executes(context -> {
                                                        int day = IntegerArgumentType.getInteger(context, "day");
                                                        ChallengeManager.debugSetDay(context.getSource().getServer(), day);
                                                        return 1;
                                                    })
                                            )
                                    )
                                    .then(Commands.literal("collect")
                                            .then(Commands.argument("count", IntegerArgumentType.integer(1, 2000))
                                                    .executes(context -> {
                                                        ServerPlayer player = context.getSource().getPlayerOrException();
                                                        int count = IntegerArgumentType.getInteger(context, "count");

                                                        ChallengeManager.debugCollectBlocks(
                                                                context.getSource().getServer(),
                                                                player,
                                                                count
                                                        );

                                                        return 1;
                                                    })
                                            )
                                    )
                            )
            );
        });
    }

    private static int showStartUsage(CommandSourceStack source) {
        ChallengeMenuMessages.showSingleDifficultyMenu(source);
        return 1;
    }

    private static int startSingle(CommandSourceStack source, ChallengeDifficulty difficulty) {
        if (ChallengeManager.isRunning()) {
            source.sendFailure(Component.literal("[올블록 챌린지] 이미 챌린지가 진행 중입니다."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        ChallengeManager.startSingle(server, difficulty);

        source.sendSuccess(
                () -> Component.literal(
                        "[올블록 챌린지] 싱글 "
                                + difficulty.getDisplayName()
                                + " 난이도로 챌린지를 시작했습니다."
                ),
                false
        );

        return 1;
    }

    private static int stop(CommandSourceStack source) {
        if (!ChallengeManager.shouldShowHud()) {
            source.sendFailure(Component.literal("[올블록 챌린지] 종료할 챌린지가 없습니다."));
            return 0;
        }

        String finalTime = ChallengeManager.getFormattedElapsedTime();
        int finalDay = ChallengeManager.getDisplayedDay();
        int collected = ChallengeManager.getCollectedCount();
        int total = ChallengeManager.getTotalTargetCount();
        double percent = ChallengeManager.getProgressPercent();

        MinecraftServer server = source.getServer();
        ChallengeManager.stop(server);

        source.sendSuccess(
                () -> Component.literal(String.format(
                        "[올블록 챌린지] 챌린지를 종료했습니다. 최종 Day: %d / 시간: %s / 도감: %d/%d (%.2f%%)",
                        finalDay,
                        finalTime,
                        collected,
                        total,
                        percent
                )),
                false
        );

        return 1;
    }

    private static int status(CommandSourceStack source) {
        if (ChallengeManager.isRunning()) {
            source.sendSuccess(
                    () -> Component.literal(String.format(
                            "[올블록 챌린지] 상태: 진행 중 / 모드: %s / 난이도: %s / Day: %d / 시간: %s / 도감: %d/%d (%.2f%%)",
                            ChallengeManager.getMode().getDisplayName(),
                            ChallengeManager.getDifficulty().getDisplayName(),
                            ChallengeManager.getDisplayedDay(),
                            ChallengeManager.getFormattedElapsedTime(),
                            ChallengeManager.getCollectedCount(),
                            ChallengeManager.getTotalTargetCount(),
                            ChallengeManager.getProgressPercent()
                    )),
                    false
            );

            return 1;
        }

        if (ChallengeManager.isFinished()) {
            source.sendSuccess(
                    () -> Component.literal(String.format(
                            "[올블록 챌린지] 상태: 결과 확정 / 결과: %s / 난이도: %s / Day: %d / 시간: %s / 도감: %d/%d (%.2f%%)",
                            ChallengeManager.getResult(),
                            ChallengeManager.getDifficulty().getDisplayName(),
                            ChallengeManager.getDisplayedDay(),
                            ChallengeManager.getFormattedElapsedTime(),
                            ChallengeManager.getCollectedCount(),
                            ChallengeManager.getTotalTargetCount(),
                            ChallengeManager.getProgressPercent()
                    )),
                    false
            );

            return 1;
        }

        source.sendSuccess(
                () -> Component.literal(String.format(
                        "[올블록 챌린지] 상태: 대기 중 / 마지막 도감: %d/%d (%.2f%%)",
                        ChallengeManager.getCollectedCount(),
                        ChallengeManager.getTotalTargetCount(),
                        ChallengeManager.getProgressPercent()
                )),
                false
        );

        return 1;
    }

    private static int progress(CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.literal(String.format(
                        "[올블록 챌린지] 도감 진행률: %d/%d (%.2f%%)",
                        ChallengeManager.getCollectedCount(),
                        ChallengeManager.getTotalTargetCount(),
                        ChallengeManager.getProgressPercent()
                )),
                false
        );

        return 1;
    }
}