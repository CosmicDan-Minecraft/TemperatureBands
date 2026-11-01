package github.cosmicdan.temperaturebands.climatesampler;

import github.cosmicdan.temperaturebands.*;
import github.cosmicdan.temperaturebands.biomehelper.SmartHelper;
import github.cosmicdan.temperaturebands.generator.BiomeProximityGenerator;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * A much-improved expansion of the simple sampler.
 * Original design and code by CosmicDan, but AI was used to find bugs and some small micro-optimizations.
 */
public class RefinedSampler implements IClimateSampler {
    // Segment index pairs for the 5 sampled cell points: 0=TL, 1=TR, 2=BL, 3=BR, 4=C
    private static final int[][] RIVER_SEGMENTS = {
            {0, 1}, {2, 3}, {0, 2}, {1, 3}, // 4 outer edges
            {0, 4}, {1, 4}, {2, 4}, {3, 4}  // 4 center diagonals
    };

    private final BiomeProximityGenerator.Config config;
    // derived values
    private final boolean isCellSizePowerOfTwo;
    private final int maxSearchSteps;
    private final double weightReciprocal;
    private final long riverGrazeThreshold;

    public RefinedSampler(BiomeProximityGenerator.Config configIn, DimensionConfig dimConfig) {
        this.config = configIn;

        this.isCellSizePowerOfTwo = TbUtils.isNumberPowerOfTwo(config.algo3cellSize());
        this.maxSearchSteps = Math.max(config.algo3searchDistanceOcean(), config.algo3searchDistanceRiver()) / config.algo3cellSize();
        double maxWeight = config.algo3noiseWeightOcean() + config.algo3noiseWeightRiver();
        this.weightReciprocal = maxWeight > 0.0 ? 1.0 / maxWeight : 0.0;

        // Quantized long threshold (2500L at cellSize 64), scaled proportionally to cellSize
        this.riverGrazeThreshold = Math.round(Climate.quantizeCoord(0.25f) * (config.algo3cellSize() / 64.0));
    }

    @Override
    public void onFirstCompute(DimensionData dimData, BiomeProximityGenerator.Config config) {
        dimData.getSmartHelper().onFirstCompute(dimData, config);
    }

    @Override
    public double calculateHumidity(DensityFunction.FunctionContext context, DimensionData dimData) {
        double[] distanceResult = findNearestOceanRiver(context.blockX(), context.blockZ(), dimData);
        double oceanDistance = distanceResult[0];
        double riverDistance = distanceResult[1];

        double finalNoise = 0.0;

        if (oceanDistance >= 0.0 && oceanDistance < config.algo3searchDistanceOcean()) {
            double proximityOcean = 1.0 - (oceanDistance / (double) config.algo3searchDistanceOcean());
            finalNoise += proximityOcean * config.algo3noiseWeightOcean();
        }

        if (riverDistance >= 0.0 && riverDistance < config.algo3searchDistanceRiver()) {
            double proximityRiver = 1.0 - (riverDistance / (double) config.algo3searchDistanceRiver());
            finalNoise += proximityRiver * config.algo3noiseWeightRiver();
        }

        // Do tempWeight if configured
        if (config.tempWeight() > 0.0) {
            SmartHelper helper = dimData.getSmartHelper();
            CachedClimatePoint point = helper.sampleSpecificDimension(context.blockX(), context.blockY(), context.blockZ(), dimData, 0, config.algo3shortcutsLevel());
            float pointTemperatureRaw = Climate.unquantizeCoord(point.temperature);
            finalNoise += (pointTemperatureRaw * config.tempWeight());
        }

        // Normalize to [0.0, 1.0], map to [-1.0, +1.0], and apply final scale
        finalNoise = ((finalNoise * weightReciprocal) * 2.0) - 1.0;
        return finalNoise * config.algo3noiseRange();
    }

