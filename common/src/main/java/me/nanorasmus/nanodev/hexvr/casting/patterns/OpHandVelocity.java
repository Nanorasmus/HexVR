package me.nanorasmus.nanodev.hexvr.casting.patterns;

import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.NullIota;
import at.petrak.hexcasting.api.casting.iota.Vec3Iota;
import me.nanorasmus.nanodev.hexvr.casting.ServerCasting;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OpHandVelocity implements ConstMediaAction {
    int hand;

    public OpHandVelocity(int hand) {
        this.hand = hand;
    }

    @Override
    public int getArgc() { return 1; }

    @Override
    public long getMediaCost() { return 0; }

    @NotNull
    @Override
    public List<Iota> execute(@NotNull List<? extends Iota> args, @NotNull CastingEnvironment ctx) {
        // Get player in question
        ServerPlayerEntity p = OperatorUtils.getPlayer(args, 0, getArgc());
        UUID uuid = p.getUuid();

        // Non-VR fallback
        if (!ServerCasting.remotePlayerHandVelocities.containsKey(uuid)) {
            return List.of(new NullIota());
        }

        // Get the previous hand positions
        ArrayList<Vec3d> previousHandPositions;
        if (hand == 0) {
            previousHandPositions = ServerCasting.remotePlayerHandVelocities.get(uuid).getRight();
        } else {
            previousHandPositions = ServerCasting.remotePlayerHandVelocities.get(uuid).getLeft();
        }

        // Calculate velocity
        Vec3d velocity = null;
        for (int i = 1; i < previousHandPositions.size(); i++) {
            Vec3d update = previousHandPositions.get(i).subtract(previousHandPositions.get(i - 1));
            if (velocity == null) {
                velocity = update;
            } else {
                velocity = velocity.multiply(0.05).add(update);
            }
        }


        return List.of(new Vec3Iota(velocity));
    }


    @NotNull
    @Override
    public ConstMediaAction.CostMediaActionResult executeWithOpCount(@NotNull List<? extends Iota> list, @NotNull CastingEnvironment castingEnvironment) {
        return DefaultImpls.executeWithOpCount(this, list, castingEnvironment);
    }

    @NotNull
    @Override
    public OperationResult operate(@NotNull CastingEnvironment castingEnvironment, @NotNull CastingImage castingImage, @NotNull SpellContinuation spellContinuation) {
        return DefaultImpls.operate(this, castingEnvironment, castingImage, spellContinuation);
    }
}
