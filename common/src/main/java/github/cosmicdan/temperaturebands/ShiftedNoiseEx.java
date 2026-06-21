package github.cosmicdan.temperaturebands;

import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Objects;

public abstract class ShiftedNoiseEx implements DensityFunction {
    final String dimensionName;
    final DimensionConfig config;
    final DensityFunction shiftX;
    final DensityFunction shiftY;
    final DensityFunction shiftZ;
    final double xzScale;
    final double yScale;
    final NoiseHolder noise;

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

    public abstract double computeOriginal(FunctionContext context);

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
                ", noise={NoiseHolder{NoiseParameters=" + noise.noiseData().value() + ",NormalNoise=" + normalNoiseConfigString + "}}" +
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
