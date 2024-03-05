package me.nanorasmus.nanodev.hexvr.forge;

import dev.architectury.platform.forge.EventBuses;
import me.nanorasmus.nanodev.hexvr.HexVR;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.Supplier;

@Mod(HexVR.MOD_ID)
public class HexVRForge {
    public HexVRForge() {
		// Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(HexVR.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        HexVR.init();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, new Supplier<Runnable>() {
            @Override
            public Runnable get() {
                return HexVR::initClient;
            }
        });
    }
}