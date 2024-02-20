package me.nanorasmus.nanodev.hex_vr.entity;

import me.nanorasmus.nanodev.hex_vr.entity.custom.TextEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.entity.EntityType;

public class Entities {
    public static void registerEntities() {
        EntityRendererRegistry.register(EntityType.ARMOR_STAND, TextEntityRenderer::new);
    }
}
