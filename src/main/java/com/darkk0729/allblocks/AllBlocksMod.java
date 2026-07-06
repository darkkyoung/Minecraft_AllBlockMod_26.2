package com.darkk0729.allblocks;

import com.darkk0729.allblocks.command.AllBlocksCommands;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllBlocksMod implements ModInitializer {
    public static final String MOD_ID = "allblocks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing All Blocks Challenge");
        AllBlocksCommands.register();
    }
}