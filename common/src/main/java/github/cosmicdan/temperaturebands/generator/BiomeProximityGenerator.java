package github.cosmicdan.temperaturebands.generator;

import github.cosmicdan.temperaturebands.*;
import github.cosmicdan.temperaturebands.climatesampler.IClimateSampler;
import github.cosmicdan.temperaturebands.climatesampler.RefinedSampler;
import github.cosmicdan.temperaturebands.climatesampler.SimpleSampler;
import net.minecraft.world.level.levelgen.DensityFunction;

public class BiomeProximityGenerator implements IGenerator {
    private final Config config;
    private final IClimateSampler biomeSampler;

    private DimensionData dimData;
    private boolean biomeSourceError = false;

    public record Config(
            DensityFunctionEx owner,
            boolean isTempDimension,
            int humidityAlgorithm,
            float algo2SecondBiomeInfluence,
            int algo2BiomeSearchDistance,
            int distanceFunction,
            float algo2BaseNoisePercent,
            float algo2MiddleWeight,
            float tempWeight,
            // algo3 config
            int algo3searchDistanceOcean,
            int algo3searchDistanceRiver,
            double algo3noiseWeightOcean,
            double algo3noiseWeightRiver,
            double algo3noiseRange,
            int algo3cellSize,
            int algo3cellSubsamplingOcean,
            int algo3cellSubsamplingRiver,
            int algo3shortcutsLevel
    ) {}

    public static BiomeProximityGenerator create(DensityFunctionEx owner, DimensionConfig dimConfig) {
        return new BiomeProximityGenerator(new BiomeProximityGenerator.Config(
                owner,
                dimConfig.type().equals(DimensionConfig.ConfigType.TEMP),
                dimConfig.humidityAlgorithm(),
                dimConfig.humidityRiverInfluence(),
                dimConfig.humiditySearchDistance(),
                dimConfig.distanceFunction(),
                dimConfig.humidityBaseNoisePercent(),
                dimConfig.humidityMiddleWeight(),
                dimConfig.humidityTempWeight(),
                // algo3 config
                dimConfig.humidityAlgo3searchDistanceOcean(),
                dimConfig.humidityAlgo3searchDistanceRiver(),
                dimConfig.humidityAlgo3noiseWeightOcean(),
                dimConfig.humidityAlgo3noiseWeightRiver(),
                dimConfig.humidityAlgo3noiseRange(),
                dimConfig.humidityAlgo3cellSize(),
                dimConfig.humidityAlgo3subsampleAmountOcean(),
                dimConfig.humidityAlgo3subsampleAmountRiver(),
                dimConfig.humidityAlgo3shortcutsLevel()
        ), dimConfig);
    }

    public BiomeProximityGenerator(Config config, DimensionConfig dimConfig) {
        this.config = config;
        if (config.humidityAlgorithm == 2) {
            biomeSampler = new SimpleSampler(config, dimConfig);
        } else if (config.humidityAlgorithm == 3) {
            biomeSampler = new RefinedSampler(config, dimConfig);
        } else {
            TbUtils.doCrash("Invalid 'humidityAlgorithm' value: " +  config.humidityAlgorithm);
            biomeSampler = null;
        }
    }

    private void onFirstCompute(DimensionData dimData) {
        this.dimData = dimData;
        if (dimData == null)
            TbUtils.doCrash("dimData must not be null");
        else if (dimData.getBiomeSource() == null)
            biomeSourceError = true;
        else {
            biomeSampler.onFirstCompute(dimData, config);
            if (config.humidityAlgorithm >= 3)
                TbUtils.determine2dDimensions(dimData.getClimateSampler(), CachedClimatePoint.IS_DIMENSION_2D);
        }
    }

    @Override
    public double onCompute(DensityFunction.FunctionContext context, DimensionData dimData) {
        if (this.dimData == null)
            onFirstCompute(dimData);
        if (biomeSourceError)
            return config.owner.computeOriginal(context);

        // Humidity has 5 levels, ranging from -1.0 (least humid) to +1.0 (most humid)
        // Level 0 = -1.00 to -0.35
        // Level 1 = -0.35 to -0.10
        // Level 2 = -0.10 to +0.10
        // Level 3 = +0.10 to +0.30
        // Level 4 = +0.30 to +1.00

        if (config.humidityAlgorithm == 2)
            return onComputeLegacy(context, dimData);
        else //if (config.humidityAlgorithm == 3)
            return biomeSampler.calculateHumidity(context, dimData);
    }

    private double onComputeLegacy(DensityFunction.FunctionContext context, DimensionData dimData) {
        double[] nearestFirstAndMaybeSecondBiomeDistance = biomeSampler.findNearestOceanRiver(context.blockX(), context.blockZ(), dimData);

        double nearestFirstBiome = nearestFirstAndMaybeSecondBiomeDistance[0];
        double nearestSecondBiomeMaybe = nearestFirstAndMaybeSecondBiomeDistance[1];

        // default value if biome wasn't found
        double noiseValue = -1.0;
        if (nearestFirstBiome >= 0.0) {
            // Determine densityfunctions (e.g. humidity) as a percentage (0.0 to 1.0)
            noiseValue = 1.0 - (nearestFirstBiome / config.algo2BiomeSearchDistance);
            // Convert from percentage to the range MC wants (vanilla expects -1.0 to +1.0)
            noiseValue = (noiseValue * 2.0) - 1.0;
        }

        if (nearestSecondBiomeMaybe >= 0.0) {
            // add second biome (e.g. river) influence
            double noiseValueSecondBiome = 1.0 - (nearestSecondBiomeMaybe / config.algo2BiomeSearchDistance);
            noiseValue = noiseValue + (noiseValueSecondBiome * config.algo2SecondBiomeInfluence);
        }

        if (config.algo2BaseNoisePercent > 0.0)
            // use humidityNoiseFactor for this point to soften the edges a bit
            noiseValue += (config.owner.computeOriginal(context) * config.algo2BaseNoisePercent);

        // apply some "middle weightiness"
        if (config.algo2MiddleWeight > 0.0)
            noiseValue = TbUtils.pullTowardsZeroLinear(noiseValue, config.algo2MiddleWeight);

        // finally do temperature adjustment
        if (config.tempWeight > 0.0) {
            double tempValue = dimData.getTemperatureNoise().compute(context);
            noiseValue += (tempValue * config.tempWeight);
        }

        return noiseValue;
    }
}
