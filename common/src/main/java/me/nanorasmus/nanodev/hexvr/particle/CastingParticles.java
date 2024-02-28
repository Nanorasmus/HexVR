package me.nanorasmus.nanodev.hexvr.particle;

import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import me.nanorasmus.nanodev.hexvr.particle.custom.StaticParticle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

public class CastingParticles {
    public static final DefaultParticleType STATIC_PARTICLE = ParticleTypes.END_ROD;

    public static void registerParticles() {
        ParticleProviderRegistry.register(STATIC_PARTICLE, StaticParticle.Factory::new);
    }


    public static Particle renderSpot(MinecraftClient client, Vec3d point, int maxAge) {
        return renderSpot(client, point, maxAge, 0.15f);
    }

    public static Particle renderSpot(MinecraftClient client, Vec3d point, int maxAge, float sizeMultiplier) {
        Particle particle = client.particleManager.addParticle(CastingParticles.STATIC_PARTICLE, point.x, point.y, point.z, 0, 0, 0);
        particle.setMaxAge(maxAge);
        particle.scale(sizeMultiplier);
        return particle;
    }

    public static Particle renderLine(MinecraftClient client, Vec3d from, Vec3d to) {
        Vec3d vel = to.subtract(from).multiply(0.1);
        Particle particle = client.particleManager.addParticle(CastingParticles.STATIC_PARTICLE, from.x, from.y, from.z, vel.x, vel.y, vel.z);
        particle.setMaxAge(10);
        particle.scale(0.1f);
        return particle;
    }
}
