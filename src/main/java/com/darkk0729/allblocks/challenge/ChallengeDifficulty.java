package com.darkk0729.allblocks.challenge;

public enum ChallengeDifficulty {
    EASY("쉬움"),
    NORMAL("보통"),
    HARD("어려움");

    private final String displayName;

    ChallengeDifficulty(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}