package me.nanorasmus.nanodev.hexvr.casting;

import at.petrak.hexcasting.api.spell.casting.ControllerInfo;
import at.petrak.hexcasting.api.spell.casting.ResolvedPatternType;
import at.petrak.hexcasting.api.spell.iota.Iota;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.api.utils.NbtCompoundBuilder;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import io.netty.buffer.ByteBuf;
import me.nanorasmus.nanodev.hexvr.entity.custom.TextEntity;
import me.nanorasmus.nanodev.hexvr.networking.NetworkingHandler;
import me.nanorasmus.nanodev.hexvr.networking.custom.PatternInteractC2S;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.TextCollector;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

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
    public ArrayList<NbtCompound> stackRaw = new ArrayList<>();
    public ArrayList<OrderedText> stack = new ArrayList<>();
    public ArrayList<Entity> textEntities = new ArrayList<>();

    public float red, green, blue = 0;
    private int index;



    public CastingPattern(ArrayList<CastingPoint> points, int index) {
        casterUUID = MinecraftClient.getInstance().player.getUuid();
        patternUUID = UUID.randomUUID();

        castingPoints = points;
        resolvedPatternType = ResolvedPatternType.UNRESOLVED;
        this.index = index;
        isLocal = true;

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

    public void updateResolution(ControllerInfo info) {

        // Update type
        resolvedPatternType = info.getResolutionType();

        // Update stack
        stack.clear();
        int width = 300;
        ArrayList<NbtCompound> tempStack = new ArrayList<>(info.getStack());
        Collections.reverse(tempStack);
        for (NbtCompound tag : tempStack) {
            if (stack.size() >= 10) {
                stack.add(Text.literal("...").formatted(Formatting.GRAY).asOrderedText());
                break;
            }
            stack.add(HexIotaTypes.getDisplayWithMaxWidth(tag, width, MinecraftClient.getInstance().textRenderer));
        }


        // Update color
        updateColor();

        // Send to other players
        stackRaw = new ArrayList<>(info.getStack());
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

    // ClientOnlyTimeoutRefreshTimer
    private final int timeoutRefreshRate = 10;
    private int timeoutRefreshTimer = 0;

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

        // Handle Stack and Ravenmind visibility
        boolean isInRange = false;
        for (Vec3d pos : handPos) {
            if (origin.isInRange(pos, originRadius)) {
                isInRange = true;
            }
        }
        if (isInRange) {
            if (timeoutRefreshTimer <= 0) {
                // Tick server interaction
                NetworkingHandler.CHANNEL.sendToServer(new PatternInteractC2S(this, true));
                timeoutRefreshTimer = timeoutRefreshRate;
            } else {
                timeoutRefreshTimer--;
            }
        } else {
            timeoutRefreshTimer = 0;
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

            // Notify server
            NetworkingHandler.CHANNEL.sendToServer(new PatternInteractC2S(this, false));
        }
    }

    private int getResolvedPatternTypeInt() {
        return switch (resolvedPatternType) {
            case UNRESOLVED -> 0;
            case ERRORED -> 1;
            case INVALID -> 2;
            case EVALUATED -> 3;
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


    // UUID for pattern
    public UUID casterUUID;
    public UUID patternUUID;

    // Server-only copy of stack after casting
    public ArrayList<Iota> serverStack;

    // Networking encoding and decoding
    private final boolean isLocal;

    public void encodeToBuffer(PacketByteBuf buf) {
        buf.writeUuid(casterUUID);
        buf.writeUuid(patternUUID);

        buf.writeInt(getResolvedPatternTypeInt());
        
        buf.writeInt(castingPoints.size());
        for (CastingPoint castingPoint : castingPoints) {
            castingPoint.encodeToBuffer(buf);
        }

        buf.writeInt(stackRaw.size());
        for (NbtCompound nbtCompound : stackRaw) {
            buf.writeNbt(nbtCompound);
        }
    }

    public void refineClientStack() {
        stack.clear();
        int width = 300;
        Collections.reverse(stackRaw);
        for (NbtCompound tag : stackRaw) {
            if (stack.size() >= 10) {
                stack.add(Text.literal("...").formatted(Formatting.GRAY).asOrderedText());
                break;
            }
            stack.add(HexIotaTypes.getDisplayWithMaxWidth(tag, width, MinecraftClient.getInstance().textRenderer));
        }
    }

    public CastingPattern(PacketByteBuf buf) {
        castingPoints = new ArrayList<>();
        isLocal = false;

        casterUUID = buf.readUuid();
        patternUUID = buf.readUuid();

        setResolvedPatternType(buf.readInt());

        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            castingPoints.add(CastingPoint.decodeFromBuffer(buf));
        }

        int stackSize = buf.readInt();
        for (int i = 0; i < stackSize; i++) {
            stackRaw.add(buf.readNbt());
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
    }

}