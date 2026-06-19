package github.cosmicdan.temperaturebands;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CommonConfig {
    public static final String sectionGlobal = "global";
    public static final String sectionGlobalTxt = " Global settings are not world-specific. These will apply to all worlds and will not save per-world since they don't affect world generation.";
    public final ModConfigSpec.BooleanValue doBenchmark;
    public static final String doBenchmarkTxt = " Only for (Neo)Forge. If true and World Preview is installed, a benchmark will be performed when opening the 'Preview' tab." +
            " Refer to the mod description page for details and tips.";
    public final ModConfigSpec.BooleanValue ignoreTheEndFailures;
    public static final String ignoreTheEndFailuresTxt = " When true, failures to replace temperature/humidity functions for these dimensions will be ignored (separate with commas, same format as blacklist). The default entry of The End is there since vanilla End uses a constant 0.0 for temperature\n" +
            " and doesn't use humidity noise at all, but the mod tries to replace temperature anyway giving a false 'report if you want support for this custom dimension' error. Just a little QoL for those who upgraded from old versions of Temperature Bands.";
    public final ModConfigSpec.BooleanValue dumpRiverAndOceanBiomes;
    public static final String dumpRiverAndOceanBiomesTxt = " If true, when loading into a world, the list of all biomes with 'minecraft:is_river' and 'minecraft:is_ocean' tags will be dumped to console/log for each dimension. Might be useful for modpack creators.";
    public static final String sectionClimateSamplerPerf = "climatesampler-performance";
    public static final String sectionClimateSamplerPerfTxt = " Global climate sampler settings related to performance (shared by all worlds, not saved per-world).\n" +
            " A custom climate sampler is used when calculating humidity based on proximity to rivers and/or oceans. These settings are currently only relevant if the 'advanced' humidity algorithm is enabled (which is the default for new worlds, but kept disabled for existing worlds).";
    public final ModConfigSpec.IntValue climateSamplerCacheSize;
    public static final String climateSamplerCacheSizeTxt = " Cache size of the custom climate sampler. Measured in number of points, not actual data size. Setting to zero will disable caching, which may result in better performance if your memory is slow/exhausted or your have a low thread-count CPU.\n" +
            " The default of 10 million can reach a maximum of about 80Mb memory usage (when full). Larger numbers seem to have greatly diminishing returns regardless of how much RAM you have (tested with 48GB of DDR4 @ 3600 MT/s)";
    public final ModConfigSpec.IntValue climateSamplerCachePrefetchRadius;
    public static final String climateSamplerCachePrefetchRadiusTxt = " If above zero, whenever climate sampling is needed, additional climate sampling (and caching) will be done asynchronously in this radius (as units of 'climateSamplerResolution' which is in the climatesampler-world section).\n" +
            " Highly recommended to leave enabled as it improves world generation speed, but don't set it too high (e.g. default of 1 means there'll probably be 8 threads calculating climate at the same time, though the threads are extremely short - a few ms at most). \n" +
            " Note that World Preview will be *slightly* slower by default due to thread starvation; reducing the thread count in World Preview can help with this (reducing by 4 threads seems best). Regular Minecraft gameplay is the main benefit of this setting.\n" +
            " If you want to stress-test your CPU, disable this completely and keep max-1 (or max) threads in World Preview.";
    public final ModConfigSpec.IntValue climateSamplerCacheExpirySeconds;
    public static final String climateSamplerCacheExpirySecondsTxt = " Lifetime of the cached climate sampler entries in seconds. Setting to zero will cause cache entries to expire immediately after one read (not recommended).\n" +
            " Setting to -1 will make cache entries last as long as they can until cache is full (where oldest cache entries will be replaced). Recommended to leave on default (5 minutes) since stale cache entries are useless once all nearby chunks have been generated.\n" +
            " Disabling with -1 might be useful if you want to stress-test your memory in World Preview; refer to the mod description page for details and tips on Benchmarking and Stress-testing.";
    public static final String sectionWorld = "world";
    public static final String sectionWorldTxt = " Default world settings. These will apply to newly-generated worlds only, existing worlds will remember their own settings.\n";
    public final ModConfigSpec.IntValue bandSize;
    public static final String bandSizeName = "configBandSize";
    public static final String bandSizeTxt = " The average size of each temperature band in blocks.\n" +
            " Vanilla Minecraft generation has 5 temperature zones - Freezing, Cool, Temperate, Warm and Hot - every biome fits into one of these five temperature ranges.\n" +
            " So for example, if this value is 2048, the distance from the *center* of one one Hot band to the next Hot band center will be about 16384 (2048 * 8) blocks (Hot through to Freezing and back again).\n" +
            " If there are any worldgen mods that add biomes with a different 'temperature range' for placement, they should be incorporated within existing bands normally.";
    public final ModConfigSpec.BooleanValue useVerticalBands;
    public static final String useVerticalBandsName = "configUseVerticalBands";
    public static final String useVerticalBandsTxt = " If true, bands will be vertical instead of horizontal.";
    public final ModConfigSpec.DoubleValue bandPositionShift;
    public static final String bandPositionShiftName = "configPosShift";
    public static final String bandPositionShiftTxt = " Shift the bands by this percentage.\n" +
            " Normally the Hot band (Desert and such) will generate at origin (0,0) so the default shift of 25% will make the origin temperate (middle temp) instead, shifted\n" +
            " downwards to simulate a 'Northern hemisphere' start (Colder will be North, or East if Vertical bands, and Hotter will be South or West). A value of 0.75 would simulate southern hemisphere start instead.\n" +
            " NOTE: Some world gen mods like Tectonic and Larion seem to have their own idea about initial world spawn and don't use origin, you might want a different mod like 'Biome Spawn Point' instead.";
    public final ModConfigSpec.DoubleValue tempRange;
    public static final String tempRangeName = "configTempRange";
    public static final String tempRangeTxt = " Absolute min/max temperature range. Increasing this will make the coldest and hottest bands larger while reducing others, lowering this value will have the opposite effect.\n" +
            " The default value makes the hottest/coldest bands a tiny bit smaller than the others.\n" +
            " Be careful, setting this value too high or too low could result in some bands not generating at all.";
    public final ModConfigSpec.DoubleValue tempGradeShift;
    public static final String tempGradeShiftName = "configGradeShift";
    public static final String tempGradeShiftTxt = " Vanilla temperature has a bias towards cold, this value is used to make the hottest and coldest roughly the same size.\n" +
            " If you set it to zero, the coldest band will be larger than all other bands and the hottest would be smaller than all others.\n" +
            " You probably wont want to change this, but I made it configurable anyway.";
    public final ModConfigSpec.IntValue bandAlgorithm;
    public static final String bandAlgorithmName = "configAlgorithm";
    public static final String bandAlgorithmTxt = " Algorithm to use. You can configure and read more about each algorithm in their own section below. Recommended to use World Preview if you want to change things around.\n" +
            " Note: There is only 1 algorithm currently, this config is here just in case I add more later.";
    public final ModConfigSpec.IntValue noiseFactor;
    public static final String noiseFactorName = "configNoiseFactor";
    public static final String noiseFactorTxt = " If above zero, will use original noise generation to help make the edges between bands a bit nicer.\n" +
            " See each algorithm for details on how noiseFactor is used.\n" +
            " The unit is arbitrary and very dependent on bandSize. For e.g. 100 is nice for a bandSize of 2048 but might be too wild if bandSize is decreased, or too tame if bandSize is increased.";
    public final ModConfigSpec.ConfigValue<String> dimBlacklist;
    public static final String dimBlacklistName = "configDimBlacklist";
    public static final String dimBlacklistTxt = " Specify a blacklist of dimensions, separated by commas. Default excludes the_nether and the_end.";
    public final ModConfigSpec.BooleanValue dimBlacklistAsWhitelist;
    public static final String dimBlacklistAsWhitelistName = "configDimBlacklistAsWhitelist";
    public static final String dimBlacklistAsWhitelistTxt = " If true, the above blacklist will be treated as a whitelist instead.";

    public static final String sectionAlgo1 = "algorithm1";
    public static final String sectionAlgo1Txt = " Settings for algorithm 1, the 'simple' or 'default' algorithm. As with world settings, these are only defaults for new worlds and they will be remembered per-world.\n" +
            " This algorithm is very simple and produces jaggy lines that aren't very random or natural looking, but it looks OK as long as you keep useNoise enabled.\n" +
            " Keeping the variance low but noiseFactor a decent amount (around 100 or so) will produce some OK looking curves/waves in the bands, but if you want something more predictable you can reduce noiseFactor and increase variance.\n";
    public final ModConfigSpec.IntValue algo1bandVariance;
    public static final String algo1bandVarianceName = "configAlgo1BandVariance";
    public static final String algo1bandVarianceTxt = " Band variance in blocks (roughly). If below 10, each band will have a completely straight edge across the world and disables all remaining algo1 features.\n" +
            " You will want to keep this a fairly small value. Making it too large could result in weirdness, especially if it's too close to the bandSize.";
    public final ModConfigSpec.DoubleValue algo1bandVarianceSteepness;
    public static final String algo1bandVarianceSteepnessName = "configAlgo1bandVarianceSteepness";
    public static final String algo1bandVarianceSteepnessTxt = " Steepness of band variance. Unit is arbitrary, you'll want to experiment and test if you change it. Probably best left alone though.";

    public static final String sectionHumidityWorld = "humidity-world";
    public static final String sectionHumidityWorldTxt = " World-specific settings for humidity. As with temperature, these settings are defaults for new worlds and each world will remember its own settings.";
    public final ModConfigSpec.IntValue humidityAlgorithm;
    public static final String humidityAlgorithmName = "configHumidityAlgorithm";
    public static final String humidityAlgorithmTxt = " Algorithm to use for humidity (aka vegetation). You can configure and read more about each algorithm in their own section below. Recommended to use World Preview if you want to change things around.\n" +
            " The default of 2 uses a 'realistic' humidity calculation based on proximity to rivers and/or oceans and can be a little slow (but still not as slow as many worldgen mods)\n" +
            " The other option, 1, uses a much simpler 'humidity bands' algorithm that run perpendicular to temperature bands.\n" +
            " Option 0 will disable humidity function entirely.";

    public static final String sectionHumidityAlgo1 = "humidity-algorithm1";
    public static final String sectionHumidityAlgo1Txt = " World-specific settings for the 'simple' humidity algorithm, i.e. humidity bands that run perpendicular to temperature.";
    public final ModConfigSpec.BooleanValue humidityAlgo1MimicTemp;
    public static final String humidityAlgo1MimicTempName = "configHumidityAlgo1MimicTemp";
    public static final String humidityAlgo1MimicTempTxt = " If true, humidity bands will use the same parameters as temperature bands, with optional scaling.\n" +
            " Currently only allows true, I feel like y'all won't care about having different band behaviour for humidity. You can still scale it with the other option.\n" +
            " Do note that the bands will still have their own randomness different to the temperature bands, assuming temperature noiseFactor is used (the vanilla humidity noise will be used for some randomness)";
    public final ModConfigSpec.DoubleValue humidityAlgo1MimicScale;
    public static final String humidityAlgo1MimicScaleName = "configHumidityAlgo1MimicScale";
    public static final String humidityAlgo1MimicScaleTxt = " Adjust the scale of humidity bands relative to temperature. The default of 0.5 for e.g. means that temperature bands will be half as big and twice as frequent.\n";

    public static final String sectionHumidityAlgo2 = "humidity-algorithm2";
    public static final String sectionHumidityAlgo2Txt = " World-specific settings for the 'advanced' humidity algorithm, i.e. humidity bands that are based on proximity to rivers and/or oceans.";
    public final ModConfigSpec.IntValue humidityResolution;
    public static final String humidityResolutionName = "configHumidityResolution";
    public static final String humidityResolutionTxt = " Lower values mean higher accuracy or 'resolution' for calculating the distance from river and/or ocean for a given area, represented as a square root (i.e. the default\n" +
            " of 8 means each 64x64 area will use the same distance values). Values lower than 4 start to become extremely expensive on CPU/worldgen time without much improvement in smoothness.";
    public final ModConfigSpec.DoubleValue humidityRiverInfluence;
    public static final String humidityRiverInfluenceName = "configHumidityRiverInfluence";
    public static final String humidityRiverInfluenceTxt = " How much rivers should contribute to final humidity value. I.e. default of 0.4 means 40% of river closeness will be added to the base humidity calculated from ocean\n" +
            " distance (which might be the lowest possible value). Setting to 0 will disable any river influence on humidity (and give a minor-to-moderate speed boost) but make world generation a bit more boring.\n" +
            " Values higher than the default makes biome placement get very 'noisy', i.e. lots of scattered 'dots' of tiny biomes around where ocean and river distance thresholds intersect. The default 0.4 does this a bit too\n" +
            " but not massively and it's actually kinda cool :)";
    public final ModConfigSpec.IntValue humiditySearchDistance;
    public static final String humiditySearchDistanceName = "configHumiditySearchDistance";
    public static final String humiditySearchDistanceTxt = " The upper distance in blocks on XZ (horizontal) axis from rivers and/or oceans to be considered as maximum 'dryness' (lowest humidity).\n" +
            " In other words, higher numbers will make humidity drop slower as distance increases from river/ocean biomes. Higher values will become more expensive on CPU/worldgen time, which can be mitigated by reducing humidityResolution\n" +
            " and/or climateSamplerResolution (below) but these will reduce worldgen quality; alternatively you can adjust climatesampler-performance settings to possibly increase performance at the expense of increased CPU/RAM load (which may\n" +
            " actually end up reducing throughput).";

    public static final String sectionClimateSamplerWorld = "climatesampler-world";
    public static final String sectionClimateSamplerWorldTxt = " World-specific settings for the climate sampler. Currently only used by humidity.\n" +
            " As with temperature, these settings are defaults for new worlds and each world will remember its own settings.\n" +
            " Unlike the climatesampler-performance settings, these settings DO modify world (humidity) generation.";
    public final ModConfigSpec.IntValue climateSamplerResolution;
    public static final String climateSamplerResolutionName = "configClimateSamplerResolution";
    public static final String climateSamplerResolutionTxt = " Lower values mean higher accuracy or 'resolution' for sampling the climate (currently only used by ocean/river search in advanced humidity algorithm).\n" +
            " The default of -1 means automatic, which is the cube of humidityResolution. This seems to be the most logical choice - biome edges look natural with sporadic patches and performance is still OK.\n" +
            " Setting this a value equal to humidityResolution will provide maximum accuracy but will become VERY expensive on CPU/worldgen time, however that might be desirable if you want biome edges to be \n" +
            " smoother and more defined with minimal biome 'patches'.";

    public CommonConfig(final ModConfigSpec.Builder builder) {
        builder.push(sectionGlobal).comment(sectionGlobalTxt);
        doBenchmark = builder
                .comment(doBenchmarkTxt)
                .define("doBenchmark", false);
        ignoreTheEndFailures = builder
                .comment(ignoreTheEndFailuresTxt)
                .define("ignoreTheEndFailures", true);
        builder.pop();
        dumpRiverAndOceanBiomes = builder.comment(dumpRiverAndOceanBiomesTxt).define("dumpRiverAndOceanBiomesTxt", false);

        builder.push(sectionClimateSamplerPerf).comment(sectionClimateSamplerPerfTxt);
        climateSamplerCacheSize = builder
                .comment(climateSamplerCacheSizeTxt)
                .defineInRange("climateSamplerCacheSize", 1000000, 0, Integer.MAX_VALUE);
        climateSamplerCachePrefetchRadius = builder
                .comment(climateSamplerCachePrefetchRadiusTxt)
                .defineInRange("climateSamplerCachePrefetchRadius", 2, 0, 8);
        climateSamplerCacheExpirySeconds = builder
                .comment(climateSamplerCacheExpirySecondsTxt)
                .defineInRange("climateSamplerCacheExpirySeconds", 300, -1, Integer.MAX_VALUE);
        builder.pop();

        builder.push(sectionWorld).comment(sectionWorldTxt);
        bandSize = builder
                .comment(bandSizeTxt)
                .defineInRange("bandSize", 2048, 512, 32768);
        useVerticalBands = builder
                .comment(useVerticalBandsTxt)
                .define("useVerticalBands", false);
        bandPositionShift = builder
                .comment(bandPositionShiftTxt)
                .defineInRange("bandPositionShift", 0.25, 0.0, 1.0);
        tempRange = builder
                .comment(tempRangeTxt)
                .defineInRange("tempRange", 0.65, 0.30, 1.0);
        tempGradeShift = builder
                .comment(tempGradeShiftTxt)
                .defineInRange("tempGradeShift", -0.05, -0.5, 0.5);
        bandAlgorithm = builder
                .comment(bandAlgorithmTxt)
                .defineInRange("bandAlgorithm", 1, 1, 1);
        noiseFactor = builder
                .comment(noiseFactorTxt)
                .defineInRange("noiseFactor", 100, 0, 1000);
        dimBlacklist = builder
                .comment(dimBlacklistTxt)
                .define("dimBlacklist", "minecraft:the_nether,minecraft:the_end");
        dimBlacklistAsWhitelist = builder
                .comment(dimBlacklistAsWhitelistTxt)
                .define("dimBlacklistAsWhitelist", false);
        builder.pop();

        builder.push(sectionAlgo1).comment(sectionAlgo1Txt);
        algo1bandVariance = builder
                .comment(algo1bandVarianceTxt)
                .defineInRange("bandVariance", 32, 0, 16384);
        algo1bandVarianceSteepness = builder
                .comment(algo1bandVarianceSteepnessTxt)
                .defineInRange("algo1bandVarianceSteepness", 0.2, 0.1, 10.0);
        builder.pop();

        builder.push(sectionHumidityWorld).comment(sectionHumidityWorldTxt);
        humidityAlgorithm = builder
                .comment(humidityAlgorithmTxt)
                .defineInRange("humidityAlgorithm", 2, 0, 2);
        builder.pop();

        builder.push(sectionHumidityAlgo1).comment(sectionHumidityAlgo1Txt);
        humidityAlgo1MimicTemp = builder
                .comment(humidityAlgo1MimicTempTxt)
                .define("humidityAlgo1MimicTemp", true);
        humidityAlgo1MimicScale = builder
                .comment(humidityAlgo1MimicScaleTxt)
                .defineInRange("humidityAlgo1MimicScale", 0.5, 0.1, 1.0);
        builder.pop();

        builder.push(sectionHumidityAlgo2).comment(sectionHumidityAlgo2Txt);
        humidityResolution = builder
                .comment(humidityResolutionTxt)
                .defineInRange("humidityResolution", 4, 1, 64);
        humidityRiverInfluence = builder
                .comment(humidityRiverInfluenceTxt)
                .defineInRange("humidityRiverInfluence", 0.4, 0.0, 5.0);
        humiditySearchDistance = builder
                .comment(humiditySearchDistanceTxt)
                .defineInRange("humiditySearchDistance", 512, 16, 16384);
        builder.pop();

        builder.push(sectionClimateSamplerWorld).comment(sectionClimateSamplerWorldTxt);
        climateSamplerResolution = builder
                .comment(climateSamplerResolutionTxt)
                .defineInRange("climateSamplerResolution", -1, -1, 64);
        builder.pop();
    }
}
