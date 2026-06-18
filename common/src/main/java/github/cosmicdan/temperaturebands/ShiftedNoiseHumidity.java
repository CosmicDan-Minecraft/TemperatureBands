package github.cosmicdan.temperaturebands;

import com.github.benmanes.caffeine.cache.Cache;
import github.cosmicdan.temperaturebands.mixin.MultiNoiseBiomeSourceInvoker;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jetbrains.annotations.NotNull;

import static github.cosmicdan.temperaturebands.TemperatureBands.*;

public class ShiftedNoiseHumidity extends ShiftedNoiseEx {
    public static final String NAME = "humidity (vegetation)";
    private static final int blockY = 64;
    private DimensionData dimData = null;
    private MultiNoiseBiomeSourceInvoker biomeSourceInvoker = null;
    //private long cacheHit = 0;
    //private long cacheTotal = 0;
    private int climateSamplerResolutionActual;

    public ShiftedNoiseHumidity(String dimensionName, DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, NoiseHolder noise) {
        super(dimensionName, shiftX, shiftY, shiftZ, xzScale, yScale, noise);
    }

    @Override
    public String getName() {
        return NAME;
    }

    private int scalePosValue(int pos, int offset) {
        int posPreScaled = (pos >> humidityResolution) + offset;
        return (posPreScaled * dimData.humidityPartSize) + dimData.partSizeMiddleOffset;
    }

    @Override
    public double compute(FunctionContext context) {
        if (dimData == null) {
            dimData = DIMENSION_DATA_CACHE.getIfPresent(dimensionName);
            if (dimData == null)
                throw new RuntimeException("Humidity function trying to compute before ServerLevel has been made. Eh?");
            biomeSourceInvoker = (MultiNoiseBiomeSourceInvoker) dimData.getBiomeSource();
            if (climateSamplerResolution < 1)
                climateSamplerResolutionActual = Math.round(humidityResolution * 0.5f);
        }
        if (!dimData.isHumidityEnabled) {
            return computeNoise(context);
        } else {
            int originPosX = scalePosValue(context.blockX(), 0);
            int originPosZ = scalePosValue(context.blockZ(), 0);

            Double humidityValue = dimData.getHumidityCached(originPosX, originPosZ);
            //cacheTotal++;
            if (humidityValue != null) {
                // chunk humidity already exists in cache
                //cacheHit++;
                return humidityValue;
            } else {
                //BlockPos nearestOceanPos = findClosestBiome(origin, biomeOceans, humiditySearchDistance, humiditySearchStep);
                double nearestOceanOrRiverDistance = getDistanceToNearestBiome(originPosX, originPosZ, dimData.biomeRiversAndOceans, humiditySearchDistance, climateSamplerResolutionActual);

                if (nearestOceanOrRiverDistance < 0) {
                    // not found, i.e. maximum search distance was reached
                    humidityValue = -1.0;
                } else {
                    humidityValue = (nearestOceanOrRiverDistance / humiditySearchDistance);
                    // value now a percentage of "dryness", let's apply some "middle weightiness"
                    //humidityValue = calcMiddleWeightedValue(humidityValue);
                    // double it to get a range from 0.0 to 2.0, then take 1.0 to get the range MC expects
                    humidityValue = (humidityValue * 2.0) - 1.0;
                    // finally, invert it since we had "dryness" but want "humidity"
                    humidityValue = -humidityValue;
                    // Humidity has 5 levels, ranging from -1.0 (least humid) to +1.0 (most humid)
                    // Level 0 = -1.00 to -0.35
                    // Level 1 = -0.35 to -0.10
                    // Level 2 = -0.10 to +0.10
                    // Level 3 = +0.10 to +0.30
                    // Level 4 = +0.30 to +1.00
                }

                //if (humidityRiverValue > 0.0f) {
                //    BlockPos nearestRiver = findClosestBiome(origin, biomeRivers);
                //}
                dimData.setHumidityCached(originPosX, originPosZ, humidityValue);
                //LOGGER.info("~~~ Set cached value for {}x{} to {}", originPosX, originPosZ, humidityValue);
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
    private double getDistanceToNearestBiome(int originX, int originZ, Cache<Holder<Biome>, Boolean> biomesToMatch, int searchRadiusXZ, int searchStep) {
        double angle = 0;
        double radius = 0;

        while (radius < searchRadiusXZ) {
            double xOffset = radius * Math.cos(angle);
            double zOffset = radius * Math.sin(angle);
            int blockX = (int) Math.round(originX + xOffset);
            int blockZ = (int) Math.round(originZ + zOffset);
            Holder<Biome> holder = getNoiseBiomeVanilla(blockX, blockZ);
            if (biomesToMatch.getIfPresent(holder) != null) {
                if (humidityUseManhattanDistance) {
                    return Math.abs(originX - blockX) + Math.abs(originZ - blockZ);
                } else {
                    return Math.hypot(Math.abs(originX - blockX), Math.abs(originZ - blockZ));
                }
            }

            angle += 0.5;
            radius += (searchStep) / 6.28; // 6.28 = 2π
        }
        return -1;
    }

    private double calcMiddleWeightedValue(double value) {
        if (value < 0.5) {
            return Math.pow(value * 2, humidityCenterWeight) * 0.5;
        } else {
            return 1.0 - Math.pow((1.0 - value) * 2, humidityCenterWeight) * 0.5;
        }
    }

    private Holder<Biome> getNoiseBiomeVanilla(int blockX, int blockZ) {
        ClimateTargetPointEx targetPointEx = sampleCachedClimateWithVanillaHumidity(blockX, blockZ);
        return biomeSourceInvoker.getParameters().findValue(targetPointEx.targetPoint());
    }

    public ClimateTargetPointEx sampleCachedClimateWithVanillaHumidity(int blockX, int blockZ) {
        //cacheTotal++;
        ClimateTargetPointEx sampleResult = dimData.getClimateSampleCached(blockX, blockZ);
        //if (sampleResult != null)
            //cacheHit++;
        // else {
        if (sampleResult == null) {
            DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
            Climate.TargetPoint sampleResultRaw = Climate.target(
                    (float) dimData.getTemperatureFunction().compute(singlePointContext),
                    //(float) sampler.humidity().compute(singlePointContext),
                    (float) computeNoise(singlePointContext), // use original non-overridden noise for humidity
                    (float) dimData.getClimateSampler().continentalness().compute(singlePointContext),
                    (float) dimData.getClimateSampler().erosion().compute(singlePointContext),
                    (float) dimData.getClimateSampler().depth().compute(singlePointContext),
                    (float) dimData.getClimateSampler().weirdness().compute(singlePointContext)
            );
            sampleResult = new ClimateTargetPointEx(sampleResultRaw, false);
            dimData.setClimateSampleCached(blockX, blockZ, sampleResult);
        }
        return sampleResult;
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
