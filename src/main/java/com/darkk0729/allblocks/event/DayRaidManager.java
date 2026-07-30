package com.darkk0729.allblocks.event;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DayRaidManager {
    private static final String PROGRESS_BOSSBAR_ID = "allblocks:progress";
    private static final int RAID_WARNING_TICKS = 20 * 5;

    private static final int DAY90_WITHER_TARGET_INTERVAL_TICKS = 60;
    private static final double DAY90_WITHER_PLAYER_PRIORITY_RANGE = 160.0D;

    public static boolean isRaidWarningActive() {
        return activeRaid != null;
    }

    private static ActiveRaid activeRaid;
    private static int raidPursuitTickCounter = 0;

    private static final Set<UUID> day90WitherIds = new HashSet<>();

    private DayRaidManager() {
    }

    public static void tick(MinecraftServer server) {
        if (!ChallengeManager.isRunning()) {
            activeRaid = null;
            raidPursuitTickCounter = 0;
            day90WitherIds.clear();
            return;
        }

        tickDay90WitherPlayerPriority(server);

        if (activeRaid != null) {
            tickActiveRaidWarning(server);
            return;
        }

        checkDayRaidStart(server);
    }

    public static void startDebugRaid(MinecraftServer server, int raidDay) {
        if (server == null) {
            return;
        }

        if (!ChallengeManager.isRunning()) {
            broadcast(server, Component.literal("[AllBlocks] Start the challenge first."));
            return;
        }

        if (raidDay < 10 || raidDay > 90 || raidDay % 10 != 0) {
            broadcast(server, Component.literal("[AllBlocks] Debug raid day must be 10, 20, 30, ..., 90."));
            return;
        }

        startRaidWarning(server, raidDay, true);
    }

    private static void checkDayRaidStart(MinecraftServer server) {
        int currentDay = ChallengeManager.getCurrentDay();

        if (currentDay < 10) {
            return;
        }

        int raidDay = (currentDay / 10) * 10;

        if (raidDay <= 0 || raidDay > 90) {
            return;
        }

        if (raidDay <= ChallengeManager.getLastDayRaidEventDay()) {
            return;
        }

        startRaidWarning(server, raidDay);
    }

    private static void startRaidWarning(MinecraftServer server, int raidDay) {
        startRaidWarning(server, raidDay, false);
    }

    private static void startRaidWarning(MinecraftServer server, int raidDay, boolean debugMode) {
        activeRaid = new ActiveRaid(raidDay, debugMode);

        setupRaidTeam(server);

        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " name {\"text\":\"" + raidDay + "일차 이벤트\",\"color\":\"red\",\"bold\":true}");
        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " color red");
        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " style progress");
        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " max 100");
        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " value 0");
        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " visible true");
        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " players @a");
        runCommand(server, "playsound minecraft:event.raid.horn master @a ~ ~ ~ 1 1");

        broadcast(server, Component.literal("[AllBlocks] Day " + raidDay + " Raid is coming..."));
    }

    private static void tickActiveRaidWarning(MinecraftServer server) {
        activeRaid.elapsedTicks++;

        int value = Math.min(100, (activeRaid.elapsedTicks * 100) / RAID_WARNING_TICKS);

        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " value " + value);

        if (activeRaid.elapsedTicks < RAID_WARNING_TICKS) {
            return;
        }

        int raidDay = activeRaid.raidDay;
        boolean debugMode = activeRaid.debugMode;
        activeRaid = null;

        runCommand(server, "bossbar set " + PROGRESS_BOSSBAR_ID + " value 100");

        runCommand(server, "title @a title {\"text\":\"Day " + raidDay + " Raid\",\"color\":\"red\",\"bold\":true}");
        runCommand(server, "title @a subtitle {\"text\":\"Survive the wave\",\"color\":\"dark_red\"}");

        triggerRaid(server, raidDay);

        ChallengeManager.refreshProgressBossBar(server);

        if (!debugMode) {
            ChallengeManager.setLastDayRaidEventDay(raidDay);
            ChallengeManager.save(server);
        } else {
            broadcast(server, Component.literal("[AllBlocks] Debug raid finished. Day raid progress was not saved."));
        }
    }

    private static void triggerRaid(MinecraftServer server, int raidDay) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        if (players.isEmpty()) {
            return;
        }

        for (ServerPlayer player : players) {
            spawnRaidForPlayer(player, raidDay);
        }

        applySkeletonBows(server);
        applyRaidSunProtection(server);
        applyCommonRaidMobAttributes(server);
        applyDaySpecificRaidEffects(server, raidDay);

        runCommand(server, "team join allblocks_raid @e[tag=allblocks_raid_mob]");
        broadcast(server, Component.literal("[AllBlocks] Day " + raidDay + " Raid has started."));
    }

    private static void applyCommonRaidMobAttributes(MinecraftServer server) {
        runCommand(server, "execute as @e[tag=allblocks_raid_mob] run attribute @s minecraft:generic.follow_range base set 64");
        runCommand(server, "execute as @e[tag=allblocks_raid_mob] run attribute @s minecraft:follow_range base set 64");

        // Base raid speed: Speed II
        runCommand(server, "effect give @e[tag=allblocks_raid_mob] minecraft:speed 999999 1 true");
    }

    private static void applyDaySpecificRaidEffects(MinecraftServer server, int raidDay) {
        switch (raidDay) {
            case 30 -> applyDay30Speed(server);
            case 60 -> applyDay60Speed(server);
            case 70 -> applyDay70Effects(server);
            default -> {
            }
        }
    }

    private static void spawnRaidForPlayer(ServerPlayer player, int raidDay) {
        switch (raidDay) {
            case 10 -> spawnSameType(player, "minecraft:zombie", randomInt(5, 10));
            case 20 -> spawnDay20Raid(player);
            case 30 -> spawnDay30Raid(player);
            case 40 -> spawnSameType(player, "minecraft:witch", randomInt(7, 10));
            case 50 -> spawnDay50Raid(player);
            case 60 -> spawnDay60Raid(player);
            case 70 -> spawnDay70Raid(player);
            case 80 -> spawnDay80Raid(player);
            case 90 -> spawnDay90Raid(player);
            default -> {
            }
        }
    }

    private static void spawnDay20Raid(ServerPlayer player) {
        int total = randomInt(7, 10);

        for (int i = 0; i < total; i++) {
            if (ThreadLocalRandom.current().nextBoolean()) {
                spawnMob(player, "minecraft:skeleton");
            } else {
                spawnMob(player, "minecraft:cave_spider");
            }
        }
    }

    private static void spawnDay30Raid(ServerPlayer player) {
        spawnDay30SameType(player, "minecraft:silverfish", randomInt(30, 40));
    }

    private static void spawnDay50Raid(ServerPlayer player) {
        int total = randomInt(10, 13);

        int pillagerPercent = randomInt(40, 70);
        int pillagerCount = Math.round(total * (pillagerPercent / 100.0F));
        int vindicatorCount = total - pillagerCount;

        spawnSameType(player, "minecraft:pillager", pillagerCount);
        spawnSameType(player, "minecraft:vindicator", vindicatorCount);
    }

    private static void spawnDay60Raid(ServerPlayer player) {
        int total = randomInt(10, 20);

        int[][] ratios = {
                {4, 4, 2},
                {4, 3, 3},
                {5, 4, 1}
        };

        int[] ratio = ratios[ThreadLocalRandom.current().nextInt(ratios.length)];

        int zombieCount = Math.round(total * (ratio[0] / 10.0F));
        int skeletonCount = Math.round(total * (ratio[1] / 10.0F));
        int creeperCount = total - zombieCount - skeletonCount;

        spawnDay60SameType(player, "minecraft:zombie", zombieCount);
        spawnDay60SameType(player, "minecraft:skeleton", skeletonCount);
        spawnDay60SameType(player, "minecraft:creeper", creeperCount);
    }

    private static void spawnDay70Raid(ServerPlayer player) {
        int total = randomInt(15, 30);

        int specialCount = Math.max(1, Math.round(total * 0.30F));
        int normalCount = total - specialCount;

        int babyZombieCount = randomInt(0, specialCount);
        int creeperCount = specialCount - babyZombieCount;

        int zombieCount = randomInt(0, normalCount);
        int skeletonCount = normalCount - zombieCount;

        spawnEnhancedSameType(player, "minecraft:zombie", zombieCount, false);
        spawnEnhancedSameType(player, "minecraft:skeleton", skeletonCount, false);
        spawnEnhancedSameType(player, "minecraft:creeper", creeperCount, false);
        spawnEnhancedSameType(player, "minecraft:zombie", babyZombieCount, true);

        if (player.level() instanceof ServerLevel level) {
            MinecraftServer server = level.getServer();

            applyDay70Armor(server);
            applyDay70FollowRange(server);
            applyDay70Scale(server);
            applyDay70WaterMovement(server);
        }

        player.sendSystemMessage(Component.literal(
                "[AllBlocks] Day 70 Raid: enhanced mobs spawned. Total: " + total
        ));
    }

    private static void spawnDay80Raid(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos spawnPos = findSpawnPosition(level, player.blockPosition());

        if (spawnPos == null) {
            player.sendSystemMessage(Component.literal(
                    "[AllBlocks] Day 80 Warden spawn failed: no valid position found."
            ));
            return;
        }

        double x = spawnPos.getX() + 0.5D;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5D;

        MinecraftServer server = level.getServer();

        runCommand(server,
                "summon minecraft:warden " + x + " " + y + " " + z
                        + " {PersistenceRequired:1b,"
                        + "Tags:[\"allblocks_raid_mob\",\"allblocks_day80_warden\"],"
                        + "Brain:{memories:{\"minecraft:dig_cooldown\":{value:{},ttl:1200000L}}}}"
        );

        applyDay80WardenAttributes(server);

        player.sendSystemMessage(Component.literal(
                "[AllBlocks] Day 80 Raid: weakened Warden spawned. HP: 150"
        ));
    }

    private static void applyDay80WardenAttributes(MinecraftServer server) {
        runCommand(server, "execute as @e[tag=allblocks_day80_warden] run attribute @s minecraft:generic.max_health base set 150");
        runCommand(server, "execute as @e[tag=allblocks_day80_warden] run attribute @s minecraft:max_health base set 150");

        runCommand(server, "execute as @e[tag=allblocks_day80_warden] run data modify entity @s Health set value 150f");

        runCommand(server, "execute as @e[tag=allblocks_day80_warden] run attribute @s minecraft:generic.follow_range base set 100");
        runCommand(server, "execute as @e[tag=allblocks_day80_warden] run attribute @s minecraft:follow_range base set 100");
    }

    private static void spawnDay90Raid(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos spawnPos = findBossSpawnPosition(level, player.blockPosition());

        if (spawnPos == null) {
            player.sendSystemMessage(Component.literal(
                    "[AllBlocks] Day 90 Wither spawn failed: no valid position found."
            ));
            return;
        }

        double x = spawnPos.getX() + 0.5D;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5D;

        MinecraftServer server = level.getServer();

        runCommand(server,
                "summon minecraft:wither " + x + " " + y + " " + z
                        + " {PersistenceRequired:1b,Tags:[\"allblocks_raid_mob\",\"allblocks_day90_wither\"]}"
        );

        rememberNearestDay90Wither(level, x, y, z);
        applyDay90WitherAttributes(server);

        player.sendSystemMessage(Component.literal(
                "[AllBlocks] Day 90 Raid: Wither spawned."
        ));
    }

    private static void applyDay90WitherAttributes(MinecraftServer server) {
        runCommand(server, "execute as @e[tag=allblocks_day90_wither] run attribute @s minecraft:generic.follow_range base set 160");
        runCommand(server, "execute as @e[tag=allblocks_day90_wither] run attribute @s minecraft:follow_range base set 160");
    }

    private static void rememberNearestDay90Wither(ServerLevel level, double x, double y, double z) {
        AABB searchBox = new AABB(
                x - 8.0D,
                y - 8.0D,
                z - 8.0D,
                x + 8.0D,
                y + 8.0D,
                z + 8.0D
        );

        List<WitherBoss> withers = level.getEntitiesOfClass(
                WitherBoss.class,
                searchBox,
                wither -> wither.isAlive()
        );

        WitherBoss nearestWither = null;
        double nearestDistanceSqr = Double.MAX_VALUE;

        for (WitherBoss wither : withers) {
            double distanceSqr = wither.distanceToSqr(x, y, z);

            if (distanceSqr < nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearestWither = wither;
            }
        }

        if (nearestWither != null) {
            day90WitherIds.add(nearestWither.getUUID());
        }
    }

    private static void tickDay90WitherPlayerPriority(MinecraftServer server) {
        if (server == null) {
            return;
        }

        raidPursuitTickCounter++;

        if (raidPursuitTickCounter < DAY90_WITHER_TARGET_INTERVAL_TICKS) {
            return;
        }

        raidPursuitTickCounter = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive()) {
                continue;
            }

            if (!(player.level() instanceof ServerLevel level)) {
                continue;
            }

            AABB searchBox = player.getBoundingBox().inflate(DAY90_WITHER_PLAYER_PRIORITY_RANGE);

            List<WitherBoss> withers = level.getEntitiesOfClass(
                    WitherBoss.class,
                    searchBox,
                    wither -> wither.isAlive() && day90WitherIds.contains(wither.getUUID())
            );

            for (WitherBoss wither : withers) {
                ServerPlayer targetPlayer = findNearestPlayerForWither(server, wither, DAY90_WITHER_PLAYER_PRIORITY_RANGE);

                if (targetPlayer == null) {
                    continue;
                }

                forceWitherTargetPlayer(wither, targetPlayer);
            }
        }
    }

    private static ServerPlayer findNearestPlayerForWither(MinecraftServer server, WitherBoss wither, double maxDistance) {
        ServerPlayer nearestPlayer = null;
        double nearestDistanceSqr = maxDistance * maxDistance;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive()) {
                continue;
            }

            if (player.level() != wither.level()) {
                continue;
            }

            double distanceSqr = player.distanceToSqr(wither);

            if (distanceSqr < nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearestPlayer = player;
            }
        }

        return nearestPlayer;
    }

    private static void forceWitherTargetPlayer(WitherBoss wither, ServerPlayer targetPlayer) {
        wither.setTarget(targetPlayer);

        int targetEntityId = targetPlayer.getId();

        // 위더의 중앙 머리와 양쪽 머리 타겟을 모두 플레이어로 고정한다.
        wither.setAlternativeTarget(0, targetEntityId);
        wither.setAlternativeTarget(1, targetEntityId);
        wither.setAlternativeTarget(2, targetEntityId);

        wither.getLookControl().setLookAt(targetPlayer, 30.0F, 30.0F);
    }

    private static void spawnSameType(ServerPlayer targetPlayer, String entityId, int count) {
        for (int i = 0; i < count; i++) {
            spawnMob(targetPlayer, entityId);
        }
    }

    private static void spawnDay30SameType(ServerPlayer targetPlayer, String entityId, int count) {
        for (int i = 0; i < count; i++) {
            spawnMob(targetPlayer, entityId, "allblocks_day30_mob");
        }
    }

    private static void spawnDay60SameType(ServerPlayer targetPlayer, String entityId, int count) {
        for (int i = 0; i < count; i++) {
            spawnMob(targetPlayer, entityId, "allblocks_day60_mob");
        }
    }

    private static void spawnEnhancedSameType(
            ServerPlayer targetPlayer,
            String entityId,
            int count,
            boolean babyZombie
    ) {
        for (int i = 0; i < count; i++) {
            spawnEnhancedMob(targetPlayer, entityId, babyZombie);
        }
    }

    private static void spawnEnhancedMob(ServerPlayer targetPlayer, String entityId, boolean babyZombie) {
        if (!(targetPlayer.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos spawnPos = findSpawnPosition(level, targetPlayer.blockPosition());

        if (spawnPos == null) {
            targetPlayer.sendSystemMessage(Component.literal(
                    "[AllBlocks] Enhanced raid mob spawn failed: no valid position found."
            ));
            return;
        }

        double x = spawnPos.getX() + 0.5D;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5D;

        MinecraftServer server = level.getServer();

        String tags = entityId.equals("minecraft:skeleton")
                ? "[\"allblocks_raid_mob\",\"allblocks_day70_mob\",\"allblocks_skeleton_mob\"]"
                : "[\"allblocks_raid_mob\",\"allblocks_day70_mob\"]";

        String nbt;

        if (babyZombie) {
            nbt = "{PersistenceRequired:1b,IsBaby:1b,Tags:" + tags + "}";
        } else {
            nbt = "{PersistenceRequired:1b,Tags:" + tags + "}";
        }

        runCommand(server, "summon " + entityId + " " + x + " " + y + " " + z + " " + nbt);

    }

    private static void applyDay70Armor(MinecraftServer server) {
        runCommand(server, "item replace entity @e[tag=allblocks_day70_mob] armor.head with minecraft:netherite_helmet");
        runCommand(server, "item replace entity @e[tag=allblocks_day70_mob] armor.chest with minecraft:netherite_chestplate");
        runCommand(server, "item replace entity @e[tag=allblocks_day70_mob] armor.legs with minecraft:netherite_leggings");
        runCommand(server, "item replace entity @e[tag=allblocks_day70_mob] armor.feet with minecraft:netherite_boots");
    }

    private static void applyDay70Effects(MinecraftServer server) {
        runCommand(server, "effect give @e[tag=allblocks_day70_mob] minecraft:strength 999999 2 true");
        runCommand(server, "effect give @e[tag=allblocks_day70_mob] minecraft:speed 999999 4 true");
        runCommand(server, "effect give @e[tag=allblocks_day70_mob] minecraft:regeneration 999999 2 true");
        runCommand(server, "effect give @e[tag=allblocks_day70_mob] minecraft:fire_resistance 999999 0 true");
    }

    private static void applyDay70WaterMovement(MinecraftServer server) {
        runCommand(server, "execute as @e[tag=allblocks_day70_mob] run attribute @s minecraft:generic.water_movement_efficiency base set 1.0");
        runCommand(server, "execute as @e[tag=allblocks_day70_mob] run attribute @s minecraft:water_movement_efficiency base set 1.0");
    }

    private static void applyDay70FollowRange(MinecraftServer server) {
        runCommand(server, "execute as @e[tag=allblocks_day70_mob] run attribute @s minecraft:generic.follow_range base set 64");
        runCommand(server, "execute as @e[tag=allblocks_day70_mob] run attribute @s minecraft:follow_range base set 64");
    }

    private static void applyDay70Scale(MinecraftServer server) {
        runCommand(server, "execute as @e[tag=allblocks_day70_mob] run attribute @s minecraft:generic.scale base set 1.5");
        runCommand(server, "execute as @e[tag=allblocks_day70_mob] run attribute @s minecraft:scale base set 1.5");
    }

    private static void applySkeletonBows(MinecraftServer server) {
        runCommand(server, "item replace entity @e[tag=allblocks_skeleton_mob] weapon.mainhand with minecraft:bow");
    }

    private static void applyRaidSunProtection(MinecraftServer server) {
        runCommand(server, "effect give @e[tag=allblocks_raid_mob,type=minecraft:zombie] minecraft:fire_resistance 999999 0 true");
        runCommand(server, "effect give @e[tag=allblocks_raid_mob,type=minecraft:skeleton] minecraft:fire_resistance 999999 0 true");
    }

    private static void applyDay60Speed(MinecraftServer server) {
        runCommand(server, "effect give @e[tag=allblocks_day60_mob] minecraft:speed 999999 3 true");
    }

    private static void applyDay30Speed(MinecraftServer server) {
        // Speed III
        runCommand(server, "effect give @e[tag=allblocks_day30_mob] minecraft:speed 999999 2 true");
    }

    private static void spawnMob(ServerPlayer targetPlayer, String entityId) {
        spawnMob(targetPlayer, entityId, null);
    }

    private static void spawnMob(ServerPlayer targetPlayer, String entityId, String extraTag) {
        if (!(targetPlayer.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos spawnPos = findSpawnPosition(level, targetPlayer.blockPosition());

        if (spawnPos == null) {
            targetPlayer.sendSystemMessage(Component.literal(
                    "[AllBlocks] Raid mob spawn failed: no valid position found."
            ));
            return;
        }

        double x = spawnPos.getX() + 0.5D;
        double y = spawnPos.getY();
        double z = spawnPos.getZ() + 0.5D;

        MinecraftServer server = level.getServer();

        String tags = buildRaidMobTags(entityId, extraTag);

        runCommand(server,
                "summon " + entityId + " " + x + " " + y + " " + z
                        + " {PersistenceRequired:1b,Tags:" + tags + "}"
        );
    }

    private static String buildRaidMobTags(String entityId, String extraTag) {
        StringBuilder tags = new StringBuilder("[\"allblocks_raid_mob\"");

        if (entityId.equals("minecraft:skeleton")) {
            tags.append(",\"allblocks_skeleton_mob\"");
        }

        if (extraTag != null && !extraTag.isBlank()) {
            tags.append(",\"").append(extraTag).append("\"");
        }

        tags.append("]");

        return tags.toString();
    }

    private static BlockPos findSpawnPosition(ServerLevel level, BlockPos playerPos) {
        for (int attempt = 0; attempt < 40; attempt++) {
            int dx = randomInt(-5, 5);
            int dz = randomInt(-5, 5);

            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                continue;
            }

            for (int dy = 5; dy >= -5; dy--) {
                BlockPos pos = playerPos.offset(dx, dy, dz);

                if (isValidSpawnSpace(level, pos)) {
                    return pos;
                }
            }
        }

        return null;
    }

    private static BlockPos findBossSpawnPosition(ServerLevel level, BlockPos playerPos) {
        for (int attempt = 0; attempt < 50; attempt++) {
            int dx = randomInt(-6, 6);
            int dz = randomInt(-6, 6);

            if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) {
                continue;
            }

            for (int dy = 6; dy >= -4; dy--) {
                BlockPos pos = playerPos.offset(dx, dy, dz);

                if (isValidBossSpawnSpace(level, pos)) {
                    return pos;
                }
            }
        }

        return null;
    }

    private static boolean isValidBossSpawnSpace(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.above(2)).isAir()
                && !level.getBlockState(pos.below()).isAir();
    }

    private static boolean isValidSpawnSpace(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && !level.getBlockState(pos.below()).isAir();
    }

    private static void setupRaidTeam(MinecraftServer server) {
        runCommand(server, "team add allblocks_raid");
        runCommand(server, "team modify allblocks_raid friendlyFire false");
        runCommand(server, "team modify allblocks_raid collisionRule never");
    }

    private static void broadcast(MinecraftServer server, Component message) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
    }

    private static int randomInt(int minInclusive, int maxInclusive) {
        return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
    }

    private static void runCommand(MinecraftServer server, String command) {
        try {
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(),
                    command
            );
        } catch (Exception ignored) {
        }
    }

    private static final class ActiveRaid {
        private final int raidDay;
        private final boolean debugMode;
        private int elapsedTicks;

        private ActiveRaid(int raidDay, boolean debugMode) {
            this.raidDay = raidDay;
            this.debugMode = debugMode;
            this.elapsedTicks = 0;
        }
    }
}