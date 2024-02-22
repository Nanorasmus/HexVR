package me.nanorasmus.nanodev.hex_vr.particle;

import me.nanorasmus.nanodev.hex_vr.particle.custom.StaticParticle;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;

public class CastingParticles {
    public static final DefaultParticleType STATIC_PARTICLE = ParticleTypes.END_ROD;

    public static void registerParticles() {
        ParticleFactoryRegistry.getInstance().register(STATIC_PARTICLE, StaticParticle.Factory::new);
    }
}
