package me.nanorasmus.nanodev.hex_vr.casting;

import at.petrak.hexcasting.api.spell.casting.ResolvedPattern;
import at.petrak.hexcasting.api.spell.casting.ResolvedPatternType;
import at.petrak.hexcasting.api.spell.math.HexCoord;
import at.petrak.hexcasting.api.spell.math.HexDir;
import at.petrak.hexcasting.api.spell.math.HexPattern;
import at.petrak.hexcasting.common.network.MsgNewSpellPatternAck;
import at.petrak.hexcasting.common.network.MsgNewSpellPatternSyn;
import at.petrak.hexcasting.xplat.IClientXplatAbstractions;
import me.nanorasmus.nanodev.hex_vr.particle.CastingParticles;
import me.nanorasmus.nanodev.hex_vr.vr.VRPlugin;
import net.blf02.vrapi.api.data.IVRData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRData;
import org.vivecraft.client_vr.VRState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Casting {

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
    double gridSize = 0.2;
    double snappingDistance = gridSize / 2;
    double backTrackDistance = Math.min(snappingDistance, gridSize / 2.5);
    static ArrayList<ResolvedPattern> patterns = new ArrayList<>();
    static ArrayList<ArrayList<CastingPoint>> patternPoints = new ArrayList<>();
    boolean usingRightHand;
    Hand hand;
    int controllerIndex;

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
    }


    public void tick(@NotNull MinecraftClient client, boolean isPressed) {
        // Return if client is null or if player is not in vr
        if (client.player == null || !VRState.vrRunning || DATA_HOLDER.vrPlayer == null || DATA_HOLDER.vrPlayer.vrdata_world_render == null)
            return;



        // Render previous patterns
        if (!patternPoints.isEmpty()) {
            for (ArrayList<CastingPoint> patternPoint : patternPoints) {
                for (int i = 0; i < patternPoint.size(); i++) {
                    Vec3d point = patternPoint.get(i).point;
                    renderSpot(client, point, 1);
                    if (i > 0) {
                        Vec3d prevPoint = patternPoint.get(i - 1).point;
                        renderLine(client, prevPoint, point);
                    } else {
                    }
                }
            }
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

    void renderSpot(MinecraftClient client, Vec3d point, int maxAge) {
        renderSpot(client, point, maxAge, 0.15f);
    }

    void renderSpot(MinecraftClient client, Vec3d point, int maxAge, float sizeMultiplier) {
        Particle particle = client.particleManager.addParticle(CastingParticles.STATIC_PARTICLE, point.x, point.y, point.z, 0, 0, 0);
        particle.setMaxAge(maxAge);
        particle.scale(sizeMultiplier);
    }

    void renderLine(MinecraftClient client, Vec3d from, Vec3d to) {
        Vec3d vel = to.subtract(from).multiply(0.1);
        Particle particle = client.particleManager.addParticle(CastingParticles.STATIC_PARTICLE, from.x, from.y, from.z, vel.x, vel.y, vel.z);
        particle.setMaxAge(10);
        particle.scale(0.1f);
    }

    void makeParticles(MinecraftClient client) {
        for (int i = 0; i < points.size(); i++) {
            Vec3d point = points.get(i).point;
            renderSpot(client, point, 1);
            if (i > 0) {
                Vec3d prevPoint = points.get(i - 1).point;
                renderLine(client, prevPoint, point);
            }
        }


        for (int i = 0; i < pointsAround.size(); i++) {
            Vec3d point = pointsAround.get(i);

            renderSpot(client, point, 1);
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
        Vec3d right = rightNormal.multiply(0.5);
        Vec3d upperRight = rightNormal.multiply(0.5).add(upNormal);
        Vec3d bottomRight = upperRight.subtract(upNormal.multiply(2));
        Vec3d left = rightNormal.multiply(-1);
        Vec3d bottomLeft = upperRight.multiply(-1);
        Vec3d upperLeft = bottomRight.multiply(-1);
        hexOffsets.clear();
        hexOffsets.addAll(Arrays.asList(upperRight, rightNormal, bottomRight, bottomLeft, left, upperLeft));
    }

    CastingPoint getNewestPoint() {
        return points.get(points.size() - 1);
    }

    void startCasting(MinecraftClient client) {


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
        if (points.size() < 1) {
            points.add(new CastingPoint(snappingPoint));
        } else {
            points.add(createCastingPoint(snappingPoint));
        }
        updatePointsAround();
    }

    void removeNewestPoint() {
        if (!seenLines.isEmpty()) {
            seenLines.remove(seenLines.size() - 1);
        }
        if (!points.isEmpty()) {
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
            // Add floating pattern
            patternPoints.add(points);

            // Send pattern to server
            HexPattern pattern = toHexPattern();
            patterns.add(new ResolvedPattern(pattern, generateHexCoord(patterns.size()), ResolvedPatternType.UNRESOLVED));
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
