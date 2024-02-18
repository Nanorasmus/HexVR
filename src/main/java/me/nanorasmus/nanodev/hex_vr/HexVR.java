package me.nanorasmus.nanodev.hex_vr;

import me.nanorasmus.nanodev.hex_vr.config.HexVRConfig;
import me.nanorasmus.nanodev.hex_vr.particle.CastingParticles;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HexVR implements ModInitializer {
    public static final String MOD_ID = "hex_vr";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final HexVRConfig CONFIG = HexVRConfig.setup();


    @Override
    public void onInitialize() {
        CastingParticles.registerParticles();
    }
}
