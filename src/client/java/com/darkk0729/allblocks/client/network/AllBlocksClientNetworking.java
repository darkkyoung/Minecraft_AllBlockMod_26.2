package com.darkk0729.allblocks.client.network;

import com.darkk0729.allblocks.client.data.ClientChallengeStateCache;
import com.darkk0729.allblocks.network.AllBlocksSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

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
    }
}