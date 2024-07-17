package me.nanorasmus.nanodev.hexvr.casting;


import at.petrak.hexcasting.api.casting.eval.ExecutionClientView;
import at.petrak.hexcasting.api.casting.eval.ResolvedPattern;
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.casting.math.HexCoord;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.items.ItemStaff;
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternC2S;
import at.petrak.hexcasting.xplat.IClientXplatAbstractions;
import dev.architectury.event.events.client.ClientGuiEvent;
import me.nanorasmus.nanodev.hexvr.config.HexVRConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.particle.Particle;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRState;

import java.util.*;
import java.util.stream.Collectors;

import static me.nanorasmus.nanodev.hexvr.particle.CastingParticles.renderLine;
import static me.nanorasmus.nanodev.hexvr.particle.CastingParticles.renderSpot;

public class Casting {
    public static ArrayList<Casting> instances = new ArrayList<>();
    public boolean isFirst = false;

    private static final ClientDataHolderVR DATA_HOLDER = ClientDataHolderVR.getInstance();
    private final ArrayList<Vec3d> hexOffsets = new ArrayList<>();
    private ArrayList<Vec3d> pointsAround = new ArrayList<>();
    ArrayList<CastingPoint> points = new ArrayList<>();

    private class Line {
        Vec3d point1;
        Vec3d point2;

        public Line(Vec3d point1, Vec3d point2) {
            this.point1 = point1;
            this.point2 = point2;
        }

        public boolean equals(Vec3d from, Vec3d to) {
            // Forwards
            if (pointsMatch(from, point1) && pointsMatch(to, point2))
                return true;
            // Backward
            return pointsMatch(from, point2) && pointsMatch(to, point1);
        }
    }

    ArrayList<Line> seenLines = new ArrayList<>();

    /**
     * Normal vector of casting plane
     */
    boolean simpleNormals = false;
    Vec3d normal = new Vec3d(0, 0, 1);
    Vec3d reverseNormal = new Vec3d(0, 0, -1);
    Vec3d rightNormal = new Vec3d(1, 0, 0);
    Vec3d upNormal = new Vec3d(0, 1, 0);
    boolean wasPressed;

    /**
     * Distance between each point
     */
    static double gridSize = HexVRConfig.client.gridSize;
    static double snappingDistance = HexVRConfig.client.snappingDistance;
    static double backTrackDistance = HexVRConfig.client.backTrackDistance;


    static ArrayList<ResolvedPattern> patterns = new ArrayList<>();
    static ArrayList<CastingPattern> castingPatterns = new ArrayList<>();
    public static ArrayList<OrderedText> stack = new ArrayList<>();
    public static OrderedText ravenMind = null;
    public static int parenCount = 0;
    public static HexPattern introspection = new HexPattern(HexDir.WEST, new ArrayList<>());
    public static HexPattern retrospection = new HexPattern(HexDir.EAST, new ArrayList<>());


    private static final int TEXT_DISTANCE = 9;


    boolean patternsAlwaysVisible = HexVRConfig.client.patternsAlwaysVisible;
    boolean usingRightHand;
    int controllerIndex;
    ArrayList<Particle> handParticles = new ArrayList<>();

    public static double particleDistance = gridSize / 10;

