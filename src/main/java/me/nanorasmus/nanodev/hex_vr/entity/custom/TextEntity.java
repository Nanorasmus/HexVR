package me.nanorasmus.nanodev.hex_vr.entity.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.world.World;

public class TextEntity extends ArmorStandEntity {
    boolean isTextEntity = true;

    public TextEntity(EntityType<TextEntity> entityType, World world) {
        super(entityType, world);
    }

    public TextEntity(World world, double x, double y, double z) {
        super(world, x, y, z);
    }
}
