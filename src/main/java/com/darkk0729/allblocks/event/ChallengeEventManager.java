package com.darkk0729.allblocks.event;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import com.darkk0729.allblocks.collection.TargetBlockRegistry;
import net.minecraft.ChatFormatting;
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
import com.darkk0729.allblocks.challenge.ChallengeDifficulty;
import com.darkk0729.allblocks.challenge.ChallengeMode;
import com.darkk0729.allblocks.challenge.TeamRaceTeam;

public final class ChallengeEventManager {
    private static final int MAX_PROGRESS_TIER = 100;

    private ChallengeEventManager() {
    }

    public static void startDebugProgressEvent(MinecraftServer server, int progressPercent) {
        if (server == null) {
            return;
        }

        if (!ChallengeManager.isRunning()) {
            broadcast(server, Component.literal("[올블록 디버그] 먼저 챌린지를 시작해주세요."));
            return;
        }

        if (progressPercent < 10 || progressPercent > 100 || progressPercent % 10 != 0) {
            broadcast(server, Component.literal("[올블록 디버그] 진행률은 10, 20, 30, ..., 100 중 하나여야 합니다."));
            return;
        }

        broadcast(
                server,
                Component.literal(
                        "[올블록 디버그] 진행률 "
                                + progressPercent
                                + "% 이벤트를 실행합니다."
                )
        );

        triggerProgressEvent(
                server,
                progressPercent
        );
    }

    public static void tick(MinecraftServer server) {
        if (!ChallengeManager.isRunning()) {
            return;
        }

        if (ChallengeManager.getMode()
                == ChallengeMode.BLOCK_RACE) {
            checkBlockRaceProgressEvents(server);
            return;
        }

        if (ChallengeManager.getMode()
                == ChallengeMode.TEAM_RACE) {
            checkTeamRaceProgressEvents(server);
            return;
        }

        checkProgressEvents(server);
    }

    private static void checkBlockRaceProgressEvents(
            MinecraftServer server
    ) {
        boolean tierChanged = false;
        int total =
                Math.max(
                        1,
                        ChallengeManager.getTotalTargetCount()
                );
        int interval =
                ChallengeManager
                        .getProgressEventIntervalPercent();

        if (interval <= 0) {
            return;
        }

        for (ServerPlayer player : getPlayers(server)) {
            String playerUuid =
                    player.getUUID().toString();

            if (!ChallengeManager
                    .getParticipants()
                    .containsKey(playerUuid)) {
                continue;
            }

            int collected =
                    ChallengeManager.getPlayerBlockCount(
                            playerUuid
                    );

            int currentPercent =
                    Math.max(
                            0,
                            Math.min(
                                    100,
                                    (int) Math.floor(
                                            collected
                                                    * 100.0D
                                                    / total
                                    )
                            )
                    );

            int currentTier =
                    Math.max(
                            0,
                            Math.min(
                                    MAX_PROGRESS_TIER,
                                    currentPercent
                                            / interval
                            )
                    );

            int lastTier =
                    ChallengeManager
                            .getParticipantLastProgressEventTier(
                                    playerUuid
                            );

            if (currentTier < lastTier) {
                ChallengeManager
                        .setParticipantLastProgressEventTier(
                                playerUuid,
                                currentTier
                        );

                tierChanged = true;
                continue;
            }

            if (currentTier <= lastTier) {
                continue;
            }

            for (int tier = lastTier + 1;
                 tier <= currentTier;
                 tier++) {
                triggerBlockRaceProgressEvent(
                        server,
                        player,
                        Math.min(
                                100,
                                tier * interval
                        )
                );
            }

            ChallengeManager
                    .setParticipantLastProgressEventTier(
                            playerUuid,
                            currentTier
                    );

            tierChanged = true;
        }

        if (tierChanged) {
            ChallengeManager.save(server);
        }
    }

