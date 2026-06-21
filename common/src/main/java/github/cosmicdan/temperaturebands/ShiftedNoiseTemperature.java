package github.cosmicdan.temperaturebands;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jetbrains.annotations.NotNull;

public class ShiftedNoiseTemperature extends ShiftedNoiseEx {
    public static final String NAME = "temperature";
    public static final int algo1bandVarianceMin = 10; // required minimum value to do configAlgo1BandVariance processing
    public final float tempBandMid;
    public final float tempGradeAdj;
    public final int tempAlgo1bandVarianceMid;
    public final boolean bandVarianceIsPowerOfTwo;
    public final boolean bandSizeIsPowerOfTwo;

    public ShiftedNoiseTemperature(String dimensionName, DimensionConfig config, DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, NoiseHolder noise) {
        super(dimensionName, config, shiftX, shiftY, shiftZ, xzScale, yScale, noise);
        tempBandMid = config.bandSize() / 2.0f;
        tempGradeAdj = tempBandMid / (config.tempRange() * 2.0f);
        if (config.algo1bandVariance() >= algo1bandVarianceMin) {
            tempAlgo1bandVarianceMid = (int) (config.algo1bandVariance() * 0.5);
        } else {
            tempAlgo1bandVarianceMid = 0;
        }
        bandVarianceIsPowerOfTwo = TbUtils.isNumberPowerOfTwo(config.algo1bandVariance());
        bandSizeIsPowerOfTwo = TbUtils.isNumberPowerOfTwo(config.bandSize());
    }

    @Override
    public double computeOriginal(FunctionContext context) {
        double d = context.blockX() * this.xzScale + this.shiftX.compute(context);
        double e = context.blockY() * this.yScale + this.shiftY.compute(context);
        double f = context.blockZ() * this.xzScale + this.shiftZ.compute(context);
        return this.noise.getValue(d, e, f);
    }

    @Override
    public double compute(FunctionContext context) {
        int bandPos;
        int bandShift;
        if (config.useVerticalBands()) {
            bandPos = Math.abs(context.blockX() - (int) (config.bandSize() * (config.bandPositionShift() * 8)));
            bandShift = Math.abs(context.blockZ());
        } else {
            bandPos = Math.abs(context.blockZ() - (int) (config.bandSize() * (config.bandPositionShift() * 8)));
            bandShift = Math.abs(context.blockX());
        }

        if (config.bandAlgorithm() == 1) {
            bandShift = (int) (bandShift * config.algo1bandVarianceSteepness());
            if (config.algo1bandVariance() >= algo1bandVarianceMin) {
                // using band variance
                bandShift = bandVarianceIsPowerOfTwo ?
                        TbUtils.fastRemainder(bandShift, config.algo1bandVariance()) :
                        bandShift % config.algo1bandVariance();
                if (bandShift > tempAlgo1bandVarianceMid) {
                    // shift is descending, adjust accordingly
                    bandShift = tempAlgo1bandVarianceMid - (bandShift - tempAlgo1bandVarianceMid);
                }
                bandPos += bandShift;
            }

            // divide bandPos by 8 because we calculate based on a bouncing gradient, idk better words lol
            bandPos /= 8;

            if (config.noiseFactor() > 0) {
                bandPos += (int) (computeOriginal(context) * config.noiseFactor());
            }
        } else {
            throw new RuntimeException("Temperature Bands has an invalid algorithm setting (" + config.bandAlgorithm() + ")");
        }

        // calculate grade
        float grade = bandSizeIsPowerOfTwo ?
                TbUtils.fastRemainder(bandPos, config.bandSize()) :
                bandPos % config.bandSize();
        if (grade > tempBandMid) {
            // grade is descending, adjust accordingly
            grade = tempBandMid - (grade - tempBandMid);
        }
        grade = grade / (tempGradeAdj);
        // shift grade because vanilla has a bias for cold
        grade = grade + config.tempGradeShift();
        if (grade < 0.0f)
            grade = 0.0f;
        float tempLimit = grade - config.tempRange();
        // invert to match vanilla lower = colder
        tempLimit = -tempLimit;

        return tempLimit;
    }

    @Override
    public void fillArray(double[] ds, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(ds, this);
    }

    @Override
    public @NotNull DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(
                new ShiftedNoiseTemperature(
                        dimensionName, config, shiftX.mapAll(visitor), shiftY.mapAll(visitor), shiftZ.mapAll(visitor), xzScale, yScale, visitor.visitNoise(this.noise)
                )
        );
    }

    @Override
    public double minValue() {
        return -this.maxValue();
    }

    @Override
    public double maxValue() {
        return this.noise.maxValue();
    }


    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return DensityFunctions.ShiftedNoise.CODEC;
    }
}
