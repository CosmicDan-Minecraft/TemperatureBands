package github.cosmicdan.temperaturebands.climatesampler;

import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.generator.BiomeProximityGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;

public interface IClimateSampler {
    void onFirstCompute(DimensionData dimData, BiomeProximityGenerator.Config config);

    double calculateHumidity(DensityFunction.FunctionContext context, DimensionData dimData);

    double[] findNearestOceanRiver(int startX, int startZ, DimensionData dimData);
}
