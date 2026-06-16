package github.cosmicdan.temperaturebands;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import org.jetbrains.annotations.NotNull;

import static github.cosmicdan.temperaturebands.TemperatureBands.*;

public class ShiftedNoiseHumidity extends ShiftedNoiseEx {
    public static final String NAME = "humidity (vegetation)";

    public ShiftedNoiseHumidity(String dimensionName, DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, NoiseHolder noise) {
        super(dimensionName, shiftX, shiftY, shiftZ, xzScale, yScale, noise);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public double compute(FunctionContext context) {
        return computeNoise(context);
    }

    private double computeNoise(FunctionContext context) {
        double d = context.blockX() * this.xzScale + this.shiftX.compute(context);
        double e = context.blockY() * this.yScale + this.shiftY.compute(context);
        double f = context.blockZ() * this.xzScale + this.shiftZ.compute(context);
        return this.noise.getValue(d, e, f);
    }

    @Override
    public void fillArray(double[] ds, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(ds, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(
                new ShiftedNoiseHumidity(
                        dimensionName, this.shiftX.mapAll(visitor), this.shiftY.mapAll(visitor), this.shiftZ.mapAll(visitor), this.xzScale, this.yScale, visitor.visitNoise(this.noise)
                )
        );
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
}
