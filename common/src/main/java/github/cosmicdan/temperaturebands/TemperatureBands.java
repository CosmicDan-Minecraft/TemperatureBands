package github.cosmicdan.temperaturebands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class TemperatureBands {
    public static final String MOD_ID = "temperaturebands";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static IModPlatform MODPLATFORM;

    public static CommonConfig CONFIG_DEFAULT = null;

    /** Used to "remember" some things for each dimension so we can access it later from various places. Entries are created in ChunkMapHooks, added to by various other hooks, and cleared on shutdown via MinecraftServerHooks */
    public static final Cache<String, DimensionData> DIMENSION_DATA_CACHE = Caffeine.newBuilder().build();

    /** As above, but "draft" versions of DimensionData that are cut-down and waiting to be fully initialized after ServerLevel has finished instantiation  **/
    public static final Cache<String, DimensionData> DIMENSION_DATA_CACHE_DRAFTS = Caffeine.newBuilder().build();


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
    // blacklist config
    public static Set<String> configDimBlacklist = new HashSet<>();
    public static boolean configDimBlacklistAsWhitelist = false;

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

    public static void finalizeDimensionDataForDraftLevel(String dimensionName) {
        DimensionData draftDimData = TemperatureBands.DIMENSION_DATA_CACHE_DRAFTS.getIfPresent(dimensionName);
        if (draftDimData != null) {
            DIMENSION_DATA_CACHE.put(dimensionName,
                    new DimensionDataAtomic(
                            draftDimData.getLevel(),
                            draftDimData.getNoiseRouter(),
                            draftDimData.getNoiseFunctionForName(ShiftedNoiseTemperature.NAME),
                            draftDimData.getNoiseFunctionForName(ShiftedNoiseHumidity.NAME)
                    ));
            DIMENSION_DATA_CACHE_DRAFTS.invalidate(dimensionName);
        } else {
            throw new RuntimeException("Tried to finalize DimensionData from draft that didn't exist (" + dimensionName + "), eh?");
        }
    }

    public static DensityFunction replaceNoiseIfNeeded(DimensionData activeDimData, String dimensionName, DensityFunction currentFunction, String functionName) {
        // first check if we already made modded function, return it if so
        DensityFunctions.HolderHolder dimDataModdedFunction = activeDimData.getNoiseFunctionForName(functionName);
        if (dimDataModdedFunction != null) {
            // We already made the modded function for this dimension, reuse it
            currentFunction = dimDataModdedFunction;
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
            activeDimData.setNoiseFunctionForName(functionName, newFunction);
            currentFunction = new DensityFunctions.HolderHolder(new Holder.Direct<>(newFunction));
            TemperatureBands.LOGGER.info("Succeeded in hooking {} for dimension '{}'", functionName, dimensionName);
        } else {
            TemperatureBands.LOGGER.error("Failed hooking temperature for dimension '{}' because it is not a Holder of ShiftedNoise type. Please report this to CosmicDan so support for this custom dimension can be added.", dimensionName);
            dumpExtraClassInfo(currentFunction);
        }
        return currentFunction;
    }

    private static void dumpExtraClassInfo(DensityFunction func) {
        LOGGER.error("    - Class type is '{}'", func.getClass().getCanonicalName());
        LOGGER.error("    - Class dump: {}", func.toString());
    }
}
