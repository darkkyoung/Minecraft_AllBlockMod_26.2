package com.darkk0729.allblocks.client.network;

import com.darkk0729.allblocks.client.data.ClientChallengeStateCache;
import com.darkk0729.allblocks.network.AllBlocksSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.darkk0729.allblocks.network.ChallengeStatusPayload;
import com.darkk0729.allblocks.client.hud.TeamRevealHud;
import com.darkk0729.allblocks.network.TeamRevealPayload;
import com.darkk0729.allblocks.network.PlayerColorChangePayload;
import com.darkk0729.allblocks.network.CloseSetupScreenPayload;
import com.darkk0729.allblocks.network.ClearMenuChatPayload;


public final class AllBlocksClientNetworking {
    private AllBlocksClientNetworking() {
    }

    public static void sendPlayerColorChange(
            String color
    ) {
        if (color == null || color.isBlank()) {
            return;
        }

        ClientPlayNetworking.send(
                new PlayerColorChangePayload(
                        color
                )
        );
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
                (payload, context) -> context.client().execute(() -> {
                    context.client()
                            .gui
                            .hud
                            .getChat()
                            .clearMessages(false);

                    context.client()
                            .gui
                            .setScreen(null);

                    TeamRevealHud.start(
                            payload.finalTeam()
                    );
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(
                CloseSetupScreenPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (payload.close()) {
                        context.client().gui.setScreen(null);
                    }
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(
                ClearMenuChatPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (payload.clear()) {
                        context.client()
                                .gui
                                .hud
                                .getChat()
                                .clearMessages(false);
                    }
                })
        );
    }

}
