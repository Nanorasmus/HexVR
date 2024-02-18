package me.nanorasmus.nanodev.hex_vr.vr;

import net.blf02.vrapi.api.IVRAPI;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.awt.*;
import java.util.ArrayList;

public class VRPlugin {
    public static IVRAPI apiInstance;

    public static void initVR() {
        ArrayList<EntrypointContainer<IVRAPI>> apis = (ArrayList<EntrypointContainer<IVRAPI>>) FabricLoader.getInstance().getEntrypointContainers("vrapi", IVRAPI.class);
        if (!apis.isEmpty()) {
            apiInstance = apis.get(0).getEntrypoint();
        }
    }
}
