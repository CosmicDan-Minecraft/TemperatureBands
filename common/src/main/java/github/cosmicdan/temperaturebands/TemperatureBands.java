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


    public TemperatureBands(final IModPlatform modPlatform) {
        MODPLATFORM = modPlatform;
        // Register common config
        final Pair<CommonConfig, ModConfigSpec> specPairConfigCommon = new ModConfigSpec.Builder().configure(CommonConfig::new);
        CONFIG_DEFAULT = specPairConfigCommon.getLeft();
        MODPLATFORM.registerConfigCommon(specPairConfigCommon.getRight());
    }

    private static Path saveDir;
    private static File savePropFile;
    private static boolean configDone = false;

    public static void onLevelStorageLoad(Path saveDirIn) {
        resetSaveAndConfig();
        saveDir = saveDirIn;
        savePropFile = saveDir.resolve(MOD_ID + ".prop").toFile();
        doModConfigIfNeeded();
    }

    public static void resetSaveAndConfig() {
        saveDir = null;
        savePropFile = null;
        configDimBlacklist.clear();
        configDone = false;
    }

    // config values (initial values shouldn't matter, they're loaded from mod or world config)
    static int configBandSize = 2048;
    static boolean configUseVerticalBands = false;
    static float configPosShift = 0.25f;
    static float configTempRange = 0.60f;
    static float configGradeShift = -0.05f;
    static int configAlgorithm = 1;
    static int configNoiseFactor = 100;
    static int configDistanceFunction = 1; // Distance function to use for various calculations. The default of 1 uses "Octile" distance which is quite fast and accurate. 0 uses "Manhattan" (or "Taxi Cab") distance which is slightly faster but quite inaccurate. 2 uses pure distance via hypotenuse calculation, which is a bit slow but provides maximum accuracy. Recommended to leave on 1 (which will be default if an invalid number is specified).
    // blacklist config
    public static Set<String> configDimBlacklist = new HashSet<>();
    public static boolean configDimBlacklistAsWhitelist = false;
    // humidity config (TODO)
    // 6 is pretty fast
    public static int humidityResolution = 4; // Lower values mean higher accuracy or "resolution" for calculating the distance from ocean/river for a given area, represented as a square root (i.e. default of 8 means each 64x64 area will use the same distance values). Values lower than 8 start to become extremely expensive on CPU/worldgen time, the "chunkyness" of the default value of 8 is significantly reduced by humidityNoiseFactor. Note that a value of 4 will effectively disable the cache, size 4 is equal to chunk size.
    public static int humiditySearchDistance = 512; // The upper distance in blocks on XZ (horizontal) axis from ocean/rivers to be considered as maximum "dryness" (lower humidity). Meaning, higher numbers will make humidity drop slower as distance increases from ocean/river biomes. Higher values will be a little more expensive on CPU/worldgen time.
    //public static float humidityRange = 2.0f; // Vanilla clamps humidity between -1.0 and +1.0. If you want that same clamping range, set this to 2.0. The default of 1.6 means it will be clamped to between -0.8 and +0.8 which takes
    public static float humidityRiverInfluence = 0.4f; // How much rivers should contribute to final humidity value. I.e. default of 0.2 means 20% of river closeness will be added to the initial humidity calculated from ocean distance (which might be the lowest possible value). Setting to 0 will disable any river influence on humidity (and give a small speed boost). Higher values will make biome placement very noisy, i.e. lots of tiny scattered "dots" of biomes.
    public static float humidityNoiseFactor = 0.5f;
    public static float humidityCenterWeight = 3.0f; // The "weightiness" towards middle values for humidity. Vanilla humidity generation tends to favour values closer to the middle, so this is used reduce the amount of humidity extremes (e.g. too much Jungle and Savanna). The default value seems good to me.

    public static int climateSamplerResolution = -1; // Lower values mean higher accuracy or "resolution" for sampling the climate (used for humidity searching). The default of -1 means automatic, which is the cube of humidityResolution (seems to be the most logical choice - biome edges look natural with sporadic patches and performance is still OK).
    public static int climateSamplerCacheSize = 1000000; // Cache size of Climate Sampler. Measured in number of points, not actual data size. The default of 1 million can reach a maximum of about 80Mb memory usage (when full). Setting to zero will disable caching, which may result in better performance if your memory is slow or exhausted.
    public static int climateSamplerCachePrefetchRadius = 0; // If above zero, whenever climate sampling is needed, additional climate sampling (and caching) will be done asynchronously in this radius (as units of "climateSamplerResolution").
    public static int climateSamplerCacheExpirySeconds = 30; // Lifetime of cached Climate Sampler entries in seconds. Setting to zero will cause cache entries to expire immediately after one read. Setting to -1 will make cache entries last as long as they can until cache is full (where oldest cache entries will be replaced). Recommended to leave on default.

    public static boolean configDoBenchmark = true;


    // algo1 config
    static int configAlgo1BandVariance = 128;
    static float configAlgo1bandVarianceSteepness = 0.1f;
    // values derived/calculated from config
    static float tempBandMid = 0f;
    static float tempGradeAdj = 0f;
    static int tempAlgo1bandVarianceMid = 0;
    public static void doModConfigIfNeeded() {
        if (!configDone) {
            // check for world prop
            if (savePropFile != null && savePropFile.exists()) {
                // world prop exists, use world config
                LOGGER.info("Using world-specific config");
                try (FileInputStream input = new FileInputStream(savePropFile)) {
                    Properties prop = new Properties();
                    prop.load(input);
                    configBandSize = Integer.parseInt(prop.getProperty("configBandSize"));
                    configUseVerticalBands = Boolean.parseBoolean(prop.getProperty("configUseVerticalBands"));
                    configPosShift = Float.parseFloat(prop.getProperty("configPosShift"));
                    configTempRange = Float.parseFloat(prop.getProperty("configTempRange"));
                    configGradeShift = Float.parseFloat(prop.getProperty("configGradeShift"));
                    configAlgorithm = Integer.parseInt(prop.getProperty("configAlgorithm"));
                    configNoiseFactor = Integer.parseInt(prop.getProperty("configNoiseFactor"));
                    if (configAlgorithm == 1) {
                        configAlgo1BandVariance = Integer.parseInt(prop.getProperty("configAlgo1BandVariance"));
                        configAlgo1bandVarianceSteepness = Float.parseFloat(prop.getProperty("configAlgo1bandVarianceSteepness"));
                    } else {
                        throw new RuntimeException("World config has unsupported algorithm (" + configAlgorithm + "), did you downgrade Temperature Bands?");
                    }
                    // Whitelist/blacklist
                    configDimBlacklistAsWhitelist = Boolean.parseBoolean(prop.getProperty("configDimBlacklistAsWhitelist"));
                    String dimBlacklist = prop.getProperty("configDimBlacklist");
                    if (dimBlacklist == null) // Loading an existing world from older mod version
                        dimBlacklist = "";
                    setDimBlacklist(configDimBlacklist, dimBlacklist);
                    // Humidity stuff (TODO)
                    //humidityCacheSize

                    updateDerivedConfig();
                } catch (IOException ex) {
                    throw new RuntimeException("Error reading world config. Crashing-out intentionally to prevent corruption. If you modified the world config manually, please fix it. Otherwise, report this Temperature Bands error.");
                }
            } else {
                // get and calc values from config
                configBandSize = TemperatureBands.CONFIG_DEFAULT.bandSize.get();
                configUseVerticalBands = TemperatureBands.CONFIG_DEFAULT.useVerticalBands.get();
                configPosShift = TemperatureBands.CONFIG_DEFAULT.bandPositionShift.get().floatValue();
                configTempRange = TemperatureBands.CONFIG_DEFAULT.tempRange.get().floatValue();
                configGradeShift = TemperatureBands.CONFIG_DEFAULT.tempGradeShift.get().floatValue();
                configAlgorithm = TemperatureBands.CONFIG_DEFAULT.bandAlgorithm.get();
                configNoiseFactor = TemperatureBands.CONFIG_DEFAULT.noiseFactor.get();
                if (configAlgorithm == 1) {
                    configAlgo1BandVariance = TemperatureBands.CONFIG_DEFAULT.algo1bandVariance.get();
                    configAlgo1bandVarianceSteepness = TemperatureBands.CONFIG_DEFAULT.algo1bandVarianceSteepness.get().floatValue();
                } else {
                    throw new RuntimeException("Unhandled algorithm at config load, fixme!");
                }
                // Whitelist/blacklist
                configDimBlacklistAsWhitelist = TemperatureBands.CONFIG_DEFAULT.dimBlacklistAsWhitelist.get();
                String dimBlacklist = TemperatureBands.CONFIG_DEFAULT.dimBlacklist.get();
                setDimBlacklist(configDimBlacklist, dimBlacklist);
                // Humidity stuff (TODO)
                //humidityCacheSize

                updateDerivedConfig();

                if (saveDir != null && saveDir.toFile().exists()) {
                    try (FileOutputStream output = new FileOutputStream(savePropFile)) {
                        Properties prop = setConfigProps();
                        prop.store(output, "World-specific Temperature Bands settings. Do not edit!");
                        LOGGER.info("Saved world config to {}", savePropFile.getName());
                    } catch (IOException ex) {
                        throw new RuntimeException("Error writing world config. Crashing-out intentionally to prevent corruption. Please report this Temperature Bands error.");
                    }
                } else {
                    // world is either deleted or exited, clear vars
                    resetSaveAndConfig();
                }
            }

            configDone = true;
        }
    }

    private static void setDimBlacklist(Set<String> listToUse, String dimBlacklist) {
        String[] dimBlacklistSplit = dimBlacklist.split(",");
        //if ()
        Collections.addAll(listToUse, dimBlacklistSplit);
        listToUse.remove("");
        if (!listToUse.isEmpty())
            LOGGER.info("Using dimension {}: {}", configDimBlacklistAsWhitelist ? "whitelist" : "blacklist", dimBlacklist);
        else
            LOGGER.info("Dimension {} is empty for this world", configDimBlacklistAsWhitelist ? "whitelist" : "blacklist");
    }

    private static @NotNull Properties setConfigProps() {
        Properties prop = new Properties();
        prop.setProperty("configBandSize", String.valueOf(configBandSize));
        prop.setProperty("configUseVerticalBands", String.valueOf(configUseVerticalBands));
        prop.setProperty("configPosShift", String.valueOf(configPosShift));
        prop.setProperty("configTempRange", String.valueOf(configTempRange));
        prop.setProperty("configGradeShift", String.valueOf(configGradeShift));
        prop.setProperty("configAlgorithm", String.valueOf(configAlgorithm));
        prop.setProperty("configNoiseFactor", String.valueOf(configNoiseFactor));
        prop.setProperty("configDimBlacklistAsWhitelist", String.valueOf(configDimBlacklistAsWhitelist));
        String configDimBlacklistRaw = "";
        for (String blacklistEntry : configDimBlacklist) {
            configDimBlacklistRaw = configDimBlacklistRaw.concat(blacklistEntry + ",");
        }
        configDimBlacklistRaw = configDimBlacklistRaw.substring(0, configDimBlacklistRaw.length() - 1);
        prop.setProperty("configDimBlacklist", configDimBlacklistRaw);
        if (configAlgorithm == 1) {
            prop.setProperty("configAlgo1BandVariance", String.valueOf(configAlgo1BandVariance));
            prop.setProperty("configAlgo1bandVarianceSteepness", String.valueOf(configAlgo1bandVarianceSteepness));
        } else {
            throw new RuntimeException("Unhandled algorithm at config world save, fixme!");
        }
        return prop;
    }

    private static void updateDerivedConfig() {
        tempBandMid = configBandSize / 2.0f;
        tempGradeAdj = tempBandMid / (configTempRange * 2.0f);
        if (configAlgo1BandVariance > CommonConfig.algo1bandVarianceMin) {
            tempAlgo1bandVarianceMid = (int) (configAlgo1BandVariance * 0.5);
        }
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
            TemperatureBands.LOGGER.error("Failed hooking temperature for dimension '{}' because it is not a Holder of ShiftedNoise type. Please report this to CosmicDan so support for this custom dimension might be added.", dimensionName);
            dumpExtraClassInfo(currentFunction);
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