    private static void clear() {
        // Delete all particles
        for (CastingPattern castingPattern : castingPatterns) {
            castingPattern.prepareDeletion();
        }

        int dropCount = stack.size();

        if (parenCount > 0) {
            // Server-Client loop to get out of introspection
            for (int i = 0; i < parenCount; i++) {
                IClientXplatAbstractions.INSTANCE.sendPacketToServer(
                        new MsgNewSpellPatternC2S(getHandWithStaff(MinecraftClient.getInstance().player), retrospection, patterns)
                );
            }
            dropCount += 1;
        }
        parenCount = 0;

        // Clear serverside stack
        if (dropCount > 0) {
            HexPattern stackClearPattern = new HexPattern(HexDir.SOUTH_EAST, new ArrayList<>());
            stackClearPattern.tryAppendDir(HexDir.NORTH_EAST);

            for (int i = 1; i < dropCount; i++) {
                stackClearPattern.tryAppendDir(HexDir.SOUTH_EAST);
                stackClearPattern.tryAppendDir(HexDir.NORTH_EAST);
            }

            IClientXplatAbstractions.INSTANCE.sendPacketToServer(
                    new MsgNewSpellPatternC2S(getHandWithStaff(MinecraftClient.getInstance().player), stackClearPattern, patterns)
            );
        }

        // Clear clientside stack
        ravenMind = null;
        stack.clear();

        // Clear patterns
        castingPatterns.clear();
        patterns.clear();

        // Clear serverside particles
        ServerCasting.clearPatterns();
    }

    public static void updateInstancesS2C(ExecutionClientView info, int index) {
        TextRenderer textRendererFont = MinecraftClient.getInstance().textRenderer;
        int width = 450;

        // Update stack
        stack.clear();
        for (NbtCompound tag : info.getStackDescs()) {
            if (stack.size() >= 24) {
                stack.add(Text.literal("...").formatted(Formatting.GRAY).asOrderedText());
                break;
            }
            stack.add(IotaType.getDisplayWithMaxWidth(tag, width, textRendererFont));
        }
        Collections.reverse(stack);


        if (info.isStackClear()) {
            clear();
            return;
        }
        // Update pattern
        if (index >= 0 && index < castingPatterns.size()) {
            ResolvedPatternType resolution = info.getResolutionType();
            CastingPattern castingPattern = castingPatterns.get(index);

            // Handle new "Undone" resolution type
            if (resolution == ResolvedPatternType.UNDONE) {
                castingPattern.prepareDeletion();
                castingPatterns.remove(index);
                patterns.remove(index);
                return;
            }

            castingPattern.updateResolution(info);

            // Do some witchcraft band-aid fix for keeping track of parenCount
            if (resolution == ResolvedPatternType.EVALUATED || resolution == ResolvedPatternType.ESCAPED) {
                String anglesSignature = castingPattern.pattern.anglesSignature();

                // Introspection
                if (anglesSignature.equalsIgnoreCase(introspection.anglesSignature()))
                    parenCount += 1;

                    // Retrospection
                else if (anglesSignature.equalsIgnoreCase(retrospection.anglesSignature())) {
                    parenCount -= 1;
                }
            }
        }


        // Update ravenmind
        if (info.getRavenmind() != null) {
            ravenMind = IotaType.getDisplayWithMaxWidth(info.getRavenmind(), width, textRendererFont);
        } else {
            ravenMind = null;
        }
    }

    public Casting(boolean rightHand, boolean simpleNormals) {
        usingRightHand = rightHand;
        this.simpleNormals = simpleNormals;

        // Set controller index
        // Left hand default
        controllerIndex = 1;
        // Check for right hand
        if (rightHand) {
            controllerIndex = 0;
        }

        if (instances.isEmpty()) {
            initStatic();
        }
        instances.add(this);
    }

    /**
     * An initialization method for static variables
     * Only triggers the first time a Casting class is initialized
     * */
    void initStatic() {
        isFirst = true;

        // Initialize pattern constants
        retrospection.tryAppendDir(HexDir.SOUTH_EAST);
        retrospection.tryAppendDir(HexDir.SOUTH_WEST);
        retrospection.tryAppendDir(HexDir.WEST);

        introspection.tryAppendDir(HexDir.SOUTH_WEST);
        introspection.tryAppendDir(HexDir.SOUTH_EAST);
        introspection.tryAppendDir(HexDir.EAST);

        // Stack and ravenmind UI
        ClientGuiEvent.RENDER_HUD.register((ctx, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();

            // Return if client is null or if player is not in vr
            if (client.player == null || !VRState.vrRunning || DATA_HOLDER.vrPlayer == null || DATA_HOLDER.vrPlayer.vrdata_world_render == null)
                return;

            TextRenderer textRenderer = client.textRenderer;

            // Ravenmind
            if (ravenMind != null) {
                int width = client.getWindow().getScaledWidth();
                ctx.drawTextWithShadow(textRenderer, ravenMind, width - textRenderer.getWidth(ravenMind) * 2, 10, 0);
            }

            // Stack
            for (int i = 0; i < stack.size(); i++) {
                ctx.drawTextWithShadow(textRenderer, stack.get(i), 0, TEXT_DISTANCE * i, 0);
            }
        });
    }


