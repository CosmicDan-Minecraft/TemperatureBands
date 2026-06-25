package github.cosmicdan.temperaturebands.densityfunctions;

import github.cosmicdan.temperaturebands.DimensionConfig;
import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.TemperatureBands;
import github.cosmicdan.temperaturebands.generator.IGenerator;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static github.cosmicdan.temperaturebands.TemperatureBands.DIMENSION_DATA_CACHE;

public abstract class ShiftedNoiseEx implements DensityFunction,OwnerFunction {
    public final String dimensionName;
    public final DimensionConfig config;
    public final DensityFunctions.ShiftedNoise funcOriginal;
    public final IGenerator gen;

    public DimensionData dimData = null;

    public ShiftedNoiseEx(String dimensionName, DimensionConfig config, DensityFunctions.ShiftedNoise funcOriginal) {
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
        double d = context.blockX() * funcOriginal.xzScale() + funcOriginal.shiftX().compute(context);
        double e = context.blockY() * funcOriginal.yScale() + funcOriginal.shiftY().compute(context);
        double f = context.blockZ() * funcOriginal.xzScale() + funcOriginal.shiftZ().compute(context);
        return funcOriginal.noise().getValue(d, e, f);
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
        return funcOriginal.noise().maxValue();
    }

    @Override
    public final @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return funcOriginal.codec();
    }
}
