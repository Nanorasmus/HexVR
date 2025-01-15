package me.nanorasmus.nanodev.hexvr.casting;

import at.petrak.hexcasting.api.spell.casting.CastingHarness;
import at.petrak.hexcasting.api.spell.casting.ControllerInfo;
import at.petrak.hexcasting.api.spell.iota.Iota;
import at.petrak.hexcasting.api.spell.iota.PatternIota;
import at.petrak.hexcasting.api.spell.math.HexDir;
import at.petrak.hexcasting.api.spell.math.HexPattern;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.common.network.MsgNewSpellPatternAck;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.utils.GameInstance;
import me.nanorasmus.nanodev.hexvr.HexVR;
import me.nanorasmus.nanodev.hexvr.networking.NetworkingHandler;
import me.nanorasmus.nanodev.hexvr.networking.custom.ClearPlayerPatterns;
import me.nanorasmus.nanodev.hexvr.networking.custom.PatternInteractC2S;
import me.nanorasmus.nanodev.hexvr.networking.custom.SpawnPattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiling.jfr.event.ServerTickTimeEvent;
import org.vivecraft.api_beta.client.VivecraftClientAPI;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.common.network.BodyPart;
import org.vivecraft.server.ServerVRPlayers;
import org.vivecraft.server.ServerVivePlayer;

import java.io.Console;
import java.util.*;
import java.util.logging.Logger;

public class ServerCasting {
    public static HashMap<UUID, ArrayList<CastingPattern>> remotePlayerPatterns = new HashMap<>();

    // <Player, [LeftHandPositions, RightHandPositions]>
    public static HashMap<UUID, Pair<ArrayList<Vec3d>, ArrayList<Vec3d>>> remotePlayerHandVelocities = new HashMap<>();
    public static final int HAND_POSITION_MEMORY_LENGTH = 3;
    public static boolean isClient = false;

    public static ArrayList<CastingPattern> patternStackInsertionQueue = new ArrayList<>();

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
            patternStackInsertionQueue.add(pattern);

