package github.cosmicdan.temperaturebands;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A cached climate point for the new refined sampler.
 * Uses volatile fields with an atomic updater for lock-free writes.
 */
public class CachedClimatePoint {
    public static boolean[] IS_DIMENSION_2D = new boolean[PlatformConstants.DIMENSION_COUNT];

    public volatile long temperature;
    public volatile long humidity;
    public volatile long continentalness;
    public volatile long erosion;
    public volatile long depth;
    public volatile long weirdness;

    private volatile int sampledMask = 0;

    private static final AtomicIntegerFieldUpdater<CachedClimatePoint> MASK_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(CachedClimatePoint.class, "sampledMask");

    public boolean isSampled(int dimensionIndex) {
        return (sampledMask & (1 << dimensionIndex)) != 0;
    }

    /**
     * Updates a field without any locking.
     * @return true if this thread successfully committed the value, false if another thread beat us to it.
     */
    public boolean updateField(int dimensionIndex, long value) {
        int currentMask;
        int nextMask;
        int bit = 1 << dimensionIndex;

        // Try to set new mask, if failure then abort (another thread already updated the value)
        do {
            currentMask = this.sampledMask;
            if ((currentMask & bit) != 0)
                return false;
            nextMask = currentMask | bit;
        } while (!MASK_UPDATER.compareAndSet(this, currentMask, nextMask));

        switch (dimensionIndex) {
            case 0 -> temperature = value;
            case 1 -> humidity = value;
            case 2 -> continentalness = value;
            case 3 -> erosion = value;
            case 4 -> depth = value;
            case 5 -> weirdness = value;
        }
        return true;
    }

    public long getField(int dimensionIndex) {
        long value = 0;
        switch (dimensionIndex) {
            case 0 -> value = temperature;
            case 1 -> value = humidity;
            case 2 -> value = continentalness;
            case 3 -> value = erosion;
            case 4 -> value = depth;
            case 5 -> value = weirdness;
            default -> TbUtils.doCrash("getField called with invalid dimensionIndex '" + dimensionIndex + "', fixme!");
        }
        return value;
    }

    private float computeNew(int dimensionIndex, Climate.Sampler sampler, int blockX, int blockY, int blockZ, boolean dummy3dNoiseForCacheOnly) {
        float result = 0.0f; // default for 3d noise
        DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
        if (dimensionIndex == 0) {
            result = (dummy3dNoiseForCacheOnly && !isDimension2dNoise(dimensionIndex)) ? 0.0f : (float) sampler.temperature().compute(context);
        } else if (dimensionIndex == 1) {
            result = (dummy3dNoiseForCacheOnly && !isDimension2dNoise(dimensionIndex)) ? 0.0f : (float) sampler.humidity().compute(context);
        } else if (dimensionIndex == 2) {
            result = (dummy3dNoiseForCacheOnly && !isDimension2dNoise(dimensionIndex)) ? 0.0f : (float) sampler.continentalness().compute(context);
        } else if (dimensionIndex == 3) {
            result = (dummy3dNoiseForCacheOnly && !isDimension2dNoise(dimensionIndex)) ? 0.0f : (float) sampler.erosion().compute(context);
        } else if (dimensionIndex == 4) {
            result = (dummy3dNoiseForCacheOnly && !isDimension2dNoise(dimensionIndex)) ? 0.0f : (float) sampler.depth().compute(context);
        } else if (dimensionIndex == 5) {
            result = (dummy3dNoiseForCacheOnly && !isDimension2dNoise(dimensionIndex)) ? 0.0f : (float) sampler.weirdness().compute(context);
        } else {
            TbUtils.doCrash("computeParamCached called with invalid index '" + dimensionIndex + "', fixme!");
        }
        return result;
    }

    private boolean isDimension2dNoise(int dimensionIndex) {
        return IS_DIMENSION_2D[dimensionIndex];
    }

    public long getOrComputeDimension(int dimensionIndex, Climate.Sampler sampler, int blockX, int blockY, int blockZ, boolean dummy3dNoiseForCacheOnly) {
        if (isDimension2dNoise(dimensionIndex) && isSampled(dimensionIndex))
            return getField(dimensionIndex);
        else {
            float result = computeNew(dimensionIndex, sampler, blockX, blockY, blockZ, dummy3dNoiseForCacheOnly);
            long resultQuant = Climate.quantizeCoord(result);
            if (dummy3dNoiseForCacheOnly || isDimension2dNoise(dimensionIndex))
                updateField(dimensionIndex, resultQuant);
            return resultQuant;
        }
    }

    public float getOrComputeDimensionUnquantized(int dimensionIndex, Climate.Sampler sampler, int blockX, int blockY, int blockZ, boolean dummy3dNoiseForCacheOnly) {
        if (isDimension2dNoise(dimensionIndex) && isSampled(dimensionIndex))
            return Climate.unquantizeCoord(getField(dimensionIndex));
        else {
            float result = computeNew(dimensionIndex, sampler, blockX, blockY, blockZ, dummy3dNoiseForCacheOnly);
            long resultQuant = Climate.quantizeCoord(result);
            if (dummy3dNoiseForCacheOnly || isDimension2dNoise(dimensionIndex))
                updateField(dimensionIndex, resultQuant);
            return result;
        }
    }
}
