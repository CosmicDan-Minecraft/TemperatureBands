package github.cosmicdan.temperaturebands.densityfunctions;

import github.cosmicdan.temperaturebands.DimensionConfig;
import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.TbUtils;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static github.cosmicdan.temperaturebands.TemperatureBands.DIMENSION_DATA_CACHE;

public abstract class ShiftedNoiseEx implements DensityFunction {
    public final String dimensionName;
    public final DimensionConfig config;
    public final DensityFunction shiftX;
    public final DensityFunction shiftY;
    public final DensityFunction shiftZ;
    public final double xzScale;
    public final double yScale;
    public final NoiseHolder noise;

    public DimensionData dimData = null;

    public ShiftedNoiseEx(String dimensionName, DimensionConfig config, DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, NoiseHolder noise) {
        this.dimensionName = dimensionName;
        this.config = config;
        this.shiftX = shiftX;
        this.shiftY = shiftY;
        this.shiftZ = shiftZ;
        this.xzScale = xzScale;
        this.yScale = yScale;
        this.noise = noise;
    }

    abstract double onCompute(FunctionContext context);

    @Override
    public final double compute(FunctionContext context) {
        if (dimData == null) {
            dimData = DIMENSION_DATA_CACHE.getIfPresent(dimensionName);
            if (dimData == null)
                TbUtils.doCrash("Couldn't find DimensionData on first compute! Eh?");
        }
        return onCompute(context);
    }

    public double computeOriginal(DensityFunction.FunctionContext context) {
        double d = context.blockX() * xzScale + shiftX.compute(context);
        double e = context.blockY() * yScale + shiftY.compute(context);
        double f = context.blockZ() * xzScale + shiftZ.compute(context);
        return noise.getValue(d, e, f);
    }

    @Override
    public double minValue() {
        return -this.maxValue();
    }

    @Override
    public double maxValue() {
        return this.noise.maxValue();
    }

    @Override
    public @NotNull KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return DensityFunctions.ShiftedNoise.CODEC;
    }

    @Override
    public String toString() {
        String normalNoiseConfigString = null;
        if (noise.noise() == null) {
            normalNoiseConfigString = "null";
        } else {
            StringBuilder sb = new StringBuilder();
            noise.noise().parityConfigString(sb);
            normalNoiseConfigString = sb.toString();
        }
        return "ShiftedNoiseEx{" +
                "dimensionName='" + dimensionName + '\'' +
                ", shiftX=" + shiftX +
                ", shiftY=" + shiftY +
                ", shiftZ=" + shiftZ +
                ", xzScale=" + xzScale +
                ", yScale=" + yScale +
                ", densityfunctions={NoiseHolder{NoiseParameters=" + noise.noiseData().value() + ",NormalNoise=" + normalNoiseConfigString + "}}" +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShiftedNoiseEx that = (ShiftedNoiseEx) o;
        return Double.compare(xzScale, that.xzScale) == 0 &&
                Double.compare(yScale, that.yScale) == 0 &&
                Objects.equals(dimensionName, that.dimensionName) &&
                Objects.equals(shiftX, that.shiftX) &&
                Objects.equals(shiftY, that.shiftY) &&
                Objects.equals(shiftZ, that.shiftZ) &&
                Objects.equals(noise, that.noise);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimensionName, shiftX, shiftY, shiftZ, xzScale, yScale, noise);
    }
}
