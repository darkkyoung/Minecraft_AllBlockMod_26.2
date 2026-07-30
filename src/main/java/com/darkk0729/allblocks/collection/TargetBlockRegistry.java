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

    private static final Set<String> FILL_EVENT_EXCLUDED_BLOCK_IDS = Set.of(
            // Support-required / unstable redstone or utility blocks
            "minecraft:lever",
            "minecraft:tripwire_hook",
            "minecraft:tripwire",
            "minecraft:ladder",
            "minecraft:scaffolding",
            "minecraft:repeater",
            "minecraft:comparator",

            // Common plants / vegetation
            "minecraft:grass",
            "minecraft:tall_grass",
            "minecraft:fern",
            "minecraft:large_fern",
            "minecraft:dead_bush",

            "minecraft:dandelion",
            "minecraft:poppy",
            "minecraft:blue_orchid",
            "minecraft:allium",
            "minecraft:azure_bluet",
            "minecraft:red_tulip",
            "minecraft:orange_tulip",
            "minecraft:white_tulip",
            "minecraft:pink_tulip",
            "minecraft:oxeye_daisy",
            "minecraft:cornflower",
            "minecraft:lily_of_the_valley",
            "minecraft:wither_rose",
            "minecraft:torchflower",

            "minecraft:sunflower",
            "minecraft:lilac",
            "minecraft:rose_bush",
            "minecraft:peony",
            "minecraft:pitcher_plant",

            "minecraft:brown_mushroom",
            "minecraft:red_mushroom",
            "minecraft:crimson_fungus",
            "minecraft:warped_fungus",
            "minecraft:crimson_roots",
            "minecraft:warped_roots",
            "minecraft:nether_sprouts",

            "minecraft:hanging_roots",
            "minecraft:spore_blossom",
            "minecraft:pink_petals",
            "minecraft:wildflowers",
            "minecraft:leaf_litter",
            "minecraft:short_dry_grass",
            "minecraft:tall_dry_grass",
            "minecraft:bush",
            "minecraft:firefly_bush",
            "minecraft:cactus_flower",

            // Crops / stems / vines
            "minecraft:wheat",
            "minecraft:carrots",
            "minecraft:potatoes",
            "minecraft:beetroots",
            "minecraft:melon_stem",
            "minecraft:pumpkin_stem",
            "minecraft:attached_melon_stem",
            "minecraft:attached_pumpkin_stem",
            "minecraft:sweet_berry_bush",

            "minecraft:cave_vines",
            "minecraft:cave_vines_plant",
            "minecraft:weeping_vines",
            "minecraft:weeping_vines_plant",
            "minecraft:twisting_vines",
            "minecraft:twisting_vines_plant",
            "minecraft:vine",
            "minecraft:glow_lichen",

            "minecraft:sugar_cane",
            "minecraft:bamboo",
            "minecraft:bamboo_sapling",
            "minecraft:cactus",
            "minecraft:cocoa",

            "minecraft:seagrass",
            "minecraft:tall_seagrass",
            "minecraft:kelp",
            "minecraft:kelp_plant",
            "minecraft:sea_pickle",
            "minecraft:lily_pad",

            "minecraft:small_dripleaf",
            "minecraft:big_dripleaf",
            "minecraft:big_dripleaf_stem",
            "minecraft:pointed_dripstone",

            // Decoration blocks that depend on support / orientation
            "minecraft:flower_pot",

            // Extra unstable plant-like blocks
            "minecraft:azalea",
            "minecraft:flowering_azalea",
            "minecraft:mangrove_propagule",

            // Amethyst clusters / buds. Collectible, but bad for mass fill events.
            "minecraft:small_amethyst_bud",
            "minecraft:medium_amethyst_bud",
            "minecraft:large_amethyst_bud",
            "minecraft:amethyst_cluster",

            // Thin / attachment-sensitive blocks
            "minecraft:snow",
            "minecraft:sculk_vein",
            "minecraft:lantern",
            "minecraft:soul_lantern",
            "minecraft:bell",

            // Extra crop / plant blocks
            "minecraft:nether_wart",
            "minecraft:torchflower_crop",
            "minecraft:pitcher_crop",
            "minecraft:chorus_flower"
    );

    private static final List<String> TARGET_BLOCK_IDS = new ArrayList<>();
    private static final List<Block> TARGET_BLOCKS = new ArrayList<>();
    private static final List<Block> FILL_EVENT_BLOCKS = new ArrayList<>();

    private static boolean initialized = false;

    private TargetBlockRegistry() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        TARGET_BLOCK_IDS.clear();
        TARGET_BLOCKS.clear();
        FILL_EVENT_BLOCKS.clear();

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

            if (!isExcludedFromFillEvent(id)) {
                FILL_EVENT_BLOCKS.add(block);
            }
        }

        initialized = true;

        AllBlocksMod.LOGGER.info(
                "AllBlocks target blocks loaded: {}, fill event blocks loaded: {}",
                TARGET_BLOCK_IDS.size(),
                FILL_EVENT_BLOCKS.size()
        );
    }

    private static boolean isExcludedFromFillEvent(String blockId) {
        if (blockId == null) {
            return true;
        }

        String path = blockId;

        int namespaceIndex = blockId.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex + 1 < blockId.length()) {
            path = blockId.substring(namespaceIndex + 1);
        }

        // 1. 버튼 / 압력판 / 횃불 / 레일류
        if (path.endsWith("_button")
                || path.endsWith("_pressure_plate")
                || path.endsWith("_torch")
                || path.endsWith("_rail")) {
            return true;
        }

        // 2. 묘목 / 주아류
        if (path.endsWith("_sapling")
                || path.endsWith("_propagule")) {
            return true;
        }

        // 3. 문 종류
        // trapdoor는 oak_trapdoor처럼 끝이 "_door"가 아니라 "pdoor"라서 여기에는 안 걸림.
        if (path.endsWith("_door")) {
            return true;
        }

        // 4. 표지판 / 배너 / 카펫 / 양초류
        if (path.endsWith("_sign")
                || path.endsWith("_banner")
                || path.endsWith("_carpet")
                || path.endsWith("_candle")
                || path.contains("candle_cake")) {
            return true;
        }

        // 5. 머리 / 해골류
        if (path.endsWith("_skull")
                || path.endsWith("_wall_skull")
                || path.endsWith("_head")
                || path.endsWith("_wall_head")) {
            return true;
        }

        // 6. 화분류
        if (path.startsWith("potted_")) {
            return true;
        }

        // 7. 산호 식물 / 산호 부채류
        if (path.endsWith("_coral")
                || path.endsWith("_coral_fan")
                || path.endsWith("_coral_wall_fan")) {
            return true;
        }

        return FILL_EVENT_EXCLUDED_BLOCK_IDS.contains(blockId);
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

    public static Block getRandomFillEventBlock() {
        if (FILL_EVENT_BLOCKS.isEmpty()) {
            return getRandomTargetBlock();
        }

        int index = ThreadLocalRandom.current().nextInt(FILL_EVENT_BLOCKS.size());
        return FILL_EVENT_BLOCKS.get(index);
    }

    public static int getFillEventBlockCount() {
        return FILL_EVENT_BLOCKS.size();
    }
}