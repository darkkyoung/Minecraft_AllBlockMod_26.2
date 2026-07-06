package com.darkk0729.allblocks.collection;

import com.darkk0729.allblocks.AllBlocksMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    private static final Set<String> TARGET_BLOCK_IDS = new HashSet<>();
    private static boolean initialized = false;

    private TargetBlockRegistry() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        TARGET_BLOCK_IDS.clear();

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

            // 아이템 형태가 없는 블록은 생존 도감 대상에서 제외
            if (item == Items.AIR) {
                continue;
            }

            TARGET_BLOCK_IDS.add(id);
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

    public static Set<String> getTargetBlockIds() {
        return Collections.unmodifiableSet(TARGET_BLOCK_IDS);
    }
}