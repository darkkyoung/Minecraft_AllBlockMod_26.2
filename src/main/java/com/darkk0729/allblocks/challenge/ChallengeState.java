package com.darkk0729.allblocks.challenge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChallengeState {
    public static final long TICKS_PER_SECOND = 20L;
    public static final long TICKS_PER_DAY = 24000L;
    public static final int MAX_DAYS = 100;

    private boolean running;
    private ChallengeMode mode;
    private long elapsedTicks;

    private final Map<String, CollectedBlockData> collectedBlocks = new HashMap<>();

    public ChallengeState() {
        this.running = false;
        this.mode = ChallengeMode.SOLO;
        this.elapsedTicks = 0L;
    }

    public boolean isRunning() {
        return running;
    }

    public ChallengeMode getMode() {
        return mode;
    }

    public long getElapsedTicks() {
        return elapsedTicks;
    }

    public int getCurrentDay() {
        return (int) (elapsedTicks / TICKS_PER_DAY) + 1;
    }

    public String getFormattedElapsedTime() {
        long totalSeconds = elapsedTicks / TICKS_PER_SECOND;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void start(ChallengeMode mode) {
        this.running = true;
        this.mode = mode;
        this.elapsedTicks = 0L;
        this.collectedBlocks.clear();
    }

    public void stop() {
        this.running = false;
    }

    public void loadFrom(
            boolean running,
            ChallengeMode mode,
            long elapsedTicks,
            Map<String, CollectedBlockData> loadedCollectedBlocks
    ) {
        this.running = running;
        this.mode = mode == null ? ChallengeMode.SOLO : mode;
        this.elapsedTicks = Math.max(0L, elapsedTicks);

        this.collectedBlocks.clear();

        if (loadedCollectedBlocks != null) {
            this.collectedBlocks.putAll(loadedCollectedBlocks);
        }
    }

    public boolean collectBlock(String blockId, UUID ownerUuid, String ownerName) {
        if (collectedBlocks.containsKey(blockId)) {
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

    public boolean tick() {
        if (!running) {
            return false;
        }

        elapsedTicks++;

        return elapsedTicks >= TICKS_PER_DAY * MAX_DAYS;
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
}