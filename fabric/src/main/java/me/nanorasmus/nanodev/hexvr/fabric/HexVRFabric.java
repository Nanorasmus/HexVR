package me.nanorasmus.nanodev.hexvr.fabric;

import me.nanorasmus.nanodev.hexvr.HexVR;
import me.nanorasmus.nanodev.hexvr.fabric.config.HexVRConfigFabric;
import net.fabricmc.api.ModInitializer;

public class HexVRFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        HexVR.init();

        HexVRConfigFabric.setup();
    }
}