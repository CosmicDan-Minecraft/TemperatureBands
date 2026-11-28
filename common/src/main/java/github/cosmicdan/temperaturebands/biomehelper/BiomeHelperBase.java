package github.cosmicdan.temperaturebands.biomehelper;

import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static github.cosmicdan.temperaturebands.TemperatureBands.LOGGER;

public abstract class BiomeHelperBase {
    private static final String noneStringForBiomeDump = " - [NONE]";

    protected void dumpBiomeInfoIfConfigured(ServerLevel level, Set<Holder<Biome>> biomeOceans, Set<Holder<Biome>> biomeRivers, @Nullable ParameterPointData pointData) {
        if (TemperatureBands.CONFIG_GLOBAL.dumpRiverAndOceanBiomes()) {
            LOGGER.info("List of all biomes with 'minecraft:is_ocean' tag for dimension '{}':", level.dimension().location());
            doDump(biomeOceans);
            if (pointData != null) {
                LOGGER.info("List of all climate dimension ranges for 'minecraft:is_ocean' biomes in dimension '{}':", level.dimension().location());
                for (Climate.ParameterPoint oceanPoint : pointData.oceanPoints) {
                    LOGGER.info(" - {}", oceanPoint.toString());
                }
            }
            LOGGER.info("List of all biomes with 'minecraft:is_river' tag for dimension '{}':", level.dimension().location());
            doDump(biomeRivers);
            if (pointData != null) {
                LOGGER.info("List of all climate dimension ranges for 'minecraft:is_river' biomes in dimension '{}':", level.dimension().location());
                for (Climate.ParameterPoint riverPoint : pointData.riverPoints) {
                    LOGGER.info(" - {}", riverPoint.toString());
                }
            }
        }
    }

    protected void doDump(Set<Holder<Biome>> biomeSet) {
        if (biomeSet.isEmpty())
            LOGGER.info(noneStringForBiomeDump);
        else {
            for (Holder<Biome> biomeHolder : biomeSet) {
                if (biomeHolder.unwrapKey().isPresent()) {
                    LOGGER.info(" - {}", biomeHolder.unwrapKey().get().location());
                }
            }
        }
    }
}
