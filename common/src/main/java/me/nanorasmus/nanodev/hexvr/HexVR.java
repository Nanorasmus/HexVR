package me.nanorasmus.nanodev.hexvr;

import com.google.common.base.Suppliers;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import me.nanorasmus.nanodev.hexvr.config.HexVRConfig;
import me.nanorasmus.nanodev.hexvr.entity.custom.TextEntityRenderer;
import me.nanorasmus.nanodev.hexvr.input.KeyInputsHandler;
import me.nanorasmus.nanodev.hexvr.particle.CastingParticles;
import net.minecraft.entity.EntityType;


public class HexVR
{
	public static final String MOD_ID = "hex_vr";

	public static void init() {
		HexVRConfig.init();

		// Register Visuals
		EntityRendererRegistry.register(Suppliers.ofInstance(EntityType.ARMOR_STAND), TextEntityRenderer::new);
		CastingParticles.registerParticles();

		// Register Casting
		KeyInputsHandler.register();
	}
}
