package com.darkk0729.allblocks.event;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import com.darkk0729.allblocks.collection.TargetBlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class ChallengeEventManager {
    private static final int MAX_PROGRESS_TIER = 10;

    private ChallengeEventManager() {
    }

    public static void tick(MinecraftServer server) {
        if (!ChallengeManager.isRunning()) {
            return;
        }

        checkProgressEvents(server);
        DayRaidManager.tick(server);
    }

    private static void checkProgressEvents(MinecraftServer server) {
        int currentTier = getCurrentProgressTier();
        int lastTier = ChallengeManager.getLastProgressEventTier();

        // 사망 패널티 등으로 진행률이 떨어진 경우, 재달성 이벤트가 가능하도록 기준 tier도 낮춘다.
        if (currentTier < lastTier) {
            ChallengeManager.setLastProgressEventTier(currentTier);
            return;
        }

        if (currentTier <= lastTier) {
            return;
        }

        for (int tier = lastTier + 1; tier <= currentTier; tier++) {
            triggerProgressEvent(server, tier);
        }

        ChallengeManager.setLastProgressEventTier(currentTier);
    }

    private static int getCurrentProgressTier() {
        int tier = (int) Math.floor(ChallengeManager.getProgressPercent() / 1.0D);
        return Math.max(0, Math.min(MAX_PROGRESS_TIER, tier));
    }

    private static void triggerProgressEvent(MinecraftServer server, int tier) {
        int progressPercent = tier * 10;
        int eventType = ThreadLocalRandom.current().nextInt(3);

        switch (eventType) {
            case 0 -> triggerDebuffEvent(server, progressPercent);
            case 1 -> triggerRandomTeleportEvent(server, progressPercent);
            case 2 -> triggerRandomBlockReplaceEvent(server, progressPercent);
            default -> triggerDebuffEvent(server, progressPercent);
        }
    }

    private static void triggerDebuffEvent(MinecraftServer server, int progressPercent) {
        List<ServerPlayer> players = getPlayers(server);

        if (players.isEmpty()) {
            return;
        }

        int effectCount = getDebuffCount(progressPercent);
        int durationSeconds = progressPercent;

        for (ServerPlayer player : players) {
            List<DebuffType> debuffs = new ArrayList<>(List.of(DebuffType.values()));
            Collections.shuffle(debuffs, ThreadLocalRandom.current());

            for (int i = 0; i < effectCount && i < debuffs.size(); i++) {
                applyDebuff(player, debuffs.get(i), durationSeconds);
            }
        }

        broadcast(server, Component.literal(
                "[AllBlocks] Progress Event " + progressPercent + "%: Random debuff"
        ));
    }

    private static int getDebuffCount(int progressPercent) {
        if (progressPercent >= 50) {
            return 3;
        }

        if (progressPercent >= 30) {
            return 2;
        }

        return 1;
    }

    private static void applyDebuff(ServerPlayer player, DebuffType type, int durationSeconds) {
        int durationTicks = Math.max(1, durationSeconds) * 20;

        switch (type) {
            case SLOWNESS -> player.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS,
                    durationTicks,
                    0
            ));
            case BLINDNESS -> player.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    Math.min(durationTicks, 30 * 20),
                    0
            ));
            case HUNGER -> player.addEffect(new MobEffectInstance(
                    MobEffects.HUNGER,
                    durationTicks,
                    0
            ));
            case WEAKNESS -> player.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS,
                    durationTicks,
                    0
            ));
        }
    }

    private static void triggerRandomTeleportEvent(MinecraftServer server, int progressPercent) {
        List<ServerPlayer> players = getPlayers(server);

        if (players.isEmpty()) {
            return;
        }

        int radius = getTeleportEventRadius(progressPercent);

        for (ServerPlayer player : players) {
            teleportPlayerRandomly(player, radius);
        }

        broadcast(server, Component.literal(
                "[AllBlocks] Progress Event " + progressPercent + "%: Random teleport"
        ));
    }

    private static void teleportPlayerRandomly(ServerPlayer player, int radius) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos origin = player.blockPosition();

        for (int attempt = 0; attempt < 40; attempt++) {
            int dx = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int dy = ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int dz = ThreadLocalRandom.current().nextInt(-radius, radius + 1);

            BlockPos target = origin.offset(dx, dy, dz);

            if (!isBodySpaceEmpty(level, target)) {
                continue;
            }

            player.teleportTo(
                    target.getX() + 0.5D,
                    target.getY(),
                    target.getZ() + 0.5D
            );

            return;
        }

        player.sendSystemMessage(Component.literal(
                "[AllBlocks] Random teleport failed: no valid space found."
        ));
    }

    private static boolean isBodySpaceEmpty(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir();
    }

    private static void triggerRandomBlockReplaceEvent(MinecraftServer server, int progressPercent) {
        List<ServerPlayer> players = getPlayers(server);

        if (players.isEmpty()) {
            return;
        }

        Block fillBlock = TargetBlockRegistry.getRandomTargetBlock();

        if (fillBlock == null) {
            fillBlock = Blocks.OBSIDIAN;
        }

        int sideLength = getProgressEventSideLength(progressPercent);
        int radius = Math.max(1, sideLength / 2);

        for (ServerPlayer player : players) {
            fillBlocksAroundPlayer(player, radius, fillBlock, players);
        }

        String fillBlockName = BuiltInRegistries.BLOCK.getKey(fillBlock).toString();

        broadcast(server, Component.literal(
                "[AllBlocks] Progress Event " + progressPercent + "%: Area filled with " + fillBlockName
        ));
    }

    private static void fillBlocksAroundPlayer(
            ServerPlayer centerPlayer,
            int radius,
            Block fillBlock,
            List<ServerPlayer> protectedPlayers
    ) {
        if (!(centerPlayer.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos center = centerPlayer.blockPosition();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);

                    if (!canFillBlock(level, pos, protectedPlayers)) {
                        continue;
                    }

                    level.setBlock(pos, fillBlock.defaultBlockState(), 3);
                }
            }
        }
    }

    private static boolean canFillBlock(ServerLevel level, BlockPos pos, List<ServerPlayer> protectedPlayers) {
        if (isAnyPlayerBodySpace(pos, protectedPlayers)) {
            return false;
        }

        var state = level.getBlockState(pos);

        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();

        return !id.equals("minecraft:end_portal")
                && !id.equals("minecraft:end_portal_frame");
    }

    private static boolean isAnyPlayerBodySpace(BlockPos pos, List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            BlockPos playerFeet = player.blockPosition();
            BlockPos playerHead = playerFeet.above();

            if (pos.equals(playerFeet) || pos.equals(playerHead)) {
                return true;
            }
        }

        return false;
    }

    private static int getProgressEventSideLength(int progressPercent) {
        // 0.5n 기준
        // 10% = 5, 20% = 10, 30% = 15 ... 100% = 50
        return Math.max(1, progressPercent / 2);
    }

    private static int getTeleportEventRadius(int progressPercent) {
        // 텔레포트 범위: 5n 기준
        // 10% = 반경 50블록
        // 20% = 반경 100블록
        // 30% = 반경 150블록
        // ...
        // 100% = 반경 500블록
        return Math.max(1, progressPercent * 5);
    }

    private static List<ServerPlayer> getPlayers(MinecraftServer server) {
        return server.getPlayerList().getPlayers();
    }

    private static void broadcast(MinecraftServer server, Component message) {
        for (ServerPlayer player : getPlayers(server)) {
            player.sendSystemMessage(message);
        }
    }

    private enum DebuffType {
        SLOWNESS,
        BLINDNESS,
        HUNGER,
        WEAKNESS
    }
}