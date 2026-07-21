package com.darkk0729.allblocks.challenge;

import com.darkk0729.allblocks.AllBlocksMod;
import com.darkk0729.allblocks.collection.BlockCollectionTracker;
import com.darkk0729.allblocks.collection.TargetBlockRegistry;
import com.darkk0729.allblocks.data.AllBlocksSaveManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.darkk0729.allblocks.event.ChallengeEventManager;

import java.util.Locale;

public final class ChallengeManager {
    private static final long AUTO_SAVE_INTERVAL_TICKS = 20L * 30L;
    private static final long BOSS_BAR_UPDATE_INTERVAL_TICKS = 20L;

    private static final String PROGRESS_BOSSBAR_ID = "allblocks:progress";

    private static ChallengeState state = new ChallengeState();
    private static long ticksSinceLastSave = 0L;
    private static long ticksSinceLastBossBarUpdate = 0L;
    private static boolean bossBarCreated = false;

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

    public static int getCollectedCount() {
        return state.getCollectedCount();
    }

    public static int getTotalTargetCount() {
        return TargetBlockRegistry.getTotalTargetCount();
    }

    public static double getProgressPercent() {
        int total = getTotalTargetCount();

        if (total <= 0) {
            return 0.0D;
        }

        return (getCollectedCount() * 100.0D) / total;
    }

    public static int getLastProgressEventTier() {
        return state.getLastProgressEventTier();
    }

    public static int getLastDayRaidEventDay() {
        return state.getLastDayRaidEventDay();
    }

    public static void setLastDayRaidEventDay(int day) {
        state.setLastDayRaidEventDay(day);
    }

    public static void setLastProgressEventTier(int tier) {
        state.setLastProgressEventTier(tier);
    }

    public static void load(MinecraftServer server) {
        state = AllBlocksSaveManager.load(server);
        ticksSinceLastSave = 0L;
        ticksSinceLastBossBarUpdate = 0L;
        bossBarCreated = false;

        AllBlocksMod.LOGGER.info(
                "AllBlocks state loaded. Running: {}, Mode: {}, Day: {}, Time: {}, Collected: {}/{}",
                state.isRunning(),
                state.getMode().getDisplayName(),
                state.getCurrentDay(),
                state.getFormattedElapsedTime(),
                getCollectedCount(),
                getTotalTargetCount()
        );

        if (state.isRunning()) {
            updateProgressBossBar(server);
        }
    }

    public static void save(MinecraftServer server) {
        AllBlocksSaveManager.save(server, state);
    }

    public static void startSolo(MinecraftServer server) {
        state.start(ChallengeMode.SOLO);
        ticksSinceLastSave = 0L;
        ticksSinceLastBossBarUpdate = 0L;

        save(server);
        recreateProgressBossBar(server);
        updateProgressBossBar(server);
    }

    public static void stop(MinecraftServer server) {
        state.stop();
        ticksSinceLastSave = 0L;
        ticksSinceLastBossBarUpdate = 0L;

        save(server);
        removeProgressBossBar(server);
    }

    public static void tick(MinecraftServer server) {
        if (!state.isRunning()) {
            return;
        }

        boolean shouldEnd = state.tick();

        if (shouldEnd) {
            state.stop();
            save(server);
            removeProgressBossBar(server);
            broadcast(server, Component.literal("[AllBlocks] Day 101 reached. Challenge automatically stopped."));
            return;
        }

        BlockCollectionTracker.tick(server);
        ChallengeEventManager.tick(server);

        ticksSinceLastBossBarUpdate++;
        if (ticksSinceLastBossBarUpdate >= BOSS_BAR_UPDATE_INTERVAL_TICKS) {
            ticksSinceLastBossBarUpdate = 0L;
            updateProgressBossBar(server);
        }

        ticksSinceLastSave++;
        if (ticksSinceLastSave >= AUTO_SAVE_INTERVAL_TICKS) {
            ticksSinceLastSave = 0L;
            save(server);
        }
    }

    public static boolean collectBlock(MinecraftServer server, ServerPlayer player, String blockId) {
        boolean collected = state.collectBlock(
                blockId,
                player.getUUID(),
                player.getName().getString()
        );

        if (collected) {
            save(server);
            updateProgressBossBar(server);
        }

        return collected;
    }

    public static void handlePlayerDeath(MinecraftServer server, ServerPlayer player, boolean pvpDeath) {
        if (server == null || player == null) {
            return;
        }

        if (!state.isRunning()) {
            return;
        }

        int minPercent = pvpDeath ? 5 : 0;
        int maxPercent = pvpDeath ? 20 : 10;

        int releasedCount = state.releaseRandomOwnedBlocks(
                player.getUUID(),
                minPercent,
                maxPercent
        );

        save(server);
        updateProgressBossBar(server);

        if (releasedCount > 0) {
            player.sendSystemMessage(Component.literal(
                    "[AllBlocks] Death penalty: You lost " + releasedCount + " collected block(s)."
            ));
        } else {
            player.sendSystemMessage(Component.literal(
                    "[AllBlocks] Death penalty: No collected blocks were lost."
            ));
        }
    }

    private static void recreateProgressBossBar(MinecraftServer server) {
        runServerCommand(server, "bossbar remove " + PROGRESS_BOSSBAR_ID);

        String titleJson = toTextJson(buildBossBarTitleText());

        runServerCommand(server, "bossbar add " + PROGRESS_BOSSBAR_ID + " " + titleJson);
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " color green");
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " style progress");
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " visible true");

        bossBarCreated = true;
    }

    private static void updateProgressBossBar(MinecraftServer server) {
        if (!state.isRunning()) {
            removeProgressBossBar(server);
            return;
        }

        if (!bossBarCreated) {
            recreateProgressBossBar(server);
        }

        int total = Math.max(1, getTotalTargetCount());
        int collected = Math.max(0, getCollectedCount());

        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " name " + toTextJson(buildBossBarTitleText()));
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " max " + total);
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " value " + Math.min(collected, total));
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " players @a");
    }

    private static void removeProgressBossBar(MinecraftServer server) {
        runServerCommand(server, "bossbar remove " + PROGRESS_BOSSBAR_ID);
        bossBarCreated = false;
    }

    private static String buildBossBarTitleText() {
        return String.format(
                Locale.ROOT,
                "All Blocks: %d/%d (%.2f%%) | Day %d | %s",
                getCollectedCount(),
                getTotalTargetCount(),
                getProgressPercent(),
                getCurrentDay(),
                getFormattedElapsedTime()
        );
    }

    private static String toTextJson(String text) {
        return "{\"text\":\"" + escapeJson(text) + "\"}";
    }

    private static String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static void runServerCommand(MinecraftServer server, String command) {
        try {
            CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
            server.getCommands().performPrefixedCommand(source, command);
        } catch (Exception e) {
            AllBlocksMod.LOGGER.warn("Failed to run server command: {}", command, e);
        }
    }

    private static void broadcast(MinecraftServer server, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }
}