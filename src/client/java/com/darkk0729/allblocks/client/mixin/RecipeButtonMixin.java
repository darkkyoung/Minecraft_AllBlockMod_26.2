package com.darkk0729.allblocks.client.mixin;

import com.darkk0729.allblocks.client.data.ClientChallengeStateCache;
import com.darkk0729.allblocks.collection.TargetBlockRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeButton.class)
public abstract class RecipeButtonMixin {
    private static final int ALLBLOCKS_CLAIMED_OVERLAY =
            0x6635A84A;

    @Inject(
            method = "extractWidgetRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeButton;getDisplayStack()Lnet/minecraft/world/item/ItemStack;",
                    shift = At.Shift.BEFORE
            )
    )
    private void allblocks$highlightClaimedRecipe(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        RecipeButton button =
                (RecipeButton) (Object) this;

        ItemStack stack =
                button.getDisplayStack();

        if (!(stack.getItem()
                instanceof BlockItem blockItem)) {
            return;
        }

        String blockId =
                BuiltInRegistries.BLOCK
                        .getKey(
                                blockItem.getBlock()
                        )
                        .toString();

        if (!TargetBlockRegistry
                .isTargetBlock(blockId)) {
            return;
        }

        ClientChallengeStateCache.SyncedBlockData data =
                ClientChallengeStateCache
                        .getBlockData(blockId);

        if (data == null
                || !data.isClaimed()) {
            return;
        }

        // Minecraft 26.2의 GUI는 즉시 그리지 않고 렌더 상태를 모아 처리한다.
        // 명시적으로 레이어를 분리해서
        // 바닐라 버튼 배경 < 초록 오버레이 < 아이템 아이콘 순서를 보장한다.
        graphics.nextStratum();

        graphics.fill(
                button.getX() + 2,
                button.getY() + 2,
                button.getX()
                        + button.getWidth()
                        - 2,
                button.getY() + 23,
                ALLBLOCKS_CLAIMED_OVERLAY
        );

        graphics.nextStratum();
    }
}