    private static void triggerBlockRaceProgressEvent(
            MinecraftServer server,
            ServerPlayer player,
            int progressPercent
    ) {
        if (player == null) {
            return;
        }

        int eventType =
                ThreadLocalRandom.current().nextInt(3);

        switch (eventType) {
            case 0 -> {
                applyRandomDebuffsToPlayer(
                        player,
                        progressPercent
                );

                player.sendSystemMessage(
                        progressEventMessage(
                                progressPercent,
                                "랜덤 디버프 발동"
                        )
                );
            }

            case 1 -> {
                teleportPlayerRandomly(
                        player,
                        getTeleportEventRadius(
                                progressPercent
                        )
                );

                player.sendSystemMessage(
                        progressEventMessage(
                                progressPercent,
                                "랜덤 텔레포트 발동"
                        )
                );
            }

            case 2 -> {
                Block fillBlock =
                        TargetBlockRegistry
                                .getRandomFillEventBlock();

                if (fillBlock == null) {
                    fillBlock = Blocks.OBSIDIAN;
                }

                int sideLength =
                        getProgressEventSideLength(
                                progressPercent
                        );

                int radius =
                        Math.max(
                                1,
                                sideLength / 2
                        );

                fillBlocksAroundPlayer(
                        player,
                        radius,
                        fillBlock,
                        getPlayers(server)
                );

                String fillBlockName =
                        BuiltInRegistries.BLOCK
                                .getKey(fillBlock)
                                .toString();

                player.sendSystemMessage(
                        progressEventMessage(
                                progressPercent,
                                "랜덤 블록 가두기 발동"
                        )
                );
            }

            default -> {
            }
        }
    }

    private static void checkTeamRaceProgressEvents(
            MinecraftServer server
    ) {
        int interval =
                ChallengeManager
                        .getProgressEventIntervalPercent();

        if (interval <= 0) {
            return;
        }

        boolean tierChanged = false;
        int total =
                Math.max(
                        1,
                        ChallengeManager
                                .getTotalTargetCount()
                );

        for (TeamRaceTeam team :
                List.of(
                        TeamRaceTeam.BLUE,
                        TeamRaceTeam.RED
                )) {
            int collected =
                    ChallengeManager
                            .getTeamBlockCount(team);

            int currentPercent =
                    Math.max(
                            0,
                            Math.min(
                                    100,
                                    (int) Math.floor(
                                            collected
                                                    * 100.0D
                                                    / total
                                    )
                            )
                    );

            int currentTier =
                    Math.max(
                            0,
                            Math.min(
                                    MAX_PROGRESS_TIER,
                                    currentPercent
                                            / interval
                            )
                    );

            int lastTier =
                    ChallengeManager
                            .getTeamLastProgressEventTier(
                                    team
                            );

            if (currentTier < lastTier) {
                ChallengeManager
                        .setTeamLastProgressEventTier(
                                team,
                                currentTier
                        );
                tierChanged = true;
                continue;
            }

            if (currentTier <= lastTier) {
                continue;
            }

            for (int tier = lastTier + 1;
                 tier <= currentTier;
                 tier++) {
                triggerTeamRaceProgressEvent(
                        server,
                        team,
                        Math.min(
                                100,
                                tier * interval
                        )
                );
            }

            ChallengeManager
                    .setTeamLastProgressEventTier(
                            team,
                            currentTier
                    );
            tierChanged = true;
        }

        if (tierChanged) {
            ChallengeManager.save(server);
        }
    }

