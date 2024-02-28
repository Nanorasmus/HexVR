package me.nanorasmus.nanodev.hexvr.forge;

import dev.architectury.platform.forge.EventBuses;
import me.nanorasmus.nanodev.hexvr.HexVR;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HexVR.MOD_ID)
public class HexVRForge {
    public HexVRForge() {
		// Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(HexVR.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        HexVR.init();
    }
}