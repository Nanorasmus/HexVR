package me.nanorasmus.nanodev.hexvr.networking;

import dev.architectury.networking.NetworkChannel;
import me.nanorasmus.nanodev.hexvr.HexVR;
import me.nanorasmus.nanodev.hexvr.networking.custom.ClearPlayerPatterns;
import me.nanorasmus.nanodev.hexvr.networking.custom.SpawnPattern;
import net.minecraft.util.Identifier;

public class NetworkingHandler {
    public static final NetworkChannel CHANNEL = NetworkChannel.create(new Identifier(HexVR.MOD_ID, "networking_channel"));

    public static void registerPackets() {
        CHANNEL.register(SpawnPattern.class, SpawnPattern::encode, SpawnPattern::new, SpawnPattern::apply);
        CHANNEL.register(ClearPlayerPatterns.class, ClearPlayerPatterns::encode, ClearPlayerPatterns::new, ClearPlayerPatterns::apply);
    }
}
