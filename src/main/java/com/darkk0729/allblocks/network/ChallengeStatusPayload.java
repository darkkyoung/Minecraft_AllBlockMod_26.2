package com.darkk0729.allblocks.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChallengeStatusPayload(
        boolean running,
        boolean finished,
        String result,
        String mode,
        String difficulty,
        long elapsedTicks,
        int currentDay,
        long finalDayRemainingTicks,
        int collectedCount,
        int totalTargetCount
) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("allblocks", "challenge_status");
    public static final Type<ChallengeStatusPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ChallengeStatusPayload> CODEC =
            StreamCodec.ofMember(ChallengeStatusPayload::write, ChallengeStatusPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(running);
        buf.writeBoolean(finished);
        buf.writeUtf(safe(result));
        buf.writeUtf(safe(mode));
        buf.writeUtf(safe(difficulty));
        buf.writeLong(elapsedTicks);
        buf.writeVarInt(currentDay);
        buf.writeLong(finalDayRemainingTicks);
        buf.writeVarInt(collectedCount);
        buf.writeVarInt(totalTargetCount);
    }

    private static ChallengeStatusPayload read(RegistryFriendlyByteBuf buf) {
        return new ChallengeStatusPayload(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readLong(),
                buf.readVarInt(),
                buf.readLong(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}