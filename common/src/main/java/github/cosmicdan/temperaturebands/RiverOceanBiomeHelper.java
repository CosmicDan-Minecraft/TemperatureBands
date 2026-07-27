package github.cosmicdan.temperaturebands;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static github.cosmicdan.temperaturebands.TemperatureBands.LOGGER;

public class RiverOceanBiomeHelper {
    private static final String noneStringForBiomeDump = " - [NONE]";

    public final Set<Holder<Biome>> biomeOceans = new HashSet<>();
    public final Set<Holder<Biome>> biomeRivers = new HashSet<>();
    private final BiomeParamSet oceanParamSet = new BiomeParamSet();
    private final BiomeParamSet riverParamSet = new BiomeParamSet();

    public RiverOceanBiomeHelper(ServerLevel level, @Nullable MultiNoiseBiomeSource biomeSource) {
        if (biomeSource != null) {
            // fetch appropriate biomes for humidity
            Set<Holder<Biome>> possibleBiomes = biomeSource.possibleBiomes();
            for (Holder<Biome> biomeHolder : possibleBiomes) {
                if (biomeHolder.is(BiomeTags.IS_RIVER)) {
                    biomeRivers.add(biomeHolder);
                } else if (biomeHolder.is(BiomeTags.IS_OCEAN)) {
                    biomeOceans.add(biomeHolder);
                }
            }
            // do dump if configured
            if (TemperatureBands.CONFIG_GLOBAL.dumpRiverAndOceanBiomes()) {
                LOGGER.info("List of all biomes with 'minecraft:is_river' tag for dimension '{}':", level.dimension().identifier());
                doDump(biomeRivers);
                LOGGER.info("List of all biomes with 'minecraft:is_ocean' tag for dimension '{}':", level.dimension().identifier());
                doDump(biomeOceans);
            }
            // make param sets for detecting river/oceans later. We do this because their params might be changed e.g. from CustomBiomeParameters mod
            for (Pair<Climate.ParameterPoint, Holder<Biome>> biomeParamPair : biomeSource.parameters().values()) {
                if (biomeParamPair.getSecond().is(BiomeTags.IS_RIVER)) {
                    riverParamSet.addParams(biomeParamPair.getFirst());
                } else if (biomeParamPair.getSecond().is(BiomeTags.IS_OCEAN)) {
                    oceanParamSet.addParams(biomeParamPair.getFirst());
                }
            }

        }
    }

    private void doDump(Set<Holder<Biome>> biomeSet) {
        if (biomeSet.isEmpty())
            LOGGER.info(noneStringForBiomeDump);
        else {
            for (Holder<Biome> biomeHolder : biomeSet) {
                if (biomeHolder.unwrapKey().isPresent()) {
                    LOGGER.info(" - {}", biomeHolder.unwrapKey().get().identifier());
                }
            }
        }
    }

    public boolean mightBeOcean(float continentalness) {
        return oceanParamSet.mightBeOcean(continentalness);
    }

    public boolean mightBeRiver(float weirdness) {
        return riverParamSet.mightBeRiver(weirdness);
    }

    private static class BiomeParamSet {
        RangeSet<Long> temperatures = TreeRangeSet.create();
        RangeSet<Long> humidities = TreeRangeSet.create();
        RangeSet<Long> continentalnesses = TreeRangeSet.create();
        RangeSet<Long> erosions = TreeRangeSet.create();
        RangeSet<Long> weirdnesses = TreeRangeSet.create();

        public void addParams(Climate.ParameterPoint paramPoint) {
            temperatures.add(Range.closed(paramPoint.temperature().min(), paramPoint.temperature().max()));
            humidities.add(Range.closed(paramPoint.humidity().min(), paramPoint.humidity().max()));
            continentalnesses.add(Range.closed(paramPoint.continentalness().min(), paramPoint.continentalness().max()));
            erosions.add(Range.closed(paramPoint.erosion().min(), paramPoint.erosion().max()));
            weirdnesses.add(Range.closed(paramPoint.weirdness().min(), paramPoint.weirdness().max()));
        }

        public boolean mightBeOcean(float continentalness) {
            return continentalnesses.contains((long) (continentalness * 10000));
        }

        public boolean mightBeRiver(float weirdness) {
            return weirdnesses.contains((long) (weirdness * 10000));
        }
    }
}
