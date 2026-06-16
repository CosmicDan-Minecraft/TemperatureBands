package github.cosmicdan.temperaturebands;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;

import static github.cosmicdan.temperaturebands.TemperatureBands.*;

public class DimensionDataAtomic extends DimensionData {
    private final DensityFunctions.HolderHolder temperatureNoise;
    private final DensityFunctions.HolderHolder temperatureHumidity;

    public DimensionDataAtomic(ServerLevel level, NoiseRouter noiseRouter) {
        super(level, noiseRouter);
        throw new RuntimeException("Wrong constructor for DimensionDataAtomic!");
    }

    public DimensionDataAtomic(ServerLevel level, NoiseRouter noiseRouter, DensityFunctions.HolderHolder noiseTemp, DensityFunctions.HolderHolder noiseVeg) {
        super(level, noiseRouter);
        temperatureNoise = noiseTemp;
        temperatureHumidity = noiseVeg;
    }

    @Override
    public DensityFunctions.HolderHolder getNoiseFunctionForName(String noiseName) {
        if (noiseName.equals(ShiftedNoiseTemperature.NAME))
            return temperatureNoise;
        else if (noiseName.equals(ShiftedNoiseHumidity.NAME))
            return temperatureHumidity;
        else
            throw new RuntimeException("Attempted getting an invalid noise: " + noiseName);
    }

    @Override
    public void setNoiseFunctionForName(String noiseName, DensityFunctions.HolderHolder noise) {
        throw new RuntimeException("Attempted adding new " + noiseName + " noise to DimensionDataAtomic!");
    }
}
