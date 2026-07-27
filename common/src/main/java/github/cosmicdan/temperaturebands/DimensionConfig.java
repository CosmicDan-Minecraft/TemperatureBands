package github.cosmicdan.temperaturebands;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public record DimensionConfig(
        ConfigType type,
        @Nullable File worldPropFile,
        // Base world config
        int bandSize,
        boolean useVerticalBands,
        float bandPositionShift,
        float tempRange,
        float tempGradeShift,
        int bandAlgorithm,
        float noiseFactorRaw,
        int distanceFunction,
        int vanillaNoiseOverride,
        // Blacklist config
        Set<String> dimBlacklist,
        boolean dimBlacklistAsWhitelist,
        // Temperature algo1 config
        int algo1bandVariance,
        float algo1bandVarianceSteepness,
        // Humidity config
        int humidityAlgorithm,
        float humidityTempWeight,
        // Humidity algo 1 (simple) config
        float humidityAlgo1MimicScale,
        // Humidity algo 2 (advanced) config
        int humidityResolution,
        float humidityRiverInfluence,
        int humiditySearchDistance,
        float humidityBaseNoisePercent,
        float humidityMiddleWeight,
        //public static float humidityRange = 2.0f, // Vanilla clamps humidity between -1.0 and +1.0. If you want that same clamping range, set this to 2.0. The default of 1.6 means it will be clamped to between -0.8 and +0.8 which takes
        //public static float humidityCenterWeight = 3.0f, // The "weightiness" towards middle values for humidity. Vanilla humidity generation tends to favour values closer to the middle, so this is used reduce the amount of humidity extremes (e.g. too much Jungle and Savanna). The default value seems good to me.
        // Climate sampler settings (world-specific)
        int climateSamplerResolution,
        boolean climateSamplerShortcuts,
        int bandLimitUpperCount,
        float bandLimitUpperValue,
        int bandLimitLowerCount,
        float bandLimitLowerValue
) {

    public enum ConfigType {
        DEFAULT, // the default config as loaded from the usual mod config file on startup
        BASE, // A base config which only applies to the base overworld dimension
        TEMP, // A temporary base config, which may be generated instead of BASE for some reason e.g. World Preview.
        DIMENSION // TODO: A final config with dimension-specific values assigned (doesn't apply to overworld)
    }

    /** Default dimension configs, as set on startup from mod config**/
    public static DimensionConfig DEFAULT = null;
    /** World-wide base/temp config for the world that's being generated/loaded **/
    public static DimensionConfig PENDING_WORLD = null;
    /** World-wide base/temp config for a source world when a world is being recreated (used by clients only) **/
    public static DimensionConfig RECREATED_WORLD_SOURCE = null;
    /** This is set if we're loading (not creating) a world without prop config, but then cleared if a world is being created (i.e. recreated).
     * It is checked later during initial world load (in  **/
    public static boolean isOnlyLoadingWorldWithoutConfig = false;

    /**
     * Called when a world is being created, recreated or loaded
     * @param saveDirIn The absolute path to the world save directory of the world being (re)created or loaded
     */
    public static void onLevelStorageLoad(Path saveDirIn) {
        if (PENDING_WORLD != null) {
            TemperatureBands.logDebug("Detected world recreation; source world config moved from PENDING_WORLD to RECREATED_WORLD_SOURCE");
            RECREATED_WORLD_SOURCE = PENDING_WORLD;
        }
        TemperatureBands.logDebug("About to load or create base config into PENDING_WORLD");
        PENDING_WORLD = createTempOrBase(saveDirIn);
    }

    public static boolean isPendingWorldDimensionWhitelisted(String dimensionName) {
        if (dimensionName.equals(Level.OVERWORLD.location().toString()))
            return true;
        if (PENDING_WORLD == null)
            return false; // Fix for "Fake" levels (e.g. Supplementaries/Moonlight lib
        boolean isWhitelistedDim = false;
        if (PENDING_WORLD.dimBlacklistAsWhitelist()) {
            if (PENDING_WORLD.dimBlacklist().contains(dimensionName))
                isWhitelistedDim = true;
        } else if (!PENDING_WORLD.dimBlacklist().contains(dimensionName))
            isWhitelistedDim = true;
        return isWhitelistedDim;
    }

    public static void clearWorldConfigs() {
        PENDING_WORLD = null;
        // we don't clear RECREATED_WORLD_SOURCE here for World Preview compat, we do that in ClientGuiHooks#CreateWorldScreenHooks
        isOnlyLoadingWorldWithoutConfig = false;
    }

    /**
     * Creates a new DEFAULT type of DimensionConfig and sets it to DEFAULT field.
     * @param loadedConfig The mod config as created by modloader, i.e. specPairConfigCommon.getLeft()
     */
    public static void createDefault(CommonConfig loadedConfig) {
        // prepare dimBlacklist set
        HashSet<String> dimBlacklist = new HashSet<>();
        TbUtils.convertCsvToSet(loadedConfig.dimBlacklist.get(), dimBlacklist);
        // sanity checks
        int bandAlgorithm = loadedConfig.bandAlgorithm.get();
        validateBandAlgorithmValue(bandAlgorithm);
        int humidityAlgorithm = loadedConfig.humidityAlgorithm.get();
        validateHumidityAlgorithmValue(humidityAlgorithm);
        // all good
        DEFAULT = new DimensionConfig(
                ConfigType.DEFAULT,
                null,
                loadedConfig.bandSize.get(),
                loadedConfig.useVerticalBands.get(),
                loadedConfig.bandPositionShift.get().floatValue(),
                loadedConfig.tempRange.get().floatValue(),
                loadedConfig.tempGradeShift.get().floatValue(),
                bandAlgorithm,
                loadedConfig.noiseFactor.get().floatValue(),
                loadedConfig.distanceFunction.get(),
                loadedConfig.vanillaNoiseOverride.get(),
                dimBlacklist,
                loadedConfig.dimBlacklistAsWhitelist.get(),
                loadedConfig.algo1bandVariance.get(),
                loadedConfig.algo1bandVarianceSteepness.get().floatValue(),
                humidityAlgorithm,
                loadedConfig.humidityTempWeight.get().floatValue(),
                loadedConfig.humidityAlgo1MimicScale.get().floatValue(),
                loadedConfig.humidityResolution.get(),
                loadedConfig.humidityRiverInfluence.get().floatValue(),
                loadedConfig.humiditySearchDistance.get(),
                loadedConfig.humidityBaseNoisePercent.get().floatValue(),
                loadedConfig.humidityMiddleWeight.get().floatValue(),
                loadedConfig.climateSamplerResolution.get(),
                loadedConfig.climateSamplerShortcuts.get(),
                loadedConfig.bandLimitUpperCount.get(),
                loadedConfig.bandLimitUpperValue.get().floatValue(),
                loadedConfig.bandLimitLowerCount.get(),
                loadedConfig.bandLimitLowerValue.get().floatValue()
        );
    }

    private static void validateBandAlgorithmValue(int bandAlgorithm) {
        if (bandAlgorithm != 1) {
            TbUtils.doCrash("Unrecognized bandAlgorithm value '" + bandAlgorithm + "' on loading default config, did you downgrade the mod?");
        }
    }

    private static void validateHumidityAlgorithmValue(int humidityAlgorithm) {
        if (humidityAlgorithm < 0 || humidityAlgorithm > 2) {
            TbUtils.doCrash("Unrecognized humidityAlgorithm value '" + humidityAlgorithm + "' on loading default config, did you downgrade the mod?");
        }
    }

    /**
     * Creates a new TEMP or BASE type of DimensionConfig.
     * @param saveDirIn The path for this world. If it exists, props will be read - otherwise DEFAULT config will be used.
     * @return A DimensionConfig of either BASE or TEMP type, where TEMP is chosen if the worldPropFile happens to be in system temporary path (e.g. World Preview)
     */
    public static @NotNull DimensionConfig createTempOrBase(@NotNull Path saveDirIn) {
        boolean worldAlreadyGenerated = false;
        if (Files.exists(saveDirIn) && Files.isDirectory(saveDirIn))
            // Always check for level.dat to determine if an existing world is present; (required for dedicated server compat.)
            worldAlreadyGenerated = saveDirIn.resolve("level.dat").toFile().exists();
        File worldPropFile = saveDirIn.resolve(TemperatureBands.MOD_ID + ".prop").toFile();
        DimensionConfig dimConfig = null;
        String worldName = worldPropFile.getParentFile().getName();
        boolean isTempProp = TbUtils.isFileInsideTemp(worldPropFile);
        String configTypeNameForLogging = isTempProp ? "temporary" : "base";
        if (!worldAlreadyGenerated) {
            isOnlyLoadingWorldWithoutConfig = false;
            if (RECREATED_WORLD_SOURCE != null) {
                // We're recreating a world...
                if (TemperatureBands.CONFIG_GLOBAL.copyConfigOnRecreateWorld()) {
                    // ... and config is set to copy source world config
                    @Nullable File sourceWorldPropFile = RECREATED_WORLD_SOURCE.worldPropFile;
                    if (sourceWorldPropFile == null)
                        return TbUtils.doCrash("Null worldPropFile from source world during recreation, eh?");
                    else if (sourceWorldPropFile.exists()) {
                        dimConfig = createConfigFromProp(sourceWorldPropFile, ConfigType.BASE, worldPropFile);
                        TemperatureBands.LOGGER.info("Loaded {} config for world '{}' from recreated world source '{}'", configTypeNameForLogging, worldName, sourceWorldPropFile.getParentFile().getName());
                    } else {
                        dimConfig = newBaseOrTempFromDefault(isTempProp, worldPropFile);
                        TemperatureBands.LOGGER.info("Created new {} config for world '{}' from mod defaults (because source world does not use Temperature Bands)", configTypeNameForLogging, worldName);
                    }
                } else {
                    // ... and config is NOT set to copy source world config, so use defaults
                    dimConfig = newBaseOrTempFromDefault(isTempProp, worldPropFile);
                    TemperatureBands.LOGGER.info("Created new {} config for world '{}' from mod defaults (because copyConfigOnRecreateWorld is not set)", configTypeNameForLogging, worldName);
                }
            } else {
                // Creating a new world
                dimConfig = newBaseOrTempFromDefault(isTempProp, worldPropFile);
                TemperatureBands.LOGGER.info("Created new {} config for world '{}' from mod defaults", configTypeNameForLogging, worldName);
            }
            // Save config to props if not temporary (after some sanity checks)
            if (!isTempProp) {
                if (dimConfig == null)
                    return TbUtils.doCrash("dimConfig is null");
                else if (dimConfig.type.equals(ConfigType.TEMP))
                    return TbUtils.doCrash("Fresh DimensionConfig config is TEMP type, eh?");
                else if (dimConfig.worldPropFile == null)
                    return TbUtils.doCrash("Fresh DimensionConfig worldPropFile is null, eh?");
                else if (dimConfig.worldPropFile.exists())
                    return TbUtils.doCrash("Fresh DimensionConfig worldPropFile already exists, eh?");
                saveConfigToProp(dimConfig, dimConfig.worldPropFile);
            } else {
                TemperatureBands.logDebug("Not saving prop because config is temporary type");
            }
        } else {
            // Loading an existing world (either fully or as part as recreation)
            if (worldPropFile.exists()) {
                dimConfig = createConfigFromProp(worldPropFile, isTempProp ? ConfigType.TEMP : ConfigType.BASE);
                TemperatureBands.LOGGER.info("Loaded {} config from world '{}' props", configTypeNameForLogging, worldName);
            } else {
                // No prop config. We could crash-out here to prevent loading non-modded worlds, but the user might be recreating a world, so just set a flag that allows loading defaults in that case
                isOnlyLoadingWorldWithoutConfig = true;
                dimConfig = newBaseOrTempFromDefault(isTempProp, worldPropFile);
            }
        }
        if (dimConfig == null)
            return TbUtils.doCrash("dimConfig is null");
        logBlacklistConfig(worldName, dimConfig.dimBlacklist, dimConfig.dimBlacklistAsWhitelist);
        return dimConfig;
    }

    private static DimensionConfig newBaseOrTempFromDefault(boolean isTempProp, File worldPropFile) {
        return new DimensionConfig(
                isTempProp ? ConfigType.TEMP : ConfigType.BASE,
                worldPropFile,
                DEFAULT.bandSize,
                DEFAULT.useVerticalBands,
                DEFAULT.bandPositionShift,
                DEFAULT.tempRange,
                DEFAULT.tempGradeShift,
                DEFAULT.bandAlgorithm,
                DEFAULT.noiseFactorRaw,
                DEFAULT.distanceFunction,
                DEFAULT.vanillaNoiseOverride,
                DEFAULT.dimBlacklist,
                DEFAULT.dimBlacklistAsWhitelist,
                DEFAULT.algo1bandVariance,
                DEFAULT.algo1bandVarianceSteepness,
                DEFAULT.humidityAlgorithm,
                DEFAULT.humidityTempWeight,
                DEFAULT.humidityAlgo1MimicScale,
                DEFAULT.humidityResolution,
                DEFAULT.humidityRiverInfluence,
                DEFAULT.humiditySearchDistance,
                DEFAULT.humidityBaseNoisePercent,
                DEFAULT.humidityMiddleWeight,
                DEFAULT.climateSamplerResolution,
                DEFAULT.climateSamplerShortcuts,
                DEFAULT.bandLimitUpperCount,
                DEFAULT.bandLimitUpperValue,
                DEFAULT.bandLimitLowerCount,
                DEFAULT.bandLimitLowerValue
        );
    }

    private static void logBlacklistConfig(String worldName, Set<String> dimBlacklist, boolean dimBlacklistAsWhitelist) {
        if (!dimBlacklist.isEmpty())
            TemperatureBands.LOGGER.info("Using dimension {} for '{}' : {}", (dimBlacklistAsWhitelist ? "whitelist" : "blacklist"), worldName, TbUtils.convertSetToCsv(dimBlacklist));
        else
            TemperatureBands.LOGGER.info("Dimension {} is empty for '{}'", (dimBlacklistAsWhitelist ? "whitelist" : "blacklist"), worldName);
    }

    private static DimensionConfig createConfigFromProp(@NotNull File worldPropInput, ConfigType type) {
        return createConfigFromProp(worldPropInput, type, null);
    }

    private static DimensionConfig createConfigFromProp(@NotNull File worldPropInput, ConfigType type, @Nullable File worldPropOutputOverride) {
        try (FileInputStream input = new FileInputStream(worldPropInput)) {
            Properties prop = new Properties();
            prop.load(input);
            // First deserialize non-primitives and do sanity checks
            // Blacklist
            Set<String> dimBlacklist = new HashSet<>();
            TbUtils.convertCsvToSet(prop.getProperty(CommonConfig.dimBlacklistName, ""), dimBlacklist);
            // temperature band algorithm
            int bandAlgorithm = Integer.parseInt(prop.getProperty(CommonConfig.bandAlgorithmName));
            validateBandAlgorithmValue(bandAlgorithm);
            // humidity algorithm (default must be 0 to preserve existing worlds)
            int humidityAlgorithm = Integer.parseInt(prop.getProperty(CommonConfig.humidityAlgorithmName, "0"));
            validateHumidityAlgorithmValue(humidityAlgorithm);
            // all good, read other props and make new config
            return new DimensionConfig(
                    type,
                    worldPropOutputOverride == null ? worldPropInput : worldPropOutputOverride,
                    // Base world config
                    Integer.parseInt(prop.getProperty(CommonConfig.bandSizeName)),
                    Boolean.parseBoolean(prop.getProperty(CommonConfig.useVerticalBandsName)),
                    Float.parseFloat(prop.getProperty(CommonConfig.bandPositionShiftName)),
                    Float.parseFloat(prop.getProperty(CommonConfig.tempRangeName)),
                    Float.parseFloat(prop.getProperty(CommonConfig.tempGradeShiftName)),
                    bandAlgorithm,
                    Float.parseFloat(prop.getProperty(CommonConfig.noiseFactorName)),
                    Integer.parseInt(prop.getProperty(CommonConfig.distanceFunctionName, String.valueOf(DEFAULT.distanceFunction))),
                    Integer.parseInt(prop.getProperty(CommonConfig.vanillaNoiseOverrideName, String.valueOf(DEFAULT.vanillaNoiseOverride))),
                    // Blacklist
                    dimBlacklist,
                    Boolean.parseBoolean(prop.getProperty(CommonConfig.dimBlacklistAsWhitelistName, String.valueOf(DEFAULT.dimBlacklistAsWhitelist))),
                    // Temperature algo1
                    Integer.parseInt(prop.getProperty(CommonConfig.algo1bandVarianceName, String.valueOf(DEFAULT.algo1bandVariance))),
                    Float.parseFloat(prop.getProperty(CommonConfig.algo1bandVarianceSteepnessName)),
                    // Humidity
                    humidityAlgorithm,
                    Float.parseFloat(prop.getProperty(CommonConfig.humidityTempWeightName, String.valueOf(DEFAULT.humidityTempWeight))),
                    // Humidity algo 1 (simple)
                    Float.parseFloat(prop.getProperty(CommonConfig.humidityAlgo1MimicScaleName, String.valueOf(DEFAULT.humidityAlgo1MimicScale))),
                    // Humidity algo 2 (advanced)
                    Integer.parseInt(prop.getProperty(CommonConfig.humidityResolutionName, String.valueOf(DEFAULT.humidityResolution))),
                    Float.parseFloat(prop.getProperty(CommonConfig.humidityRiverInfluenceName, String.valueOf(DEFAULT.humidityRiverInfluence))),
                    Integer.parseInt(prop.getProperty(CommonConfig.humiditySearchDistanceName, String.valueOf(DEFAULT.humiditySearchDistance))),
                    Float.parseFloat(prop.getProperty(CommonConfig.humidityBaseNoisePercentName, String.valueOf(DEFAULT.humidityBaseNoisePercent))),
                    Float.parseFloat(prop.getProperty(CommonConfig.humidityMiddleWeightName, String.valueOf(DEFAULT.humidityMiddleWeight))),
                    Integer.parseInt(prop.getProperty(CommonConfig.climateSamplerResolutionName, String.valueOf(DEFAULT.climateSamplerResolution))),
                    Boolean.parseBoolean(prop.getProperty(CommonConfig.climateSamplerShortcutsName, String.valueOf(DEFAULT.climateSamplerShortcuts))),
                    // band limit
                    Integer.parseInt(prop.getProperty(CommonConfig.bandLimitUpperCountName, String.valueOf(0))), // default 0 to preserve pre-2.2.0 worlds
                    Float.parseFloat(prop.getProperty(CommonConfig.bandLimitUpperValueName, String.valueOf(DEFAULT.bandLimitUpperValue))),
                    Integer.parseInt(prop.getProperty(CommonConfig.bandLimitLowerCountName, String.valueOf(0))), // default 0 to preserve pre-2.2.0 worlds
                    Float.parseFloat(prop.getProperty(CommonConfig.bandLimitLowerValueName, String.valueOf(DEFAULT.bandLimitLowerValue)))
            );
        } catch (IOException ex) {
            return TbUtils.doCrash(ex, "Error reading world config, crashing-out intentionally to prevent corruption. Full error is above. If you modified the world config manually, please fix it. Otherwise, report this Temperature Bands error.");
        }
    }

    private static void saveConfigToProp(@NotNull DimensionConfig configToSave, @NotNull File worldPropFileOutput) {
        try (FileOutputStream worldPropOutputStream = new FileOutputStream(worldPropFileOutput)) {
            Properties prop = new Properties();
            // Base world config
            prop.setProperty(CommonConfig.bandSizeName, String.valueOf(configToSave.bandSize));
            prop.setProperty(CommonConfig.useVerticalBandsName, String.valueOf(configToSave.useVerticalBands));
            prop.setProperty(CommonConfig.bandPositionShiftName, String.valueOf(configToSave.bandPositionShift));
            prop.setProperty(CommonConfig.tempRangeName, String.valueOf(configToSave.tempRange));
            prop.setProperty(CommonConfig.tempGradeShiftName, String.valueOf(configToSave.tempGradeShift));
            prop.setProperty(CommonConfig.bandAlgorithmName, String.valueOf(configToSave.bandAlgorithm));
            prop.setProperty(CommonConfig.noiseFactorName, String.valueOf(configToSave.noiseFactorRaw));
            prop.setProperty(CommonConfig.distanceFunctionName, String.valueOf(configToSave.distanceFunction));
            prop.setProperty(CommonConfig.vanillaNoiseOverrideName, String.valueOf(configToSave.vanillaNoiseOverride));
            // Blacklist
            if (!configToSave.dimBlacklist.isEmpty()) {
                prop.setProperty(CommonConfig.dimBlacklistName, TbUtils.convertSetToCsv(configToSave.dimBlacklist));
                prop.setProperty(CommonConfig.dimBlacklistAsWhitelistName, String.valueOf(configToSave.dimBlacklistAsWhitelist));
            }
            // Temperature algo1
            if (configToSave.bandAlgorithm == 1) {
                prop.setProperty(CommonConfig.algo1bandVarianceName, String.valueOf(configToSave.algo1bandVariance));
                prop.setProperty(CommonConfig.algo1bandVarianceSteepnessName, String.valueOf(configToSave.algo1bandVarianceSteepness));
            } else {
                TbUtils.doCrash("Unhandled algorithm at config world save, fixme!");
            }
            // Humidity
            prop.setProperty(CommonConfig.humidityAlgorithmName, String.valueOf(configToSave.humidityAlgorithm));
            prop.setProperty(CommonConfig.humidityTempWeightName, String.valueOf(configToSave.humidityTempWeight));
            if (configToSave.humidityAlgorithm == 1) {
                // Humidity algo 1 (simple)
                prop.setProperty(CommonConfig.humidityAlgo1MimicScaleName, String.valueOf(configToSave.humidityAlgo1MimicScale));
            } else if (configToSave.humidityAlgorithm == 2) {
                // Humidity algo 2 (advanced)
                prop.setProperty(CommonConfig.humidityResolutionName, String.valueOf(configToSave.humidityResolution));
                prop.setProperty(CommonConfig.humidityRiverInfluenceName, String.valueOf(configToSave.humidityRiverInfluence));
                prop.setProperty(CommonConfig.humiditySearchDistanceName, String.valueOf(configToSave.humiditySearchDistance));
                prop.setProperty(CommonConfig.humidityBaseNoisePercentName, String.valueOf(configToSave.humidityBaseNoisePercent));
                prop.setProperty(CommonConfig.humidityMiddleWeightName, String.valueOf(configToSave.humidityMiddleWeight));
            } else if (configToSave.humidityAlgorithm != 0) {
                TbUtils.doCrash("Unhandled humidity algorithm at config world save, fixme!");
            }
            prop.setProperty(CommonConfig.climateSamplerResolutionName, String.valueOf(configToSave.climateSamplerResolution));
            prop.setProperty(CommonConfig.climateSamplerShortcutsName, String.valueOf(configToSave.climateSamplerShortcuts));
            // band limit
            prop.setProperty(CommonConfig.bandLimitUpperCountName, String.valueOf(configToSave.bandLimitUpperCount));
            prop.setProperty(CommonConfig.bandLimitUpperValueName, String.valueOf(configToSave.bandLimitUpperValue));
            prop.setProperty(CommonConfig.bandLimitLowerCountName, String.valueOf(configToSave.bandLimitLowerCount));
            prop.setProperty(CommonConfig.bandLimitLowerValueName, String.valueOf(configToSave.bandLimitLowerValue));

            prop.store(worldPropOutputStream, "World-specific Temperature Bands settings. Do not edit!");
        } catch (IOException ex) {
            TbUtils.doCrash(ex, "Couldn't save world-specific config. Crashing-out intentionally to prevent world corruption. Error details are above.");
        }
    }
}
