package com.darkk0729.allblocks.data;

import com.darkk0729.allblocks.AllBlocksMod;
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

            ChallengeState state = new ChallengeState();
            state.loadFrom(
                    data.running,
                    parseMode(data.mode),
                    Math.max(0L, data.elapsedTicks),
                    data.collectedBlocks == null ? new HashMap<>() : data.collectedBlocks
            );

            AllBlocksMod.LOGGER.info("Loaded AllBlocks challenge state from {}", path);
            return state;
        } catch (Exception e) {
            AllBlocksMod.LOGGER.error("Failed to load AllBlocks challenge state. A new state will be used.", e);
            return new ChallengeState();
        }
    }

    public static void save(MinecraftServer server, ChallengeState state) {
        Path path = getSavePath(server);

        try {
            Files.createDirectories(path.getParent());

            SaveData data = new SaveData();
            data.running = state.isRunning();
            data.mode = state.getMode().name();
            data.elapsedTicks = state.getElapsedTicks();
            data.currentDay = state.getCurrentDay();
            data.formattedTime = state.getFormattedElapsedTime();
            data.collectedBlocks = new HashMap<>(state.getCollectedBlocks());

            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            AllBlocksMod.LOGGER.error("Failed to save AllBlocks challenge state.", e);
        }
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

    private static final class SaveData {
        boolean running;
        String mode;
        long elapsedTicks;
        int currentDay;
        String formattedTime;
        Map<String, ChallengeState.CollectedBlockData> collectedBlocks;
    }
}