package com.darkk0729.allblocks.command;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class AllBlocksCommands {
    private AllBlocksCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("allblocks")
                            .then(Commands.literal("start")
                                    .executes(context -> start(context.getSource())))
                            .then(Commands.literal("stop")
                                    .executes(context -> stop(context.getSource())))
                            .then(Commands.literal("status")
                                    .executes(context -> status(context.getSource())))
                            .then(Commands.literal("progress")
                                    .executes(context -> progress(context.getSource())))
            );
        });
    }

    private static int start(CommandSourceStack source) {
        if (ChallengeManager.isRunning()) {
            source.sendFailure(Component.literal("[AllBlocks] Challenge is already running."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        ChallengeManager.startSolo(server);

        source.sendSuccess(
                () -> Component.literal("[AllBlocks] All Blocks Challenge started. Mode: Solo"),
                false
        );

        return 1;
    }

    private static int stop(CommandSourceStack source) {
        if (!ChallengeManager.isRunning()) {
            source.sendFailure(Component.literal("[AllBlocks] No challenge is currently running."));
            return 0;
        }

        String finalTime = ChallengeManager.getFormattedElapsedTime();
        int finalDay = ChallengeManager.getCurrentDay();
        int collected = ChallengeManager.getCollectedCount();
        int total = ChallengeManager.getTotalTargetCount();
        double percent = ChallengeManager.getProgressPercent();

        MinecraftServer server = source.getServer();
        ChallengeManager.stop(server);

        source.sendSuccess(
                () -> Component.literal(String.format(
                        "[AllBlocks] Challenge stopped. Final Day: %d / Time: %s / Progress: %d/%d (%.2f%%)",
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
                            "[AllBlocks] Status: Running / Mode: %s / Day: %d / Time: %s / Progress: %d/%d (%.2f%%)",
                            ChallengeManager.getMode().getDisplayName(),
                            ChallengeManager.getCurrentDay(),
                            ChallengeManager.getFormattedElapsedTime(),
                            ChallengeManager.getCollectedCount(),
                            ChallengeManager.getTotalTargetCount(),
                            ChallengeManager.getProgressPercent()
                    )),
                    false
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal(String.format(
                            "[AllBlocks] Status: Not running. Last Progress: %d/%d (%.2f%%)",
                            ChallengeManager.getCollectedCount(),
                            ChallengeManager.getTotalTargetCount(),
                            ChallengeManager.getProgressPercent()
                    )),
                    false
            );
        }

        return 1;
    }

    private static int progress(CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.literal(String.format(
                        "[AllBlocks] Progress: %d/%d (%.2f%%)",
                        ChallengeManager.getCollectedCount(),
                        ChallengeManager.getTotalTargetCount(),
                        ChallengeManager.getProgressPercent()
                )),
                false
        );

        return 1;
    }
}