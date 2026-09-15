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
import com.darkk0729.allblocks.network.ChallengeStatusPayload;
import java.util.UUID;

public final class ChallengeManager {
    private static final long AUTO_SAVE_INTERVAL_TICKS = 20L * 30L;
    private static final long BOSS_BAR_UPDATE_INTERVAL_TICKS = 20L;
    private static final long STATUS_SYNC_INTERVAL_TICKS = 20L;
    private static final String PROGRESS_BOSSBAR_ID = "allblocks:progress";

    private static ChallengeState state = new ChallengeState();
    private static long ticksSinceLastSave = 0L;
    private static long ticksSinceLastBossBarUpdate = 0L;
    private static long ticksSinceLastStatusSync = 0L;
    private static boolean bossBarCreated = false;

    private ChallengeManager() {
    }

    public static void handlePlayerJoin(MinecraftServer server, ServerPlayer player) {
        if (server == null || player == null) return;

        if (state.isRunning()) {
            if (state.getMode() == ChallengeMode.TEAM_RACE) {
                TeamRaceTeam team =
                        state.getParticipantTeam(player.getUUID().toString());

                if (!team.isAssigned()) {
                    player.sendSystemMessage(Component.literal(
                            "[올블록 챌린지] 이미 시작된 팀 레이스에는 중도 참가할 수 없습니다."
                    ));

                    syncToPlayer(player);
                    syncStatusToPlayer(player);
                    return;
                }

                state.registerParticipant(
                        player.getUUID(),
                        player.getName().getString()
                );

                applyTeamScoreboardMembership(
                        server,
                        player,
                        team
                );

                save(server);
                syncToAllPlayers(server);
                syncStatusToPlayer(player);
                return;
            }

            state.registerParticipant(
                    player.getUUID(),
                    player.getName().getString()
            );

            save(server);
            syncToAllPlayers(server);
            syncStatusToPlayer(player);
            return;
        }

        syncToPlayer(player);
        syncStatusToPlayer(player);
    }

