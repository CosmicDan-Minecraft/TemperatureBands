package github.cosmicdan.temperaturebands.generator;

import com.github.benmanes.caffeine.cache.*;
import github.cosmicdan.temperaturebands.DimensionConfig;
import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.DensityFunctionEx;
import github.cosmicdan.temperaturebands.waterlocator.IWaterLocator;
import github.cosmicdan.temperaturebands.waterlocator.SimpleWaterLocator;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.*;

public class BiomeProximityGenerator implements IGenerator {
    public static final int blockY = 64; // TODO: Change to zero. Or just use 3D cache? Make it configurable. 3D cache improves support but costs twice as much cache space.

    private final Config config;
    // initialized in constructor and/or derived from config arg
    private final IWaterLocator waterLocator;

    private DimensionData dimData;
    private boolean biomeSourceError = false;

    public record Config(
            DensityFunctionEx owner,
            boolean isTempDimension,
            int noiseResolution,
            int climateSamplerResolutionRaw,
            float secondBiomeInfluence,
            int biomeSearchDistance,
            int distanceFunction,
            float baseNoisePercent,
            float middleWeight,
            float tempWeight
    ) {}

    public static BiomeProximityGenerator create(DensityFunctionEx owner, DimensionConfig config) {
        return new BiomeProximityGenerator(new BiomeProximityGenerator.Config(
                owner,
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
    }

    public BiomeProximityGenerator(Config config) {
        this.config = config;
        if (true) {
            waterLocator = new SimpleWaterLocator(config);
        }
    }

    private void onFirstCompute(DimensionData dimData) {
        this.dimData = dimData;
        if (dimData == null)
            TbUtils.doCrash("dimData must not be null");
        else if (dimData.getBiomeSource() == null)
            biomeSourceError = true;
    }

    @Override
    public double onCompute(DensityFunction.FunctionContext context, DimensionData dimData) {
        if (this.dimData == null)
            onFirstCompute(dimData);
        if (biomeSourceError)
            return config.owner.computeOriginal(context);

        // default value if biome wasn't found
        double noiseValue = -1.0;
        /*
        Pair<Double, Double> nearestFirstAndMaybeSecondBiomeDistance = getDistanceToNearestBiomesSimple(
                originPosX,
                originPosZ,
                config.owner.getBiomesFirst(),
                config.owner.getBiomesSecond(),
                config.biomeSearchDistance,
                samplerResolution
        );
         */
        Pair<Double, Double> nearestFirstAndMaybeSecondBiomeDistance = waterLocator.findNearestOceanRiver(context.blockX(), context.blockZ(), dimData);

        double nearestFirstBiome = nearestFirstAndMaybeSecondBiomeDistance.getLeft();
        double nearestSecondBiomeMaybe = nearestFirstAndMaybeSecondBiomeDistance.getRight();
        // Humidity has 5 levels, ranging from -1.0 (least humid) to +1.0 (most humid)
        // Level 0 = -1.00 to -0.35
        // Level 1 = -0.35 to -0.10
        // Level 2 = -0.10 to +0.10
        // Level 3 = +0.10 to +0.30
        // Level 4 = +0.30 to +1.00

        if (nearestFirstBiome >= 0.0) {
            // Determine densityfunctions (e.g. humidity) as a percentage (0.0 to 1.0)
            noiseValue = 1.0 - (nearestFirstBiome / config.biomeSearchDistance);
            // Convert from percentage to the range MC wants (vanilla expects -1.0 to +1.0)
            noiseValue = (noiseValue * 2.0) - 1.0;
        }

        if (nearestSecondBiomeMaybe >= 0.0) {
            // add second biome (e.g. river) influence
            double noiseValueSecondBiome = 1.0 - (nearestSecondBiomeMaybe / config.biomeSearchDistance);
            noiseValue = noiseValue + (noiseValueSecondBiome * config.secondBiomeInfluence);
        }

        if (config.baseNoisePercent > 0.0)
            // use humidityNoiseFactor for this point to soften the edges a bit
            noiseValue += (config.owner.computeOriginal(context) * config.baseNoisePercent);

        // apply some "middle weightiness"
        if (config.middleWeight > 0.0)
            noiseValue = TbUtils.pullTowardsZeroLinear(noiseValue, config.middleWeight);

        // finally do temperature adjustment
        if (config.tempWeight > 0.0) {
            double tempValue = dimData.getTemperatureNoise().compute(context);
            noiseValue += (tempValue * config.tempWeight);
        }

        return noiseValue;
    }
}
