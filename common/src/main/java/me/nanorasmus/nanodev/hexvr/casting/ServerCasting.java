package me.nanorasmus.nanodev.hexvr.casting;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.utils.GameInstance;
import me.nanorasmus.nanodev.hexvr.networking.NetworkingHandler;
import me.nanorasmus.nanodev.hexvr.networking.custom.ClearPlayerPatterns;
import me.nanorasmus.nanodev.hexvr.networking.custom.SpawnPattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class ServerCasting {
    public static HashMap<UUID, ArrayList<CastingPattern>> remotePlayerPatterns = new HashMap<>();
    public static boolean isClient = false;

    public static void initCommon() {

    }


    public static void addPattern(UUID owner, CastingPattern pattern) {
        ArrayList<CastingPattern> list = remotePlayerPatterns.get(owner);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(pattern);
        remotePlayerPatterns.put(owner, list);

        if (!isClient) {
            ArrayList<ServerPlayerEntity> players = new ArrayList<>();
            players.addAll(GameInstance.getServer().getPlayerManager().getPlayerList());

            for (int i = 0; i < players.size(); i++) {
                ServerPlayerEntity p = players.get(i);
                if (p.getUuid() != owner) {
                    NetworkingHandler.CHANNEL.sendToPlayer(p, new SpawnPattern(owner, pattern));
                }
            }
        } else {
            pattern.updateColor();
        }
    }

    public static void clearPatternsOwnedBy(UUID owner) {
        if (remotePlayerPatterns.containsKey(owner)) {
            remotePlayerPatterns.put(owner, new ArrayList<>());
        }
        if (!isClient) {
            ArrayList<ServerPlayerEntity> players = new ArrayList<>();
            players.addAll(GameInstance.getServer().getPlayerManager().getPlayerList());

            for (int i = 0; i < players.size(); i++) {
                ServerPlayerEntity p = players.get(i);
                if (p.getUuid() != owner) {
                    NetworkingHandler.CHANNEL.sendToPlayer(p, new ClearPlayerPatterns(owner));
                }
            }
        }
    }


    // Client-only
    public static void initClient() {
        isClient = true;
        ClientTickEvent.CLIENT_POST.register((client) -> {
            renderClient();
        });
    }

    public static void renderClient() {
        remotePlayerPatterns.forEach((uuid, patterns) -> {
            for (int i = 0; i < patterns.size(); i++) {
                patterns.get(i).render(new ArrayList<>());
            }
        });
    }

    public static void sendPatternToServer(CastingPattern pattern) {
        NetworkingHandler.CHANNEL.sendToServer(new SpawnPattern(MinecraftClient.getInstance().player.getUuid(), pattern));
    }
    public static void clearPatterns() {
        NetworkingHandler.CHANNEL.sendToServer(new ClearPlayerPatterns(MinecraftClient.getInstance().player.getUuid()));
    }
}
