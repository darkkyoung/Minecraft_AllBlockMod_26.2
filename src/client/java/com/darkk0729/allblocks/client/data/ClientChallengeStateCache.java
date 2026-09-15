package com.darkk0729.allblocks.client.data;

import com.darkk0729.allblocks.network.AllBlocksSyncPayload;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import com.darkk0729.allblocks.challenge.ChallengeDifficulty;
import com.darkk0729.allblocks.network.ChallengeStatusPayload;

public final class ClientChallengeStateCache {
    private static boolean synced = false;

    private static boolean running = false;
    private static boolean finished = false;
    private static String result = "NONE";
    private static String mode = "SOLO";
    private static ChallengeDifficulty difficulty = ChallengeDifficulty.HARD;
    private static long elapsedTicks = 0L;
    private static int currentDay = 1;
    private static int collectedCount = 0;
    private static int totalTargetCount = 0;

    private static final Map<String, SyncedBlockData> blocks = new HashMap<>();
    private static final Map<String, SyncedParticipantData> participants =
            new LinkedHashMap<>();

    private ClientChallengeStateCache() {
    }

    public static void apply(AllBlocksSyncPayload payload) {
        if (payload == null) {
            return;
        }

        synced = true;

        running = payload.running();
        finished = payload.finished();
        result = payload.result();
        mode = payload.mode();
        elapsedTicks = Math.max(0L, payload.elapsedTicks());
        currentDay = Math.max(1, payload.currentDay());
        collectedCount = Math.max(0, payload.collectedCount());
        totalTargetCount = Math.max(0, payload.totalTargetCount());
        participants.clear();

        if (payload.participants() != null) {
            for (AllBlocksSyncPayload.ParticipantEntry entry :
                    payload.participants()) {

                if (entry == null
                        || entry.playerUuid() == null
                        || entry.playerUuid().isBlank()) {
                    continue;
                }

                participants.put(
                        entry.playerUuid(),
                        new SyncedParticipantData(
                                entry.playerUuid(),
                                entry.playerName(),
                                entry.color(),
                                entry.teamId(),
                                Math.max(0, entry.collectedCount())
                        )
                );
            }
        }

        blocks.clear();

        if (payload.blocks() != null) {
            for (AllBlocksSyncPayload.BlockEntry entry : payload.blocks()) {
                if (entry == null || entry.blockId() == null || entry.blockId().isBlank()) {
                    continue;
                }

                blocks.put(entry.blockId(), new SyncedBlockData(
                        entry.ownerType(),
                        entry.ownerId(),
                        entry.ownerName(),
                        entry.state()
                ));
            }
        }
    }

    public static void applyStatus(ChallengeStatusPayload payload) {
        if (payload == null) return;

        synced = true;
        running = payload.running();
        finished = payload.finished();
        result = payload.result() == null ? "NONE" : payload.result();
        mode = payload.mode() == null ? "SOLO" : payload.mode();
        difficulty = parseDifficulty(payload.difficulty());
        elapsedTicks = Math.max(0L, payload.elapsedTicks());
        currentDay = Math.max(1, payload.currentDay());
        collectedCount = Math.max(0, payload.collectedCount());
        totalTargetCount = Math.max(0, payload.totalTargetCount());
    }

    public static void clear() {
        synced = false;
        running = false;
        finished = false;
        result = "NONE";
        mode = "SOLO";
        difficulty = ChallengeDifficulty.HARD;
        elapsedTicks = 0L;
        currentDay = 1;
        collectedCount = 0;
        totalTargetCount = 0;
        blocks.clear();
        participants.clear();
    }

    public static boolean isSynced() {
        return synced;
    }

    public static boolean isRunning() {
        return running;
    }

    public static boolean isFinished() {
        return finished;
    }

    public static boolean shouldShowHud() {
        return running || finished;
    }

    public static String getResult() {
        return result;
    }

    public static String getMode() {
        return mode;
    }

    public static ChallengeDifficulty getDifficulty() { return difficulty; }

    public static long getElapsedTicks() {
        return elapsedTicks;
    }

    public static int getCurrentDay() {
        return currentDay;
    }

    public static int getCollectedCount() {
        return collectedCount;
    }

    public static int getTotalTargetCount() {
        return totalTargetCount;
    }

    public static double getProgressPercent() {
        if (totalTargetCount <= 0) {
            return 0.0D;
        }

        return collectedCount * 100.0D / totalTargetCount;
    }

    private static ChallengeDifficulty parseDifficulty(String name) {
        if (name == null || name.isBlank()) return ChallengeDifficulty.HARD;

        try {
            return ChallengeDifficulty.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return ChallengeDifficulty.HARD;
        }
    }

    public static String getFormattedElapsedTime() {
        long totalSeconds = elapsedTicks / 20L;

        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static SyncedBlockData getBlockData(String blockId) {
        return blocks.get(blockId);
    }

    public static Map<String, SyncedBlockData> getBlocks() {
        return Collections.unmodifiableMap(blocks);
    }

    public static SyncedParticipantData getParticipant(
            String playerUuid
    ) {
        if (playerUuid == null) {
            return null;
        }

        return participants.get(playerUuid);
    }

    public static Map<String, SyncedParticipantData> getParticipants() {
        return Collections.unmodifiableMap(participants);
    }

    public static boolean isTeamRace() {
        return "TEAM_RACE".equals(mode);
    }

    public static int getTeamScore(String teamId) {
        if (teamId == null || teamId.isBlank()) return 0;

        for (SyncedParticipantData participant : participants.values()) {
            if (participant == null) continue;
            if (!teamId.equals(participant.teamId())) continue;
            return Math.max(0, participant.collectedCount());
        }

        return 0;
    }

    public static String getPlayerTeam(String playerUuid) {
        SyncedParticipantData participant = getParticipant(playerUuid);

        if (participant == null
                || participant.teamId() == null
                || participant.teamId().isBlank()) {
            return "NONE";
        }

        return participant.teamId();
    }

    public record SyncedParticipantData(
            String playerUuid,
            String playerName,
            String color,
            String teamId,
            int collectedCount
    ) {
    }

    public record SyncedBlockData(
            String ownerType,
            String ownerId,
            String ownerName,
            String state
    ) {
        public boolean isClaimed() {
            return "CLAIMED".equals(state);
        }

        public boolean isReleased() {
            return "RELEASED".equals(state);
        }
    }
}