package me.nanorasmus.nanodev.hex_vr.casting;

import at.petrak.hexcasting.api.spell.math.HexDir;
import net.minecraft.util.math.Vec3d;

public class CastingPoint {
    public Vec3d point;
    public HexDir direction;

    public CastingPoint(Vec3d point) {
        this.point = point;
        direction = null;
    }

    public CastingPoint(Vec3d point, HexDir direction) {
        this.point = point;
        this.direction = direction;
    }
}
