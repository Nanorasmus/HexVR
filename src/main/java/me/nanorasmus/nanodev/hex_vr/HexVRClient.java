package me.nanorasmus.nanodev.hex_vr;

import me.nanorasmus.nanodev.hex_vr.config.HexVRConfig;
import me.nanorasmus.nanodev.hex_vr.entity.Entities;
import me.nanorasmus.nanodev.hex_vr.events.KeyInputHandler;
import me.nanorasmus.nanodev.hex_vr.vr.VRPlugin;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class HexVRClient implements ClientModInitializer {

    public static HexVRConfig.HexVRClientConfig config;
    public static final Map<String, Identifier> textureMap = new HashMap<>();

    @Override
    public void onInitializeClient() {

        VRPlugin.initVR();
        KeyInputHandler.register();


        Entities.registerEntities();
    }
}