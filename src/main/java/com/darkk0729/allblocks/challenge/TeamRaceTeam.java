package com.darkk0729.allblocks.challenge;

public enum TeamRaceTeam {
    NONE("미배정"),
    BLUE("블루팀"),
    RED("레드팀");

    private final String displayName;

    TeamRaceTeam(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getOwnerId() {
        return name();
    }

    public boolean isAssigned() {
        return this != NONE;
    }

    public static TeamRaceTeam fromName(String name) {
        if (name == null || name.isBlank()) {
            return NONE;
        }

        try {
            return TeamRaceTeam.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }
}