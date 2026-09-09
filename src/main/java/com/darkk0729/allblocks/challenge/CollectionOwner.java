package com.darkk0729.allblocks.challenge;

import java.util.UUID;

public record CollectionOwner(
        CollectionOwnerType type,
        String id,
        String displayName
) {
    public static final String CO_OP_OWNER_ID = "coop_shared";

    public CollectionOwner {
        type = type == null ? CollectionOwnerType.NONE : type;
        id = id == null ? "" : id;
        displayName = displayName == null ? "" : displayName;
    }

    public boolean isValid() {
        return type != CollectionOwnerType.NONE && !id.isBlank();
    }

    public static CollectionOwner player(UUID uuid, String playerName) {
        return new CollectionOwner(
                CollectionOwnerType.PLAYER,
                uuid == null ? "" : uuid.toString(),
                playerName
        );
    }

    public static CollectionOwner shared() {
        return new CollectionOwner(
                CollectionOwnerType.SHARED,
                CO_OP_OWNER_ID,
                "공용"
        );
    }

    public static CollectionOwner team(String teamId, String teamName) {
        return new CollectionOwner(
                CollectionOwnerType.TEAM,
                teamId,
                teamName
        );
    }
}