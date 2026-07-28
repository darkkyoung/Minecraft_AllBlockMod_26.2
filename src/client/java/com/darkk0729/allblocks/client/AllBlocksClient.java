package com.darkk0729.allblocks.client;

import com.darkk0729.allblocks.client.screen.BlockCodexScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public final class AllBlocksClient implements ClientModInitializer {
    private static KeyMapping openCodexKey;

    @Override
    public void onInitializeClient() {
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