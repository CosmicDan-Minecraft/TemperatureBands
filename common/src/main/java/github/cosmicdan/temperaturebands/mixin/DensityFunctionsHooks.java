package github.cosmicdan.temperaturebands.mixin;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @noinspection UnusedMixin
 * Empty (and unregistered) hooks to manually verify that our desired noise functions are unchanged between MC updates.
 * Doesn't actually do anything, just blocks compile if signatures and such have changed.
 */
public abstract class DensityFunctionsHooks {
    @Mixin(DensityFunctions.ShiftedNoise.class)
    public static abstract class ShiftedNoiseHooks {
        @Inject(method = "<init>", at = @At("TAIL"))
        private void afterConstruct(DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, DensityFunction.NoiseHolder noise, CallbackInfo ci) {}

        @Inject(method = "compute", at = @At("TAIL"))
        private void afterCompute(DensityFunction.FunctionContext functionContext, CallbackInfoReturnable<Double> cir) {}
    }

    @Mixin(DensityFunctions.Ap2.class)
    public static abstract class Ap2Hooks {
        @Inject(method = "<init>", at = @At("TAIL"))
        private void afterConstruct(DensityFunctions.TwoArgumentSimpleFunction.Type type, DensityFunction func1, DensityFunction func2, double minValue, double maxValue, CallbackInfo ci) {}

        @Inject(method = "<init>", at = @At("TAIL"))
        private void afterCompute(DensityFunctions.TwoArgumentSimpleFunction.Type type, DensityFunction func1, DensityFunction func2, double minValue, double maxValue, CallbackInfo ci) {}
    }
}
