package github.cosmicdan.temperaturebands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import github.cosmicdan.temperaturebands.noise.ShiftedNoiseEx;
import github.cosmicdan.temperaturebands.noise.ShiftedNoiseHumidity;
import github.cosmicdan.temperaturebands.noise.ShiftedNoiseTemperature;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.util.*;

public final class TemperatureBands {
    public static final String MOD_NAME = "TemperatureBands";
    public static final String MOD_ID = "temperaturebands";
    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    public static boolean addLoadingScreenText;
    private static final boolean IS_IDEA_DEBUG = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains("jdwp");
    private final CommonConfig modConfig;

    /** Global (not dimension-specific) config shared by all worlds/dimensions **/
    public static GlobalConfig CONFIG_GLOBAL = null;
    /** Base world data and config. Set via ? */
    public static DimensionData DIMENSION_DATA_BASE;
    /** Dimension-specific data and config for the current world. Entries are created via ChunkMapHooks, finalized via ServerLevelHooks,
     * are read from various places, and cleared on server shutdown via MinecraftServerHooks */
    public static final Cache<String, DimensionData> DIMENSION_DATA_CACHE = Caffeine.newBuilder().build();
    public static boolean isFirstDimensionData = true;
    /** Used on client to check if "Delete World" screen is currently active (avoids loading world data if so) **/
    public static boolean isDeleteScreenActive = false;

    private static final Map<String, Set<String>> failedDimensionNoiseReplacements = new HashMap<>();

    public TemperatureBands(final IModPlatform modPlatform) {
        // Register common config
        final Pair<CommonConfig, ModConfigSpec> specPairConfigCommon = new ModConfigSpec.Builder().configure(CommonConfig::new);
        modConfig = specPairConfigCommon.getLeft();
        modPlatform.registerConfigCommon(specPairConfigCommon.getRight());
    }

    public static void logDebug(String msg, String... varargs) {
        if (IS_IDEA_DEBUG)
            TemperatureBands.LOGGER.warn(msg, (Object[]) varargs);
    }

    public void onConfigLoaded(boolean isReload) {
        logDebug("Loading mod config");
        CONFIG_GLOBAL = GlobalConfig.create(modConfig);
        DimensionConfig.createDefault(modConfig);
    }

    public static void clearDimensionDataAndConfig(@Nullable String dimensionName) {
        // first clear all caches in dimensiondatas
        if (dimensionName != null)
            TemperatureBands.LOGGER.info("Level {} unloaded, clearing all caches/configs", dimensionName);
        for (@NonNull DimensionData dimData : DIMENSION_DATA_CACHE.asMap().values())
            dimData.clearCaches();
        DimensionConfig.clearWorldConfigs();
        isFirstDimensionData = true;
        DIMENSION_DATA_BASE = null;
        DIMENSION_DATA_CACHE.invalidateAll();
    }

    /**
     * Reminder the passed-in dimDataFinal reference might be destroyed upon exit of this method
     */
    public static DensityFunction replaceNoiseIfNeeded(DimensionData dimData, String dimensionName, DensityFunction currentFunction, String functionName) {
        // first check if we already made modded function, return it if so
        DensityFunctions.HolderHolder dimDataModdedFunction = dimData.getNoiseFunctionForName(functionName);
        if (dimDataModdedFunction != null) {
            // We already made the modded function for this dimension, reuse it
            currentFunction = dimDataModdedFunction;
            //TemperatureBands.LOGGER.info("...returned already-replaced function");
            TemperatureBands.logDebug("DimensionData for {} already has modded {}, no need to replace/recreate", dimensionName, functionName);
        } else if (currentFunction instanceof DensityFunctions.HolderHolder currentFunctionHolder && currentFunctionHolder.function().value() instanceof DensityFunctions.ShiftedNoise currentFunctionActual) {
            // Probably vanilla function, replace it
            ShiftedNoiseEx newNoise = null;
            if (functionName.equals(ShiftedNoiseTemperature.NAME))
                newNoise = new ShiftedNoiseTemperature(dimensionName, dimData.config, currentFunctionActual.shiftX(), currentFunctionActual.shiftY(), currentFunctionActual.shiftZ(), currentFunctionActual.xzScale(), currentFunctionActual.yScale(), currentFunctionActual.noise());
            else if (functionName.equals(ShiftedNoiseHumidity.NAME))
                newNoise = new ShiftedNoiseHumidity(dimensionName, dimData.config, currentFunctionActual.shiftX(), currentFunctionActual.shiftY(), currentFunctionActual.shiftZ(), currentFunctionActual.xzScale(), currentFunctionActual.yScale(), currentFunctionActual.noise());
            else
                TbUtils.doCrash("Unhandled noise type: " + functionName + ". Please add this dimension to blacklist, and/or report the error so support for this dimension might be added (if it's a mod-added dimension).");
            DensityFunctions.HolderHolder newFunction = new DensityFunctions.HolderHolder(new Holder.Direct<>(newNoise));
            currentFunction = new DensityFunctions.HolderHolder(new Holder.Direct<>(newFunction));
            DimensionData.recreateDimDataWithNewNoiseFunction(dimensionName, dimData, functionName, (DensityFunctions.HolderHolder) currentFunction);
            TemperatureBands.LOGGER.info("Succeeded in hooking {} for dimension '{}'", functionName, dimensionName);
        } else {
            // function is not ShiftedNoise
            if (CONFIG_GLOBAL.ignoreDimensionFailures().contains(dimensionName))
                return currentFunction;
            Set<String> failedDimensionEntry = failedDimensionNoiseReplacements.computeIfAbsent(dimensionName, k -> new HashSet<>());
            if (!failedDimensionEntry.contains(functionName)) {
                TemperatureBands.LOGGER.error("Failed hooking {} for dimension '{}' because it is not a Holder of ShiftedNoise type. Please report this to CosmicDan so support for this custom dimension might be added.", functionName, dimensionName);
                failedDimensionEntry.add(functionName);
                TbUtils.dumpExtraClassInfo(currentFunction);
            }
        }
        return currentFunction;
    }
}
