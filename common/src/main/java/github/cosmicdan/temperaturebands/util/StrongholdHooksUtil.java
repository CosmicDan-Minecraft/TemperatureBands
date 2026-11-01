package github.cosmicdan.temperaturebands.util;

import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.PlatformConstants;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.concurrent.atomic.AtomicReference;

public class StrongholdHooksUtil {
    public static void initIfNeeded(AtomicReference<AtomicState> tb_$STATE, BiomeSource biomeSource) {
        if (tb_$STATE.get() == null) {
            for (DimensionData dimDataValue : TemperatureBands.DIMENSION_DATA_CACHE.asMap().values()) {
                if (dimDataValue.getBiomeSource() == biomeSource) {
                    tb_$STATE.set(new StrongholdHooksUtil.AtomicState(dimDataValue));
                }
            }
        }
    }

    public static class AtomicState {
        public final DimensionData dimData; // doesn't need to be atomic because all relevant members are effectively final at time of creation

        public final ThreadLocal<Long> continentalnessOceanMax = ThreadLocal.withInitial(() -> Long.MIN_VALUE);

        public AtomicState(DimensionData dimDataIn) {
            dimData = dimDataIn;
        }
    }

    public static int getCalcOverride(AtomicState threadState) {
        return threadState.dimData.config.strongholdCalcOverride();
    }

    public static int getRange(AtomicState threadState) {
        int rangeVal = threadState.dimData.config.strongholdCalcRange();
        if (rangeVal == 0)
            rangeVal = PlatformConstants.STRONGHOLD_SEARCH_RANGE_DEFAULT;
        return rangeVal;
    }

    public static BlockPos findAnyLandHorizontal(int blockXstart, int blockY, int blockZstart, AtomicState threadState, RandomSource randomSource, Climate.Sampler sampler) {
        int quartXstart = QuartPos.fromBlock(blockXstart);
        int quartZstart = QuartPos.fromBlock(blockZstart);
        int quartRange = QuartPos.fromBlock(getRange(threadState));
        int quartY = QuartPos.fromBlock(blockY);
        BlockPos retVal = null;
        int foundCount = 0;

        for (int quartZcount = -quartRange; quartZcount <= quartRange; quartZcount += 1) {
            for (int quartXcount = -quartRange; quartXcount <= quartRange; quartXcount += 1) {
                int quartXhere = quartXstart + quartXcount;
                int quartZhere = quartZstart + quartZcount;
                long continentalness = Climate.quantizeCoord((float) sampler.continentalness().compute(new DensityFunction.SinglePointContext(
                        QuartPos.toBlock(quartXhere),
                        QuartPos.toBlock(quartY),
                        QuartPos.toBlock(quartZhere)
                )));
                if (isAnyLand(threadState, continentalness)) {
                    if (retVal == null || randomSource.nextInt(foundCount + 1) == 0) {
                        retVal = new BlockPos(QuartPos.toBlock(quartXhere), blockY, QuartPos.toBlock(quartZhere));
                    }

                    foundCount++;
                }
            }
        }

        return retVal;
    }

    private static boolean isAnyLand(AtomicState threadState, long continentalnessInput) {
        if (threadState.continentalnessOceanMax.get() == Long.MIN_VALUE) {
            final long[] currentMax = {Long.MIN_VALUE};
            // need to calculate the maximum continentalness of all ocean biomes
            MultiNoiseBiomeSource biomeSource = threadState.dimData.getBiomeSource();
            if (biomeSource == null) {
                TbUtils.doCrash("MultiNoiseBiomeSource wasn't found for this dimension, cannot continue with this strongholdCalcOverride setting.");
            } else {
                biomeSource.parameters().values().forEach(pair -> {
                    Climate.ParameterPoint point = pair.getFirst();
                    Holder<Biome> biomeHolder = pair.getSecond();
                    if (biomeHolder.is(BiomeTags.IS_OCEAN))
                        //threadState.continentalnessOceanMax.set(Math.max(threadState.continentalnessOceanMax.get(), point.continentalness().max()));
                        currentMax[0] = Math.max(currentMax[0], point.continentalness().max());
                });
            }
            threadState.continentalnessOceanMax.set(currentMax[0]);
        }
        return continentalnessInput > threadState.continentalnessOceanMax.get();
    }
}
