package github.cosmicdan.temperaturebands.mixin;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dummy hook to manually "verify" that ShiftedNoise is unchanged between MC updates.
 * Doesn't actually do anything, just blocks compile if signatures and such have changed.
 */
@Mixin(DensityFunctions.ShiftedNoise.class)
abstract class DensityFunctionsHooks {

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void onConstruct(DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, DensityFunction.NoiseHolder noise, CallbackInfo ci) {
    }

    @Inject(
            method = "compute",
            at = @At("TAIL")
    )
    private void onCompute(DensityFunction.FunctionContext functionContext, CallbackInfoReturnable<Double> cir) {
    }
}
