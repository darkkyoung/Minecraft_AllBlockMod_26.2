package com.darkk0729.allblocks.challenge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.LinkedHashMap;

public class ChallengeState {
    public static final long TICKS_PER_SECOND = 20L;
    public static final long TICKS_PER_DAY = 24000L;
    public static final int MAX_DAYS = 100;

    private boolean running;
    private ChallengeMode mode;
    private ChallengeDifficulty difficulty;
    private boolean finished;
    private ChallengeResult result;

    // 실제 플레이타임 타이머
    private long elapsedTicks;

    // 인게임 월드 Day 계산용
    private long startWorldTime;
    private long worldElapsedTicks;
    private long lastWorldClockTime;

    private int lastProgressEventTier;
    private int lastDayRaidEventDay;

    private final Map<String, CollectedBlockData> collectedBlocks = new HashMap<>();
    private final Map<String, ParticipantData> participants = new LinkedHashMap<>();

    public ChallengeState() {
        this.running = false;
        this.finished = false;
        this.result = ChallengeResult.NONE;
        this.mode = ChallengeMode.SOLO;
        this.difficulty = ChallengeDifficulty.HARD;
        this.elapsedTicks = 0L;
        this.startWorldTime = 0L;
        this.worldElapsedTicks = 0L;
        this.lastWorldClockTime = 0L;
        this.lastProgressEventTier = 0;
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

    public int getLastDayRaidEventDay() {
        return lastDayRaidEventDay;
    }

    public void setLastDayRaidEventDay(int lastDayRaidEventDay) {
        this.lastDayRaidEventDay = Math.max(0, Math.min(100, lastDayRaidEventDay));
    }

    public void setLastProgressEventTier(int lastProgressEventTier) {
        this.lastProgressEventTier = Math.max(0, Math.min(10, lastProgressEventTier));
    }

    // Day는 실제 플레이타임이 아니라 인게임 월드 시간 기준
    public int getCurrentDay() {
        return (int) (worldElapsedTicks / TICKS_PER_DAY) + 1;
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
        start(mode, ChallengeDifficulty.HARD, 0L);
    }

    public void start(ChallengeMode mode, long startWorldTime) {
        start(mode, ChallengeDifficulty.HARD, startWorldTime);
    }

    public void start(ChallengeMode mode, ChallengeDifficulty difficulty, long startWorldTime) {
        this.running = true;
        this.finished = false;

        this.mode = mode == null ? ChallengeMode.SOLO : mode;
        this.difficulty = difficulty == null ? ChallengeDifficulty.HARD : difficulty;

        long safeStartWorldTime = Math.max(0L, startWorldTime);

        this.elapsedTicks = 0L;
        this.startWorldTime = safeStartWorldTime;
        this.worldElapsedTicks = 0L;
        this.lastWorldClockTime = safeStartWorldTime;

        this.result = ChallengeResult.NONE;
        this.lastProgressEventTier = 0;
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
            long elapsedTicks,
            long startWorldTime,
            long worldElapsedTicks,
            ChallengeResult result,
            int lastProgressEventTier,
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

        this.elapsedTicks = Math.max(0L, elapsedTicks);
        this.startWorldTime = Math.max(0L, startWorldTime);
        this.worldElapsedTicks = Math.max(0L, worldElapsedTicks);

        this.lastProgressEventTier = Math.max(0, Math.min(10, lastProgressEventTier));
        this.lastDayRaidEventDay = Math.max(0, Math.min(100, lastDayRaidEventDay));

        this.collectedBlocks.clear();

        if (loadedCollectedBlocks != null) {
            this.collectedBlocks.putAll(loadedCollectedBlocks);
        }

        this.participants.clear();

        if (loadedParticipants != null) {
            this.participants.putAll(loadedParticipants);
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

        /*
         * 26.x의 overworld clock 값이 하루 주기처럼 되감기는 경우를 대비한다.
         * 자연스럽게 하루가 넘어가며 current가 last보다 작아졌다면,
         * 24000틱을 더해서 실제 경과량으로 보정한다.
         */
        if (delta < 0L) {
            delta += TICKS_PER_DAY;
        }

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

        return worldElapsedTicks >= TICKS_PER_DAY * MAX_DAYS;
    }

    public boolean tick() {
        if (!running) {
            return false;
        }

        elapsedTicks++;
        return false;
    }

    public boolean collectBlock(String blockId, UUID ownerUuid, String ownerName) {
        CollectedBlockData existingData = collectedBlocks.get(blockId);

        if (existingData != null && existingData.state == BlockCollectionState.CLAIMED) {
            return false;
        }

        collectedBlocks.put(blockId, new CollectedBlockData(
                ownerUuid.toString(),
                ownerName,
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

    public int getOwnedBlockCount(String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) {
            return 0;
        }

        int count = 0;

        for (CollectedBlockData data : collectedBlocks.values()) {
            if (data == null) {
                continue;
            }

            if (data.state != BlockCollectionState.CLAIMED) {
                continue;
            }

            if (!playerUuid.equals(data.ownerUuid)) {
                continue;
            }

            count++;
        }

        return count;
    }

    public int releaseRandomOwnedBlocks(UUID ownerUuid, int minPercent, int maxPercent) {
        if (ownerUuid == null) {
            return 0;
        }

        String ownerId = ownerUuid.toString();
        List<String> ownedBlockIds = new ArrayList<>();

        for (Map.Entry<String, CollectedBlockData> entry : collectedBlocks.entrySet()) {
            CollectedBlockData data = entry.getValue();

            if (data == null) {
                continue;
            }

            if (data.state != BlockCollectionState.CLAIMED) {
                continue;
            }

            if (!ownerId.equals(data.ownerUuid)) {
                continue;
            }

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

            data.ownerUuid = "";
            data.ownerName = "";
            data.state = BlockCollectionState.RELEASED;
        }

        return lossCount;
    }

    public enum ChallengeResult {
        NONE,
        CLEAR,
        FAIL
    }

    public enum BlockCollectionState {
        UNCLAIMED,
        CLAIMED,
        RELEASED
    }

    public static class CollectedBlockData {
        public String ownerUuid;
        public String ownerName;
        public BlockCollectionState state;

        public CollectedBlockData() {
        }

        public CollectedBlockData(String ownerUuid, String ownerName, BlockCollectionState state) {
            this.ownerUuid = ownerUuid;
            this.ownerName = ownerName;
            this.state = state;
        }
    }

    public static class ParticipantData {
        public String playerUuid;
        public String playerName;
        public String color;

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
        }
    }
}