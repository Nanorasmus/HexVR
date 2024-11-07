package me.nanorasmus.nanodev.hexvr.casting;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.utils.GameInstance;
import me.nanorasmus.nanodev.hexvr.networking.NetworkingHandler;
import me.nanorasmus.nanodev.hexvr.networking.custom.ClearPlayerPatterns;
import me.nanorasmus.nanodev.hexvr.networking.custom.SpawnPattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import org.vivecraft.server.ServerVRPlayers;
import org.vivecraft.server.ServerVivePlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class ServerCasting {
    public static HashMap<UUID, ArrayList<CastingPattern>> remotePlayerPatterns = new HashMap<>();

    // <Player, [LeftHandPositions, RightHandPositions]>
    public static HashMap<UUID, Pair<ArrayList<Vec3d>, ArrayList<Vec3d>>> remotePlayerHandVelocities = new HashMap<>();
    public static final int HAND_POSITION_MEMORY_LENGTH = 3;
    public static boolean isClient = false;

    public static void initCommon() {

        // Register server tick function
        TickEvent.SERVER_PRE.register(ServerCasting::tickServer);
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

    // Server-only
    public static void initServer() {

    }

    static void tickServer(MinecraftServer server) {
        // Update the hand velocities
        updateHandVelocities(server);
    }

    static void updateHandVelocities(MinecraftServer server) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            UUID uuid = p.getUuid();

            // Remove entry and continue if they are not in VR
            if (!ServerVRPlayers.isVRPlayer(p)) {
                remotePlayerHandVelocities.remove(uuid);
                continue;
            }
            ServerVivePlayer pVR = ServerVRPlayers.getVivePlayer(p);

            // Get the current list of previous positions
            Pair<ArrayList<Vec3d>, ArrayList<Vec3d>> oldHandPositions;
            if (remotePlayerHandVelocities.containsKey(uuid)) {
                oldHandPositions = remotePlayerHandVelocities.get(uuid);
            } else {
                oldHandPositions = new Pair<>(new ArrayList<>(), new ArrayList<>());
            }

            // Update right hand
            ArrayList<Vec3d> rightHandPositions = oldHandPositions.getRight();
            rightHandPositions.add(pVR.getControllerPos(0, p));
            if (rightHandPositions.size() > HAND_POSITION_MEMORY_LENGTH) {
                rightHandPositions.remove(0);
            }

            // Update left hand
            ArrayList<Vec3d> leftHandPositions = oldHandPositions.getLeft();
            leftHandPositions.add(pVR.getControllerPos(1, p));
            if (leftHandPositions.size() > HAND_POSITION_MEMORY_LENGTH) {
                leftHandPositions.remove(0);
            }

            remotePlayerHandVelocities.put(uuid, new Pair<>(leftHandPositions, rightHandPositions));
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
