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

public abstract class MarkedNoiseEx implements DensityFunction,OwnerFunction {
    public final String dimensionName;
    public final DimensionConfig config;
    public final DensityFunction funcOriginal;
    public final IGenerator gen;

    public DimensionData dimData = null;

    public MarkedNoiseEx(String dimensionName, DimensionConfig config, DensityFunction funcOriginal) {
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
    public final double computeOriginal(DensityFunction.FunctionContext context) {
        return funcOriginal.compute(context);
    }

    @Override
    public final void fillArray(double[] ds, ContextProvider contextProvider) {
        funcOriginal.fillArray(ds, contextProvider);
    }

    @Override
    public final double minValue() {
        return funcOriginal.minValue();
    }

    @Override
    public final double maxValue() {
        return funcOriginal.maxValue();
    }

    @Override
    public final @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return funcOriginal.codec();
    }
}
