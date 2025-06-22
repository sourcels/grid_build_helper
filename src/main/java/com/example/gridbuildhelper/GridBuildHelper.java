package com.example.gridbuildhelper;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GridBuildHelper implements ModInitializer {
    public static final String MOD_ID = "com/example/gridbuildhelper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Block Outlines Mod Initializing!");
    }
}
