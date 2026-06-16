package github.cosmicdan.temperaturebands;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;

public class DimensionDataDraft extends DimensionData {
    private DensityFunctions.HolderHolder temperatureNoise;
    private DensityFunctions.HolderHolder temperatureHumidity;

    public DimensionDataDraft(ServerLevel level, NoiseRouter noiseRouter) {
        super(level, noiseRouter);
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
        if (noiseName.equals(ShiftedNoiseTemperature.NAME))
            temperatureNoise = noise;
        else if (noiseName.equals(ShiftedNoiseHumidity.NAME))
            temperatureHumidity = noise;
        else
            throw new RuntimeException("Attempted setting an invalid noise: " + noiseName);
    }




    /*
    @Override
    public DensityFunctions.HolderHolder getNoiseFunctionForClass(Class<ShiftedNoiseEx> desiredFunctionClass) {
        switch (desiredFunctionClass) {
            case ShiftedNoiseTemperature.class:
                return temperatureNoise;
                break;

        }

        return temperatureNoise;
    }

    @Override
    public void setTemperatureNoise(DensityFunctions.HolderHolder noise) {
        if (temperatureNoise == null) {
            temperatureNoise = noise;
            TemperatureBands.LOGGER.info("~~~ Got new ShiftedNoiseTemperature. Class dump:");
            TemperatureBands.LOGGER.info("~~~ {}", noise.toString());
        } else {
            TemperatureBands.LOGGER.info("~~~ Got *replacement* ShiftedNoiseTemperature. Class dump:");
            TemperatureBands.LOGGER.info("~~~ {}", noise.toString());
        }
    }
     */
}
