package me.nanorasmus.nanodev.hexvr;

import at.petrak.hexcasting.api.spell.casting.ResolvedPattern;
import at.petrak.hexcasting.api.spell.casting.ResolvedPatternType;
import at.petrak.hexcasting.api.spell.math.HexCoord;
import at.petrak.hexcasting.api.spell.math.HexDir;
import at.petrak.hexcasting.api.spell.math.HexPattern;
import com.google.common.base.Suppliers;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import me.nanorasmus.nanodev.hexvr.casting.CastingPattern;
import me.nanorasmus.nanodev.hexvr.casting.ServerCasting;
import me.nanorasmus.nanodev.hexvr.casting.patterns.RegisterPatterns;
import me.nanorasmus.nanodev.hexvr.config.HexVRConfig;
import me.nanorasmus.nanodev.hexvr.entity.custom.TextEntityRenderer;
import me.nanorasmus.nanodev.hexvr.input.KeyInputsHandler;
import me.nanorasmus.nanodev.hexvr.networking.NetworkingHandler;
import me.nanorasmus.nanodev.hexvr.particle.CastingParticles;
import net.minecraft.entity.EntityType;

import java.util.ArrayList;


public class HexVR
{
	public static final String MOD_ID = "hex_vr";

	public static void init() {
		// Register Patterns
		RegisterPatterns.registerPatterns();

		NetworkingHandler.registerPackets();
		ServerCasting.initCommon();

	}

	public static Runnable initClient() {
		// Register Visuals
		EntityRendererRegistry.register(Suppliers.ofInstance(EntityType.ARMOR_STAND), TextEntityRenderer::new);
		CastingParticles.registerParticles();

		// Do additional client-side ServerCasting init
		ServerCasting.initClient();
		CastingPattern.init();

		// Register Casting
		KeyInputsHandler.register();
        return null;
    }

	public static Runnable initServer() {
		ServerCasting.initServer();
		return null;
	}
}
