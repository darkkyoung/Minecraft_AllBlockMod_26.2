package com.darkk0729.allblocks.event;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerDeathHandler {
    private PlayerDeathHandler() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return;
            }

            if (!ChallengeManager.isRunning()) {
                return;
            }

            MinecraftServer server = entity.level().getServer();

            if (server == null) {
                return;
            }

            boolean pvpDeath = damageSource.getEntity() instanceof ServerPlayer;

            ChallengeManager.handlePlayerDeath(server, player, pvpDeath);
        });
    }
}