package github.cosmicdan.temperaturebands.waterlocator;

import github.cosmicdan.temperaturebands.DimensionData;
import org.apache.commons.lang3.tuple.Pair;

public interface IWaterLocator {
    Pair<Double, Double> findNearestOceanRiver(int startX, int startZ, DimensionData dimData);
}
