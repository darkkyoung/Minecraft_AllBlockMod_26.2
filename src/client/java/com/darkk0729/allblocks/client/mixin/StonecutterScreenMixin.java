package com.darkk0729.allblocks.client.mixin;

import com.darkk0729.allblocks.client.data.ClientChallengeStateCache;
import com.darkk0729.allblocks.collection.TargetBlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(StonecutterScreen.class)
public abstract class StonecutterScreenMixin {
    private static final int ALLBLOCKS_CLAIMED_OVERLAY =
            0x665ECF63;

    @Shadow
    private int startIndex;

    @Shadow
    @Final
    private static int RECIPES_COLUMNS;

    @Shadow
    @Final
    private static int RECIPES_IMAGE_SIZE_WIDTH;

    @Shadow
    @Final
    private static int RECIPES_IMAGE_SIZE_HEIGHT;

    @Inject(
            method = "extractRecipes",
            at = @At("HEAD")
    )
    private void allblocks$highlightClaimedStonecutterRecipes(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int endIndex,
            CallbackInfo ci
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        StonecutterScreen screen =
                (StonecutterScreen) (Object) this;

        List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>>
                entries =
                screen.getMenu()
                        .getVisibleRecipes()
                        .entries();

        if (entries.isEmpty()) {
            return;
        }

        ContextMap context =
                SlotDisplayContext.fromLevel(
                        minecraft.level
                );

        boolean openedOverlayStratum = false;

        int safeEndIndex =
                Math.min(
                        endIndex,
                        entries.size()
                );

        for (int recipeIndex = startIndex;
             recipeIndex < safeEndIndex;
             recipeIndex++) {
            ItemStack stack =
                    entries.get(recipeIndex)
                            .recipe()
                            .optionDisplay()
                            .resolveForFirstStack(
                                    context
                            );

            if (!(stack.getItem()
                    instanceof BlockItem blockItem)) {
                continue;
            }

            String blockId =
                    BuiltInRegistries.BLOCK
                            .getKey(
                                    blockItem.getBlock()
                            )
                            .toString();

            if (!TargetBlockRegistry
                    .isTargetBlock(blockId)) {
                continue;
            }

            ClientChallengeStateCache.SyncedBlockData data =
                    ClientChallengeStateCache
                            .getBlockData(blockId);

            if (data == null
                    || !data.isClaimed()) {
                continue;
            }

            if (!openedOverlayStratum) {
                // 석재 절단기 버튼 배경 위,
                // 아이템 아이콘 아래에 연두색 표시를 둔다.
                graphics.nextStratum();
                openedOverlayStratum = true;
            }

            int visibleIndex =
                    recipeIndex - startIndex;

            int slotX =
                    x
                            + (visibleIndex
                            % RECIPES_COLUMNS)
                            * RECIPES_IMAGE_SIZE_WIDTH;

            int slotY =
                    y
                            + (visibleIndex
                            / RECIPES_COLUMNS)
                            * RECIPES_IMAGE_SIZE_HEIGHT;

            graphics.fill(
                    slotX + 1,
                    slotY + 1,
                    slotX
                            + RECIPES_IMAGE_SIZE_WIDTH
                            - 1,
                    slotY
                            + RECIPES_IMAGE_SIZE_HEIGHT
                            - 1,
                    ALLBLOCKS_CLAIMED_OVERLAY
            );
        }

        if (openedOverlayStratum) {
            // 뒤이어 바닐라가 그리는 결과 아이콘을
            // 오버레이보다 위 레이어에 배치한다.
            graphics.nextStratum();
        }
    }
}
