package github.cosmicdan.temperaturebands;

import github.cosmicdan.temperaturebands.densityfunctions.*;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static github.cosmicdan.temperaturebands.TemperatureBands.CONFIG_GLOBAL;
import static github.cosmicdan.temperaturebands.TemperatureBands.LOGGER;

public class DimensionData {
    public final boolean isLevelReady;
    public final DimensionConfig config;
    public final ServerLevel level;
    public final NoiseRouter noiseRouter;
    private final @Nullable DensityFunctions.HolderHolder noiseTemperature;
    private final @Nullable DensityFunctions.HolderHolder noiseHumidity;
    // humidity-related things
    public final Set<Holder<Biome>> biomeOceans = new HashSet<>();
    public final Set<Holder<Biome>> biomeRivers = new HashSet<>();

    private static final String noneStringForBiomeDump = " - [NONE]";

    @SuppressWarnings("LoggingSimilarMessage")
    public DimensionData(boolean isLevelReady, DimensionConfig config, ServerLevel level, NoiseRouter noiseRouter, @Nullable DensityFunctions.HolderHolder noiseTemperature, @Nullable DensityFunctions.HolderHolder noiseHumidity) {
        this.isLevelReady = isLevelReady;
        this.config = config;
        this.level = level;
        this.noiseRouter = noiseRouter;
        this.noiseTemperature = noiseTemperature;
        this.noiseHumidity = noiseHumidity;

        // sanity check
        if (config == null)
            TbUtils.doCrash("Tried to create new DimensionData but provided config is null, eh?");
        else if (isLevelReady) {
            // setup and verification for humidity
            if (config.humidityAlgorithm() != 0) {
                if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource)) {
                    if (!CONFIG_GLOBAL.ignoreDimensionFailures().contains(level.dimension().location().toString()))
                        LOGGER.error("Error: The dimension {} does not use a MultiNoiseBiomeSource; humidity modification cannot continue. Please report this to CosmicDan so support for this custom dimension might be added.", level.dimension().location());
                } else {
                    // fetch appropriate biomes for humidity
                    Set<Holder<Biome>> possibleBiomes = level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
                    for (Holder<Biome> biomeHolder : possibleBiomes) {
                        if (biomeHolder.is(BiomeTags.IS_RIVER)) {
                            biomeRivers.add(biomeHolder);
                        } else if (biomeHolder.is(BiomeTags.IS_OCEAN)) {
                            biomeOceans.add(biomeHolder);
                        }
                    }
                    if (TemperatureBands.CONFIG_GLOBAL.dumpRiverAndOceanBiomes()) {
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
            }
        }
    }

    public NoiseRouter getNoiseRouter() {
        return noiseRouter;
    }

    public @Nullable DensityFunctions.HolderHolder getNoiseFunctionForName(String noiseName) {
        if (noiseName.equals(OwnerFunction.TEMPERATURE_NAME))
            return noiseTemperature;
        else if (noiseName.equals(OwnerFunction.HUMIDITY_NAME))
            return noiseHumidity;
        else
            return TbUtils.doCrash("Attempted getting an invalid densityfunctions: " + noiseName);
    }

    public static @Nullable DensityFunction createModdedFunction(String functionName, DensityFunction originalFunction, String dimensionName, DimensionConfig config) {
        if (originalFunction instanceof DensityFunctions.ShiftedNoise originalFunctionShiftedNoise) {
            if (functionName.equals(OwnerFunction.TEMPERATURE_NAME))
                return new TemperatureShiftedNoise(dimensionName, config, originalFunctionShiftedNoise);
            else if (functionName.equals(OwnerFunction.HUMIDITY_NAME))
                return new HumidityShiftedNoise(dimensionName, config, originalFunctionShiftedNoise);
        } else if (originalFunction instanceof DensityFunctions.TwoArgumentSimpleFunction originalFunctionAp2) {
            if (functionName.equals(OwnerFunction.TEMPERATURE_NAME))
                return new TemperatureAp2(dimensionName, config, originalFunctionAp2);
            else if (functionName.equals(OwnerFunction.HUMIDITY_NAME))
                return new HumidityAp2(dimensionName, config, originalFunctionAp2);
        }
        return null;
    }

    public static void recreateDimDataWithNewNoiseFunction(String dimensionName, DimensionData dimData, String noiseName, DensityFunctions.HolderHolder noiseFunction) {
        if (noiseName.equals(OwnerFunction.TEMPERATURE_NAME))
            dimData = new DimensionData(false, dimData.config, dimData.level, dimData.noiseRouter, noiseFunction, dimData.noiseHumidity);
        else if (noiseName.equals(OwnerFunction.HUMIDITY_NAME))
            dimData = new DimensionData(false, dimData.config, dimData.level, dimData.noiseRouter, dimData.noiseTemperature, noiseFunction);
        else
            TbUtils.doCrash("Attempted recreating with invalid densityfunctions: " + noiseName);

        TemperatureBands.DIMENSION_DATA_CACHE.put(dimensionName, dimData);
    }

    public static void recreateDimensionWithLevelReady(String dimensionName, DimensionData dimDataToReady) {
        if (!dimDataToReady.isLevelReady) {
            DimensionData dimDataNew = new DimensionData(
                    true,
                    dimDataToReady.config,
                    dimDataToReady.level,
                    dimDataToReady.noiseRouter,
                    dimDataToReady.noiseTemperature,
                    dimDataToReady.noiseHumidity
            );
            TemperatureBands.DIMENSION_DATA_CACHE.put(dimensionName, dimDataNew);
        }
    }

    public BiomeSource getBiomeSource() {
        return level.getChunkSource().getGenerator().getBiomeSource();
    }

    public Climate.Sampler getClimateSampler() {
        return level.getChunkSource().randomState().sampler();
    }

    public @NotNull DensityFunctions.HolderHolder getTemperatureNoise() {
        if (noiseTemperature == null)
            return TbUtils.doCrash("noiseTemperature must not be null");
        return noiseTemperature;
    }

    public void clearCaches() {
        if (noiseTemperature != null) {
            if (noiseTemperature.function().value() instanceof OwnerFunction func) {
                func.cancelAllCacheTasks();
            }
        }
        if (noiseHumidity != null) {
            if (noiseHumidity.function().value() instanceof OwnerFunction func) {
                func.cancelAllCacheTasks();
            }
        }
    }

    @Override
    public String toString() {
        return "DimensionData{" +
                "level=" + level +
                '(' + level.hashCode() + ')' +
                '}';
    }
}