    public void tick(@NotNull MinecraftClient client, boolean isPressed) {
        // Return if client is null or if player is not in vr
        if (client.player == null || !VRState.vrRunning || DATA_HOLDER.vrPlayer == null || DATA_HOLDER.vrPlayer.vrdata_world_render == null)
            return;


        // Render previous patterns at all times (Limited to 1 instance to prevent unnecessary calls)
        if (isFirst && patternsAlwaysVisible && !castingPatterns.isEmpty()) {
            renderPreviousPatterns();
        }


        // If it wasn't pressed before
        if (!wasPressed) {
            // If the player isn't casting
            if (!isPressed)
                return;

                // If the player just started casting
            else
                startCasting(client);
        }
        // If it was pressed before
        else {
            // If the player just stopped casting
            if (!isPressed)
                finishCasting(client);

                // If the player is currently casting
            else
                castingTick(client);
        }
        wasPressed = isPressed;
    }

    void renderPreviousPatterns() {
        ArrayList<Vec3d> points = new ArrayList<>();
        for (Casting instance : instances) {
            points.add(instance.getPoint());
        }
        for (CastingPattern castingPattern : castingPatterns) {
            castingPattern.render(points);
        }
    }

    void makeParticles() {
        // Previous points
        for (int i = 0; i < points.size(); i++) {
            CastingPoint point = points.get(i);
            point.filterParticles();
            point.addParticle(renderSpot(point.point, 1));
            if (i > 0) {
                Vec3d prevPoint = points.get(i - 1).point;
                point.addParticle(renderLine(prevPoint, point.point));
            }
        }


        // Snapping points
        for (int i = 0; i < pointsAround.size(); i++) {
            Vec3d point = pointsAround.get(i);

            renderSpot(point, 1);
        }


        // Hand line
        for(Particle particle : handParticles) {
            particle.markDead();
        }
        handParticles = initializeLine(getNewestPoint().point, getPoint());


        // Render previous patterns only when casting
        if (!patternsAlwaysVisible && !castingPatterns.isEmpty()) {
            renderPreviousPatterns();
        }
    }

    Vec3d getPoint() {
        return DATA_HOLDER.vrPlayer.vrdata_world_render.getController(controllerIndex).getPosition();
    }

    void updateNormals() {
        if (simpleNormals) {
            reverseNormal = DATA_HOLDER.vrPlayer.vrdata_world_render.hmd.getPosition().subtract(0, 0, 0).subtract(getPoint());
            normal = reverseNormal.negate();
            upNormal = new Vec3d(0, 1, 0);
            rightNormal = normal.crossProduct(upNormal).normalize();

        } else {
            reverseNormal = DATA_HOLDER.vrPlayer.vrdata_world_render.getController(controllerIndex).getDirection();
            normal = reverseNormal.negate();
            rightNormal = normal.crossProduct(new Vec3d(0, 1, 0)).normalize();
            upNormal = normal.crossProduct(rightNormal).normalize();
        }
    }

    void perCastInit() {

        // Normals
        updateNormals();

        // Initialize Hex Offsets
        Vec3d right = rightNormal;
        Vec3d upperRight = right.multiply(0.5).add(upNormal);
        Vec3d bottomRight = upperRight.subtract(upNormal.multiply(2));
        Vec3d left = right.multiply(-1);
        Vec3d bottomLeft = upperRight.multiply(-1);
        Vec3d upperLeft = bottomRight.multiply(-1);
        hexOffsets.clear();
        hexOffsets.addAll(Arrays.asList(upperRight, right, bottomRight, bottomLeft, left, upperLeft));
    }

