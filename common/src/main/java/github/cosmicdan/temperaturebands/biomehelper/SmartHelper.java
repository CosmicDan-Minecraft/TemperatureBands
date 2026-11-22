package github.cosmicdan.temperaturebands.biomehelper;

import github.cosmicdan.temperaturebands.*;
import github.cosmicdan.temperaturebands.generator.BiomeProximityGenerator;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class SmartHelper extends BiomeHelperBase {
    private final ParameterPointData oceanRiverPoints;

    public SmartHelper(DimensionData dimData, ServerLevel level, @Nullable MultiNoiseBiomeSource biomeSource) {
        if (biomeSource != null) {
            // first gather all ocean/river biome points
            List<Climate.ParameterPoint> oceanPointsRaw = new ArrayList<>();
            List<Climate.ParameterPoint> riverPointsRaw = new ArrayList<>();
            biomeSource.parameters().values().forEach(pair -> {
                Climate.ParameterPoint point = pair.getFirst();
                Holder<Biome> biomeHolder = pair.getSecond();
                if (biomeHolder.is(BiomeTags.IS_OCEAN))
                    oceanPointsRaw.add(point);
                if (biomeHolder.is(BiomeTags.IS_RIVER))
                    riverPointsRaw.add(point);
            });
            // second, merge parameter points where possible
            oceanRiverPoints = new ParameterPointData(
                    PointMerger.mergeParameterPoints(oceanPointsRaw), dimData.config.humidity3_climateDimensionSampleOrderOcean(),
                    PointMerger.mergeParameterPoints(riverPointsRaw), dimData.config.humidity3_climateDimensionSampleOrderRiver()
            );

            // do dump if configured
            if (TemperatureBands.CONFIG_GLOBAL.dumpRiverAndOceanBiomes()) {
                final Set<Holder<Biome>> biomeOceans = new HashSet<>();
                final Set<Holder<Biome>> biomeRivers = new HashSet<>();
                Set<Holder<Biome>> possibleBiomes = biomeSource.possibleBiomes();
                for (Holder<Biome> biomeHolder : possibleBiomes) {
                    if (biomeHolder.is(BiomeTags.IS_RIVER)) {
                        biomeRivers.add(biomeHolder);
                    } else if (biomeHolder.is(BiomeTags.IS_OCEAN)) {
                        biomeOceans.add(biomeHolder);
                    }
                }
                dumpBiomeInfoIfConfigured(level, biomeOceans, biomeRivers, oceanRiverPoints);
            }
        } else {
            oceanRiverPoints = null;
        }
    }

    public void onFirstCompute(DimensionData dimData, BiomeProximityGenerator.Config config) {
        // verify that all oceans have non-wildcard continentalness, and that humidity is a wildcard
        for (Climate.ParameterPoint point : oceanRiverPoints.oceanPoints) {
            if (point.continentalness().min() == PlatformConstants.CLIMATE_DIMENSION_VALUE_MIN && point.continentalness().max() == PlatformConstants.CLIMATE_DIMENSION_VALUE_MAX) {
                TbUtils.doCrash(buildVerifyErrorMsgForBannedWildcardDimension("minecraft:is_ocean", "continentalness"));
            }
            if (point.humidity().min() != PlatformConstants.CLIMATE_DIMENSION_VALUE_MIN || point.humidity().max() != PlatformConstants.CLIMATE_DIMENSION_VALUE_MAX) {
                TbUtils.doCrash(buildVerifyErrorMsgForMissingWildcardHumidity("minecraft:is_ocean"));
            }
        }
        // verify that all rivers have non-wildcard weirdness, and that humidity is a wildcard
        for (Climate.ParameterPoint point : oceanRiverPoints.riverPoints) {
            if (point.weirdness().min() == PlatformConstants.CLIMATE_DIMENSION_VALUE_MIN && point.weirdness().max() == PlatformConstants.CLIMATE_DIMENSION_VALUE_MAX) {
                TbUtils.doCrash(buildVerifyErrorMsgForBannedWildcardDimension("minecraft:is_river", "weirdness"));
            }
            if (point.humidity().min() != PlatformConstants.CLIMATE_DIMENSION_VALUE_MIN || point.humidity().max() != PlatformConstants.CLIMATE_DIMENSION_VALUE_MAX) {
                TbUtils.doCrash(buildVerifyErrorMsgForMissingWildcardHumidity("minecraft:is_river"));
            }
        }
    }

    private String buildVerifyErrorMsgForBannedWildcardDimension(String biomeTag, String wildcardDimension) {
        return "Error: one of your '" + biomeTag + "' biomes has a wildcard range for the " + wildcardDimension + " dimension. Advanced/Refined humidity algorithms cannot function.\n" +
                "Please switch to Simple Humidity instead. Consider diagnosing and/or fixing this via the Custom Biome Parameters mod, and/or report this error to CosmicDan or\n" +
                "the author of whatever worldgen mod you're using.";
    }

    private String buildVerifyErrorMsgForMissingWildcardHumidity(String biomeTag) {
        return "Error: one of your '" + biomeTag + "' biomes does not have a wildcard range for the humidity dimension. Advanced/Refined humidity algorithms cannot function.\n" +
                "Please switch to Simple Humidity instead. Consider diagnosing and/or fixing this via the Custom Biome Parameters mod, and/or report this error to CosmicDan or\n" +
                "the author of whatever worldgen mod you're using.";
    }

    public CachedClimatePoint sample(int blockX, int blockY, int blockZ, DimensionData dimData, boolean sampleForOcean, boolean sampleForRiver, int shortcutsLevel) {
        // Round-down to nearest multiple of 2. Minecraft actually uses a 4x4x4 resolution for climate noise, but this causes accuracy issues when calculating distance.
        blockX = blockX & ~1;
        blockZ = blockZ & ~1;
        CachedClimatePoint cachedPoint = dimData.getCachedPoint(blockX, blockY, blockZ);
        oceanRiverPoints.computeRequired(cachedPoint, dimData.getClimateSampler(), blockX, blockY, blockZ, sampleForOcean, sampleForRiver, shortcutsLevel);
        return cachedPoint;
    }

    public CachedClimatePoint sampleSpecificDimension(int blockX, int blockY, int blockZ, DimensionData dimData, int climateDimension, int shortcutsLevel) {
        // Round-down to nearest multiple of 2. Minecraft actually uses a 4x4x4 resolution for climate noise, but this causes accuracy issues when calculating distance.
        blockX = blockX & ~1;
        blockZ = blockZ & ~1;
        CachedClimatePoint cachedPoint = dimData.getCachedPoint(blockX, blockY, blockZ);
        // get the specific dimension requested
        cachedPoint.getOrComputeDimension(climateDimension, dimData.getClimateSampler(), blockX, blockY, blockZ, false);
        return cachedPoint;
    }

    public boolean isOcean(CachedClimatePoint sample, int shortcutsLevel) {
        return oceanRiverPoints.isOcean(sample, shortcutsLevel);
    }

    public boolean isRiver(CachedClimatePoint sample, int shortcutsLevel) {
        return oceanRiverPoints.isRiver(sample, shortcutsLevel);
    }

    private static class PointMerger {
        /**
         * Merges a list of Climate.ParameterPoint objects by combining adjacent
         * or overlapping ranges along any single matching dimension.
         *
         * A highly-compressed version of what was already done in Custom Biome Parameters mod.
         */
        public static List<Climate.ParameterPoint> mergeParameterPoints(List<Climate.ParameterPoint> inputPoints) {
            List<Climate.ParameterPoint> points = new ArrayList<>(inputPoints);
            boolean mergedAny;

            // Merge until no more merges possible
            do {
                mergedAny = false;
                List<Climate.ParameterPoint> nextPass = new ArrayList<>();
                boolean[] consumed = new boolean[points.size()];

                for (int i = 0; i < points.size(); i++) {
                    if (consumed[i]) continue;
                    Climate.ParameterPoint p1 = points.get(i);
                    boolean combined = false;

                    for (int j = i + 1; j < points.size(); j++) {
                        if (consumed[j]) continue;
                        Climate.ParameterPoint p2 = points.get(j);

                        Climate.ParameterPoint merged = tryMerge(p1, p2);
                        if (merged != null) {
                            nextPass.add(merged);
                            consumed[i] = true;
                            consumed[j] = true;
                            combined = true;
                            mergedAny = true;
                            break;
                        }
                    }

                    if (!combined) {
                        nextPass.add(p1);
                    }
                }
                points = nextPass;
            } while (mergedAny);

            return points;
        }

        private static Climate.ParameterPoint tryMerge(Climate.ParameterPoint p1, Climate.ParameterPoint p2) {
            if (p1.offset() != p2.offset())
                return null;

            boolean tempEq = Objects.equals(p1.temperature(), p2.temperature());
            boolean humEq = Objects.equals(p1.humidity(), p2.humidity());
            boolean contEq = Objects.equals(p1.continentalness(), p2.continentalness());
            boolean erosEq = Objects.equals(p1.erosion(), p2.erosion());
            boolean depthEq = Objects.equals(p1.depth(), p2.depth());
            boolean weirdEq = Objects.equals(p1.weirdness(), p2.weirdness());

            int differences = 0;
            if (!tempEq) differences++;
            if (!humEq) differences++;
            if (!contEq) differences++;
            if (!erosEq) differences++;
            if (!depthEq) differences++;
            if (!weirdEq) differences++;

            if (differences != 1)
                // more than 1 dimension differs, cannot merge
                return null;

            // Find and merge the one parameter that differs, if possible
            if (!tempEq && canMergeParams(p1.temperature(), p2.temperature())) {
                return new Climate.ParameterPoint(
                        mergeParams(p1.temperature(), p2.temperature()),
                        p1.humidity(), p1.continentalness(), p1.erosion(), p1.depth(), p1.weirdness(), p1.offset()
                );
            } else if (!humEq && canMergeParams(p1.humidity(), p2.humidity())) {
                return new Climate.ParameterPoint(
                        p1.temperature(),
                        mergeParams(p1.humidity(), p2.humidity()),
                        p1.continentalness(), p1.erosion(), p1.depth(), p1.weirdness(), p1.offset()
                );
            } else if (!contEq && canMergeParams(p1.continentalness(), p2.continentalness())) {
                return new Climate.ParameterPoint(
                        p1.temperature(), p1.humidity(),
                        mergeParams(p1.continentalness(), p2.continentalness()),
                        p1.erosion(), p1.depth(), p1.weirdness(), p1.offset()
                );
            } else if (!erosEq && canMergeParams(p1.erosion(), p2.erosion())) {
                return new Climate.ParameterPoint(
                        p1.temperature(), p1.humidity(), p1.continentalness(),
                        mergeParams(p1.erosion(), p2.erosion()),
                        p1.depth(), p1.weirdness(), p1.offset()
                );
            } else if (!depthEq && canMergeParams(p1.depth(), p2.depth())) {
                return new Climate.ParameterPoint(
                        p1.temperature(), p1.humidity(), p1.continentalness(), p1.erosion(),
                        mergeParams(p1.depth(), p2.depth()),
                        p1.weirdness(), p1.offset()
                );
            } else if (!weirdEq && canMergeParams(p1.weirdness(), p2.weirdness())) {
                return new Climate.ParameterPoint(
                        p1.temperature(), p1.humidity(), p1.continentalness(), p1.erosion(), p1.depth(),
                        mergeParams(p1.weirdness(), p2.weirdness()),
                        p1.offset()
                );
            }

            return null;
        }

        private static boolean canMergeParams(Climate.Parameter p1, Climate.Parameter p2) {
            // Check if both parameters are either overlapping or contiguous
            return Math.max(p1.min(), p2.min()) <= Math.min(p1.max(), p2.max()) + 1;
        }

        private static Climate.Parameter mergeParams(Climate.Parameter p1, Climate.Parameter p2) {
            long min = Math.min(p1.min(), p2.min());
            long max = Math.max(p1.max(), p2.max());
            return new Climate.Parameter(min, max);
        }
    }
}
