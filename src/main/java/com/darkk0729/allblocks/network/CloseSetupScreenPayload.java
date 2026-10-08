package com.darkk0729.allblocks.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CloseSetupScreenPayload(boolean close)
        implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(
                    "allblocks",
                    "close_setup_screen"
            );

    public static final Type<CloseSetupScreenPayload> TYPE =
            new Type<>(ID);

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            CloseSetupScreenPayload
            > CODEC =
            StreamCodec.ofMember(
                    CloseSetupScreenPayload::write,
                    CloseSetupScreenPayload::read
            );

    private void write(
            RegistryFriendlyByteBuf buf
    ) {
        buf.writeBoolean(close);
    }

    private static CloseSetupScreenPayload read(
            RegistryFriendlyByteBuf buf
    ) {
        return new CloseSetupScreenPayload(
                buf.readBoolean()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
