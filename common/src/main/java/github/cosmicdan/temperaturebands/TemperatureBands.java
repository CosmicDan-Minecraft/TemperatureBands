package github.cosmicdan.temperaturebands;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public final class TemperatureBands {
    public static final String MOD_ID = "temperaturebands";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static IModPlatform MODPLATFORM;

    public static CommonConfig CONFIG_DEFAULT = null;

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
        clearSaveAndConfig();
        saveDir = saveDirIn;
        savePropFile = saveDir.resolve(MOD_ID + ".prop").toFile();
        doTempConfig();
    }

    public static void clearSaveAndConfig() {
        saveDir = null;
        savePropFile = null;
        configDone = false;
    }

    // config values (initial values shouldn't matter, they're loaded from mod or world config)
    private static int configBandSize = 2048;
    private static boolean configUseVerticalBands = false;
    private static float configPosShift = 0.25f;
    private static float configTempRange = 0.60f;
    private static float configGradeShift = -0.05f;
    private static int configAlgorithm = 1;
    private static int configNoiseFactor = 100;
    // algo1 config
    private static int configAlgo1BandVariance = 128;
    private static float configAlgo1bandVarianceSteepness = 0.1f;
    // values derived/calculated from config
    private static float tempBandMid = 0f;
    private static float tempGradeAdj = 0f;
    private static int tempAlgo1bandVarianceMid = 0;

    public static void doTempConfig() {
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
                    updateDerivedConfig();
                } catch (IOException ex) {
                    throw new RuntimeException("Error reading world config. Crashing-out intentionally to prevent corruption. Please report this Temperature Bands error.");
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
                updateDerivedConfig();
                if (saveDir != null && saveDir.toFile().exists()) {
                    try (FileOutputStream output = new FileOutputStream(savePropFile)) {
                        Properties prop = new Properties();
                        prop.setProperty("configBandSize", String.valueOf(configBandSize));
                        prop.setProperty("configUseVerticalBands", String.valueOf(configUseVerticalBands));
                        prop.setProperty("configPosShift", String.valueOf(configPosShift));
                        prop.setProperty("configTempRange", String.valueOf(configTempRange));
                        prop.setProperty("configGradeShift", String.valueOf(configGradeShift));
                        prop.setProperty("configAlgorithm", String.valueOf(configAlgorithm));
                        prop.setProperty("configNoiseFactor", String.valueOf(configNoiseFactor));
                        if (configAlgorithm == 1) {
                            prop.setProperty("configAlgo1BandVariance", String.valueOf(configAlgo1BandVariance));
                            prop.setProperty("configAlgo1bandVarianceSteepness", String.valueOf(configAlgo1bandVarianceSteepness));
                        } else {
                            throw new RuntimeException("Unhandled algorithm at config world save, fixme!");
                        }
                        prop.store(output, "World-specific Temperature Bands settings. Do not edit!");
                        LOGGER.info("Saved world config to " + savePropFile.getName());
                    } catch (IOException ex) {
                        throw new RuntimeException("Error writing world config. Crashing-out intentionally to prevent corruption. Please report this Temperature Bands error.");
                    }
                } else {
                    // world is either deleted or exited, clear vars
                    clearSaveAndConfig();
                }
            }
            if (false) {
                // debug
                LOGGER.info("~~~~");
                LOGGER.info(String.valueOf(configBandSize));
                LOGGER.info(String.valueOf(configUseVerticalBands));
                LOGGER.info(String.valueOf(configPosShift));
                LOGGER.info(String.valueOf(configTempRange));
                LOGGER.info(String.valueOf(configGradeShift));
                LOGGER.info(String.valueOf(configAlgorithm));
                LOGGER.info(String.valueOf(configAlgo1BandVariance));
                LOGGER.info(String.valueOf(configAlgo1bandVarianceSteepness));
                LOGGER.info("~~~~");
            }
            configDone = true;
        }
    }

    private static void updateDerivedConfig() {
        tempBandMid = configBandSize / 2.0f;
        tempGradeAdj = tempBandMid / (configTempRange * 2.0f);
        if (configAlgo1BandVariance > CommonConfig.algo1bandVarianceMin) {
            tempAlgo1bandVarianceMid = configAlgo1BandVariance / 2;
        }
    }

    public static double doTempNoise(DensityFunction.FunctionContext context, Operation<Double> original) {
        int bandPos;
        int bandShift;
        if (configUseVerticalBands) {
            bandPos = Math.abs(context.blockX() - (int) (configBandSize * (configPosShift * 8)));
            bandShift = Math.abs(context.blockZ());
        } else {
            bandPos = Math.abs(context.blockZ() - (int) (configBandSize * (configPosShift * 8)));
            bandShift = Math.abs(context.blockX());
        }

        if (configAlgorithm == 1) {
            bandShift = (int) (bandShift * configAlgo1bandVarianceSteepness);
            if (configAlgo1BandVariance >= CommonConfig.algo1bandVarianceMin) {
                // using band variance
                bandShift = bandShift % configAlgo1BandVariance;
                if (bandShift > tempAlgo1bandVarianceMid) {
                    // shift is descending, adjust accordingly
                    bandShift = tempAlgo1bandVarianceMid - (bandShift - tempAlgo1bandVarianceMid);
                }
                bandPos += bandShift;
            }

            // divide bandPos by 8 because we calculate based on a bouncing gradient, idk better words lol
            bandPos /= 8;

            if (configNoiseFactor > 0) {
                bandPos += (int) (original.call(context) * configNoiseFactor);
            }
        } else {
            throw new RuntimeException("Temperature Bands has an invalid algorithm setting (" + configAlgorithm + ")");
        }

        // calculate grade
        float grade = bandPos % configBandSize;
        if (grade > tempBandMid) {
            // grade is descending, adjust accordingly
            grade = tempBandMid - (grade - tempBandMid);
        }
        grade = grade / (tempGradeAdj);
        // shift grade because vanilla has a bias for cold
        grade = grade + configGradeShift;
        if (grade < 0.0f)
            grade = 0.0f;
        // we now have a grade from 0.0 to temperaturebands_$tempRange, make it a -/+ value with 0.0 at middle
        float tempLimit = grade - configTempRange;
        // invert to match vanilla lower = colder
        tempLimit = -tempLimit;

        return tempLimit;
    }
}
