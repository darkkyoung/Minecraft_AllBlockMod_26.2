package com.darkk0729.allblocks.client.toast;

import com.darkk0729.allblocks.collection.TargetBlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class CodexRegistrationToast implements Toast {
    private static final long DISPLAY_TIME_MILLIS = 5000L;

    private static final int WIDTH = 160;
    private static final int HEIGHT = 32;

    private static final int COLOR_BG = 0xEE202020;
    private static final int COLOR_BG_DARK = 0xEE141414;
    private static final int COLOR_BORDER_GOLD = 0xFFFFD85A;
    private static final int COLOR_BORDER_DARK_GOLD = 0xFF8A5A00;
    private static final int COLOR_TITLE = 0xFFFFD85A;
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    private final ItemStack stack;
    private final String blockName;

    private Visibility visibility = Visibility.SHOW;

    private CodexRegistrationToast(ItemStack stack, String blockName) {
        this.stack = stack;
        this.blockName = blockName == null || blockName.isBlank()
                ? "알 수 없는 블록"
                : blockName;
    }

    public static void show(String blockId) {
        Minecraft client = Minecraft.getInstance();

        if (client == null || client.gui == null) {
            return;
        }

        ItemStack stack = findStack(blockId);
        String blockName = stack.isEmpty()
                ? prettifyBlockId(blockId)
                : stack.getHoverName().getString();

        client.gui.toastManager().addToast(new CodexRegistrationToast(stack, blockName));
    }

    @Override
    public Visibility getWantedVisibility() {
        return visibility;
    }

    @Override
    public void update(ToastManager manager, long time) {
        double multiplier = manager.getNotificationDisplayTimeMultiplier();

        if (time >= DISPLAY_TIME_MILLIS * multiplier) {
            visibility = Visibility.HIDE;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long time) {
        drawPanel(graphics);

        if (!stack.isEmpty()) {
            graphics.item(stack, 8, 8);
        }

        graphics.text(font, "도감 등록", 31, 7, COLOR_TITLE, false);

        String name = shorten(font, blockName, 120);
        graphics.text(font, name, 31, 19, COLOR_TEXT, false);
    }

    @Override
    public int width() {
        return WIDTH;
    }

    @Override
    public int height() {
        return HEIGHT;
    }

    private static void drawPanel(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, WIDTH, HEIGHT, COLOR_BG);
        graphics.fill(1, 1, WIDTH - 1, HEIGHT - 1, COLOR_BG_DARK);

        // 바깥 금색 테두리
        graphics.fill(0, 0, WIDTH, 1, COLOR_BORDER_GOLD);
        graphics.fill(0, HEIGHT - 1, WIDTH, HEIGHT, COLOR_BORDER_DARK_GOLD);
        graphics.fill(0, 0, 1, HEIGHT, COLOR_BORDER_GOLD);
        graphics.fill(WIDTH - 1, 0, WIDTH, HEIGHT, COLOR_BORDER_DARK_GOLD);

        // 아이콘 박스
        graphics.fill(6, 5, 26, 27, 0xAA000000);
        graphics.fill(6, 5, 26, 6, COLOR_BORDER_DARK_GOLD);
        graphics.fill(6, 26, 26, 27, COLOR_BORDER_DARK_GOLD);
        graphics.fill(6, 5, 7, 27, COLOR_BORDER_DARK_GOLD);
        graphics.fill(25, 5, 26, 27, COLOR_BORDER_DARK_GOLD);
    }

    private static ItemStack findStack(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return ItemStack.EMPTY;
        }

        for (Block block : TargetBlockRegistry.getTargetBlocks()) {
            var key = BuiltInRegistries.BLOCK.getKey(block);

            if (key == null) {
                continue;
            }

            if (blockId.equals(key.toString())) {
                return new ItemStack(block);
            }
        }

        return ItemStack.EMPTY;
    }

    private static String shorten(Font font, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return "";
        }

        if (font.width(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        String current = text;

        while (!current.isEmpty() && font.width(current + suffix) > maxWidth) {
            current = current.substring(0, current.length() - 1);
        }

        return current + suffix;
    }

    private static String prettifyBlockId(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return "알 수 없는 블록";
        }

        String path = blockId;

        int namespaceSplit = path.indexOf(':');
        if (namespaceSplit >= 0 && namespaceSplit + 1 < path.length()) {
            path = path.substring(namespaceSplit + 1);
        }

        String[] parts = path.split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            if (!builder.isEmpty()) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0)));

            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.isEmpty() ? blockId : builder.toString();
    }
}