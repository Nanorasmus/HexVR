package me.nanorasmus.nanodev.hex_vr;

import com.google.gson.GsonBuilder;
import me.nanorasmus.nanodev.hex_vr.casting.Casting;
import me.nanorasmus.nanodev.hex_vr.config.HexVRConfig;
import me.nanorasmus.nanodev.hex_vr.events.KeyInputHandler;
import me.nanorasmus.nanodev.hex_vr.particle.CastingParticles;
import me.nanorasmus.nanodev.hex_vr.particle.custom.StaticParticle;
import me.nanorasmus.nanodev.hex_vr.vr.VRPlugin;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleFactory;

public class HexVRClient implements ClientModInitializer {

    public static HexVRConfig.HexVRClientConfig config;

    @Override
    public void onInitializeClient() {

        VRPlugin.initVR();
        KeyInputHandler.register();


    }
}