package github.cosmicdan.temperaturebands.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import github.cosmicdan.temperaturebands.CommonConfig;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(DensityFunctions.ShiftedNoise.class)
abstract class DensityFunctionsHooks {
    @Shadow @Final public static KeyDispatchDataCodec<DensityFunctions.ShiftedNoise> CODEC;
    @Unique
    private boolean temperaturebands_$isTempNoise;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void onConstruct(DensityFunction densityFunction, DensityFunction densityFunction2, DensityFunction densityFunction3, double d, double e, DensityFunction.NoiseHolder noiseHolder, CallbackInfo ci) {
        Optional<ResourceKey<NormalNoise.NoiseParameters>> key = noiseHolder.noiseData().unwrapKey();
        if (key.isPresent()) {
            final String noiseType = key.get().location().getPath();
            if (noiseType.equals("temperature") || noiseType.equals("temperature_large")) {
                temperaturebands_$isTempNoise = true;
                TemperatureBands.doTempConfig();
            }
        }
    }

    @WrapMethod(
            method = "compute"
    )
    private double onCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        if (temperaturebands_$isTempNoise) {
            return TemperatureBands.doTempNoise(context, original);
        } else {
            return original.call(context);
        }
    }
}