    public static void changeOwnPlayerColor(
            MinecraftServer server,
            ServerPlayer player,
            String requestedColor
    ) {
        if (server == null || player == null) {
            return;
        }

        if (state.isRunning()
                && state.getMode() == ChallengeMode.TEAM_RACE
                && !state.getParticipantTeam(player.getUUID().toString()).isAssigned()) {
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

    public static int getDisplayedDay() {
        return state.getCurrentDay() + 1;
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

    public static TeamRaceTeam getParticipantTeam(String playerUuid) {
        return state.getParticipantTeam(playerUuid);
    }

    public static boolean setParticipantTeam(
            MinecraftServer server,
            ServerPlayer player,
            TeamRaceTeam team
    ) {
        if (server == null || player == null || team == null) {
            return false;
        }

        state.registerParticipant(
                player.getUUID(),
                player.getName().getString()
        );

        boolean changed =
                state.setParticipantTeam(
                        player.getUUID(),
                        team
                );

        if (!changed) {
            return false;
        }

        save(server);
        syncToAllPlayers(server);
        return true;
    }

    public static int getTeamBlockCount(TeamRaceTeam team) {
        if (team == null || !team.isAssigned()) {
            return 0;
        }

        return state.getOwnedBlockCount(
                CollectionOwnerType.TEAM,
                team.getOwnerId()
        );
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

    private static CollectionOwner resolveCollectionOwner(ServerPlayer player) {
        if (player == null) return null;

        if (state.getMode() == ChallengeMode.CO_OP) {
            return CollectionOwner.shared();
        }

        if (state.getMode() == ChallengeMode.TEAM_RACE) {
            TeamRaceTeam team =
                    state.getParticipantTeam(
                            player.getUUID().toString()
                    );

            if (!team.isAssigned()) {
                return null;
            }

            return CollectionOwner.team(team);
        }

        return CollectionOwner.player(
                player.getUUID(),
                player.getName().getString()
        );
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
        ticksSinceLastStatusSync = 0L;
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

    public static void syncStatusToAllPlayers(MinecraftServer server) {
        if (server == null) return;

        ChallengeStatusPayload payload = createStatusPayload();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void syncStatusToPlayer(ServerPlayer player) {
        if (player == null) return;
        ServerPlayNetworking.send(player, createStatusPayload());
    }

    private static ChallengeStatusPayload createStatusPayload() {
        return new ChallengeStatusPayload(
                state.isRunning(),
                state.isFinished(),
                state.getResult().name(),
                state.getMode().name(),
                state.getDifficulty().name(),
                state.getElapsedTicks(),
                getDisplayedDay(),
                state.getCollectedCount(),
                getTotalTargetCount()
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

                            participant.teamId == null
                                    ? TeamRaceTeam.NONE.name()
                                    : participant.teamId,

                            getParticipantVisibleCollectedCount(
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
                            data.ownerType == null
                                    ? CollectionOwnerType.NONE.name()
                                    : data.ownerType.name(),
                            data.ownerId == null
                                    ? ""
                                    : data.ownerId,
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
                getDisplayedDay(),
                state.getCollectedCount(),
                getTotalTargetCount(),
                participantEntries,
                blockEntries
        );
    }

    private static int getParticipantVisibleCollectedCount(String playerUuid) {
        if (state.getMode() == ChallengeMode.CO_OP) {
            return state.getOwnedBlockCount(
                    CollectionOwnerType.SHARED,
                    CollectionOwner.CO_OP_OWNER_ID
            );
        }

        if (state.getMode() == ChallengeMode.TEAM_RACE) {
            TeamRaceTeam team =
                    state.getParticipantTeam(playerUuid);

            return getTeamBlockCount(team);
        }

        return state.getOwnedBlockCount(
                CollectionOwnerType.PLAYER,
                playerUuid
        );
    }

    public static void save(MinecraftServer server) {
        AllBlocksSaveManager.save(server, state);
    }

    public static void startSolo(MinecraftServer server) {
        startSingle(server, ChallengeDifficulty.HARD);
    }

    public static void startSingle(
            MinecraftServer server,
            ChallengeDifficulty difficulty
    ) {
        TeamRaceSetupManager.reset(server);
        startMode(server, ChallengeMode.SOLO, difficulty);
    }

    public static void startCoop(
            MinecraftServer server,
            ChallengeDifficulty difficulty
    ) {
        TeamRaceSetupManager.reset(server);
        startMode(server, ChallengeMode.CO_OP, difficulty);
    }

    public static void startTeamRace(
            MinecraftServer server,
            ChallengeDifficulty difficulty,
            Map<UUID, TeamRaceTeam> assignments
    ) {
        if (server == null
                || assignments == null
                || assignments.isEmpty()) {
            return;
        }

        runServerCommand(
                server,
                "time of minecraft:overworld rate 1"
        );

        runServerCommand(
                server,
                "time of minecraft:overworld resume"
        );

        runServerCommand(
                server,
                "time of minecraft:overworld set 0"
        );

        ChallengeDifficulty safeDifficulty =
                difficulty == null
                        ? ChallengeDifficulty.HARD
                        : difficulty;

        state.start(
                ChallengeMode.TEAM_RACE,
                safeDifficulty,
                getCurrentWorldTime(server)
        );

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {

            TeamRaceTeam team =
                    assignments.getOrDefault(
                            player.getUUID(),
                            TeamRaceTeam.NONE
                    );

            if (!team.isAssigned()) {
                continue;
            }

            state.registerParticipant(
                    player.getUUID(),
                    player.getName().getString()
            );

            state.setParticipantTeam(
                    player.getUUID(),
                    team
            );

            applyTeamScoreboardMembership(
                    server,
                    player,
                    team
            );
        }

        ticksSinceLastSave = 0L;
        ticksSinceLastBossBarUpdate = 0L;
        ticksSinceLastStatusSync = 0L;
        FinalDayManager.reset();

        save(server);
        recreateProgressBossBar(server);
        updateProgressBossBar(server);
        syncToAllPlayers(server);
        syncStatusToAllPlayers(server);
    }

    public static boolean isTeamRaceSetupActive() {
        return TeamRaceSetupManager.isActive();
    }

    private static void startMode(
            MinecraftServer server,
            ChallengeMode mode,
            ChallengeDifficulty difficulty
    ) {
        if (server == null) return;

        runServerCommand(server, "time of minecraft:overworld rate 1");
        runServerCommand(server, "time of minecraft:overworld resume");
        runServerCommand(server, "time of minecraft:overworld set 0");

        ChallengeDifficulty safeDifficulty = difficulty == null
                ? ChallengeDifficulty.HARD
                : difficulty;

        state.start(
                mode == null ? ChallengeMode.SOLO : mode,
                safeDifficulty,
                getCurrentWorldTime(server)
        );

        registerOnlinePlayers(server);

        ticksSinceLastSave = 0L;
        ticksSinceLastBossBarUpdate = 0L;
        ticksSinceLastStatusSync = 0L;
        FinalDayManager.reset();

        save(server);
        recreateProgressBossBar(server);
        updateProgressBossBar(server);
        syncToAllPlayers(server);
        syncStatusToAllPlayers(server);
    }

    public static void stop(MinecraftServer server) {
        TeamRaceSetupManager.reset(server);
        state.stop();
        ticksSinceLastSave = 0L;
        ticksSinceLastBossBarUpdate = 0L;
        ticksSinceLastStatusSync = 0L;
        FinalDayManager.reset();

        save(server);
        removeProgressBossBar(server);
        syncStatusToAllPlayers(server);
    }

    public static void tick(MinecraftServer server) {
        TeamRaceSetupManager.tick(server);

        if (!state.isRunning()) {
            return;
        }

        ChallengeRules rules = state.getRules();

        boolean shouldEnd = state.tick(getCurrentWorldTime(server));

        BlockCollectionTracker.tick(server);

        if (rules.progressEventsEnabled()) {
            ChallengeEventManager.tick(server);
        }

        if (rules.dayRaidEventsEnabled()) {
            DayRaidManager.tick(server);
        }

        if (rules.finalDayLimitEnabled()) {
            FinalDayManager.tick(server);
        }

        if (shouldEnd && rules.finalDayLimitEnabled()) {
            if (state.getMode() == ChallengeMode.TEAM_RACE) {
                finishTeamRaceByScore(server);
            } else {
                finishChallenge(
                        server,
                        getCollectedCount() >= getTotalTargetCount()
                                ? ChallengeState.ChallengeResult.CLEAR
                                : ChallengeState.ChallengeResult.FAIL
                );
            }

            return;
        }

        ticksSinceLastStatusSync++;

        if (ticksSinceLastStatusSync >= STATUS_SYNC_INTERVAL_TICKS) {
            ticksSinceLastStatusSync = 0L;
            syncStatusToAllPlayers(server);
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

    public static boolean collectBlock(
            MinecraftServer server,
            ServerPlayer player,
            String blockId
    ) {
        if (server == null
                || player == null
                || blockId == null
                || blockId.isBlank()
                || !state.isRunning()) {
            return false;
        }

        if (state.getMode() == ChallengeMode.TEAM_RACE
                && !state.getParticipantTeam(
                player.getUUID().toString()
        ).isAssigned()) {
            return false;
        }

        state.registerParticipant(
                player.getUUID(),
                player.getName().getString()
        );

        CollectionOwner owner = resolveCollectionOwner(player);

        boolean collected = state.collectBlock(blockId, owner);

        if (collected) {
            sendCodexToast(player, blockId);

            if (getCollectedCount() >= getTotalTargetCount()) {
                if (state.getMode() == ChallengeMode.TEAM_RACE) {
                    finishTeamRaceByScore(server);
                } else {
                    finishChallenge(
                            server,
                            ChallengeState.ChallengeResult.CLEAR
                    );
                }
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

        CollectionOwner owner = resolveCollectionOwner(player);

        for (Block block : TargetBlockRegistry.getTargetBlocks()) {
            String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

            boolean collected = state.collectBlock(blockId, owner);

            if (!collected) {
                continue;
            }

            collectedNow++;

            if (collectedNow >= safeCount) {
                break;
            }
        }

        if (getCollectedCount() >= getTotalTargetCount()) {
            if (state.getMode() == ChallengeMode.TEAM_RACE) {
                finishTeamRaceByScore(server);
            } else {
                finishChallenge(
                        server,
                        ChallengeState.ChallengeResult.CLEAR
                );
            }
        } else {
            save(server);
            updateProgressBossBar(server);
            syncToAllPlayers(server);
        }

        player.sendSystemMessage(Component.literal(
                "[Block Race Debug] 도감 블록 " + collectedNow + "개를 획득 처리했습니다. 현재 "
                        + getCollectedCount() + "/" + getTotalTargetCount()
                        + " (" + String.format(Locale.ROOT, "%.1f", getProgressPercent()) + "%)"
        ));
    }

    private static void finishTeamRaceByScore(
            MinecraftServer server
    ) {
        int blueScore =
                getTeamBlockCount(TeamRaceTeam.BLUE);

        int redScore =
                getTeamBlockCount(TeamRaceTeam.RED);

        ChallengeState.ChallengeResult result;

        if (blueScore > redScore) {
            result = ChallengeState.ChallengeResult.BLUE_WIN;
        } else if (redScore > blueScore) {
            result = ChallengeState.ChallengeResult.RED_WIN;
        } else {
            result = ChallengeState.ChallengeResult.DRAW;
        }

        finishChallenge(server, result);
    }

    private static void finishChallenge(
            MinecraftServer server,
            ChallengeState.ChallengeResult result
    ) {
        if (server == null || !state.isRunning()) {
            return;
        }

        ChallengeMode finishedMode =
                state.getMode();

        state.finish(result);

        ticksSinceLastSave = 0L;
        ticksSinceLastBossBarUpdate = 0L;
        ticksSinceLastStatusSync = 0L;

        FinalDayManager.reset();

        save(server);
        updateProgressBossBar(server);
        syncToAllPlayers(server);
        syncStatusToAllPlayers(server);

        if (finishedMode == ChallengeMode.TEAM_RACE) {
            showTeamRaceResult(server, result);
        } else {
            FinalDayManager.showResult(server, result);
        }
    }


    private static void showTeamRaceResult(
            MinecraftServer server,
            ChallengeState.ChallengeResult result
    ) {
        int blueScore =
                getTeamBlockCount(TeamRaceTeam.BLUE);

        int redScore =
                getTeamBlockCount(TeamRaceTeam.RED);

        String title;
        String color;
        String resultText;

        if (result == ChallengeState.ChallengeResult.BLUE_WIN) {
            title = "블루팀 승리!";
            color = "blue";
            resultText = "블루팀 승리";
        } else if (result == ChallengeState.ChallengeResult.RED_WIN) {
            title = "레드팀 승리!";
            color = "red";
            resultText = "레드팀 승리";
        } else {
            title = "무승부";
            color = "gold";
            resultText = "무승부";
        }

        runServerCommand(
                server,
                "title @a times 10 80 20"
        );

        runServerCommand(
                server,
                "title @a title "
                        + jsonText(title, color)
        );

        runServerCommand(
                server,
                "title @a subtitle "
                        + jsonText(
                        "블루팀 "
                                + blueScore
                                + " | 레드팀 "
                                + redScore,
                        "white"
                )
        );

        runServerCommand(
                server,
                "playsound minecraft:entity.player.levelup master @a ~ ~ ~ 1 1"
        );

        broadcast(
                server,
                Component.literal(
                        "[올블록 챌린지] 팀 레이스 종료 | "
                                + resultText
                                + " | 블루팀 "
                                + blueScore
                                + " : "
                                + redScore
                                + " 레드팀"
                )
        );
    }


    public static void handlePlayerDeath(
            MinecraftServer server,
            ServerPlayer player,
            boolean pvpDeath
    ) {
        if (server == null || player == null || !state.isRunning()) {
            return;
        }

        int minPercent = pvpDeath ? 5 : 0;
        int maxPercent = pvpDeath ? 20 : 10;

        CollectionOwner penaltyOwner = resolveCollectionOwner(player);

        if (penaltyOwner == null || !penaltyOwner.isValid()) {
            return;
        }

        int releasedCount = state.releaseRandomOwnedBlocks(
                penaltyOwner.type(),
                penaltyOwner.id(),
                minPercent,
                maxPercent
        );

        save(server);
        updateProgressBossBar(server);
        syncToAllPlayers(server);
        syncStatusToAllPlayers(server);

        if (state.getMode() == ChallengeMode.CO_OP) {
            String message = releasedCount > 0
                    ? "[AllBlocks] " + player.getName().getString()
                    + " 사망: 공용 도감에서 블록 "
                    + releasedCount + "개를 잃었습니다."
                    : "[AllBlocks] " + player.getName().getString()
                    + " 사망: 공용 도감에서 잃은 블록은 없습니다.";

            broadcast(server, Component.literal(message));
            return;
        }

        if (state.getMode() == ChallengeMode.TEAM_RACE) {
            TeamRaceTeam team =
                    state.getParticipantTeam(
                            player.getUUID().toString()
                    );

            String message = releasedCount > 0
                    ? "[AllBlocks] "
                    + player.getName().getString()
                    + " 사망: "
                    + team.getDisplayName()
                    + " 도감에서 블록 "
                    + releasedCount
                    + "개를 잃었습니다."
                    : "[AllBlocks] "
                    + player.getName().getString()
                    + " 사망: "
                    + team.getDisplayName()
                    + " 도감에서 잃은 블록은 없습니다.";

            broadcast(server, Component.literal(message));
            return;
        }

        if (releasedCount > 0) {
            player.sendSystemMessage(Component.literal(
                    "[AllBlocks] Death penalty: You lost "
                            + releasedCount
                            + " collected block(s)."
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

        /*
         * safeDay는 챌린지 기준 날짜.
         *
         * 0일차   -> 0틱
         * 1일차   -> 24,000틱
         * 10일차  -> 240,000틱
         * 30일차  -> 720,000틱
         * 100일차 -> 2,400,000틱
         */
        int safeDay = Math.max(1, Math.min(100, day));
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

        if (!state.getRules().finalDayLimitEnabled() || getDisplayedDay() != 100) {
            FinalDayManager.reset();
        }

        save(server);
        updateProgressBossBar(server);
        syncStatusToAllPlayers(server);

        broadcast(server, Component.literal(
                "[AllBlocks] Debug day set to Day " + getDisplayedDay()
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


    private static String jsonText(
            String text,
            String color
    ) {
        return "{\"text\":\""
                + escapeJson(text)
                + "\",\"color\":\""
                + color
                + "\",\"bold\":true}";
    }



    private static String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }


    private static void applyTeamScoreboardMembership(
            MinecraftServer server,
            ServerPlayer player,
            TeamRaceTeam team
    ) {
        if (server == null
                || player == null
                || team == null
                || !team.isAssigned()) {
            return;
        }

        runServerCommand(
                server,
                "team add allblocks_blue"
        );

        runServerCommand(
                server,
                "team add allblocks_red"
        );

        runServerCommand(
                server,
                "team modify allblocks_blue color blue"
        );

        runServerCommand(
                server,
                "team modify allblocks_red color red"
        );

        String scoreboardTeam =
                team == TeamRaceTeam.BLUE
                        ? "allblocks_blue"
                        : "allblocks_red";

        runServerCommand(
                server,
                "team join "
                        + scoreboardTeam
                        + " "
                        + player.getName().getString()
        );
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