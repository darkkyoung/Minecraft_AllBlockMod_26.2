package com.darkk0729.allblocks;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import com.darkk0729.allblocks.collection.TargetBlockRegistry;
import com.darkk0729.allblocks.command.AllBlocksCommands;
import com.darkk0729.allblocks.event.ChallengeTicker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.darkk0729.allblocks.event.PlayerDeathHandler;
// import com.darkk0729.allblocks.network.AllBlocksNetworking;
// import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class AllBlocksMod implements ModInitializer {
    public static final String MOD_ID = "allblocks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing All Blocks Challenge");
        // AllBlocksNetworking.registerPayloads();

        TargetBlockRegistry.initialize();
        AllBlocksCommands.register();
        ChallengeTicker.register();
        PlayerDeathHandler.register();

        ServerLifecycleEvents.SERVER_STARTED.register(ChallengeManager::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(ChallengeManager::save);

        /*
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> ChallengeManager.syncToPlayer(handler.player));
        });

         */
    }
}