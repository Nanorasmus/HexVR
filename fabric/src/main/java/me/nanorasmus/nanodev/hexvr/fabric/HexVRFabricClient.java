package me.nanorasmus.nanodev.hexvr.fabric;

import me.nanorasmus.nanodev.hexvr.HexVR;
import me.nanorasmus.nanodev.hexvr.fabric.config.HexVRConfigFabric;
import net.fabricmc.api.ClientModInitializer;

public class HexVRFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HexVR.initClient();

        HexVRConfigFabric.setup();
    }
}
