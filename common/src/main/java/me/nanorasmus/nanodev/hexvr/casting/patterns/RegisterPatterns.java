package me.nanorasmus.nanodev.hexvr.casting.patterns;

import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.lib.hex.HexActions;
import org.vivecraft.common.network.BodyPart;

public class RegisterPatterns {
    public static void registerPatterns() {
        // Head
        HexActions.make("hexvr/head_pos", new ActionRegistryEntry(HexPattern.fromAngles("qqwqwqwqqw", HexDir.WEST),
                new OpHeadPosition()));
        HexActions.make("hexvr/head_rot", new ActionRegistryEntry(HexPattern.fromAngles("qqwqwqwqqwqqawdedw", HexDir.WEST),
                new OpHeadRotation()));


        // Hand Pos
        HexActions.make("hexvr/right_hand_pos", new ActionRegistryEntry(HexPattern.fromAngles("edd", HexDir.EAST),
                new OpBodyPartPosition(BodyPart.MAIN_HAND)));
        HexActions.make("hexvr/left_hand_pos", new ActionRegistryEntry(HexPattern.fromAngles("qaa", HexDir.WEST),
                new OpBodyPartPosition(BodyPart.OFF_HAND)));

        // Hand Rot
        HexActions.make("hexvr/right_hand_rot", new ActionRegistryEntry(HexPattern.fromAngles("qwa", HexDir.EAST),
                new OpBodyPartRotation(BodyPart.MAIN_HAND)));
        HexActions.make("hexvr/left_hand_rot", new ActionRegistryEntry(HexPattern.fromAngles("ewd", HexDir.WEST),
                new OpBodyPartRotation(BodyPart.OFF_HAND)));

        // Hand Velocity
        HexActions.make("hexvr/right_hand_vel", new ActionRegistryEntry(HexPattern.fromAngles("qwaa", HexDir.EAST),
                new OpHandVelocity(0)));
        HexActions.make("hexvr/left_hand_vel", new ActionRegistryEntry(HexPattern.fromAngles("ewdd", HexDir.WEST),
                new OpHandVelocity(1)));

    }
}
