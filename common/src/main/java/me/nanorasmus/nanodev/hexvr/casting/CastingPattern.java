package me.nanorasmus.nanodev.hexvr.casting;

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView;
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import io.netty.buffer.ByteBuf;
import me.nanorasmus.nanodev.hexvr.entity.custom.TextEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.TextCollector;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static me.nanorasmus.nanodev.hexvr.particle.CastingParticles.*;

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
    public ResolvedPatternType resolvedPatternType;
    public HexPattern pattern;
    public ArrayList<OrderedText> stack = new ArrayList<>();
    public ArrayList<Entity> textEntities = new ArrayList<>();

    public float red, green, blue = 0;
    private int index;



    public CastingPattern(ArrayList<CastingPoint> points, int index, HexPattern pattern) {
        castingPoints = points;
        resolvedPatternType = ResolvedPatternType.UNRESOLVED;
        this.index = index;
        isLocal = true;
        this.pattern = pattern;

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
        Color color = colors.get(resolvedPatternType);
        red = (float) color.getRed() / 256;
        green = (float) color.getGreen() / 256;
        blue = (float) color.getBlue() / 256;

        if (!isLocal) {
            red = Math.max(0, red - 0.5f);
            green = Math.max(0, green - 0.5f);
            blue = Math.max(0, blue - 0.5f);
        }

        initializeLines(this);
    }

    public void updateResolution(ExecutionClientView info) {

        // Update type
        resolvedPatternType = info.getResolutionType();

        // Update stack
        stack.clear();
        int width = 500;
        for (NbtCompound tag : info.getStackDescs()) {
            if (stack.size() >= 7) {
                stack.add(Text.literal("...").formatted(Formatting.GRAY).asOrderedText() );
                break;
            }
            stack.add(IotaType.getDisplayWithMaxWidth(tag, width, MinecraftClient.getInstance().textRenderer));
        }
        Collections.reverse(stack);


        // Update color
        updateColor();

        // Send to other players
        ServerCasting.sendPatternToServer(this);
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

        // Render points
        for (int i = 0; i < castingPoints.size(); i++) {
            CastingPoint point = castingPoints.get(i);

            // Filter particle list for dead particles
            point.filterParticles();

            // Particles for pattern points
            if (point.pointParticleTimer <= 0) {
                point.pointParticleTimer = point.pointParticleCooldown;

                point.addParticle(renderSpot(point.point, point.pointParticleCooldown - 1, red, green, blue));
            }
            point.pointParticleTimer -= 1;

            // Particles for pattern lines
            if (i > 0) {
                if (point.lineParticleTimer <= 0) {
                    point.lineParticleTimer = point.lineParticleCooldown;

                    Vec3d prevPoint = castingPoints.get(i - 1).point;
                    point.addParticle(renderLine(prevPoint, point.point, red, green, blue));
                }
                point.lineParticleTimer -= 1;
            }

        }

        // Don't render stack if not local
        if (!isLocal) { return; }


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

                TextEntity entity = new TextEntity(origin.x, y, origin.z);
                entity.setNoGravity(true);
                entity.setInvisible(true);
                entity.patterns = stack.get(i);
                entity.setCustomNameVisible(true);

                textEntities.add(entity);
                entity.spawn();
            }
        } else if (!textEntities.isEmpty() && !isInRange) {
            clearText();
        }
    }

    private int getResolvedPatternTypeInt() {
        return switch (resolvedPatternType) {
            case UNRESOLVED -> 0;
            case ERRORED -> 1;
            case INVALID -> 2;
            case EVALUATED, UNDONE -> 3;
            case ESCAPED -> 4;
        };
    }

    private void setResolvedPatternType(int i) {
        switch (i) {
            case 0:
                resolvedPatternType = ResolvedPatternType.UNRESOLVED;
                break;
            case 1:
                resolvedPatternType = ResolvedPatternType.ERRORED;
                break;
            case 2:
                resolvedPatternType = ResolvedPatternType.INVALID;
                break;
            case 3:
                resolvedPatternType = ResolvedPatternType.EVALUATED;
                break;
            case 4:
                resolvedPatternType = ResolvedPatternType.ESCAPED;
                break;

        }
    }


    // Networking encoding and decoding
    private final boolean isLocal;
    public void encodeToBuffer(ByteBuf buf) {
        buf.writeInt(getResolvedPatternTypeInt());
        
        buf.writeInt(castingPoints.size());
        for (int i = 0; i < castingPoints.size(); i++) {
            castingPoints.get(i).encodeToBuffer(buf);
        }
    }

    public CastingPattern(ByteBuf buf) {
        castingPoints = new ArrayList<>();
        isLocal = false;


        setResolvedPatternType(buf.readInt());

        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            castingPoints.add(CastingPoint.decodeFromBuffer(buf));
        }

        // Get origin
        origin = new Vec3d(0, 0, 0);
        for (CastingPoint point : castingPoints) {
            origin = origin.add(point.point);
        }
        origin = origin.multiply(1.0 / castingPoints.size());

        // Get distance to the furthest point from origin;
        originRadius = 0.0;
        for (CastingPoint point : castingPoints) {
            double distance = origin.distanceTo(point.point);
            if (distance > originRadius) originRadius = distance;
        }

        pattern = null;
    }

}