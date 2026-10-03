package com.darkk0729.allblocks.challenge;

import java.util.Locale;

public enum PlayerCodexColor {
    RED("빨강", 0xFFFF5555),
    ORANGE("주황", 0xFFFFAA00),
    YELLOW("노랑", 0xFFFFFF55),
    LIME("연두", 0xFF55FF55),
    GREEN("초록", 0xFF00AA00),
    CYAN("하늘", 0xFF55FFFF),
    BLUE("파랑", 0xFF5555FF),
    PURPLE("보라", 0xFFAA55FF),
    PINK("핑크", 0xFFFF55FF),
    BLACK("검정", 0xFF000000),
    WHITE("흰색", 0xFFFFFFFF);

    private final String displayName;
    private final int argb;

    PlayerCodexColor(String displayName, int argb) {
        this.displayName = displayName;
        this.argb = argb;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getArgb() {
        return argb;
    }

    public static PlayerCodexColor fromName(String name) {
        if (name == null || name.isBlank()) {
            return BLUE;
        }

        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BLUE;
        }
    }
}