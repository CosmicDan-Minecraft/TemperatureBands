package github.cosmicdan.temperaturebands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class TemperatureBands {
    public static final String MOD_ID = "temperaturebands";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static IModPlatform MODPLATFORM;

    public static CommonConfig CONFIG_DEFAULT = null;

    /** Used to "remember" some things for each dimension so we can access it later from various places. Entries are created in ChunkMapHooks, added to by various other hooks, and cleared on shutdown via MinecraftServerHooks */
    public static final Cache<String, DimensionData> DIMENSION_DATA_CACHE = Caffeine.newBuilder().build();

    public static boolean isDeleteScreenActive = false;

    public TemperatureBands(final IModPlatform modPlatform) {
        MODPLATFORM = modPlatform;
        // Register common config
        final Pair<CommonConfig, ModConfigSpec> specPairConfigCommon = new ModConfigSpec.Builder().configure(CommonConfig::new);
        CONFIG_DEFAULT = specPairConfigCommon.getLeft();
        MODPLATFORM.registerConfigCommon(specPairConfigCommon.getRight());
    }

    private static Path saveDir;
    private static Path saveDirPrev;
    private static File savePropFile;
    private static File savePropFilePrev;
    private static boolean configDone = false;
    private static boolean configForRecreateDone = false;

    public static void onLevelStorageLoad(Path saveDirIn) {
        if (saveDir != null) {
            // we're recreating a world from another world
            saveDirPrev = saveDir;
            savePropFilePrev = savePropFile;
            configForRecreateDone = false;
        }
        saveDir = saveDirIn;
        savePropFile = saveDir.resolve(MOD_ID + ".prop").toFile();
        doModConfigIfNeeded();
    }

    public static void clearDimensionDataAndConfig() {
        saveDir = null;
        saveDirPrev = null;
        savePropFile = null;
        savePropFilePrev = null;
        configDimBlacklist.clear();
        configDone = false;
        configForRecreateDone = false;
    }

    // config values (initial values shouldn't matter, they're loaded from mod or world config)
    // Global settings (not per-world)
    public static boolean configDoBenchmark = false;
    public static boolean copyConfigOnRecreateWorld = true;
    public static Set<String> ignoreDimensionFailures = new HashSet<>();
    public static boolean dumpRiverAndOceanBiomes = false;
    // Global climate sampler settings (not per-world)
    public static int configClimateSamplerCacheSize = 5;
    public static int configClimateSamplerCachePrefetchRadius = -8;

    // Base world config
    static int configBandSize = 2048;
    static boolean configUseVerticalBands = false;
    static float configPosShift = 0.25f;
    static float configTempRange = 0.60f;
    static float configGradeShift = -0.05f;
    static int configAlgorithm = 1;
    static int configNoiseFactor = 100;
    static int configDistanceFunction = 1;
    // Whitelist/blacklist config
    public static Set<String> configDimBlacklist = new HashSet<>();
    public static boolean configDimBlacklistAsWhitelist = false;
    // Temperature algo1 config
    static int configAlgo1BandVariance = 128;
    static float configAlgo1bandVarianceSteepness = 0.1f;
    // Humidity config
    public static int configHumidityAlgorithm = 2; // TODO, simple algorithm (value 1)
    // Humidity algo 1 (simple) config
    public static boolean configHumidityAlgo1MimicTemp = true; // TODO, simple algorithm
    public static float configHumidityAlgo1MimicScale = 0.5f; // TODO, simple algorithm
    // Humidity algo 2 (advanced) config
    public static int configHumidityResolution = 4;
    public static float configHumidityRiverInfluence = 0.4f;
    public static int configHumiditySearchDistance = 512;
    //public static float humidityRange = 2.0f; // Vanilla clamps humidity between -1.0 and +1.0. If you want that same clamping range, set this to 2.0. The default of 1.6 means it will be clamped to between -0.8 and +0.8 which takes
    //public static float humidityNoiseFactor = 0.5f;
    //public static float humidityCenterWeight = 3.0f; // The "weightiness" towards middle values for humidity. Vanilla humidity generation tends to favour values closer to the middle, so this is used reduce the amount of humidity extremes (e.g. too much Jungle and Savanna). The default value seems good to me.
    // Climate sampler settings (world-specific)
    public static int configClimateSamplerResolution = -1;

    public static void doModConfigGlobal() {
        // global config (not per-world) is always read from mod config
        configDoBenchmark = TemperatureBands.CONFIG_DEFAULT.doBenchmark.get();
        TbUtils.convertCsvToSet(TemperatureBands.CONFIG_DEFAULT.ignoreDimensionFailures.get(), ignoreDimensionFailures);
        dumpRiverAndOceanBiomes = TemperatureBands.CONFIG_DEFAULT.dumpRiverAndOceanBiomes.get();
        configClimateSamplerCacheSize = TemperatureBands.CONFIG_DEFAULT.climateSamplerCacheSize.get();
        configClimateSamplerCachePrefetchRadius = TemperatureBands.CONFIG_DEFAULT.climateSamplerCachePrefetchRadius.get();
        if (configClimateSamplerCachePrefetchRadius < 0) {
            int threadCount = Runtime.getRuntime().availableProcessors();
            int originalSetting = configClimateSamplerCachePrefetchRadius;
            configClimateSamplerCachePrefetchRadius = threadCount / -configClimateSamplerCachePrefetchRadius;
            if (configClimateSamplerCachePrefetchRadius < 1) {
                LOGGER.info("Disabling climate sampler prefetching due to not enough threads (i.e. from {}/{}, based on {} CPU threads available)", -originalSetting, threadCount, threadCount);
                configClimateSamplerCachePrefetchRadius = 0;
            } else
                LOGGER.info("Using radius of {} for climate sampler prefetching (from {}/{}, based on {} CPU threads available)", configClimateSamplerCachePrefetchRadius, -originalSetting, threadCount, threadCount);
        }
    }

    private static void saveWorldSpecificConfig(File propFileOut, boolean isRecreate) {
        try (FileOutputStream output = new FileOutputStream(propFileOut)) {
            Properties prop = setConfigPropsForSaving();
            prop.store(output, "World-specific Temperature Bands settings. Do not edit!");
            if (isRecreate) {
                LOGGER.info("Copied world-specific config from {}/{} to {}", savePropFilePrev.getParentFile().getName(), savePropFilePrev.getName(), propFileOut.getParentFile().getName());
            } else {
                LOGGER.info("Saved world-specific config to {}/{}", propFileOut.getParentFile().getName(), propFileOut.getName());
            }
        } catch (IOException ex) {
            for (StackTraceElement element : ex.getStackTrace()) {
                LOGGER.error(element.toString());
            }
            throw new RuntimeException("Error writing world-specific config. Crashing-out intentionally to prevent world corruption. Error details are above.");
        }
    }

    public static void loadDefaultConfig(File propFileOut, boolean isRecreatingWorld) {
        // get and calc values from config
        if (isRecreatingWorld)
            LOGGER.info("Loading default config because 'copyConfigOnRecreateWorld' is false (i.e. world is being 'upgraded')");
        else
            LOGGER.info("Loading default config (creating a new world)");
        configBandSize = TemperatureBands.CONFIG_DEFAULT.bandSize.get();
        configUseVerticalBands = TemperatureBands.CONFIG_DEFAULT.useVerticalBands.get();
        configPosShift = TemperatureBands.CONFIG_DEFAULT.bandPositionShift.get().floatValue();
        configTempRange = TemperatureBands.CONFIG_DEFAULT.tempRange.get().floatValue();
        configGradeShift = TemperatureBands.CONFIG_DEFAULT.tempGradeShift.get().floatValue();
        configAlgorithm = TemperatureBands.CONFIG_DEFAULT.bandAlgorithm.get();
        configNoiseFactor = TemperatureBands.CONFIG_DEFAULT.noiseFactor.get();
        // Whitelist/blacklist
        configDimBlacklistAsWhitelist = TemperatureBands.CONFIG_DEFAULT.dimBlacklistAsWhitelist.get();
        configDistanceFunction = TemperatureBands.CONFIG_DEFAULT.distanceFunction.get();
        String dimBlacklist = TemperatureBands.CONFIG_DEFAULT.dimBlacklist.get();
        setDimBlacklist(dimBlacklist, configDimBlacklist);
        // Temp algo 1
        if (configAlgorithm == 1) {
            configAlgo1BandVariance = TemperatureBands.CONFIG_DEFAULT.algo1bandVariance.get();
            configAlgo1bandVarianceSteepness = TemperatureBands.CONFIG_DEFAULT.algo1bandVarianceSteepness.get().floatValue();
        } else {
            throw new RuntimeException("Unhandled algorithm at config load, fixme!");
        }
        // Humidity
        configHumidityAlgorithm = TemperatureBands.CONFIG_DEFAULT.humidityAlgorithm.get();
        if (configHumidityAlgorithm == 1) {
            // Humidity algo 1 (simple)
            configHumidityAlgo1MimicTemp = TemperatureBands.CONFIG_DEFAULT.humidityAlgo1MimicTemp.get();
            configHumidityAlgo1MimicScale = TemperatureBands.CONFIG_DEFAULT.humidityAlgo1MimicScale.get().floatValue();
        } else if (configHumidityAlgorithm == 2) {
            // Humidity algo 2 (advanced)
            configHumidityResolution = TemperatureBands.CONFIG_DEFAULT.humidityResolution.get();
            configHumidityRiverInfluence = TemperatureBands.CONFIG_DEFAULT.humidityRiverInfluence.get().floatValue();
            configHumiditySearchDistance = TemperatureBands.CONFIG_DEFAULT.humiditySearchDistance.get();
        } else if (configHumidityAlgorithm != 0) {
            throw new RuntimeException("Unhandled humidity algorithm at config load, fixme!");
        }
        configClimateSamplerResolution = TemperatureBands.CONFIG_DEFAULT.climateSamplerResolution.get();

        saveWorldSpecificConfig(propFileOut, false);
    }

    public static void doModConfigIfNeeded() {
        if (saveDirPrev != null && !configForRecreateDone) {
            // we're recreating a world from another
            doModConfigGlobal();
            if (copyConfigOnRecreateWorld)
                saveWorldSpecificConfig(savePropFile, true);
            else
                loadDefaultConfig(savePropFile, true);
            configForRecreateDone = true;
        } else if (!configDone) {
            doModConfigGlobal();
            if (savePropFile != null && savePropFile.exists()) {
                // world prop exists, use world config
                LOGGER.info("Loading world-specific config from {}/{}", savePropFile.getParentFile().getName(), savePropFile.getName());
                try (FileInputStream input = new FileInputStream(savePropFile)) {
                    Properties prop = new Properties();
                    prop.load(input);
                    configBandSize = Integer.parseInt(prop.getProperty(CommonConfig.bandSizeName));
                    configUseVerticalBands = Boolean.parseBoolean(prop.getProperty(CommonConfig.useVerticalBandsName));
                    configPosShift = Float.parseFloat(prop.getProperty(CommonConfig.bandPositionShiftName));
                    configTempRange = Float.parseFloat(prop.getProperty(CommonConfig.tempRangeName));
                    configGradeShift = Float.parseFloat(prop.getProperty(CommonConfig.tempGradeShiftName));
                    configAlgorithm = Integer.parseInt(prop.getProperty(CommonConfig.bandAlgorithmName));
                    configNoiseFactor = Integer.parseInt(prop.getProperty(CommonConfig.noiseFactorName));
                    configDistanceFunction = Integer.parseInt(prop.getProperty(CommonConfig.distanceFunctionName, "1"));
                    // Whitelist/blacklist
                    String dimBlacklist = prop.getProperty(CommonConfig.dimBlacklistName, "");
                    setDimBlacklist(dimBlacklist, configDimBlacklist);
                    configDimBlacklistAsWhitelist = Boolean.parseBoolean(prop.getProperty(CommonConfig.dimBlacklistAsWhitelistName));
                    // Temp algo 1
                    if (configAlgorithm == 1) {
                        configAlgo1BandVariance = Integer.parseInt(prop.getProperty(CommonConfig.algo1bandVarianceName));
                        configAlgo1bandVarianceSteepness = Float.parseFloat(prop.getProperty(CommonConfig.algo1bandVarianceSteepnessName));
                    } else {
                        throw new RuntimeException("World config has unsupported temperature algorithm (" + configAlgorithm + "), did you downgrade Temperature Bands?");
                    }
                    // Humidity
                    configHumidityAlgorithm = Integer.parseInt(prop.getProperty(CommonConfig.humidityAlgorithmName, "0")); // default must be 0 to preserve existing worlds
                    if (configHumidityAlgorithm == 1) {
                        // Humidity algo 1 (simple)
                        configHumidityAlgo1MimicTemp = Boolean.parseBoolean(prop.getProperty(CommonConfig.humidityAlgo1MimicTempName));
                        configHumidityAlgo1MimicScale = Float.parseFloat(prop.getProperty(CommonConfig.humidityAlgo1MimicScaleName));
                    } else if (configHumidityAlgorithm == 2) {
                        // Humidity algo 2 (advanced)
                        configHumidityResolution = Integer.parseInt(prop.getProperty(CommonConfig.humidityResolutionName));
                        configHumidityRiverInfluence = Float.parseFloat(prop.getProperty(CommonConfig.humidityRiverInfluenceName));
                        configHumiditySearchDistance = Integer.parseInt(prop.getProperty(CommonConfig.humiditySearchDistanceName));
                    } else if (configHumidityAlgorithm != 0) {
                        throw new RuntimeException("World config has unsupported humidity algorithm (" + configHumidityAlgorithm + "), did you downgrade Temperature Bands?");
                    }
                    // Climate sampler (world-specific)
                    configClimateSamplerResolution = Integer.parseInt(prop.getProperty(CommonConfig.climateSamplerResolutionName, "-1"));

                } catch (IOException ex) {
                    throw new RuntimeException("Error reading world config. Crashing-out intentionally to prevent corruption. If you modified the world config manually, please fix it. Otherwise, report this Temperature Bands error.");
                }
            } else if (saveDir != null && saveDir.toFile().exists()) {
                loadDefaultConfig(savePropFile, false);
            } else {
                // should never happen
                try {
                    throw new RuntimeException("Error while trying to load default config or saving world-specific config. Temperature Bands probably won't work right.");
                } catch (RuntimeException ex) {
                    LOGGER.error(ex.getMessage());
                    for (StackTraceElement element : ex.getStackTrace()) {
                        LOGGER.error(element.toString());
                    }
                }
            }
            configDone = true;
        }
    }

    private static void setDimBlacklist(String dimBlacklistIn, Set<String> dimBlackListOut) {
        TbUtils.convertCsvToSet(dimBlacklistIn, dimBlackListOut);
        if (!dimBlackListOut.isEmpty())
            LOGGER.info("Applying dimension {}: {}", configDimBlacklistAsWhitelist ? "whitelist" : "blacklist", dimBlacklistIn);
        else
            LOGGER.info("Dimension {} is empty for this world", configDimBlacklistAsWhitelist ? "whitelist" : "blacklist");
    }

    private static @NotNull Properties setConfigPropsForSaving() {
        Properties prop = new Properties();
        prop.setProperty(CommonConfig.bandSizeName, String.valueOf(configBandSize));
        prop.setProperty(CommonConfig.useVerticalBandsName, String.valueOf(configUseVerticalBands));
        prop.setProperty(CommonConfig.bandPositionShiftName, String.valueOf(configPosShift));
        prop.setProperty(CommonConfig.tempRangeName, String.valueOf(configTempRange));
        prop.setProperty(CommonConfig.tempGradeShiftName, String.valueOf(configGradeShift));
        prop.setProperty(CommonConfig.bandAlgorithmName, String.valueOf(configAlgorithm));
        prop.setProperty(CommonConfig.noiseFactorName, String.valueOf(configNoiseFactor));
        prop.setProperty(CommonConfig.distanceFunctionName, String.valueOf(configDistanceFunction));
        // Whitelist/blacklist

        prop.setProperty(CommonConfig.dimBlacklistName, TbUtils.convertSetToCsv(configDimBlacklist));
        prop.setProperty(CommonConfig.dimBlacklistAsWhitelistName, String.valueOf(configDimBlacklistAsWhitelist));
        // Temp algo 1
        if (configAlgorithm == 1) {
            prop.setProperty(CommonConfig.algo1bandVarianceName, String.valueOf(configAlgo1BandVariance));
            prop.setProperty(CommonConfig.algo1bandVarianceSteepnessName, String.valueOf(configAlgo1bandVarianceSteepness));
        } else {
            throw new RuntimeException("Unhandled algorithm at config world save, fixme!");
        }
        // Humidity
        prop.setProperty(CommonConfig.humidityAlgorithmName, String.valueOf(configHumidityAlgorithm));
        if (configHumidityAlgorithm == 1) {
            // Humidity algo 1 (simple)
            prop.setProperty(CommonConfig.humidityAlgo1MimicTempName, String.valueOf(configHumidityAlgo1MimicTemp));
            prop.setProperty(CommonConfig.humidityAlgo1MimicScaleName, String.valueOf(configHumidityAlgo1MimicScale));
        } else if (configHumidityAlgorithm == 2) {
            // Humidity algo 2 (advanced)
            prop.setProperty(CommonConfig.humidityResolutionName, String.valueOf(configHumidityResolution));
            prop.setProperty(CommonConfig.humidityRiverInfluenceName, String.valueOf(configHumidityRiverInfluence));
            prop.setProperty(CommonConfig.humiditySearchDistanceName, String.valueOf(configHumiditySearchDistance));
        } else if (configHumidityAlgorithm != 0) {
            throw new RuntimeException("Unhandled humidity algorithm at config world save, fixme!");
        }
        prop.setProperty(CommonConfig.climateSamplerResolutionName, String.valueOf(configClimateSamplerResolution));
        return prop;
    }

    public static boolean isDimensionWhitelisted(String dimensionName) {
        boolean isWhitelistedDim = false;
        if (TemperatureBands.configDimBlacklistAsWhitelist) {
            if (TemperatureBands.configDimBlacklist.contains(dimensionName))
                isWhitelistedDim = true;
        } else if (!TemperatureBands.configDimBlacklist.contains(dimensionName))
            isWhitelistedDim = true;
        return isWhitelistedDim;
    }

    private static final Map<String, Set<String>> failedDimensionNoiseReplacements = new HashMap<>();

    /**
     * WARNING - The passed-in activeDimData might be destroyed upon exit of this method.
     */
    public static DensityFunction replaceNoiseIfNeeded(DimensionData activeDimData, String dimensionName, DensityFunction currentFunction, String functionName) {
        // first check if we already made modded function, return it if so
        DensityFunctions.HolderHolder dimDataModdedFunction = activeDimData.getNoiseFunctionForName(functionName);
        if (dimDataModdedFunction != null) {
            // We already made the modded function for this dimension, reuse it
            currentFunction = dimDataModdedFunction;
            //TemperatureBands.LOGGER.info("...returned already-replaced function");
        } else if (currentFunction instanceof DensityFunctions.HolderHolder currentFunctionHolder && currentFunctionHolder.function().value() instanceof DensityFunctions.ShiftedNoise currentFunctionActual) {
            // Vanilla noise that we want to replace
            ShiftedNoiseEx newNoise = null;
            if (functionName.equals(ShiftedNoiseTemperature.NAME))
                newNoise = new ShiftedNoiseTemperature(dimensionName, currentFunctionActual.shiftX(), currentFunctionActual.shiftY(), currentFunctionActual.shiftZ(), currentFunctionActual.xzScale(), currentFunctionActual.yScale(), currentFunctionActual.noise());
            else if (functionName.equals(ShiftedNoiseHumidity.NAME))
                newNoise = new ShiftedNoiseHumidity(dimensionName, currentFunctionActual.shiftX(), currentFunctionActual.shiftY(), currentFunctionActual.shiftZ(), currentFunctionActual.xzScale(), currentFunctionActual.yScale(), currentFunctionActual.noise());
            else
                throw new RuntimeException("Unhandled noise type: " + functionName);
            DensityFunctions.HolderHolder newFunction = new DensityFunctions.HolderHolder(new Holder.Direct<>(newNoise));
            currentFunction = new DensityFunctions.HolderHolder(new Holder.Direct<>(newFunction));
            DimensionData.recreateDimDataWithNewNoiseFunction(dimensionName, activeDimData, functionName, (DensityFunctions.HolderHolder) currentFunction);
            TemperatureBands.LOGGER.info("Succeeded in hooking {} for dimension '{}'", functionName, dimensionName);
        } else {
            if (ignoreDimensionFailures.contains(dimensionName))
                return currentFunction;
            Set<String> failedDimensionEntry = failedDimensionNoiseReplacements.computeIfAbsent(dimensionName, k -> new HashSet<>());
            if (!failedDimensionEntry.contains(functionName)) {
                TemperatureBands.LOGGER.error("Failed hooking {} for dimension '{}' because it is not a Holder of ShiftedNoise type. Please report this to CosmicDan so support for this custom dimension might be added.", functionName, dimensionName);
                failedDimensionEntry.add(functionName);
                dumpExtraClassInfo(currentFunction);
            }
        }
        return currentFunction;
    }

    public static Map.Entry<String, @NonNull DimensionData> findDimDataViaNoiseRouter(NoiseRouter noiseRouter) {
        String dimensionName = null;
        DimensionData activeDimData = null;
        for (Map.Entry<String, @NonNull DimensionData> dimDataEntry : TemperatureBands.DIMENSION_DATA_CACHE.asMap().entrySet()) {
            if (dimDataEntry.getValue().getNoiseRouter().equals(noiseRouter)) {
                return dimDataEntry;
            }
        }
        return null;
    }

    public static double calculateBlockDistance(int startX, int endX, int startZ, int endZ) {
        if (configDistanceFunction == 0) {
            // Manhattan distance
            return Math.abs(startX - endX) + Math.abs(startZ - endZ);
        } else if (configDistanceFunction == 2) {
            // Pure (hypotenuse)
            return Math.hypot(Math.abs(startX - endX), Math.abs(startZ - endZ));
        } else {
            // Octile (diagonal)
            long distanceX = Math.abs(startX - endX);
            long distanceZ = Math.abs(startZ - endZ);
            double straightCost = 1.0;
            double diagonalCost = Math.sqrt(2);

            if (distanceX > distanceZ) {
                return ((diagonalCost * distanceZ) + (straightCost * (distanceX - distanceZ)));
            } else {
                return ((diagonalCost * distanceX) + (straightCost * (distanceZ - distanceX)));
            }
        }
    }

    private static void dumpExtraClassInfo(DensityFunction func) {
        LOGGER.error("    - Class type is '{}'", func.getClass().getCanonicalName());
        LOGGER.error("    - Class dump: {}", func.toString());
    }

    public static int batchesTotal = 0;
    public static int batchesDone = -1;
    private static Instant benchmarkStart;

    public static void benchmarkReset() {
        if (batchesTotal > 0) {
            LOGGER.info("WorldPreview benchmark cancelled");
        }
        batchesTotal = 0;
        batchesDone = -1;
        benchmarkStart = Instant.now();
    }

    public static void benchmarkStart(int batchesSize) {
        LOGGER.info("Starting WorldPreview benchmark, waiting for {} batches to finish...", batchesSize);
        benchmarkStart = Instant.now();
        batchesTotal = batchesSize;
        batchesDone = 0;
    }

    public static void benchmarkBatchDone() {
        batchesDone++;
        if (batchesDone >= batchesTotal) {
            LOGGER.info("WorldPreview benchmark finished, {} batches took {} seconds." , batchesTotal, String.format("%.2f", Duration.between(benchmarkStart, Instant.now()).abs().toMillis() / 1000.0));
            batchesTotal = 0;
            batchesDone = -1;
        }
    }
}
