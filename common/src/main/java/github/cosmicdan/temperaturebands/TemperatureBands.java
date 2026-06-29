package github.cosmicdan.temperaturebands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
        if (dimensionName == null || dimensionName.equals("minecraft:overworld")) {
            if (dimensionName == null)
                TemperatureBands.logDebug("World creation cancelled, clearing all dimension data/config");
            else
                TemperatureBands.logDebug("Overworld was unloaded, clearing all dimension data/config");
            // world creation screen was canceled or overworld was unloaded, clear *all* data
            for (@NonNull DimensionData dimData : DIMENSION_DATA_CACHE.asMap().values())
                dimData.clearCaches();
            DIMENSION_DATA_CACHE.invalidateAll();
            DimensionConfig.clearWorldConfigs();
            isFirstDimensionData = true;
            DIMENSION_DATA_BASE = null;
        } else {
            DimensionData dimData = DIMENSION_DATA_CACHE.getIfPresent(dimensionName);
            if (dimData != null) {
                TemperatureBands.LOGGER.info("Level {} unloaded, clearing caches/configs for this specific dimension", dimensionName);
                dimData.clearCaches();
                DIMENSION_DATA_CACHE.invalidate(dimensionName);
            }
        }
    }

    /**
     * Reminder: the passed-in dimData reference might be destroyed upon exit of this method
     */
    public static DensityFunction replaceNoiseIfNeeded(DimensionData dimData, String dimensionName, DensityFunction currentFunction, String functionName) {
        // first check if we already made modded function, return it if so
        DensityFunctions.HolderHolder dimDataModdedFunction = dimData.getNoiseFunctionForName(functionName);
        if (dimDataModdedFunction != null) {
            // We already made the modded function for this dimension, reuse it
            currentFunction = dimDataModdedFunction;
            //TemperatureBands.LOGGER.info("...returned already-replaced function");
            TemperatureBands.logDebug("DimensionData for {} already has modded {}, no need to replace/recreate", dimensionName, functionName);
        } else if (currentFunction instanceof DensityFunctions.HolderHolder currentFunctionHolder) {
            // verification
            if (!functionName.equals(DensityFunctionEx.TEMPERATURE_NAME) && !functionName.equals(DensityFunctionEx.HUMIDITY_NAME))
                TbUtils.doCrash("Unhandled DensityFunction name: " + functionName + ". Fixme! [Noise type should've already been verified via LevelHooks$NoiseRouterHooks#onMapDensityFunction]");
            DensityFunction newNoise = DimensionData.createModdedFunction(functionName, currentFunctionHolder.function().value(), dimensionName, dimData.config, dimData.level.getSeed());
            if (newNoise == null) {
                Set<String> failedDimensionEntry = failedDimensionNoiseReplacements.computeIfAbsent(dimensionName, k -> new HashSet<>());
                if (!failedDimensionEntry.contains(functionName)) {
                    TemperatureBands.LOGGER.error("Failed hooking {} for dimension '{}' because the inner noise is an unrecognized type. Please report this to CosmicDan so support for this custom dimension noise might be added.", functionName, dimensionName);
                    failedDimensionEntry.add(functionName);
                    TbUtils.dumpExtraClassInfo(currentFunction);
                }
            } else {
                DensityFunctions.HolderHolder newFunction = new DensityFunctions.HolderHolder(new Holder.Direct<>(newNoise));
                currentFunction = new DensityFunctions.HolderHolder(new Holder.Direct<>(newFunction));
                DimensionData.recreateDimDataWithNewNoiseFunction(dimensionName, dimData, functionName, (DensityFunctions.HolderHolder) currentFunction);
                TemperatureBands.LOGGER.info("Succeeded in hooking {} for dimension '{}'", functionName, dimensionName);
            }
        } else {
            // function is not a HolderHolder
            if (CONFIG_GLOBAL.ignoreDimensionFailures().contains(dimensionName))
                return currentFunction;
            Set<String> failedDimensionEntry = failedDimensionNoiseReplacements.computeIfAbsent(dimensionName, k -> new HashSet<>());
            if (!failedDimensionEntry.contains(functionName)) {
                TemperatureBands.LOGGER.error("Failed hooking {} for dimension '{}' because it is not a HolderHolder type. Please report this to CosmicDan so support for this custom dimension might be added.", functionName, dimensionName);
                failedDimensionEntry.add(functionName);
                TbUtils.dumpExtraClassInfo(currentFunction);
            }
        }
        return currentFunction;
    }
}
