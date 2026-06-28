package github.cosmicdan.temperaturebands;

import java.util.HashSet;
import java.util.Set;

public record GlobalConfig(
        // Global settings (not per-world)
        boolean copyConfigOnRecreateWorld,
        Set<String> ignoreDimensionFailures,
        boolean dumpRiverAndOceanBiomes,
        // Global climate sampler settings (not per-world)
        int climateSamplerCacheSize,
        int climateSamplerCachePrefetchRadius,
        int climateSamplerMax,
        int climateSamplerCacheDelay,
        boolean climateSamplerWarmupMsg
) {
    public static GlobalConfig create(CommonConfig loadedConfig) {
        // prepare ignoreDimensionFailures set
        HashSet<String> ignoreDimensionFailures = new HashSet<>();
        TbUtils.convertCsvToSet(loadedConfig.ignoreDimensionFailures.get(), ignoreDimensionFailures);
        // prepare automatic configClimateSamplerCachePrefetchRadius
        int climateSamplerCachePrefetchRadius = loadedConfig.climateSamplerCachePrefetchRadius.get();
        if (climateSamplerCachePrefetchRadius < 0) {
            int threadCount = Runtime.getRuntime().availableProcessors();
            int originalSetting = climateSamplerCachePrefetchRadius;
            climateSamplerCachePrefetchRadius = threadCount / -climateSamplerCachePrefetchRadius;
            if (climateSamplerCachePrefetchRadius < 1) {
                TemperatureBands.LOGGER.info("Disabling climate sampler prefetching due to not enough threads (i.e. from {}/{}, based on {} CPU threads available)", -originalSetting, threadCount, threadCount);
                climateSamplerCachePrefetchRadius = 0;
            } else
                TemperatureBands.LOGGER.info("Using radius of {} for climate sampler prefetching (from {}/{}, based on {} CPU threads available)", climateSamplerCachePrefetchRadius, -originalSetting, threadCount, threadCount);
        }
        // all done
        return new GlobalConfig(
                loadedConfig.copyConfigOnRecreateWorld.get(),
                ignoreDimensionFailures,
                loadedConfig.dumpRiverAndOceanBiomes.get(),
                loadedConfig.climateSamplerCacheSize.get(),
                climateSamplerCachePrefetchRadius,
                loadedConfig.climateSamplerMax.get(),
                loadedConfig.climateSamplerCacheDelay.get(),
                loadedConfig.climateSamplerWarmupMsg.get()
        );
    }
}
