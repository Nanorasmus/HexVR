package me.nanorasmus.nanodev.hex_vr.casting;

import at.petrak.hexcasting.api.spell.casting.ControllerInfo;
import at.petrak.hexcasting.api.spell.casting.ResolvedPattern;
import at.petrak.hexcasting.api.spell.casting.ResolvedPatternType;
import at.petrak.hexcasting.api.spell.math.HexCoord;
import at.petrak.hexcasting.api.spell.math.HexDir;
import at.petrak.hexcasting.api.spell.math.HexPattern;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import at.petrak.hexcasting.common.network.MsgNewSpellPatternSyn;
import at.petrak.hexcasting.xplat.IClientXplatAbstractions;
import me.nanorasmus.nanodev.hex_vr.HexVRClient;
import me.nanorasmus.nanodev.hex_vr.particle.CastingParticles;
import me.nanorasmus.nanodev.hex_vr.vr.VRPlugin;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

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
    static double gridSize = HexVRClient.config.gridSize;
    static double snappingDistance = HexVRClient.config.snappingDistance;
    static double backTrackDistance = HexVRClient.config.backTrackDistance;


    static ArrayList<ResolvedPattern> patterns = new ArrayList<>();
    static ArrayList<CastingPattern> castingPatterns = new ArrayList<>();
    public static ArrayList<Text> stack = new ArrayList<>();
    public static Text ravenMind = null;


    private static final float TEXT_DISTANCE = 9;


    boolean patternsAlwaysVisible = HexVRClient.config.patternsAlwaysVisible;
    boolean usingRightHand;
    Hand hand;
    int controllerIndex;
    ArrayList<Particle> handParticles = new ArrayList<>();

    static double particleDistance = gridSize / 10;

    private static void clear() {
        for (CastingPattern castingPattern : castingPatterns) {
            castingPattern.prepareDeletion();
        }
        castingPatterns.clear();
        patterns.clear();

        ravenMind = null;
        stack.clear();
    }


    public static void updateInstancesS2C(ControllerInfo info, int index) {
        if (info.isStackClear()) {
            clear();
            return;
        }
        // Update pattern
        if (index < castingPatterns.size()) {
            castingPatterns.get(index).updateResolution(info);
        }



        // Update stack
        stack.clear();
        for (NbtCompound tag : info.getStack()) {
            stack.add(HexIotaTypes.getDisplay(tag));
        }
        Collections.reverse(stack);


        // Update ravenmind
        if (info.getRavenmind() != null) {
            ravenMind = HexIotaTypes.getDisplay(info.getRavenmind());
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
        hand = Hand.OFF_HAND;
        // Check for right hand
        if (rightHand) {
            controllerIndex = 0;
            hand = Hand.MAIN_HAND;
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
        CastingPattern.init();
        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            if (ravenMind != null) {
                MinecraftClient client = MinecraftClient.getInstance();
                int width = client.getWindow().getScaledWidth();
                textRenderer.drawWithShadow(matrixStack, ravenMind, width - textRenderer.getWidth(ravenMind) * 2, 10, 0);
            }

            for (int i = 0; i < stack.size(); i++) {
                textRenderer.drawWithShadow(matrixStack, stack.get(i), 0, TEXT_DISTANCE * i, 0);
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

    Particle renderSpot(MinecraftClient client, Vec3d point, int maxAge) {
        return renderSpot(client, point, maxAge, 0.15f);
    }

    Particle renderSpot(MinecraftClient client, Vec3d point, int maxAge, float sizeMultiplier) {
        Particle particle = client.particleManager.addParticle(CastingParticles.STATIC_PARTICLE, point.x, point.y, point.z, 0, 0, 0);
        particle.setMaxAge(maxAge);
        particle.scale(sizeMultiplier);
        return particle;
    }

    Particle renderLine(MinecraftClient client, Vec3d from, Vec3d to) {
        Vec3d vel = to.subtract(from).multiply(0.1);
        Particle particle = client.particleManager.addParticle(CastingParticles.STATIC_PARTICLE, from.x, from.y, from.z, vel.x, vel.y, vel.z);
        particle.setMaxAge(10);
        particle.scale(0.1f);
        return particle;
    }

    void makeParticles(MinecraftClient client) {
        // Previous points
        for (int i = 0; i < points.size(); i++) {
            CastingPoint point = points.get(i);
            point.filterParticles();
            point.addParticle(renderSpot(client, point.point, 1));
            if (i > 0) {
                Vec3d prevPoint = points.get(i - 1).point;
                point.addParticle(renderLine(client, prevPoint, point.point));
            }
        }


        // Snapping points
        for (int i = 0; i < pointsAround.size(); i++) {
            Vec3d point = pointsAround.get(i);

            renderSpot(client, point, 1);
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
            reverseNormal = VRPlugin.apiInstance.getPreTickVRPlayer().getHMD().position().subtract(0, 0, 0).subtract(getPoint());
            normal = reverseNormal.negate();
            upNormal = new Vec3d(0, 1, 0);
            rightNormal = normal.crossProduct(upNormal).normalize();

        } else {
            reverseNormal = VRPlugin.apiInstance.getPreTickVRPlayer().getController(controllerIndex).getLookAngle();
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
            renderLine(MinecraftClient.getInstance(), from.lerp(to, lerpIncrement), to)
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
        }

        if (points.size() > 1 && currentPos.distanceTo(points.get(points.size() - 2).point) <= backTrackDistance) {
            removeNewestPoint();
        }

        makeParticles(client);


    }

    HexDir getStartingDir() {
        return points.get(1).direction;
    }

    HexPattern toHexPattern() {
        if (points.size() < 3) {
            return null;
        }
        HexPattern hexPattern = new HexPattern(getStartingDir(), new ArrayList<>());

        for (int i = 2; i < points.size(); i++) {
            if (!hexPattern.tryAppendDir(points.get(i).direction)) {
                MinecraftClient.getInstance().player.sendMessage(Text.of("Adding direction \"" + points.get(i).direction + "\" failed! Status: " + hexPattern));
            }
        }
        return hexPattern;
    }

    HexCoord generateHexCoord(int index) {
        return new HexCoord(0, index * 64);
    }

    void finishCasting(MinecraftClient client) {
        if (points.size() > 2) {
            // Initialize Pattern
            HexPattern pattern = toHexPattern();
            ResolvedPattern resolvedPattern = new ResolvedPattern(pattern, generateHexCoord(patterns.size()), ResolvedPatternType.UNRESOLVED);
            patterns.add(resolvedPattern);

            // Add floating pattern
            castingPatterns.add(new CastingPattern((ArrayList<CastingPoint>) points.clone(), resolvedPattern, castingPatterns.size()));

            // Send pattern to server
            IClientXplatAbstractions.INSTANCE.sendPacketToServer(
                    new MsgNewSpellPatternSyn(hand, pattern, patterns)
            );


            // Act like the new pattern has already been resolved
            patterns.remove(patterns.size() - 1);
            patterns.add(new ResolvedPattern(pattern, generateHexCoord(patterns.size()), ResolvedPatternType.EVALUATED));


        }
        points.clear();
        seenLines.clear();
    }
}
