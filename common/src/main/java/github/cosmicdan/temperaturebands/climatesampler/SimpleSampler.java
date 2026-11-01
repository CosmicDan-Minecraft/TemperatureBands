package github.cosmicdan.temperaturebands.climatesampler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import github.cosmicdan.temperaturebands.*;
import github.cosmicdan.temperaturebands.generator.BiomeProximityGenerator;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.Nullable;

import static github.cosmicdan.temperaturebands.TemperatureBands.CONFIG_GLOBAL;

/**
 * Simple water locator performs a 2D Archimedean spiral search.
 * Original algorithm - not very accurate nor fast. Kept for backwards compatibility only.
 */
public class SimpleSampler implements IClimateSampler {
    private final BiomeProximityGenerator.Config config;
    private final Cache<Long, ClimateTargetPointEx> climateSamplerCache;

    private final int noiseResolution;
    private final int samplerResolution;
    private final boolean oceansOnly;
    private final int noisePartSize;
    private final int noisePartSizeMiddleOffset;

    public SimpleSampler(BiomeProximityGenerator.Config genConfig, DimensionConfig dimConfig) {
        config = genConfig;
        noiseResolution = dimConfig.humidityResolution();
        if (dimConfig.climateSamplerResolution() == -1)
            this.samplerResolution = noiseResolution * noiseResolution * noiseResolution;
        else if (dimConfig.climateSamplerResolution() == -2)
            this.samplerResolution = noiseResolution * noiseResolution;
        else if (dimConfig.climateSamplerResolution() == 0)
            this.samplerResolution = TbUtils.doCrash("climateSamplerResolution cannot be zero, please fix your config!");
        else
            this.samplerResolution =  dimConfig.climateSamplerResolution();
        if (CONFIG_GLOBAL.climateSamplerCacheSize() > 0) {
            climateSamplerCache = Caffeine.newBuilder()
                    .maximumWeight((long) CONFIG_GLOBAL.climateSamplerCacheSize() * 1024 * 1024)
                    .weigher((Long key, ClimateTargetPointEx value) -> value.sizeWithLongKey())
                    .build();
        } else
            climateSamplerCache = null;
        oceansOnly = (config.algo2SecondBiomeInfluence() == 0.0);
        noisePartSize = noiseResolution * noiseResolution;
        noisePartSizeMiddleOffset = (int)Math.round(noisePartSize * 0.5);
    }

    @Override
    public void onFirstCompute(DimensionData dimData, BiomeProximityGenerator.Config config) {
        // no-op
    }

    @Override
    public double calculateHumidity(DensityFunction.FunctionContext context, DimensionData dimData) {
        TbUtils.doCrash("Tried to call calculateHumidity on SimpleSampler, this is only for later samplers! Fix me!");
        return 0.0;
    }

    // Simple 2D Archimedean spiral check
    @Override
    public double[] findNearestOceanRiver(int startX, int startZ, DimensionData dimData) {

        int originPosX = scalePosForResolution(startX, noiseResolution, noisePartSize, noisePartSizeMiddleOffset);
        int originPosZ = scalePosForResolution(startZ, noiseResolution, noisePartSize, noisePartSizeMiddleOffset);

        double angle = 0;
        double radius = 0;
        double firstBiomeDistance = -1.0;
        double secondBiomeDistance = -1.0;

        while (radius < config.algo2BiomeSearchDistance()) {
            double xOffset = radius * Math.cos(angle);
            double zOffset = radius * Math.sin(angle);
            int thisX = (int) Math.round(originPosX + xOffset);
            int thisZ = (int) Math.round(originPosZ + zOffset);
            Holder<Biome> holder = getNoiseBiome(thisX, thisZ, dimData);
            //if (holder != null && firstBiomeDistance == -1.0 && dimData.getLegacyHelper().biomeOceans.contains(holder))
            if (holder != null && firstBiomeDistance == -1.0 && dimData.getLegacyHelper().isOcean(holder))
                firstBiomeDistance = TbUtils.calculateBlockDistance(config.distanceFunction(), originPosX, thisX, originPosZ, thisZ);
            //else if (holder != null && !oceansOnly && secondBiomeDistance == -1.0 && dimData.getLegacyHelper().biomeRivers.contains(holder))
            else if (holder != null && !oceansOnly && secondBiomeDistance == -1.0 && dimData.getLegacyHelper().isRiver(holder))
                secondBiomeDistance = TbUtils.calculateBlockDistance(config.distanceFunction(), originPosX, thisX, originPosZ, thisZ);

            if (firstBiomeDistance > -1.0 && (secondBiomeDistance > -1.0 || oceansOnly))
                return new double[]{firstBiomeDistance, secondBiomeDistance};

            angle += 0.5;
            radius += (samplerResolution) / 6.28; // 6.28 = 2π
        }
        return new double[]{firstBiomeDistance, secondBiomeDistance};
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
        ClimateTargetPointEx result = climateSamplerCache.getIfPresent(packedPos);
        if (result == null) {
            result = sampleClimate(packedPos, dimData);
            if (result != null)
                climateSamplerCache.put(packedPos, result);
        }
        return result;
    }

    public final @Nullable ClimateTargetPointEx sampleClimate(long packedBlockPos, DimensionData dimData) {
        // Note: if firstBiomeOnly, we're just looking for oceans so only sampling continentalness is required
        int blockX = TbUtils.unpackBlockFromLongX(packedBlockPos);
        int blockZ = TbUtils.unpackBlockFromLongZ(packedBlockPos);
        DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(blockX, PlatformConstants.BLOCK_Y_FIXED, blockZ);
        float continentalnessResult = (float) dimData.getClimateSampler().continentalness().compute(singlePointContext);
        float weirdnessResult = oceansOnly ? 0.0f : (float) dimData.getClimateSampler().weirdness().compute(singlePointContext);
        boolean doShortcuts = dimData.config.climateSamplerShortcuts();
        if (!doShortcuts || dimData.getLegacyHelper().mightBeOcean(continentalnessResult) || dimData.getLegacyHelper().mightBeRiver(weirdnessResult)) {
            float temperatureResult = oceansOnly || doShortcuts ? 0.0f : (float) dimData.getTemperatureNoise().compute(singlePointContext);
            float humidityResult = oceansOnly || doShortcuts ? 0.0f : (float) config.owner().computeOriginal(singlePointContext); // always use original non-overridden densityfunctions for humidity
            float erosionResult = oceansOnly ? 0.0f : (float) dimData.getClimateSampler().erosion().compute(singlePointContext);
            float depthResult = oceansOnly ? 0.0f : (float) dimData.getClimateSampler().depth().compute(singlePointContext);
            Climate.TargetPoint sampleResultRaw = Climate.target(temperatureResult, humidityResult, continentalnessResult, erosionResult, depthResult, weirdnessResult);
            return new ClimateTargetPointEx(sampleResultRaw, false);
        } else {
            // point is definitely not an ocean or river
            return null;
        }
    }
}
