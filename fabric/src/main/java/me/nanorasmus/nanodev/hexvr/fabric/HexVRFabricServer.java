package me.nanorasmus.nanodev.hexvr.fabric;

import me.nanorasmus.nanodev.hexvr.HexVR;
import net.fabricmc.api.DedicatedServerModInitializer;

public class HexVRFabricServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        HexVR.initServer();
    }
}