    @Override
    public double[] findNearestOceanRiver(int originX, int originZ, DimensionData dimData) {
        double[] bestDists = new double[]{config.algo3searchDistanceOcean(), config.algo3searchDistanceRiver()};
        boolean searchForOcean = config.algo3searchDistanceOcean() > 0;
        boolean searchForRiver = config.algo3searchDistanceRiver() > 0;

        final int alignedGridX = isCellSizePowerOfTwo
                ? (originX & -config.algo3cellSize())
                : Math.floorDiv(originX, config.algo3cellSize()) * config.algo3cellSize();
        final int alignedGridZ = isCellSizePowerOfTwo
                ? (originZ & -config.algo3cellSize())
                : Math.floorDiv(originZ, config.algo3cellSize()) * config.algo3cellSize();

        for (int ring = 0; ring <= maxSearchSteps; ring++) {
            double minDistanceThisRing = Math.max(0, (ring - 1) * config.algo3cellSize());
            if (minDistanceThisRing >= bestDists[0]) searchForOcean = false;
            if (minDistanceThisRing >= bestDists[1]) searchForRiver = false;
            if (!searchForOcean && !searchForRiver) break;

            int offsetRing = ring * config.algo3cellSize();
            for (int i = -ring; i <= ring; i++) {
                // Re-check dynamically mid-ring in case an earlier cell in this ring found a close hit
                boolean activeOcean = searchForOcean && minDistanceThisRing < bestDists[0];
                boolean activeRiver = searchForRiver && minDistanceThisRing < bestDists[1];
                if (!activeOcean && !activeRiver) break;

                int offsetI = i * config.algo3cellSize();

                // Top wall (also covers center cell when ring == 0)
                checkCellAndUpdate(alignedGridX + offsetI, alignedGridZ - offsetRing, originX, originZ, dimData, activeOcean, activeRiver, bestDists);

                if (ring > 0) {
                    // Bottom wall
                    checkCellAndUpdate(alignedGridX + offsetI, alignedGridZ + offsetRing, originX, originZ, dimData, activeOcean, activeRiver, bestDists);

                    // Left and right walls (excluding corners already covered by top/bottom)
                    if (i > -ring && i < ring) {
                        checkCellAndUpdate(alignedGridX - offsetRing, alignedGridZ + offsetI, originX, originZ, dimData, activeOcean, activeRiver, bestDists);
                        checkCellAndUpdate(alignedGridX + offsetRing, alignedGridZ + offsetI, originX, originZ, dimData, activeOcean, activeRiver, bestDists);
                    }
                }
            }
        }

        return bestDists;
    }

