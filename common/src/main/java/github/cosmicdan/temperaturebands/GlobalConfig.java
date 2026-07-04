package github.cosmicdan.temperaturebands;

import java.util.HashSet;
import java.util.Set;

public record GlobalConfig(
        // Global settings (not per-world)
        boolean copyConfigOnRecreateWorld,
        Set<String> ignoreDimensionFailures,
        boolean dumpRiverAndOceanBiomes,
        // Global climate sampler settings (not per-world)
        int climateSamplerCacheSize
) {
    public static GlobalConfig create(CommonConfig loadedConfig) {
        // prepare ignoreDimensionFailures set
        HashSet<String> ignoreDimensionFailures = new HashSet<>();
        TbUtils.convertCsvToSet(loadedConfig.ignoreDimensionFailures.get(), ignoreDimensionFailures);
        // all done
        return new GlobalConfig(
                loadedConfig.copyConfigOnRecreateWorld.get(),
                ignoreDimensionFailures,
                loadedConfig.dumpRiverAndOceanBiomes.get(),
                loadedConfig.climateSamplerCacheSize.get()
        );
    }
}
