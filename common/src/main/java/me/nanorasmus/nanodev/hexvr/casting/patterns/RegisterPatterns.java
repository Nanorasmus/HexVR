package me.nanorasmus.nanodev.hexvr.casting.patterns;

import at.petrak.hexcasting.api.PatternRegistry;
import at.petrak.hexcasting.api.spell.math.HexDir;
import at.petrak.hexcasting.api.spell.math.HexPattern;
import me.nanorasmus.nanodev.hexvr.HexVR;
import net.minecraft.util.Identifier;
import org.vivecraft.common.network.BodyPart;

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
                    new OpBodyPartPosition(BodyPart.MAIN_HAND));
            PatternRegistry.mapPattern(HexPattern.fromAngles("qaa", HexDir.WEST),
                    new Identifier(HexVR.MOD_ID, "left_hand_pos"),
                    new OpBodyPartPosition(BodyPart.OFF_HAND));

            // Hand Rot
            PatternRegistry.mapPattern(HexPattern.fromAngles("qwa", HexDir.EAST),
                    new Identifier(HexVR.MOD_ID, "right_hand_rot"),
                    new OpBodyPartRotation(BodyPart.MAIN_HAND));
            PatternRegistry.mapPattern(HexPattern.fromAngles("ewd", HexDir.WEST),
                    new Identifier(HexVR.MOD_ID, "left_hand_rot"),
                    new OpBodyPartRotation(BodyPart.OFF_HAND));

            // Hand Velocity
            PatternRegistry.mapPattern(HexPattern.fromAngles("qwaa", HexDir.EAST),
                    new Identifier(HexVR.MOD_ID, "right_hand_vel"),
                    new OpHandVelocity(0));
            PatternRegistry.mapPattern(HexPattern.fromAngles("ewdd", HexDir.WEST),
                    new Identifier(HexVR.MOD_ID, "left_hand_vel"),
                    new OpHandVelocity(1));

        } catch (PatternRegistry.RegisterPatternException exn) {
            exn.printStackTrace();
        }
    }
}
