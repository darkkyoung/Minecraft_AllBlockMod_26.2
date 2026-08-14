package com.darkk0729.allblocks.data;

import com.darkk0729.allblocks.AllBlocksMod;
import com.darkk0729.allblocks.challenge.ChallengeDifficulty;
import com.darkk0729.allblocks.challenge.ChallengeMode;
import com.darkk0729.allblocks.challenge.ChallengeState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

public final class AllBlocksSaveManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SAVE_FILE_NAME = "allblocks_state.json";

    private AllBlocksSaveManager() {
    }

    public static ChallengeState load(MinecraftServer server) {
        Path path = getSavePath(server);

        if (!Files.exists(path)) {
            AllBlocksMod.LOGGER.info("No AllBlocks save file found. Creating new challenge state.");
            return new ChallengeState();
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            SaveData data = GSON.fromJson(reader, SaveData.class);

            if (data == null) {
                return new ChallengeState();
            }

            long currentWorldTime = getCurrentWorldTime(server);

            long savedElapsedTicks = Math.max(0L, data.elapsedTicks);

            long savedWorldElapsedTicks = data.worldElapsedTicks == null
                    ? 0L
                    : Math.max(0L, data.worldElapsedTicks);

            long startWorldTime = data.startWorldTime == null
                    ? Math.max(0L, currentWorldTime - savedWorldElapsedTicks)
                    : Math.max(0L, data.startWorldTime);

            ChallengeState state = new ChallengeState();
            state.loadFrom(
                    data.running,
                    data.finished,
                    parseMode(data.mode),
                    parseDifficulty(data.difficulty),
                    savedElapsedTicks,
                    startWorldTime,
                    savedWorldElapsedTicks,
                    parseResult(data.result),
                    Math.max(0, data.lastProgressEventTier),
                    Math.max(0, data.lastDayRaidEventDay),
                    data.collectedBlocks == null
                            ? new HashMap<>()
                            : data.collectedBlocks,
                    data.participants == null
                            ? new LinkedHashMap<>()
                            : data.participants
            );

            if (state.isRunning()) {
                state.syncWorldTime(currentWorldTime);
            }

            AllBlocksMod.LOGGER.info("Loaded AllBlocks challenge state from {}", path);
            return state;
        } catch (Exception e) {
            AllBlocksMod.LOGGER.error("Failed to load AllBlocks challenge state. A new state will be used.", e);
            return new ChallengeState();
        }
    }

    public static void save(MinecraftServer server, ChallengeState state) {
        if (server == null || state == null) {
            return;
        }

        Path path = getSavePath(server);

        try {
            Files.createDirectories(path.getParent());

            SaveData data = new SaveData();
            data.running = state.isRunning();
            data.finished = state.isFinished();
            data.result = state.getResult().name();
            data.mode = state.getMode().name();
            data.difficulty = state.getDifficulty().name();

            data.elapsedTicks = state.getElapsedTicks();
            data.startWorldTime = state.getStartWorldTime();
            data.worldElapsedTicks = state.getWorldElapsedTicks();

            data.currentDay = state.getCurrentDay();
            data.formattedTime = state.getFormattedElapsedTime();
            data.lastProgressEventTier = state.getLastProgressEventTier();
            data.lastDayRaidEventDay = state.getLastDayRaidEventDay();
            data.collectedBlocks = new HashMap<>(state.getCollectedBlocks());

            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            AllBlocksMod.LOGGER.error("Failed to save AllBlocks challenge state.", e);
        }
    }

    private static long getCurrentWorldTime(MinecraftServer server) {
        if (server == null || server.overworld() == null) {
            return 0L;
        }

        return Math.max(0L, server.overworld().getOverworldClockTime());
    }

    private static Path getSavePath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve(SAVE_FILE_NAME);
    }

    private static ChallengeMode parseMode(String modeName) {
        if (modeName == null || modeName.isBlank()) {
            return ChallengeMode.SOLO;
        }

        try {
            return ChallengeMode.valueOf(modeName);
        } catch (IllegalArgumentException e) {
            return ChallengeMode.SOLO;
        }
    }

    private static ChallengeDifficulty parseDifficulty(String difficultyName) {
        if (difficultyName == null || difficultyName.isBlank()) {
            return ChallengeDifficulty.HARD;
        }

        try {
            return ChallengeDifficulty.valueOf(difficultyName);
        } catch (IllegalArgumentException e) {
            return ChallengeDifficulty.HARD;
        }
    }

    private static ChallengeState.ChallengeResult parseResult(String resultName) {
        if (resultName == null || resultName.isBlank()) {
            return ChallengeState.ChallengeResult.NONE;
        }

        try {
            return ChallengeState.ChallengeResult.valueOf(resultName);
        } catch (IllegalArgumentException e) {
            return ChallengeState.ChallengeResult.NONE;
        }
    }

    private static final class SaveData {
        boolean running;
        boolean finished;
        String result;
        String mode;
        String difficulty;

        long elapsedTicks;
        Long startWorldTime;
        Long worldElapsedTicks;

        int currentDay;
        String formattedTime;
        int lastProgressEventTier;
        int lastDayRaidEventDay;
        Map<String, ChallengeState.CollectedBlockData> collectedBlocks;
        Map<String, ChallengeState.ParticipantData> participants;
    }
}