package github.cosmicdan.temperaturebands.densityfunctions;

import github.cosmicdan.temperaturebands.ClimateTargetPointEx;
import github.cosmicdan.temperaturebands.generator.IGenerator;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Set;

public interface OwnerFunction {
    String HUMIDITY_NAME = "humidity (vegetation)";
    String TEMPERATURE_NAME = "temperature";

    IGenerator createGen();
    double computeOriginal(DensityFunction.FunctionContext context);
    ClimateTargetPointEx sampleClimate(long packedBlockPos, boolean firstBiomeOnly);
    Set<Holder<Biome>> getBiomesFirst();
    Set<Holder<Biome>> getBiomesSecond();
    void cancelAllCacheTasks();
}
