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

    public record Config(
            DensityFunctionEx owner,
            boolean swapBandAxis,
            int bandSize,
            float bandPositionShift,
            float tempRange,
            float tempGradeShift,
            int noiseFactor,
            int algo1bandVariance,
            float algo1bandVarianceSteepness,
            float tempWeight
    ) {}

    public static BandsGenerator create(DensityFunctionEx owner, DimensionConfig config, boolean isHumidity, float tempWeight) {
        return new BandsGenerator(new BandsGenerator.Config(
                owner,
                isHumidity != config.useVerticalBands(),
                isHumidity ? Math.round(config.bandSize() * config.humidityAlgo1MimicScale()) : config.bandSize(),
                config.bandPositionShift(),
                config.tempRange(),
                config.tempGradeShift(),
                config.noiseFactor(),
                config.algo1bandVariance(),
                config.algo1bandVarianceSteepness(),
                tempWeight // pass in -1.0 if not used (e.g. temperature function)
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
    }

    @Override
    public double onCompute(DensityFunction.FunctionContext context, DimensionData dimData) {
        int bandShift;
        int bandPos;
        int bandPosOffset = (int) (config.bandSize * (config.bandPositionShift * 8));
        if (config.swapBandAxis) {
            bandShift = Math.abs(context.blockZ());
            bandPos = Math.abs(context.blockX() - bandPosOffset);
        } else {
            bandShift = Math.abs(context.blockX());
            bandPos = Math.abs(context.blockZ() - bandPosOffset);
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

        if (config.noiseFactor > 0) {
            bandPos += (int) (config.owner.computeOriginal(context) * config.noiseFactor);
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

    @Override
    public void cancelAllCacheTasks() {}
}
