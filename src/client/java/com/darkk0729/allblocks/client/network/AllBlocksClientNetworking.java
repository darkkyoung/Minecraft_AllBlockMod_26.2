package com.darkk0729.allblocks.client.network;

import com.darkk0729.allblocks.client.data.ClientChallengeStateCache;
import com.darkk0729.allblocks.network.AllBlocksSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.darkk0729.allblocks.network.ChallengeStatusPayload;
import com.darkk0729.allblocks.client.hud.TeamRevealHud;
import com.darkk0729.allblocks.network.TeamRevealPayload;
import com.darkk0729.allblocks.network.PlayerColorChangePayload;
import com.darkk0729.allblocks.network.CloseSetupScreenPayload;
import com.darkk0729.allblocks.network.ChatHeightBoostPayload;
import net.minecraft.client.Minecraft;


public final class AllBlocksClientNetworking {
    private static boolean chatHeightBoostActive = false;
    private static int chatHeightBoostTicksRemaining = 0;
    private static double originalChatHeightFocused = 1.0D;
    private static double originalChatHeightUnfocused = 1.0D;
    private static double originalChatScale = 1.0D;
    private static double originalChatWidth = 1.0D;

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
                (payload, context) -> context.client().execute(() ->
                        TeamRevealHud.start(payload.finalTeam())
                )
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
                ChatHeightBoostPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        startChatHeightBoost(
                                context.client(),
                                payload.durationTicks()
                        )
                )
        );
    }

    public static void tickChatHeightBoost(
            Minecraft client
    ) {
        if (!chatHeightBoostActive
                || client == null) {
            return;
        }

        if (chatHeightBoostTicksRemaining > 0) {
            chatHeightBoostTicksRemaining--;
        }

        if (chatHeightBoostTicksRemaining <= 0) {
            restoreChatHeight(client);
        }
    }

    public static void clearChatHeightBoost(
            Minecraft client
    ) {
        if (client == null) {
            chatHeightBoostActive = false;
            chatHeightBoostTicksRemaining = 0;
            return;
        }

        restoreChatHeight(client);
    }

    private static void startChatHeightBoost(
            Minecraft client,
            int durationTicks
    ) {
        if (client == null) {
            return;
        }

        if (!chatHeightBoostActive) {
            originalChatHeightFocused =
                    client.options
                            .chatHeightFocused()
                            .get();

            originalChatHeightUnfocused =
                    client.options
                            .chatHeightUnfocused()
                            .get();

            originalChatScale =
                    client.options
                            .chatScale()
                            .get();

            originalChatWidth =
                    client.options
                            .chatWidth()
                            .get();

            chatHeightBoostActive = true;
        }

        client.options
                .chatHeightFocused()
                .set(1.0D);

        client.options
                .chatHeightUnfocused()
                .set(1.0D);

        client.options
                .chatWidth()
                .set(1.0D);

        client.options
                .chatScale()
                .set(
                        Math.min(
                                originalChatScale,
                                0.75D
                        )
                );

        chatHeightBoostTicksRemaining =
                Math.max(
                        chatHeightBoostTicksRemaining,
                        Math.max(
                                1,
                                durationTicks
                        )
                );
    }

    private static void restoreChatHeight(
            Minecraft client
    ) {
        if (!chatHeightBoostActive
                || client == null) {
            return;
        }

        client.options
                .chatHeightFocused()
                .set(originalChatHeightFocused);

        client.options
                .chatHeightUnfocused()
                .set(originalChatHeightUnfocused);

        client.options
                .chatScale()
                .set(originalChatScale);

        client.options
                .chatWidth()
                .set(originalChatWidth);

        chatHeightBoostActive = false;
        chatHeightBoostTicksRemaining = 0;
    }
}
