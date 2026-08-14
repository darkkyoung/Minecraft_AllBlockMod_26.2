package com.darkk0729.allblocks.challenge;

import java.util.Locale;

public enum PlayerCodexColor {
    RED("빨강", 0xFFFF5555),
    ORANGE("주황", 0xFFFFAA00),
    YELLOW("노랑", 0xFFFFFF55),
    LIME("라임", 0xFF55FF55),
    GREEN("초록", 0xFF00AA00),
    CYAN("청록", 0xFF55FFFF),
    BLUE("파랑", 0xFF5555FF),
    PURPLE("보라", 0xFFAA55FF),
    PINK("분홍", 0xFFFF55FF),
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