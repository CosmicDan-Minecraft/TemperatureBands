package github.cosmicdan.temperaturebands.generator;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import github.cosmicdan.temperaturebands.ClimateTargetPointEx;
import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.mixin.MultiNoiseBiomeSourceInvoker;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static github.cosmicdan.temperaturebands.TemperatureBands.CONFIG_GLOBAL;

public class BiomeProximityGenerator implements IGenerator {
    public static final int blockY = 64;

    private final Config config;
    // initialized in constructor and/or derived from config arg
    private final int climateSamplerResolution;
    private final ConcurrentMap<Long, CompletableFuture<ClimateTargetPointEx>> climateSamplerCacheActiveFutures;
    private final AsyncLoadingCache<Long, ClimateTargetPointEx> climateSamplerCache;
    private final boolean firstBiomeOnly;
    private final int noisePartSize;
    private final int noisePartSizeMiddleOffset;

    private DimensionData dimData;
    private boolean biomeSourceError = false;
    private MultiNoiseBiomeSourceInvoker biomeSourceInvoker;
    private boolean samplerCacheCooldownElapsed = false;

    public record Config(
            BiomeProximityGeneratorOwner owner,
            String dimensionName,
            boolean isTempDimension,
            int noiseResolution,
            int climateSamplerResolution,
            float secondBiomeInfluence,
            int biomeSearchDistance,
            int distanceFunction
    ) {}

