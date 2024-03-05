package me.nanorasmus.nanodev.hexvr.entity.custom;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.world.World;

public class TextEntity extends ArmorStandEntity {
    boolean isTextEntity = true;

    public TextEntity(EntityType<TextEntity> entityType, World world) {
        super(entityType, world);
    }

    public TextEntity(double x, double y, double z) {
        super(MinecraftClient.getInstance().world, x, y, z);
    }

    public void spawn() {
        MinecraftClient.getInstance().world.addEntity(this.getUuid().hashCode(), this);
    }
}
