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
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.vivecraft.server.ServerVRPlayers;
import org.vivecraft.server.ServerVivePlayer;

import java.util.List;

public class OpHeadRotation implements ConstMediaAction {

    @Override
    public int getArgc() { return 1; }

    @Override
    public long getMediaCost() { return 0; }

    @NotNull
    @Override
    public List<Iota> execute(@NotNull List<? extends Iota> args, @NotNull CastingEnvironment ctx) {
        // Get player in question
        ServerPlayerEntity p = OperatorUtils.getPlayer(args, 0, getArgc());


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
    public ConstMediaAction.CostMediaActionResult executeWithOpCount(@NotNull List<? extends Iota> list, @NotNull CastingEnvironment castingEnvironment) {
        return DefaultImpls.executeWithOpCount(this, list, castingEnvironment);
    }

    @NotNull
    @Override
    public OperationResult operate(@NotNull CastingEnvironment castingEnvironment, @NotNull CastingImage castingImage, @NotNull SpellContinuation spellContinuation) {
        return DefaultImpls.operate(this, castingEnvironment, castingImage, spellContinuation);
    }
}
