package github.cosmicdan.temperaturebands.util;

import github.cosmicdan.temperaturebands.DensityFunctionEx;
import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.TemperatureBands;
import github.cosmicdan.temperaturebands.generator.BiomeProximityGenerator;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

public class CacheRecylerHooksUtil {
    public static class ThreadState {
        public boolean initialized = false;
        public DimensionData dimData = null;
        public boolean doCacheRecycle = false;
    }

    public static void initIfNeeded(CacheRecylerHooksUtil.ThreadState stateData, Climate.Sampler sampler, BiomeSource biomeSource) {
        if (stateData.initialized)
            return;
        // find dimData via biomeSource
        for (DimensionData dimDataValue : TemperatureBands.DIMENSION_DATA_CACHE.asMap().values()) {
            if (dimDataValue.getBiomeSource() == biomeSource) {
                stateData.dimData = dimDataValue;
                if (stateData.dimData.config.humidityAlgo3cacheRecycleWorld()) {
                    if (stateData.dimData.isCacheRecyclerPossible() && sampler.humidity() instanceof DensityFunctionEx funcEx)
                        if (funcEx.gen instanceof BiomeProximityGenerator)
                            stateData.doCacheRecycle = true;
                }
            }
        }
        stateData.initialized = true;
    }

    public static Climate.TargetPoint onGetNoiseBiomeWithSampler(CacheRecylerHooksUtil.ThreadState stateData, int i, int j, int k, Climate.Sampler sampler) {
        if (stateData.initialized)
            if (stateData.doCacheRecycle && stateData.dimData.isCacheReady())
                return stateData.dimData.sampleFinal(i, j, k, sampler);
        return null;
    }
}
