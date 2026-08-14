package com.darkk0729.allblocks.network;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class AllBlocksNetworking {

    private AllBlocksNetworking() {
    }

    public static void registerPayloads() {

        // 서버 -> 클라이언트
        PayloadTypeRegistry.clientboundPlay().register(
                AllBlocksSyncPayload.TYPE,
                AllBlocksSyncPayload.CODEC
        );

        // 클라이언트 -> 서버
        PayloadTypeRegistry.serverboundPlay().register(
                PlayerColorChangePayload.TYPE,
                PlayerColorChangePayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                PlayerColorChangePayload.TYPE,
                (payload, context) ->
                        context.server().execute(() ->
                                ChallengeManager.changeOwnPlayerColor(
                                        context.server(),
                                        context.player(),
                                        payload.color()
                                )
                        )
        );
    }
}