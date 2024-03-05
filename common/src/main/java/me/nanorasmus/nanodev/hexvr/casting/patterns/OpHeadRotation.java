package me.nanorasmus.nanodev.hexvr.casting.patterns;

import at.petrak.hexcasting.api.spell.ConstMediaAction;
import at.petrak.hexcasting.api.spell.OperationResult;
import at.petrak.hexcasting.api.spell.OperatorUtils;
import at.petrak.hexcasting.api.spell.casting.CastingContext;
import at.petrak.hexcasting.api.spell.casting.eval.SpellContinuation;
import at.petrak.hexcasting.api.spell.iota.Iota;
import at.petrak.hexcasting.api.spell.iota.NullIota;
import at.petrak.hexcasting.api.spell.iota.Vec3Iota;
import at.petrak.hexcasting.api.spell.mishaps.MishapBadEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.vivecraft.server.ServerVRPlayers;
import org.vivecraft.server.ServerVivePlayer;

import java.util.List;

public class OpHeadRotation implements ConstMediaAction {
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


        // Non-VR error
        if (!ServerVRPlayers.isVRPlayer(p)) {
            return List.of(new NullIota());
        }

        // VR logic
        ServerVivePlayer pVR = ServerVRPlayers.getVivePlayer(p);
        return List.of(new Vec3Iota(pVR.getHMDDir()));
    }

    @NotNull
    @Override
    public OperationResult operate(SpellContinuation continuation, List<Iota> stack, Iota ravenmind, CastingContext castingContext){
        return DefaultImpls.operate(this, continuation, stack, ravenmind, castingContext);
    }
}
