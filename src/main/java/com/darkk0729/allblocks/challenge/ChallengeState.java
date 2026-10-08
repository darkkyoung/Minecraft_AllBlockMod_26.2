package com.darkk0729.allblocks.challenge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.LinkedHashMap;
import com.google.gson.annotations.SerializedName;

public class ChallengeState {
    public static final String SPECTATOR_TEAM_ID = "SPECTATOR";
    public static final long TICKS_PER_SECOND = 20L;
    public static final long TICKS_PER_DAY = 24000L;
    public static final int MAX_DAYS = 100;

    private boolean running;
    private ChallengeMode mode;
    private ChallengeDifficulty difficulty;
    private ChallengeTimeLimitType timeLimitType;
    private long timeLimitTicks;
    private int progressEventIntervalPercent;
    private int dayRaidIntervalDays;
    private boolean finished;
    private ChallengeResult result;

    // 실제 플레이타임 타이머
    private long elapsedTicks;

    // 인게임 월드 Day 계산용
    private long startWorldTime;
    private long worldElapsedTicks;
    private long lastWorldClockTime;

    private int lastProgressEventTier;
    private int blueTeamLastProgressEventTier;
    private int redTeamLastProgressEventTier;
    private int lastDayRaidEventDay;

    private final Map<String, CollectedBlockData> collectedBlocks = new HashMap<>();
    private final Map<String, ParticipantData> participants = new LinkedHashMap<>();

    public ChallengeState() {
        this.running = false;
        this.finished = false;
        this.result = ChallengeResult.NONE;
        this.mode = ChallengeMode.SOLO;
        this.difficulty = ChallengeDifficulty.HARD;
        this.timeLimitType = ChallengeTimeLimitType.IN_GAME_TIME;
        this.timeLimitTicks = TICKS_PER_DAY * MAX_DAYS;
        this.progressEventIntervalPercent = 10;
        this.dayRaidIntervalDays = 10;
        this.elapsedTicks = 0L;
        this.startWorldTime = 0L;
        this.worldElapsedTicks = 0L;
        this.lastWorldClockTime = 0L;
        this.lastProgressEventTier = 0;
        this.blueTeamLastProgressEventTier = 0;
        this.redTeamLastProgressEventTier = 0;
        this.lastDayRaidEventDay = 0;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return finished;
    }

    public ChallengeResult getResult() {
        return result;
    }

    public ChallengeMode getMode() {
        return mode;
    }

    public ChallengeDifficulty getDifficulty() {
        return difficulty;
    }

    public ChallengeRules getRules() {
        return ChallengeRules.from(difficulty);
    }

    public ChallengeTimeLimitType getTimeLimitType() {
        return timeLimitType;
    }

    public long getTimeLimitTicks() {
        return timeLimitTicks;
    }

    public int getProgressEventIntervalPercent() {
        return progressEventIntervalPercent;
    }

    public int getDayRaidIntervalDays() {
        return dayRaidIntervalDays;
    }

    public boolean hasTimeLimit() {
        return timeLimitType != ChallengeTimeLimitType.NONE
                && timeLimitTicks > 0L;
    }

    public long getRemainingTimeLimitTicks() {
        if (!hasTimeLimit()) {
            return 0L;
        }

        long elapsed = timeLimitType == ChallengeTimeLimitType.PLAY_TIME
                ? elapsedTicks
                : worldElapsedTicks;

        return Math.max(0L, timeLimitTicks - elapsed);
    }

    public boolean isInGameFinalDay() {
        return running
                && timeLimitType == ChallengeTimeLimitType.IN_GAME_TIME
                && timeLimitTicks > 0L
                && getRemainingTimeLimitTicks() > 0L
                && getRemainingTimeLimitTicks() <= TICKS_PER_DAY;
    }

    public long getElapsedTicks() {
        return elapsedTicks;
    }

    public long getStartWorldTime() {
        return startWorldTime;
    }

    public long getWorldElapsedTicks() {
        return worldElapsedTicks;
    }

    public int getLastProgressEventTier() {
        return lastProgressEventTier;
    }

