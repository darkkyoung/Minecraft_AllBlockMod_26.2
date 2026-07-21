package com.darkk0729.allblocks.collection;

import com.darkk0729.allblocks.AllBlocksMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class TargetBlockRegistry {
    private static final Set<String> EXCLUDED_BLOCK_IDS = Set.of(
            "minecraft:air",
            "minecraft:cave_air",
            "minecraft:void_air",

            "minecraft:bedrock",
            "minecraft:end_portal_frame",
            "minecraft:spawner",
            "minecraft:trial_spawner",

            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:structure_block",
            "minecraft:structure_void",
            "minecraft:jigsaw",
            "minecraft:barrier",
            "minecraft:light",

            "minecraft:suspicious_sand",
            "minecraft:suspicious_gravel",

            "minecraft:player_head",
            "minecraft:player_wall_head"
    );

    private static final List<String> TARGET_BLOCK_IDS = new ArrayList<>();
    private static final List<Block> TARGET_BLOCKS = new ArrayList<>();

    private static boolean initialized = false;

    private TargetBlockRegistry() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        TARGET_BLOCK_IDS.clear();
        TARGET_BLOCKS.clear();

        for (Block block : BuiltInRegistries.BLOCK) {
            var blockId = BuiltInRegistries.BLOCK.getKey(block);

            if (blockId == null) {
                continue;
            }

            String id = blockId.toString();

            if (EXCLUDED_BLOCK_IDS.contains(id)) {
                continue;
            }

            Item item = block.asItem();

            if (item == Items.AIR) {
                continue;
            }

            TARGET_BLOCK_IDS.add(id);
            TARGET_BLOCKS.add(block);
        }

        initialized = true;

        AllBlocksMod.LOGGER.info("AllBlocks target blocks loaded: {}", TARGET_BLOCK_IDS.size());
    }

    public static boolean isTargetBlock(String blockId) {
        return TARGET_BLOCK_IDS.contains(blockId);
    }

    public static int getTotalTargetCount() {
        return TARGET_BLOCK_IDS.size();
    }

    public static List<String> getTargetBlockIds() {
        return Collections.unmodifiableList(TARGET_BLOCK_IDS);
    }

    public static Block getRandomTargetBlock() {
        if (TARGET_BLOCKS.isEmpty()) {
            return null;
        }

        int index = ThreadLocalRandom.current().nextInt(TARGET_BLOCKS.size());
        return TARGET_BLOCKS.get(index);
    }
}