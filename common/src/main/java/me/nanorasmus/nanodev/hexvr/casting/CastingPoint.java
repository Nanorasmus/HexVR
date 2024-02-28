package me.nanorasmus.nanodev.hexvr.casting;

import at.petrak.hexcasting.api.spell.math.HexDir;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class CastingPoint {
    public Vec3d point;
    public HexDir direction;

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
                MinecraftClient.getInstance().player.sendMessage(Text.of("There was a null particle!"));
                continue;
            }

            particle.markDead();
        }
    }
}