package com.darkk0729.allblocks.challenge;

import com.darkk0729.allblocks.network.TeamRevealPayload;
import com.darkk0729.allblocks.network.CloseSetupScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TeamRaceSetupManager {
    private static final int REVEAL_TICKS = 20 * 7;
    private static final int COUNTDOWN_SECONDS = 5;
    private static final int COUNTDOWN_TICKS =
            20 * COUNTDOWN_SECONDS;

    private static SetupPhase phase = SetupPhase.IDLE;
    private static ChallengeDifficulty difficulty =
            ChallengeDifficulty.HARD;
    private static ChallengeTimeLimitType timeLimitType =
            ChallengeTimeLimitType.IN_GAME_TIME;
    private static long timeLimitTicks =
            ChallengeState.TICKS_PER_DAY
                    * ChallengeState.MAX_DAYS;
    private static int progressEventIntervalPercent = 10;
    private static int dayRaidIntervalDays = 10;
    private static boolean resetWorldTime = true;

    private static UUID controllerUuid;
    private static int revealTicksRemaining = 0;
    private static int revealElapsedTicks = 0;
    private static int countdownTicksRemaining = 0;
    private static int lastCountdownSecond = -1;

    private static final Map<UUID, String> participantNames =
            new LinkedHashMap<>();

    private static final Map<UUID, TeamRaceTeam> assignments =
            new LinkedHashMap<>();

    private static final Set<UUID> spectators =
            new LinkedHashSet<>();

    private TeamRaceSetupManager() {
    }

    public static boolean isActive() {
        return phase != SetupPhase.IDLE;
    }

    public static boolean isLocked() {
        return phase == SetupPhase.COUNTDOWN;
    }

    public static ChallengeDifficulty getDifficulty() {
        return difficulty;
    }

    public static Map<UUID, TeamRaceTeam> getAssignments() {
        return Collections.unmodifiableMap(assignments);
    }

    public static Set<UUID> getSpectators() {
        return Collections.unmodifiableSet(spectators);
    }

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }

        if (phase == SetupPhase.REVEALING) {
            tickReveal(server);
            return;
        }

        if (phase == SetupPhase.COUNTDOWN) {
            tickCountdown(server);
        }
    }

    private static void tickReveal(
            MinecraftServer server
    ) {
        revealElapsedTicks++;

        if (shouldPlayRevealTickSound(
                revealElapsedTicks
        )) {
            playRevealTickSound(server);
        }

        if (revealTicksRemaining > 0) {
            revealTicksRemaining--;
        }

        if (revealTicksRemaining > 0) {
            return;
        }

        phase = SetupPhase.READY;
        applyScoreboardTeams(server);
        showAssignmentResult(server);
    }

    public static boolean begin(
            MinecraftServer server,
            ServerPlayer controller,
            ChallengeDifficulty selectedDifficulty
    ) {
        return begin(
                server,
                controller,
                selectedDifficulty,
                ChallengeTimeLimitType.IN_GAME_TIME,
                ChallengeState.TICKS_PER_DAY
                        * ChallengeState.MAX_DAYS,
                10,
                10,
                true
        );
    }

    public static boolean begin(
            MinecraftServer server,
            ServerPlayer controller,
            ChallengeDifficulty selectedDifficulty,
            ChallengeTimeLimitType selectedTimeLimitType,
            long selectedTimeLimitTicks,
            boolean shouldResetWorldTime
    ) {
        return begin(
                server,
                controller,
                selectedDifficulty,
                selectedTimeLimitType,
                selectedTimeLimitTicks,
                10,
                10,
                shouldResetWorldTime
        );
    }

    public static boolean begin(
            MinecraftServer server,
            ServerPlayer controller,
            ChallengeDifficulty selectedDifficulty,
            ChallengeTimeLimitType selectedTimeLimitType,
            long selectedTimeLimitTicks,
            int selectedProgressEventIntervalPercent,
            int selectedDayRaidIntervalDays,
            boolean shouldResetWorldTime
    ) {
        if (server == null || controller == null) {
            return false;
        }

        if (ChallengeManager.isRunning()) {
            controller.sendSystemMessage(
                    Component.literal(
                            "[올블록 챌린지] 이미 챌린지가 진행 중입니다."
                    )
            );
            return false;
        }

        if (isActive()
                && controllerUuid != null
                && !controllerUuid.equals(
                        controller.getUUID()
                )) {
            controller.sendSystemMessage(
                    Component.literal(
                            "[올블록 챌린지] 다른 플레이어가 팀 편성을 진행 중입니다."
                    )
            );
            return false;
        }

        controllerUuid = controller.getUUID();

        difficulty =
                selectedDifficulty == null
                        ? ChallengeDifficulty.HARD
                        : selectedDifficulty;

        timeLimitType =
                selectedTimeLimitType == null
                        ? ChallengeTimeLimitType.IN_GAME_TIME
                        : selectedTimeLimitType;

        timeLimitTicks =
                timeLimitType == ChallengeTimeLimitType.NONE
                        ? 0L
                        : Math.max(
                                1L,
                                selectedTimeLimitTicks
                        );

        progressEventIntervalPercent =
                Math.max(
                        0,
                        Math.min(
                                100,
                                selectedProgressEventIntervalPercent
                        )
                );

        dayRaidIntervalDays =
                Math.max(
                        0,
                        Math.min(
                                10000,
                                selectedDayRaidIntervalDays
                        )
                );

        resetWorldTime = shouldResetWorldTime;

        if (!captureOnlineParticipants(server)) {
            controller.sendSystemMessage(
                    Component.literal(
                            "[올블록 챌린지] 팀 레이스는 최소 2명이 필요합니다."
                    )
            );
            reset(server);
            return false;
        }

        randomizeAssignments();
        startReveal(server);
        return true;
    }

    public static boolean randomizeAgain(
            MinecraftServer server,
            ServerPlayer controller
    ) {
        if (!canControl(controller)) {
            return false;
        }

        if (phase != SetupPhase.READY
                && phase != SetupPhase.MANUAL) {
            return false;
        }

        if (!captureOnlineParticipants(server)) {
            controller.sendSystemMessage(
                    Component.literal(
                            "[올블록 챌린지] 팀 레이스는 최소 2명이 필요합니다."
                    )
            );
            return false;
        }

        randomizeAssignments();
        startReveal(server);
        return true;
    }

    public static void showRerollMenu(
            ServerPlayer controller
    ) {
        if (!canControl(controller)) {
            return;
        }

        controller.sendSystemMessage(separator());
        controller.sendSystemMessage(
                Component.literal(
                        "[ 팀 다시 뽑기 ]"
                ).withStyle(
                        ChatFormatting.GOLD,
                        ChatFormatting.BOLD
                )
        );
        controller.sendSystemMessage(
                Component.literal("")
        );

        controller.sendSystemMessage(
                clickable(
                        "[랜덤 팀 배정]",
                        ChatFormatting.GREEN,
                        "/올블록 @팀 랜덤"
                ).append(
                        Component.literal(
                                " 인원을 균등하게 다시 랜덤 배정합니다."
                        ).withStyle(ChatFormatting.WHITE)
                )
        );

        controller.sendSystemMessage(
                clickable(
                        "[수동 팀 배정]",
                        ChatFormatting.YELLOW,
                        "/올블록 @팀 수동"
                ).append(
                        Component.literal(
                                " 플레이어를 레드/블루팀 또는 관전자로 직접 배정합니다."
                        ).withStyle(ChatFormatting.WHITE)
                )
        );

        controller.sendSystemMessage(separator());
    }

    public static boolean beginManual(
            MinecraftServer server,
            ServerPlayer controller
    ) {
        if (!canControl(controller)
                || phase != SetupPhase.READY) {
            return false;
        }

        if (!captureOnlineParticipants(server)) {
            return false;
        }

        assignments.clear();
        spectators.clear();

        for (UUID uuid :
                participantNames.keySet()) {
            assignments.put(
                    uuid,
                    TeamRaceTeam.NONE
            );
        }

        phase = SetupPhase.MANUAL;
        clearScoreboardMembers(server);
        showManualEditor(controller);

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {
            if (player.getUUID()
                    .equals(controllerUuid)) {
                continue;
            }

            player.sendSystemMessage(
                    Component.literal(
                            "[올블록 챌린지] 팀 편성자가 수동 팀 배정을 진행 중입니다."
                    )
            );
        }

        return true;
    }

    public static boolean cycleManualTeam(
            MinecraftServer server,
            ServerPlayer controller,
            String targetUuid
    ) {
        if (!canControl(controller)
                || phase != SetupPhase.MANUAL) {
            return false;
        }

        UUID uuid;

        try {
            uuid = UUID.fromString(targetUuid);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (!assignments.containsKey(uuid)) {
            return false;
        }

        if (spectators.remove(uuid)) {
            assignments.put(
                    uuid,
                    TeamRaceTeam.NONE
            );
        } else {
            TeamRaceTeam current =
                    assignments.getOrDefault(
                            uuid,
                            TeamRaceTeam.NONE
                    );

            switch (current) {
                case NONE -> assignments.put(
                        uuid,
                        TeamRaceTeam.RED
                );

                case RED -> assignments.put(
                        uuid,
                        TeamRaceTeam.BLUE
                );

                case BLUE -> {
                    assignments.put(
                            uuid,
                            TeamRaceTeam.NONE
                    );
                    spectators.add(uuid);
                }
            }
        }

        applyScoreboardTeams(server);
        showManualEditor(controller);
        return true;
    }

    public static boolean confirm(
            MinecraftServer server,
            ServerPlayer controller
    ) {
        if (!canControl(controller)) {
            return false;
        }

        if (phase != SetupPhase.READY
                && phase != SetupPhase.MANUAL) {
            return false;
        }

        if (!sameOnlineParticipants(server)) {
            controller.sendSystemMessage(
                    Component.literal(
                            "[올블록 챌린지] 팀 편성 중 접속 인원이 변경되었습니다. 다시 팀을 배정해주세요."
                    )
            );
            return false;
        }

        int redCount = 0;
        int blueCount = 0;

        for (UUID uuid :
                participantNames.keySet()) {
            if (spectators.contains(uuid)) {
                continue;
            }

            TeamRaceTeam team =
                    assignments.getOrDefault(
                            uuid,
                            TeamRaceTeam.NONE
                    );

            if (!team.isAssigned()) {
                controller.sendSystemMessage(
                        Component.literal(
                                "[올블록 챌린지] 아직 팀 또는 관전 여부가 정해지지 않은 플레이어가 있습니다."
                        )
                );
                return false;
            }

            if (team == TeamRaceTeam.RED) {
                redCount++;
            } else if (team == TeamRaceTeam.BLUE) {
                blueCount++;
            }
        }

        if (redCount < 1 || blueCount < 1) {
            controller.sendSystemMessage(
                    Component.literal(
                            "[올블록 챌린지] 레드팀과 블루팀에 최소 1명씩 필요합니다."
                    )
            );
            return false;
        }

        if (Math.abs(redCount - blueCount) > 1) {
            controller.sendSystemMessage(
                    Component.literal(
                            "[올블록 챌린지] 두 팀의 인원 차이는 최대 1명이어야 합니다."
                    )
            );
            return false;
        }

        applyScoreboardTeams(server);
        closeControllerSetupScreen(
                server
        );
        startCountdown(server);
        return true;
    }

    private static void startCountdown(
            MinecraftServer server
    ) {
        phase = SetupPhase.COUNTDOWN;
        countdownTicksRemaining = COUNTDOWN_TICKS;
        lastCountdownSecond = COUNTDOWN_SECONDS;

        clearParticipantInventories(server);
        setParticipantGameModeSilently(
                server,
                GameType.ADVENTURE
        );

        broadcast(
                server,
                Component.literal(
                        "[올블록 챌린지] 팀 배정이 확정되었습니다. 게임을 시작합니다."
                ).withStyle(ChatFormatting.GREEN)
        );

        showCountdownTitle(
                server,
                COUNTDOWN_SECONDS
        );
    }

    private static void tickCountdown(
            MinecraftServer server
    ) {
        if (!sameOnlineParticipants(server)) {
            broadcast(
                    server,
                    Component.literal(
                            "[올블록 챌린지] 카운트다운 중 접속 인원이 변경되어 팀 편성이 취소되었습니다."
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
            showCountdownTitle(
                    server,
                    second
            );
        }
    }

    private static void finishCountdown(
            MinecraftServer server
    ) {
        Map<UUID, TeamRaceTeam> finalAssignments =
                new LinkedHashMap<>(assignments);

        Set<UUID> finalSpectators =
                new LinkedHashSet<>(spectators);

        ChallengeDifficulty finalDifficulty =
                difficulty;

        ChallengeTimeLimitType finalTimeLimitType =
                timeLimitType;

        long finalTimeLimitTicks =
                timeLimitTicks;

        int finalProgressEventIntervalPercent =
                progressEventIntervalPercent;

        int finalDayRaidIntervalDays =
                dayRaidIntervalDays;

        boolean finalResetWorldTime =
                resetWorldTime;

        clearParticipantInventories(server);

        ChallengeManager.startTeamRace(
                server,
                finalDifficulty,
                finalAssignments,
                finalTimeLimitType,
                finalTimeLimitTicks,
                finalProgressEventIntervalPercent,
                finalDayRaidIntervalDays,
                finalResetWorldTime,
                finalSpectators
        );

        showStartTitle(server);
        completeSetup();
    }

    private static void clearParticipantInventories(
            MinecraftServer server
    ) {
        for (String playerName :
                participantNames.values()) {
            runCommand(
                    server,
                    "clear " + playerName
            );
        }
    }

    private static void setParticipantGameModeSilently(
            MinecraftServer server,
            GameType gameMode
    ) {
        if (server == null || gameMode == null) {
            return;
        }

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {
            if (participantNames.containsKey(
                    player.getUUID()
            )) {
                player.setGameMode(gameMode);
            }
        }
    }

    private static void setParticipantGameMode(
            MinecraftServer server,
            String gameMode
    ) {
        for (String playerName :
                participantNames.values()) {
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
        playCountdownSound(
                server,
                second
        );

        for (String playerName :
                participantNames.values()) {
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
        playStartSound(server);

        for (String playerName :
                participantNames.values()) {
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

    private static void playCountdownSound(
            MinecraftServer server,
            int second
    ) {
        float pitch =
                switch (second) {
                    case 5 -> 0.65F;
                    case 4 -> 0.78F;
                    case 3 -> 0.93F;
                    case 2 -> 1.10F;
                    case 1 -> 1.30F;
                    default -> 1.0F;
                };

        for (String playerName :
                participantNames.values()) {
            runCommand(
                    server,
                    "execute as "
                            + playerName
                            + " at @s run playsound "
                            + "minecraft:block.note_block.pling "
                            + "master @s ~ ~ ~ 0.9 "
                            + pitch
            );
        }
    }

    private static void playStartSound(
            MinecraftServer server
    ) {
        for (String playerName :
                participantNames.values()) {
            runCommand(
                    server,
                    "execute as "
                            + playerName
                            + " at @s run playsound "
                            + "minecraft:block.note_block.pling "
                            + "master @s ~ ~ ~ 1.0 1.65"
            );
        }
    }

    private static void completeSetup() {
        phase = SetupPhase.IDLE;
        difficulty = ChallengeDifficulty.HARD;
        timeLimitType =
                ChallengeTimeLimitType.IN_GAME_TIME;
        timeLimitTicks =
                ChallengeState.TICKS_PER_DAY
                        * ChallengeState.MAX_DAYS;
        progressEventIntervalPercent = 10;
        dayRaidIntervalDays = 10;
        resetWorldTime = true;
        controllerUuid = null;
        revealTicksRemaining = 0;
        revealElapsedTicks = 0;
        countdownTicksRemaining = 0;
        lastCountdownSecond = -1;
        participantNames.clear();
        assignments.clear();
        spectators.clear();
    }

    public static void reset(
            MinecraftServer server
    ) {
        boolean wasCountdown =
                phase == SetupPhase.COUNTDOWN;

        if (server != null && wasCountdown) {
            setParticipantGameMode(
                    server,
                    "survival"
            );

            for (String playerName :
                    participantNames.values()) {
                runCommand(
                        server,
                        "title "
                                + playerName
                                + " clear"
                );
            }
        }

        completeSetup();

        if (server != null) {
            runCommand(
                    server,
                    "team remove allblocks_blue"
            );
            runCommand(
                    server,
                    "team remove allblocks_red"
            );
        }
    }

    private static boolean captureOnlineParticipants(
            MinecraftServer server
    ) {
        List<ServerPlayer> players =
                server.getPlayerList().getPlayers();

        if (players.size() < 2) {
            return false;
        }

        participantNames.clear();

        for (ServerPlayer player : players) {
            participantNames.put(
                    player.getUUID(),
                    player.getName().getString()
            );
        }

        return true;
    }

    private static void randomizeAssignments() {
        List<UUID> players =
                new ArrayList<>(
                        participantNames.keySet()
                );

        Collections.shuffle(
                players,
                ThreadLocalRandom.current()
        );

        assignments.clear();
        spectators.clear();

        int blueCount = players.size() / 2;
        int redCount = players.size() / 2;

        if (players.size() % 2 != 0) {
            if (ThreadLocalRandom.current()
                    .nextBoolean()) {
                blueCount++;
            } else {
                redCount++;
            }
        }

        for (int i = 0;
             i < players.size();
             i++) {
            TeamRaceTeam team =
                    i < blueCount
                            ? TeamRaceTeam.BLUE
                            : TeamRaceTeam.RED;

            assignments.put(
                    players.get(i),
                    team
            );
        }
    }

    private static void startReveal(
            MinecraftServer server
    ) {
        phase = SetupPhase.REVEALING;
        revealTicksRemaining = REVEAL_TICKS;
        revealElapsedTicks = 0;

        closeControllerSetupScreen(
                server
        );
        clearScoreboardMembers(server);
        playRevealTickSound(server);

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {
            TeamRaceTeam team =
                    assignments.getOrDefault(
                            player.getUUID(),
                            TeamRaceTeam.NONE
                    );

            if (!team.isAssigned()) {
                continue;
            }

            ServerPlayNetworking.send(
                    player,
                    new TeamRevealPayload(
                            team.name()
                    )
            );
        }
    }

    private static boolean shouldPlayRevealTickSound(
            int elapsedTicks
    ) {
        if (elapsedTicks <= 60) {
            return elapsedTicks % 3 == 0;
        }

        return elapsedTicks == 65
                || elapsedTicks == 71
                || elapsedTicks == 78
                || elapsedTicks == 86
                || elapsedTicks == 96
                || elapsedTicks == 108
                || elapsedTicks == 122
                || elapsedTicks == 140;
    }

    private static void playRevealTickSound(
            MinecraftServer server
    ) {
        if (server == null) {
            return;
        }

        for (String playerName :
                participantNames.values()) {
            runCommand(
                    server,
                    "execute as "
                            + playerName
                            + " at @s run playsound "
                            + "minecraft:block.note_block.bell "
                            + "master @s ~ ~ ~ 0.7 1.0"
            );
        }
    }

    private static void closeControllerSetupScreen(
            MinecraftServer server
    ) {
        if (server == null
                || controllerUuid == null) {
            return;
        }

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {
            if (!controllerUuid.equals(
                    player.getUUID()
            )) {
                continue;
            }

            ServerPlayNetworking.send(
                    player,
                    new CloseSetupScreenPayload(
                            true
                    )
            );
            return;
        }
    }

    private static void showAssignmentResult(
            MinecraftServer server
    ) {
        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(separator());
            player.sendSystemMessage(
                    Component.literal(
                            "[ 팀 배정 결과 ]"
                    ).withStyle(
                            ChatFormatting.GOLD,
                            ChatFormatting.BOLD
                    )
            );
            player.sendSystemMessage(
                    Component.literal("")
            );
            player.sendSystemMessage(
                    Component.literal(
                            "레드팀"
                    ).withStyle(
                            ChatFormatting.RED,
                            ChatFormatting.BOLD
                    )
            );
            player.sendSystemMessage(
                    buildTeamPlayerLine(
                            TeamRaceTeam.RED
                    )
            );
            player.sendSystemMessage(
                    Component.literal("")
            );
            player.sendSystemMessage(
                    Component.literal(
                            "블루팀"
                    ).withStyle(
                            ChatFormatting.BLUE,
                            ChatFormatting.BOLD
                    )
            );
            player.sendSystemMessage(
                    buildTeamPlayerLine(
                            TeamRaceTeam.BLUE
                    )
            );
            player.sendSystemMessage(
                    Component.literal("")
            );

            if (player.getUUID()
                    .equals(controllerUuid)) {
                player.sendSystemMessage(
                        clickable(
                                "[확정]",
                                ChatFormatting.GREEN,
                                "/올블록 @팀 확정"
                        ).append(
                                Component.literal("   ")
                        ).append(
                                clickable(
                                        "[다시 팀 뽑기]",
                                        ChatFormatting.YELLOW,
                                        "/올블록 @팀 재추첨메뉴"
                                )
                        )
                );
            } else {
                player.sendSystemMessage(
                        Component.literal(
                                "팀 편성자가 결과를 확인 중입니다."
                        ).withStyle(ChatFormatting.GRAY)
                );
            }

            player.sendSystemMessage(separator());
        }
    }

    private static void showManualEditor(
            ServerPlayer controller
    ) {
        controller.sendSystemMessage(separator());
        controller.sendSystemMessage(
                Component.literal(
                        "[ 수동 팀 배정 ]"
                ).withStyle(
                        ChatFormatting.GOLD,
                        ChatFormatting.BOLD
                )
        );
        controller.sendSystemMessage(
                Component.literal(
                        "이름 클릭: 흰색 → 레드 → 블루 → 관전자 → 흰색"
                ).withStyle(ChatFormatting.GRAY)
        );
        controller.sendSystemMessage(
                Component.literal("")
        );

        controller.sendSystemMessage(
                Component.literal(
                        "레드팀 : "
                ).withStyle(ChatFormatting.RED)
                        .append(
                                buildClickableManualLine(
                                        TeamRaceTeam.RED
                                )
                        )
        );

        controller.sendSystemMessage(
                Component.literal(
                        "블루팀 : "
                ).withStyle(ChatFormatting.BLUE)
                        .append(
                                buildClickableManualLine(
                                        TeamRaceTeam.BLUE
                                )
                        )
        );

        controller.sendSystemMessage(
                Component.literal(
                        "관전자 : "
                ).withStyle(ChatFormatting.GRAY)
                        .append(
                                buildClickableSpectatorLine()
                        )
        );

        controller.sendSystemMessage(
                Component.literal(
                        "미배정 : "
                ).withStyle(ChatFormatting.WHITE)
                        .append(
                                buildClickableManualLine(
                                        TeamRaceTeam.NONE
                                )
                        )
        );

        controller.sendSystemMessage(
                Component.literal("")
        );

        controller.sendSystemMessage(
                clickable(
                        "[확정]",
                        ChatFormatting.GREEN,
                        "/올블록 @팀 확정"
                ).append(
                        Component.literal("   ")
                ).append(
                        clickable(
                                "[랜덤 팀 배정]",
                                ChatFormatting.YELLOW,
                                "/올블록 @팀 랜덤"
                        )
                )
        );

        controller.sendSystemMessage(separator());
    }

    private static MutableComponent buildClickableManualLine(
            TeamRaceTeam requestedTeam
    ) {
        MutableComponent line =
                Component.literal("");

        boolean first = true;

        for (Map.Entry<UUID, String> entry :
                participantNames.entrySet()) {
            if (spectators.contains(entry.getKey())) {
                continue;
            }

            TeamRaceTeam team =
                    assignments.getOrDefault(
                            entry.getKey(),
                            TeamRaceTeam.NONE
                    );

            if (team != requestedTeam) {
                continue;
            }

            if (!first) {
                line.append(
                        Component.literal("  ")
                );
            }

            ChatFormatting color =
                    switch (team) {
                        case RED -> ChatFormatting.RED;
                        case BLUE -> ChatFormatting.BLUE;
                        case NONE -> ChatFormatting.WHITE;
                    };

            line.append(
                    clickable(
                            entry.getValue(),
                            color,
                            "/올블록 @팀 순환 "
                                    + entry.getKey()
                    )
            );

            first = false;
        }

        if (first) {
            line.append(
                    Component.literal("-")
                            .withStyle(
                                    ChatFormatting.DARK_GRAY
                            )
            );
        }

        return line;
    }

    private static MutableComponent
    buildClickableSpectatorLine() {
        MutableComponent line =
                Component.literal("");

        boolean first = true;

        for (Map.Entry<UUID, String> entry :
                participantNames.entrySet()) {
            if (!spectators.contains(
                    entry.getKey())) {
                continue;
            }

            if (!first) {
                line.append(
                        Component.literal("  ")
                );
            }

            line.append(
                    clickable(
                            entry.getValue(),
                            ChatFormatting.GRAY,
                            "/올블록 @팀 순환 "
                                    + entry.getKey()
                    )
            );

            first = false;
        }

        if (first) {
            line.append(
                    Component.literal("-")
                            .withStyle(
                                    ChatFormatting.DARK_GRAY
                            )
            );
        }

        return line;
    }

    private static MutableComponent buildTeamPlayerLine(
            TeamRaceTeam requestedTeam
    ) {
        MutableComponent line =
                Component.literal("");

        boolean first = true;

        for (Map.Entry<UUID, String> entry :
                participantNames.entrySet()) {
            if (assignments.get(
                    entry.getKey())
                    != requestedTeam) {
                continue;
            }

            if (!first) {
                line.append(
                        Component.literal(", ")
                );
            }

            line.append(
                    Component.literal(
                            entry.getValue()
                    ).withStyle(
                            requestedTeam
                                    == TeamRaceTeam.RED
                                    ? ChatFormatting.RED
                                    : ChatFormatting.BLUE
                    )
            );

            first = false;
        }

        return line;
    }

    private static void applyScoreboardTeams(
            MinecraftServer server
    ) {
        setupScoreboardTeams(server);
        clearScoreboardMembers(server);

        for (Map.Entry<UUID, TeamRaceTeam> entry :
                assignments.entrySet()) {
            if (spectators.contains(
                    entry.getKey())) {
                continue;
            }

            TeamRaceTeam team =
                    entry.getValue();

            if (team == null
                    || !team.isAssigned()) {
                continue;
            }

            String playerName =
                    participantNames.get(
                            entry.getKey()
                    );

            if (playerName == null
                    || playerName.isBlank()) {
                continue;
            }

            String scoreboardTeam =
                    team == TeamRaceTeam.BLUE
                            ? "allblocks_blue"
                            : "allblocks_red";

            runCommand(
                    server,
                    "team join "
                            + scoreboardTeam
                            + " "
                            + playerName
            );
        }
    }

    private static void setupScoreboardTeams(
            MinecraftServer server
    ) {
        runCommand(
                server,
                "team add allblocks_blue"
        );
        runCommand(
                server,
                "team add allblocks_red"
        );
        runCommand(
                server,
                "team modify allblocks_blue color blue"
        );
        runCommand(
                server,
                "team modify allblocks_red color red"
        );
    }

    private static void clearScoreboardMembers(
            MinecraftServer server
    ) {
        setupScoreboardTeams(server);

        runCommand(
                server,
                "team empty allblocks_blue"
        );
        runCommand(
                server,
                "team empty allblocks_red"
        );
    }

    private static boolean sameOnlineParticipants(
            MinecraftServer server
    ) {
        List<ServerPlayer> online =
                server.getPlayerList().getPlayers();

        if (online.size()
                != participantNames.size()) {
            return false;
        }

        for (ServerPlayer player : online) {
            if (!participantNames.containsKey(
                    player.getUUID())) {
                return false;
            }
        }

        return true;
    }

    private static boolean canControl(
            ServerPlayer player
    ) {
        if (player == null
                || controllerUuid == null
                || !controllerUuid.equals(
                        player.getUUID()
                )) {
            if (player != null) {
                player.sendSystemMessage(
                        Component.literal(
                                "[올블록 챌린지] 팀 편성자만 이 메뉴를 조작할 수 있습니다."
                        )
                );
            }

            return false;
        }

        return true;
    }

    private static MutableComponent clickable(
            String text,
            ChatFormatting color,
            String command
    ) {
        return Component.literal(text)
                .withStyle(style -> style
                        .withColor(color)
                        .withBold(true)
                        .withClickEvent(
                                new ClickEvent.RunCommand(
                                        command
                                )
                        )
                );
    }

    private static MutableComponent separator() {
        return Component.literal(
                "━━━━━━━━━━━━━━━━━━━━"
        ).withStyle(
                ChatFormatting.DARK_GRAY
        );
    }

    private static void broadcast(
            MinecraftServer server,
            Component component
    ) {
        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(component);
        }
    }

    private static void runCommand(
            MinecraftServer server,
            String command
    ) {
        if (server == null
                || command == null
                || command.isBlank()) {
            return;
        }

        try {
            CommandSourceStack source =
                    server.createCommandSourceStack()
                            .withSuppressedOutput();

            server.getCommands()
                    .performPrefixedCommand(
                            source,
                            command
                    );
        } catch (Exception ignored) {
        }
    }

    private enum SetupPhase {
        IDLE,
        REVEALING,
        READY,
        MANUAL,
        COUNTDOWN
    }
}
