package com.darkk0729.allblocks.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlayerColorChangePayload(
        String color
) implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(
                    "allblocks",
                    "change_player_color"
            );

    public static final Type<PlayerColorChangePayload> TYPE =
            new Type<>(ID);

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            PlayerColorChangePayload
            > CODEC =
            StreamCodec.ofMember(
                    PlayerColorChangePayload::write,
                    PlayerColorChangePayload::read
            );

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(color == null ? "" : color);
    }

    private static PlayerColorChangePayload read(
            RegistryFriendlyByteBuf buf
    ) {
        return new PlayerColorChangePayload(
                buf.readUtf()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}