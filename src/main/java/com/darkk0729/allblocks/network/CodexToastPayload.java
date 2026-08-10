package com.darkk0729.allblocks.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CodexToastPayload(String blockId) implements CustomPacketPayload {
    public static final Identifier ID =
            Identifier.fromNamespaceAndPath("allblocks", "codex_toast");

    public static final Type<CodexToastPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, CodexToastPayload> CODEC =
            StreamCodec.ofMember(CodexToastPayload::write, CodexToastPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(blockId == null ? "" : blockId);
    }

    private static CodexToastPayload read(RegistryFriendlyByteBuf buf) {
        return new CodexToastPayload(buf.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}