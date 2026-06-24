package github.cosmicdan.temperaturebands.generator;

import github.cosmicdan.temperaturebands.DimensionData;
import net.minecraft.world.level.levelgen.DensityFunction;

public interface IGenerator {
    double onCompute(DensityFunction.FunctionContext context, DimensionData dimData);
    void cancelAllCacheTasks();
}
