package com.darkk0729.allblocks.client.network;

import com.darkk0729.allblocks.client.data.ClientChallengeStateCache;
import com.darkk0729.allblocks.network.AllBlocksSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.darkk0729.allblocks.network.ChallengeStatusPayload;
import com.darkk0729.allblocks.client.hud.TeamRevealHud;
import com.darkk0729.allblocks.network.TeamRevealPayload;


public final class AllBlocksClientNetworking {
    private AllBlocksClientNetworking() {
    }

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(
                AllBlocksSyncPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        ClientChallengeStateCache.apply(payload)
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                ChallengeStatusPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        ClientChallengeStateCache.applyStatus(payload)
                )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                TeamRevealPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        TeamRevealHud.start(payload.finalTeam())
                )
        );
    }
}