    CastingPoint getNewestPoint() {
        return points.get(points.size() - 1);
    }

    ArrayList<Particle> initializeLine(Vec3d from, Vec3d to) {
        ArrayList<Particle> particles = new ArrayList<>();

        Vec3d direction = to.subtract(from);
        for (int i = 1; i < 100; i++) {
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

    void startCasting(MinecraftClient client) {
        // Delete floating patterns if sneaking
        if (client.player.isSneaking()) {
            clear();
        }

        // Normal casting init
        perCastInit();
        points.add(new CastingPoint(getPoint()));
        updatePointsAround();
    }

    ArrayList<Vec3d> getPointsAround(Vec3d point) {
        return (ArrayList<Vec3d>) hexOffsets.stream().map(offset -> offset.multiply(gridSize).add(point)).collect(Collectors.toList());
    }

    /**
     * Checks if a line between 2 points has already been made
     *
     * @return true if line already exists
     */
    boolean lineSeen(Vec3d point1, Vec3d point2) {
        for (Line line : seenLines) {
            if (line.equals(point1, point2))
                return true;
        }
        return false;
    }

    boolean pointsMatch(Vec3d point1, Vec3d point2) {
        return point1.distanceTo(point2) < snappingDistance / 2;
    }

    boolean hasPoint(Vec3d possiblePoint) {
        for (CastingPoint point : points) {
            if (pointsMatch(point.point, possiblePoint))
                return true;
        }
        return false;
    }

    /**
     * Filters snapping points for unavailable points
     */
    ArrayList<Vec3d> filterPossiblePoints(ArrayList<Vec3d> possiblePoints) {
        ArrayList<Vec3d> output = new ArrayList<>();

        Vec3d newestPoint = getNewestPoint().point;

        for (int i = 0; i < possiblePoints.size(); i++) {
            Vec3d possiblePoint = possiblePoints.get(i);

            // Accept point if it hasn't been used before
            if (!hasPoint(possiblePoint)) {
                output.add(possiblePoint);
                continue;
            }

            // accept if a line between the possible point and the newest established point has NOT already been seen
            if (!lineSeen(newestPoint, possiblePoint)) {
                output.add(possiblePoint);
                continue;
            }
        }
        return output;
    }

    void updatePointsAround() {
        pointsAround = filterPossiblePoints(getPointsAround(getNewestPoint().point));
    }

    Vec3d findClosestSnappingPoint(Vec3d point) {
        if (pointsAround.isEmpty())
            return null;
        Vec3d snappingPoint = pointsAround.get(0);
        double distance = point.distanceTo(snappingPoint);
        for (int i = 0; i < pointsAround.size(); i++) {
            Vec3d pointAround = pointsAround.get(i);
            double distanceCandidate = point.distanceTo(pointAround);
            if (distanceCandidate < distance) {
                snappingPoint = pointAround;
                distance = distanceCandidate;
            }
        }
        return snappingPoint;
    }

    double findClosestSnappingPointDistance(Vec3d point) {
        if (pointsAround.isEmpty())
            return 512;
        Vec3d snappingPoint = pointsAround.get(0);
        double distance = point.distanceTo(snappingPoint);
        for (int i = 0; i < pointsAround.size(); i++) {
            Vec3d pointAround = pointsAround.get(i);
            double distanceCandidate = point.distanceTo(pointAround);
            if (distanceCandidate < distance) {
                distance = distanceCandidate;
            }
        }
        return distance;
    }

    void addPoint(Vec3d point) {
        // Find nearest snapping point
        Vec3d snappingPoint = findClosestSnappingPoint(point);
        seenLines.add(new Line(getNewestPoint().point, snappingPoint));

        // Initialize CastingPoint
        CastingPoint newPoint;
        if (points.size() < 1) {
            newPoint = new CastingPoint(snappingPoint);
        } else {
            newPoint = createCastingPoint(snappingPoint);

            // Initialize Particles
            initializeLine(getNewestPoint().point, newPoint.point);
        }


        // Finish up
        points.add(newPoint);
        updatePointsAround();
    }

    void removeNewestPoint() {
        if (!seenLines.isEmpty()) {
            seenLines.remove(seenLines.size() - 1);
        }
        if (!points.isEmpty()) {
            points.get(points.size() - 1).prepareDeletion();
            points.remove(points.size() - 1);
        }
        updatePointsAround();
    }

    Vec3d toCastingPlane(Vec3d point3D) {
        return new Vec3d(point3D.dotProduct(rightNormal), point3D.dotProduct(upNormal), 0);
    }

    HexDir getPointDirection(Vec3d point) {
        CastingPoint lastPoint = getNewestPoint();

        Vec3d point3D = point.subtract(lastPoint.point);
        Vec3d point2D = toCastingPlane(point3D).negate();


        HexDir direction;
        // Northern
        if (point2D.y > snappingDistance / 5) {
            if (point2D.x < 0)
                direction = HexDir.NORTH_WEST;
            else
                direction = HexDir.NORTH_EAST;
        }
        // Southern
        else if (point2D.y < -(snappingDistance / 5)) {
            if (point2D.x < 0)
                direction = HexDir.SOUTH_WEST;
            else
                direction = HexDir.SOUTH_EAST;
        }
        // Equator
        else {
            if (point2D.x < 0)
                direction = HexDir.WEST;
            else
                direction = HexDir.EAST;
        }

        return direction;
    }

    CastingPoint createCastingPoint(Vec3d point) {
        HexDir direction = getPointDirection(point);

        return new CastingPoint(point, direction);
    }

    void castingTick(MinecraftClient client) {
        Vec3d currentPos = getPoint();

        // Trigger new point
        if (findClosestSnappingPointDistance(currentPos) <= snappingDistance) {
            addPoint(currentPos);
            client.world.playSound(client.player.getBlockPos(), SoundEvents.BLOCK_LANTERN_HIT, SoundCategory.PLAYERS, 0.4F, 4, false);
        }

        if (points.size() > 1 && currentPos.distanceTo(points.get(points.size() - 2).point) <= backTrackDistance) {
            removeNewestPoint();
        }

        makeParticles();


    }

    HexDir getStartingDir() {
        return points.get(1).direction;
    }

    HexPattern toHexPattern() {
        return CastingPoint.pointArrayToHexPattern(points, getStartingDir());
    }

    HexCoord generateHexCoord(int index) {
        return new HexCoord(0, index * 64);
    }

    static Hand getHandWithStaff(ClientPlayerEntity p) {
        if (p.getOffHandStack().getItem() instanceof ItemStaff) {
            return Hand.OFF_HAND;
        }
        return Hand.MAIN_HAND;
    }

    void finishCasting(MinecraftClient client) {
        if (points.size() > 2) {
            // Initialize Pattern
            HexPattern pattern = toHexPattern();
            ResolvedPattern resolvedPattern = new ResolvedPattern(pattern, generateHexCoord(patterns.size()), ResolvedPatternType.UNRESOLVED);
            patterns.add(resolvedPattern);

            // Add floating pattern
            castingPatterns.add(new CastingPattern((ArrayList<CastingPoint>) points.clone(), castingPatterns.size(), pattern));

            // Send pattern to server
            IClientXplatAbstractions.INSTANCE.sendPacketToServer(
                    new MsgNewSpellPatternC2S(getHandWithStaff(client.player), pattern, patterns)
            );


            // Act like the new pattern has already been resolved
            patterns.remove(patterns.size() - 1);
            patterns.add(new ResolvedPattern(pattern, generateHexCoord(patterns.size()), ResolvedPatternType.EVALUATED));


        }
        points.clear();
        seenLines.clear();
    }
}
