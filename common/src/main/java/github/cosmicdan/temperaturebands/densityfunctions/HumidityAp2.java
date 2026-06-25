package github.cosmicdan.temperaturebands.densityfunctions;

import github.cosmicdan.temperaturebands.ClimateTargetPointEx;
import github.cosmicdan.temperaturebands.CommonConfig;
import github.cosmicdan.temperaturebands.DimensionConfig;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.generator.BandsGenerator;
import github.cosmicdan.temperaturebands.generator.BiomeProximityGenerator;
import github.cosmicdan.temperaturebands.generator.IGenerator;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class HumidityAp2 extends Ap2NoiseEx {
    public HumidityAp2(String dimensionName, DimensionConfig config, DensityFunctions.TwoArgumentSimpleFunction funcOriginal) {
        super(dimensionName, config, funcOriginal);
    }

    @Override
    public IGenerator createGen() {
        if (config.humidityAlgorithm() == 1)
            return BandsGenerator.create(this, config, true, config.humidityTempWeight());
        else if (config.humidityAlgorithm() == 2)
            return BiomeProximityGenerator.create(this, config);
        else
            return TbUtils.doCrash("Temperature Bands has an unrecognized '" + CommonConfig.humidityAlgorithmName + "' setting of '" + config.humidityAlgorithm() + "'");
    }

    @Override
    public @NotNull DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new HumidityAp2(dimensionName, config, (DensityFunctions.TwoArgumentSimpleFunction) funcOriginal.mapAll(visitor)));
    }

    @Override
    public ClimateTargetPointEx sampleClimate(long packedBlockPos, boolean firstBiomeOnly) {
        // if firstBiomeOnly is true, we're just looking for oceans so only sampling continentalness is  required
        int blockX = TbUtils.unpackBlockFromLongX(packedBlockPos);
        int blockZ = TbUtils.unpackBlockFromLongZ(packedBlockPos);
        DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(blockX, BiomeProximityGenerator.blockY, blockZ);
        float temperatureResult = firstBiomeOnly ? 0.0f : (float) dimData.getTemperatureNoise().compute(singlePointContext);
        float humidityResult = firstBiomeOnly ? 0.0f : (float) computeOriginal(singlePointContext); // use original non-overridden densityfunctions for humidity
        float continentalnessResult = (float) dimData.getClimateSampler().continentalness().compute(singlePointContext);
        float erosionResult = firstBiomeOnly ? 0.0f : (float) dimData.getClimateSampler().erosion().compute(singlePointContext);
        float depthResult = firstBiomeOnly ? 0.0f : (float) dimData.getClimateSampler().depth().compute(singlePointContext);
        float weirdnessResult = firstBiomeOnly ? 0.0f : (float) dimData.getClimateSampler().weirdness().compute(singlePointContext);

        Climate.TargetPoint sampleResultRaw = Climate.target(temperatureResult, humidityResult, continentalnessResult, erosionResult, depthResult, weirdnessResult);
        return new ClimateTargetPointEx(sampleResultRaw, false);
    }

    @Override
    public Set<Holder<Biome>> getBiomesFirst() {
        return dimData.biomeOceans;
    }

    @Override
    public Set<Holder<Biome>> getBiomesSecond() {
        return dimData.biomeRivers;
    }

    @Override
    public void cancelAllCacheTasks() {
        gen.cancelAllCacheTasks();
    }
}
