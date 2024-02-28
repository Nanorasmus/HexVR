package me.nanorasmus.nanodev.hexvr.forge.mixin;

import at.petrak.hexcasting.common.network.MsgNewSpellPatternAck;
import me.nanorasmus.nanodev.hexvr.casting.Casting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MsgNewSpellPatternAck.class)
public class HexCastingNewSpellPatternS2CMixin {

    @Inject(at = @At("TAIL"), method = "handle", remap = false)
    private static void handle(MsgNewSpellPatternAck self, CallbackInfo ci) {
        Casting.updateInstancesS2C(self.info(), self.index());
    }
}
