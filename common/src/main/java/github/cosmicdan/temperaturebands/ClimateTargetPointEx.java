package github.cosmicdan.temperaturebands;

import net.minecraft.world.level.biome.Climate;

public record ClimateTargetPointEx(
        Climate.TargetPoint targetPoint,
        boolean hasFinalHumidity
) {}
