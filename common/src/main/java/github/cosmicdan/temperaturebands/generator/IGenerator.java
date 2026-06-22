package github.cosmicdan.temperaturebands.generator;

import github.cosmicdan.temperaturebands.DimensionData;
import net.minecraft.world.level.levelgen.DensityFunction;

public interface IGenerator {
    double compute(DensityFunction.FunctionContext context, DimensionData dimData);
    double computeOriginal(DensityFunction.FunctionContext context);
    void cancelAllCacheTasks();
}
