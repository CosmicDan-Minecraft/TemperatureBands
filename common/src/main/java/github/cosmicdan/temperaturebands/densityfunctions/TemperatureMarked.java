package github.cosmicdan.temperaturebands.densityfunctions;

import github.cosmicdan.temperaturebands.ClimateTargetPointEx;
import github.cosmicdan.temperaturebands.CommonConfig;
import github.cosmicdan.temperaturebands.DimensionConfig;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.generator.BandsGenerator;
import github.cosmicdan.temperaturebands.generator.IGenerator;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TemperatureMarked extends MarkedNoiseEx implements OwnerFunction {
    public TemperatureMarked(String dimensionName, DimensionConfig config, DensityFunction funcOriginal) {
        super(dimensionName, config, funcOriginal);
    }

    @Override
    public IGenerator createGen() {
        if (config.bandAlgorithm() == 1)
            return BandsGenerator.create(this, config, false, -1.0f);
        else
            return TbUtils.doCrash("Temperature Bands has an unrecognized '" + CommonConfig.bandAlgorithmName + "' setting of '" + config.bandAlgorithm() + "'");
    }

    @Override
    public @NotNull DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new TemperatureMarked(dimensionName, config, funcOriginal.mapAll(visitor)));
    }

    @Override
    public ClimateTargetPointEx sampleClimate(long packedBlockPos, boolean firstBiomeOnly) {
        // not used by any temperature algorithm
        return null;
    }

    @Override
    public Set<Holder<Biome>> getBiomesFirst() {
        // not used by any temperature algorithm
        return null;
    }

    @Override
    public Set<Holder<Biome>> getBiomesSecond() {
        // not used by any temperature algorithm
        return null;
    }

    @Override
    public void cancelAllCacheTasks() {
        gen.cancelAllCacheTasks();
    }
}
