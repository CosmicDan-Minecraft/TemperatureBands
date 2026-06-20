package github.cosmicdan.temperaturebands;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CommonConfig {
    public static final String sectionGlobal = "global";
    public static final String sectionGlobalTxt = """
             [global] settings are not world-specific. These will apply to all worlds and will not save per-world since they don't affect world generation.
             --------""";
    public final ModConfigSpec.BooleanValue doBenchmark;
    public static final String doBenchmarkTxt = """
             
             [doBenchmark] is only available for (Neo)Forge right now. If true and World Preview is installed, a benchmark will be performed when opening the 'Preview' tab
                (and after closing the World Preview settings menu, i.e. whenever chunk previews start to generate).
              - Refer to the mod description page for details and tips on benchmarking and stress-testing.""";
    public final ModConfigSpec.BooleanValue copyConfigOnRecreateWorld;
    public static final String copyConfigOnRecreateWorldTxt = """
             
             [copyConfigOnRecreateWorld] will, when true, copy the previous world Temperature Bands config to the new world when "Recreate" is being done.
              - Set this to false if you want to "upgrade" your world with new features/defaults when updating the mod.
              - Relevant for client only. Servers will need to manually recreate their worlds (i.e. set the same seed/settings in server config and delete old world).""";
    public final ModConfigSpec.ConfigValue<String> ignoreDimensionFailures; // TODO: Actually change to a blacklist and prefill with minecraft:the_end. Also make whitelist final values end with a comma.
    public static final String ignoreDimensionFailuresTxt = """

             [ignoreDimensionFailures] is a list of dimensions to ignore logging errors about when replacing their temperature/humidity functions fail.
              - Separate with commas. Trailing comma doesn't matter.
              - The default entry of The End is there since vanilla End uses a constant 0.0 for temperature (for some reason, though it doesn't use any humidity noise at all)
                and the mod will try to replace temperature anyway, giving a false 'report if you want support for this custom dimension' error. Please don't report that error :)""";
    public final ModConfigSpec.BooleanValue dumpRiverAndOceanBiomes;
    public static final String dumpRiverAndOceanBiomesTxt = """

             [dumpRiverAndOceanBiomes] will, if set to true, dump a list of all biomes with 'minecraft:is_river' and 'minecraft:is_ocean' tags from each dimension to console/log.
              - Might be useful for modpack creators (in relation to humidity algorithms)""";
    public static final String sectionClimateSamplerPerf = "climatesampler-performance";
    public static final String sectionClimateSamplerPerfTxt = """
             [climatesampler-performance] are global climate sampler settings related to performance (shared by all worlds, not saved per-world).
              - A custom climate sampler is used when calculating humidity based on proximity to rivers and/or oceans. These settings are currently only relevant if the 'advanced'
                humidity algorithm is enabled (which is the default for new worlds, but left disabled for existing worlds).
             --------""";
    public final ModConfigSpec.IntValue climateSamplerCacheSize;
    public static final String climateSamplerCacheSizeTxt = """

             [climateSamplerCacheSize] is the size of our custom climate sampler, in megabytes. Setting to zero will disable caching, which may result in better performance if your
                memory is slow/exhausted (probably not) or your have a low thread-count CPU (maybe).
              - The default of 5Mb can hold almost 50 thousand samples so should be more than enough for any environment.""";
    public final ModConfigSpec.IntValue climateSamplerCachePrefetchRadius;
    public static final String climateSamplerCachePrefetchRadiusTxt = """

             [climateSamplerCachePrefetchRadius] is, if non-zero (along with climateSamplerCacheSize also being non-zero), the rough radius to perform additional sampling (and
                caching) while caching-and-sampling the climate. This will be done asynchronously as units of 'climateSamplerResolution' (which is in the climatesampler-world
                section).
              - Negative numbers refer to a fraction of available CPU threads; the default of -8 will use one-eighth of threads as radius (actual thread count will be much higher
                but they're very short-lived - a few MS - and modern Java handles this well).
              - Highly recommended to leave enabled as it drastically improves world generation speed, but there are diminishing returns if set too high.
              - If you experience 'can't keep up' warnings while exploring new chunks, try reducing this value. A low fixed number like 1 or 2 could be better.
              - Result is rounded-down, meaning setting it too far negative could result in 0 which will disable prefetching. The default of -8 will do this if your CPU has *less*
                than 8 threads, which is probably appropriate.
              - Do note that World Preview (and new world creation) will be much slower at the very start, but the speed will improve over time and actually generate quicker
                overall, especially in high resolutions of World Preview and/or high chunk rendering distances. Reducing thread count in World Preview settings a little might help too.
              - Finally, if you want to stress-test your CPU, disable this completely with 0 and keep max-1 (or max) threads in World Preview.""";
    public static final String sectionWorld = "world";
    public static final String sectionWorldTxt = """
             [world] are general defaults for new worlds. These will apply to newly-generated worlds only, existing worlds will remember their own settings.
             --------""";
    public final ModConfigSpec.IntValue bandSize;
    public static final String bandSizeName = "configBandSize";
    public static final String bandSizeTxt = """
             
             [bandSize] is the average size of each temperature band, in blocks.
              - Vanilla Minecraft generation has 5 temperature zones - Freezing, Cool, Temperate, Warm and Hot - every biome fits into one of these five temperature ranges.
                For an example, when this value is 2048, the distance from the *center* of one one Hot band to the next Hot band center will be about 16384 (2048 * 8) blocks;
                Hot through to Freezing then back again.
              - If there are any worldgen mods that add biomes with a different 'temperature range' for placement, they should be incorporated within existing bands normally.""";
    public final ModConfigSpec.BooleanValue useVerticalBands;
    public static final String useVerticalBandsName = "configUseVerticalBands";
    public static final String useVerticalBandsTxt = """
             
             [useVerticalBands] will, if true, use vertical instead of horizontal.""";
    public final ModConfigSpec.DoubleValue bandPositionShift;
    public static final String bandPositionShiftName = "configPosShift";
    public static final String bandPositionShiftTxt = """
             
             [bandPositionShift] will shift the bands by the given percentage.
              - Normally the Hot band (Desert and such) will generate at origin (0,0) so the default shift of 25% will make the origin temperate (middle temp) instead, shifted
                downwards to simulate a 'Northern hemisphere' start (Colder will be North, or East if Vertical bands, and Hotter will be South or West). A value of 0.75 would
                simulate a southern hemisphere start instead.
              - Note that some world gen mods like Tectonic and Larion seem to have their own idea about initial world spawn and don't use world origin, in these cases you might
                want to use a mod like 'Biome Spawn Point' too.""";
    public final ModConfigSpec.DoubleValue tempRange;
    public static final String tempRangeName = "configTempRange";
    public static final String tempRangeTxt = """
             
             [tempRange] is the absolute min/max temperature range.
              - Increasing will make the coldest and hottest bands larger while reducing others, lowering will have the opposite effect.
              - The default value makes hottest/coldest bands a tiny bit smaller than the others.
                Setting this value too high or low might result in some bands not generating at all.""";
    public final ModConfigSpec.DoubleValue tempGradeShift;
    public static final String tempGradeShiftName = "configGradeShift";
    public static final String tempGradeShiftTxt = """
             
             [tempGradeShift] is used to make the hottest and coldest bands roughly the same size.
              - Vanilla temperature has a bias towards cold, this solves that. If e.g. you set it to zero, the coldest band will be larger than all other bands and the hottest
                would be smaller than all others.""";
    public final ModConfigSpec.IntValue bandAlgorithm;
    public static final String bandAlgorithmName = "configAlgorithm";
    public static final String bandAlgorithmTxt = """
             
             [bandAlgorithm] is the algorithm to use for temperature bands.
              - You can configure and read more about each algorithm in their own section below. Recommended to use World Preview if you want to change things.
              - Note that there is only 1 algorithm currently, this config is here just in case I add more later.""";
    public final ModConfigSpec.IntValue noiseFactor;
    public static final String noiseFactorName = "configNoiseFactor";
    public static final String noiseFactorTxt = """
             
             [noiseFactor] will, when above zero, incorporate original noise generation into temperature bands to help make the shape and edges of bands a bit nicer.
              - HIGHLY recommended to keep it on.
              - See each algorithm for details on how noiseFactor is used. The unit is arbitrary and very dependent on bandSize. For e.g. 100 is nice for a bandSize of 2048 but
                might be too wild if bandSize is decreased, or too tame if bandSize is increased.""";
    public final ModConfigSpec.IntValue distanceFunction;
    public static final String distanceFunctionName = "configDistanceFunction";
    public static final String distanceFunctionTxt = """

            [distanceFunction] provides a choice in algorithm for calculating distances (currently only used by the default/advanced humidity stuff).
             - A value of 1 is the default and uses "octile" distance which is reasonably fast and accurate.
             - A value of 0 uses "Manhattan" (or "taxi cab") distance which is faster but a bit inaccurate.
             - A value of 2 uses "real" or "Euclidean" distance via hypotenuse calculation, which is a bit slower but gives maximum accuracy.
             - Recommended to leave on 1 (octile). While Euclidean distance is only about 3% slower, it doesn't look any better - just "different".
               Although Euclidean might give better smoothness if you're using very high accuracy for humidity and climate sampler. Manhattan distance is about 5%
               faster than octile but looks kinda bad - can be very "noisy" and regularly "skips" biome transitions.""";
    public final ModConfigSpec.ConfigValue<String> dimBlacklist;
    public static final String dimBlacklistName = "configDimBlacklist";
    public static final String dimBlacklistTxt = """
             
             [dimBlacklist] specifies a blacklist of dimensions to exclude from *all* modifications.
              - Separate with commas. Trailing comma doesn't matter.
              - Default excludes the_nether and the_end.""";
    public final ModConfigSpec.BooleanValue dimBlacklistAsWhitelist;
    public static final String dimBlacklistAsWhitelistName = "configDimBlacklistAsWhitelist";
    public static final String dimBlacklistAsWhitelistTxt = """
             
             [dimBlacklistAsWhitelist] will, when true, use the above blacklist as a whitelist instead.""";

    public static final String sectionAlgo1 = "algorithm1";
    public static final String sectionAlgo1Txt = """
             [algorithm1] are settings for temperature algorithm 1, the 'simple' or 'default' algorithm.
              - As before, these settings are only the defaults for new worlds - each world will remember its own settings.
              - This algorithm is quite simple and produces jaggy lines that aren't too random or natural looking alone, but it looks pretty good as long as you keep noiseFactor.
              - Keeping the variance low but noiseFactor a decent amount (around 100 or so) will produce some decent looking curves/waves in the bands with some randomness, but if you
                want something more predictable you can reduce noiseFactor while optionally increasing variance.
             --------""";
    public final ModConfigSpec.IntValue algo1bandVariance;
    public static final String algo1bandVarianceName = "configAlgo1BandVariance";
    public static final String algo1bandVarianceTxt = """
             
             [configAlgo1BandVariance] is the band variance in blocks (roughly). If below 10, each band will have a completely straight edge across the world and will disable all
                remaining algo1 features.
              - You will want to keep this a fairly small value. Making it too large will result in weirdness, especially if it's too close to the bandSize.""";
    public final ModConfigSpec.DoubleValue algo1bandVarianceSteepness;
    public static final String algo1bandVarianceSteepnessName = "configAlgo1bandVarianceSteepness";
    public static final String algo1bandVarianceSteepnessTxt = """
             
             [algo1bandVarianceSteepness] is the "steepness" of band variance.
              - Unit is arbitrary, you'll want to experiment and test if you change it. Probably best left alone, though.""";

    public static final String sectionHumidityWorld = "humidity-world";
    public static final String sectionHumidityWorldTxt = """
             [humidity-world] are the world-specific settings for humidity (aka vegetation).
              - As before, these settings are only the defaults for new worlds - each world will remember its own settings.
             --------""";
    public final ModConfigSpec.IntValue humidityAlgorithm;
    public static final String humidityAlgorithmName = "configHumidityAlgorithm";
    public static final String humidityAlgorithmTxt = """
             
             [humidityAlgorithm] is the algorithm to use for humidity (aka vegetation).
              - You can configure and read more about each algorithm in their own section below. Recommended to use World Preview if you want to change things around.
              - A value of 2 (default) uses a "realistic" humidity calculation based on proximity to rivers and/or oceans. Can be a little slow, but still not as slow as many
                worldgen mods, and it's definitely worth it (in my opinion).
              - A value of 1 uses a much simpler 'humidity bands' algorithm that run perpendicular to temperature bands.
              - A value of 0 will disable humidity feature entirely, keeping vanilla random noise humidity.""";

    public static final String sectionHumidityAlgo1 = "humidity-algorithm1";
    public static final String sectionHumidityAlgo1Txt = """
             [humidity-algorithm1] are world-specific settings for the 'simple' humidity algorithm, i.e. humidity bands that run perpendicular to temperature.
             --------""";
    public final ModConfigSpec.BooleanValue humidityAlgo1MimicTemp;
    public static final String humidityAlgo1MimicTempName = "configHumidityAlgo1MimicTemp";
    public static final String humidityAlgo1MimicTempTxt = """

             [humidityAlgo1MimicTemp] will, when true, make humidity bands use the same parameters as temperature bands, with optional scaling.
              - Vanilla Minecraft generation has 5 humidity zones, just like temperature, so this works out well.
              - Currently only allows true, I feel like y'all won't care about having different band behaviour for humidity. You can still scale it below.
                Do note that the bands will still have their own randomness different to the temperature bands, assuming temperature noiseFactor is used (the vanilla humidity noise
                function will be used, though - not temperature noise)""";
    public final ModConfigSpec.DoubleValue humidityAlgo1MimicScale;
    public static final String humidityAlgo1MimicScaleName = "configHumidityAlgo1MimicScale";
    public static final String humidityAlgo1MimicScaleTxt = """

             [humidityAlgo1MimicScale] will scale the humidity bands, in relation to temperature bands.
              - The default of 0.5 for e.g. means that humidity bands will be half as big and twice as frequent as temperature bands (on average).""";
    public static final String sectionHumidityAlgo2 = "humidity-algorithm2";
    public static final String sectionHumidityAlgo2Txt = """
             [sectionHumidityAlgo2] are the world-specific settings for the 'advanced' humidity algorithm, i.e. humidity based on proximity to rivers and/or oceans.
             --------""";
    public final ModConfigSpec.IntValue humidityResolution;
    public static final String humidityResolutionName = "configHumidityResolution";
    public static final String humidityResolutionTxt = """
             
             [humidityResolution] is the accuracy or "resolution" for calculating the distance from river and/or ocean for a given area, where lower values means more accuracy.
              - Represented as a square root, i.e. the default of 4 means each 16x16 area (each chunk) will use the same distance values.
              - This is a performance vs accuracy choice but the default of 4 seems good; values lower than 4 start to become extremely expensive on CPU/worldgen time without
                much improvement in smoothness, and values above 4 start to become a bit "chunky". The max value of 8 (each 64x64 area having same humidity) results in a
                silly checkerboard look (but is very fast).
              - If you change this, be sure to check out "climateSamplerResolution" in the "[climatesampler-world]" section too - these two settings are closely related.""";
    public final ModConfigSpec.DoubleValue humidityRiverInfluence;
    public static final String humidityRiverInfluenceName = "configHumidityRiverInfluence";
    public static final String humidityRiverInfluenceTxt = """
             
             [humidityRiverInfluence] determines how much rivers should contribute to final humidity value.
              - The default of 0.4 means 40% of river closeness will be added to the base humidity that was determined by ocean closeness.
              - Setting to 0 will disable any river influence on humidity (only oceans will count) and give a whopping ~40% speed increase due to our ability to take some shortcuts
                when only searching for oceans (only needs to compute continentalness), but it does make worldgen quite a bit more boring and less realistic.
              - Values higher than the default makes biome placement very 'noisy' (until approaching 1.0), i.e. lots of scattered 'dots' of tiny biomes around where ocean and
                river distance thresholds intersect. The default 0.4 does this a bit too but not excessively, and it's actually kinda cool :)
              - Values at/above 1.0 will effectively double/multiply humidity values. Might be useful if you want a world where rivers count for "more humidity" than oceans.
              - Regardless of the value, keeping this enabled has the same cost in performance - so the choice of what influence to use, if any, is purely up to personal taste.""";
    public final ModConfigSpec.IntValue humiditySearchDistance;
    public static final String humiditySearchDistanceName = "configHumiditySearchDistance";
    public static final String humiditySearchDistanceTxt = """
             
             [humiditySearchDistance] is the maximal distance in blocks (on XZ/horizontal axes) from rivers and/or oceans to be considered as 'absolutely dry' (the
                lowest humidity). In other words, higher numbers will make humidity drop slower as distance increases from river and/or ocean biomes.
              - In plain terms, it is how far we will search for rivers and/or oceans from any given point, with final humidity being based on actualDistance/searchDistance.
              - Higher values will become exponentially more expensive on CPU/worldgen time, though it can be mitigated by reducing humidityResolution and/or
                climateSamplerResolution at the expense of worldgen quality (you'll see more "chunkyness" in biome borders). Alternatively, you can try adjusting
                climatesampler-performance settings to *maybe* improve efficiency at the expense of increased CPU/RAM load, which may actually end up reducing
                throughput/worldgen speed anyway.""";

    public static final String sectionClimateSamplerWorld = "climatesampler-world";
    public static final String sectionClimateSamplerWorldTxt = """
             [climatesampler-world] are the world-specific settings for the climate sampler. Currently only used by humidity.
                Unlike the climatesampler-performance settings, these DO affect worldgen.
             --------""";
    public final ModConfigSpec.IntValue climateSamplerResolution;
    public static final String climateSamplerResolutionName = "configClimateSamplerResolution";
    public static final String climateSamplerResolutionTxt = """
             
             [climateSamplerResolution] is the accuracy or "resolution" for sampling the climate.
              - Currently only used by ocean/river search in the advanced humidity algorithm.
              - The default of -1 means automatic, which is the cube of humidityResolution. This seems to be the most logical balance between quality and performance - biome edges
                look natural with sporadic patches of other biomes potential to the nearby humidity/temperature values to make blending a bit more exciting.
              - Setting this a value equal to humidityResolution will provide maximum accuracy but will become VERY expensive on CPU/worldgen time, though that might be desirable
                if you want biome edges to be smoother and defined with minimal biome 'patches'.
              - If you've raised humidityResolution (meaning less accuracy) for some performance, it might be useful to manually set this value to something closer to the new
                humidityResolution rather than leaving on automatic; such a change could result in better overall smoothness without significant efficiency loss. Do note that
                it's pointless to have this value lower than humidityResolution, though - all that will do is burn CPU time for no reason.'""";

    public CommonConfig(final ModConfigSpec.Builder builder) {
        builder.push(sectionGlobal).comment(sectionGlobalTxt);
        doBenchmark = builder
                .comment(doBenchmarkTxt)
                .define("doBenchmark", false);
        copyConfigOnRecreateWorld = builder
                .comment(copyConfigOnRecreateWorldTxt)
                .define("copyConfigOnRecreateWorld", true);
        ignoreDimensionFailures = builder
                .comment(ignoreDimensionFailuresTxt)
                .define("ignoreDimensionFailures", "minecraft:the_end,");
        dumpRiverAndOceanBiomes = builder
                .comment(dumpRiverAndOceanBiomesTxt)
                .define("dumpRiverAndOceanBiomes", false);
        builder.pop();

        builder.push(sectionClimateSamplerPerf).comment(sectionClimateSamplerPerfTxt);
        climateSamplerCacheSize = builder
                .comment(climateSamplerCacheSizeTxt)
                .defineInRange("climateSamplerCacheSize", 5, 0, 128);
        climateSamplerCachePrefetchRadius = builder
                .comment(climateSamplerCachePrefetchRadiusTxt)
                .defineInRange("climateSamplerCachePrefetchRadius", -8, -32, 32);
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
        distanceFunction = builder
                .comment(distanceFunctionTxt)
                .defineInRange("distanceFunction", 1, 0, 2);
        dimBlacklist = builder
                .comment(dimBlacklistTxt)
                .define("dimBlacklist", "minecraft:the_nether,minecraft:the_end,");
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
                .defineInRange("humidityResolution", 4, 1, 8);
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
