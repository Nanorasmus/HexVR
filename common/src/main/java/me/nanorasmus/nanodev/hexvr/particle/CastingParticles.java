package me.nanorasmus.nanodev.hexvr.particle;

import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import me.nanorasmus.nanodev.hexvr.casting.CastingPattern;
import me.nanorasmus.nanodev.hexvr.casting.CastingPoint;
import me.nanorasmus.nanodev.hexvr.particle.custom.StaticParticle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

import static me.nanorasmus.nanodev.hexvr.casting.Casting.particleDistance;

public class CastingParticles {
    public static final DefaultParticleType STATIC_PARTICLE = ParticleTypes.END_ROD;

    static MinecraftClient client = MinecraftClient.getInstance();

    public static void registerParticles() {
        ParticleProviderRegistry.register(STATIC_PARTICLE, StaticParticle.Factory::new);
    }

    // Rendering

    public static void initializeLines(CastingPattern pattern) {
        // Delete all previous particles if there are any to re-initialize them
        pattern.prepareDeletion();

        for (int i = 0; i < pattern.castingPoints.size(); i++) {
            CastingPoint point = pattern.castingPoints.get(i);
            point.filterParticles();
            point.addParticle(renderSpot(point.point, 1, pattern.red, pattern.green, pattern.blue));
            if (i > 0) {
                Vec3d prevPoint = pattern.castingPoints.get(i - 1).point;
                point.addParticles(initializeLine(prevPoint, point.point));
            }
        }
    }

    public static ArrayList<Particle> initializeLine(Vec3d from, Vec3d to) {
        ArrayList<Particle> particles = new ArrayList<>();

        Vec3d direction = to.subtract(from);
        for (int i = 1; i < 50; i++) {
            // Get total distance to be incremented
            double increment = particleDistance * i;

            if (increment > direction.length()) break;

            // Turn it into a relative distance for lerping
            double lerpIncrement = increment / direction.length();

            particles.add(
                    renderLine(from.lerp(to, lerpIncrement), to)
            );
        }

        return particles;
    }

    public static Particle renderSpot(Vec3d point, int maxAge, float red, float green, float blue) {
        Particle particle = renderSpot(point, maxAge);
        particle.setColor(red, green, blue);
        return particle;
    }


    public static Particle renderSpot(Vec3d point, int maxAge) {
        return renderSpot(point, maxAge, 0.15f);
    }

    public static Particle renderSpot(Vec3d point, int maxAge, float sizeMultiplier) {
        Particle particle = client.particleManager.addParticle(CastingParticles.STATIC_PARTICLE, point.x, point.y, point.z, 0, 0, 0);
        particle.setMaxAge(maxAge);
        particle.scale(sizeMultiplier);
        return particle;
    }

    public static Particle renderLine(Vec3d from, Vec3d to, float red, float green, float blue) {
        Particle particle = renderLine(from, to);
        particle.setColor(red, green, blue);
        return particle;
    }

    public static Particle renderLine(Vec3d from, Vec3d to) {
        Vec3d vel = to.subtract(from).multiply(0.1 / 1.5);
        Particle particle = client.particleManager.addParticle(CastingParticles.STATIC_PARTICLE, from.x, from.y, from.z, vel.x, vel.y, vel.z);
        particle.setMaxAge(15);
        particle.scale(0.15f);
        return particle;
    }
}
