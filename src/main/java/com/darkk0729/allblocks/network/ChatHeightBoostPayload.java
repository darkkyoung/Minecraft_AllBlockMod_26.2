package com.darkk0729.allblocks.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChatHeightBoostPayload(int durationTicks)
        implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(
                    "allblocks",
                    "chat_height_boost"
            );

    public static final Type<ChatHeightBoostPayload> TYPE =
            new Type<>(ID);

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ChatHeightBoostPayload
            > CODEC =
            StreamCodec.ofMember(
                    ChatHeightBoostPayload::write,
                    ChatHeightBoostPayload::read
            );

    private void write(
            RegistryFriendlyByteBuf buf
    ) {
        buf.writeVarInt(
                Math.max(
                        1,
                        durationTicks
                )
        );
    }

    private static ChatHeightBoostPayload read(
            RegistryFriendlyByteBuf buf
    ) {
        return new ChatHeightBoostPayload(
                Math.max(
                        1,
                        buf.readVarInt()
                )
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
