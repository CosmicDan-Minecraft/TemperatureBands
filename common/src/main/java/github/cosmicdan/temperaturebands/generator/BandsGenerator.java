package github.cosmicdan.temperaturebands.generator;

import github.cosmicdan.temperaturebands.DimensionConfig;
import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.DensityFunctionEx;
import net.minecraft.world.level.levelgen.DensityFunction;

public class BandsGenerator implements IGenerator {
    public static final int algo1bandVarianceMin = 10; // required minimum value to do configAlgo1BandVariance processing

    private final Config config;
    // derived from constructor config arg
    private final float tempBandMid;
    public final float tempGradeAdj;
    public final int tempAlgo1bandVarianceMid;
    private final boolean bandSizeIsPowerOfTwo;
    public final boolean bandVarianceIsPowerOfTwo;
    private final int noiseFactor;

    public record Config(
            DensityFunctionEx owner,
            boolean swapBandAxis,
            int bandSize,
            float bandPositionShift,
            float tempRange,
            float tempGradeShift,
            float noiseFactorRaw,
            int algo1bandVariance,
            float algo1bandVarianceSteepness,
            float tempWeight,
            int bandLimitUpperCount,
            float bandLimitUpperValue,
            int bandLimitLowerCount,
            float bandLimitLowerValue
    ) {}

    public static BandsGenerator create(DensityFunctionEx owner, DimensionConfig config, boolean isHumidity, float tempWeight) {
        return new BandsGenerator(new BandsGenerator.Config(
                owner,
                isHumidity != config.useVerticalBands(),
                isHumidity ? Math.round(config.bandSize() * config.humidityAlgo1MimicScale()) : config.bandSize(),
                config.bandPositionShift(),
                config.tempRange(),
                config.tempGradeShift(),
                config.noiseFactorRaw(),
                config.algo1bandVariance(),
                config.algo1bandVarianceSteepness(),
                tempWeight, // pass in -1.0 if not used (e.g. temperature function)
                config.bandLimitUpperCount(),
                config.bandLimitUpperValue(),
                config.bandLimitLowerCount(),
                config.bandLimitLowerValue()
        ));
    }

    public BandsGenerator(Config config) {
        this.config = config;
        //derived values
        tempBandMid = config.bandSize / 2.0f;
        tempGradeAdj = tempBandMid / (config.tempRange * 2.0f);
        if (config.algo1bandVariance >= algo1bandVarianceMin) {
            tempAlgo1bandVarianceMid = (int) (config.algo1bandVariance * 0.5);
        } else {
            tempAlgo1bandVarianceMid = 0;
        }
        bandSizeIsPowerOfTwo = TbUtils.isNumberPowerOfTwo(config.bandSize);
        bandVarianceIsPowerOfTwo = TbUtils.isNumberPowerOfTwo(config.algo1bandVariance);
        if (config.noiseFactorRaw < 1.0) {
            noiseFactor = Math.round(config.noiseFactorRaw * config.bandSize);
        } else {
            noiseFactor = Math.round(config.noiseFactorRaw);
        }
    }

    @Override
    public double onCompute(DensityFunction.FunctionContext context, DimensionData dimData) {
        int bandShift;
        int bandPos;
        int bandPosOffset = (int) (config.bandSize * (config.bandPositionShift * 8));
        int blockPos;
        if (config.swapBandAxis) {
            bandShift = Math.abs(context.blockZ());
            bandPos = Math.abs(context.blockX() - bandPosOffset);
            blockPos = context.blockX();
        } else {
            bandShift = Math.abs(context.blockX());
            bandPos = Math.abs(context.blockZ() - bandPosOffset);
            blockPos = context.blockZ();
        }

        // check for band limits, return fixed value if so
        if (blockPos < 1 && config.bandLimitUpperCount > 0 && Math.abs(blockPos) > (config.bandSize * config.bandLimitUpperCount)) {
            return config.bandLimitUpperValue;
        } else if (blockPos > 1 && config.bandLimitLowerCount > 0 && Math.abs(blockPos) > (config.bandSize * config.bandLimitLowerCount)) {
            return config.bandLimitLowerValue;
        }

        bandShift = (int) (bandShift * config.algo1bandVarianceSteepness);
        if (config.algo1bandVariance >= algo1bandVarianceMin) {
            // using band variance
            bandShift = bandVarianceIsPowerOfTwo ?
                    TbUtils.fastRemainder(bandShift, config.algo1bandVariance) :
                    bandShift % config.algo1bandVariance;
            if (bandShift > tempAlgo1bandVarianceMid) {
                // shift is descending, adjust accordingly
                bandShift = tempAlgo1bandVarianceMid - (bandShift - tempAlgo1bandVarianceMid);
            }
            bandPos += bandShift;
        }

        // divide bandPos by 8 because we calculate based on a bouncing gradient, idk better words lol
        bandPos /= 8;

        if (noiseFactor > 0) {
            bandPos += (int) (config.owner.computeOriginal(context) * noiseFactor);
        }

        // calculate grade
        float grade = bandSizeIsPowerOfTwo ?
                TbUtils.fastRemainder(bandPos, config.bandSize) :
                bandPos % config.bandSize;
        if (grade > tempBandMid) {
            // grade is descending, adjust accordingly
            grade = tempBandMid - (grade - tempBandMid);
        }
        grade = grade / tempGradeAdj;
        // shift grade because vanilla has a bias for cold
        grade = grade + config.tempGradeShift;
        if (grade < 0.0f)
            grade = 0.0f;
        float tempLimit = grade - config.tempRange;
        // invert to match vanilla lower = colder
        tempLimit = -tempLimit;

        if (config.tempWeight > 0.0 ) {
            // Only relevant for humidity
            double tempValue = dimData.getTemperatureNoise().compute(context);
            tempLimit += (float) (tempValue * config.tempWeight);
        }

        return tempLimit;
    }
}
