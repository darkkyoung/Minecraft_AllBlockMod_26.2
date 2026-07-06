package com.darkk0729.allblocks.collection;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class BlockCollectionTracker {
    private static final long SCAN_INTERVAL_TICKS = 20L;
    private static long tickCounter = 0L;

    private BlockCollectionTracker() {
    }

    public static void tick(MinecraftServer server) {
        if (!ChallengeManager.isRunning()) {
            return;
        }

        tickCounter++;

        if (tickCounter < SCAN_INTERVAL_TICKS) {
            return;
        }

        tickCounter = 0L;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            scanPlayerInventory(server, player);
        }
    }

    private static void scanPlayerInventory(MinecraftServer server, ServerPlayer player) {
        Inventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                continue;
            }

            Block block = blockItem.getBlock();
            var blockId = BuiltInRegistries.BLOCK.getKey(block);

            if (blockId == null) {
                continue;
            }

            String id = blockId.toString();

            if (!TargetBlockRegistry.isTargetBlock(id)) {
                continue;
            }

            boolean newlyCollected = ChallengeManager.collectBlock(server, player, id);

            if (newlyCollected) {
                player.sendSystemMessage(Component.literal(
                        "[AllBlocks] New block collected: " + id
                ));
            }
        }
    }
}