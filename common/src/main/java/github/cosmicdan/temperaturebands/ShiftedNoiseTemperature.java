package github.cosmicdan.temperaturebands;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jetbrains.annotations.NotNull;

import static github.cosmicdan.temperaturebands.TemperatureBands.*;

// Modified copy of ShiftedNoise for Temperature override. DensityFunctionHooks is used to manually verify that ShiftedNoise hasn't changed.
public class ShiftedNoiseTemperature extends ShiftedNoiseEx {
    public static final String NAME = "temperature";
    public static final int algo1bandVarianceMin = 10; // fixed minimum value to enable configAlgo1BandVariance
    // config-derived values
    static float tempBandMid = 0f;
    static float tempGradeAdj = 0f;
    static int tempAlgo1bandVarianceMid = 0;

    public ShiftedNoiseTemperature(String dimensionName, DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, NoiseHolder noise) {
        super(dimensionName, shiftX, shiftY, shiftZ, xzScale, yScale, noise);
        tempBandMid = configBandSize / 2.0f;
        tempGradeAdj = tempBandMid / (configTempRange * 2.0f);
        if (configAlgo1BandVariance >= algo1bandVarianceMin) {
            tempAlgo1bandVarianceMid = (int) (configAlgo1BandVariance * 0.5);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public double compute(FunctionContext context) {
        int bandPos;
        int bandShift;
        if (configUseVerticalBands) {
            bandPos = Math.abs(context.blockX() - (int) (configBandSize * (configPosShift * 8)));
            bandShift = Math.abs(context.blockZ());
        } else {
            bandPos = Math.abs(context.blockZ() - (int) (configBandSize * (configPosShift * 8)));
            bandShift = Math.abs(context.blockX());
        }

        if (configAlgorithm == 1) {
            bandShift = (int) (bandShift * configAlgo1bandVarianceSteepness);
            if (configAlgo1BandVariance >= algo1bandVarianceMin) {
                // using band variance
                bandShift = bandShift % configAlgo1BandVariance;
                if (bandShift > tempAlgo1bandVarianceMid) {
                    // shift is descending, adjust accordingly
                    bandShift = tempAlgo1bandVarianceMid - (bandShift - tempAlgo1bandVarianceMid);
                }
                bandPos += bandShift;
            }

            // divide bandPos by 8 because we calculate based on a bouncing gradient, idk better words lol
            bandPos /= 8;

            if (configNoiseFactor > 0) {
                bandPos += (int) (computeNoise(context) * configNoiseFactor);
            }
        } else {
            throw new RuntimeException("Temperature Bands has an invalid algorithm setting (" + configAlgorithm + ")");
        }

        // calculate grade
        float grade = bandPos % configBandSize;
        if (grade > tempBandMid) {
            // grade is descending, adjust accordingly
            grade = tempBandMid - (grade - tempBandMid);
        }
        grade = grade / (tempGradeAdj);
        // shift grade because vanilla has a bias for cold
        grade = grade + configGradeShift;
        if (grade < 0.0f)
            grade = 0.0f;
        // we now have a grade from 0.0 to temperaturebands_$tempRange, make it a -/+ value with 0.0 at middle
        float tempLimit = grade - configTempRange;
        // invert to match vanilla lower = colder
        tempLimit = -tempLimit;

        return tempLimit;
    }

    private double computeNoise(FunctionContext context) {
        double d = context.blockX() * this.xzScale + this.shiftX.compute(context);
        double e = context.blockY() * this.yScale + this.shiftY.compute(context);
        double f = context.blockZ() * this.xzScale + this.shiftZ.compute(context);
        return this.noise.getValue(d, e, f);
    }

    @Override
    public void fillArray(double[] ds, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(ds, this);
    }

    @Override
    public @NotNull DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new ShiftedNoiseTemperature(dimensionName, shiftX.mapAll(visitor), shiftY.mapAll(visitor), shiftZ.mapAll(visitor), xzScale, yScale, visitor.visitNoise(this.noise)));
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
