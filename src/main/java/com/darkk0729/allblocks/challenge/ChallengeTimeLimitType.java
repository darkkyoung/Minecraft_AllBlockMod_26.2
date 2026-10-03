package com.darkk0729.allblocks.challenge;

public enum ChallengeTimeLimitType {
    NONE("제한 없음"),
    PLAY_TIME("플레이 시간"),
    IN_GAME_TIME("인게임 시간");

    private final String displayName;

    ChallengeTimeLimitType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
