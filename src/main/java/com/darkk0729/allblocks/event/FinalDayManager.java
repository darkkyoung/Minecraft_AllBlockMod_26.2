package com.darkk0729.allblocks.event;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import com.darkk0729.allblocks.collection.TargetBlockRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import com.darkk0729.allblocks.challenge.ChallengeState;

public final class FinalDayManager {
    private static final int FINAL_DAY = 100;
    private static final long TICKS_PER_SECOND = 20L;
    private static final long TICKS_PER_DAY = 24000L;
    private static final long FINAL_END_TICKS = FINAL_DAY * TICKS_PER_DAY;

    private static final String PROGRESS_BOSSBAR_ID = "allblocks:progress";

    private static boolean active = false;
    private static boolean warnedFiveMinutes = false;
    private static boolean warnedThreeMinutes = false;
    private static boolean warnedOneMinute = false;

    private static int bossBarUpdateTicker = 0;
    private static int lastCountdownSecond = -1;

    private FinalDayManager() {
    }

    public static void reset() {
        active = false;
        warnedFiveMinutes = false;
        warnedThreeMinutes = false;
        warnedOneMinute = false;
        bossBarUpdateTicker = 0;
        lastCountdownSecond = -1;
    }

    public static boolean isFinalDayActive() {
        return active;
    }

    public static void tick(MinecraftServer server) {
        if (server == null || !ChallengeManager.isRunning()) {
            reset();
            return;
        }

        if (ChallengeManager.getCurrentDay() != FINAL_DAY) {
            return;
        }

        long remainingTicks = Math.max(0L, FINAL_END_TICKS - ChallengeManager.getWorldElapsedTicks());

        if (!active) {
            startFinalDay(server);
        }

        sendTimeWarnings(server, remainingTicks);
        sendFinalCountdown(server, remainingTicks);

        bossBarUpdateTicker++;
        if (bossBarUpdateTicker >= TICKS_PER_SECOND) {
            bossBarUpdateTicker = 0;
            updateFinalDayBossBar(server, remainingTicks);
        }
    }

    public static void showResult(MinecraftServer server, ChallengeState.ChallengeResult result) {
        if (server == null) {
            return;
        }

        int collected = ChallengeManager.getCollectedCount();
        int total = TargetBlockRegistry.getTargetBlocks().size();

        if (result == ChallengeState.ChallengeResult.CLEAR) {
            runCommand(server, "title @a times 10 70 20");
            runCommand(server, "title @a title " + jsonText("CLEAR", "green"));
            runCommand(server, "title @a subtitle " + jsonText("모든 블록 수집 완료 | 수집 " + collected + " / " + total, "yellow"));
            runCommand(server, "playsound minecraft:entity.player.levelup master @a ~ ~ ~ 1 1");

            runCommand(server, "tellraw @a " + jsonText("[Block Race] 챌린지 클리어! 모든 블록을 수집했습니다. 최종 기록: " + collected + " / " + total, "green"));
        } else {
            runCommand(server, "title @a times 10 80 20");
            runCommand(server, "title @a title " + jsonText("FAIL", "red"));
            runCommand(server, "title @a subtitle " + jsonText("모든 블록을 수집하지 못했습니다 | 수집 " + collected + " / " + total, "gray"));
            runCommand(server, "playsound minecraft:entity.wither.death master @a ~ ~ ~ 1 0.8");

            runCommand(server, "tellraw @a " + jsonText("[Block Race] 챌린지 실패. 모든 블록을 수집하지 못했습니다. 최종 기록: " + collected + " / " + total, "red"));
        }

        reset();
    }

    private static void startFinalDay(MinecraftServer server) {
        active = true;
        bossBarUpdateTicker = 0;
        lastCountdownSecond = -1;

        runCommand(server, "title @a times 10 60 20");
        runCommand(server, "title @a title " + jsonText("최종일", "gold"));
        runCommand(server, "title @a subtitle " + jsonText("100일차를 끝까지 버티세요", "yellow"));
        runCommand(server, "playsound minecraft:block.note_block.bell master @a ~ ~ ~ 1 0.8");

        updateFinalDayBossBar(server, TICKS_PER_DAY);
    }

