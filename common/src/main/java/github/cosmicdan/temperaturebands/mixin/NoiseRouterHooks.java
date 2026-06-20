package github.cosmicdan.temperaturebands.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.ShiftedNoiseHumidity;
import github.cosmicdan.temperaturebands.ShiftedNoiseTemperature;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

/**
 * Responsible for overriding relevant DensityFunctions in NoiseRouter if applicable (i.e. has an entry in TemperatureBands.NOISEROUTER_TEMP_OVERRIDES)
 */
@Mixin(NoiseRouter.class)
public class NoiseRouterHooks {
    @Shadow
    @Final
    private DensityFunction temperature;

    @Shadow
    @Final
    private DensityFunction vegetation;

    @Unique
    private NoiseRouter temperatureBands_$getSelf() {
        return (NoiseRouter)(Object) this;
    }

    @WrapOperation(
            method = "mapAll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/DensityFunction;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/DensityFunction;")
    )
    public DensityFunction onMapDensityFunction(DensityFunction instance, DensityFunction.Visitor visitor, Operation<DensityFunction> original) {
        DensityFunction currentFunction = instance;
        if (currentFunction.equals(temperature) || currentFunction.equals(vegetation)) {
            // First find the dimension that this NoiseRouter belongs to, and get existing dimensiondata if present
            String dimensionName = null;
            DimensionData activeDimData = null;
            for (Map.Entry<String, @NonNull DimensionData> dimDataEntry : TemperatureBands.DIMENSION_DATA_CACHE.asMap().entrySet()) {
                if (dimDataEntry.getValue().getNoiseRouter().equals(temperatureBands_$getSelf())) {
                    activeDimData = dimDataEntry.getValue();
                    dimensionName = dimDataEntry.getKey();
                }
            }
            if (activeDimData != null) {
                // dimension is whitelisted
                if (currentFunction.equals(temperature))
                    currentFunction = TemperatureBands.replaceNoiseIfNeeded(activeDimData, dimensionName, currentFunction, ShiftedNoiseTemperature.NAME);
                else if (currentFunction.equals(vegetation) && TemperatureBands.configHumidityAlgorithm != 0)
                    currentFunction = TemperatureBands.replaceNoiseIfNeeded(activeDimData, dimensionName, currentFunction, ShiftedNoiseHumidity.NAME);
            }
        }
        return original.call(currentFunction, visitor);
    }

}

