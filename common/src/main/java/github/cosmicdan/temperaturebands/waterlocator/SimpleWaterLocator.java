package github.cosmicdan.temperaturebands.waterlocator;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import github.cosmicdan.temperaturebands.ClimateTargetPointEx;
import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.generator.BiomeProximityGenerator;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import static github.cosmicdan.temperaturebands.TemperatureBands.CONFIG_GLOBAL;

/**
 * Simple water locator performs a 2D Archimedean spiral search.
 * Original algorithm - not very accurate nor fast. Kept for backwards compatibility only.
 */
public class SimpleWaterLocator implements IWaterLocator {
    private final BiomeProximityGenerator.Config config;
    private final int samplerResolution;
    private final Cache<Long, ClimateTargetPointEx> climateSamplerCache;
    private final boolean firstBiomeOnly;
    private final int noisePartSize;
    private final int noisePartSizeMiddleOffset;

    public SimpleWaterLocator(BiomeProximityGenerator.Config configIn) {
        config = configIn;
        if (config.climateSamplerResolutionRaw() == -1)
            this.samplerResolution = config.noiseResolution() * config.noiseResolution() * config.noiseResolution();
        else if (config.climateSamplerResolutionRaw() == -2)
            this.samplerResolution = config.noiseResolution() * config.noiseResolution();
        else if (config.climateSamplerResolutionRaw() == 0)
            this.samplerResolution = TbUtils.doCrash("climateSamplerResolution cannot be zero, please fix your config!");
        else
            this.samplerResolution =  config.climateSamplerResolutionRaw();
        if (CONFIG_GLOBAL.climateSamplerCacheSize() > 0) {
            climateSamplerCache = Caffeine.newBuilder()
                    .maximumWeight((long) CONFIG_GLOBAL.climateSamplerCacheSize() * 1024 * 1024)
                    .weigher((Long key, ClimateTargetPointEx value) -> value.sizeWithLongKey())
                    .build();
        } else
            climateSamplerCache = null;
        firstBiomeOnly = (config.secondBiomeInfluence() == 0.0);
        noisePartSize = config.noiseResolution() * config.noiseResolution();
        noisePartSizeMiddleOffset = (int)Math.round(noisePartSize * 0.5);
    }

    // Simple 2D Archimedean spiral check
    @Override
    public Pair<Double, Double> findNearestOceanRiver(int startX, int startZ, DimensionData dimData) {
        int originPosX = scalePosForResolution(startX, config.noiseResolution(), noisePartSize, noisePartSizeMiddleOffset);
        int originPosZ = scalePosForResolution(startZ, config.noiseResolution(), noisePartSize, noisePartSizeMiddleOffset);

        double angle = 0;
        double radius = 0;
        double firstBiomeDistance = -1.0;
        double secondBiomeDistance = -1.0;

        while (radius < config.biomeSearchDistance()) {
            double xOffset = radius * Math.cos(angle);
            double zOffset = radius * Math.sin(angle);
            int thisX = (int) Math.round(originPosX + xOffset);
            int thisZ = (int) Math.round(originPosZ + zOffset);
            Holder<Biome> holder = getNoiseBiome(thisX, thisZ, dimData);
            if (holder != null && firstBiomeDistance == -1.0 && config.owner().getBiomesFirst().contains(holder))
                firstBiomeDistance = TbUtils.calculateBlockDistance(config.distanceFunction(), originPosX, thisX, originPosZ, thisZ);
            else if (holder != null && !firstBiomeOnly && secondBiomeDistance == -1.0 && config.owner().getBiomesSecond().contains(holder))
                secondBiomeDistance = TbUtils.calculateBlockDistance(config.distanceFunction(), originPosX, thisX, originPosZ, thisZ);

            if (firstBiomeDistance > -1.0 && (secondBiomeDistance > -1.0 || firstBiomeOnly))
                return Pair.of(firstBiomeDistance, secondBiomeDistance);

            angle += 0.5;
            radius += (samplerResolution) / 6.28; // 6.28 = 2π
        }
        return Pair.of(firstBiomeDistance, secondBiomeDistance);
    }

    private int scalePosForResolution(int pos, int resolution, int partSize, int partSizeMid) {
        int posPreScaled = (pos >> resolution);
        return (posPreScaled * partSize) + partSizeMid;
    }

    private @Nullable Holder<Biome> getNoiseBiome(int blockX, int blockZ, DimensionData dimData) {
        final ClimateTargetPointEx targetPointEx;
        if (CONFIG_GLOBAL.climateSamplerCacheSize() > 0)
            targetPointEx = sampleClimateCached(blockX, blockZ, dimData);
        else
            targetPointEx = sampleClimate(TbUtils.packBlockXZtoLong(blockX, blockZ), dimData);
        return targetPointEx == null ? null : dimData.getBiomeSource().parameters().findValue(targetPointEx.targetPoint());
    }

    public @Nullable ClimateTargetPointEx sampleClimateCached(int blockX, int blockZ, DimensionData dimData) {
        long packedPos = TbUtils.packBlockXZtoLong(blockX, blockZ);
        if (CONFIG_GLOBAL.doBenchmark() && CONFIG_GLOBAL.benchmarkCacheStats() && TbUtils.benchmarkActive)
            TbUtils.benchmarkSampleTotalCount.getAndIncrement();
        ClimateTargetPointEx result = climateSamplerCache.getIfPresent(packedPos);
        if (result == null) {
            result = sampleClimate(packedPos, dimData);
            if (result != null)
                climateSamplerCache.put(packedPos, result);
        } else if (CONFIG_GLOBAL.doBenchmark() && CONFIG_GLOBAL.benchmarkCacheStats() && TbUtils.benchmarkActive)
            TbUtils.benchmarkSampleCacheHitCount.getAndIncrement();
        return result;
    }

    public final @Nullable ClimateTargetPointEx sampleClimate(long packedBlockPos, DimensionData dimData) {
        // Note: if firstBiomeOnly, we're just looking for oceans so only sampling continentalness is required
        int blockX = TbUtils.unpackBlockFromLongX(packedBlockPos);
        int blockZ = TbUtils.unpackBlockFromLongZ(packedBlockPos);
        DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(blockX, BiomeProximityGenerator.blockY, blockZ);
        float continentalnessResult = (float) dimData.getClimateSampler().continentalness().compute(singlePointContext);
        float weirdnessResult = firstBiomeOnly ? 0.0f : (float) dimData.getClimateSampler().weirdness().compute(singlePointContext);
        boolean doShortcuts = dimData.config.climateSamplerShortcuts();
        if (!doShortcuts || dimData.mightBeOcean(continentalnessResult) || dimData.mightBeRiver(weirdnessResult)) {
            float temperatureResult = firstBiomeOnly || doShortcuts ? 0.0f : (float) dimData.getTemperatureNoise().compute(singlePointContext);
            float humidityResult = firstBiomeOnly || doShortcuts ? 0.0f : (float) config.owner().computeOriginal(singlePointContext); // always use original non-overridden densityfunctions for humidity
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
