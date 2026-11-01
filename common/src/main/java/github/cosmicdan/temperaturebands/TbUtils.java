package github.cosmicdan.temperaturebands;

import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static github.cosmicdan.temperaturebands.TemperatureBands.CONFIG_GLOBAL;
import static github.cosmicdan.temperaturebands.TemperatureBands.LOGGER;

public class TbUtils {
    private static Path TEMP_PATH = null;

    public static void convertCsvToSet(String inputCsv, Set<String> outputSet) {
        String[] inputCsvSplit = inputCsv.split(",");
        Collections.addAll(outputSet, inputCsvSplit);
        outputSet.remove("");
    }

    public static String convertSetToCsv(Set<String> inputSet) {
        String outputCsv = "";
        for (String entry : inputSet) {
            outputCsv = outputCsv.concat(entry + ",");
        }
        return outputCsv;
    }

    public static double calculateBlockDistance(double configuredDistanceFunction, double startX, double endX, double startZ, double endZ) {
        if (configuredDistanceFunction == 0) {
            // Manhattan distance
            return Math.abs(startX - endX) + Math.abs(startZ - endZ);
        } else if (configuredDistanceFunction == 1) {
            // Octile (diagonal first then cardinal)
            double distanceX = Math.abs(startX - endX);
            double distanceZ = Math.abs(startZ - endZ);
            double straightCost = 1.0;
            double diagonalCost = Math.sqrt(2);

            if (distanceX > distanceZ) {
                return ((diagonalCost * distanceZ) + (straightCost * (distanceX - distanceZ)));
            } else {
                return ((diagonalCost * distanceX) + (straightCost * (distanceZ - distanceX)));
            }
        } else if (configuredDistanceFunction == 2) {
            // Pure (Euclidean Safe)
            return Math.hypot(Math.abs(startX - endX), Math.abs(startZ - endZ));
        } else { // 3
            // Optimal (Euclidean Fast)
            double dx = endX - startX;
            double dz = endZ - startZ;
            return Math.sqrt((dx * dx) + (dz * dz));
        }
    }

    public static boolean isNumberPowerOfTwo(int n) {
        return n > 0 && Integer.bitCount(n) == 1;
    }

    /** Assumes the divisor is a power of two (see above method). Still works if n is negative. **/
    public static int fastRemainder(int n, int divisor) {
        return (n < 0) ? (n | -divisor) : (n & (divisor - 1));
    }

    public static void dumpExtraClassInfo(Object clazz) {
        TemperatureBands.LOGGER.error("    - Class type is '{}'", clazz.getClass().getCanonicalName());
        if (clazz instanceof DensityFunctions.HolderHolder holderHolder) {
            DensityFunction funcInner = holderHolder.function().value();
            TemperatureBands.LOGGER.error("    - Inner class type is '{}'", funcInner.getClass().getCanonicalName());
        }
        TemperatureBands.LOGGER.error("    - Class dump: {}", clazz.toString());
    }

