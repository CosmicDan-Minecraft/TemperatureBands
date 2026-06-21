package github.cosmicdan.temperaturebands;

import net.minecraft.world.level.levelgen.DensityFunction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

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

    static void dumpExtraClassInfo(DensityFunction func) {
        TemperatureBands.LOGGER.error("    - Class type is '{}'", func.getClass().getCanonicalName());
        TemperatureBands.LOGGER.error("    - Class dump: {}", func.toString());
    }

    public static int batchesTotal = 0;
    public static int batchesDone = -1;
    private static Instant benchmarkStart;

    public static void benchmarkReset() {
        if (batchesTotal > 0) {
            TemperatureBands.LOGGER.info("WorldPreview benchmark cancelled");
        }
        batchesTotal = 0;
        batchesDone = -1;
        benchmarkStart = Instant.now();
    }

    public static void benchmarkStart(int batchesSize) {
        TemperatureBands.LOGGER.info("Starting WorldPreview benchmark, waiting for {} batches to finish...", batchesSize);
        benchmarkStart = Instant.now();
        batchesTotal = batchesSize;
        batchesDone = 0;
    }

    public static void benchmarkBatchDone() {
        batchesDone++;
        if (batchesDone >= batchesTotal) {
            TemperatureBands.LOGGER.info("WorldPreview benchmark finished, {} batches took {} seconds." , batchesTotal, String.format("%.2f", Duration.between(benchmarkStart, Instant.now()).abs().toMillis() / 1000.0));
            batchesTotal = 0;
            batchesDone = -1;
        }
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
                logExceptionAsError(ex);
                TemperatureBands.LOGGER.error("Exception while trying to get temporary path, full error above. Cannot continue.");
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
            logExceptionAsError(ex);
            TemperatureBands.LOGGER.error("Exception while trying to get real path of '{}', full error above. Cannot continue.", pathToCheck);
        }
        return false;
    }

    public static void logExceptionAsError(Exception ex) {
        TemperatureBands.LOGGER.error(ex.toString());
        for (StackTraceElement element : ex.getStackTrace()) {
            TemperatureBands.LOGGER.error(element.toString());
        }
    }
}
