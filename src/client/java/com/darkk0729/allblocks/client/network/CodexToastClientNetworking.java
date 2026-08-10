package com.darkk0729.allblocks.client.network;

import com.darkk0729.allblocks.client.toast.CodexRegistrationToast;
import com.darkk0729.allblocks.network.CodexToastPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class CodexToastClientNetworking {
    private CodexToastClientNetworking() {
    }

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(
                CodexToastPayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        CodexRegistrationToast.show(payload.blockId())
                )
        );
    }
}