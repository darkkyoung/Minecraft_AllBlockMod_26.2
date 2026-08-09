package com.darkk0729.allblocks.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record AllBlocksSyncPayload(
        boolean running,
        boolean finished,
        String result,
        String mode,
        long elapsedTicks,
        int currentDay,
        int collectedCount,
        int totalTargetCount,
        List<BlockEntry> blocks
) implements CustomPacketPayload {
    private static final int MAX_BLOCK_ENTRIES = 2000;

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath("allblocks", "sync_state");

    public static final Type<AllBlocksSyncPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, AllBlocksSyncPayload> CODEC =
            StreamCodec.ofMember(AllBlocksSyncPayload::write, AllBlocksSyncPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(running);
        buf.writeBoolean(finished);
        buf.writeUtf(safeString(result));
        buf.writeUtf(safeString(mode));
        buf.writeLong(elapsedTicks);
        buf.writeVarInt(currentDay);
        buf.writeVarInt(collectedCount);
        buf.writeVarInt(totalTargetCount);

        int entryCount = Math.min(blocks == null ? 0 : blocks.size(), MAX_BLOCK_ENTRIES);
        buf.writeVarInt(entryCount);

        if (blocks == null) {
            return;
        }

        for (int i = 0; i < entryCount; i++) {
            BlockEntry entry = blocks.get(i);

            buf.writeUtf(safeString(entry.blockId()));
            buf.writeUtf(safeString(entry.ownerUuid()));
            buf.writeUtf(safeString(entry.ownerName()));
            buf.writeUtf(safeString(entry.state()));
        }
    }

    private static AllBlocksSyncPayload read(RegistryFriendlyByteBuf buf) {
        boolean running = buf.readBoolean();
        boolean finished = buf.readBoolean();
        String result = buf.readUtf();
        String mode = buf.readUtf();
        long elapsedTicks = buf.readLong();
        int currentDay = buf.readVarInt();
        int collectedCount = buf.readVarInt();
        int totalTargetCount = buf.readVarInt();

        int entryCount = Math.max(0, Math.min(buf.readVarInt(), MAX_BLOCK_ENTRIES));
        List<BlockEntry> blocks = new ArrayList<>(entryCount);

        for (int i = 0; i < entryCount; i++) {
            blocks.add(new BlockEntry(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf()
            ));
        }

        return new AllBlocksSyncPayload(
                running,
                finished,
                result,
                mode,
                elapsedTicks,
                currentDay,
                collectedCount,
                totalTargetCount,
                blocks
        );
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record BlockEntry(
            String blockId,
            String ownerUuid,
            String ownerName,
            String state
    ) {
    }
}