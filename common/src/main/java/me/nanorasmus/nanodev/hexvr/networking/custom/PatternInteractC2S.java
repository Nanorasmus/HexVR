package me.nanorasmus.nanodev.hexvr.networking.custom;

import dev.architectury.networking.NetworkManager;
import me.nanorasmus.nanodev.hexvr.casting.CastingPattern;
import me.nanorasmus.nanodev.hexvr.casting.ServerCasting;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.UUID;
import java.util.function.Supplier;

public class PatternInteractC2S {
    public final UUID interacterUUID;
    public final UUID patternCasterUUID;
    public final UUID patternUUID;
    public final boolean started;

    public PatternInteractC2S(PacketByteBuf buf) {
        this.interacterUUID = buf.readUuid();
        this.patternCasterUUID = buf.readUuid();
        this.patternUUID = buf.readUuid();
        this.started = buf.readBoolean();
    }

    public PatternInteractC2S(CastingPattern pattern, boolean started) {
        this.interacterUUID = MinecraftClient.getInstance().player.getUuid();
        this.patternCasterUUID = pattern.casterUUID;
        this.patternUUID = pattern.patternUUID;
        this.started = started;
    }

    public void encode(PacketByteBuf buf) {
        buf.writeUuid(interacterUUID);
        buf.writeUuid(patternCasterUUID);
        buf.writeUuid(patternUUID);
        buf.writeBoolean(started);
    }

    public void apply(Supplier<NetworkManager.PacketContext> contextSupplier) {
        if (contextSupplier.get().getEnv() == EnvType.SERVER) {
            // Check for uuid spoofing from the client
            ServerCasting.playerPatternInteractionStateChange(interacterUUID, patternCasterUUID, patternUUID, started);
        }
    }
}
