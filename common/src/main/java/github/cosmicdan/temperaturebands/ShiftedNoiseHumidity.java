package github.cosmicdan.temperaturebands;

import com.github.benmanes.caffeine.cache.*;
import github.cosmicdan.temperaturebands.mixin.MultiNoiseBiomeSourceInvoker;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static github.cosmicdan.temperaturebands.TemperatureBands.*;

public class ShiftedNoiseHumidity extends ShiftedNoiseEx {
    public static final String NAME = "humidity (vegetation)";
    private static final int blockY = 64;
    private final AsyncLoadingCache<Long, ClimateTargetPointEx> climateSamplerCache;
    private DimensionData dimData = null;
    private MultiNoiseBiomeSourceInvoker biomeSourceInvoker = null;
    private int climateSamplerResolutionActual;
    private boolean oceansOnly = false;

    public ShiftedNoiseHumidity(String dimensionName, DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, NoiseHolder noise) {
        super(dimensionName, shiftX, shiftY, shiftZ, xzScale, yScale, noise);
        if (configClimateSamplerCacheSize > 0) {
            Caffeine<Long, ClimateTargetPointEx> cacheBuilder = Caffeine.newBuilder().maximumWeight((long) configClimateSamplerCacheSize * 1024 * 1024).weigher((key, value) -> value.sizeWithLongKey());
            climateSamplerCache = cacheBuilder.buildAsync(this::sampleClimate);
        } else {
            climateSamplerCache = null;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    private int scalePosValueForHumidity(int pos, int offset) {
        int posPreScaled = (pos >> configHumidityResolution) + offset;
        return (posPreScaled * dimData.humidityPartSize) + dimData.partSizeMiddleOffset;
    }

    @Override
    public double compute(FunctionContext context) {
        if (dimData == null) {
            dimData = DIMENSION_DATA_CACHE.getIfPresent(dimensionName);
            if (dimData == null)
                throw new RuntimeException("Humidity function trying to compute before ServerLevel has been made. Eh?");
            biomeSourceInvoker = (MultiNoiseBiomeSourceInvoker) dimData.getBiomeSource();
            climateSamplerResolutionActual = configClimateSamplerResolution;
            if (climateSamplerResolutionActual < 0)
                climateSamplerResolutionActual = configHumidityResolution * configHumidityResolution * configHumidityResolution;
            if (configHumidityRiverInfluence == 0.0)
                oceansOnly = true;
        }
        if (!dimData.isHumidityEnabled) {
            return computeNoise(context);
        } else {
            int originPosX = scalePosValueForHumidity(context.blockX(), 0);
            int originPosZ = scalePosValueForHumidity(context.blockZ(), 0);

            // default humidity if biome wasn't found
            double humidityValue = -1.0;
            // TODO: Redo the "ocean only" option when humidityRiverInfluence = 0.0 (and benchmark then add % increase note to config comment)
            Pair<Double, Double> nearestOceanAndRiverDistance = getDistanceToNearestBiomes(context, originPosX, originPosZ, dimData.biomeOceans, dimData.biomeRivers, configHumiditySearchDistance, climateSamplerResolutionActual);
            double nearestOcean = nearestOceanAndRiverDistance.getLeft();
            double nearestRiver = nearestOceanAndRiverDistance.getRight();
            // Humidity has 5 levels, ranging from -1.0 (least humid) to +1.0 (most humid)
            // Level 0 = -1.00 to -0.35
            // Level 1 = -0.35 to -0.10
            // Level 2 = -0.10 to +0.10
            // Level 3 = +0.10 to +0.30
            // Level 4 = +0.30 to +1.00

            if (nearestOcean >= 0.0) {
                // Determine humidity as a percentage (0.0 to 1.0)
                humidityValue = 1.0 - (nearestOcean / configHumiditySearchDistance);
                // Then apply some "middle weightiness"
                //humidityValue = calcMiddleWeightedValue(humidityValue);
                // Finally, clamp it to the range MC wants (vanilla expects -1.0 to +1.0)
                humidityValue = (humidityValue * 2.0) - 1.0;
            }

            if (nearestRiver >= 0.0) {
                // add river influence
                double humidityValue2 = 1.0 - (nearestRiver / configHumiditySearchDistance);
                humidityValue = humidityValue + (humidityValue2 * configHumidityRiverInfluence);
            }

            // use humidityNoiseFactor for this point to soften the edges a bit
            //double noiseOriginal = computeNoise(context) * humidityNoiseFactor;

            /*
            if (cacheTotal > 131072) {
                long cacheSize = dimData.getClimateSampleCacheSize();
                LOGGER.info("Climate Sampler cache hitrate for past {} calculations = {}%. Cache is {}% full ({}/{})", cacheTotal, String.format("%.1f", (cacheHit / (float) cacheTotal) * 100), String.format("%.1f", (cacheSize / (float) climateSamplerCacheSize) * 100), cacheSize, climateSamplerCacheSize);
                cacheTotal = 0;
                cacheHit = 0;
            }
             */

            //LOGGER.info("~~~ Cache hit = {}", cacheHit);
            return humidityValue;
        }
    }

    // Simple 2D archimedean spiral check
    private Pair<Double, Double> getDistanceToNearestBiomes(FunctionContext context, int originX, int originZ, Set<Holder<Biome>> firstBiomes, @Nullable Set<Holder<Biome>> secondBiomes, int searchRadiusXZ, int searchStep) {
        double angle = 0;
        double radius = 0;
        double firstBiomeDistance = -1.0;
        double secondBiomeDistance = -1.0;

        while (radius < searchRadiusXZ) {
            double xOffset = radius * Math.cos(angle);
            double zOffset = radius * Math.sin(angle);
            int blockX = (int) Math.round(originX + xOffset);
            int blockZ = (int) Math.round(originZ + zOffset);
            Holder<Biome> holder = getNoiseBiome(context, blockX, blockZ);
            if (firstBiomeDistance == -1.0 && firstBiomes.contains(holder))
                firstBiomeDistance = calculateBlockDistance(originX, blockX, originZ, blockZ);
            else if (!oceansOnly && secondBiomeDistance == -1.0 && secondBiomes.contains(holder))
                secondBiomeDistance = calculateBlockDistance(originX, blockX, originZ, blockZ);

            if (firstBiomeDistance > -1.0 && (secondBiomeDistance > -1.0 || oceansOnly))
                return Pair.of(firstBiomeDistance, secondBiomeDistance);

            angle += 0.5;
            radius += (searchStep) / 6.28; // 6.28 = 2π
        }
        return Pair.of(firstBiomeDistance, secondBiomeDistance);
    }

    /*
    private double calcMiddleWeightedValue(double value) {
        if (value < 0.5) {
            return Math.pow(value * 2, humidityCenterWeight) * 0.5;
        } else {
            return 1.0 - Math.pow((1.0 - value) * 2, humidityCenterWeight) * 0.5;
        }
    }
     */

    private Holder<Biome> getNoiseBiome(FunctionContext context, int blockX, int blockZ) {
        final ClimateTargetPointEx targetPointEx;
        if (configClimateSamplerCacheSize > 0) {
            targetPointEx = sampleClimateCached(context, blockX, blockZ);
        } else {
            targetPointEx = sampleClimate(DimensionData.packBlockXZtoLong(blockX, blockZ));
        }
        return biomeSourceInvoker.getParameters().findValue(targetPointEx.targetPoint());
    }

    public ClimateTargetPointEx sampleClimateCached(FunctionContext context, int blockX, int blockZ) {
        ClimateTargetPointEx result = climateSamplerCache.get(DimensionData.packBlockXZtoLong(blockX, blockZ)).join();
        if (configClimateSamplerCachePrefetchRadius > 0 && configClimateSamplerCacheSize > 0) {
            // another spiral. Cbf making the methods common.
            double angle = 0;
            double radius = 0;
            List<Long> points = new ArrayList<>();
            while (radius <= configClimateSamplerCachePrefetchRadius) {
                double xOffset = radius * Math.cos(angle);
                double zOffset = radius * Math.sin(angle);
                int thisX = (int) Math.round(blockX + xOffset);
                int thisZ = (int) Math.round(blockZ + zOffset);
                points.add(DimensionData.packBlockXZtoLong(thisX, thisZ));
                angle += 0.5;
                radius += climateSamplerResolutionActual / 6.28; // 6.28 = 2π
            }
            climateSamplerCache.getAll(points);
        }
        return result;
    }

    private ClimateTargetPointEx sampleClimate(long packedBlockPos) {
        int blockX = DimensionData.unpackBlockFromLongX(packedBlockPos);
        int blockZ = DimensionData.unpackBlockFromLongZ(packedBlockPos);
        DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
        //float temperatureResult = searchOpts.equals(ClimateSearchResults.OCEANS_ONLY) ? 0.0f : (float) dimData.getTemperatureFunction().compute(singlePointContext);
        float temperatureResult = oceansOnly ? 0.0f : (float) dimData.getClimateSampler().temperature().compute(singlePointContext);
        float humidityResult = oceansOnly ? 0.0f : (float) computeNoise(singlePointContext); // use original non-overridden noise for humidity
        float continentalnessResult = (float) dimData.getClimateSampler().continentalness().compute(singlePointContext);
        float erosionResult = oceansOnly ? 0.0f : (float) dimData.getClimateSampler().erosion().compute(singlePointContext);
        float depthResult = oceansOnly ? 0.0f : (float) dimData.getClimateSampler().depth().compute(singlePointContext);
        float weirdnessResult = oceansOnly ? 0.0f : (float) dimData.getClimateSampler().weirdness().compute(singlePointContext);

        Climate.TargetPoint sampleResultRaw = Climate.target(temperatureResult, humidityResult, continentalnessResult, erosionResult, depthResult, weirdnessResult);
        return new ClimateTargetPointEx(sampleResultRaw, false);
    }

    private double computeNoise(FunctionContext context) {
        double d = context.blockX() * this.xzScale + this.shiftX.compute(context);
        double e = context.blockY() * this.yScale + this.shiftY.compute(context);
        double f = context.blockZ() * this.xzScale + this.shiftZ.compute(context);
        return this.noise.getValue(d, e, f);
    }

    @Override
    public void fillArray(double[] ds, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(ds, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(
                new ShiftedNoiseHumidity(
                        dimensionName, this.shiftX.mapAll(visitor), this.shiftY.mapAll(visitor), this.shiftZ.mapAll(visitor), this.xzScale, this.yScale, visitor.visitNoise(this.noise)
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
}
