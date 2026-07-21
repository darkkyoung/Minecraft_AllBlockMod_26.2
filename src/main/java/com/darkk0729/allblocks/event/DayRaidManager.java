package com.darkk0729.allblocks.event;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class DayRaidManager {
    private static final String RAID_BOSSBAR_ID = "allblocks:day_raid";
    private static final int RAID_WARNING_TICKS = 20 * 5;

    private static ActiveRaid activeRaid;

    private DayRaidManager() {
    }

    public static void tick(MinecraftServer server) {
        if (!ChallengeManager.isRunning()) {
            removeRaidBossBar(server);
            activeRaid = null;
            return;
        }

        if (activeRaid != null) {
            tickActiveRaidWarning(server);
            return;
        }

        checkDayRaidStart(server);
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
        activeRaid = new ActiveRaid(raidDay);

        setupRaidTeam(server);
        removeRaidBossBar(server);

        runCommand(server, "bossbar add " + RAID_BOSSBAR_ID + " {\"text\":\"Day " + raidDay + " Raid\"}");
        runCommand(server, "bossbar set " + RAID_BOSSBAR_ID + " color red");
        runCommand(server, "bossbar set " + RAID_BOSSBAR_ID + " style progress");
        runCommand(server, "bossbar set " + RAID_BOSSBAR_ID + " max 100");
        runCommand(server, "bossbar set " + RAID_BOSSBAR_ID + " value 0");
        runCommand(server, "bossbar set " + RAID_BOSSBAR_ID + " visible true");
        runCommand(server, "bossbar set " + RAID_BOSSBAR_ID + " players @a");

        runCommand(server, "playsound minecraft:event.raid.horn master @a ~ ~ ~ 1 1");

        broadcast(server, Component.literal("[AllBlocks] Day " + raidDay + " Raid is coming..."));
    }

    private static void tickActiveRaidWarning(MinecraftServer server) {
        activeRaid.elapsedTicks++;

        int value = Math.min(100, (activeRaid.elapsedTicks * 100) / RAID_WARNING_TICKS);

        runCommand(server, "bossbar set " + RAID_BOSSBAR_ID + " value " + value);

        if (activeRaid.elapsedTicks < RAID_WARNING_TICKS) {
            return;
        }

        int raidDay = activeRaid.raidDay;
        activeRaid = null;

        removeRaidBossBar(server);

        runCommand(server, "title @a title {\"text\":\"Day " + raidDay + " Raid\",\"color\":\"red\",\"bold\":true}");
        runCommand(server, "title @a subtitle {\"text\":\"Survive the wave\",\"color\":\"dark_red\"}");

        triggerRaid(server, raidDay);

        ChallengeManager.setLastDayRaidEventDay(raidDay);
        ChallengeManager.save(server);
    }

    private static void triggerRaid(MinecraftServer server, int raidDay) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        if (players.isEmpty()) {
            return;
        }

        for (ServerPlayer player : players) {
            spawnRaidForPlayer(player, raidDay);
        }

        runCommand(server, "team join allblocks_raid @e[tag=allblocks_raid_mob]");

        broadcast(server, Component.literal("[AllBlocks] Day " + raidDay + " Raid has started."));
    }

    private static void spawnRaidForPlayer(ServerPlayer player, int raidDay) {
        switch (raidDay) {
            case 10 -> spawnSameType(player, "minecraft:zombie", randomInt(5, 10));
            case 20 -> spawnDay20Raid(player);
            case 30 -> spawnSameType(player, "minecraft:silverfish", randomInt(30, 40));
            case 40 -> spawnSameType(player, "minecraft:witch", randomInt(7, 10));
            case 50 -> spawnDay50Raid(player);
            case 60 -> spawnDay60Raid(player);
            case 70 -> spawnDay70Placeholder(player);
            case 80 -> spawnDay80Placeholder(player);
            case 90 -> spawnDay90Placeholder(player);
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

        spawnSameType(player, "minecraft:zombie", zombieCount);
        spawnSameType(player, "minecraft:skeleton", skeletonCount);
        spawnSameType(player, "minecraft:creeper", creeperCount);
    }

    private static void spawnDay70Placeholder(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(
                "[AllBlocks] Day 70 Raid is reserved for enhanced mobs. It will be implemented next."
        ));
    }

    private static void spawnDay80Placeholder(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(
                "[AllBlocks] Day 80 Raid is reserved for weakened Warden. It will be implemented next."
        ));
    }

    private static void spawnDay90Placeholder(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(
                "[AllBlocks] Day 90 Raid is reserved for Wither. It will be implemented next."
        ));
    }

    private static void spawnSameType(ServerPlayer targetPlayer, String entityId, int count) {
        for (int i = 0; i < count; i++) {
            spawnMob(targetPlayer, entityId);
        }
    }

    private static void spawnMob(ServerPlayer targetPlayer, String entityId) {
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

        runCommand(server,
                "summon " + entityId + " " + x + " " + y + " " + z
                        + " {PersistenceRequired:1b,Tags:[\"allblocks_raid_mob\"]}"
        );
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

    private static void removeRaidBossBar(MinecraftServer server) {
        runCommand(server, "bossbar remove " + RAID_BOSSBAR_ID);
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
        private int elapsedTicks;

        private ActiveRaid(int raidDay) {
            this.raidDay = raidDay;
            this.elapsedTicks = 0;
        }
    }
}