            ArrayList<ServerPlayerEntity> players = new ArrayList<>((GameInstance.getServer().getPlayerManager().getPlayerList()));
            for (int i = 0; i < players.size(); i++) {
                ServerPlayerEntity p = players.get(i);
                if (p.getUuid() != owner) {
                    NetworkingHandler.CHANNEL.sendToPlayer(p, new SpawnPattern(owner, pattern));
                }
            }
        } else {
            pattern.updateColor();
            pattern.refineClientStack();
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
    private static class PlayerInteraction {
        UUID interacter;
        UUID interactedPatternCaster;
        UUID interactedPattern;

        int age = 0;
        int timeoutTimer = 20;

        public PlayerInteraction(UUID interacter, UUID interactedPatternCaster, UUID interactedPattern) {
            this.interacter = interacter;
            this.interactedPatternCaster = interactedPatternCaster;
            this.interactedPattern = interactedPattern;
        }
    }
    public static ArrayList<PlayerInteraction> playerInteractions = new ArrayList<>();

    public static void initServer() {

    }

    static void tickServer(MinecraftServer server) {
        // Update the hand velocities
        updateHandVelocities(server);

        // Hande stack insertion queue
        handleStackInsertionQueue(server);

        // Handle player interactions
        handlePlayerInteractions(server);
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
            rightHandPositions.add(pVR.getBodyPartPos(BodyPart.MAIN_HAND));
            if (rightHandPositions.size() > HAND_POSITION_MEMORY_LENGTH) {
                rightHandPositions.remove(0);
            }

            // Update left hand
            ArrayList<Vec3d> leftHandPositions = oldHandPositions.getLeft();
            leftHandPositions.add(pVR.getBodyPartPos(BodyPart.OFF_HAND));
            if (leftHandPositions.size() > HAND_POSITION_MEMORY_LENGTH) {
                leftHandPositions.remove(0);
            }

            remotePlayerHandVelocities.put(uuid, new Pair<>(leftHandPositions, rightHandPositions));
        }
    }
    static void handleStackInsertionQueue(MinecraftServer server) {
        for (CastingPattern pattern : patternStackInsertionQueue) {
            pattern.serverStack = new ArrayList<>(IXplatAbstractions.INSTANCE.getHarness(server.getPlayerManager().getPlayer(pattern.casterUUID), Hand.MAIN_HAND).getStack());
        }
        patternStackInsertionQueue.clear();
    }

    static void handlePlayerInteractions(MinecraftServer server) {
        ArrayList<PlayerInteraction> playerInteractionsToRemove = new ArrayList<>();
        for (PlayerInteraction playerInteraction : playerInteractions) {
            ServerPlayerEntity interacter = server.getPlayerManager().getPlayer(playerInteraction.interacter);
            if (interacter == null) {
                server.sendMessage(Text.of("HexVR Error: Interacter is null?!?!"));
                playerInteractionsToRemove.add(playerInteraction);
                continue;
            }


            // Tick the age
            playerInteraction.age += 1;
            playerInteraction.timeoutTimer -= 1;

            if (playerInteraction.timeoutTimer <= 0) {
                playerInteractionsToRemove.add(playerInteraction);
                continue;
            }

            // Check for stack transfer attempt
            for (PlayerInteraction otherPlayerInteraction : playerInteractions) {
                if (playerInteraction == otherPlayerInteraction) continue;

                // Handle duplicates
                if (playerInteraction.interacter.equals(otherPlayerInteraction.interacter)
                && playerInteraction.interactedPattern.equals(otherPlayerInteraction.interactedPattern)
                && playerInteraction.interactedPatternCaster.equals(otherPlayerInteraction.interactedPatternCaster)) {
                    boolean deletionAlreadyTriggered = false;
                    for (PlayerInteraction toRemove : playerInteractionsToRemove) {
                        if (toRemove == playerInteraction || toRemove == otherPlayerInteraction) {
                            deletionAlreadyTriggered = true;
                            break;
                        }
                    }

                    // Delete the younger one and tick the older one
                    if (!deletionAlreadyTriggered && playerInteraction.age <= otherPlayerInteraction.age) {
                        playerInteraction.age = -1;
                        playerInteractionsToRemove.add(playerInteraction);
                        otherPlayerInteraction.timeoutTimer = 15;
                        break;
                    }
                }

                // Continue if it's the same person with both hands or if this is the receiver interaction as the owner should handle it
                if (playerInteraction.interacter.equals(otherPlayerInteraction.interacter)
                || !playerInteraction.interacter.equals(playerInteraction.interactedPatternCaster)) continue;

                int age = Math.min(playerInteraction.age, otherPlayerInteraction.age);

                if (playerInteraction.interactedPatternCaster.equals(otherPlayerInteraction.interactedPatternCaster)
                && playerInteraction.interactedPattern.equals(otherPlayerInteraction.interactedPattern)) {
                    // Someone's trying to transfer their stack!

                    // Find the pattern
                    CastingPattern pattern = null;
                    for (CastingPattern potentialPattern : remotePlayerPatterns.get(playerInteraction.interactedPatternCaster)) {
                        if (potentialPattern.casterUUID.equals(playerInteraction.interactedPatternCaster) && potentialPattern.patternUUID.equals(playerInteraction.interactedPattern)) {
                            pattern = potentialPattern;
                            break;
                        }
                    }
                    // Contínue loop if pattern is null or if neither player owns the pattern
                    if (pattern == null || !(pattern.casterUUID.equals(playerInteraction.interacter) || pattern.casterUUID.equals(otherPlayerInteraction.interacter))) continue;

                    // Get position of pattern
                    BlockPos patternPosition = new BlockPos(pattern.origin);

                    // Handle transfer
                    if (age > 75) {
                        ServerPlayerEntity reciever = server.getPlayerManager().getPlayer(
                                pattern.casterUUID.equals(playerInteraction.interacter) ? otherPlayerInteraction.interacter : playerInteraction.interacter
                        );
                        CastingHarness recieverHarness = IXplatAbstractions.INSTANCE.getHarness(reciever, Hand.MAIN_HAND);
                        List<Iota> stack = recieverHarness.getStack();
                        stack.addAll(pattern.serverStack);
                        recieverHarness.setStack(stack);

                        ControllerInfo recieverHarnessInfo = recieverHarness.executeIota(new PatternIota(HexPattern.fromAngles("", HexDir.EAST)), reciever.getWorld());
                        IXplatAbstractions.INSTANCE.sendPacketToPlayer(reciever,
                                new MsgNewSpellPatternAck(recieverHarnessInfo, 0));

                        IXplatAbstractions.INSTANCE.setHarness(reciever, recieverHarness);


                        // Clean up
                        playerInteractionsToRemove.add(playerInteraction);
                        playerInteractionsToRemove.add(otherPlayerInteraction);

                        // Play sound
                        server.getPlayerManager().getPlayer(playerInteraction.interacter).getWorld().playSound(
                                null, patternPosition,
                                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS,
                                1, 1
                        );
                    }

                    // Play ticking sounds
                    else if (age == 15 || age == 30 || age == 45 || age == 60) {
                        server.getPlayerManager().getPlayer(playerInteraction.interacter).getWorld().playSound(
                            null, patternPosition,
                            SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.PLAYERS,
                            1, 1
                        );
                    }
                }
            }
        }
        for (PlayerInteraction playerInteraction : playerInteractionsToRemove) {
            playerInteractions.remove(playerInteraction);
        }
    }

    public static void playerPatternInteractionStateChange(UUID player, UUID patternCasterUUID, UUID patternUUID, boolean started) {
        if (started) {
            playerInteractions.add(new PlayerInteraction(player, patternCasterUUID, patternUUID));
        } else {
            for (PlayerInteraction playerInteraction : playerInteractions) {
                if (playerInteraction.interacter == player
                        && playerInteraction.interactedPatternCaster == patternCasterUUID
                        && playerInteraction.interactedPattern == patternUUID) {
                    playerInteractions.remove(playerInteraction);
                    break;
                }
            }
        }
    }

    // Client-only
    public static ClientDataHolderVR DATA_HOLDER;

    public static void initClient() {
        isClient = true;
        DATA_HOLDER = ClientDataHolderVR.getInstance();

        ClientTickEvent.CLIENT_POST.register((client) -> {
            renderClient();
        });
    }

    public static void renderClient() {
        boolean isInVR = VivecraftClientAPI.getInstance().isVrActive();

        if (MinecraftClient.getInstance() == null || MinecraftClient.getInstance().world == null) return;

        List<AbstractClientPlayerEntity> remotePlayers = MinecraftClient.getInstance().world.getPlayers();

        for (int i = 0; i < remotePlayers.size(); i++) {
            UUID remotePlayerUUID = remotePlayers.get(i).getUuid();
            if (!remotePlayerPatterns.containsKey(remotePlayerUUID)) continue;

            ArrayList<CastingPattern> patterns = remotePlayerPatterns.get(remotePlayerUUID);
            for (int p = 0; p < patterns.size(); p++) {
                if (isInVR) {
                    patterns.get(p).render(
                            new ArrayList<>(List.of(
                                DATA_HOLDER.vrPlayer.vrdata_world_render.getController(0).getPosition(),
                                DATA_HOLDER.vrPlayer.vrdata_world_render.getController(1).getPosition()
                        ))
                    );
                } else {
                    patterns.get(p).render(new ArrayList<>());
                }
            }
        }
    }

    public static void sendPatternToServer(CastingPattern pattern) {
        NetworkingHandler.CHANNEL.sendToServer(new SpawnPattern(MinecraftClient.getInstance().player.getUuid(), pattern));
        NetworkingHandler.CHANNEL.sendToServer(new PatternInteractC2S(pattern, false));
    }
    public static void clearPatterns() {
        NetworkingHandler.CHANNEL.sendToServer(new ClearPlayerPatterns(MinecraftClient.getInstance().player.getUuid()));
    }
}
