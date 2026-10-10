package com.darkk0729.allblocks.challenge;

import com.darkk0729.allblocks.command.ChallengeMenuMessages;
import com.darkk0729.allblocks.network.ClearMenuChatPayload;
import com.darkk0729.allblocks.network.CloseSetupScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class ChallengeSetupManager {
    private static final long TICKS_PER_MINUTE =
            ChallengeState.TICKS_PER_SECOND * 60L;

    private static final int DEFAULT_PROGRESS_EVENT_INTERVAL = 10;
    private static final int DEFAULT_DAY_RAID_INTERVAL = 10;
    private static final int MAX_MANUAL_TIME_VALUE = 10000;
    private static final int MAX_MANUAL_DAY_INTERVAL = 10000;

    private static final int COUNTDOWN_SECONDS = 5;
    private static final int COUNTDOWN_TICKS =
            (int) (
                    COUNTDOWN_SECONDS
                            * ChallengeState.TICKS_PER_SECOND
            );

    private static SetupPhase phase = SetupPhase.IDLE;
    private static UUID controllerUuid;
    private static ChallengeMode mode;

    private static ChallengeDifficulty difficulty =
            ChallengeDifficulty.NORMAL;

    private static ChallengeTimeLimitType timeLimitType =
            ChallengeTimeLimitType.IN_GAME_TIME;
    private static long timeLimitTicks =
            ChallengeState.TICKS_PER_DAY
                    * ChallengeState.MAX_DAYS;
    private static boolean timeLimitManual = false;

    private static int progressEventIntervalPercent =
            DEFAULT_PROGRESS_EVENT_INTERVAL;
    private static boolean progressEventManual = false;

    private static int dayRaidIntervalDays =
            DEFAULT_DAY_RAID_INTERVAL;
    private static boolean dayRaidManual = false;

    private static boolean resetWorldTime = false;

    private static int countdownTicksRemaining = 0;
    private static int lastCountdownSecond = -1;

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

        if (phase == SetupPhase.COUNTDOWN) {
            player.sendSystemMessage(
                    Component.literal(
                            "[올블록 챌린지] 게임 시작 카운트다운이 진행 중입니다."
                    ).withStyle(ChatFormatting.YELLOW)
            );
            return false;
        }

        if (isActive()
                && controllerUuid != null
                && !controllerUuid.equals(
                        player.getUUID()
                )) {
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
        resetSettings();

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
        phase = SetupPhase.SETTINGS;
        showSettings(player);
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
        if (!isSettingsController(player)
                || selectedDifficulty == null) {
            return false;
        }

        difficulty = selectedDifficulty;
        showSettings(player);
        return true;
    }

    public static boolean selectTimeLimitType(
            ServerPlayer player,
            ChallengeTimeLimitType selectedType
    ) {
        if (!isSettingsController(player)
                || selectedType == null) {
            return false;
        }

        timeLimitType = selectedType;
        timeLimitManual = false;

        switch (selectedType) {
            case IN_GAME_TIME ->
                    timeLimitTicks =
                            ChallengeState.TICKS_PER_DAY
                                    * ChallengeState.MAX_DAYS;

            case PLAY_TIME ->
                    timeLimitTicks =
                            TICKS_PER_MINUTE * 100L;

            case NONE ->
                    timeLimitTicks = 0L;
        }

        showSettings(player);
        return true;
    }

    public static boolean selectTimeLimitPreset(
            ServerPlayer player,
            int amount
    ) {
        if (!isSettingsController(player)
                || timeLimitType
                == ChallengeTimeLimitType.NONE
                || amount <= 0) {
            return false;
        }

        if (timeLimitType
                == ChallengeTimeLimitType.IN_GAME_TIME) {
            if (amount != 25
                    && amount != 50
                    && amount != 75
                    && amount != 100) {
                return false;
            }

            timeLimitTicks =
                    ChallengeState.TICKS_PER_DAY
                            * amount;
        } else {
            if (amount != 60
                    && amount != 100
                    && amount != 180
                    && amount != 360) {
                return false;
            }

            timeLimitTicks =
                    TICKS_PER_MINUTE
                            * amount;
        }

        timeLimitManual = false;
        showSettings(player);
        return true;
    }

    public static boolean selectTimeLimitManual(
            ServerPlayer player
    ) {
        if (!isSettingsController(player)
                || timeLimitType
                == ChallengeTimeLimitType.NONE) {
            return false;
        }

        if (timeLimitTicks <= 0L) {
            timeLimitTicks =
                    timeLimitType
                            == ChallengeTimeLimitType.IN_GAME_TIME
                            ? ChallengeState.TICKS_PER_DAY
                            * ChallengeState.MAX_DAYS
                            : TICKS_PER_MINUTE * 100L;
        }

        timeLimitManual = true;
        showSettings(player);
        return true;
    }

    public static boolean adjustTimeLimit(
            ServerPlayer player,
            int delta
    ) {
        if (!isSettingsController(player)
                || !timeLimitManual
                || timeLimitType
                == ChallengeTimeLimitType.NONE
                || delta == 0) {
            return false;
        }

        long unitTicks =
                timeLimitType
                        == ChallengeTimeLimitType.IN_GAME_TIME
                        ? ChallengeState.TICKS_PER_DAY
                        : TICKS_PER_MINUTE;

        long currentAmount =
                Math.max(
                        1L,
                        timeLimitTicks / unitTicks
                );

        long adjusted =
                Math.max(
                        1L,
                        Math.min(
                                MAX_MANUAL_TIME_VALUE,
                                currentAmount + delta
                        )
                );

        timeLimitTicks =
                unitTicks * adjusted;

        showSettings(player);
        return true;
    }

    public static boolean selectProgressEventInterval(
            ServerPlayer player,
            int intervalPercent
    ) {
        if (!isSettingsController(player)
                || difficulty == ChallengeDifficulty.EASY
                || (intervalPercent != 0
                && intervalPercent != 5
                && intervalPercent != 10
                && intervalPercent != 20)) {
            return false;
        }

        progressEventIntervalPercent =
                intervalPercent;
        progressEventManual = false;
        showSettings(player);
        return true;
    }

    public static boolean selectProgressEventManual(
            ServerPlayer player
    ) {
        if (!isSettingsController(player)
                || difficulty == ChallengeDifficulty.EASY) {
            return false;
        }

        if (progressEventIntervalPercent <= 0) {
            progressEventIntervalPercent =
                    DEFAULT_PROGRESS_EVENT_INTERVAL;
        }

        progressEventManual = true;
        showSettings(player);
        return true;
    }

    public static boolean adjustProgressEventInterval(
            ServerPlayer player,
            int delta
    ) {
        if (!isSettingsController(player)
                || difficulty == ChallengeDifficulty.EASY
                || !progressEventManual
                || delta == 0) {
            return false;
        }

        progressEventIntervalPercent =
                Math.max(
                        1,
                        Math.min(
                                100,
                                progressEventIntervalPercent
                                        + delta
                        )
                );

        showSettings(player);
        return true;
    }

    public static boolean selectDayRaidInterval(
            ServerPlayer player,
            int intervalDays
    ) {
        if (!isSettingsController(player)
                || difficulty == ChallengeDifficulty.EASY
                || (intervalDays != 0
                && intervalDays != 5
                && intervalDays != 10
                && intervalDays != 20)) {
            return false;
        }

        dayRaidIntervalDays = intervalDays;
        dayRaidManual = false;
        showSettings(player);
        return true;
    }

    public static boolean selectDayRaidManual(
            ServerPlayer player
    ) {
        if (!isSettingsController(player)
                || difficulty == ChallengeDifficulty.EASY) {
            return false;
        }

        if (dayRaidIntervalDays <= 0) {
            dayRaidIntervalDays =
                    DEFAULT_DAY_RAID_INTERVAL;
        }

        dayRaidManual = true;
        showSettings(player);
        return true;
    }

    public static boolean adjustDayRaidInterval(
            ServerPlayer player,
            int delta
    ) {
        if (!isSettingsController(player)
                || difficulty == ChallengeDifficulty.EASY
                || !dayRaidManual
                || delta == 0) {
            return false;
        }

        dayRaidIntervalDays =
                Math.max(
                        1,
                        Math.min(
                                MAX_MANUAL_DAY_INTERVAL,
                                dayRaidIntervalDays + delta
                        )
                );

        showSettings(player);
        return true;
    }

    public static boolean selectWorldTime(
            ServerPlayer player,
            boolean shouldResetWorldTime
    ) {
        if (!isSettingsController(player)) {
            return false;
        }

        resetWorldTime = shouldResetWorldTime;
        showSettings(player);
        return true;
    }

    public static boolean backFromSettings(
            ServerPlayer player
    ) {
        if (!isSettingsController(player)) {
            return false;
        }

        ChallengeMode previousMode = mode;
        mode = null;
        phase = SetupPhase.MODE;

        if (previousMode == ChallengeMode.TEAM_RACE
                || previousMode
                == ChallengeMode.BLOCK_RACE) {
            ChallengeMenuMessages.showRaceModeMenu(
                    player
            );
        } else {
            ChallengeMenuMessages.showModeMenu(
                    player
            );
        }

        return true;
    }

    public static boolean finishSettings(
            MinecraftServer server,
            ServerPlayer player
    ) {
        if (server == null
                || !isSettingsController(player)
                || mode == null) {
            return false;
        }

        ChallengeMode selectedMode = mode;
        ChallengeDifficulty selectedDifficulty =
                difficulty;
        ChallengeTimeLimitType selectedTimeLimitType =
                timeLimitType;
        long selectedTimeLimitTicks =
                timeLimitTicks;

        int selectedProgressEventInterval =
                selectedDifficulty
                        == ChallengeDifficulty.EASY
                        ? 0
                        : progressEventIntervalPercent;

        int selectedDayRaidInterval =
                selectedDifficulty
                        == ChallengeDifficulty.EASY
                        ? 0
                        : dayRaidIntervalDays;

        boolean selectedResetWorldTime =
                resetWorldTime;

        if (selectedMode == ChallengeMode.SOLO
                || selectedMode == ChallengeMode.CO_OP) {
            startCountdown(server);
            return true;
        }

        completeSetup();

        switch (selectedMode) {
            case SOLO, CO_OP -> {
                // Solo/Co-op are handled by the shared countdown above.
            }

            case BLOCK_RACE -> {
                boolean started =
                        BlockRaceSetupManager.begin(
                                server,
                                selectedDifficulty,
                                selectedTimeLimitType,
                                selectedTimeLimitTicks,
                                selectedProgressEventInterval,
                                selectedDayRaidInterval,
                                selectedResetWorldTime
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
                                selectedProgressEventInterval,
                                selectedDayRaidInterval,
                                selectedResetWorldTime
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

    public static void tick(
            MinecraftServer server
    ) {
        if (server == null
                || phase != SetupPhase.COUNTDOWN) {
            return;
        }

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

    private static void startCountdown(
            MinecraftServer server
    ) {
        phase = SetupPhase.COUNTDOWN;
        countdownTicksRemaining = COUNTDOWN_TICKS;
        lastCountdownSecond = COUNTDOWN_SECONDS;

        hideChatForCountdown(server);

        showCountdownTitle(
                server,
                COUNTDOWN_SECONDS
        );
    }

    private static void finishCountdown(
            MinecraftServer server
    ) {
        ChallengeMode finalMode = mode;
        ChallengeDifficulty finalDifficulty =
                difficulty;
        ChallengeTimeLimitType finalTimeLimitType =
                timeLimitType;
        long finalTimeLimitTicks =
                timeLimitTicks;

        int finalProgressEventInterval =
                finalDifficulty
                        == ChallengeDifficulty.EASY
                        ? 0
                        : progressEventIntervalPercent;

        int finalDayRaidInterval =
                finalDifficulty
                        == ChallengeDifficulty.EASY
                        ? 0
                        : dayRaidIntervalDays;

        boolean finalResetWorldTime =
                resetWorldTime;

        if (finalMode == ChallengeMode.SOLO) {
            ChallengeManager.startSingle(
                    server,
                    finalDifficulty,
                    finalTimeLimitType,
                    finalTimeLimitTicks,
                    finalProgressEventInterval,
                    finalDayRaidInterval,
                    finalResetWorldTime
            );
        } else if (finalMode == ChallengeMode.CO_OP) {
            ChallengeManager.startCoop(
                    server,
                    finalDifficulty,
                    finalTimeLimitType,
                    finalTimeLimitTicks,
                    finalProgressEventInterval,
                    finalDayRaidInterval,
                    finalResetWorldTime
            );
        } else {
            completeSetup();
            return;
        }

        showStartTitle(server);
        completeSetup();
    }

    private static void hideChatForCountdown(
            MinecraftServer server
    ) {
        if (server == null) {
            return;
        }

        for (ServerPlayer target :
                server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(
                    target,
                    new ClearMenuChatPayload(
                            true
                    )
            );

            ServerPlayNetworking.send(
                    target,
                    new CloseSetupScreenPayload(
                            true
                    )
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

        runCommand(
                server,
                "title @a times 0 20 0"
        );

        runCommand(
                server,
                "title @a title "
                        + "{\"text\":\""
                        + second
                        + "\",\"color\":\"gold\",\"bold\":true}"
        );
    }

    private static void showStartTitle(
            MinecraftServer server
    ) {
        playStartSound(server);

        runCommand(
                server,
                "title @a times 0 30 10"
        );

        runCommand(
                server,
                "title @a title "
                        + "{\"text\":\"시작!\",\"color\":\"green\",\"bold\":true}"
        );
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

        runCommand(
                server,
                "execute as @a at @s run playsound "
                        + "minecraft:block.note_block.pling "
                        + "master @s ~ ~ ~ 0.9 "
                        + pitch
        );
    }

    private static void playStartSound(
            MinecraftServer server
    ) {
        runCommand(
                server,
                "execute as @a at @s run playsound "
                        + "minecraft:block.note_block.pling "
                        + "master @s ~ ~ ~ 1.0 1.65"
        );
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

    public static void reset() {
        completeSetup();
    }

    private static boolean isSettingsController(
            ServerPlayer player
    ) {
        return canControl(player)
                && phase == SetupPhase.SETTINGS
                && mode != null;
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
                                "[올블록 챌린지] 챌린지 설정을 시작한 플레이어만 이 메뉴를 조작할 수 있습니다."
                        ).withStyle(ChatFormatting.RED)
                );
            }
            return false;
        }

        return true;
    }

    private static void showSettings(
            ServerPlayer player
    ) {
        ChallengeMenuMessages.showGameSettings(
                player,
                difficulty,
                timeLimitType,
                timeLimitTicks,
                timeLimitManual,
                progressEventIntervalPercent,
                progressEventManual,
                dayRaidIntervalDays,
                dayRaidManual,
                resetWorldTime
        );
    }

    private static void resetSettings() {
        difficulty = ChallengeDifficulty.NORMAL;
        timeLimitType =
                ChallengeTimeLimitType.IN_GAME_TIME;
        timeLimitTicks =
                ChallengeState.TICKS_PER_DAY
                        * ChallengeState.MAX_DAYS;
        timeLimitManual = false;

        progressEventIntervalPercent =
                DEFAULT_PROGRESS_EVENT_INTERVAL;
        progressEventManual = false;

        dayRaidIntervalDays =
                DEFAULT_DAY_RAID_INTERVAL;
        dayRaidManual = false;

        resetWorldTime = false;
    }

    private static void completeSetup() {
        phase = SetupPhase.IDLE;
        controllerUuid = null;
        mode = null;
        countdownTicksRemaining = 0;
        lastCountdownSecond = -1;
        resetSettings();
    }

    private enum SetupPhase {
        IDLE,
        MODE,
        SETTINGS,
        COUNTDOWN
    }
}