    private void checkCellAndUpdate(int cellX, int cellZ, int origX, int origZ, DimensionData dimData, boolean searchOcean, boolean searchRiver, double[] bestDists) {
        int maxX = cellX + config.algo3cellSize();
        int maxZ = cellZ + config.algo3cellSize();
        int midX = cellX + (config.algo3cellSize() >> 1);
        int midZ = cellZ + (config.algo3cellSize() >> 1);

        int[] xs = new int[]{cellX, maxX, cellX, maxX, midX};
        int[] zs = new int[]{cellZ, cellZ, maxZ, maxZ, midZ};
        CachedClimatePoint[] pts = new CachedClimatePoint[5];
        boolean[] isOceanPt = new boolean[5];

        SmartHelper helper = dimData.getSmartHelper();
        for (int i = 0; i < 5; i++) {
            pts[i] = helper.sample(xs[i], PlatformConstants.BLOCK_Y_FIXED, zs[i], dimData, searchOcean, searchRiver, config.algo3shortcutsLevel());
            if (searchOcean) {
                isOceanPt[i] = helper.isOcean(pts[i], config.algo3shortcutsLevel());
            }
        }

        if (searchOcean) {
            int closestIdx = -1;
            double minCornerDist = Double.MAX_VALUE;

            for (int i = 0; i < 5; i++) {
                if (isOceanPt[i]) {
                    double d = TbUtils.calculateBlockDistance(config.distanceFunction(), origX, xs[i], origZ, zs[i]);
                    if (d < minCornerDist) {
                        minCornerDist = d;
                        closestIdx = i;
                    }
                }
            }

            if (closestIdx != -1) {
                double refined = refineOcean(cellX, cellZ, minCornerDist, origX, origZ, dimData);
                if (refined < bestDists[0]) bestDists[0] = refined;
            }
        }

        if (searchRiver) {
            double localBestRiver = bestDists[1];

            // Check for direct hit check on the 5 base points (in case weirdness is an exact 0)
            for (int i = 0; i < 5; i++) {
                if (helper.isRiver(pts[i], config.algo3shortcutsLevel())) {
                    double d = TbUtils.calculateBlockDistance(config.distanceFunction(), origX, xs[i], origZ, zs[i]);
                    if (d < localBestRiver)
                        localBestRiver = d;
                }
            }

            // Check all 8 segments for single crossings AND same-edge double crossings
            int bestSegA = -1, bestSegB = -1;
            double minAnchorDist = Double.MAX_VALUE;

            boolean hasDoubleCross = false;
            int dblX1 = 0, dblZ1 = 0, dblMidX = 0, dblMidZ = 0, dblX2 = 0, dblZ2 = 0;
            long dblW1 = 0L, dblMidW = 0L, dblW2 = 0L;
            double minDoubleAnchorDist = Double.MAX_VALUE;

            for (int[] seg : RIVER_SEGMENTS) {
                int a = seg[0];
                int b = seg[1];

                // Skip segments that are entirely inside open ocean
                if (searchOcean && isOceanPt[a] && isOceanPt[b])
                    continue;

                long wA = pts[a].weirdness;
                long wB = pts[b].weirdness;
                int anchorX = (xs[a] + xs[b]) >> 1;
                int anchorZ = (zs[a] + zs[b]) >> 1;
                double anchorDist = TbUtils.calculateBlockDistance(config.distanceFunction(), origX, anchorX, origZ, anchorZ);

                if (crossesOrTouchesZero(wA, wB)) {
                    if (anchorDist < minAnchorDist) {
                        minAnchorDist = anchorDist;
                        bestSegA = a;
                        bestSegB = b;
                    }
                } else if (Math.abs(wA) < riverGrazeThreshold || Math.abs(wB) < riverGrazeThreshold) {
                    CachedClimatePoint midPt = helper.sample(anchorX, PlatformConstants.BLOCK_Y_FIXED, anchorZ, dimData, false, true, config.algo3shortcutsLevel());
                    long wM = midPt.weirdness;
                    if (helper.isRiver(midPt, config.algo3shortcutsLevel()) && anchorDist < localBestRiver) {
                        localBestRiver = anchorDist;
                    }
                    if (crossesOrTouchesZero(wA, wM) && anchorDist < minDoubleAnchorDist) {
                        minDoubleAnchorDist = anchorDist;
                        hasDoubleCross = true;
                        dblX1 = xs[a]; dblZ1 = zs[a]; dblW1 = wA;
                        dblMidX = anchorX; dblMidZ = anchorZ; dblMidW = wM;
                        dblX2 = xs[b]; dblZ2 = zs[b]; dblW2 = wB;
                    }
                }
            }
            if (bestSegA != -1) {
                localBestRiver = refineRiverSegment(
                        xs[bestSegA], zs[bestSegA], pts[bestSegA].weirdness,
                        xs[bestSegB], zs[bestSegB], pts[bestSegB].weirdness,
                        origX, origZ, localBestRiver, dimData
                );
            }
            if (hasDoubleCross) {
                localBestRiver = refineRiverSegment(dblX1, dblZ1, dblW1, dblMidX, dblMidZ, dblMidW, origX, origZ, localBestRiver, dimData);
                localBestRiver = refineRiverSegment(dblMidX, dblMidZ, dblMidW, dblX2, dblZ2, dblW2, origX, origZ, localBestRiver, dimData);
            }
            bestDists[1] = localBestRiver;
        }
    }

    /**
     * Overflow-safe check for whether two quantized long weirdness values cross or touch zero.
     */
    private static boolean crossesOrTouchesZero(long w1, long w2) {
        return (w1 <= 0L && w2 >= 0L) || (w1 >= 0L && w2 <= 0L);
    }

