package com.darkk0729.allblocks.command;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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
            );
        });
    }

    private static int start(CommandSourceStack source) {
        if (ChallengeManager.isRunning()) {
            source.sendFailure(Component.literal("[AllBlocks] Challenge is already running."));
            return 0;
        }

        ChallengeManager.startSolo();
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

        ChallengeManager.stop();
        source.sendSuccess(
                () -> Component.literal("[AllBlocks] All Blocks Challenge stopped."),
                false
        );
        return 1;
    }

    private static int status(CommandSourceStack source) {
        if (ChallengeManager.isRunning()) {
            source.sendSuccess(
                    () -> Component.literal("[AllBlocks] Status: Running / Mode: "
                            + ChallengeManager.getMode().getDisplayName()),
                    false
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal("[AllBlocks] Status: Not running."),
                    false
            );
        }

        return 1;
    }
}