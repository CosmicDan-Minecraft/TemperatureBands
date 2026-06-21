package github.cosmicdan.temperaturebands;

import com.github.benmanes.caffeine.cache.*;
import github.cosmicdan.temperaturebands.mixin.MultiNoiseBiomeSourceInvoker;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static github.cosmicdan.temperaturebands.TemperatureBands.*;

public class ShiftedNoiseHumidity extends ShiftedNoiseEx {
    public static final String NAME = "humidity (vegetation)";
    private static final int blockY = 64;

    private final AsyncLoadingCache<Long, ClimateTargetPointEx> climateSamplerCache;
    private final ConcurrentMap<Long, CompletableFuture<ClimateTargetPointEx>> climateSamplerCacheActiveFutures;
    //private final Set<CompletableFuture<?>> climateSamplerCacheActiveFutures;
    private final int climateSamplerResolutionActual;
    private final boolean oceansOnly;
    private final int humidityPartSize;
    private final int partSizeMiddleOffset;

    private DimensionData owner = null;
    private boolean biomeSourceError = false;
    private MultiNoiseBiomeSourceInvoker biomeSourceInvoker;
    private boolean samplerCacheCooldownElapsed = false;

    public ShiftedNoiseHumidity(String dimensionName, DimensionConfig config, DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, NoiseHolder noise) {
        super(dimensionName, config, shiftX, shiftY, shiftZ, xzScale, yScale, noise);
        if (CONFIG_GLOBAL.climateSamplerCacheSize() > 0) {
            //climateSamplerCacheActiveFutures = ConcurrentHashMap.newKeySet();
            // create loader
            climateSamplerCacheActiveFutures = new ConcurrentHashMap<>();
            AsyncCacheLoader<Long, ClimateTargetPointEx> climateSamplerCacheLoader = new AsyncCacheLoader<>() {
                @Override
                public @NotNull CompletableFuture<? extends ClimateTargetPointEx> asyncLoad(@NotNull Long packedBlockPos, @NotNull Executor executor) {
                    //return CompletableFuture.supplyAsync(() -> ShiftedNoiseHumidity.this.sampleClimate(packedBlockPos), executor);
                    CompletableFuture<ClimateTargetPointEx> future = new CompletableFuture<>() {
                        @Override
                        public boolean cancel(boolean mayInterruptIfRunning) {
                            boolean result = super.cancel(mayInterruptIfRunning);
                            if (result) {
                                // Remove from your map when the future is explicitly cancelled
                                climateSamplerCacheActiveFutures.remove(packedBlockPos);
                            }
                            return result;
                        }
                    };

                    // Put the future into your tracked map immediately
                    climateSamplerCacheActiveFutures.put(packedBlockPos, future);

                    // Perform your expensive asynchronous operation on the provided executor
                    CompletableFuture.runAsync(() -> {
                        try {
                            // Example of heavy computation or HTTP request
                            future.complete(sampleClimateFuture(packedBlockPos, future));
                        } catch (CancellationException e) {
                            future.completeExceptionally(e);
                        //} catch (Exception e) {
                        //    future.completeExceptionally(e);
                        } finally {
                            climateSamplerCacheActiveFutures.remove(packedBlockPos);
                        }
                    }, executor);

                    return future;
                }
            };
            climateSamplerCache = Caffeine.newBuilder()
                    .maximumWeight((long) CONFIG_GLOBAL.climateSamplerCacheSize() * 1024 * 1024)
                    .weigher((Long key, ClimateTargetPointEx value) -> value.sizeWithLongKey())
                    .buildAsync(climateSamplerCacheLoader);
            /*
            climateSamplerCache = Caffeine.newBuilder()
                    .maximumWeight((long) CONFIG_GLOBAL.climateSamplerCacheSize() * 1024 * 1024)
                    .weigher((Long key, ClimateTargetPointEx value) -> value.sizeWithLongKey())
                    .buildAsync(this::sampleClimateInterruptable);
             */
        } else {
            climateSamplerCacheActiveFutures = null;
            climateSamplerCache = null;
        }
        if (config.climateSamplerResolution() < 0)
            climateSamplerResolutionActual = config.humidityResolution() * config.humidityResolution() * config.humidityResolution();
        else
            climateSamplerResolutionActual = config.climateSamplerResolution();
        oceansOnly = (config.humidityRiverInfluence() == 0.0);
        humidityPartSize = config.humidityResolution() * config.humidityResolution();
        partSizeMiddleOffset = (int)Math.round(humidityPartSize * 0.5);
        if (config.type().equals(DimensionConfig.ConfigType.TEMP))
            samplerCacheCooldownElapsed = true; // ignore prefetch cooldown for temporary worlds (e.g. world preview)
        else if (CONFIG_GLOBAL.climateSamplerCacheDelay() == 0)
            samplerCacheCooldownElapsed = true; // also ignore when delay is set to zero
    }