    private static void triggerTeamRaceProgressEvent(
            MinecraftServer server,
            TeamRaceTeam team,
            int progressPercent
    ) {
        List<ServerPlayer> teamPlayers =
                getTeamPlayers(
                        server,
                        team
                );

        if (teamPlayers.isEmpty()) {
            return;
        }

        int eventType =
                ThreadLocalRandom.current()
                        .nextInt(3);

        switch (eventType) {
            case 0 -> {
                for (ServerPlayer player :
                        teamPlayers) {
                    applyRandomDebuffsToPlayer(
                            player,
                            progressPercent
                    );
                }

                sendToPlayers(
                        teamPlayers,
                        progressEventMessage(
                                progressPercent,
                                "랜덤 디버프 발동"
                        )
                );
            }

            case 1 -> {
                int radius =
                        getTeleportEventRadius(
                                progressPercent
                        );

                for (ServerPlayer player :
                        teamPlayers) {
                    teleportPlayerRandomly(
                            player,
                            radius
                    );
                }

                sendToPlayers(
                        teamPlayers,
                        progressEventMessage(
                                progressPercent,
                                "랜덤 텔레포트 발동"
                        )
                );
            }

            case 2 -> {
                Block fillBlock =
                        TargetBlockRegistry
                                .getRandomFillEventBlock();

                if (fillBlock == null) {
                    fillBlock = Blocks.OBSIDIAN;
                }

                int sideLength =
                        getProgressEventSideLength(
                                progressPercent
                        );

                int radius =
                        Math.max(
                                1,
                                sideLength / 2
                        );

                List<ServerPlayer> protectedPlayers =
                        getPlayers(server);

                for (ServerPlayer player :
                        teamPlayers) {
                    fillBlocksAroundPlayer(
                            player,
                            radius,
                            fillBlock,
                            protectedPlayers
                    );
                }

                sendToPlayers(
                        teamPlayers,
                        progressEventMessage(
                                progressPercent,
                                "랜덤 블록 가두기 발동"
                        )
                );
            }

            default -> {
            }
        }
    }

    private static void checkProgressEvents(MinecraftServer server) {
        int interval =
                ChallengeManager
                        .getProgressEventIntervalPercent();

        if (interval <= 0) {
            return;
        }

        int currentTier =
                getCurrentProgressTier(
                        interval
                );
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
            triggerProgressEvent(
                    server,
                    Math.min(
                            100,
                            tier * interval
                    )
            );
        }