    /**
     * Uses false-position (linear interpolation) with bisection fallback, aligned to SmartHelper's 2-block grid.
     */
    private double refineRiverSegment(int x1, int z1, long w1, int x2, int z2, long w2, int origX, int origZ, double currentBestDist, DimensionData dimData) {
        SmartHelper helper = dimData.getSmartHelper();
        double bestDist = currentBestDist;
        for (int i = 0; i < config.algo3cellSubsamplingRiver(); i++) {
            int sX1 = x1 & ~1;
            int sZ1 = z1 & ~1;
            int sX2 = x2 & ~1;
            int sZ2 = z2 & ~1;
            if (Math.abs(sX2 - sX1) <= 2 && Math.abs(sZ2 - sZ1) <= 2) break;
            long absW1 = Math.abs(w1);
            long absW2 = Math.abs(w2);
            long denom = absW1 + absW2;
            double t = denom > 0L ? (double) absW1 / (double) denom : 0.5;
            int midX = ((int) Math.round(x1 + t * (x2 - x1))) & ~1;
            int midZ = ((int) Math.round(z1 + t * (z2 - z1))) & ~1;
            // If false-position stalls on an endpoint due to curve skewness, fall back to bisection
            if ((midX == sX1 && midZ == sZ1) || (midX == sX2 && midZ == sZ2)) {
                midX = ((sX1 + sX2) >> 1) & ~1;
                midZ = ((sZ1 + sZ2) >> 1) & ~1;
                if ((midX == sX1 && midZ == sZ1) || (midX == sX2 && midZ == sZ2))
                    break;
            }
            CachedClimatePoint midPt = helper.sample(midX, PlatformConstants.BLOCK_Y_FIXED, midZ, dimData, false, true, config.algo3shortcutsLevel());
            if (helper.isRiver(midPt, config.algo3shortcutsLevel())) {
                double d = TbUtils.calculateBlockDistance(config.distanceFunction(), origX, midX, origZ, midZ);
                if (d < bestDist) bestDist = d;
            }
            if (crossesOrTouchesZero(w1, midPt.weirdness)) {
                x2 = midX;
                z2 = midZ;
                w2 = midPt.weirdness;
            } else {
                x1 = midX;
                z1 = midZ;
                w1 = midPt.weirdness;
            }
        }
        return bestDist;
    }

    private double refineOcean(int windowMinX, int windowMinZ, double initialDist,
                               int origX, int origZ, DimensionData dimData) {
        int windowMaxX = windowMinX + config.algo3cellSize();
        int windowMaxZ = windowMinZ + config.algo3cellSize();
        int lowX = windowMinX;
        int lowZ = windowMinZ;
        int highX = windowMaxX;
        int highZ = windowMaxZ;
        double bestDist = initialDist;
        SmartHelper helper = dimData.getSmartHelper();
        for (int i = 0; i < config.algo3cellSubsamplingOcean(); i++) {
            if ((highX - lowX) <= 2 && (highZ - lowZ) <= 2) break;
            int midX = ((lowX + highX) >> 1) & ~1;
            int midZ = ((lowZ + highZ) >> 1) & ~1;
            CachedClimatePoint sampleCenter = helper.sample(midX, PlatformConstants.BLOCK_Y_FIXED, midZ, dimData, true, false, config.algo3shortcutsLevel());
            if (helper.isOcean(sampleCenter, config.algo3shortcutsLevel())) {
                double currentDist = TbUtils.calculateBlockDistance(config.distanceFunction(), origX, midX, origZ, midZ);
                if (currentDist < bestDist) bestDist = currentDist;
                if (origX < midX) highX = midX;
                else if (origX > midX) lowX = midX;
                else lowX = highX = midX;
                if (origZ < midZ) highZ = midZ;
                else if (origZ > midZ) lowZ = midZ;
                else lowZ = highZ = midZ;
            } else {
                // Ensure probe point never clamps onto midX itself at the upper cell boundary
                int stepX = (midX + 2 <= windowMaxX) ? (midX + 2) : (midX - 2);
                int stepZ = (midZ + 2 <= windowMaxZ) ? (midZ + 2) : (midZ - 2);
                CachedClimatePoint sampleX = helper.sample(stepX, PlatformConstants.BLOCK_Y_FIXED, midZ, dimData, true, false, config.algo3shortcutsLevel());
                CachedClimatePoint sampleZ = helper.sample(midX, PlatformConstants.BLOCK_Y_FIXED, stepZ, dimData, true, false, config.algo3shortcutsLevel());
                boolean lowerContX = (stepX > midX)
                        ? (sampleX.continentalness < sampleCenter.continentalness)
                        : (sampleX.continentalness > sampleCenter.continentalness);
                if (lowerContX) lowX = midX;
                else highX = midX;
                boolean lowerContZ = (stepZ > midZ)
                        ? (sampleZ.continentalness < sampleCenter.continentalness)
                        : (sampleZ.continentalness > sampleCenter.continentalness);
                if (lowerContZ) lowZ = midZ;
                else highZ = midZ;
            }
        }
        return bestDist;
    }
}
