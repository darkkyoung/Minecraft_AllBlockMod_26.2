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
import net.minecraft.client.Minecraft;


public final class AllBlocksClientNetworking {
    private static boolean menuChatLayoutActive = false;
    private static int menuChatLayoutTicksRemaining = 0;
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
                ClearMenuChatPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    if (payload.clear()) {
                        showMenuChatLayout(
                                context.client()
                        );
                    } else {
                        restoreMenuChatLayout(
                                context.client()
                        );
                    }
                })
        );
    }

    public static void tickMenuChatLayout(
            Minecraft client
    ) {
        if (!menuChatLayoutActive
                || client == null) {
            return;
        }

        if (menuChatLayoutTicksRemaining > 0) {
            menuChatLayoutTicksRemaining--;
        }

        if (menuChatLayoutTicksRemaining <= 0) {
            restoreMenuChatLayout(client);
        }
    }

    public static void clearMenuChatLayout(
            Minecraft client
    ) {
        restoreMenuChatLayout(client);
    }

    private static void showMenuChatLayout(
            Minecraft client
    ) {
        if (client == null) {
            return;
        }

        if (!menuChatLayoutActive) {
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

            menuChatLayoutActive = true;
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

        client.gui
                .hud
                .getChat()
                .clearMessages(false);

        menuChatLayoutTicksRemaining =
                20 * 30;
    }

    private static void restoreMenuChatLayout(
            Minecraft client
    ) {
        if (!menuChatLayoutActive
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

        menuChatLayoutActive = false;
        menuChatLayoutTicksRemaining = 0;
    }
}