        ChallengeManager.setLastProgressEventTier(currentTier);
    }

    private static int getCurrentProgressTier(
            int interval
    ) {
        if (interval <= 0) {
            return 0;
        }

        int progressPercent =
                Math.max(
                        0,
                        Math.min(
                                100,
                                (int) Math.floor(
                                        ChallengeManager
                                                .getProgressPercent()
                                )
                        )
                );

        int tier =
                progressPercent
                        / interval;

        return Math.max(
                0,
                Math.min(
                        MAX_PROGRESS_TIER,
                        tier
                )
        );
    }

    private static void triggerProgressEvent(
            MinecraftServer server,
            int progressPercent
    ) {

        if (ChallengeManager.getMode() == ChallengeMode.CO_OP) {
            triggerCoopProgressEvent(server, progressPercent);
            return;
        }

        int eventType = ThreadLocalRandom.current().nextInt(3);

        switch (eventType) {
            case 0 -> triggerDebuffEvent(server, progressPercent);
            case 1 -> triggerRandomTeleportEvent(server, progressPercent);
            case 2 -> triggerRandomBlockReplaceEvent(server, progressPercent);
            default -> triggerDebuffEvent(server, progressPercent);
        }
    }

    private static void triggerCoopProgressEvent(MinecraftServer server, int progressPercent) {
        List<ServerPlayer> players = getPlayers(server);

        if (players.isEmpty()) {
            return;
        }

        for (ServerPlayer player : players) {
            int eventType = ThreadLocalRandom.current().nextInt(3);

            switch (eventType) {
                case 0 -> {
                    applyRandomDebuffsToPlayer(player, progressPercent);

                    player.sendSystemMessage(
                            progressEventMessage(
                                    progressPercent,
                                    "랜덤 디버프 발동"
                            )
                    );
                }

                case 1 -> {
                    teleportPlayerRandomly(
                            player,
                            getTeleportEventRadius(progressPercent)
                    );

                    player.sendSystemMessage(
                            progressEventMessage(
                                    progressPercent,
                                    "랜덤 텔레포트 발동"
                            )
                    );
                }

                case 2 -> {
                    Block fillBlock =
                            TargetBlockRegistry.getRandomFillEventBlock();

                    if (fillBlock == null) {
                        fillBlock = Blocks.OBSIDIAN;
                    }

                    int sideLength =
                            getProgressEventSideLength(progressPercent);

                    int radius =
                            Math.max(1, sideLength / 2);

                    fillBlocksAroundPlayer(
                            player,
                            radius,
                            fillBlock,
                            players
                    );

                    String fillBlockName =
                            BuiltInRegistries.BLOCK
                                    .getKey(fillBlock)
                                    .toString();

                    player.sendSystemMessage(
                            progressEventMessage(
                                    progressPercent,
                                    "랜덤 블록 가두기 발동"
                            )
                    );
                }

                default -> {
                }
            }
        }
    }

    private static void triggerDebuffEvent(MinecraftServer server, int progressPercent) {
        List<ServerPlayer> players = getPlayers(server);

        if (players.isEmpty()) {
            return;
        }

        for (ServerPlayer player : players) {
            applyRandomDebuffsToPlayer(player, progressPercent);
        }

        broadcast(
                server,
                progressEventMessage(
                        progressPercent,
                        "랜덤 디버프 발동"
                )
        );
    }

    private static void applyRandomDebuffsToPlayer(
            ServerPlayer player,
            int progressPercent
    ) {
        int effectCount = getDebuffCount(progressPercent);
        int durationSeconds = getDebuffDurationSeconds(progressPercent);

        List<DebuffType> debuffs =
                new ArrayList<>(getAvailableDebuffs());

        Collections.shuffle(
                debuffs,
                ThreadLocalRandom.current()
        );

        for (int i = 0;
             i < effectCount && i < debuffs.size();
             i++) {

            applyDebuff(
                    player,
                    debuffs.get(i),
                    durationSeconds
            );
        }
    }

    private static List<DebuffType> getAvailableDebuffs() {
        if (ChallengeManager.getDifficulty()
                == ChallengeDifficulty.NORMAL) {

            return List.of(
                    DebuffType.SLOWNESS,
                    DebuffType.BLINDNESS,
                    DebuffType.HUNGER,
                    DebuffType.WEAKNESS,
                    DebuffType.MINING_FATIGUE,
                    DebuffType.POISON
            );
        }

        // HARD
        return List.of(DebuffType.values());
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

    private static int getDebuffDurationSeconds(int progressPercent) {
        // n%일 때 3n초.
        // 단, 전체 디버프 기본 지속시간은 최대 150초.
        return Math.min(progressPercent * 3, 150);
    }

    private static void applyDebuff(ServerPlayer player, DebuffType type, int durationSeconds) {
        int finalDurationSeconds = getFinalDebuffDurationSeconds(type, durationSeconds);
        int durationTicks = Math.max(1, finalDurationSeconds) * 20;

        switch (type) {
            case SLOWNESS -> player.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS,
                    durationTicks,
                    0
            ));
            case BLINDNESS -> player.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    durationTicks,
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
            case MINING_FATIGUE -> player.addEffect(new MobEffectInstance(
                    MobEffects.MINING_FATIGUE,
                    durationTicks,
                    0
            ));
            case NAUSEA -> player.addEffect(new MobEffectInstance(
                    MobEffects.NAUSEA,
                    durationTicks,
                    0
            ));
            case POISON -> player.addEffect(new MobEffectInstance(
                    MobEffects.POISON,
                    durationTicks,
                    0
            ));
            case WITHER -> player.addEffect(new MobEffectInstance(
                    MobEffects.WITHER,
                    durationTicks,
                    0
            ));
        }
    }

    private static int getFinalDebuffDurationSeconds(DebuffType type, int durationSeconds) {
        int maxSeconds = type == DebuffType.NAUSEA ? 30 : 150;
        return Math.min(Math.max(1, durationSeconds), maxSeconds);
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

        broadcast(
                server,
                progressEventMessage(
                        progressPercent,
                        "랜덤 텔레포트 발동"
                )
        );
    }

    private static void teleportPlayerRandomly(
            ServerPlayer player,
            int radius
    ) {
        if (!(player.level()
                instanceof ServerLevel level)) {
            return;
        }

        BlockPos origin =
                player.blockPosition();

        int safeRadius =
                Math.max(
                        1,
                        radius
                );

        int minY =
                level.getMinY() + 1;

        int maxY =
                level.getMaxY() - 2;

        // 현재 플레이어가 점프 중인지, 낙하 중인지와 무관하게
        // 바닐라 randomTeleport의 착지/충돌 판정을 이용해 목적지를 찾는다.
        for (int attempt = 0;
             attempt < 128;
             attempt++) {
            int dx =
                    ThreadLocalRandom.current()
                            .nextInt(
                                    -safeRadius,
                                    safeRadius + 1
                            );

            int dy =
                    ThreadLocalRandom.current()
                            .nextInt(
                                    -safeRadius,
                                    safeRadius + 1
                            );

            int dz =
                    ThreadLocalRandom.current()
                            .nextInt(
                                    -safeRadius,
                                    safeRadius + 1
                            );

            if (dx == 0
                    && dz == 0) {
                continue;
            }

            int targetY =
                    Math.max(
                            minY,
                            Math.min(
                                    maxY,
                                    origin.getY() + dy
                            )
                    );

            boolean teleported =
                    player.randomTeleport(
                            origin.getX()
                                    + dx
                                    + 0.5D,
                            targetY,
                            origin.getZ()
                                    + dz
                                    + 0.5D,
                            false
                    );

            if (teleported) {
                return;
            }
        }

        // 극단적으로 랜덤 시도가 모두 막힌 경우,
        // 주변의 랜덤 X/Z 열에서 현재 높이에 가까운 안전한 공간을 직접 찾는다.
        for (int attempt = 0;
             attempt < 32;
             attempt++) {
            int dx =
                    ThreadLocalRandom.current()
                            .nextInt(
                                    -safeRadius,
                                    safeRadius + 1
                            );

            int dz =
                    ThreadLocalRandom.current()
                            .nextInt(
                                    -safeRadius,
                                    safeRadius + 1
                            );

            if (dx == 0
                    && dz == 0) {
                continue;
            }

            BlockPos target =
                    findNearestSafeTeleportSpace(
                            level,
                            origin.getX() + dx,
                            origin.getY(),
                            origin.getZ() + dz
                    );

            if (target == null) {
                continue;
            }

            player.teleportTo(
                    target.getX() + 0.5D,
                    target.getY(),
                    target.getZ() + 0.5D
            );

            return;
        }
    }

    private static BlockPos findNearestSafeTeleportSpace(
            ServerLevel level,
            int x,
            int originY,
            int z
    ) {
        int minY =
                level.getMinY() + 1;

        int maxY =
                level.getMaxY() - 2;

        int clampedOriginY =
                Math.max(
                        minY,
                        Math.min(
                                maxY,
                                originY
                        )
                );

        int maxOffset =
                Math.max(
                        clampedOriginY - minY,
                        maxY - clampedOriginY
                );

        for (int offset = 0;
             offset <= maxOffset;
             offset++) {
            int upY =
                    clampedOriginY
                            + offset;

            if (upY <= maxY) {
                BlockPos up =
                        new BlockPos(
                                x,
                                upY,
                                z
                        );

                if (isSafeTeleportSpace(
                        level,
                        up
                )) {
                    return up;
                }
            }

            if (offset == 0) {
                continue;
            }

            int downY =
                    clampedOriginY
                            - offset;

            if (downY >= minY) {
                BlockPos down =
                        new BlockPos(
                                x,
                                downY,
                                z
                        );

                if (isSafeTeleportSpace(
                        level,
                        down
                )) {
                    return down;
                }
            }
        }

        return null;
    }

    private static boolean isSafeTeleportSpace(
            ServerLevel level,
            BlockPos pos
    ) {
        return isBodySpaceEmpty(
                level,
                pos
        )
                && level.getBlockState(
                        pos.below()
                ).blocksMotion();
    }

    private static boolean isBodySpaceEmpty(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(
                        pos.above()
                ).isAir();
    }

    private static void triggerRandomBlockReplaceEvent(MinecraftServer server, int progressPercent) {
        List<ServerPlayer> players = getPlayers(server);

        if (players.isEmpty()) {
            return;
        }

        Block fillBlock = TargetBlockRegistry.getRandomFillEventBlock();

        if (fillBlock == null) {
            fillBlock = Blocks.OBSIDIAN;
        }

        int sideLength = getProgressEventSideLength(progressPercent);
        int radius = Math.max(1, sideLength / 2);

        for (ServerPlayer player : players) {
            fillBlocksAroundPlayer(player, radius, fillBlock, players);
        }

        String fillBlockName = BuiltInRegistries.BLOCK.getKey(fillBlock).toString();

        broadcast(
                server,
                progressEventMessage(
                        progressPercent,
                        "랜덤 블록 가두기 발동"
                )
        );
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

    private static int getTeleportEventRadius(
            int progressPercent
    ) {
        if (ChallengeManager.getDifficulty()
                == ChallengeDifficulty.NORMAL) {

            // NORMAL: n% × 2
            // 10% = 20
            // 20% = 40
            // ...
            // 100% = 200
            return Math.max(
                    1,
                    progressPercent * 2
            );
        }

        // HARD: n% × 5
        // 10% = 50
        // ...
        // 100% = 500
        return Math.max(
                1,
                progressPercent * 5
        );
    }

    private static List<ServerPlayer> getPlayers(
            MinecraftServer server
    ) {
        List<ServerPlayer> players =
                new ArrayList<>();

        if (server == null) {
            return players;
        }

        for (ServerPlayer player :
                server.getPlayerList().getPlayers()) {
            if (ChallengeManager.isActiveChallengePlayer(
                    player
            )) {
                players.add(player);
            }
        }

        return players;
    }

    private static List<ServerPlayer> getTeamPlayers(
            MinecraftServer server,
            TeamRaceTeam team
    ) {
        List<ServerPlayer> players =
                new ArrayList<>();

        if (server == null
                || team == null
                || !team.isAssigned()) {
            return players;
        }

        for (ServerPlayer player :
                getPlayers(server)) {
            TeamRaceTeam playerTeam =
                    ChallengeManager
                            .getParticipantTeam(
                                    player.getUUID()
                                            .toString()
                            );

            if (playerTeam == team) {
                players.add(player);
            }
        }

        return players;
    }

    private static void sendToPlayers(
            List<ServerPlayer> players,
            Component message
    ) {
        for (ServerPlayer player : players) {
            player.sendSystemMessage(message);
        }
    }

    private static Component progressEventMessage(
            int progressPercent,
            String eventText
    ) {
        return Component.literal(
                        progressPercent + "%"
                )
                .withStyle(
                        ChatFormatting.GREEN,
                        ChatFormatting.BOLD
                )
                .append(
                        Component.literal(
                                " 달성! "
                                        + eventText
                        ).withStyle(
                                ChatFormatting.WHITE
                        )
                );
    }

    private static void broadcast(
            MinecraftServer server,
            Component message
    ) {
        for (ServerPlayer player :
                getPlayers(server)) {
            player.sendSystemMessage(message);
        }
    }

    private enum DebuffType {
        SLOWNESS,
        BLINDNESS,
        HUNGER,
        WEAKNESS,
        MINING_FATIGUE,
        NAUSEA,
        POISON,
        WITHER
    }
}