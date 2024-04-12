package me.nanorasmus.nanodev.hexvr.casting.patterns;

import at.petrak.hexcasting.api.spell.ConstMediaAction;
import at.petrak.hexcasting.api.spell.OperationResult;
import at.petrak.hexcasting.api.spell.OperatorUtils;
import at.petrak.hexcasting.api.spell.casting.CastingContext;
import at.petrak.hexcasting.api.spell.casting.eval.SpellContinuation;
import at.petrak.hexcasting.api.spell.iota.Iota;
import at.petrak.hexcasting.api.spell.iota.NullIota;
import at.petrak.hexcasting.api.spell.iota.Vec3Iota;
import me.nanorasmus.nanodev.hexvr.casting.ServerCasting;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.vivecraft.server.ServerVRPlayers;
import org.vivecraft.server.ServerVivePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OpHandVelocity implements ConstMediaAction {
    int hand;

    public OpHandVelocity(int hand) {
        this.hand = hand;
    }

    @NotNull
    @Override
    public Text getDisplayName() {
        return DefaultImpls.getDisplayName(this);
    }

    @Override
    public boolean getAlwaysProcessGreatSpell() { return false; }

    @Override
    public boolean getCausesBlindDiversion() { return false; }

    @Override
    public boolean isGreat() { return false; }

    @Override
    public int getArgc() { return 1; }

    @Override
    public int getMediaCost() { return 0; }

    @NotNull
    @Override
    public List<Iota> execute(@NotNull List<? extends Iota> args, @NotNull CastingContext ctx) {
        // Get player in question
        ServerPlayerEntity p = OperatorUtils.getPlayer(args, 0, getArgc());
        ctx.assertEntityInRange(p);
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
    public OperationResult operate(SpellContinuation continuation, List<Iota> stack, Iota ravenmind, CastingContext castingContext){
        return DefaultImpls.operate(this, continuation, stack, ravenmind, castingContext);
    }
}
