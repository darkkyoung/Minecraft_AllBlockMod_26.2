package com.darkk0729.allblocks.client;

import com.darkk0729.allblocks.client.hud.ChallengeInfoHud;
import com.darkk0729.allblocks.client.screen.BlockCodexScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.darkk0729.allblocks.client.network.CodexToastClientNetworking;
import com.darkk0729.allblocks.client.network.AllBlocksClientNetworking;
import com.darkk0729.allblocks.client.data.ClientChallengeStateCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import com.darkk0729.allblocks.client.hud.TeamRevealHud;
import com.darkk0729.allblocks.client.hud.TeamRaceHud;

public final class AllBlocksClient implements ClientModInitializer {
    private static KeyMapping openCodexKey;

    @Override
    public void onInitializeClient() {
        CodexToastClientNetworking.registerReceivers();
        AllBlocksClientNetworking.registerReceivers();

        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    ClientChallengeStateCache.clear();
                    TeamRevealHud.clear();
                }
        );

        ChallengeInfoHud.register();
        TeamRevealHud.register();
        TeamRaceHud.register();

        openCodexKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.allblocks.open_codex",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_B,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openCodexKey.consumeClick()) {
                if (client.player == null) {
                    continue;
                }

                client.gui.setScreen(new BlockCodexScreen());
            }
        });
    }
}