    public static double pullTowardsZeroLinear(double value, double weight) {
        double pullForce = weight * Math.abs(value);
        if (value > 0)
            return Math.max(0, value - pullForce);
        else if (value < 0)
            return Math.min(0, value + pullForce);
        return 0;
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

    public static Path getTempPath() {
        if (TEMP_PATH == null) {
            try {
                TEMP_PATH = Paths.get(System.getProperty("java.io.tmpdir")).toRealPath();
            } catch (IOException ex) {
                //logExceptionAsError(ex);
                //TemperatureBands.LOGGER.error("Exception while trying to get temporary path, full error above. Cannot continue.");
                doCrash(ex, "Couldn't get system temporary path");
            }
        }
        return TEMP_PATH;
    }

    public static boolean isFileInsideTemp(File pathToCheck) {
        try {
            // first loop through parents to find path that actually exists
            while (!pathToCheck.exists()) {
                pathToCheck = pathToCheck.getParentFile();
                if (pathToCheck == null)
                    return false;
            }
            return pathToCheck.toPath().toRealPath().startsWith(getTempPath());
        } catch (IOException ex) {
            //logExceptionAsError(ex);
            //TemperatureBands.LOGGER.error("Exception while trying to get real path of '{}', full error above. Cannot continue.", pathToCheck);
            doCrash(ex, "Couldn't get real path of '" + pathToCheck + "'.");
        }
        return false;
    }

    public static <T> T doCrash(String msg) throws ReportedException {
        return doCrash(new RuntimeException(), msg);
    }

    public static <T> T doCrash(Throwable throwable, String msg) throws ReportedException {
        CrashReport report = CrashReport.forThrowable(throwable, "Temperature Bands error: " + msg);
        throw new ReportedException(report);
    }

    public static @Nullable MultiNoiseBiomeSource findMultiNoiseBiomeSource(ServerLevel level, boolean logProgress) {
        String dimensionName = level.dimension().location().toString();
        BiomeSource biomeSource = level.getChunkSource().getGenerator().getBiomeSource();
        if (biomeSource instanceof MultiNoiseBiomeSource)
            return (MultiNoiseBiomeSource) biomeSource;
        else {
            // some mods badly overwrite MultiNoiseBiomeSource with their own modded source that only implements base
            // BiomeSource (e.g. Blueprint), try to find original via reflection
            if (logProgress)
                LOGGER.warn("The dimension {} does not use MultiNoiseBiomeSource, attempting to find one via reflection...", dimensionName);
            Field[] biomeSourceFields = biomeSource.getClass().getDeclaredFields();
            for (Field field : biomeSourceFields) {
                try {
                    field.setAccessible(true);
                    String biomeSourceFieldName = field.getName();
                    Object biomeSourceFieldValue = field.get(biomeSource);
                    if (biomeSourceFieldValue instanceof MultiNoiseBiomeSource) {
                        if (logProgress)
                            LOGGER.warn("...found via field '{}'", biomeSourceFieldName);
                        return (MultiNoiseBiomeSource) biomeSourceFieldValue;
                    }
                } catch (IllegalAccessException ignored) {}
            }

            if (!CONFIG_GLOBAL.ignoreDimensionFailures().contains(dimensionName) && logProgress) {
                LOGGER.error("Error: The dimension {} does not use a MultiNoiseBiomeSource; additionally it was not found via reflection.", dimensionName);
                if (dimensionName.equals("minecraft:overworld"))
                    LOGGER.error("This is unexpected for the overworld; there must be another mod that is overwriting the BiomeSource with a non MultiNoise type, which is a naughty thing to do.");
                LOGGER.error("Please report this to CosmicDan so support for this modded dimension might be added.");
                LOGGER.error("Dump of BiomeSource class for debugging:");
                TbUtils.dumpExtraClassInfo(biomeSource);
            }
        }
        return null;
    }

    public static List<Integer> intListDeserialize(String listRaw) {
        if (listRaw == null || listRaw.trim().isEmpty())
            return List.of();

        return Arrays.stream(listRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    public static String intListSerialize(List<? extends Integer> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
                .map(String::valueOf) // Equivalent to String.valueOf(integer)
                .collect(Collectors.joining(","));
    }

    public static void determine2dDimensions(Climate.Sampler sampler, boolean[] isDimension2d) {
        DensityFunction.SinglePointContext firstContext = new DensityFunction.SinglePointContext(0, 0, 0);
        Climate.TargetPoint firstPoint = Climate.target(
                (float) sampler.temperature().compute(firstContext),
                0.0f,
                (float) sampler.continentalness().compute(firstContext),
                (float) sampler.erosion().compute(firstContext),
                (float) sampler.depth().compute(firstContext),
                (float) sampler.weirdness().compute(firstContext)
        );
        DensityFunction.SinglePointContext secondContext = new DensityFunction.SinglePointContext(0, 200, 0);
        Climate.TargetPoint secondPoint = Climate.target(
                (float) sampler.temperature().compute(secondContext),
                0.0f,
                (float) sampler.continentalness().compute(secondContext),
                (float) sampler.erosion().compute(secondContext),
                (float) sampler.depth().compute(secondContext),
                (float) sampler.weirdness().compute(secondContext)
        );

        if (firstPoint.temperature() == secondPoint.temperature())
            isDimension2d[0] = true;
        if (firstPoint.humidity() == secondPoint.humidity())
            isDimension2d[1] = true;
        if (firstPoint.continentalness() == secondPoint.continentalness())
            isDimension2d[2] = true;
        if (firstPoint.erosion() == secondPoint.erosion())
            isDimension2d[3] = true;
        if (firstPoint.depth() == secondPoint.depth())
            isDimension2d[4] = true;
        if (firstPoint.weirdness() == secondPoint.weirdness())
            isDimension2d[5] = true;
    }

    // for World Preview Benchmark (doBenchmark)
    public static volatile boolean benchmarkActive = false;
    public static AtomicInteger batchesTotal = new AtomicInteger(0);
    public static AtomicInteger batchesDone = new AtomicInteger(-1);
    public static AtomicInteger chunksTotal = new AtomicInteger(0);
    private static AtomicReference<Instant> benchmarkStart;
    // for Level Load Benchmark (doLevelLoadBenchmark)
    public static long levelLoadBenchmarkStart = -1L;

    public static void benchmarkReset() {
        if (TemperatureBands.isClientSide() && TbUtilsClient.isOnWorldCreateScreen()) {
            if (batchesTotal.get() > 0) {
                TemperatureBands.LOGGER.info("WorldPreview benchmark cancelled");
            }
            benchmarkActive = false;
            batchesTotal.set(0);
            batchesDone.set(-1);
            chunksTotal.set(0);
        }
    }

    public static void benchmarkStart(int batchesSize, int chunks) {
        if (TemperatureBands.isClientSide() && TbUtilsClient.isOnWorldCreateScreen()) {
            TemperatureBands.LOGGER.info("Starting WorldPreview benchmark, waiting for {} samples via {} batches to finish...", chunks, batchesSize);
            benchmarkActive = true;
            batchesTotal.set(batchesSize);
            batchesDone.set(0);
            chunksTotal.set(chunks);
            benchmarkStart = new AtomicReference<>(Instant.now());
        }
    }

    public static void benchmarkBatchDone() {
        if (TemperatureBands.isClientSide() && TbUtilsClient.isOnWorldCreateScreen()) {
            while (benchmarkStart == null) {
                try {
                    // Sometimes happens when the benchmark is too fast, just wait (World Preview always calls this from another thread)
                    //noinspection BusyWait
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }
            batchesDone.getAndIncrement();
            if (batchesDone.get() >= batchesTotal.get()) {
                benchmarkActive = false;
                double timeMillis = Duration.between(benchmarkStart.get(), Instant.now()).abs().toMillis();
                TemperatureBands.LOGGER.info("WorldPreview benchmark finished. {} samples via {} batches finished in {} seconds. Average speed of {} samples per second." ,
                        chunksTotal,
                        batchesTotal,
                        String.format("%.2f", timeMillis / 1000.0),
                        String.format("%.2f", chunksTotal.get() / (timeMillis / 1000.0))
                );
                batchesTotal.set(0);
                batchesDone.set(-1);
                chunksTotal.set(0);
            }
        }
    }
}
