package github.cosmicdan.temperaturebands.generator;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import github.cosmicdan.temperaturebands.ClimateTargetPointEx;
import github.cosmicdan.temperaturebands.DimensionConfig;
import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.DensityFunctionEx;
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
import java.util.concurrent.atomic.AtomicInteger;

import static github.cosmicdan.temperaturebands.TemperatureBands.CONFIG_GLOBAL;

public class BiomeProximityGenerator implements IGenerator {
    public static final int blockY = 64;

    private final Config config;
    // initialized in constructor and/or derived from config arg
    private final int samplerResolution;
    private final ConcurrentMap<Long, CompletableFuture<ClimateTargetPointEx>> climateSamplerCacheActiveFutures;
    private final AsyncLoadingCache<Long, ClimateTargetPointEx> climateSamplerCache;
    private final boolean firstBiomeOnly;
    private final int noisePartSize;
    private final int noisePartSizeMiddleOffset;
    private final int samplerPartSize;
    private final int samplerMiddleOffset;

    private DimensionData dimData;
    private boolean biomeSourceError = false;
    private boolean samplerCacheCooldownElapsed = false;

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
        this.samplerResolution = (config.climateSamplerResolutionRaw < 0) ? config.noiseResolution * config.noiseResolution * config.noiseResolution : config.climateSamplerResolutionRaw;
        this.samplerPartSize = samplerResolution * samplerResolution;
        this.samplerMiddleOffset = (int)Math.round(samplerPartSize * 0.5);

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
    }

    @Override
    public double onCompute(DensityFunction.FunctionContext context, DimensionData dimData) {
        if (this.dimData == null)
            onFirstCompute(dimData);
        if (biomeSourceError)
            return config.owner.computeOriginal(context);
        int originPosX = scalePosForResolution(context.blockX(), config.noiseResolution, noisePartSize, noisePartSizeMiddleOffset);
        int originPosZ = scalePosForResolution(context.blockZ(), config.noiseResolution, noisePartSize, noisePartSizeMiddleOffset);

        // default value if biome wasn't found
        double noiseValue = -1.0;
        Pair<Double, Double> nearestFirstAndMaybeSecondBiomeDistance = getDistanceToNearestBiomes(
                originPosX,
                originPosZ,
                config.owner.getBiomesFirst(),
                config.owner.getBiomesSecond(),
                config.biomeSearchDistance,
                samplerResolution
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

    private int scalePosForResolution(int pos, int resolution, int partSize, int partSizeMid) {
        int posPreScaled = (pos >> resolution);
        return (posPreScaled * partSize) + partSizeMid;
    }

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
            targetPointEx = sampleClimateCachedAndMaybePrefetch(blockX, blockZ);
        } else {
            targetPointEx = config.owner.sampleClimate(TbUtils.packBlockXZtoLong(blockX, blockZ), firstBiomeOnly);
            if (!samplerCacheCooldownElapsed) {
                if (dimData.level.getServer().getTickCount() > CONFIG_GLOBAL.climateSamplerCacheDelay())
                    samplerCacheCooldownElapsed = true;
            }
        }
        return dimData.getBiomeSource().parameters().findValue(targetPointEx.targetPoint());
    }

    public ClimateTargetPointEx sampleClimateCachedAndMaybePrefetch(int blockX, int blockZ) {
        long packedPos = TbUtils.packBlockXZtoLong(blockX, blockZ);
        ClimateTargetPointEx result = climateSamplerCache.synchronous().getIfPresent(packedPos);
        if (result == null) {
            result = config.owner.sampleClimate(packedPos, firstBiomeOnly);
            climateSamplerCache.synchronous().put(packedPos, result);
        }
        if (CONFIG_GLOBAL.climateSamplerCachePrefetchRadius() > 0 && CONFIG_GLOBAL.climateSamplerCacheSize() > 0 && climateSamplerCacheActiveFutures.size() < CONFIG_GLOBAL.climateSamplerMax()) {
            // another spiral. Cbf making the methods common.
            double angle = 0;
            double radius = 0;
            List<Long> points = new ArrayList<>();
            while (radius <= CONFIG_GLOBAL.climateSamplerCachePrefetchRadius()) {
                double xOffset = radius * Math.cos(angle);
                double zOffset = radius * Math.sin(angle);
                int thisX = (int) Math.round(blockX + xOffset);
                int thisZ = (int) Math.round(blockZ + zOffset);
                // rescale to ensure cache hits
                thisX = scalePosForResolution(thisX, samplerResolution, samplerPartSize, samplerMiddleOffset);
                thisZ = scalePosForResolution(thisZ, samplerResolution, samplerPartSize, samplerMiddleOffset);
                points.add(TbUtils.packBlockXZtoLong(thisX, thisZ));
                angle += 0.5;
                radius += samplerResolution / 6.28; // 6.28 = 2π
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
