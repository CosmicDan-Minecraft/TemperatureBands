package github.cosmicdan.temperaturebands.noise;

import github.cosmicdan.temperaturebands.*;
import github.cosmicdan.temperaturebands.generator.BandsGenerator;
import github.cosmicdan.temperaturebands.generator.BiomeProximityGenerator;
import github.cosmicdan.temperaturebands.generator.BiomeProximityGeneratorOwner;
import github.cosmicdan.temperaturebands.generator.IGenerator;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static github.cosmicdan.temperaturebands.TemperatureBands.*;

public class ShiftedNoiseHumidity extends ShiftedNoiseEx implements BiomeProximityGeneratorOwner {
    public static final String NAME = "humidity (vegetation)";

    private final IGenerator gen;

    private DimensionData dimData = null;

    public ShiftedNoiseHumidity(String dimensionName, DimensionConfig config, DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, NoiseHolder noise) {
        super(dimensionName, config, shiftX, shiftY, shiftZ, xzScale, yScale, noise);
        if (config.humidityAlgorithm() == 2) {
            gen = new BiomeProximityGenerator(new BiomeProximityGenerator.Config(
                    this,
                    dimensionName,
                    config.type().equals(DimensionConfig.ConfigType.TEMP),
                    config.humidityResolution(),
                    config.climateSamplerResolution(),
                    config.humidityRiverInfluence(),
                    config.humiditySearchDistance(),
                    config.distanceFunction(),
                    config.humidityBaseNoisePercent(),
                    config.humidityMiddleWeight(),
                    config.humidityTempWeight()
            ));
        } else if (config.humidityAlgorithm() == 1) {
            gen = new BandsGenerator(new BandsGenerator.Config(
                    this,
                    !config.useVerticalBands(), // opposite to configured temp band direction
                    Math.round(config.bandSize() * config.humidityAlgo1MimicScale()),
                    config.bandPositionShift(),
                    config.tempRange(),
                    config.tempGradeShift(),
                    config.noiseFactor(),
                    config.algo1bandVariance(),
                    config.algo1bandVarianceSteepness(),
                    config.humidityTempWeight()
            ));
        } else {
            TbUtils.doCrash("Temperature Bands has an unrecognized '" + CommonConfig.humidityAlgorithmName + "' setting of '" + config.humidityAlgorithm() + "'");
            gen = null;
        }
    }

    @Override
    public double compute(FunctionContext context) {
        if (dimData == null) {
            dimData = DIMENSION_DATA_CACHE.getIfPresent(dimensionName);
            if (dimData == null)
                TbUtils.doCrash("Couldn't find DimensionData on first compute! Eh?");
        }
        return gen.compute(context, dimData);
    }

    @Override
    public void fillArray(double[] ds, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(ds, this);
    }

    @Override
    public @NotNull DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(
                new ShiftedNoiseHumidity(
                        dimensionName, config, this.shiftX.mapAll(visitor), this.shiftY.mapAll(visitor), this.shiftZ.mapAll(visitor), this.xzScale, this.yScale, visitor.visitNoise(this.noise)
                )
        );
    }

    @Override
    public double minValue() {
        return -this.maxValue();
    }

    @Override
    public double maxValue() {
        return this.noise.maxValue();
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return DensityFunctions.ShiftedNoise.CODEC;
    }

    public void cancelAllCacheTasks() {
        gen.cancelAllCacheTasks();
    }

    @Override
    public ClimateTargetPointEx sampleClimate(long packedBlockPos, boolean firstBiomeOnly) {
        // if firstBiomeOnly is true, we're just looking for oceans so only sampling continentalness is  required
        int blockX = TbUtils.unpackBlockFromLongX(packedBlockPos);
        int blockZ = TbUtils.unpackBlockFromLongZ(packedBlockPos);
        DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(blockX, BiomeProximityGenerator.blockY, blockZ);
        //float temperatureResult = firstBiomeOnly ? 0.0f : (float) owner.getTemperatureNoise().compute(singlePointContext);
        float temperatureResult = firstBiomeOnly ? 0.0f : (float) dimData.getClimateSampler().temperature().compute(singlePointContext);
        float humidityResult = firstBiomeOnly ? 0.0f : (float) gen.computeOriginal(singlePointContext); // use original non-overridden noise for humidity
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
    public DensityFunction getShiftX() {
        return this.shiftX;
    }

    @Override
    public DensityFunction getShiftY() {
        return this.shiftY;
    }

    @Override
    public DensityFunction getShiftZ() {
        return this.shiftZ;
    }

    @Override
    public double getXZScale() {
        return this.xzScale;
    }

    @Override
    public double getYScale() {
        return this.yScale;
    }

    @Override
    public NoiseHolder getNoise() {
        return this.noise;
    }
}
