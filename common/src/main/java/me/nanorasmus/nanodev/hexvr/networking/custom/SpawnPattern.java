package me.nanorasmus.nanodev.hexvr.networking.custom;

import dev.architectury.networking.NetworkManager;
import me.nanorasmus.nanodev.hexvr.casting.CastingPattern;
import me.nanorasmus.nanodev.hexvr.casting.ServerCasting;
import net.fabricmc.api.EnvType;
import net.minecraft.network.PacketByteBuf;

import java.util.UUID;
import java.util.function.Supplier;

public class SpawnPattern {
    public final UUID owner;
    public final CastingPattern pattern;

    public SpawnPattern(PacketByteBuf buf) {
        this.owner = buf.readUuid();
        this.pattern = new CastingPattern(buf);
    }

    public SpawnPattern(UUID owner, CastingPattern pattern) {
        this.owner = owner;
        this.pattern = pattern;
    }

    public void encode(PacketByteBuf buf) {
        buf.writeUuid(owner);
        pattern.encodeToBuffer(buf);
    }

    public void apply(Supplier<NetworkManager.PacketContext> contextSupplier) {
        if (contextSupplier.get().getEnv() == EnvType.SERVER) {
            // Check for uuid spoofing from the client
            ServerCasting.addPattern(contextSupplier.get().getPlayer().getUuid(), pattern);
        } else {
            ServerCasting.addPattern(owner, pattern);
        }
    }
}
