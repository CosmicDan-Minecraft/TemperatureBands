package github.cosmicdan.temperaturebands;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;

public abstract class DimensionData {
    private final ServerLevel level;
    private final NoiseRouter noiseRouter;

    public DimensionData(ServerLevel level, NoiseRouter noiseRouter) {
        this.level = level;
        this.noiseRouter = noiseRouter;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public NoiseRouter getNoiseRouter() {
        return noiseRouter;
    }

    public abstract DensityFunctions.HolderHolder getNoiseFunctionForName(String noiseName);
    public abstract void setNoiseFunctionForName(String noiseName, DensityFunctions.HolderHolder noise);

    @Override
    public String toString() {
        return "DimensionData{" +
                "level=" + level +
                '(' + level.hashCode() + ')' +
                '}';
    }
}
