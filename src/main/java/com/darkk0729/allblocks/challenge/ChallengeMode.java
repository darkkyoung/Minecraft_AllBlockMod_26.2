package com.darkk0729.allblocks.challenge;

public enum ChallengeMode {
    SOLO("Solo"),
    CO_OP("Co-op"),
    TEAM_RACE("Team Race"),
    BLOCK_RACE("Block Race");

    private final String displayName;

    ChallengeMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}