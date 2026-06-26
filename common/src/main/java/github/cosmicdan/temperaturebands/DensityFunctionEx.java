package github.cosmicdan.temperaturebands;

import github.cosmicdan.temperaturebands.generator.BandsGenerator;
import github.cosmicdan.temperaturebands.generator.BiomeProximityGenerator;
import github.cosmicdan.temperaturebands.generator.IGenerator;
import net.minecraft.core.Holder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static github.cosmicdan.temperaturebands.TemperatureBands.DIMENSION_DATA_CACHE;

public class DensityFunctionEx implements DensityFunction {
    public final static String HUMIDITY_NAME = "humidity (vegetation)";
    public final static String TEMPERATURE_NAME = "temperature";

    public final String dimensionName;
    public final String functionName;
    public final DimensionConfig config;
    public final DensityFunction funcOriginal;
    public final IGenerator gen;

    public DimensionData dimData = null;

    public DensityFunctionEx(String dimensionName, String functionName, DimensionConfig config, DensityFunction funcOriginal) {
        this.dimensionName = dimensionName;
        this.config = config;
        this.funcOriginal = funcOriginal;
        this.functionName = functionName;
        if (functionName.equals(TEMPERATURE_NAME))
            this.gen = createTemperature();
        else if (functionName.equals(HUMIDITY_NAME))
            this.gen = createHumidity();
        else
            this.gen = TbUtils.doCrash("Tried to create DensityFunctionEx with unhandled functionName '{}', fixme!");
    }

    private @NotNull IGenerator createTemperature() {
        if (config.bandAlgorithm() == 1)
            return BandsGenerator.create(this, config, false, -1.0f);
        else
            return TbUtils.doCrash("Temperature Bands has an unrecognized '" + CommonConfig.bandAlgorithmName + "' setting of '" + config.bandAlgorithm() + "'");
    }

    private @NotNull IGenerator createHumidity() {
        if (config.humidityAlgorithm() == 1)
            return BandsGenerator.create(this, config, true, config.humidityTempWeight());
        else if (config.humidityAlgorithm() == 2)
            return BiomeProximityGenerator.create(this, config);
        else
            return TbUtils.doCrash("Temperature Bands has an unrecognized '" + CommonConfig.humidityAlgorithmName + "' setting of '" + config.humidityAlgorithm() + "'");
    }

    public final double computeOriginal(DensityFunction.FunctionContext context) {
        return funcOriginal.compute(context);
    }

    public final ClimateTargetPointEx sampleClimate(long packedBlockPos, boolean firstBiomeOnly) {
        // if firstBiomeOnly is true, we're just looking for oceans so only sampling continentalness is required
        int blockX = TbUtils.unpackBlockFromLongX(packedBlockPos);
        int blockZ = TbUtils.unpackBlockFromLongZ(packedBlockPos);
        DensityFunction.SinglePointContext singlePointContext = new DensityFunction.SinglePointContext(blockX, BiomeProximityGenerator.blockY, blockZ);
        float temperatureResult = firstBiomeOnly ? 0.0f : (float) dimData.getTemperatureNoise().compute(singlePointContext);
        float humidityResult = firstBiomeOnly ? 0.0f : (float) computeOriginal(singlePointContext); // always use original non-overridden densityfunctions for humidity
        float continentalnessResult = (float) dimData.getClimateSampler().continentalness().compute(singlePointContext);
        float erosionResult = firstBiomeOnly ? 0.0f : (float) dimData.getClimateSampler().erosion().compute(singlePointContext);
        float depthResult = firstBiomeOnly ? 0.0f : (float) dimData.getClimateSampler().depth().compute(singlePointContext);
        float weirdnessResult = firstBiomeOnly ? 0.0f : (float) dimData.getClimateSampler().weirdness().compute(singlePointContext);

        Climate.TargetPoint sampleResultRaw = Climate.target(temperatureResult, humidityResult, continentalnessResult, erosionResult, depthResult, weirdnessResult);
        return new ClimateTargetPointEx(sampleResultRaw, false);
    }

    public final Set<Holder<Biome>> getBiomesFirst() {
        return dimData.biomeOceans;
    }

    public final Set<Holder<Biome>> getBiomesSecond() {
        return dimData.biomeRivers;
    }

    public final void cancelAllCacheTasks() {
        gen.cancelAllCacheTasks();
    }

    @Override
    public final double compute(FunctionContext context) {
        if (dimData == null) {
            dimData = DIMENSION_DATA_CACHE.getIfPresent(dimensionName);
            if (dimData == null)
                TbUtils.doCrash("Couldn't find DimensionData on first compute! Eh?");
        }
        return gen.onCompute(context, dimData);
    }

    @Override
    public final void fillArray(double[] ds, ContextProvider contextProvider) {
        funcOriginal.fillArray(ds, contextProvider);
    }

    @Override
    public final @NotNull DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new DensityFunctionEx(dimensionName, functionName, config, funcOriginal.mapAll(visitor)));
    }

    @Override
    public final double minValue() {
        return funcOriginal.minValue();
    }

    @Override
    public final double maxValue() {
        return funcOriginal.maxValue();
    }

    @Override
    public final @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return funcOriginal.codec();
    }
}
