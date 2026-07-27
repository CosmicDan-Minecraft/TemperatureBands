package github.cosmicdan.temperaturebands.generator;

import com.github.benmanes.caffeine.cache.*;
import github.cosmicdan.temperaturebands.ClimateTargetPointEx;
import github.cosmicdan.temperaturebands.DimensionConfig;
import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.DensityFunctionEx;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static github.cosmicdan.temperaturebands.TemperatureBands.CONFIG_GLOBAL;

public class BiomeProximityGenerator implements IGenerator {
    public static final int blockY = 64;

    public static AtomicLong benchmarkSampleCacheHitCount = new AtomicLong(0);
    public static AtomicLong benchmarkSampleTotalCount = new AtomicLong(0);

    private final Config config;
    // initialized in constructor and/or derived from config arg
    private final int samplerResolution;
    private final Cache<Long, ClimateTargetPointEx> climateSamplerCache;
    private final boolean firstBiomeOnly;
    private final int noisePartSize;
    private final int noisePartSizeMiddleOffset;

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
        if (config.climateSamplerResolutionRaw == -1)
            this.samplerResolution = config.noiseResolution * config.noiseResolution * config.noiseResolution;
        else if (config.climateSamplerResolutionRaw == -2)
            this.samplerResolution = config.noiseResolution * config.noiseResolution;
        else if (config.climateSamplerResolutionRaw == 0)
            this.samplerResolution = TbUtils.doCrash("climateSamplerResolution cannot be zero, please fix your config!");
        else
            this.samplerResolution =  config.climateSamplerResolutionRaw;

        if (CONFIG_GLOBAL.climateSamplerCacheSize() > 0) {
            climateSamplerCache = Caffeine.newBuilder()
                    .maximumWeight((long) CONFIG_GLOBAL.climateSamplerCacheSize() * 1024 * 1024)
                    .weigher((Long key, ClimateTargetPointEx value) -> value.sizeWithLongKey())
                    .build();
        } else
            climateSamplerCache = null;
        firstBiomeOnly = (config.secondBiomeInfluence == 0.0);
        noisePartSize = config.noiseResolution * config.noiseResolution;
        noisePartSizeMiddleOffset = (int)Math.round(noisePartSize * 0.5);
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
        int originPosX = scalePosForResolution(context.blockX(), config.noiseResolution, noisePartSize, noisePartSizeMiddleOffset);
        int originPosZ = scalePosForResolution(context.blockZ(), config.noiseResolution, noisePartSize, noisePartSizeMiddleOffset);

        // default value if biome wasn't found
        double noiseValue = -1.0;
        Pair<Double, Double> nearestFirstAndMaybeSecondBiomeDistance = getDistanceToNearestBiomesSimple(
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
    private Pair<Double, Double> getDistanceToNearestBiomesSimple(int originX, int originZ, Set<Holder<Biome>> firstBiomes, Set<Holder<Biome>> secondBiomes, int searchRadiusXZ, int searchStep) {
        double angle = 0;
        double radius = 0;
        double firstBiomeDistance = -1.0;
        double secondBiomeDistance = -1.0;

        while (radius < searchRadiusXZ) {
            double xOffset = radius * Math.cos(angle);
            double zOffset = radius * Math.sin(angle);
            int thisX = (int) Math.round(originX + xOffset);
            int thisZ = (int) Math.round(originZ + zOffset);
            Holder<Biome> holder = getNoiseBiome(thisX, thisZ);
            if (holder != null && firstBiomeDistance == -1.0 && firstBiomes.contains(holder))
                firstBiomeDistance = TbUtils.calculateBlockDistance(config.distanceFunction, originX, thisX, originZ, thisZ);
            else if (holder != null && !firstBiomeOnly && secondBiomeDistance == -1.0 && secondBiomes.contains(holder))
                secondBiomeDistance = TbUtils.calculateBlockDistance(config.distanceFunction, originX, thisX, originZ, thisZ);

            if (firstBiomeDistance > -1.0 && (secondBiomeDistance > -1.0 || firstBiomeOnly))
                return Pair.of(firstBiomeDistance, secondBiomeDistance);

            angle += 0.5;
            radius += (searchStep) / 6.28; // 6.28 = 2π
        }
        return Pair.of(firstBiomeDistance, secondBiomeDistance);
    }

    private @Nullable Holder<Biome> getNoiseBiome(int blockX, int blockZ) {
        final ClimateTargetPointEx targetPointEx;
        if (CONFIG_GLOBAL.climateSamplerCacheSize() > 0)
            targetPointEx = sampleClimateCached(blockX, blockZ);
        else
            targetPointEx = sampleClimate(TbUtils.packBlockXZtoLong(blockX, blockZ));
        return targetPointEx == null ? null : dimData.getBiomeSource().parameters().findValue(targetPointEx.targetPoint());
    }

    public @Nullable ClimateTargetPointEx sampleClimateCached(int blockX, int blockZ) {
        long packedPos = TbUtils.packBlockXZtoLong(blockX, blockZ);
        if (TbUtils.benchmarkActive)
            benchmarkSampleTotalCount.getAndIncrement();
        ClimateTargetPointEx result = climateSamplerCache.getIfPresent(packedPos);
        if (result == null) {
            result = sampleClimate(packedPos);
            if (result != null)
                climateSamplerCache.put(packedPos, result);
        } else if (TbUtils.benchmarkActive)
            benchmarkSampleCacheHitCount.getAndIncrement();
        return result;
    }

    private @Nullable ClimateTargetPointEx sampleClimateFuture(Long packedBlockPos, CompletableFuture<ClimateTargetPointEx> future) {
        if (future.isCancelled()) {
            return null;
        }
        return sampleClimate(packedBlockPos);
    }

    public final @Nullable ClimateTargetPointEx sampleClimate(long packedBlockPos) {
        // Note: if firstBiomeOnly, we're just looking for oceans so only sampling continentalness is required
        int blockX = TbUtils.unpackBlockFromLongX(packedBlockPos);
        int blockZ = TbUtils.unpackBlockFromLongZ(packedBlockPos);
        DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(blockX, BiomeProximityGenerator.blockY, blockZ);
        float continentalnessResult = (float) dimData.getClimateSampler().continentalness().compute(singlePointContext);
        float weirdnessResult = firstBiomeOnly ? 0.0f : (float) dimData.getClimateSampler().weirdness().compute(singlePointContext);
        boolean doShortcuts = dimData.config.climateSamplerShortcuts();
        if (!doShortcuts || dimData.mightBeOcean(continentalnessResult) || dimData.mightBeRiver(weirdnessResult)) {
            float temperatureResult = firstBiomeOnly || doShortcuts ? 0.0f : (float) dimData.getTemperatureNoise().compute(singlePointContext);
            float humidityResult = firstBiomeOnly || doShortcuts ? 0.0f : (float) config.owner.computeOriginal(singlePointContext); // always use original non-overridden densityfunctions for humidity
            float erosionResult = firstBiomeOnly ? 0.0f : (float) dimData.getClimateSampler().erosion().compute(singlePointContext);
            float depthResult = firstBiomeOnly ? 0.0f : (float) dimData.getClimateSampler().depth().compute(singlePointContext);
            Climate.TargetPoint sampleResultRaw = Climate.target(temperatureResult, humidityResult, continentalnessResult, erosionResult, depthResult, weirdnessResult);
            return new ClimateTargetPointEx(sampleResultRaw, false);
        } else {
            // point is definitely not an ocean or river
            return null;
        }
    }
}
