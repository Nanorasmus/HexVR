package me.nanorasmus.nanodev.hexvr.casting;

import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import io.netty.buffer.ByteBuf;
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
                continue;
            }

            particle.markDead();
        }
    }


    public static HexPattern pointArrayToHexPattern(ArrayList<CastingPoint> points) {
        return pointArrayToHexPattern(points, HexDir.EAST);
    }
    public static HexPattern pointArrayToHexPattern(ArrayList<CastingPoint> points, HexDir startingDir) {
        if (points.size() < 3) {
            return null;
        }
        HexPattern hexPattern = new HexPattern(startingDir, new ArrayList<>());

        for (int i = 2; i < points.size(); i++) {
            if (!hexPattern.tryAppendDir(points.get(i).direction)) {
                MinecraftClient.getInstance().player.sendMessage(Text.of("Adding direction \"" + points.get(i).direction + "\" failed! Status: " + hexPattern));
            }
        }
        return hexPattern;
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