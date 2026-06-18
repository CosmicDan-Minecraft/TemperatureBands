package github.cosmicdan.temperaturebands;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
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

import static github.cosmicdan.temperaturebands.TemperatureBands.LOGGER;

public class DimensionData {
    public final boolean isDraft;
    public final boolean isHumidityEnabled;
    private final ServerLevel level; // NOT thread safe because it's highly mutable, impossible to make it so
    private final NoiseRouter noiseRouter; // Shallowly thread-safe
    private final @Nullable DensityFunctions.HolderHolder noiseTemperature; // Shallowly thread-safe
    private final @Nullable DensityFunctions.HolderHolder noiseHumidity; // Shallowly thread-safe
    private final MultiNoiseBiomeSource biomeSource; // Probably shallowly thread-safe
    public final Cache<Holder<Biome>, Boolean> biomeRivers = Caffeine.newBuilder().build(); // Thread-safe
    public final Cache<Holder<Biome>, Boolean> biomeOceans = Caffeine.newBuilder().build(); // Thread-safe
    //public final Cache<Holder<Biome>, Boolean> biomeRiversAndOceans = Caffeine.newBuilder().build(); // Thread-safe

    public final int humidityPartSize = TemperatureBands.humidityResolution * TemperatureBands.humidityResolution;
    public final int partSizeMiddleOffset = (int)Math.round(humidityPartSize * 0.5);

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
            isHumidityEnabled = true;
            // verification (only necessary for humidity)
            if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource biomeSourceInstance)) {
                biomeSource = null;
                LOGGER.error("Error: The dimension {} does not use a MultiNoiseBiomeSource; humidity modification cannot continue. Please report this to CosmicDan so support for this custom dimension might be added.", level.dimension().location());
            } else {
                // setup stuff for humidity
                this.biomeSource = biomeSourceInstance;
                // fetch appropriate biomes for humidity purposes
                Set<Holder<Biome>> possibleBiomes = level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
                for (Holder<Biome> biomeHolder : possibleBiomes) {
                    if (biomeHolder.is(BiomeTags.IS_RIVER)) {
                        biomeRivers.put(biomeHolder, Boolean.TRUE);
                        //biomeRiversAndOceans.put(biomeHolder, Boolean.TRUE);
                    } else if (biomeHolder.is(BiomeTags.IS_OCEAN)) {
                        biomeOceans.put(biomeHolder, Boolean.TRUE);
                        //biomeRiversAndOceans.put(biomeHolder, Boolean.TRUE);
                    }
                }
                biomeRivers.cleanUp();
                biomeOceans.cleanUp();
                //biomeRiversAndOceans.cleanUp();
                // TODO: only do this if config option to dump river/ocean biome names is set
                LOGGER.info("List of all biomes with 'minecraft:is_river' tag:");
                for (Holder<Biome> biomeHolder : biomeRivers.asMap().keySet()) {
                    if (biomeHolder.unwrapKey().isPresent()) {
                        LOGGER.info(" - {}", biomeHolder.unwrapKey().get().location());
                    }
                }
                LOGGER.info("List of all biomes with 'minecraft:is_ocean' tag:");
                for (Holder<Biome> biomeHolder : biomeOceans.asMap().keySet()) {
                    if (biomeHolder.unwrapKey().isPresent()) {
                        LOGGER.info(" - {}", biomeHolder.unwrapKey().get().location());
                    }
                }

                /*
                // TEMP: River/Ocean biome dump
                // TODO: Just do this manual work, but then do biome finding with original *and* new "simplified" version, and compare results (see if they're accurate)
                Map<Holder<Biome>, Set<Climate.ParameterPoint>> biomePoints = new HashMap<>();
                List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomeParams = ((MultiNoiseBiomeSourceInvoker) getBiomeSource()).getParameters().values();
                for (Pair<Climate.ParameterPoint, Holder<Biome>> paramEntry : biomeParams) {
                    if (paramEntry.getSecond().is(BiomeTags.IS_RIVER) || paramEntry.getSecond().is(BiomeTags.IS_OCEAN)) {
                        Climate.ParameterPoint paramPoint = paramEntry.getFirst();
                        Holder<Biome> biome = paramEntry.getSecond();
                        Set<Climate.ParameterPoint> biomeEntry = biomePoints.get(biome);
                        if (biomeEntry == null) {
                            biomeEntry = new HashSet<>();
                            biomePoints.put(biome, biomeEntry);
                        }
                        biomeEntry.add(paramPoint);
                    }
                }
                LOGGER.info("~~~ START DUMP OF biomePoints");
                for (Map.Entry<Holder<Biome>, Set<Climate.ParameterPoint>> entry : biomePoints.entrySet()) {
                    LOGGER.info("{}:", entry.getKey().unwrapKey().get().location());
                    for (Climate.ParameterPoint value : entry.getValue()) {
                        LOGGER.info("    {}", value.toString());
                    }
                }
                LOGGER.info("~~~ END DUMP");
                 */
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
