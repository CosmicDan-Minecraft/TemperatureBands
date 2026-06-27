package github.cosmicdan.temperaturebands;

import github.cosmicdan.temperaturebands.generator.BiomeProximityGenerator;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.spongepowered.asm.mixin.injection.At;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

    public static double calculateBlockDistance(int configuredDistanceFunction, int startX, int endX, int startZ, int endZ) {
        if (configuredDistanceFunction == 0) {
            // Manhattan distance
            return Math.abs(startX - endX) + Math.abs(startZ - endZ);
        } else if (configuredDistanceFunction == 2) {
            // Pure (hypotenuse)
            return Math.hypot(Math.abs(startX - endX), Math.abs(startZ - endZ));
        } else {
            // Octile (diagonal first then cardinal)
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

    public static boolean isNumberPowerOfTwo(int n) {
        return n > 0 && Integer.bitCount(n) == 1;
    }

    /** Assumes the divisor is a power of two (see above method). Still works if n is negative. **/
    public static int fastRemainder(int n, int divisor) {
        return (n < 0) ? (n | -divisor) : (n & (divisor - 1));
    }

    public static void dumpExtraClassInfo(DensityFunction func) {
        TemperatureBands.LOGGER.error("    - Class type is '{}'", func.getClass().getCanonicalName());
        if (func instanceof DensityFunctions.HolderHolder holderHolder) {
            DensityFunction funcInner = holderHolder.function().value();
            TemperatureBands.LOGGER.error("    - Inner class type is '{}'", funcInner.getClass().getCanonicalName());
        }
        TemperatureBands.LOGGER.error("    - Class dump: {}", func.toString());
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

    public static volatile boolean benchmarkActive = false;
    public static AtomicInteger batchesTotal = new AtomicInteger(0);
    public static AtomicInteger batchesDone = new AtomicInteger(-1);
    public static AtomicInteger chunksTotal = new AtomicInteger(0);
    private static Instant benchmarkStart;

    public static void benchmarkReset() {
        if (batchesTotal.get() > 0) {
            TemperatureBands.LOGGER.info("WorldPreview benchmark cancelled");
        }
        benchmarkActive = false;
        batchesTotal.set(0);
        batchesDone.set(-1);
        chunksTotal.set(0);
        BiomeProximityGenerator.benchmarkSampleTotalCount.set(0);
        BiomeProximityGenerator.benchmarkSampleCacheHitCount.set(0);
    }

    public static void benchmarkStart(int batchesSize, int chunks) {
        TemperatureBands.LOGGER.info("Starting WorldPreview benchmark, waiting for {} chunks via {} batches to finish...", chunks, batchesSize);
        benchmarkActive = true;
        benchmarkStart = Instant.now();
        batchesTotal.set(batchesSize);
        batchesDone.set(0);
        chunksTotal.set(chunks);
        BiomeProximityGenerator.benchmarkSampleTotalCount.set(0);
        BiomeProximityGenerator.benchmarkSampleCacheHitCount.set(0);
    }

    public static void benchmarkBatchDone() {
        batchesDone.getAndIncrement();
        if (batchesDone.get() >= batchesTotal.get()) {
            benchmarkActive = false;
            double timeMillis = Duration.between(benchmarkStart, Instant.now()).abs().toMillis();
            int cacheHitCount = BiomeProximityGenerator.benchmarkSampleCacheHitCount.get();
            int cacheTotalCount = BiomeProximityGenerator.benchmarkSampleTotalCount.get();
            TemperatureBands.LOGGER.info("WorldPreview benchmark finished. {} chunks via {} batches finished in {} seconds. Average generation speed of {} chunks per second. Sampler cache hit-rate was {}% ({} over {})." ,
                    chunksTotal,
                    batchesTotal,
                    String.format("%.2f", timeMillis / 1000.0),
                    String.format("%.2f", chunksTotal.get() / (timeMillis / 1000.0)),
                    String.format("%.2f", (cacheHitCount / (double) cacheTotalCount) * 100),
                    String.format("%,d", cacheHitCount) + " cached reads",
                    String.format("%,d", cacheTotalCount) + " total sample count"
            );
            batchesTotal.set(0);
            batchesDone.set(-1);
            chunksTotal.set(0);
        }
    }
}
