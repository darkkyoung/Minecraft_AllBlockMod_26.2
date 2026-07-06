package com.darkk0729.allblocks.event;

import com.darkk0729.allblocks.challenge.ChallengeManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class ChallengeTicker {
    private ChallengeTicker() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ChallengeManager::tick);
    }
}