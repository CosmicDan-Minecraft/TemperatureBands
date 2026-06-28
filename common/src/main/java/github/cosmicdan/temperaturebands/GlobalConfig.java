package github.cosmicdan.temperaturebands;

import java.util.HashSet;
import java.util.Set;

public record GlobalConfig(
        // Global settings (not per-world)
        boolean copyConfigOnRecreateWorld,
        Set<String> ignoreDimensionFailures
) {
    public static GlobalConfig create(CommonConfig loadedConfig) {
        // prepare ignoreDimensionFailures set
        HashSet<String> ignoreDimensionFailures = new HashSet<>();
        TbUtils.convertCsvToSet(loadedConfig.ignoreDimensionFailures.get(), ignoreDimensionFailures);
        // prepare automatic configClimateSamplerCachePrefetchRadius
        // all done
        return new GlobalConfig(
                loadedConfig.copyConfigOnRecreateWorld.get(),
                ignoreDimensionFailures
        );
    }
}