    public int getTeamLastProgressEventTier(
            TeamRaceTeam team
    ) {
        if (team == TeamRaceTeam.BLUE) {
            return blueTeamLastProgressEventTier;
        }

        if (team == TeamRaceTeam.RED) {
            return redTeamLastProgressEventTier;
        }

        return 0;
    }

    public void setTeamLastProgressEventTier(
            TeamRaceTeam team,
            int tier
    ) {
        int safeTier =
                Math.max(
                        0,
                        Math.min(
                                100,
                                tier
                        )
                );

        if (team == TeamRaceTeam.BLUE) {
            blueTeamLastProgressEventTier =
                    safeTier;
        } else if (team == TeamRaceTeam.RED) {
            redTeamLastProgressEventTier =
                    safeTier;
        }
    }

    public int getLastDayRaidEventDay() {
        return lastDayRaidEventDay;
    }

    public void setLastDayRaidEventDay(int lastDayRaidEventDay) {
        this.lastDayRaidEventDay = Math.max(0, lastDayRaidEventDay);
    }

    public void setLastProgressEventTier(int lastProgressEventTier) {
        this.lastProgressEventTier = Math.max(0, Math.min(100, lastProgressEventTier));
    }

    // Day는 실제 플레이타임이 아니라 인게임 월드 시간 기준
    public int getCurrentDay() {
        return (int) (worldElapsedTicks / TICKS_PER_DAY);
    }

