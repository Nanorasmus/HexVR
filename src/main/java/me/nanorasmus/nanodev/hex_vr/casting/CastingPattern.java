package me.nanorasmus.nanodev.hex_vr.casting;

import at.petrak.hexcasting.api.spell.casting.ControllerInfo;
import at.petrak.hexcasting.api.spell.casting.ResolvedPattern;
import at.petrak.hexcasting.api.spell.casting.ResolvedPatternType;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import me.nanorasmus.nanodev.hex_vr.entity.custom.TextEntity;
import me.nanorasmus.nanodev.hex_vr.particle.CastingParticles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static me.nanorasmus.nanodev.hex_vr.casting.Casting.particleDistance;

public class CastingPattern {
    public static HashMap<ResolvedPatternType, Color> colors = new HashMap<>();

    public static void init() {
        colors.put(ResolvedPatternType.UNRESOLVED, Color.gray);
        colors.put(ResolvedPatternType.ERRORED, Color.red);
        colors.put(ResolvedPatternType.INVALID, Color.red);
        colors.put(ResolvedPatternType.EVALUATED, Color.magenta);
        colors.put(ResolvedPatternType.ESCAPED, Color.yellow);
    }

    public Vec3d origin;
    public double originRadius;
    public ArrayList<CastingPoint> castingPoints;
    public ResolvedPattern pattern;
    public ArrayList<Text> stack = new ArrayList<>();
    public ArrayList<Entity> textEntities = new ArrayList<>();

    private float red, green, blue = 0;
    private int index;



    public CastingPattern(ArrayList<CastingPoint> points, ResolvedPattern resolvedPattern, int index) {
        castingPoints = points;
        pattern = resolvedPattern;
        this.index = index;

        // Get origin
        origin = new Vec3d(0, 0, 0);
        for (CastingPoint point : points) {
            origin = origin.add(point.point);
        }
        origin = origin.multiply(1.0 / points.size());

        // Get distance to the furthest point from origin;
        originRadius = 0.0;
        for (CastingPoint point : castingPoints) {
            double distance = origin.distanceTo(point.point);
            if (distance > originRadius) originRadius = distance;
        }

        updateColor();
    }

    public void prepareDeletion() {
        for (CastingPoint castingPoint : castingPoints) {
            castingPoint.prepareDeletion();
        }
        clearText();
    }

    void updateColor() {
        Color color = colors.get(getType());
        red = (float) color.getRed() / 256;
        green = (float) color.getGreen() / 256;
        blue = (float) color.getBlue() / 256;

        initializeLines(MinecraftClient.getInstance());
    }

    public void updateResolution(ControllerInfo info) {

        // Update type
        pattern.setType(info.getResolutionType());

        // Update stack
        stack.clear();
        for (NbtCompound tag : info.getStack()) {
            stack.add(HexIotaTypes.getDisplay(tag));
        }
        Collections.reverse(stack);


        // Update color
        updateColor();
    }


    public ResolvedPatternType getType() {
        return pattern.getType();
    }


    // Render logic
    private final double textDistance = 0.05;
    void clearText() {
        for (Entity text : textEntities) {
            text.kill();
        }
        textEntities.clear();
    }
    public void render(ArrayList<Vec3d> handPos) {
        MinecraftClient client = MinecraftClient.getInstance();


        // Handle Stack and Ravenmind visibility
        boolean isInRange = false;
        for (Vec3d pos : handPos) {
            if (origin.isInRange(pos, originRadius)) {
                isInRange = true;
            }
        }
        if (textEntities.isEmpty() && isInRange) {
            double size = stack.size() * textDistance;

            for (int i = 0; i < stack.size(); i++) {
                double y = origin.y - originRadius * 0.9 - i * textDistance;

                TextEntity entity = new TextEntity(client.world, origin.x, y, origin.z);
                entity.setNoGravity(true);
                entity.setInvisible(true);
                entity.setCustomName(stack.get(i));
                entity.setCustomNameVisible(true);

                textEntities.add(entity);
                // client.world.addEntity((69000 + i) * 1000 + index , entity);
                client.world.addEntity(entity.getUuid().hashCode(), entity);
            }
        } else if (!textEntities.isEmpty() && !isInRange) {
            clearText();
        }


        // Render points
        for (int i = 0; i < castingPoints.size(); i++) {
            CastingPoint point = castingPoints.get(i);
            point.filterParticles();
            point.addParticle(renderSpot(client, point.point, 1));
            if (i > 0) {
                Vec3d prevPoint = castingPoints.get(i - 1).point;
                point.addParticle(renderLine(client, prevPoint, point.point));
            }
        }
    }

    void initializeLines(MinecraftClient client) {
        // Delete all previous particles if there are any to re-initialize them
        prepareDeletion();

        for (int i = 0; i < castingPoints.size(); i++) {
            CastingPoint point = castingPoints.get(i);
            point.filterParticles();
            point.addParticle(renderSpot(client, point.point, 1));
            if (i > 0) {
                Vec3d prevPoint = castingPoints.get(i - 1).point;
                point.addParticles(initializeLine(client, prevPoint, point.point));
            }
        }
    }

    ArrayList<Particle> initializeLine(MinecraftClient client, Vec3d from, Vec3d to) {
        ArrayList<Particle> particles = new ArrayList<>();

        Vec3d direction = to.subtract(from);
        for (int i = 1; i < 100; i++) {
            // Get total distance to be incremented
            double increment = particleDistance * i;

            if (increment > direction.length()) break;

            // Turn it into a relative distance for lerping
            double lerpIncrement = increment / direction.length();

            particles.add(
                    renderLine(client, from.lerp(to, lerpIncrement), to)
            );
        }

        return particles;
    }

    Particle renderSpot(MinecraftClient client, Vec3d point, int maxAge) {
        Particle particle = client.particleManager.addParticle(CastingParticles.STATIC_PARTICLE, point.x, point.y, point.z, 0, 0, 0);
        particle.setMaxAge(maxAge);
        particle.scale(0.15f);
        particle.setColor(red, green, blue);
        return particle;
    }

    Particle renderLine(MinecraftClient client, Vec3d from, Vec3d to) {
        Vec3d vel = to.subtract(from).multiply(0.1);
        Particle particle = client.particleManager.addParticle(CastingParticles.STATIC_PARTICLE, from.x, from.y, from.z, vel.x, vel.y, vel.z);
        particle.setMaxAge(10);
        particle.scale(0.1f);
        particle.setColor(red, green, blue);
        return particle;
    }
}
