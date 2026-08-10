package com.darkk0729.allblocks.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class CodexToastNetworking {
    private CodexToastNetworking() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(
                CodexToastPayload.TYPE,
                CodexToastPayload.CODEC
        );
    }
}