package com.darkk0729.allblocks.client.screen;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import com.darkk0729.allblocks.challenge.ChallengeState;
import com.darkk0729.allblocks.collection.TargetBlockRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;

import java.util.ArrayList;
import java.util.List;

public final class BlockCodexScreen extends Screen {
    private static final Identifier CODEX_BACKGROUND_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "allblocks",
                    "textures/gui/block_codex_background.png"
            );

    private static final int CODEX_TEXTURE_WIDTH = 1412;
    private static final int CODEX_TEXTURE_HEIGHT = 1114;

    private static final int COLUMNS = 12;
    private static final int ROWS = 7;
    private static final int ITEMS_PER_PAGE = COLUMNS * ROWS;

    private static final int SLOT_SIZE = 20;
    private static final int SLOT_GAP = 2;

    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 300;

    private static final int HEADER_HEIGHT = 114;
    private static final int FOOTER_HEIGHT = 30;

    private static final int CLOSE_BUTTON_SIZE = 24;
    private static final int PLAYER_ICON_SIZE = 14;

    private static final int FILTER_Y_OFFSET = 95;

    private static final int COLOR_OVERLAY = 0x99000000;

    private static final int COLOR_SLOT = 0x66584634;
    private static final int COLOR_SLOT_HOVER = 0xAA8B6A43;
    private static final int COLOR_SLOT_CLAIMED = 0x8835A84A;   // 획득 블록 배경: 그냥 초록색
    private static final int COLOR_SLOT_UNCLAIMED = 0x665E5548;
    private static final int COLOR_SLOT_RELEASED = 0x666C506B;

    private static final int COLOR_TEXT = 0xFF3B2818;
    private static final int COLOR_TEXT_DIM = 0xFF78644C;

    private static final int DEFAULT_PLAYER_COLOR = 0xFF4EA3FF; // 임시 플레이어 고유 색(파랑)
    private static final int COLOR_UNCLAIMED = 0xFF71685B;
    private static final int COLOR_RELEASED = 0xFF8E4E84;
    private static final int COLOR_SELECTED = 0xFFE2B94B;
    private static final int COLOR_CLAIMED = DEFAULT_PLAYER_COLOR;

    private int page = 0;
    private CodexFilter filter = CodexFilter.ALL;

    private Block selectedBlock;
    private int selectedSlotX;
    private int selectedSlotY;

    public BlockCodexScreen() {
        super(Component.literal("블록 도감"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        List<Block> filteredBlocks = getFilteredBlocks();
        int maxPage = getMaxPage(filteredBlocks.size());

        if (page > maxPage) {
            page = maxPage;
        }

        int panelX = getPanelX();
        int panelY = getPanelY();

        drawBackground(graphics);
        drawMainPanel(graphics, panelX, panelY);
        drawHeader(graphics, panelX, panelY, filteredBlocks.size(), mouseX, mouseY);
        drawFilters(graphics, panelX, panelY, mouseX, mouseY);
        drawBlockGrid(graphics, panelX, panelY, mouseX, mouseY, filteredBlocks);
        drawFooter(graphics, panelX, panelY, maxPage, mouseX, mouseY);

        if (selectedBlock != null) {
            drawDetailPopup(graphics, panelX, panelY, selectedBlock);
        }

        Block hoveredBlock = getHoveredBlock(panelX, panelY, mouseX, mouseY, filteredBlocks);

        if (hoveredBlock != null && hoveredBlock != selectedBlock) {
            drawHoverNameTooltip(graphics, mouseX, mouseY, hoveredBlock);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }

        double mouseX = event.x();
        double mouseY = event.y();

        int panelX = getPanelX();
        int panelY = getPanelY();

        if (handleCloseClick(panelX, panelY, mouseX, mouseY)) {
            return true;
        }

        if (handleFilterClick(panelX, panelY, mouseX, mouseY)) {
            return true;
        }

        if (handlePageClick(panelX, panelY, mouseX, mouseY)) {
            return true;
        }

        if (handleBlockClick(panelX, panelY, mouseX, mouseY)) {
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_B) {
            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    private boolean handleCloseClick(int panelX, int panelY, double mouseX, double mouseY) {
        int x = getCloseButtonX(panelX);
        int y = getCloseButtonY(panelY);

        if (!isInside(mouseX, mouseY, x, y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)) {
            return false;
        }

        this.onClose();
        return true;
    }

    private void drawBackground(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, this.width, this.height, COLOR_OVERLAY);
    }

    private void drawMainPanel(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY
    ) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                CODEX_BACKGROUND_TEXTURE,

                panelX,
                panelY,

                0,
                0,

                PANEL_WIDTH,
                PANEL_HEIGHT,

                CODEX_TEXTURE_WIDTH,
                CODEX_TEXTURE_HEIGHT,

                CODEX_TEXTURE_WIDTH,
                CODEX_TEXTURE_HEIGHT
        );
    }

    private void drawHeader(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int filteredCount,
            int mouseX,
            int mouseY
    ) {
        int centerX =
                panelX + PANEL_WIDTH / 2;

        // 제목
        drawScaledCenteredBoldText(
                graphics,
                "블록 도감",
                centerX,
                panelY + 49,
                0xFF6B3D20,
                1.35F
        );

        drawCloseButton(
                graphics,
                panelX,
                panelY,
                mouseX,
                mouseY
        );

        String playerName =
                getHeaderPlayerName();

        int playerColor =
                getPlayerColor(playerName);

        // 플레이어 얼굴
        int playerX =
                centerX - PLAYER_ICON_SIZE / 2;

        int playerY =
                panelY + 67;

        drawPlayerIconSlot(
                graphics,
                playerX,
                playerY,
                playerColor
        );

        // 플레이어 개인 수집 개수
        String collectedText =
                Integer.toString(
                        getHeaderPlayerCollectedCount()
                );

        drawCenteredText(
                graphics,
                collectedText,
                centerX,
                panelY + 83,
                COLOR_TEXT,
                false
        );

        // 얼굴에 마우스를 올리면 닉네임
        if (isInside(
                mouseX,
                mouseY,
                playerX - 2,
                playerY - 2,
                PLAYER_ICON_SIZE + 4,
                PLAYER_ICON_SIZE + 4
        )) {
            drawTextTooltip(
                    graphics,
                    mouseX,
                    mouseY,
                    playerName
            );
        }
    }

    private void drawCloseButton(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY
    ) {
        int x = getCloseButtonX(panelX);
        int y = getCloseButtonY(panelY);

        boolean hovered =
                isInside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        CLOSE_BUTTON_SIZE,
                        CLOSE_BUTTON_SIZE
                );

        // X 자체는 배경 이미지에 이미 그려져 있다.
        // 마우스를 올렸을 때만 아주 약하게 강조한다.
        if (hovered) {
            graphics.fill(
                    x,
                    y,
                    x + CLOSE_BUTTON_SIZE,
                    y + CLOSE_BUTTON_SIZE,
                    0x22FFFFFF
            );

            graphics.outline(
                    x,
                    y,
                    CLOSE_BUTTON_SIZE,
                    CLOSE_BUTTON_SIZE,
                    0xAAFFD580
            );
        }
    }

    private void drawPlayerIconSlot(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int borderColor
    ) {
        // 얼굴 뒤 배경
        graphics.fill(
                x,
                y,
                x + PLAYER_ICON_SIZE,
                y + PLAYER_ICON_SIZE,
                0x88E4CFA7
        );

        if (this.minecraft != null && this.minecraft.player != null) {

            // Minecraft 26.2의 실제 플레이어 스킨 텍스처
            Identifier skinTexture =
                    this.minecraft.player
                            .getSkin()
                            .body()
                            .texturePath();

            // 스킨에서 정면 얼굴만 추출해서 GUI에 표시
            PlayerFaceExtractor.extractRenderState(
                    graphics,
                    skinTexture,
                    x,
                    y,
                    PLAYER_ICON_SIZE,
                    true,
                    false,
                    0xFFFFFFFF
            );
        }

        // 플레이어 고유 색 테두리
        drawThickOutline(
                graphics,
                x,
                y,
                PLAYER_ICON_SIZE,
                PLAYER_ICON_SIZE,
                borderColor
        );
    }

    private void drawCenteredText(
            GuiGraphicsExtractor graphics,
            String text,
            int centerX,
            int y,
            int color,
            boolean shadow
    ) {
        int textX = centerX - this.font.width(text) / 2;
        graphics.text(this.font, text, textX, y, color, shadow);
    }

    private void drawScaledCenteredText(
            GuiGraphicsExtractor graphics,
            String text,
            int centerX,
            int y,
            int color,
            boolean shadow,
            float scale
    ) {
        var matrices = graphics.pose();

        float textX = centerX - (this.font.width(text) * scale) / 2.0F;

        matrices.pushMatrix();
        matrices.translate(textX, y);
        matrices.scale(scale, scale);

        graphics.text(this.font, text, 0, 0, color, shadow);

        matrices.popMatrix();
    }

    private void drawScaledCenteredBoldText(
            GuiGraphicsExtractor graphics,
            String text,
            int centerX,
            int y,
            int color,
            float scale
    ) {
        var matrices = graphics.pose();

        float textX =
                centerX
                        - (this.font.width(text) * scale) / 2.0F;

        matrices.pushMatrix();

        matrices.translate(
                textX,
                y
        );

        matrices.scale(
                scale,
                scale
        );

        // 기본 글자
        graphics.text(
                this.font,
                text,
                0,
                0,
                color,
                false
        );

        // 오른쪽으로 1px 겹쳐서 굵게
        graphics.text(
                this.font,
                text,
                1,
                0,
                color,
                false
        );

        matrices.popMatrix();
    }

    private int getCloseButtonX(int panelX) {
        return panelX + PANEL_WIDTH - CLOSE_BUTTON_SIZE - 15;
    }

    private int getCloseButtonY(int panelY) {
        return panelY + 13;
    }

    private void drawFilters(GuiGraphicsExtractor graphics, int panelX, int panelY, int mouseX, int mouseY) {
        int totalFilterWidth = 40 + 5 + 42 + 5 + 50 + 5 + 42;
        int x = panelX + (PANEL_WIDTH - totalFilterWidth) / 2;
        int y = panelY + FILTER_Y_OFFSET;

        for (CodexFilter currentFilter : CodexFilter.values()) {
            int width = currentFilter.width;
            boolean hovered = isInside(mouseX, mouseY, x, y, width, 15);
            boolean selected = filter == currentFilter;

            int bgColor;

            if (selected) {
                bgColor = 0xCC7C4C28;
            } else if (hovered) {
                bgColor = 0x88D6B980;
            } else {
                bgColor = 0x55E7D2AA;
            }

            int borderColor =
                    selected
                            ? 0xFFE0B35A
                            : 0xFF8C6740;

            int textColor =
                    selected
                            ? 0xFFFFF1CE
                            : COLOR_TEXT;

            graphics.fill(
                    x,
                    y,
                    x + width,
                    y + 15,
                    bgColor
            );

            graphics.outline(
                    x,
                    y,
                    width,
                    15,
                    borderColor
            );

            graphics.text(
                    this.font,
                    currentFilter.label,
                    x + 5,
                    y + 4,
                    textColor,
                    false
            );

            x += width + 5;
        }
    }

    private void drawBlockGrid(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int mouseX,
            int mouseY,
            List<Block> filteredBlocks
    ) {
        int gridX = getGridX(panelX);
        int gridY = getGridY(panelY);

        int startIndex = page * ITEMS_PER_PAGE;

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int indexInPage = row * COLUMNS + column;
                int blockIndex = startIndex + indexInPage;

                int slotX = gridX + column * (SLOT_SIZE + SLOT_GAP);
                int slotY = gridY + row * (SLOT_SIZE + SLOT_GAP);

                if (blockIndex >= filteredBlocks.size()) {
                    drawEmptySlot(graphics, slotX, slotY);
                    continue;
                }

                Block block = filteredBlocks.get(blockIndex);
                drawBlockSlot(graphics, block, slotX, slotY, mouseX, mouseY);
            }
        }
    }

    private void drawEmptySlot(
            GuiGraphicsExtractor graphics,
            int x,
            int y
    ) {
        graphics.fill(
                x,
                y,
                x + SLOT_SIZE,
                y + SLOT_SIZE,
                0x3349362A
        );

        graphics.outline(
                x,
                y,
                SLOT_SIZE,
                SLOT_SIZE,
                0x88705A40
        );
    }

    private void drawBlockSlot(GuiGraphicsExtractor graphics, Block block, int x, int y, int mouseX, int mouseY) {
        String blockId = getBlockId(block);
        BlockStatus status = getBlockStatus(blockId);

        boolean hovered = isInside(mouseX, mouseY, x, y, SLOT_SIZE, SLOT_SIZE);
        boolean selected = block == selectedBlock;

        int slotBackgroundColor = switch (status) {
            case CLAIMED -> COLOR_SLOT_CLAIMED;
            case RELEASED -> COLOR_SLOT_RELEASED;
            case UNCLAIMED -> COLOR_SLOT_UNCLAIMED;
        };

        if (hovered) {
            slotBackgroundColor = COLOR_SLOT_HOVER;
        }

        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, slotBackgroundColor);

        int borderColor = switch (status) {
            case CLAIMED -> getOwnerColor(blockId);
            case RELEASED -> COLOR_RELEASED;
            case UNCLAIMED -> COLOR_UNCLAIMED;
        };

        if (status == BlockStatus.CLAIMED) {
            drawThickOutline(
                    graphics,
                    x,
                    y,
                    SLOT_SIZE,
                    SLOT_SIZE,
                    borderColor
            );
        } else {
            graphics.outline(
                    x,
                    y,
                    SLOT_SIZE,
                    SLOT_SIZE,
                    borderColor
            );
        }

        if (selected) {
            graphics.outline(x - 1, y - 1, SLOT_SIZE + 2, SLOT_SIZE + 2, COLOR_SELECTED);
        }

        ItemStack stack = new ItemStack(block);
        graphics.item(stack, x + 2, y + 2);

        if (status == BlockStatus.UNCLAIMED) {
            graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0x44000000);
        }

        if (status == BlockStatus.RELEASED) {
            graphics.text(this.font, "!", x + 13, y + 10, 0xFFFF66FF, false);
        }
    }

    private void drawFooter(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelY,
            int maxPage,
            int mouseX,
            int mouseY
    ) {
        int centerX =
                panelX + PANEL_WIDTH / 2;

        // 우상단 페이지 표시
        String pageText =
                (page + 1)
                        + " / "
                        + (maxPage + 1);

        int pageTextX =
                panelX
                        + PANEL_WIDTH
                        - 28
                        - this.font.width(pageText);

        graphics.text(
                this.font,
                pageText,
                pageTextX,
                panelY + 49,
                COLOR_TEXT_DIM,
                false
        );

        // 하단에는 화살표만
        int buttonY =
                panelY + PANEL_HEIGHT - 29;

        int prevX =
                centerX - 36;

        int nextX =
                centerX + 12;

        drawThinPageButton(
                graphics,
                prevX,
                buttonY,
                24,
                14,
                "<",
                mouseX,
                mouseY,
                page > 0
        );

        drawThinPageButton(
                graphics,
                nextX,
                buttonY,
                24,
                14,
                ">",
                mouseX,
                mouseY,
                page < maxPage
        );
    }

    private void drawThinPageButton(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            String text,
            int mouseX,
            int mouseY,
            boolean enabled
    ) {
        boolean hovered =
                enabled
                        && isInside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        width,
                        height
                );

        int textColor;

        if (!enabled) {
            textColor = 0x6678644C;
        } else if (hovered) {
            textColor = 0xFF5C2F18;
        } else {
            textColor = 0xFF7A5738;
        }

        int textX =
                x
                        + width / 2
                        - this.font.width(text) / 2;

        graphics.text(
                this.font,
                text,
                textX,
                y + 3,
                textColor,
                false
        );
    }

    private void drawDetailPopup(GuiGraphicsExtractor graphics, int panelX, int panelY, Block block) {
        int popupWidth = 158;
        int popupHeight = 104;

        boolean selectedOnLeft = selectedSlotX < panelX + (PANEL_WIDTH / 2);

        int popupX = selectedOnLeft
                ? selectedSlotX + SLOT_SIZE + 8
                : selectedSlotX - popupWidth - 8;

        int popupY = selectedSlotY;

        int minY = panelY + 6;
        int maxY = panelY + PANEL_HEIGHT - popupHeight - 6;

        if (popupY < minY) {
            popupY = minY;
        }

        if (popupY > maxY) {
            popupY = maxY;
        }

        graphics.fill(
                popupX,
                popupY,
                popupX + popupWidth,
                popupY + popupHeight,
                0xFFF1DEB6
        );

        graphics.outline(
                popupX,
                popupY,
                popupWidth,
                popupHeight,
                0xFF7E4E2B
        );

        String blockId = getBlockId(block);
        ItemStack stack = new ItemStack(block);
        String displayName = stack.getHoverName().getString();
        String shortBlockId = getDisplayBlockId(blockId);

        BlockStatus status = getBlockStatus(blockId);
        String owner = getOwnerName(blockId);
        String realm = guessRealmHint(blockId);

        graphics.item(stack, popupX + 8, popupY + 8);

        graphics.text(this.font, displayName, popupX + 30, popupY + 9, COLOR_TEXT, false);
        graphics.text(this.font, fitText(shortBlockId, 138), popupX + 8, popupY + 27, COLOR_TEXT_DIM, false);

        graphics.text(this.font, "상태: " + status.label, popupX + 8, popupY + 43, status.color, false);
        graphics.text(this.font, "소유자: " + owner, popupX + 8, popupY + 56, COLOR_TEXT, false);

        if (status == BlockStatus.CLAIMED) {
            graphics.text(this.font, "위치: " + realm + " / 상세 예정", popupX + 8, popupY + 70, COLOR_TEXT_DIM, false);
            graphics.text(this.font, "도구: 데이터 예정", popupX + 8, popupY + 83, COLOR_TEXT_DIM, false);
        } else {
            graphics.text(this.font, "위치: " + realm, popupX + 8, popupY + 70, COLOR_TEXT_DIM, false);
            graphics.text(this.font, "도구: ???", popupX + 8, popupY + 83, COLOR_TEXT_DIM, false);
        }
    }

    private void drawHoverNameTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Block block) {
        ItemStack stack = new ItemStack(block);
        String displayName = stack.getHoverName().getString();

        int padding = 5;
        int tooltipWidth = this.font.width(displayName) + padding * 2;
        int tooltipHeight = 16;

        int tooltipX = mouseX + 10;
        int tooltipY = mouseY + 10;

        if (tooltipX + tooltipWidth > this.width) {
            tooltipX = mouseX - tooltipWidth - 10;
        }

        if (tooltipY + tooltipHeight > this.height) {
            tooltipY = mouseY - tooltipHeight - 10;
        }

        graphics.fill(
                tooltipX,
                tooltipY,
                tooltipX + tooltipWidth,
                tooltipY + tooltipHeight,
                0xFFF1DEB6
        );

        graphics.outline(
                tooltipX,
                tooltipY,
                tooltipWidth,
                tooltipHeight,
                0xFF7E4E2B
        );

        graphics.text(
                this.font,
                displayName,
                tooltipX + padding,
                tooltipY + 4,
                COLOR_TEXT,
                false
        );
    }

    private void drawTextTooltip(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            String text
    ) {
        if (text == null || text.isBlank()) {
            return;
        }

        int padding = 5;
        int tooltipWidth = this.font.width(text) + padding * 2;
        int tooltipHeight = 16;

        int tooltipX = mouseX + 10;
        int tooltipY = mouseY + 10;

        if (tooltipX + tooltipWidth > this.width) {
            tooltipX = mouseX - tooltipWidth - 10;
        }

        if (tooltipY + tooltipHeight > this.height) {
            tooltipY = mouseY - tooltipHeight - 10;
        }

        graphics.fill(
                tooltipX,
                tooltipY,
                tooltipX + tooltipWidth,
                tooltipY + tooltipHeight,
                0xFFF1DEB6
        );

        graphics.outline(
                tooltipX,
                tooltipY,
                tooltipWidth,
                tooltipHeight,
                0xFF7E4E2B
        );

        graphics.text(
                this.font,
                text,
                tooltipX + padding,
                tooltipY + 4,
                COLOR_TEXT,
                false
        );
    }

    private boolean handleFilterClick(int panelX, int panelY, double mouseX, double mouseY) {
        int totalFilterWidth = 40 + 5 + 42 + 5 + 50 + 5 + 42;
        int x = panelX + (PANEL_WIDTH - totalFilterWidth) / 2;
        int y = panelY + FILTER_Y_OFFSET;

        for (CodexFilter currentFilter : CodexFilter.values()) {
            int width = currentFilter.width;

            if (isInside(mouseX, mouseY, x, y, width, 15)) {
                filter = currentFilter;
                page = 0;
                selectedBlock = null;
                return true;
            }

            x += width + 5;
        }

        return false;
    }

    private boolean handlePageClick(
            int panelX,
            int panelY,
            double mouseX,
            double mouseY
    ) {
        List<Block> filteredBlocks =
                getFilteredBlocks();

        int maxPage =
                getMaxPage(filteredBlocks.size());

        int centerX =
                panelX + PANEL_WIDTH / 2;

        int buttonY =
                panelY + PANEL_HEIGHT - 29;

        int prevX =
                centerX - 36;

        int nextX =
                centerX + 12;

        if (isInside(
                mouseX,
                mouseY,
                prevX,
                buttonY,
                24,
                14
        ) && page > 0) {

            page--;
            selectedBlock = null;

            return true;
        }

        if (isInside(
                mouseX,
                mouseY,
                nextX,
                buttonY,
                24,
                14
        ) && page < maxPage) {

            page++;
            selectedBlock = null;

            return true;
        }

        return false;
    }

    private boolean handleBlockClick(int panelX, int panelY, double mouseX, double mouseY) {
        List<Block> filteredBlocks = getFilteredBlocks();

        int gridX = getGridX(panelX);
        int gridY = getGridY(panelY);
        int startIndex = page * ITEMS_PER_PAGE;

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int indexInPage = row * COLUMNS + column;
                int blockIndex = startIndex + indexInPage;

                if (blockIndex >= filteredBlocks.size()) {
                    continue;
                }

                int slotX = gridX + column * (SLOT_SIZE + SLOT_GAP);
                int slotY = gridY + row * (SLOT_SIZE + SLOT_GAP);

                if (!isInside(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE)) {
                    continue;
                }

                Block clickedBlock = filteredBlocks.get(blockIndex);

                // 이미 선택된 블록을 다시 클릭하면 상세 팝업을 닫는다.
                if (selectedBlock == clickedBlock) {
                    selectedBlock = null;
                    return true;
                }

                selectedBlock = clickedBlock;
                selectedSlotX = slotX;
                selectedSlotY = slotY;

                return true;
            }
        }

        return false;
    }

    private Block getHoveredBlock(
            int panelX,
            int panelY,
            double mouseX,
            double mouseY,
            List<Block> filteredBlocks
    ) {
        int gridX = getGridX(panelX);
        int gridY = getGridY(panelY);
        int startIndex = page * ITEMS_PER_PAGE;

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int indexInPage = row * COLUMNS + column;
                int blockIndex = startIndex + indexInPage;

                if (blockIndex >= filteredBlocks.size()) {
                    continue;
                }

                int slotX = gridX + column * (SLOT_SIZE + SLOT_GAP);
                int slotY = gridY + row * (SLOT_SIZE + SLOT_GAP);

                if (isInside(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE)) {
                    return filteredBlocks.get(blockIndex);
                }
            }
        }

        return null;
    }

    private List<Block> getFilteredBlocks() {
        List<Block> result = new ArrayList<>();

        for (Block block : TargetBlockRegistry.getTargetBlocks()) {
            String blockId = getBlockId(block);
            BlockStatus status = getBlockStatus(blockId);

            if (filter.matches(status)) {
                result.add(block);
            }
        }

        return result;
    }

    private BlockStatus getBlockStatus(String blockId) {
        ChallengeState.CollectedBlockData data = ChallengeManager.getBlockCollectionData(blockId);

        if (data == null || data.state == null) {
            return BlockStatus.UNCLAIMED;
        }

        String stateName = data.state.name();

        if ("CLAIMED".equals(stateName)) {
            return BlockStatus.CLAIMED;
        }

        if ("RELEASED".equals(stateName)) {
            return BlockStatus.RELEASED;
        }

        return BlockStatus.UNCLAIMED;
    }

    private String getOwnerName(String blockId) {
        ChallengeState.CollectedBlockData data = ChallengeManager.getBlockCollectionData(blockId);

        if (data == null || data.ownerName == null || data.ownerName.isBlank()) {
            return "없음";
        }

        return data.ownerName;
    }

    private String getHeaderPlayerName() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return "플레이어";
        }

        return this.minecraft.player.getName().getString();
    }

    private int getHeaderPlayerCollectedCount() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return 0;
        }

        String playerUuid =
                this.minecraft.player.getUUID().toString();

        int count = 0;

        for (Block block : TargetBlockRegistry.getTargetBlocks()) {
            String blockId = getBlockId(block);

            ChallengeState.CollectedBlockData data =
                    ChallengeManager.getBlockCollectionData(blockId);

            if (data == null || data.state == null) {
                continue;
            }

            if (data.state != ChallengeState.BlockCollectionState.CLAIMED) {
                continue;
            }

            if (!playerUuid.equals(data.ownerUuid)) {
                continue;
            }

            count++;
        }

        return count;
    }

    private int getPlayerColor(String playerName) {
        // 지금은 임시로 하나의 고정색 사용
        // 나중에 멀티플레이 색 분기 시 여기만 바꾸면 됨
        return DEFAULT_PLAYER_COLOR;
    }

    private int getOwnerColor(String blockId) {
        ChallengeState.CollectedBlockData data =
                ChallengeManager.getBlockCollectionData(blockId);

        if (data == null
                || data.ownerUuid == null
                || data.ownerUuid.isBlank()) {
            return DEFAULT_PLAYER_COLOR;
        }

        return getPlayerColor(data.ownerUuid);
    }

    private String getDisplayBlockId(String blockId) {
        if (blockId == null) {
            return "unknown";
        }

        if (blockId.startsWith("minecraft:")) {
            return blockId.substring("minecraft:".length());
        }

        return blockId;
    }

    private String fitText(String text, int maxWidth) {
        if (text == null) {
            return "";
        }

        if (this.font.width(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        int suffixWidth = this.font.width(suffix);

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            String next = builder.toString() + text.charAt(i);

            if (this.font.width(next) + suffixWidth > maxWidth) {
                break;
            }

            builder.append(text.charAt(i));
        }

        return builder + suffix;
    }

    private String guessRealmHint(String blockId) {
        if (blockId.contains("nether")
                || blockId.contains("crimson")
                || blockId.contains("warped")
                || blockId.contains("blackstone")
                || blockId.contains("basalt")
                || blockId.contains("netherrack")
                || blockId.contains("soul_")
                || blockId.contains("ancient_debris")
                || blockId.contains("quartz")) {
            return "네더";
        }

        if (blockId.contains("end_")
                || blockId.contains("purpur")
                || blockId.contains("chorus")) {
            return "엔드";
        }

        return "오버월드";
    }

    private String getBlockId(Block block) {
        var key = BuiltInRegistries.BLOCK.getKey(block);
        return key == null ? "unknown" : key.toString();
    }

    private int getMaxPage(int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }

        return Math.max(0, (itemCount - 1) / ITEMS_PER_PAGE);
    }

    private int getPanelX() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int getPanelY() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    private int getGridX(int panelX) {
        int gridWidth = COLUMNS * SLOT_SIZE + (COLUMNS - 1) * SLOT_GAP;
        return panelX + (PANEL_WIDTH - gridWidth) / 2;
    }

    private int getGridY(int panelY) {
        return panelY + HEADER_HEIGHT;
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }

    private void drawThickOutline(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        graphics.outline(
                x - 2,
                y - 2,
                width + 4,
                height + 4,
                color
        );

        graphics.outline(
                x - 1,
                y - 1,
                width + 2,
                height + 2,
                color
        );

        graphics.outline(
                x,
                y,
                width,
                height,
                color
        );
    }

    private enum CodexFilter {
        ALL("전체", 40),
        CLAIMED("획득", 42),
        UNCLAIMED("미획득", 50),
        RELEASED("분실", 42);

        private final String label;
        private final int width;

        CodexFilter(String label, int width) {
            this.label = label;
            this.width = width;
        }

        private boolean matches(BlockStatus status) {
            return switch (this) {
                case ALL -> true;
                case CLAIMED -> status == BlockStatus.CLAIMED;
                case UNCLAIMED -> status == BlockStatus.UNCLAIMED || status == BlockStatus.RELEASED;
                case RELEASED -> status == BlockStatus.RELEASED;
            };
        }
    }

    private enum BlockStatus {
        CLAIMED("획득 완료", COLOR_CLAIMED),
        UNCLAIMED("미획득", COLOR_UNCLAIMED),
        RELEASED("잃어버림", COLOR_RELEASED);

        private final String label;
        private final int color;

        BlockStatus(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }
}