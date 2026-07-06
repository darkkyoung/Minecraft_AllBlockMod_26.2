package com.darkk0729.allblocks.challenge;

public class ChallengeState {
    private boolean running;
    private ChallengeMode mode;

    public ChallengeState() {
        this.running = false;
        this.mode = ChallengeMode.SOLO;
    }

    public boolean isRunning() {
        return running;
    }

    public ChallengeMode getMode() {
        return mode;
    }

    public void start(ChallengeMode mode) {
        this.running = true;
        this.mode = mode;
    }

    public void stop() {
        this.running = false;
    }
}