package com.darkk0729.allblocks.challenge;

public enum ChallengeMode {
    SOLO("싱글"),
    CO_OP("협동"),
    TEAM_RACE("팀 레이스"),
    BLOCK_RACE("블록 레이스");

    private final String displayName;

    ChallengeMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}