package github.cosmicdan.temperaturebands;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
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
    public MultiNoiseBiomeSource biomeSource;
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
        if (config == null) {
            TbUtils.doCrash("Tried to create new DimensionData but provided config is null, eh?");
        } else if (isLevelReady && config.humidityAlgorithm() != 0) {
            // setup and verification for humidity
            biomeSource = TbUtils.findMultiNoiseBiomeSource(level, true);
            if (biomeSource != null) {
                // fetch appropriate biomes for humidity
                Set<Holder<Biome>> possibleBiomes = biomeSource.possibleBiomes();
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

    public NoiseRouter getNoiseRouter() {
        return noiseRouter;
    }

    public @Nullable DensityFunctions.HolderHolder getNoiseFunctionForName(String noiseName) {
        if (noiseName.equals(DensityFunctionEx.TEMPERATURE_NAME))
            return noiseTemperature;
        else if (noiseName.equals(DensityFunctionEx.HUMIDITY_NAME))
            return noiseHumidity;
        else
            return TbUtils.doCrash("Attempted getting an invalid densityfunctions: " + noiseName);
    }

    public static @Nullable DensityFunction createModdedFunction(String functionName, DensityFunction originalFunction, String dimensionName, DimensionConfig config, long seed) {
        if (functionName.equals(DensityFunctionEx.TEMPERATURE_NAME) || functionName.equals(DensityFunctionEx.HUMIDITY_NAME)) {
            if (config.vanillaNoiseOverride() > 0) {
                if (config.vanillaNoiseOverride() == 2 || !(originalFunction instanceof DensityFunctions.ShiftedNoise)) {
                    RandomSource vanillaRandom = new XoroshiroRandomSource(seed);
                    NormalNoise vanillaNoiseShift = NormalNoise.create(vanillaRandom, new NormalNoise.NoiseParameters(-3, List.of(1.0, 1.0, 1.0, 0.0)));
                    DensityFunction.NoiseHolder vanillaShiftShared = new DensityFunction.NoiseHolder(new Holder.Direct<>(vanillaNoiseShift.parameters()), vanillaNoiseShift);
                    NormalNoise vanillaNoise;

                    if (functionName.equals(DensityFunctionEx.TEMPERATURE_NAME))
                        vanillaNoise = NormalNoise.create(vanillaRandom, new NormalNoise.NoiseParameters(-10, List.of(1.5, 0.0, 1.0, 0.0, 0.0, 0.0)));
                    else
                        vanillaNoise = NormalNoise.create(vanillaRandom, new NormalNoise.NoiseParameters(-8, List.of(1.0, 1.0, 0.0, 0.0, 0.0, 0.0)));

                    LOGGER.info("Replaced base {} noise with vanilla-like noise (because vanillaNoiseOverride is {}{})", functionName, config.vanillaNoiseOverride(), config.vanillaNoiseOverride() == 1 ? " and original noise was type was '" + originalFunction.getClass().getSimpleName() + "'" : "");
                    originalFunction = new DensityFunctions.ShiftedNoise(
                            new DensityFunctions.ShiftA(vanillaShiftShared),
                            DensityFunctions.constant(0.0),
                            new DensityFunctions.ShiftB(vanillaShiftShared),
                            0.25,
                            0.0,
                            new DensityFunction.NoiseHolder(new Holder.Direct<>(vanillaNoise.parameters()), vanillaNoise)
                    );
                }
            }
            return new DensityFunctionEx(dimensionName, functionName, config, originalFunction);
        }
        return null;
    }

    public static void recreateDimDataWithNewNoiseFunction(String dimensionName, DimensionData dimData, String noiseName, DensityFunctions.HolderHolder noiseFunction) {
        if (noiseName.equals(DensityFunctionEx.TEMPERATURE_NAME))
            dimData = new DimensionData(false, dimData.config, dimData.level, dimData.noiseRouter, noiseFunction, dimData.noiseHumidity);
        else if (noiseName.equals(DensityFunctionEx.HUMIDITY_NAME))
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

    public MultiNoiseBiomeSource getBiomeSource() {
        if (biomeSource == null)
            // happens sometimes when C2ME + mods that modify BiomeSource (e.g. Blueprnt lib) are installed
            biomeSource = TbUtils.findMultiNoiseBiomeSource(level, false);
        return biomeSource;
    }

    public Climate.Sampler getClimateSampler() {
        return level.getChunkSource().randomState().sampler();
    }

    public @NotNull DensityFunctions.HolderHolder getTemperatureNoise() {
        if (noiseTemperature == null)
            return TbUtils.doCrash("noiseTemperature must not be null");
        return noiseTemperature;
    }

    @Override
    public String toString() {
        return "DimensionData{" +
                "level=" + level +
                '(' + level.hashCode() + ')' +
                '}';
    }
}
