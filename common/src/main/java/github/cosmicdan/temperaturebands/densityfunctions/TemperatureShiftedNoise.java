package github.cosmicdan.temperaturebands.densityfunctions;

import github.cosmicdan.temperaturebands.CommonConfig;
import github.cosmicdan.temperaturebands.DimensionConfig;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.generator.BandsGenerator;
import github.cosmicdan.temperaturebands.generator.IGenerator;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jetbrains.annotations.NotNull;

public class TemperatureShiftedNoise extends ShiftedNoiseEx {
    public static final String NAME = "temperature";

    private final IGenerator gen;

    public TemperatureShiftedNoise(String dimensionName, DimensionConfig config, DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, NoiseHolder noise) {
        super(dimensionName, config, shiftX, shiftY, shiftZ, xzScale, yScale, noise);

        if (config.bandAlgorithm() == 1) {
            gen = new BandsGenerator(new BandsGenerator.Config(
                    this,
                    config.useVerticalBands(),
                    config.bandSize(),
                    config.bandPositionShift(),
                    config.tempRange(),
                    config.tempGradeShift(),
                    config.noiseFactor(),
                    config.algo1bandVariance(),
                    config.algo1bandVarianceSteepness(),
                    -1.0f // not used by temperature itself
            ));
        } else {
            TbUtils.doCrash("Temperature Bands has an unrecognized '" + CommonConfig.bandAlgorithmName + "' setting of '" + config.bandAlgorithm() + "'");
            gen = null;
        }
    }

    @Override
    public double onCompute(FunctionContext context) {
        return gen.onCompute(context, dimData);
    }

    @Override
    public void fillArray(double[] ds, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(ds, this);
    }

    @Override
    public @NotNull DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(
                new TemperatureShiftedNoise(
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
