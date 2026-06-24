package github.cosmicdan.temperaturebands.generator;

import github.cosmicdan.temperaturebands.ClimateTargetPointEx;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Set;

public interface BiomeProximityGeneratorOwner {
    ClimateTargetPointEx sampleClimate(long packedBlockPos, boolean firstBiomeOnly);
    double computeOriginal(DensityFunction.FunctionContext context);
    Set<Holder<Biome>> getBiomesFirst();
    Set<Holder<Biome>> getBiomesSecond();
    DensityFunction.NoiseHolder getNoise();
}