    // 타이머는 실제 플레이타임 기준
    public String getFormattedElapsedTime() {
        long totalSeconds = elapsedTicks / TICKS_PER_SECOND;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void start(ChallengeMode mode) {
        start(
                mode,
                ChallengeDifficulty.HARD,
                0L,
                ChallengeTimeLimitType.IN_GAME_TIME,
                TICKS_PER_DAY * MAX_DAYS,
                10,
                10
        );
    }

    public void start(ChallengeMode mode, long startWorldTime) {
        start(
                mode,
                ChallengeDifficulty.HARD,
                startWorldTime,
                ChallengeTimeLimitType.IN_GAME_TIME,
                TICKS_PER_DAY * MAX_DAYS,
                10,
                10
        );
    }

    public void start(
            ChallengeMode mode,
            ChallengeDifficulty difficulty,
            long startWorldTime
    ) {
        start(
                mode,
                difficulty,
                startWorldTime,
                ChallengeTimeLimitType.IN_GAME_TIME,
                TICKS_PER_DAY * MAX_DAYS,
                10,
                10
        );
    }

    public void start(
            ChallengeMode mode,
            ChallengeDifficulty difficulty,
            long startWorldTime,
            ChallengeTimeLimitType timeLimitType,
            long timeLimitTicks
    ) {
        start(
                mode,
                difficulty,
                startWorldTime,
                timeLimitType,
                timeLimitTicks,
                10,
                10
        );
    }

    public void start(
            ChallengeMode mode,
            ChallengeDifficulty difficulty,
            long startWorldTime,
            ChallengeTimeLimitType timeLimitType,
            long timeLimitTicks,
            int progressEventIntervalPercent,
            int dayRaidIntervalDays
    ) {
        this.running = true;
        this.finished = false;

        this.mode = mode == null ? ChallengeMode.SOLO : mode;
        this.difficulty = difficulty == null ? ChallengeDifficulty.HARD : difficulty;
        this.timeLimitType = timeLimitType == null
                ? ChallengeTimeLimitType.IN_GAME_TIME
                : timeLimitType;

        long safeStartWorldTime = Math.max(0L, startWorldTime);
        long safeTimeLimitTicks = Math.max(0L, timeLimitTicks);

        if (this.timeLimitType == ChallengeTimeLimitType.NONE) {
            safeTimeLimitTicks = 0L;
        } else if (safeTimeLimitTicks <= 0L) {
            safeTimeLimitTicks = TICKS_PER_DAY * MAX_DAYS;
        }

        this.timeLimitTicks = safeTimeLimitTicks;
        this.progressEventIntervalPercent =
                Math.max(
                        0,
                        Math.min(
                                100,
                                progressEventIntervalPercent
                        )
                );
        this.dayRaidIntervalDays =
                Math.max(
                        0,
                        Math.min(
                                10000,
                                dayRaidIntervalDays
                        )
                );
        this.elapsedTicks = 0L;
        this.startWorldTime = safeStartWorldTime;
        this.worldElapsedTicks = 0L;
        this.lastWorldClockTime = safeStartWorldTime;

        this.result = ChallengeResult.NONE;
        this.lastProgressEventTier = 0;
        this.blueTeamLastProgressEventTier = 0;
        this.redTeamLastProgressEventTier = 0;
        this.lastDayRaidEventDay = 0;
        this.collectedBlocks.clear();
        this.participants.clear();
    }

    public void stop() {
        this.running = false;
        this.finished = false;
        this.result = ChallengeResult.NONE;
    }

    public void finish(ChallengeResult result) {
        this.running = false;
        this.finished = true;
        this.result = result == null ? ChallengeResult.FAIL : result;
    }

    public void loadFrom(
            boolean running,
            boolean finished,
            ChallengeMode mode,
            ChallengeDifficulty difficulty,
            ChallengeTimeLimitType timeLimitType,
            long timeLimitTicks,
            int progressEventIntervalPercent,
            int dayRaidIntervalDays,
            long elapsedTicks,
            long startWorldTime,
            long worldElapsedTicks,
            ChallengeResult result,
            int lastProgressEventTier,
            int blueTeamLastProgressEventTier,
            int redTeamLastProgressEventTier,
            int lastDayRaidEventDay,
            Map<String, CollectedBlockData> loadedCollectedBlocks,
            Map<String, ParticipantData> loadedParticipants
    ) {
        this.finished = finished;
        this.result = result == null ? ChallengeResult.NONE : result;

        if (this.result == ChallengeResult.NONE) {
            this.finished = false;
        }

        this.running = running && !this.finished;
        this.mode = mode == null ? ChallengeMode.SOLO : mode;
        this.difficulty = difficulty == null ? ChallengeDifficulty.HARD : difficulty;
        this.timeLimitType = timeLimitType == null
                ? ChallengeTimeLimitType.IN_GAME_TIME
                : timeLimitType;
        this.timeLimitTicks = Math.max(0L, timeLimitTicks);
        this.progressEventIntervalPercent =
                Math.max(
                        0,
                        Math.min(
                                100,
                                progressEventIntervalPercent
                        )
                );
        this.dayRaidIntervalDays =
                Math.max(
                        0,
                        Math.min(
                                10000,
                                dayRaidIntervalDays
                        )
                );

        if (this.timeLimitType == ChallengeTimeLimitType.NONE) {
            this.timeLimitTicks = 0L;
        } else if (this.timeLimitTicks <= 0L) {
            this.timeLimitTicks = TICKS_PER_DAY * MAX_DAYS;
        }

        this.elapsedTicks = Math.max(0L, elapsedTicks);
        this.startWorldTime = Math.max(0L, startWorldTime);
        this.worldElapsedTicks = Math.max(0L, worldElapsedTicks);

        this.lastProgressEventTier =
                Math.max(
                        0,
                        Math.min(
                                100,
                                lastProgressEventTier
                        )
                );
        this.blueTeamLastProgressEventTier =
                Math.max(
                        0,
                        Math.min(
                                100,
                                blueTeamLastProgressEventTier
                        )
                );
        this.redTeamLastProgressEventTier =
                Math.max(
                        0,
                        Math.min(
                                100,
                                redTeamLastProgressEventTier
                        )
                );
        this.lastDayRaidEventDay = Math.max(0, lastDayRaidEventDay);

        this.collectedBlocks.clear();

        if (loadedCollectedBlocks != null) {
            this.collectedBlocks.putAll(loadedCollectedBlocks);
        }

        for (CollectedBlockData data : this.collectedBlocks.values()) {
            if (data == null) continue;

            if (data.ownerId == null) data.ownerId = "";
            if (data.ownerName == null) data.ownerName = "";

            if (data.state == BlockCollectionState.CLAIMED) {
                if (data.ownerType == null || data.ownerType == CollectionOwnerType.NONE) {
                    data.ownerType = CollectionOwnerType.PLAYER;
                }
            } else {
                data.ownerType = CollectionOwnerType.NONE;
                data.ownerId = "";
                data.ownerName = "";
            }
        }

        this.participants.clear();

        if (loadedParticipants != null) {
            this.participants.putAll(loadedParticipants);
        }

        for (ParticipantData participant : this.participants.values()) {
            if (participant == null) continue;

            if (participant.playerUuid == null) {
                participant.playerUuid = "";
            }

            if (participant.playerName == null) {
                participant.playerName = "";
            }

            if (participant.color == null || participant.color.isBlank()) {
                participant.color = PlayerCodexColor.BLUE.name();
            }

            if (participant.teamId == null || participant.teamId.isBlank()) {
                participant.teamId = TeamRaceTeam.NONE.name();
            }

            participant.lastProgressEventTier =
                    Math.max(
                            0,
                            Math.min(
                                    100,
                                    participant.lastProgressEventTier
                            )
                    );
        }
    }

    public void setWorldElapsedTicks(long worldElapsedTicks) {
        this.worldElapsedTicks = Math.max(0L, worldElapsedTicks);
    }

    public void resetWorldClockTracker(long currentWorldTime) {
        this.lastWorldClockTime = Math.max(0L, currentWorldTime);
    }

    public void syncWorldTime(long currentWorldTime) {
        long safeCurrentWorldTime = Math.max(0L, currentWorldTime);
        long safeLastWorldClockTime = Math.max(0L, lastWorldClockTime);
        long delta = safeCurrentWorldTime - safeLastWorldClockTime;

        if (delta > 0L) {
            this.worldElapsedTicks += delta;
        }

        this.lastWorldClockTime = safeCurrentWorldTime;
    }

    public boolean tick(long currentWorldTime) {
        if (!running) {
            return false;
        }

        // 실제 플레이타임 타이머
        elapsedTicks++;

        // 인게임 Day 카운트
        syncWorldTime(currentWorldTime);

        return switch (timeLimitType) {
            case NONE -> false;
            case PLAY_TIME -> elapsedTicks >= timeLimitTicks;
            case IN_GAME_TIME -> worldElapsedTicks >= timeLimitTicks;
        };
    }

    public boolean collectBlock(String blockId, CollectionOwner owner) {
        if (blockId == null || blockId.isBlank() || owner == null || !owner.isValid()) {
            return false;
        }

        CollectedBlockData existingData = collectedBlocks.get(blockId);

        if (existingData != null && existingData.state == BlockCollectionState.CLAIMED) {
            return false;
        }

        collectedBlocks.put(blockId, new CollectedBlockData(
                owner.type(),
                owner.id(),
                owner.displayName(),
                BlockCollectionState.CLAIMED
        ));

        return true;
    }

    public boolean isCollected(String blockId) {
        return collectedBlocks.containsKey(blockId)
                && collectedBlocks.get(blockId).state == BlockCollectionState.CLAIMED;
    }

    public int getCollectedCount() {
        int count = 0;

        for (CollectedBlockData data : collectedBlocks.values()) {
            if (data.state == BlockCollectionState.CLAIMED) {
                count++;
            }
        }

        return count;
    }

    public Map<String, CollectedBlockData> getCollectedBlocks() {
        return Collections.unmodifiableMap(collectedBlocks);
    }

    public Map<String, ParticipantData> getParticipants() {
        return Collections.unmodifiableMap(participants);
    }

    public ParticipantData getParticipant(String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) {
            return null;
        }

        return participants.get(playerUuid);
    }

