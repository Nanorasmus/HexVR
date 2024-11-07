package me.nanorasmus.nanodev.hexvr.casting.patterns;

import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.lib.hex.HexActions;
import me.nanorasmus.nanodev.hexvr.HexVR;
import net.minecraft.util.Identifier;

public class RegisterPatterns {
    public static void registerPatterns() {
        // Head
        HexActions.make("hexvr/head_pos", new ActionRegistryEntry(HexPattern.fromAngles("qqwqwqwqqw", HexDir.WEST),
                new OpHeadPosition()));
        HexActions.make("hexvr/head_rot", new ActionRegistryEntry(HexPattern.fromAngles("qqwqwqwqqwqqawdedw", HexDir.WEST),
                new OpHeadRotation()));


        // Hand Pos
        HexActions.make("hexvr/right_hand_pos", new ActionRegistryEntry(HexPattern.fromAngles("edd", HexDir.EAST),
                new OpHandPosition(0)));
        HexActions.make("hexvr/left_hand_pos", new ActionRegistryEntry(HexPattern.fromAngles("qaa", HexDir.WEST),
                new OpHandPosition(1)));

        // Hand Rot
        HexActions.make("hexvr/right_hand_rot", new ActionRegistryEntry(HexPattern.fromAngles("qwa", HexDir.EAST),
                new OpHandRotation(0)));
        HexActions.make("hexvr/left_hand_rot", new ActionRegistryEntry(HexPattern.fromAngles("ewd", HexDir.WEST),
                new OpHandRotation(1)));

        // Hand Velocity
        HexActions.make("hexvr/right_hand_vel", new ActionRegistryEntry(HexPattern.fromAngles("qwaa", HexDir.EAST),
                new OpHandVelocity(0)));
        HexActions.make("hexvr/left_hand_vel", new ActionRegistryEntry(HexPattern.fromAngles("ewdd", HexDir.WEST),
                new OpHandVelocity(1)));

    }
}
