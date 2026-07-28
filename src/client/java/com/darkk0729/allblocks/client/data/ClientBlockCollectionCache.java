package com.darkk0729.allblocks.client.data;

import java.util.HashMap;
import java.util.Map;

public final class ClientBlockCollectionCache {
    private static final Map<String, BlockStatus> BLOCK_STATUSES = new HashMap<>();

    private ClientBlockCollectionCache() {
    }

    public static BlockStatus getStatus(String blockId) {
        return BLOCK_STATUSES.getOrDefault(blockId, BlockStatus.unclaimed());
    }

    public static void setStatus(String blockId, BlockStatus status) {
        if (blockId == null || status == null) {
            return;
        }

        BLOCK_STATUSES.put(blockId, status);
    }

    public static int getClaimedCount() {
        int count = 0;

        for (BlockStatus status : BLOCK_STATUSES.values()) {
            if (status.state() == ClientBlockState.CLAIMED) {
                count++;
            }
        }

        return count;
    }

    public static void clear() {
        BLOCK_STATUSES.clear();
    }

    public record BlockStatus(
            ClientBlockState state,
            String ownerName
    ) {
        public static BlockStatus unclaimed() {
            return new BlockStatus(ClientBlockState.UNCLAIMED, "");
        }

        public boolean hasOwner() {
            return ownerName != null && !ownerName.isBlank();
        }
    }

    public enum ClientBlockState {
        UNCLAIMED("미획득"),
        CLAIMED("획득 완료"),
        RELEASED("잃어버림");

        private final String displayName;

        ClientBlockState(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}