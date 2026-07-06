package com.darkk0729.allblocks.challenge;

import com.darkk0729.allblocks.AllBlocksMod;
import com.darkk0729.allblocks.data.AllBlocksSaveManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ChallengeManager {
    private static final long AUTO_SAVE_INTERVAL_TICKS = 20L * 30L;

    private static ChallengeState state = new ChallengeState();
    private static long ticksSinceLastSave = 0L;

    private ChallengeManager() {
    }

    public static boolean isRunning() {
        return state.isRunning();
    }

    public static ChallengeMode getMode() {
        return state.getMode();
    }

    public static long getElapsedTicks() {
        return state.getElapsedTicks();
    }

    public static int getCurrentDay() {
        return state.getCurrentDay();
    }

    public static String getFormattedElapsedTime() {
        return state.getFormattedElapsedTime();
    }

    public static void load(MinecraftServer server) {
        state = AllBlocksSaveManager.load(server);
        ticksSinceLastSave = 0L;

        AllBlocksMod.LOGGER.info(
                "AllBlocks state loaded. Running: {}, Mode: {}, Day: {}, Time: {}",
                state.isRunning(),
                state.getMode().getDisplayName(),
                state.getCurrentDay(),
                state.getFormattedElapsedTime()
        );
    }

    public static void save(MinecraftServer server) {
        AllBlocksSaveManager.save(server, state);
    }

    public static void startSolo(MinecraftServer server) {
        state.start(ChallengeMode.SOLO);
        ticksSinceLastSave = 0L;
        save(server);
    }

    public static void stop(MinecraftServer server) {
        state.stop();
        ticksSinceLastSave = 0L;
        save(server);
    }

    public static void tick(MinecraftServer server) {
        if (!state.isRunning()) {
            return;
        }

        boolean shouldEnd = state.tick();

        if (shouldEnd) {
            state.stop();
            save(server);
            broadcast(server, Component.literal("[AllBlocks] Day 101 reached. Challenge automatically stopped."));
            return;
        }

        ticksSinceLastSave++;

        if (ticksSinceLastSave >= AUTO_SAVE_INTERVAL_TICKS) {
            ticksSinceLastSave = 0L;
            save(server);
        }
    }

    private static void broadcast(MinecraftServer server, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }
}