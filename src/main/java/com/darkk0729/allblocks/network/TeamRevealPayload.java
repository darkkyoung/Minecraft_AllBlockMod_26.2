package com.darkk0729.allblocks.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TeamRevealPayload(String finalTeam)
        implements CustomPacketPayload {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(
                    "allblocks",
                    "team_reveal"
            );

    public static final Type<TeamRevealPayload> TYPE =
            new Type<>(ID);

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            TeamRevealPayload
            > CODEC =
            StreamCodec.ofMember(
                    TeamRevealPayload::write,
                    TeamRevealPayload::read
            );

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(finalTeam == null ? "" : finalTeam);
    }

    private static TeamRevealPayload read(
            RegistryFriendlyByteBuf buf
    ) {
        return new TeamRevealPayload(buf.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}