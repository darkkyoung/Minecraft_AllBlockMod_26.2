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
import com.darkk0729.allblocks.challenge.TeamRaceSetupManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

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
                                                ChallengeMenuMessages.showRaceModeMenu(
                                                        context.getSource()
                                                );
                                                return 1;
                                            })
                                    )
                                    .then(Commands.literal("teamrace")
                                            .executes(context -> {
                                                ChallengeMenuMessages.showTeamRaceDifficultyMenu(
                                                        context.getSource()
                                                );
                                                return 1;
                                            })
                                    )
                                    .then(Commands.literal("blockrace")
                                            .executes(context -> {
                                                ChallengeMenuMessages.showBlockRaceComingSoon(
                                                        context.getSource()
                                                );
                                                return 1;
                                            })
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
                                    .then(Commands.literal("coop")
                                            .then(Commands.literal("easy")
                                                    .executes(context -> startCoop(
                                                            context.getSource(),
                                                            ChallengeDifficulty.EASY
                                                    )))
                                            .then(Commands.literal("normal")
                                                    .executes(context -> startCoop(
                                                            context.getSource(),
                                                            ChallengeDifficulty.NORMAL
                                                    )))
                                            .then(Commands.literal("hard")
                                                    .executes(context -> startCoop(
                                                            context.getSource(),
                                                            ChallengeDifficulty.HARD
                                                    )))
                                    )
                            )
                            .then(Commands.literal("stop")
                                    .executes(context -> stop(context.getSource())))
                            .then(Commands.literal("teamrace")
                                    .then(Commands.literal("begin")
                                            .then(Commands.literal("easy")
                                                    .executes(context ->
                                                            beginTeamRaceSetup(
                                                                    context.getSource(),
                                                                    ChallengeDifficulty.EASY
                                                            )))
                                            .then(Commands.literal("normal")
                                                    .executes(context ->
                                                            beginTeamRaceSetup(
                                                                    context.getSource(),
                                                                    ChallengeDifficulty.NORMAL
                                                            )))
                                            .then(Commands.literal("hard")
                                                    .executes(context ->
                                                            beginTeamRaceSetup(
                                                                    context.getSource(),
                                                                    ChallengeDifficulty.HARD
                                                            )))
                                    )
                                    .then(Commands.literal("reroll")
                                            .executes(context ->
                                                    teamRaceRerollMenu(
                                                            context.getSource()
                                                    ))
                                    )
                                    .then(Commands.literal("random")
                                            .executes(context ->
                                                    teamRaceRandom(
                                                            context.getSource()
                                                    ))
                                    )
                                    .then(Commands.literal("manual")
                                            .executes(context ->
                                                    teamRaceManual(
                                                            context.getSource()
                                                    ))
                                    )
                                    .then(Commands.literal("cycle")
                                            .then(Commands.argument(
                                                                    "playerUuid",
                                                                    StringArgumentType.word()
                                                            )
                                                            .executes(context ->
                                                                    teamRaceCycle(
                                                                            context.getSource(),
                                                                            StringArgumentType.getString(
                                                                                    context,
                                                                                    "playerUuid"
                                                                            )
                                                                    ))
                                            )
                                    )
                                    .then(Commands.literal("confirm")
                                            .executes(context ->
                                                    teamRaceConfirm(
                                                            context.getSource()
                                                    ))
                                    )
                            )
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
        ChallengeMenuMessages.showModeMenu(source);
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

    private static int startCoop(
            CommandSourceStack source,
            ChallengeDifficulty difficulty
    ) {
        if (ChallengeManager.isRunning()) {
            source.sendFailure(Component.literal(
                    "[올블록 챌린지] 이미 챌린지가 진행 중입니다."
            ));
            return 0;
        }

        MinecraftServer server = source.getServer();

        ChallengeManager.startCoop(
                server,
                difficulty
        );

        source.sendSuccess(
                () -> Component.literal(
                        "[올블록 챌린지] 협동 "
                                + difficulty.getDisplayName()
                                + " 난이도로 챌린지를 시작했습니다."
                ),
                false
        );

        return 1;
    }

    private static int beginTeamRaceSetup(
            CommandSourceStack source,
            ChallengeDifficulty difficulty
    ) throws CommandSyntaxException {
        if (ChallengeManager.isRunning()) {
            source.sendFailure(Component.literal(
                    "[올블록 챌린지] 이미 챌린지가 진행 중입니다."
            ));
            return 0;
        }

        ServerPlayer player =
                source.getPlayerOrException();

        return TeamRaceSetupManager.begin(
                source.getServer(),
                player,
                difficulty
        ) ? 1 : 0;
    }

    private static int teamRaceRerollMenu(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        TeamRaceSetupManager.showRerollMenu(player);
        return 1;
    }

    private static int teamRaceRandom(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        return TeamRaceSetupManager.randomizeAgain(
                source.getServer(),
                player
        ) ? 1 : 0;
    }

    private static int teamRaceManual(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        return TeamRaceSetupManager.beginManual(
                source.getServer(),
                player
        ) ? 1 : 0;
    }

    private static int teamRaceCycle(
            CommandSourceStack source,
            String playerUuid
    ) throws CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        return TeamRaceSetupManager.cycleManualTeam(
                source.getServer(),
                player,
                playerUuid
        ) ? 1 : 0;
    }

    private static int teamRaceConfirm(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        return TeamRaceSetupManager.confirm(
                source.getServer(),
                player
        ) ? 1 : 0;
    }

    private static int stop(CommandSourceStack source) {
        if (!ChallengeManager.shouldShowHud()
                && !ChallengeManager.isTeamRaceSetupActive()) {
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