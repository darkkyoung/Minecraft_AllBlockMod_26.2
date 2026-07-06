package com.darkk0729.allblocks.challenge;

public final class ChallengeManager {
    private static final ChallengeState STATE = new ChallengeState();

    private ChallengeManager() {
    }

    public static boolean isRunning() {
        return STATE.isRunning();
    }

    public static ChallengeMode getMode() {
        return STATE.getMode();
    }

    public static void startSolo() {
        STATE.start(ChallengeMode.SOLO);
    }

    public static void stop() {
        STATE.stop();
    }
}