    /*
    private CompletableFuture<ClimateTargetPointEx> sampleClimateInterruptable(Long packedBlockPos, Executor executor) {
        CompletableFuture<ClimateTargetPointEx> future = new CompletableFuture<>();
        executor.execute(() -> {
            Thread currentThread = Thread.currentThread();
            future.whenComplete((result, throwable) -> {
                if (future.isCancelled()) {
                    currentThread.interrupt();
                }
            });
            try {
                future.complete(sampleClimate(packedBlockPos));
            } catch (Exception ignored) {
                //future.completeExceptionally(ignored);
            }
        });
        return future;
    }
     */

    @Override
    public double computeOriginal(FunctionContext context) {
        double d = context.blockX() * this.xzScale + this.shiftX.compute(context);
        double e = context.blockY() * this.yScale + this.shiftY.compute(context);
        double f = context.blockZ() * this.xzScale + this.shiftZ.compute(context);
        return this.noise.getValue(d, e, f);
    }

    private void onFirstCompute() {
        owner = DIMENSION_DATA_CACHE.getIfPresent(dimensionName);
        if (owner == null)
            throw new RuntimeException("Couldn't find DimensionData on first compute! Eh?");
        final BiomeSource biomeSource = owner.getBiomeSource();
        if (biomeSource instanceof MultiNoiseBiomeSource biomeSourceNoise) {
            biomeSourceInvoker = (MultiNoiseBiomeSourceInvoker) biomeSourceNoise;
        } else {
            // we already logged error about biomeSource not being MultiNoiseBiomeSource, just set a flag and move on
            biomeSourceError = true;
        }
    }

    @Override
    public double compute(FunctionContext context) {
        if (owner == null)
            onFirstCompute();
        if (biomeSourceError)
            return computeOriginal(context);
        int originPosX = scalePosValueForHumidity(context.blockX(), 0);
        int originPosZ = scalePosValueForHumidity(context.blockZ(), 0);

        // default humidity if biome wasn't found
        double humidityValue = -1.0;
        Pair<Double, Double> nearestOceanAndRiverDistance = getDistanceToNearestBiomes(context, originPosX, originPosZ, owner.biomeOceans, owner.biomeRivers, config.humiditySearchDistance(), climateSamplerResolutionActual);
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
            humidityValue = 1.0 - (nearestOcean / config.humiditySearchDistance());
            // Then apply some "middle weightiness"
            //humidityValue = calcMiddleWeightedValue(humidityValue);
            // Finally, clamp it to the range MC wants (vanilla expects -1.0 to +1.0)
            humidityValue = (humidityValue * 2.0) - 1.0;
        }

        if (nearestRiver >= 0.0) {
            // add river influence
            double humidityValue2 = 1.0 - (nearestRiver / config.humiditySearchDistance());
            humidityValue = humidityValue + (humidityValue2 * config.humidityRiverInfluence());
        }

        // use humidityNoiseFactor for this point to soften the edges a bit
        //double noiseOriginal = computeNoise(context) * humidityNoiseFactor;

