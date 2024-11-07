package me.nanorasmus.nanodev.hexvr.fabric.mixin;

import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternS2C;
import me.nanorasmus.nanodev.hexvr.casting.Casting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MsgNewSpellPatternS2C.class)
public class HexCastingNewSpellPatternS2CMixin {

    @Inject(at = @At("TAIL"), method = "handle", remap = false)
    private static void handle(MsgNewSpellPatternS2C self, CallbackInfo ci) {
        Casting.updateInstancesS2C(self.info(), self.index());
    }
}
