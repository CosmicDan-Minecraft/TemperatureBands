package github.cosmicdan.temperaturebands;

import net.minecraft.world.level.levelgen.DensityFunction;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

public class TbUtils {
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

    public static double calculateBlockDistance(int startX, int endX, int startZ, int endZ) {
        if (TemperatureBands.configDistanceFunction == 0) {
            // Manhattan distance
            return Math.abs(startX - endX) + Math.abs(startZ - endZ);
        } else if (TemperatureBands.configDistanceFunction == 2) {
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
}