    public BiomeProximityGenerator(Config config) {
        this.config = config;
        this.climateSamplerResolution = (config.climateSamplerResolution < 0) ? config.noiseResolution * config.noiseResolution * config.noiseResolution : config.climateSamplerResolution;

        if (CONFIG_GLOBAL.climateSamplerCacheSize() > 0) {
            climateSamplerCacheActiveFutures = new ConcurrentHashMap<>();
            AsyncCacheLoader<Long, ClimateTargetPointEx> climateSamplerCacheLoader = new AsyncCacheLoader<>() {
                @Override
                public @NotNull CompletableFuture<? extends ClimateTargetPointEx> asyncLoad(@NotNull Long packedBlockPos, @NotNull Executor executor) {
                    CompletableFuture<ClimateTargetPointEx> future = new CompletableFuture<>() {
                        @Override
                        public boolean cancel(boolean mayInterruptIfRunning) {
                            boolean result = super.cancel(mayInterruptIfRunning);
                            if (result) {
                                climateSamplerCacheActiveFutures.remove(packedBlockPos);
                            }
                            return result;
                        }
                    };
                    climateSamplerCacheActiveFutures.put(packedBlockPos, future);
                    CompletableFuture.runAsync(() -> {
                        try {
                            future.complete(sampleClimateFuture(packedBlockPos, future));
                        } catch (CancellationException e) {
                            future.completeExceptionally(e);
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
        } else {
            climateSamplerCacheActiveFutures = null;
            climateSamplerCache = null;
        }
        firstBiomeOnly = (config.secondBiomeInfluence == 0.0);
        noisePartSize = config.noiseResolution * config.noiseResolution;
        noisePartSizeMiddleOffset = (int)Math.round(noisePartSize * 0.5);
        if (config.isTempDimension)
            samplerCacheCooldownElapsed = true; // ignore prefetch cooldown for temporary worlds (e.g. world preview)
        else if (CONFIG_GLOBAL.climateSamplerCacheDelay() == 0)
            samplerCacheCooldownElapsed = true; // also ignore when delay is set to zero
    }

    private void onFirstCompute(DimensionData dimData) {
        this.dimData = dimData;
        if (dimData == null)
            TbUtils.doCrash("dimData must not be null");
        final BiomeSource biomeSource = dimData.getBiomeSource();
        if (biomeSource instanceof MultiNoiseBiomeSource biomeSourceNoise) {
            biomeSourceInvoker = (MultiNoiseBiomeSourceInvoker) biomeSourceNoise;
        } else {
            // we already logged error about biomeSource not being MultiNoiseBiomeSource, just set a flag and move on
            biomeSourceError = true;
        }
    }

    @Override
    public double compute(DensityFunction.FunctionContext context, DimensionData dimData) {
        if (biomeSourceError)
            return computeOriginal(context);
        if (this.dimData == null)
            onFirstCompute(dimData);
        int originPosX = scalePosForNoiseResolution(context.blockX());
        int originPosZ = scalePosForNoiseResolution(context.blockZ());

        // default value if biome wasn't found
        double noiseValue = -1.0;
        Pair<Double, Double> nearestFirstAndMaybeSecondBiomeDistance = getDistanceToNearestBiomes(
                originPosX,
                originPosZ,
                config.owner.getBiomesFirst(),
                config.owner.getBiomesSecond(),
                config.biomeSearchDistance,
                climateSamplerResolution
        );
        double nearestFirstBiome = nearestFirstAndMaybeSecondBiomeDistance.getLeft();
        double nearestSecondBiomeMaybe = nearestFirstAndMaybeSecondBiomeDistance.getRight();
        // Humidity has 5 levels, ranging from -1.0 (least humid) to +1.0 (most humid)
        // Level 0 = -1.00 to -0.35
        // Level 1 = -0.35 to -0.10
        // Level 2 = -0.10 to +0.10
        // Level 3 = +0.10 to +0.30
        // Level 4 = +0.30 to +1.00

        if (nearestFirstBiome >= 0.0) {
            // Determine noise (e.g. humidity) as a percentage (0.0 to 1.0)
            noiseValue = 1.0 - (nearestFirstBiome / config.biomeSearchDistance);
            // Then apply some "middle weightiness"
            //noiseValue = calcMiddleWeightedValue(noiseValue);
            // Finally, clamp it to the range MC wants (vanilla expects -1.0 to +1.0)
            noiseValue = (noiseValue * 2.0) - 1.0;
        }

        if (nearestSecondBiomeMaybe >= 0.0) {
            // add second biome (e.g. river) influence
            double noiseValueSecondBiome = 1.0 - (nearestSecondBiomeMaybe / config.biomeSearchDistance);
            noiseValue = noiseValue + (noiseValueSecondBiome * config.secondBiomeInfluence);
        }

        // use humidityNoiseFactor for this point to soften the edges a bit
        //double noiseOriginal = computeNoise(context) * humidityNoiseFactor;

        return noiseValue;
    }

    @Override
    public double computeOriginal(DensityFunction.FunctionContext context) {
        double d = context.blockX() * config.owner.getXZScale() + config.owner.getShiftX().compute(context);
        double e = context.blockY() * config.owner.getYScale() + config.owner.getShiftY().compute(context);
        double f = context.blockZ() * config.owner.getXZScale() + config.owner.getShiftZ().compute(context);
        return config.owner.getNoise().getValue(d, e, f);
    }

    private int scalePosForNoiseResolution(int pos) {
        // TODO: replicate for sampler spiral
        int posPreScaled = (pos >> config.noiseResolution);
        return (posPreScaled * noisePartSize) + noisePartSizeMiddleOffset;
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

    // Simple 2D Archimedean spiral check
    private Pair<Double, Double> getDistanceToNearestBiomes(int originX, int originZ, Set<Holder<Biome>> firstBiomes, Set<Holder<Biome>> secondBiomes, int searchRadiusXZ, int searchStep) {
        double angle = 0;
        double radius = 0;
        double firstBiomeDistance = -1.0;
        double secondBiomeDistance = -1.0;

        while (radius < searchRadiusXZ) {
            double xOffset = radius * Math.cos(angle);
            double zOffset = radius * Math.sin(angle);
            int blockX = (int) Math.round(originX + xOffset);
            int blockZ = (int) Math.round(originZ + zOffset);
            Holder<Biome> holder = getNoiseBiome(blockX, blockZ);
            if (firstBiomeDistance == -1.0 && firstBiomes.contains(holder))
                firstBiomeDistance = TbUtils.calculateBlockDistance(config.distanceFunction, originX, blockX, originZ, blockZ);
            else if (!firstBiomeOnly && secondBiomeDistance == -1.0 && secondBiomes.contains(holder))
                secondBiomeDistance = TbUtils.calculateBlockDistance(config.distanceFunction, originX, blockX, originZ, blockZ);

            if (firstBiomeDistance > -1.0 && (secondBiomeDistance > -1.0 || firstBiomeOnly))
                return Pair.of(firstBiomeDistance, secondBiomeDistance);

            angle += 0.5;
            radius += (searchStep) / 6.28; // 6.28 = 2π
        }
        return Pair.of(firstBiomeDistance, secondBiomeDistance);
    }

    private Holder<Biome> getNoiseBiome(int blockX, int blockZ) {
        final ClimateTargetPointEx targetPointEx;
        if (CONFIG_GLOBAL.climateSamplerCacheSize() > 0 && samplerCacheCooldownElapsed) {
            targetPointEx = sampleClimateAndMaybePrefetch(blockX, blockZ);
        } else {
            targetPointEx = config.owner.sampleClimate(TbUtils.packBlockXZtoLong(blockX, blockZ), firstBiomeOnly);
            if (!samplerCacheCooldownElapsed) {
                if (dimData.level.getServer().getTickCount() > CONFIG_GLOBAL.climateSamplerCacheDelay())
                    samplerCacheCooldownElapsed = true;
            }
        }
        return biomeSourceInvoker.getParameters().findValue(targetPointEx.targetPoint());
    }

    public ClimateTargetPointEx sampleClimateAndMaybePrefetch(int blockX, int blockZ) {
        long packedPos = TbUtils.packBlockXZtoLong(blockX, blockZ);
        ClimateTargetPointEx result = climateSamplerCache.synchronous().getIfPresent(packedPos);
        if (result == null) {
            result = config.owner.sampleClimate(packedPos, firstBiomeOnly);
            climateSamplerCache.synchronous().put(packedPos, result);
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
                radius += climateSamplerResolution / 6.28; // 6.28 = 2π
            }
            climateSamplerCache.getAll(points);
        }
        return result;
    }

    private ClimateTargetPointEx sampleClimateFuture(Long packedBlockPos, CompletableFuture<ClimateTargetPointEx> future) {
        if (future.isCancelled()) {
            return null;
        }
        return config.owner.sampleClimate(packedBlockPos, firstBiomeOnly);
    }

    @Override
    public void cancelAllCacheTasks() {
        for (CompletableFuture<ClimateTargetPointEx> future : climateSamplerCacheActiveFutures.values()) {
            if (future != null)
                future.cancel(true);
        }
        climateSamplerCache.synchronous().invalidateAll();
    }
}