        //LOGGER.info("~~~ Cache hit = {}", cacheHit);
        return humidityValue;
    }

    private int scalePosValueForHumidity(int pos, int offset) {
        int posPreScaled = (pos >> config.humidityResolution()) + offset;
        return (posPreScaled * humidityPartSize) + partSizeMiddleOffset;
    }

    // Simple 2D Archimedean spiral check
    private Pair<Double, Double> getDistanceToNearestBiomes(FunctionContext context, int originX, int originZ, Set<Holder<Biome>> firstBiomes, Set<Holder<Biome>> secondBiomes, int searchRadiusXZ, int searchStep) {
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
                firstBiomeDistance = TbUtils.calculateBlockDistance(config.distanceFunction(), originX, blockX, originZ, blockZ);
            else if (!oceansOnly && secondBiomeDistance == -1.0 && secondBiomes.contains(holder))
                secondBiomeDistance = TbUtils.calculateBlockDistance(config.distanceFunction(), originX, blockX, originZ, blockZ);

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
        //if (owner.level.getServer().getTickCount() == 200) // ten seconds
        //    TemperatureBands.logDebug("10 seconds have passed");
        if (CONFIG_GLOBAL.climateSamplerCacheSize() > 0 && samplerCacheCooldownElapsed) {
            targetPointEx = sampleClimateCached(context, blockX, blockZ);
        } else {
            targetPointEx = sampleClimate(TbUtils.packBlockXZtoLong(blockX, blockZ));
            if (!samplerCacheCooldownElapsed) {
                if (owner.level.getServer().getTickCount() > CONFIG_GLOBAL.climateSamplerCacheDelay())
                    samplerCacheCooldownElapsed = true;
            }
        }
        return biomeSourceInvoker.getParameters().findValue(targetPointEx.targetPoint());
    }

    private ClimateTargetPointEx sampleClimateFuture(Long packedBlockPos, CompletableFuture<ClimateTargetPointEx> future) {
        if (future.isCancelled()) {
            throw new CancellationException("Load was cancelled by downstream");
        }
        return sampleClimate(packedBlockPos);
    }

    public ClimateTargetPointEx sampleClimateCached(FunctionContext context, int blockX, int blockZ) {
        ClimateTargetPointEx result = climateSamplerCache.get(TbUtils.packBlockXZtoLong(blockX, blockZ)).join();
        if (updateActiveSamplers) {
            sampleCount++;
            if (sampleCount == sampleCountMax) {
                sampleCount = 0;
                activeSamplers = climateSamplerCacheActiveFutures.size();
            }
        }
        if (CONFIG_GLOBAL.climateSamplerCachePrefetchRadius() > 0 && CONFIG_GLOBAL.climateSamplerCacheSize() > 0) {
            // another spiral. Cbf making the methods common.
            double angle = 0;
            double radius = 0;
            List<Long> points = new ArrayList<>();
            while (radius <= CONFIG_GLOBAL.climateSamplerCachePrefetchRadius()) {
                double xOffset = radius * Math.cos(angle);
                double zOffset = radius * Math.sin(angle);
                int thisX = (int) Math.round(blockX + xOffset);
                int thisZ = (int) Math.round(blockZ + zOffset);
                points.add(TbUtils.packBlockXZtoLong(thisX, thisZ));
                angle += 0.5;
                radius += climateSamplerResolutionActual / 6.28; // 6.28 = 2π
            }
            climateSamplerCache.getAll(points);
        }
        return result;
    }

    private ClimateTargetPointEx sampleClimate(long packedBlockPos) {
        int blockX = TbUtils.unpackBlockFromLongX(packedBlockPos);
        int blockZ = TbUtils.unpackBlockFromLongZ(packedBlockPos);
        DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
        //float temperatureResult = searchOpts.equals(ClimateSearchResults.OCEANS_ONLY) ? 0.0f : (float) dimData.getTemperatureFunction().compute(singlePointContext);
        float temperatureResult = oceansOnly ? 0.0f : (float) owner.getClimateSampler().temperature().compute(singlePointContext);
        float humidityResult = oceansOnly ? 0.0f : (float) computeOriginal(singlePointContext); // use original non-overridden noise for humidity
        float continentalnessResult = (float) owner.getClimateSampler().continentalness().compute(singlePointContext);
        float erosionResult = oceansOnly ? 0.0f : (float) owner.getClimateSampler().erosion().compute(singlePointContext);
        float depthResult = oceansOnly ? 0.0f : (float) owner.getClimateSampler().depth().compute(singlePointContext);
        float weirdnessResult = oceansOnly ? 0.0f : (float) owner.getClimateSampler().weirdness().compute(singlePointContext);

        Climate.TargetPoint sampleResultRaw = Climate.target(temperatureResult, humidityResult, continentalnessResult, erosionResult, depthResult, weirdnessResult);
        return new ClimateTargetPointEx(sampleResultRaw, false);
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
        for (CompletableFuture<ClimateTargetPointEx> future : climateSamplerCacheActiveFutures.values()) {
            if (future != null)
                future.cancel(true);
        }
        /*
        for (CompletableFuture<ClimateTargetPointEx> cacheTask : climateSamplerCache.asMap().values()) {
            try {
                cacheTask.cancel(true);
            } catch (Exception ignored) {}
        }

         */
        climateSamplerCache.synchronous().invalidateAll();
    }
}
