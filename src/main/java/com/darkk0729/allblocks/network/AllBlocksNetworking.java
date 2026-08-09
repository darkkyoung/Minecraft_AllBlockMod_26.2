package com.darkk0729.allblocks.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class AllBlocksNetworking {
    private AllBlocksNetworking() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(
                AllBlocksSyncPayload.TYPE,
                AllBlocksSyncPayload.CODEC
        );
    }
}