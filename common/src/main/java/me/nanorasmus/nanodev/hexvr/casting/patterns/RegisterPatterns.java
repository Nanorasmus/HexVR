package me.nanorasmus.nanodev.hexvr.casting.patterns;

import at.petrak.hexcasting.api.PatternRegistry;
import at.petrak.hexcasting.api.spell.math.HexDir;
import at.petrak.hexcasting.api.spell.math.HexPattern;
import me.nanorasmus.nanodev.hexvr.HexVR;
import net.minecraft.util.Identifier;

public class RegisterPatterns {
    public static void registerPatterns() {
        try {
            // Head
            PatternRegistry.mapPattern(HexPattern.fromAngles("qqwqwqwqqw", HexDir.WEST),
                    new Identifier(HexVR.MOD_ID, "head_pos"),
                    new OpHeadPosition());
            PatternRegistry.mapPattern(HexPattern.fromAngles("qqwqwqwqqwqqawdedw", HexDir.WEST),
                    new Identifier(HexVR.MOD_ID, "head_rot"),
                    new OpHeadRotation());


            // Hand Pos
            PatternRegistry.mapPattern(HexPattern.fromAngles("edd", HexDir.EAST),
                    new Identifier(HexVR.MOD_ID, "right_hand_pos"),
                    new OpHandPosition(0));
            PatternRegistry.mapPattern(HexPattern.fromAngles("qaa", HexDir.WEST),
                    new Identifier(HexVR.MOD_ID, "left_hand_pos"),
                    new OpHandPosition(1));

            // Hand Rot
            PatternRegistry.mapPattern(HexPattern.fromAngles("qwa", HexDir.EAST),
                    new Identifier(HexVR.MOD_ID, "right_hand_rot"),
                    new OpHandRotation(0));
            PatternRegistry.mapPattern(HexPattern.fromAngles("ewd", HexDir.WEST),
                    new Identifier(HexVR.MOD_ID, "left_hand_rot"),
                    new OpHandRotation(1));

        } catch (PatternRegistry.RegisterPatternException exn) {
            exn.printStackTrace();
        }
    }
}