    public ParticipantData findParticipantByName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return null;
        }

        for (ParticipantData participant : participants.values()) {
            if (participant == null || participant.playerName == null) {
                continue;
            }

            if (participant.playerName.equalsIgnoreCase(playerName)) {
                return participant;
            }
        }

        return null;
    }

    public ParticipantData rebindParticipantIdentity(
            String oldPlayerUuid,
            UUID newPlayerUuid,
            String newPlayerName
    ) {
        if (oldPlayerUuid == null || oldPlayerUuid.isBlank() || newPlayerUuid == null) {
            return null;
        }

        ParticipantData participant = participants.get(oldPlayerUuid);

        if (participant == null) {
            return null;
        }

        String newUuid = newPlayerUuid.toString();

        if (oldPlayerUuid.equals(newUuid)) {
            if (newPlayerName != null && !newPlayerName.isBlank()) {
                participant.playerName = newPlayerName;
            }
            return participant;
        }

        LinkedHashMap<String, ParticipantData> rebound = new LinkedHashMap<>();

        for (Map.Entry<String, ParticipantData> entry : participants.entrySet()) {
            if (oldPlayerUuid.equals(entry.getKey())) {
                participant.playerUuid = newUuid;

                if (newPlayerName != null && !newPlayerName.isBlank()) {
                    participant.playerName = newPlayerName;
                }

                rebound.put(newUuid, participant);
            } else {
                rebound.put(entry.getKey(), entry.getValue());
            }
        }

        participants.clear();
        participants.putAll(rebound);

        for (CollectedBlockData data : collectedBlocks.values()) {
            if (data == null
                    || data.state != BlockCollectionState.CLAIMED
                    || data.ownerType != CollectionOwnerType.PLAYER
                    || !oldPlayerUuid.equals(data.ownerId)) {
                continue;
            }

            data.ownerId = newUuid;

            if (newPlayerName != null && !newPlayerName.isBlank()) {
                data.ownerName = newPlayerName;
            }
        }

        return participant;
    }

    public ParticipantData registerParticipant(UUID playerUuid, String playerName) {
        if (playerUuid == null) {
            return null;
        }

        String uuid = playerUuid.toString();

        ParticipantData existing = participants.get(uuid);

        if (existing != null) {
            if (playerName != null && !playerName.isBlank()) {
                existing.playerName = playerName;
            }

            return existing;
        }

        PlayerCodexColor[] colors = PlayerCodexColor.values();

        PlayerCodexColor defaultColor =
                colors[participants.size() % colors.length];

        ParticipantData created = new ParticipantData(
                uuid,
                playerName == null ? "" : playerName,
                defaultColor.name()
        );

        participants.put(uuid, created);

        return created;
    }

    public boolean setParticipantColor(
            UUID playerUuid,
            PlayerCodexColor color
    ) {
        if (playerUuid == null || color == null) {
            return false;
        }

        ParticipantData participant =
                participants.get(playerUuid.toString());

        if (participant == null) {
            return false;
        }

        participant.color = color.name();

        return true;
    }

    public int getParticipantLastProgressEventTier(
            String playerUuid
    ) {
        ParticipantData participant =
                getParticipant(playerUuid);

        if (participant == null) {
            return 0;
        }

        return Math.max(
                0,
                Math.min(
                                    100,
                                    participant.lastProgressEventTier
                )
        );
    }

    public boolean setParticipantLastProgressEventTier(
            String playerUuid,
            int tier
    ) {
        ParticipantData participant =
                getParticipant(playerUuid);

        if (participant == null) {
            return false;
        }

        participant.lastProgressEventTier =
                Math.max(
                        0,
                        Math.min(100, tier)
                );

        return true;
    }

    public boolean isParticipantSpectator(
            String playerUuid
    ) {
        ParticipantData participant =
                getParticipant(playerUuid);

        return participant != null
                && SPECTATOR_TEAM_ID.equals(
                        participant.teamId
                );
    }

    public boolean setParticipantSpectator(
            UUID playerUuid
    ) {
        if (playerUuid == null) {
            return false;
        }

        ParticipantData participant =
                participants.get(
                        playerUuid.toString()
                );

        if (participant == null) {
            return false;
        }

        participant.teamId = SPECTATOR_TEAM_ID;
        return true;
    }

    public TeamRaceTeam getParticipantTeam(String playerUuid) {
        ParticipantData participant = getParticipant(playerUuid);

        if (participant == null) {
            return TeamRaceTeam.NONE;
        }

        return TeamRaceTeam.fromName(participant.teamId);
    }

    public boolean setParticipantTeam(
            UUID playerUuid,
            TeamRaceTeam team
    ) {
        if (playerUuid == null || team == null) {
            return false;
        }

        ParticipantData participant =
                participants.get(playerUuid.toString());

        if (participant == null) {
            return false;
        }

        participant.teamId = team.name();
        return true;
    }

    public void clearParticipantTeams() {
        for (ParticipantData participant : participants.values()) {
            if (participant == null) continue;
            participant.teamId = TeamRaceTeam.NONE.name();
        }
    }

    public int getOwnedBlockCount(CollectionOwnerType ownerType, String ownerId) {
        if (ownerType == null || ownerType == CollectionOwnerType.NONE
                || ownerId == null || ownerId.isBlank()) {
            return 0;
        }

        int count = 0;

        for (CollectedBlockData data : collectedBlocks.values()) {
            if (data == null || data.state != BlockCollectionState.CLAIMED) continue;
            if (data.ownerType != ownerType) continue;
            if (!ownerId.equals(data.ownerId)) continue;
            count++;
        }

        return count;
    }

    public int releaseRandomOwnedBlocks(
            CollectionOwnerType ownerType,
            String ownerId,
            int minPercent,
            int maxPercent
    ) {
        if (ownerType == null || ownerType == CollectionOwnerType.NONE
                || ownerId == null || ownerId.isBlank()) {
            return 0;
        }

        List<String> ownedBlockIds = new ArrayList<>();

        for (Map.Entry<String, CollectedBlockData> entry : collectedBlocks.entrySet()) {
            CollectedBlockData data = entry.getValue();

            if (data == null) continue;
            if (data.state != BlockCollectionState.CLAIMED) continue;
            if (data.ownerType != ownerType) continue;
            if (!ownerId.equals(data.ownerId)) continue;

            ownedBlockIds.add(entry.getKey());
        }

        if (ownedBlockIds.isEmpty()) {
            return 0;
        }

        int safeMinPercent = Math.max(0, minPercent);
        int safeMaxPercent = Math.max(safeMinPercent, maxPercent);

        int minLossCount = safeMinPercent <= 0
                ? 0
                : Math.max(1, (int) Math.ceil(ownedBlockIds.size() * (safeMinPercent / 100.0D)));

        int maxLossCount = (int) Math.floor(ownedBlockIds.size() * (safeMaxPercent / 100.0D));

        if (safeMinPercent > 0 && maxLossCount == 0) {
            maxLossCount = 1;
        }

        maxLossCount = Math.min(maxLossCount, ownedBlockIds.size());
        minLossCount = Math.min(minLossCount, maxLossCount);

        int lossCount;

        if (maxLossCount <= minLossCount) {
            lossCount = minLossCount;
        } else {
            lossCount = ThreadLocalRandom.current().nextInt(minLossCount, maxLossCount + 1);
        }

        if (lossCount <= 0) {
            return 0;
        }

        Collections.shuffle(ownedBlockIds, ThreadLocalRandom.current());

        for (int i = 0; i < lossCount; i++) {
            String blockId = ownedBlockIds.get(i);
            CollectedBlockData data = collectedBlocks.get(blockId);

            if (data == null) {
                continue;
            }

            data.ownerType = CollectionOwnerType.NONE;
            data.ownerId = "";
            data.ownerName = "";
            data.state = BlockCollectionState.RELEASED;
        }

        return lossCount;
    }

    public enum ChallengeResult {
        NONE,
        CLEAR,
        FAIL,
        BLUE_WIN,
        RED_WIN,
        BLOCK_RACE_WIN,
        DRAW
    }

    public enum BlockCollectionState {
        UNCLAIMED,
        CLAIMED,
        RELEASED
    }

    public static class CollectedBlockData {
        public CollectionOwnerType ownerType = CollectionOwnerType.NONE;

        @SerializedName(value = "ownerId", alternate = {"ownerUuid"})
        public String ownerId = "";

        public String ownerName = "";
        public BlockCollectionState state;

        public CollectedBlockData() {
        }

        public CollectedBlockData(
                CollectionOwnerType ownerType,
                String ownerId,
                String ownerName,
                BlockCollectionState state
        ) {
            this.ownerType = ownerType == null ? CollectionOwnerType.NONE : ownerType;
            this.ownerId = ownerId == null ? "" : ownerId;
            this.ownerName = ownerName == null ? "" : ownerName;
            this.state = state;
        }
    }

    public static class ParticipantData {
        public String playerUuid;
        public String playerName;
        public String color;
        public String teamId = TeamRaceTeam.NONE.name();
        public int lastProgressEventTier = 0;

        public ParticipantData() {
        }

        public ParticipantData(
                String playerUuid,
                String playerName,
                String color
        ) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.color = color;
            this.teamId = TeamRaceTeam.NONE.name();
        }
    }
}