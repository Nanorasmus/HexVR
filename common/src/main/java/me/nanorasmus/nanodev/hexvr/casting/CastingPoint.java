package me.nanorasmus.nanodev.hexvr.casting;

import at.petrak.hexcasting.api.spell.math.HexDir;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

import static me.nanorasmus.nanodev.hexvr.casting.Casting.castingPatterns;

public class CastingPoint {
    public Vec3d point;
    public HexDir direction;

    public final int pointParticleCooldown = 30;
    public int pointParticleTimer = 0;
    public final int lineParticleCooldown = 2;
    public int lineParticleTimer = 0;
    public ArrayList<Particle> particles = new ArrayList<>();

    public CastingPoint(Vec3d point) {
        this.point = point;
        direction = null;
    }

    public CastingPoint(Vec3d point, HexDir direction) {
        this.point = point;
        this.direction = direction;
    }

    public void filterParticles() {
        particles.removeIf(particle -> !particle.isAlive());
    }

    public void addParticle(Particle particle) {
        particles.add(particle);
    }
    public void addParticles(ArrayList<Particle> particles) {
        particles.addAll(particles);
    }

    public void prepareDeletion() {
        for (Particle particle : particles) {
            if (particle == null) {
                continue;
            }

            particle.markDead();
        }
    }


    // Networking
    public void encodeToBuffer(ByteBuf buf) {
        buf.writeDouble(point.x);
        buf.writeDouble(point.y);
        buf.writeDouble(point.z);
    }
    public static CastingPoint decodeFromBuffer(ByteBuf buf) {
        return new CastingPoint(new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }
}