package com.darkk0729.allblocks.collection;

import com.darkk0729.allblocks.AllBlocksMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
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

            // Survival-unobtainable / creative-only / structure-only blocks
            "minecraft:bedrock",
            "minecraft:reinforced_deepslate",
            "minecraft:budding_amethyst",
            "minecraft:end_portal_frame",
            "minecraft:spawner",
            "minecraft:trial_spawner",
            "minecraft:vault",
            "minecraft:frogspawn",
            "minecraft:petrified_oak_slab",

            // Suspicious blocks are not obtainable as normal inventory blocks.
            "minecraft:suspicious_sand",
            "minecraft:suspicious_gravel",

            // Infested blocks are not valid normal survival collection targets.
            "minecraft:infested_stone",
            "minecraft:infested_cobblestone",
            "minecraft:infested_stone_bricks",
            "minecraft:infested_mossy_stone_bricks",
            "minecraft:infested_cracked_stone_bricks",
            "minecraft:infested_chiseled_stone_bricks",
            "minecraft:infested_deepslate",

            // Player heads are not normally obtainable in vanilla survival.
            "minecraft:player_head",
            "minecraft:player_wall_head",

            // Command / debug / structure blocks
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:structure_block",
            "minecraft:structure_void",
            "minecraft:jigsaw",
            "minecraft:barrier",
            "minecraft:light",

            //이 새끼들 뭐야
            "minecraft:test_block",
            "minecraft:test_instance_block",

            // Technical / non-inventory block states.
            // Most of these are also filtered by item == Items.AIR or BlockItem check.
            "minecraft:fire",
            "minecraft:soul_fire",
            "minecraft:nether_portal",
            "minecraft:end_portal",
            "minecraft:end_gateway",
            "minecraft:moving_piston",
            "minecraft:piston_head",
            "minecraft:water",
            "minecraft:lava",
            "minecraft:frosted_ice",
            "minecraft:farmland",
            "minecraft:dirt_path",
            "minecraft:redstone_wire",
            "minecraft:tripwire",
            "minecraft:chorus_plant"
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

            if (!(item instanceof BlockItem blockItem)) {
                continue;
            }

            // 수집 감지는 BlockItem#getBlock() 기준으로 이루어진다.
            // 따라서 아이템이 실제로 이 블록 자신을 가리키는 경우만 도감 대상으로 인정한다.
            // 예: wall_head류, redstone_wire류 같은 상태 전용 블록이 섞이는 것을 방지.
            if (blockItem.getBlock() != block) {
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

    public static List<Block> getTargetBlocks() {
        return Collections.unmodifiableList(TARGET_BLOCKS);
    }

    public static Block getRandomTargetBlock() {
        if (TARGET_BLOCKS.isEmpty()) {
            return null;
        }

        int index = ThreadLocalRandom.current().nextInt(TARGET_BLOCKS.size());
        return TARGET_BLOCKS.get(index);
    }
}