    private static void sendTimeWarnings(MinecraftServer server, long remainingTicks) {
        long remainingSeconds = getRemainingSeconds(remainingTicks);

        if (remainingSeconds <= 60 && !warnedOneMinute) {
            warnedFiveMinutes = true;
            warnedThreeMinutes = true;
            warnedOneMinute = true;

            runCommand(server, "title @a times 5 50 10");
            runCommand(server, "title @a title " + jsonText("1분 남음", "red"));
            runCommand(server, "title @a subtitle " + jsonText("서둘러 모든 블록을 모으세요!", "yellow"));
            runCommand(server, "playsound minecraft:block.note_block.bell master @a ~ ~ ~ 1 0.5");
            return;
        }

        if (remainingSeconds <= 180 && !warnedThreeMinutes) {
            warnedFiveMinutes = true;
            warnedThreeMinutes = true;

            runCommand(server, "title @a times 5 45 10");
            runCommand(server, "title @a title " + jsonText("3분 남음", "gold"));
            runCommand(server, "title @a subtitle " + jsonText("최종일이 얼마 남지 않았습니다", "yellow"));
            runCommand(server, "playsound minecraft:block.note_block.pling master @a ~ ~ ~ 1 0.8");
            return;
        }

        if (remainingSeconds <= 300 && !warnedFiveMinutes) {
            warnedFiveMinutes = true;

            runCommand(server, "title @a times 5 40 10");
            runCommand(server, "title @a title " + jsonText("5분 남음", "yellow"));
            runCommand(server, "title @a subtitle " + jsonText("최종 카운트다운 시작", "gold"));
            runCommand(server, "playsound minecraft:block.note_block.pling master @a ~ ~ ~ 1 1");
        }
    }

    private static void sendFinalCountdown(MinecraftServer server, long remainingTicks) {
        int remainingSeconds = (int) getRemainingSeconds(remainingTicks);

        if (remainingSeconds <= 0 || remainingSeconds > 10) {
            return;
        }

        if (remainingSeconds == lastCountdownSecond) {
            return;
        }

        lastCountdownSecond = remainingSeconds;

        String color = remainingSeconds <= 3 ? "red" : "gold";

        runCommand(server, "title @a times 0 20 0");
        runCommand(server, "title @a title " + jsonText(String.valueOf(remainingSeconds), color));
        runCommand(server, "playsound minecraft:block.note_block.hat master @a ~ ~ ~ 1 1");
    }

    private static void updateFinalDayBossBar(MinecraftServer server, long remainingTicks) {
        int collected = ChallengeManager.getCollectedCount();
        int total = TargetBlockRegistry.getTargetBlocks().size();

        String timeText = formatRemainingTime(remainingTicks);
        String textColor = getTextColor(remainingTicks);
        String bossBarColor = getBossBarColor(remainingTicks);

        String bossBarText = "최종일 | 남은 시간 " + timeText + " | 수집 " + collected + " / " + total;

        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " visible true");
        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " color " + bossBarColor);
        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " style progress");
        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " max " + Math.max(total, 1));
        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " value " + Math.min(collected, Math.max(total, 1)));
        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " name " + jsonText(bossBarText, textColor));
    }

    private static String formatRemainingTime(long remainingTicks) {
        long totalSeconds = getRemainingSeconds(remainingTicks);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        return String.format("%02d:%02d", minutes, seconds);
    }

    private static long getRemainingSeconds(long remainingTicks) {
        return Math.max(0L, (remainingTicks + TICKS_PER_SECOND - 1L) / TICKS_PER_SECOND);
    }

    private static String getTextColor(long remainingTicks) {
        long remainingSeconds = getRemainingSeconds(remainingTicks);

        if (remainingSeconds <= 60) {
            return "red";
        }

        if (remainingSeconds <= 180) {
            return "gold";
        }

        if (remainingSeconds <= 300) {
            return "yellow";
        }

        return "green";
    }

    private static String getBossBarColor(long remainingTicks) {
        long remainingSeconds = getRemainingSeconds(remainingTicks);

        if (remainingSeconds <= 60) {
            return "red";
        }

        if (remainingSeconds <= 300) {
            return "yellow";
        }

        return "green";
    }

    private static String jsonText(String text, String color) {
        return "{\"text\":\"" + escapeJson(text) + "\",\"color\":\"" + color + "\"}";
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void runCommand(MinecraftServer server, String command) {
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
    }
}