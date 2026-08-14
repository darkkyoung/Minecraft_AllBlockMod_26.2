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
import com.darkk0729.allblocks.event.DayRaidManager;
import com.darkk0729.allblocks.event.FinalDayManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import com.darkk0729.allblocks.network.CodexToastPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.darkk0729.allblocks.network.AllBlocksSyncPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public static void handlePlayerJoin(
            MinecraftServer server,
            ServerPlayer player
    ) {
        if (server == null || player == null) {
            return;
        }

        if (state.isRunning()) {
            state.registerParticipant(
                    player.getUUID(),
                    player.getName().getString()
            );

            save(server);

            // 기존 플레이어 화면에도 새 얼굴이 생겨야 하므로 전체 동기화
            syncToAllPlayers(server);

            return;
        }

        if (state.isFinished()) {
            syncToPlayer(player);
        }
    }

    public static void changeOwnPlayerColor(
            MinecraftServer server,
            ServerPlayer player,
            String requestedColor
    ) {
        if (server == null || player == null) {
            return;
        }

        PlayerCodexColor color =
                PlayerCodexColor.fromName(requestedColor);

        state.registerParticipant(
                player.getUUID(),
                player.getName().getString()
        );

        boolean changed =
                state.setParticipantColor(
                        player.getUUID(),
                        color
                );

        if (!changed) {
            return;
        }

        save(server);

        // 다른 플레이어에게도 즉시 새 색 전달
        syncToAllPlayers(server);
    }

    public static boolean isRunning() {
        return state.isRunning();
    }

    public static boolean isFinished() {
        return state.isFinished();
    }

    public static ChallengeState.ChallengeResult getResult() {
        return state.getResult();
    }

    public static boolean shouldShowHud() {
        return state.isRunning() || state.isFinished();
    }

    public static ChallengeMode getMode() {
        return state.getMode();
    }

    public static ChallengeDifficulty getDifficulty() {
        return state.getDifficulty();
    }

    public static long getElapsedTicks() {
        return state.getElapsedTicks();
    }

    public static long getWorldElapsedTicks() {
        return state.getWorldElapsedTicks();
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

    public static ChallengeState.CollectedBlockData getBlockCollectionData(String blockId) {
        if (blockId == null) {
            return null;
        }

        return state.getCollectedBlocks().get(blockId);
    }

    public static Map<String, ChallengeState.ParticipantData> getParticipants() {
        return state.getParticipants();
    }

    private static void registerOnlinePlayers(
            MinecraftServer server
    ) {
        if (server == null) {
            return;
        }

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {

            state.registerParticipant(
                    player.getUUID(),
                    player.getName().getString()
            );
        }
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

    public static void refreshProgressBossBar(MinecraftServer server) {
        if (server == null || !shouldShowHud()) {
            return;
        }

        updateProgressBossBar(server);
    }

    public static void setLastProgressEventTier(int tier) {
        state.setLastProgressEventTier(tier);
    }

    public static void load(MinecraftServer server) {
        state = AllBlocksSaveManager.load(server);

        if (state.isRunning()) {
            state.resetWorldClockTracker(getCurrentWorldTime(server));
        }

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

    public static void syncToAllPlayers(
            MinecraftServer server
    ) {
        if (server == null) {
            return;
        }

        AllBlocksSyncPayload payload =
                createSyncPayload();

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {

            ServerPlayNetworking.send(
                    player,
                    payload
            );
        }
    }

    public static void syncToPlayer(
            ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        ServerPlayNetworking.send(
                player,
                createSyncPayload()
        );
    }

    private static AllBlocksSyncPayload createSyncPayload() {

        List<AllBlocksSyncPayload.ParticipantEntry>
                participantEntries =
                new ArrayList<>();

        for (ChallengeState.ParticipantData participant :
                state.getParticipants().values()) {

            if (participant == null) {
                continue;
            }

            participantEntries.add(
                    new AllBlocksSyncPayload.ParticipantEntry(
                            participant.playerUuid == null
                                    ? ""
                                    : participant.playerUuid,

                            participant.playerName == null
                                    ? ""
                                    : participant.playerName,

                            participant.color == null
                                    ? PlayerCodexColor.BLUE.name()
                                    : participant.color,

                            state.getOwnedBlockCount(
                                    participant.playerUuid
                            )
                    )
            );
        }


        List<AllBlocksSyncPayload.BlockEntry>
                blockEntries =
                new ArrayList<>();

        for (Map.Entry<
                String,
                ChallengeState.CollectedBlockData
                > entry :
                state.getCollectedBlocks().entrySet()) {

            String blockId =
                    entry.getKey();

            ChallengeState.CollectedBlockData data =
                    entry.getValue();

            if (blockId == null || data == null) {
                continue;
            }

            blockEntries.add(
                    new AllBlocksSyncPayload.BlockEntry(
                            blockId,

                            data.ownerUuid == null
                                    ? ""
                                    : data.ownerUuid,

                            data.ownerName == null
                                    ? ""
                                    : data.ownerName,

                            data.state == null
                                    ? "UNCLAIMED"
                                    : data.state.name()
                    )
            );
        }

        return new AllBlocksSyncPayload(
                state.isRunning(),
                state.isFinished(),
                state.getResult().name(),
                state.getMode().name(),
                state.getElapsedTicks(),
                state.getCurrentDay(),
                state.getCollectedCount(),
                getTotalTargetCount(),
                participantEntries,
                blockEntries
        );
    }


    public static void save(MinecraftServer server) {
        AllBlocksSaveManager.save(server, state);
    }

    public static void startSolo(MinecraftServer server) {
        startSingle(server, ChallengeDifficulty.HARD);
    }

    public static void startSingle(MinecraftServer server, ChallengeDifficulty difficulty) {
        runServerCommand(server, "time of minecraft:overworld set 0");

        ChallengeDifficulty safeDifficulty = difficulty == null
                ? ChallengeDifficulty.HARD
                : difficulty;

        state.start(ChallengeMode.SOLO, safeDifficulty, getCurrentWorldTime(server));
        registerOnlinePlayers(server);
        ticksSinceLastSave = 0L;
        ticksSinceLastBossBarUpdate = 0L;
        FinalDayManager.reset();

        save(server);
        recreateProgressBossBar(server);
        updateProgressBossBar(server);
        syncToAllPlayers(server);
    }

    public static void stop(MinecraftServer server) {
        state.stop();
        ticksSinceLastSave = 0L;
        ticksSinceLastBossBarUpdate = 0L;
        FinalDayManager.reset();

        save(server);
        removeProgressBossBar(server);
        // syncToAllPlayers(server);
    }

    public static void tick(MinecraftServer server) {
        if (!state.isRunning()) {
            return;
        }

        ChallengeRules rules = state.getRules();

        boolean shouldEnd = state.tick(getCurrentWorldTime(server));

        BlockCollectionTracker.tick(server);

        if (rules.progressEventsEnabled()) {
            ChallengeEventManager.tick(server);
        }

        if (rules.finalDayLimitEnabled()) {
            FinalDayManager.tick(server);
        }

        if (shouldEnd && rules.finalDayLimitEnabled()) {
            finishChallenge(server, getCollectedCount() >= getTotalTargetCount()
                    ? ChallengeState.ChallengeResult.CLEAR
                    : ChallengeState.ChallengeResult.FAIL);
            return;
        }

        ticksSinceLastBossBarUpdate++;

        if (ticksSinceLastBossBarUpdate >= BOSS_BAR_UPDATE_INTERVAL_TICKS) {
            ticksSinceLastBossBarUpdate = 0L;

            if (!DayRaidManager.isRaidWarningActive() && !FinalDayManager.isFinalDayActive()) {
                updateProgressBossBar(server);
            }
        }

        ticksSinceLastSave++;
        if (ticksSinceLastSave >= AUTO_SAVE_INTERVAL_TICKS) {
            ticksSinceLastSave = 0L;
            save(server);
        }
    }

    public static boolean collectBlock(MinecraftServer server, ServerPlayer player, String blockId) {
        state.registerParticipant(
                player.getUUID(),
                player.getName().getString()
        );

        boolean collected = state.collectBlock(
                blockId,
                player.getUUID(),
                player.getName().getString()
        );

        if (collected) {
            sendCodexToast(player, blockId);

            if (getCollectedCount() >= getTotalTargetCount()) {
                finishChallenge(server, ChallengeState.ChallengeResult.CLEAR);
            } else {
                save(server);
                updateProgressBossBar(server);
                syncToAllPlayers(server);
            }
        }

        return collected;
    }

    private static void sendCodexToast(ServerPlayer player, String blockId) {
        if (player == null || blockId == null || blockId.isBlank()) {
            return;
        }

        try {
            ServerPlayNetworking.send(player, new CodexToastPayload(blockId));
        } catch (Exception e) {
            AllBlocksMod.LOGGER.warn("Failed to send codex toast payload for block: {}", blockId, e);
        }
    }

    public static void debugCollectBlocks(MinecraftServer server, ServerPlayer player, int count) {
        if (server == null || player == null) {
            return;
        }

        if (!state.isRunning()) {
            player.sendSystemMessage(Component.literal("[Block Race Debug] 챌린지가 시작되지 않았습니다."));
            return;
        }

        int safeCount = Math.max(1, count);
        int collectedNow = 0;

        for (Block block : TargetBlockRegistry.getTargetBlocks()) {
            String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

            boolean collected = state.collectBlock(
                    blockId,
                    player.getUUID(),
                    player.getName().getString()
            );

            if (!collected) {
                continue;
            }

            collectedNow++;

            if (collectedNow >= safeCount) {
                break;
            }
        }

        if (getCollectedCount() >= getTotalTargetCount()) {
            finishChallenge(server, ChallengeState.ChallengeResult.CLEAR);
        } else {
            save(server);
            updateProgressBossBar(server);
        }

        player.sendSystemMessage(Component.literal(
                "[Block Race Debug] 도감 블록 " + collectedNow + "개를 획득 처리했습니다. 현재 "
                        + getCollectedCount() + "/" + getTotalTargetCount()
                        + " (" + String.format(Locale.ROOT, "%.1f", getProgressPercent()) + "%)"
        ));
    }

    private static void finishChallenge(MinecraftServer server, ChallengeState.ChallengeResult result) {
        if (server == null) {
            return;
        }

        if (!state.isRunning()) {
            return;
        }

        state.finish(result);
        ticksSinceLastSave = 0L;
        ticksSinceLastBossBarUpdate = 0L;
        FinalDayManager.reset();

        save(server);
        updateProgressBossBar(server);
        // syncToAllPlayers(server);

        FinalDayManager.showResult(server, result);
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
        syncToAllPlayers(server);

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

    public static void debugSetDay(MinecraftServer server, int day) {
        if (server == null) {
            return;
        }

        if (!state.isRunning()) {
            broadcast(server, Component.literal("[AllBlocks] Challenge is not running."));
            return;
        }

        int safeDay = Math.max(1, Math.min(101, day));

        /*
         * safeDay는 월드 절대 날짜가 아니라 "챌린지 기준 날짜"다.
         *
         * 1일차   -> 0틱
         * 2일차   -> 24,000틱
         * 30일차  -> 696,000틱
         * 100일차 -> 2,376,000틱
         */
        long targetWorldElapsedTicks = (safeDay - 1L) * ChallengeState.TICKS_PER_DAY;

        /*
         * 중요:
         * 여기에 state.getStartWorldTime()을 더하면 안 된다.
         *
         * 그 값을 더하면 기존 월드 절대 시간이 섞여서
         * /allblocks debug day 30을 했는데 58일차, 59일차처럼 튀는 문제가 생긴다.
         */
        long targetWorldTime = targetWorldElapsedTicks;

        runServerCommand(server, "time of minecraft:overworld set " + targetWorldTime);

        /*
         * 챌린지 내부 Day는 월드 시간 계산 결과에 맡기지 않고 직접 고정한다.
         * 그리고 다음 tick에서 delta가 중복으로 더해지지 않도록
         * lastWorldClockTime도 우리가 방금 설정한 targetWorldTime으로 맞춘다.
         */
        state.setWorldElapsedTicks(targetWorldElapsedTicks);
        state.resetWorldClockTracker(targetWorldTime);

        if (!state.getRules().finalDayLimitEnabled() || state.getCurrentDay() != 100) {
            FinalDayManager.reset();
        }

        save(server);
        updateProgressBossBar(server);

        broadcast(server, Component.literal(
                "[AllBlocks] Debug day set to Day " + state.getCurrentDay()
                        + " | Timer " + state.getFormattedElapsedTime()
        ));
    }

    private static void recreateProgressBossBar(MinecraftServer server) {
        runServerCommand(server, "bossbar remove " + PROGRESS_BOSSBAR_ID);

        String titleJson = buildBossBarTitleJson();

        runServerCommand(server, "bossbar add " + PROGRESS_BOSSBAR_ID + " " + titleJson);
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " color green");
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " style progress");
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " visible true");

        bossBarCreated = true;
    }

    private static void updateProgressBossBar(MinecraftServer server) {
        if (!shouldShowHud()) {
            removeProgressBossBar(server);
            return;
        }

        if (FinalDayManager.isFinalDayActive()) {
            return;
        }

        if (!bossBarCreated) {
            recreateProgressBossBar(server);
        }

        int total = Math.max(1, getTotalTargetCount());
        int collected = Math.max(0, getCollectedCount());

        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " color green");
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " style progress");
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " visible true");

        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " name " + buildBossBarTitleJson());
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " max " + total);
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " value " + Math.min(collected, total));
        runServerCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " players @a");
    }

    private static void removeProgressBossBar(MinecraftServer server) {
        runServerCommand(server, "bossbar remove " + PROGRESS_BOSSBAR_ID);
        bossBarCreated = false;
    }

    private static long getCurrentWorldTime(MinecraftServer server) {
        if (server == null || server.overworld() == null) {
            return 0L;
        }

        return Math.max(0L, server.overworld().getOverworldClockTime());
    }

    private static String buildBossBarTitleJson() {
        String mainText = String.format(
                Locale.ROOT,
                "도감 진행률 | %d / %d ",
                getCollectedCount(),
                getTotalTargetCount()
        );

        String percentText = String.format(
                Locale.ROOT,
                "(%.2f%%)",
                getProgressPercent()
        );

        return "{"
                + "\"text\":\"" + escapeJson(mainText) + "\","
                + "\"color\":\"white\","
                + "\"extra\":[{"
                + "\"text\":\"" + escapeJson(percentText) + "\","
                + "\"color\":\"gold\""
                + "}]"
                + "}";
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