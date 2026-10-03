package com.darkk0729.allblocks.challenge;

import com.darkk0729.allblocks.command.ChallengeMenuMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChallengeSetupManager {
    private static final long TICKS_PER_MINUTE = 20L * 60L;
    private static final Pattern TIME_PATTERN =
            Pattern.compile("^(\\d+)\\s*(분|일)$");

    private static SetupPhase phase = SetupPhase.IDLE;
    private static UUID controllerUuid;
    private static ChallengeMode mode;
    private static ChallengeDifficulty difficulty = ChallengeDifficulty.NORMAL;
    private static ChallengeTimeLimitType timeLimitType =
            ChallengeTimeLimitType.IN_GAME_TIME;
    private static long timeLimitTicks =
            ChallengeState.TICKS_PER_DAY * ChallengeState.MAX_DAYS;

    private ChallengeSetupManager() {
    }

    public static boolean isActive() {
        return phase != SetupPhase.IDLE;
    }

    public static boolean begin(
            MinecraftServer server,
            ServerPlayer player
    ) {
        if (server == null || player == null) {
            return false;
        }

        if (ChallengeManager.isRunning()
                || TeamRaceSetupManager.isActive()
                || BlockRaceSetupManager.isActive()) {
            player.sendSystemMessage(
                    Component.literal(
                            "[올블록 챌린지] 이미 챌린지가 진행 중이거나 시작 절차가 진행 중입니다."
                    ).withStyle(ChatFormatting.RED)
            );
            return false;
        }

        if (ChallengeManager.isFinished()) {
            ChallengeManager.stop(server);
        }

        if (isActive()
                && controllerUuid != null
                && !controllerUuid.equals(player.getUUID())) {
            player.sendSystemMessage(
                    Component.literal(
                            "[올블록 챌린지] 다른 플레이어가 챌린지 설정을 진행 중입니다."
                    ).withStyle(ChatFormatting.RED)
            );
            return false;
        }

        controllerUuid = player.getUUID();
        phase = SetupPhase.MODE;
        mode = null;
        difficulty = ChallengeDifficulty.NORMAL;
        timeLimitType = ChallengeTimeLimitType.IN_GAME_TIME;
        timeLimitTicks =
                ChallengeState.TICKS_PER_DAY
                        * ChallengeState.MAX_DAYS;

        ChallengeMenuMessages.showModeMenu(player);
        return true;
    }

    public static boolean selectMode(
            ServerPlayer player,
            ChallengeMode selectedMode
    ) {
        if (!canControl(player)
                || selectedMode == null) {
            return false;
        }

        mode = selectedMode;
        phase = SetupPhase.DIFFICULTY;

        switch (selectedMode) {
            case SOLO ->
                    ChallengeMenuMessages.showSingleDifficultyMenu(player);
            case CO_OP ->
                    ChallengeMenuMessages.showCoopDifficultyMenu(player);
            case TEAM_RACE ->
                    ChallengeMenuMessages.showTeamRaceDifficultyMenu(player);
            case BLOCK_RACE ->
                    ChallengeMenuMessages.showBlockRaceDifficultyMenu(player);
        }

        return true;
    }

    public static boolean showRaceModeMenu(
            ServerPlayer player
    ) {
        if (!canControl(player)) {
            return false;
        }

        mode = null;
        phase = SetupPhase.MODE;
        ChallengeMenuMessages.showRaceModeMenu(player);
        return true;
    }

    public static boolean selectDifficulty(
            ServerPlayer player,
            ChallengeDifficulty selectedDifficulty
    ) {
        if (!canControl(player)
                || mode == null
                || selectedDifficulty == null) {
            return false;
        }

        difficulty = selectedDifficulty;
        phase = SetupPhase.TIME_LIMIT;
        ChallengeMenuMessages.showTimeLimitMenu(player);
        return true;
    }

    public static boolean useDefaultTimeLimit(
            ServerPlayer player
    ) {
        if (!canControl(player)
                || phase != SetupPhase.TIME_LIMIT) {
            return false;
        }

        timeLimitType = ChallengeTimeLimitType.IN_GAME_TIME;
        timeLimitTicks =
                ChallengeState.TICKS_PER_DAY
                        * ChallengeState.MAX_DAYS;

        phase = SetupPhase.WORLD_TIME;
        ChallengeMenuMessages.showWorldTimeMenu(player);
        return true;
    }

    public static boolean handleChatInput(
            MinecraftServer server,
            ServerPlayer player,
            String rawMessage
    ) {
        if (server == null
                || player == null
                || phase != SetupPhase.TIME_LIMIT
                || controllerUuid == null
                || !controllerUuid.equals(player.getUUID())) {
            return false;
        }

        String message = rawMessage == null
                ? ""
                : rawMessage.trim();

        if (message.equals("없음")) {
            timeLimitType = ChallengeTimeLimitType.NONE;
            timeLimitTicks = 0L;
            phase = SetupPhase.WORLD_TIME;
            ChallengeMenuMessages.showWorldTimeMenu(player);
            return true;
        }

        Matcher matcher = TIME_PATTERN.matcher(message);

        if (!matcher.matches()) {
            sendInvalidTimeMessage(player);
            return true;
        }

        long amount;

        try {
            amount = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            sendInvalidTimeMessage(player);
            return true;
        }

        if (amount <= 0L) {
            sendInvalidTimeMessage(player);
            return true;
        }

        String unit = matcher.group(2);

        try {
            if ("분".equals(unit)) {
                timeLimitType = ChallengeTimeLimitType.PLAY_TIME;
                timeLimitTicks =
                        Math.multiplyExact(
                                amount,
                                TICKS_PER_MINUTE
                        );
            } else {
                timeLimitType = ChallengeTimeLimitType.IN_GAME_TIME;
                timeLimitTicks =
                        Math.multiplyExact(
                                amount,
                                ChallengeState.TICKS_PER_DAY
                        );
            }
        } catch (ArithmeticException e) {
            player.sendSystemMessage(
                    Component.literal(
                            "[올블록 챌린지] 입력한 시간이 너무 큽니다."
                    ).withStyle(ChatFormatting.RED)
            );
            return true;
        }

        phase = SetupPhase.WORLD_TIME;
        ChallengeMenuMessages.showWorldTimeMenu(player);
        return true;
    }

    public static boolean selectWorldTime(
            MinecraftServer server,
            ServerPlayer player,
            boolean resetWorldTime
    ) {
        if (server == null
                || !canControl(player)
                || phase != SetupPhase.WORLD_TIME
                || mode == null) {
            return false;
        }

        ChallengeMode selectedMode = mode;
        ChallengeDifficulty selectedDifficulty = difficulty;
        ChallengeTimeLimitType selectedTimeLimitType =
                timeLimitType;
        long selectedTimeLimitTicks = timeLimitTicks;

        completeSetup();

        switch (selectedMode) {
            case SOLO -> ChallengeManager.startSingle(
                    server,
                    selectedDifficulty,
                    selectedTimeLimitType,
                    selectedTimeLimitTicks,
                    resetWorldTime
            );

            case CO_OP -> ChallengeManager.startCoop(
                    server,
                    selectedDifficulty,
                    selectedTimeLimitType,
                    selectedTimeLimitTicks,
                    resetWorldTime
            );

            case BLOCK_RACE -> {
                boolean started =
                        BlockRaceSetupManager.begin(
                                server,
                                selectedDifficulty,
                                selectedTimeLimitType,
                                selectedTimeLimitTicks,
                                resetWorldTime
                        );

                if (!started) {
                    player.sendSystemMessage(
                            Component.literal(
                                    "[올블록 챌린지] 블록 레이스를 시작할 수 없습니다."
                            ).withStyle(ChatFormatting.RED)
                    );
                }
            }

            case TEAM_RACE -> {
                boolean started =
                        TeamRaceSetupManager.begin(
                                server,
                                player,
                                selectedDifficulty,
                                selectedTimeLimitType,
                                selectedTimeLimitTicks,
                                resetWorldTime
                        );

                if (!started) {
                    player.sendSystemMessage(
                            Component.literal(
                                    "[올블록 챌린지] 팀 레이스 설정을 시작할 수 없습니다."
                            ).withStyle(ChatFormatting.RED)
                    );
                }
            }
        }

        return true;
    }

    public static void reset() {
        completeSetup();
    }

    private static boolean canControl(
            ServerPlayer player
    ) {
        if (player == null
                || controllerUuid == null
                || !controllerUuid.equals(player.getUUID())) {
            if (player != null) {
                player.sendSystemMessage(
                        Component.literal(
                                "[올블록 챌린지] 챌린지 설정을 시작한 플레이어만 이 메뉴를 조작할 수 있습니다."
                        ).withStyle(ChatFormatting.RED)
                );
            }
            return false;
        }

        return true;
    }

    private static void sendInvalidTimeMessage(
            ServerPlayer player
    ) {
        player.sendSystemMessage(
                Component.literal(
                        "[올블록 챌린지] 시간 설정을 확인할 수 없습니다. '120분', '100일', '없음'과 같이 입력해주세요."
                ).withStyle(ChatFormatting.RED)
        );
    }

    private static void completeSetup() {
        phase = SetupPhase.IDLE;
        controllerUuid = null;
        mode = null;
        difficulty = ChallengeDifficulty.NORMAL;
        timeLimitType = ChallengeTimeLimitType.IN_GAME_TIME;
        timeLimitTicks =
                ChallengeState.TICKS_PER_DAY
                        * ChallengeState.MAX_DAYS;
    }

    private enum SetupPhase {
        IDLE,
        MODE,
        DIFFICULTY,
        TIME_LIMIT,
        WORLD_TIME
    }
}
