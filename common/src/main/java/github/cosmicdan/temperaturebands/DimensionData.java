package github.cosmicdan.temperaturebands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static github.cosmicdan.temperaturebands.TemperatureBands.CONFIG_DEFAULT;
import static github.cosmicdan.temperaturebands.TemperatureBands.LOGGER;

public class DimensionData {
    public final boolean isDraft;
    public final boolean isHumidityEnabled;
    private final ServerLevel level; // NOT thread safe because it's highly mutable, impossible to make it so
    private final NoiseRouter noiseRouter; // Shallowly thread-safe
    private final @Nullable DensityFunctions.HolderHolder noiseTemperature; // Shallowly thread-safe
    private final @Nullable DensityFunctions.HolderHolder noiseHumidity; // Shallowly thread-safe
    private final MultiNoiseBiomeSource biomeSource; // Probably shallowly thread-safe
    public final Set<Holder<Biome>> biomeRivers = new HashSet<>();
    public final Set<Holder<Biome>> biomeOceans = new HashSet<>();
    public static final String noneStringForBiomeDump = " - [NONE]";

    public final int humidityPartSize = TemperatureBands.configHumidityResolution * TemperatureBands.configHumidityResolution;
    public final int partSizeMiddleOffset = (int)Math.round(humidityPartSize * 0.5);

    @SuppressWarnings("LoggingSimilarMessage")
    public DimensionData(boolean isDraft, ServerLevel level, NoiseRouter noiseRouter, @Nullable DensityFunctions.HolderHolder noiseTemperature, @Nullable DensityFunctions.HolderHolder noiseHumidity) {
        this.isDraft = isDraft;
        this.level = level;
        this.noiseRouter = noiseRouter;
        this.noiseTemperature = noiseTemperature;
        this.noiseHumidity = noiseHumidity;

        if (isDraft) {
            // world is still under construction
            biomeSource = null;
            isHumidityEnabled = false;
        } else if (noiseHumidity != null) {
            // verification (only necessary for humidity)
            if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource biomeSourceInstance)) {
                biomeSource = null;
                isHumidityEnabled = false;
                LOGGER.error("Error: The dimension {} does not use a MultiNoiseBiomeSource; humidity modification cannot continue. Please report this to CosmicDan so support for this custom dimension might be added.", level.dimension().location());
            } else {
                // setup stuff for humidity
                biomeSource = biomeSourceInstance;
                isHumidityEnabled = true;
                // fetch appropriate biomes for humidity purposes
                Set<Holder<Biome>> possibleBiomes = level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
                for (Holder<Biome> biomeHolder : possibleBiomes) {
                    if (biomeHolder.is(BiomeTags.IS_RIVER)) {
                        biomeRivers.add(biomeHolder);
                        //biomeRiversAndOceans.put(biomeHolder, Boolean.TRUE);
                    } else if (biomeHolder.is(BiomeTags.IS_OCEAN)) {
                        biomeOceans.add(biomeHolder);
                        //biomeRiversAndOceans.put(biomeHolder, Boolean.TRUE);
                    }
                }
                //biomeRiversAndOceans.cleanUp();
                if (TemperatureBands.dumpRiverAndOceanBiomes) {
                    LOGGER.info("List of all biomes with 'minecraft:is_river' tag for dimension '{}':", level.dimension().location());
                    if (biomeRivers.isEmpty())
                        LOGGER.info(noneStringForBiomeDump);
                    else {
                        for (Holder<Biome> biomeHolder : biomeRivers) {
                            if (biomeHolder.unwrapKey().isPresent()) {
                                LOGGER.info(" - {}", biomeHolder.unwrapKey().get().location());
                            }
                        }
                    }
                    LOGGER.info("List of all biomes with 'minecraft:is_ocean' tag for dimension '{}':", level.dimension().location());
                    if (biomeOceans.isEmpty())
                        LOGGER.info(noneStringForBiomeDump);
                    else {
                        for (Holder<Biome> biomeHolder : biomeOceans) {
                            if (biomeHolder.unwrapKey().isPresent()) {
                                LOGGER.info(" - {}", biomeHolder.unwrapKey().get().location());
                            }
                        }
                    }
                }
            }
        } else {
            // humidity function not enabled (or not yet setup)
            biomeSource = null;
            isHumidityEnabled = false;
        }
    }

    public NoiseRouter getNoiseRouter() {
        return noiseRouter;
    }

    public DensityFunctions.HolderHolder getNoiseFunctionForName(String noiseName) {
        if (noiseName.equals(ShiftedNoiseTemperature.NAME))
            return noiseTemperature;
        else if (noiseName.equals(ShiftedNoiseHumidity.NAME))
            return noiseHumidity;
        else
            throw new RuntimeException("Attempted getting an invalid noise: " + noiseName);
    }

    @NotNull
    public DensityFunctions.HolderHolder getTemperatureFunction() {
        if (noiseTemperature == null)
            throw new RuntimeException("Tried to get TemperatureFunction but it hasn't been set setup yet, eh?");
        else
            return noiseTemperature;
    }

    public static DimensionData recreateDimDataWithNewNoiseFunction(String dimensionName, DimensionData dimData, String noiseName, DensityFunctions.HolderHolder noiseFunction) {
        if (noiseName.equals(ShiftedNoiseTemperature.NAME))
            dimData = new DimensionData(true, dimData.level, dimData.noiseRouter, noiseFunction, dimData.noiseHumidity);
        else if (noiseName.equals(ShiftedNoiseHumidity.NAME))
            dimData = new DimensionData(true, dimData.level, dimData.noiseRouter, dimData.noiseTemperature, noiseFunction);
        else
            throw new RuntimeException("Attempted recreating with invalid noise: " + noiseName);

        TemperatureBands.DIMENSION_DATA_CACHE.put(dimensionName, dimData);
        return dimData;
    }

    public static void finalizeDimData(String dimensionName, DimensionData dimData) {
        if (dimData.isDraft) {
            dimData = new DimensionData(false, dimData.level, dimData.noiseRouter, dimData.noiseTemperature, dimData.noiseHumidity);
            TemperatureBands.DIMENSION_DATA_CACHE.put(dimensionName, dimData);
        }
    }

    public MultiNoiseBiomeSource getBiomeSource() {
        return biomeSource;
    }

    public Climate.Sampler getClimateSampler() {
        return level.getChunkSource().randomState().sampler();
    }

    public static long packBlockXZtoLong(int blockX, int blockZ) {
        return (((long)blockX) << 32) | (blockZ & 0xffffffffL);
    }

    public static int unpackBlockFromLongX(long packedPos) {
        return (int) (packedPos >> 32);
    }

    public static int unpackBlockFromLongZ(long packedPos) {
        return (int) packedPos;
    }

    @Override
    public String toString() {
        return "DimensionData{" +
                "level=" + level +
                '(' + level.hashCode() + ')' +
                '}';
    }
}
