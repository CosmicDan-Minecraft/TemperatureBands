package github.cosmicdan.temperaturebands.biomehelper;

import github.cosmicdan.temperaturebands.CachedClimatePoint;
import github.cosmicdan.temperaturebands.PlatformConstants;
import github.cosmicdan.temperaturebands.TbUtils;
import net.minecraft.world.level.biome.Climate;

import java.util.List;

public class ParameterPointData {
    public final List<Climate.ParameterPoint> oceanPoints;
    private final int[] oceanParamMapping = new int[PlatformConstants.DIMENSION_COUNT];
    private final boolean[] oceanParamRequired = new boolean[PlatformConstants.DIMENSION_COUNT];
    private final int oceanRequiredMask;

    public final List<Climate.ParameterPoint> riverPoints;
    private final int[] riverParamMapping = new int[PlatformConstants.DIMENSION_COUNT];
    private final boolean[] riverParamRequired = new boolean[PlatformConstants.DIMENSION_COUNT];
    private final int riverRequiredMask;

    private final int allParamsMask;

    public ParameterPointData(List<Climate.ParameterPoint> oceanPointsIn, List<? extends Integer> oceanOrder,
                              List<Climate.ParameterPoint> riverPointsIn, List<? extends Integer> riverOrder) {
        this.oceanPoints = oceanPointsIn;
        this.riverPoints = riverPointsIn;
        this.oceanRequiredMask = setup(oceanParamMapping, oceanOrder, oceanPoints, oceanParamRequired);
        this.riverRequiredMask = setup(riverParamMapping, riverOrder, riverPointsIn, riverParamRequired);

        int mask = 0;
        for (int dim = 0; dim < PlatformConstants.DIMENSION_COUNT; dim++) {
            // Exclude humidity (1) and depth (4)
            if (dim != 1 && dim != 4) {
                mask |= (1 << dim);
            }
        }
        this.allParamsMask = mask;
    }

    private int setup(int[] paramMapping, List<? extends Integer> order, List<Climate.ParameterPoint> points, boolean[] paramRequired) {
        if (order.size() != PlatformConstants.DIMENSION_COUNT)
            TbUtils.doCrash("The provided dimension sample order must have exactly six elements (please fix your config): " + order);

        int mappingIndex = 0;
        for (int dimensionIndex : order) {
            if (dimensionIndex < 0 || dimensionIndex >= PlatformConstants.DIMENSION_COUNT)
                TbUtils.doCrash("Value '" + dimensionIndex + "' is not valid in provided dimension sample order (please fix your config): " + order);
            else {
                paramMapping[mappingIndex] = dimensionIndex;
            }
            mappingIndex++;
        }

        int requiredBitmask = 0;
        for (Climate.ParameterPoint point : points) {
            requiredBitmask |= setParamRequiredIfNotWildcard(point.temperature(), 0, paramMapping, paramRequired);
            requiredBitmask |= setParamRequiredIfNotWildcard(point.continentalness(), 2, paramMapping, paramRequired);
            requiredBitmask |= setParamRequiredIfNotWildcard(point.erosion(), 3, paramMapping, paramRequired);
            requiredBitmask |= setParamRequiredIfNotWildcard(point.depth(), 4, paramMapping, paramRequired);// TODO: Remove this?
            requiredBitmask |= setParamRequiredIfNotWildcard(point.weirdness(), 5, paramMapping, paramRequired);
        }
        return requiredBitmask;
    }

    private int setParamRequiredIfNotWildcard(Climate.Parameter param, int paramIndex, int[] paramMapping, boolean[] paramRequired) {
        if (param.min() > -PlatformConstants.CLIMATE_DIMENSION_MIN_MAX_WILDCARD || param.max() < PlatformConstants.CLIMATE_DIMENSION_MIN_MAX_WILDCARD) {
            for (int i = 0; i < PlatformConstants.DIMENSION_COUNT; i++) {
                if (paramMapping[i] == paramIndex) {
                    paramRequired[i] = true;
                    return (paramIndex != 1 && paramIndex != 4) ? (1 << paramIndex) : 0;
                }
            }
        }
        return 0;
    }

    public void computeRequired(CachedClimatePoint cachedPoint, Climate.Sampler sampler,
                                int blockX, int blockY, int blockZ,
                                boolean sampleForOcean, boolean sampleForRiver, int shortcutsLevel) {
        int neededMask;
        if (shortcutsLevel == 0) {
            neededMask = allParamsMask;
        } else {
            neededMask = 0;
            if (sampleForOcean) neededMask |= oceanRequiredMask;
            if (sampleForRiver) neededMask |= riverRequiredMask;
        }

        if (neededMask == 0) return;

        for (int dim = 0; dim < PlatformConstants.DIMENSION_COUNT; dim++) {
            if ((neededMask & (1 << dim)) != 0 && !cachedPoint.isSampled(dim)) {
                cachedPoint.getOrComputeDimension(dim, sampler, blockX, blockY, blockZ, true);
            }
        }
    }

    public boolean isOcean(CachedClimatePoint sample, int shortcutsLevel) {
        return isMatchDirect(sample, shortcutsLevel, oceanPoints, oceanParamMapping, oceanParamRequired);
    }

    public boolean isRiver(CachedClimatePoint sample, int shortcutsLevel) {
        return isMatchDirect(sample, shortcutsLevel, riverPoints, riverParamMapping, riverParamRequired);
    }

    private boolean isMatchDirect(CachedClimatePoint sample, int shortcutsLevel,
                                  List<Climate.ParameterPoint> points, int[] parameterMapping, boolean[] parameterRequired) {
        pointLoop:
        for (Climate.ParameterPoint point : points) {
            for (int i = 0; i < parameterMapping.length; i++) {
                if (shortcutsLevel == 0 || parameterRequired[i]) {
                    int dimIndex = parameterMapping[i];
                    if (dimIndex == 1 || dimIndex == 4)
                        continue; // Skip humidity (1) and depth (4). We already verify elsewhere that humidity is a wildcard for all oceans/rivers.

                    Climate.Parameter param = switch (dimIndex) {
                        case 0 -> point.temperature();
                        case 2 -> point.continentalness();
                        case 3 -> point.erosion();
                        case 5 -> point.weirdness();
                        default -> {
                            TbUtils.doCrash("Unhandled parameterMapping value '" + dimIndex + "', fixme!");
                            yield null;
                        }
                    };

                    if (param == null) return false;

                    long val;
                    if (sample.isSampled(dimIndex)) {
                        val = sample.getField(dimIndex);
                    } else {
                        TbUtils.doCrash("Tried to get param value at dimension index " + dimIndex + " (mapped at " + i + ") but it wasn't computed yet, fixme!");
                        return false;
                    }

                    if (val < param.min() || val > param.max()) {
                        continue pointLoop;
                    }
                }
            }
            return true;
        }
        return false;
    }
}
