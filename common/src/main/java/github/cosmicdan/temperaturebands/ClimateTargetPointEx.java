package github.cosmicdan.temperaturebands;

import net.minecraft.world.level.biome.Climate;

public record ClimateTargetPointEx(
        Climate.TargetPoint targetPoint,
        boolean hasFinalHumidity
) {
    public int sizeWithLongKey() {
        return 32 + // size of a Long (key)
               64;  // size of Climate.TargetPoint (six primitive longs, plus object stuff and padding)
    }
}
