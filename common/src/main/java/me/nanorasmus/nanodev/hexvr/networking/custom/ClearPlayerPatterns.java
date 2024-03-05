package me.nanorasmus.nanodev.hexvr.networking.custom;

import dev.architectury.networking.NetworkManager;
import me.nanorasmus.nanodev.hexvr.casting.CastingPattern;
import me.nanorasmus.nanodev.hexvr.casting.ServerCasting;
import net.fabricmc.api.EnvType;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;
import java.util.function.Supplier;

public class ClearPlayerPatterns {
    public final UUID owner;

    public ClearPlayerPatterns(PacketByteBuf buf) {
        this.owner = buf.readUuid();
    }

    public ClearPlayerPatterns(UUID owner) {
        this.owner = owner;
    }

    public void encode(PacketByteBuf buf) {
        buf.writeUuid(owner);
    }

    public void apply(Supplier<NetworkManager.PacketContext> contextSupplier) {
        if (contextSupplier.get().getEnv() == EnvType.SERVER) {
            // Check for uuid spoofing from the client
            ServerCasting.clearPatternsOwnedBy(contextSupplier.get().getPlayer().getUuid());
        } else {
            ServerCasting.clearPatternsOwnedBy(owner);
        }
    }
}
