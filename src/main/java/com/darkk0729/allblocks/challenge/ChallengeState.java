package com.darkk0729.allblocks.challenge;

public class ChallengeState {
    public static final long TICKS_PER_SECOND = 20L;
    public static final long TICKS_PER_DAY = 24000L;
    public static final int MAX_DAYS = 100;

    private boolean running;
    private ChallengeMode mode;
    private long elapsedTicks;

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
    }

    public void stop() {
        this.running = false;
    }

    public void loadFrom(boolean running, ChallengeMode mode, long elapsedTicks) {
        this.running = running;
        this.mode = mode == null ? ChallengeMode.SOLO : mode;
        this.elapsedTicks = Math.max(0L, elapsedTicks);
    }

    /**
     * @return true if the challenge reached the time limit and should be stopped.
     */
    public boolean tick() {
        if (!running) {
            return false;
        }

        elapsedTicks++;

        return elapsedTicks >= TICKS_PER_DAY * MAX_DAYS;
    }
}