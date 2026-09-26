package com.darkk0729.allblocks.challenge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class BlockRaceSetupManager {
    private static final int COUNTDOWN_SECONDS = 5;
    private static final int COUNTDOWN_TICKS = 20 * COUNTDOWN_SECONDS;

    private static boolean active = false;
    private static ChallengeDifficulty difficulty = ChallengeDifficulty.HARD;
    private static int countdownTicksRemaining = 0;
    private static int lastCountdownSecond = -1;

    private static final Map<UUID, String> participantNames =
            new LinkedHashMap<>();

    private BlockRaceSetupManager() {
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean begin(
            MinecraftServer server,
            ChallengeDifficulty selectedDifficulty
    ) {
        if (server == null
                || ChallengeManager.isRunning()
                || TeamRaceSetupManager.isActive()
                || active) {
            return false;
        }

        if (!captureOnlineParticipants(server)) {
            broadcast(
                    server,
                    Component.literal(
                            "[올블록 챌린지] 블록 레이스는 최소 2명이 필요합니다."
                    ).withStyle(ChatFormatting.RED)
            );
            return false;
        }

        difficulty =
                selectedDifficulty == null
                        ? ChallengeDifficulty.HARD
                        : selectedDifficulty;

        active = true;
        countdownTicksRemaining = COUNTDOWN_TICKS;
        lastCountdownSecond = COUNTDOWN_SECONDS;

        clearParticipantInventories(server);
        setParticipantGameMode(server, "adventure");

        broadcast(
                server,
                Component.literal(
                        "[올블록 챌린지] 블록 레이스를 시작합니다. 5초 후 출발합니다."
                ).withStyle(ChatFormatting.GREEN)
        );

        showCountdownTitle(server, COUNTDOWN_SECONDS);
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (server == null || !active) {
            return;
        }

        if (!sameOnlineParticipants(server)) {
            broadcast(
                    server,
                    Component.literal(
                            "[올블록 챌린지] 카운트다운 중 접속 인원이 변경되어 블록 레이스 시작이 취소되었습니다."
                    ).withStyle(ChatFormatting.RED)
            );

            reset(server);
            return;
        }

        clearParticipantInventories(server);

        if (countdownTicksRemaining > 0) {
            countdownTicksRemaining--;
        }

        if (countdownTicksRemaining <= 0) {
            finishCountdown(server);
            return;
        }

        int second =
                (countdownTicksRemaining + 19) / 20;

        if (second > 0
                && second != lastCountdownSecond) {
            lastCountdownSecond = second;
            showCountdownTitle(server, second);
        }
    }

    public static void reset(MinecraftServer server) {
        if (server != null && active) {
            setParticipantGameMode(server, "survival");

            for (String playerName : participantNames.values()) {
                runCommand(server, "title " + playerName + " clear");
            }
        }

        active = false;
        difficulty = ChallengeDifficulty.HARD;
        countdownTicksRemaining = 0;
        lastCountdownSecond = -1;
        participantNames.clear();
    }

    private static void finishCountdown(MinecraftServer server) {
        ChallengeDifficulty finalDifficulty =
                difficulty;

        clearParticipantInventories(server);
        setParticipantGameMode(server, "survival");

        active = false;

        boolean started =
                ChallengeManager.startBlockRace(
                        server,
                        finalDifficulty
                );

        if (started) {
            showStartTitle(server);
        }

        difficulty = ChallengeDifficulty.HARD;
        countdownTicksRemaining = 0;
        lastCountdownSecond = -1;
        participantNames.clear();
    }

    private static boolean captureOnlineParticipants(
            MinecraftServer server
    ) {
        if (server == null) {
            return false;
        }

        if (server.getPlayerList().getPlayers().size() < 2) {
            return false;
        }

        participantNames.clear();

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {
            participantNames.put(
                    player.getUUID(),
                    player.getName().getString()
            );
        }

        return true;
    }

    private static boolean sameOnlineParticipants(
            MinecraftServer server
    ) {
        if (server == null
                || server.getPlayerList().getPlayers().size()
                != participantNames.size()) {
            return false;
        }

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {
            if (!participantNames.containsKey(player.getUUID())) {
                return false;
            }
        }

        return true;
    }

    private static void clearParticipantInventories(
            MinecraftServer server
    ) {
        for (String playerName : participantNames.values()) {
            runCommand(server, "clear " + playerName);
        }
    }

    private static void setParticipantGameMode(
            MinecraftServer server,
            String gameMode
    ) {
        for (String playerName : participantNames.values()) {
            runCommand(
                    server,
                    "gamemode "
                            + gameMode
                            + " "
                            + playerName
            );
        }
    }

    private static void showCountdownTitle(
            MinecraftServer server,
            int second
    ) {
        for (String playerName : participantNames.values()) {
            runCommand(
                    server,
                    "title "
                            + playerName
                            + " times 0 20 0"
            );

            runCommand(
                    server,
                    "title "
                            + playerName
                            + " title "
                            + "{\"text\":\""
                            + second
                            + "\",\"color\":\"gold\",\"bold\":true}"
            );
        }
    }

    private static void showStartTitle(
            MinecraftServer server
    ) {
        for (String playerName : participantNames.values()) {
            runCommand(
                    server,
                    "title "
                            + playerName
                            + " times 0 30 10"
            );

            runCommand(
                    server,
                    "title "
                            + playerName
                            + " title "
                            + "{\"text\":\"시작!\",\"color\":\"green\",\"bold\":true}"
            );
        }
    }

    private static void broadcast(
            MinecraftServer server,
            Component message
    ) {
        if (server == null) {
            return;
        }

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }

    private static void runCommand(
            MinecraftServer server,
            String command
    ) {
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withSuppressedOutput(),
                command
        );
    }
}
