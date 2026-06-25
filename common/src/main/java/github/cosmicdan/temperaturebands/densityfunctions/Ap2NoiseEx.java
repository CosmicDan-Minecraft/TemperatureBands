package github.cosmicdan.temperaturebands.densityfunctions;

import github.cosmicdan.temperaturebands.DimensionConfig;
import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.generator.IGenerator;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jetbrains.annotations.NotNull;

import static github.cosmicdan.temperaturebands.TemperatureBands.DIMENSION_DATA_CACHE;

public abstract class Ap2NoiseEx implements DensityFunctions.TwoArgumentSimpleFunction,OwnerFunction {
    public final String dimensionName;
    public final DimensionConfig config;
    public final DensityFunctions.TwoArgumentSimpleFunction funcOriginal;
    public final IGenerator gen;

    public DimensionData dimData = null;

    public Ap2NoiseEx(String dimensionName, DimensionConfig config, DensityFunctions.TwoArgumentSimpleFunction funcOriginal) {
        this.dimensionName = dimensionName;
        this.config = config;
        this.funcOriginal = funcOriginal;
        this.gen = createGen();
    }

    @Override
    public final double compute(FunctionContext context) {
        if (dimData == null) {
            dimData = DIMENSION_DATA_CACHE.getIfPresent(dimensionName);
            if (dimData == null)
                TbUtils.doCrash("Couldn't find DimensionData on first compute! Eh?");
        }
        return gen.onCompute(context, dimData);
    }

    @Override
    public double computeOriginal(DensityFunction.FunctionContext context) {
        double d = argument1().compute(context);
        double var10000;
        switch (type().ordinal()) {
            case 0 -> var10000 = d + argument2().compute(context);
            case 1 -> var10000 = d == 0.0 ? 0.0 : d * argument2().compute(context);
            case 2 -> var10000 = d < argument2().minValue() ? d : Math.min(d, argument2().compute(context));
            case 3 -> var10000 = d > argument2().maxValue() ? d : Math.max(d, argument2().compute(context));
            default -> throw new MatchException(null, null);
        }

        return var10000;
    }

    @Override
    public @NotNull DensityFunctions.TwoArgumentSimpleFunction.Type type() {
        return funcOriginal.type();
    }

    @Override
    public @NotNull DensityFunction argument1() {
        return funcOriginal.argument1();
    }

    @Override
    public @NotNull DensityFunction argument2() {
        return funcOriginal.argument2();
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        funcOriginal.fillArray(array, contextProvider);
    }

    @Override
    public double minValue() {
        return funcOriginal.minValue();
    }

    @Override
    public double maxValue() {
        return funcOriginal.maxValue();
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return funcOriginal.codec();
    }
}
