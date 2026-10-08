package com.darkk0729.allblocks.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ClearMenuChatPayload(boolean clear)
        implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(
                    "allblocks",
                    "clear_menu_chat"
            );

    public static final Type<ClearMenuChatPayload> TYPE =
            new Type<>(ID);

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            ClearMenuChatPayload
            > CODEC =
            StreamCodec.ofMember(
                    ClearMenuChatPayload::write,
                    ClearMenuChatPayload::read
            );

    private void write(
            RegistryFriendlyByteBuf buf
    ) {
        buf.writeBoolean(clear);
    }

    private static ClearMenuChatPayload read(
            RegistryFriendlyByteBuf buf
    ) {
        return new ClearMenuChatPayload(
                buf.readBoolean()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
