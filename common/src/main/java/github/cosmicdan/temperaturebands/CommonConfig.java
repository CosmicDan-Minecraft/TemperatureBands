package github.cosmicdan.temperaturebands;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class CommonConfig {
    public static final String sectionGlobal = "global";
    public static final String sectionGlobalTxt = """
             [global] settings are not world-specific. These will apply to all worlds and will not save per-world since they don't affect world generation.
             --------""";
    public final ForgeConfigSpec.BooleanValue doBenchmark;
    public static final String doBenchmarkTxt = """
             
             [doBenchmark] will, if "World Preview (Unofficial Fork)" is installed, do a benchmark when opening the 'Preview' tab (and after closing the World Preview settings menu,
                i.e. whenever chunk previews start to generate).
              - Do not scroll or resize the window while World Preview is active, it will cause the benchmark to fail or never complete. To restart benchmark, simply enter
                the World Preview settings menu then exit out. Wait for all visible chunks to generate to see the results in the log/console.
              - This does have a small performance cost, which ALWAYS applies - even if World Preview is not active - so be sure to disable this once you're done testing.
              - The option "Enable drag preloading" must be disabled in World Preview settings for benchmark to work, which isn't the default. You will be reminded in the log output
                to do this if this still needs to be done (benchmarking won't work until you make this change).""";
    public final ForgeConfigSpec.BooleanValue doLevelLoadBenchmark;
    public static final String doLevelLoadBenchmarkTxt = """
             
             [doLevelLoadBenchmark] will, when true, perform a simple benchmark (time measurement) of initial level loading. In particular, it includes the
                generation/loading of spawn chunks.
              - Works on both server and client, unlike the World Preview benchmark.
              - While the result will appear on any level load, new or existing, it's only useful when creating a world (to record a baseline) and then recreating that
                world (i.e. same seed) with some changed worldgen settings (e.g. Temperature Bands config).
              - Enabled by default since it costs practically nothing. Only a single line will be written to the log once the level has finished loading.""";
    public final ForgeConfigSpec.BooleanValue copyConfigOnRecreateWorld;
    public static final String copyConfigOnRecreateWorldTxt = """
             
             [copyConfigOnRecreateWorld] will, when true, copy the previous world Temperature Bands config to the new world when "Recreate" is being done.
              - Set this to false if you want to "upgrade" your world with new features and defaults after updating the mod. You will still need to Recreate the world.
              - Relevant for client only. Servers will need to manually recreate their worlds (e.g. set the same seed/settings in server config and delete old world).
              - WARNING: If true, and your Stronghold config settings in the source world are different to the Stronghold settings in this config, the recreated world
                might use whatever setting is specified in this config file anyway (rather than the source world). To avoid this, restart the game client.""";
    public final ForgeConfigSpec.ConfigValue<String> ignoreDimensionFailures;
    public static final String ignoreDimensionFailuresTxt = """

             [ignoreDimensionFailures] is a list of dimensions to ignore logging errors about when replacing their temperature/humidity functions fail.
              - Separate with commas. Trailing comma doesn't matter.
              - The default entry of The End is there since vanilla End uses a constant 0.0 for temperature (for some reason, though it doesn't use any humidity noise at all)
                and the mod will try to replace temperature anyway, giving a false 'report if you want support for this custom dimension' error. Please don't report that error :)""";
    public final ForgeConfigSpec.BooleanValue dumpRiverAndOceanBiomes;
    public static final String dumpRiverAndOceanBiomesTxt = """

             [dumpRiverAndOceanBiomes] will, if set to true, dump a list of all biomes with 'minecraft:is_river' and 'minecraft:is_ocean' tags from each dimension to console/log.
              - Might be useful for modpack creators (in relation to humidity algorithms).
              - If the Refined Humidity algorithm is used, this will also dump the parameter ranges of each noise dimension for the detected ocean/river biomes.""";
    public final ForgeConfigSpec.BooleanValue showStrongholdProgress;
    public static final String showStrongholdProgressTxt = """
            
            [showStrongholdProgress] will, when true, show the progress of calculating Stronghold spawn locations when generating a new world on the client.
             - Just a small QoL feature. Some worldgen mods (e.g. Lithosphere) when used in combination with Advanced/Refined humidity can be VERY slow with this process.
               Advanced Humidity basically takes forever and is unusable, and Refined humidity can take 2 or more minutes for this process to finish.
             - Be sure to check out the [stronghold-generation-tweaks] section for options to speed up this process!""";

    public static final String sectionWorld = "world";
    public static final String sectionWorldTxt = """
             [world] are general defaults for new worlds. These will apply to newly-generated worlds only, existing worlds will remember their own settings.
             --------""";
    public final ForgeConfigSpec.IntValue bandSize;
    public static final String bandSizeName = "configBandSize";
    public static final String bandSizeTxt = """
             
             [bandSize] is the average size of each temperature band, in blocks.
              - Vanilla Minecraft generation has 5 temperature zones - Freezing, Cool, Temperate, Warm and Hot - every biome fits into one of these five temperature ranges.
                For an example, when this value is 2048, the distance from the *center* of one one Hot band to the next Hot band center will be about 16384 (2048 * 8) blocks;
                Hot through to Freezing then back again.
              - If there are any worldgen mods that add biomes with a different 'temperature range' for placement, they should be incorporated within existing bands normally.
              - If you want to maintain as much performance as possible, keep this as a power of two.""";
    public final ForgeConfigSpec.BooleanValue useVerticalBands;
    public static final String useVerticalBandsName = "configUseVerticalBands";
    public static final String useVerticalBandsTxt = """
             
             [useVerticalBands] will, if true, use vertical instead of horizontal.""";
    public final ForgeConfigSpec.DoubleValue bandPositionShift;
    public static final String bandPositionShiftName = "configPosShift";
    public static final String bandPositionShiftTxt = """
             
             [bandPositionShift] will shift the bands by the given percentage.
              - Normally the Hot band (Desert and such) will generate at origin (0,0) so the default shift of 25% will make the origin temperate (middle temp) instead, shifted
                downwards to simulate a 'Northern hemisphere' start (Colder will be North, or East if Vertical bands, and Hotter will be South or West). A value of 0.75 would
                simulate a southern hemisphere start instead.
              - Note that some world gen mods like Tectonic and Larion seem to have their own idea about initial world spawn and don't use world origin, in these cases you might
                want to use a mod like 'Biome Spawn Point' too.""";
    public final ForgeConfigSpec.DoubleValue tempRange;
    public static final String tempRangeName = "configTempRange";
    public static final String tempRangeTxt = """
             
             [tempRange] is the absolute min/max temperature range.
              - Increasing will make the coldest and hottest bands larger while reducing others, lowering will have the opposite effect.
              - The default value makes hottest/coldest bands a tiny bit smaller than the others.
                Setting this value too high or low might result in some bands not generating at all.""";
    public final ForgeConfigSpec.DoubleValue tempGradeShift;
    public static final String tempGradeShiftName = "configGradeShift";
    public static final String tempGradeShiftTxt = """
             
             [tempGradeShift] is used to make the hottest and coldest bands roughly the same size.
              - Vanilla temperature has a bias towards cold, this solves that. If e.g. you set it to zero, the coldest band will be larger than all other bands and the hottest
                would be smaller than all others.""";
    public final ForgeConfigSpec.IntValue bandAlgorithm;
    public static final String bandAlgorithmName = "configAlgorithm";
    public static final String bandAlgorithmTxt = """
             
             [bandAlgorithm] is the algorithm to use for temperature bands.
              - You can configure and read more about each algorithm in their own section below. Recommended to use World Preview if you want to change things.
              - Note that there is only 1 algorithm currently, this config is here just in case I add more later.""";
    public final ForgeConfigSpec.DoubleValue noiseFactor;
    public static final String noiseFactorName = "configNoiseFactor";
    public static final String noiseFactorTxt = """
             
             [noiseFactor] will, when above zero, incorporate original noise generation into temperature bands to help make the shape and edges of bands a bit nicer.
              - HIGHLY recommended to keep it on.
              - When specified as a decimal below 1.0, it is treated as a percentage of bandSize. The default of 5% seems to be a good value for any bandSize.
              - When specified as a number of 1 or above, it is a fixed "block number" amount and NOT dependent on bandSize. This is the old behavior (before 2.2.0), if you
                do this you should probably keep it around 5% of whatever your bandSize is - otherwise you might see some weirdness.""";
    public final ForgeConfigSpec.IntValue distanceFunction;
    public static final String distanceFunctionName = "configDistanceFunction";
    public static final String distanceFunctionTxt = """

            [distanceFunction] provides a choice in algorithm for calculating distances (currently only used by the default/advanced humidity stuff).
             - A value of 3 is the "Optimal" or "Euclidean Fast" distance, which uses square root. It is reasonably fast and very accurate, and the current default.
             - A value of 0 uses "Manhattan" (or "taxi cab") distance which is faster but a bit inaccurate.
             - A value of 1 is the old default and uses "octile" distance which is reasonably fast and accurate, but on the new advanced humidity code it creates polygonal
               edges at biome-humidity changes.
             - A value of 2 is the "Pure" or "Euclidean Safe" distance, which uses hypotenuse. It is the slowest but most accurate. Better to use 3, it is only kept for
               backwards compatibility.
             - Recommended to leave on 3. There isn't much performance difference between all of these, but 3 is only a tiny bit less accurate than 2 while much nicer than the
               others.""";
    public final ForgeConfigSpec.IntValue vanillaNoiseOverride;
    public static final String vanillaNoiseOverrideName = "configVanillaNoiseOverride";
    public static final String vanillaNoiseOverrideTxt = """
             
             [vanillaNoiseOverride] is used to override the base noise with vanilla-like Shifted Noise. I.e. the noise used with noiseFactor and humidityBaseNoisePercent.
              - The default value of 1 will only replace the noise if it's not already a ShiftedNoise type. Recommended.
              - A value of 2 will always replace the noise. Only recommended if you're using a world gen mod that happens to keep ShiftedNoise for
                temperature and humidity, but NOT recommended for vanilla world generation (see second-last point for why).
              - A value of 0 will disable this function. Not recommended (see next point why).
              - This is implemented to help re-introduce some variation to various worldgen mods, and usually improves performance a little. Without it, biomes can become extremely large
                and boring, and sometimes extremely slow to generate. Lithosphere is the best example of this (though Lithosphere is honestly not recommended in general because of how slow it is).
              - Setting to 2 (to always replace) is only recommended for non-vanilla worldgen because the replacement is *not* a 1:1 recreation of vanilla noise given the same seed.
              - This function is particularly useful for many world gen mods like Larion where their original temperature/humidity noises are very "gentle" which results in extremely straight bands.
                For some mods you may want to increase noiseFactor above the defaults.""";
    public final ForgeConfigSpec.ConfigValue<String> dimBlacklist;
    public static final String dimBlacklistName = "configDimBlacklist";
    public static final String dimBlacklistTxt = """
             
             [dimBlacklist] specifies a blacklist of dimensions to exclude from *all* modifications.
              - Separate with commas. Trailing comma doesn't matter.
              - Default excludes the_nether and the_end.
              - Adding minecraft:overworld won't do anything, it is always whitelisted.""";
    public final ForgeConfigSpec.BooleanValue dimBlacklistAsWhitelist;
    public static final String dimBlacklistAsWhitelistName = "configDimBlacklistAsWhitelist";
    public static final String dimBlacklistAsWhitelistTxt = """
             
             [dimBlacklistAsWhitelist] will, when true, use the above blacklist as a whitelist instead.
              - Adding minecraft:overworld is not necessary, it is always whitelisted.""";

    public static final String sectionAlgo1 = "algorithm1";
    public static final String sectionAlgo1Txt = """
             [algorithm1] are settings for temperature algorithm 1, the 'standard bands' algorithm.
              - As before, these settings are only the defaults for new worlds - each world will remember its own settings.
              - This algorithm is quite simple and produces jaggy lines that aren't too random or natural looking alone, but they look quite good as long as you keep noiseFactor.
              - Keeping the variance low but noiseFactor a decent amount (around 100 or so) will produce some decent looking curves/waves in the bands with some randomness, but if you
                want something more predictable you can reduce noiseFactor while optionally increasing variance.
             --------""";
    public final ForgeConfigSpec.IntValue algo1bandVariance;
    public static final String algo1bandVarianceName = "configAlgo1BandVariance";
    public static final String algo1bandVarianceTxt = """
             
             [configAlgo1BandVariance] is the band variance in blocks (roughly). If below 10, each band will have a completely straight edge across the world and will disable all
                remaining algo1 features.
              - You will want to keep this a fairly small value. Making it too large will result in weirdness, especially if it's too close to the bandSize.
              - If you want to maintain as much performance as possible, keep this as a power of two.""";
    public final ForgeConfigSpec.DoubleValue algo1bandVarianceSteepness;
    public static final String algo1bandVarianceSteepnessName = "configAlgo1bandVarianceSteepness";
    public static final String algo1bandVarianceSteepnessTxt = """
             
             [algo1bandVarianceSteepness] is the "steepness" of band variance.
              - Unit is arbitrary, you'll want to experiment and test if you change it. Probably best left alone, though.""";

    public static final String sectionHumidityWorld = "humidity-world";
    public static final String sectionHumidityWorldTxt = """
             [humidity-world] are the world-specific settings for humidity (aka vegetation).
              - As before, these settings are only the defaults for new worlds - each world will remember its own settings.
             --------""";
    public final ForgeConfigSpec.IntValue humidityAlgorithm;
    public static final String humidityAlgorithmName = "configHumidityAlgorithm";
    public static final String humidityAlgorithmTxt = """
             
             [humidityAlgorithm] is the algorithm to use for humidity (aka vegetation).
              - You can configure and read more about each algorithm in their own section below. Recommended to use World Preview if you want to change things around.
              - A value of 3 (default) is the "refined" algorithm, based on proximity factors like algorithm 2 but faster and nicer.
              - A value of 2 is a simple proximity check against rivers and/or oceans. It's not recommended as it has been succeeded by algorithm 3.
                It's still kept here for compatibility with older worlds, though.
              - A value of 1 uses a much simpler 'humidity bands' algorithm that run perpendicular to temperature bands.
              - A value of 0 will disable humidity feature entirely, keeping vanilla random noise humidity.""";
    public final ForgeConfigSpec.DoubleValue humidityTempWeight;
    public static final String humidityTempWeightName = "humidityTempWeight";
    public static final String humidityTempWeightTxt = """
             
             [humidityTempWeight] is how much the temperature will additionally influence humidity.
              - For humidity algorithm 3 (Refined) and 2 (Advanced) it introduces some biome variation between the warmer and cooler sides of the temperate
                (middle temperature) bands.
              - For humidity algorithm 1 (Simple, i.e. bands perpendicular to temperature), it applies some angle via narrowing/widening on the humidity bands - as you
                approach the cold band, lower-humidity biomes will become wider; as you approach the hot band, higher-humidity biomes will become wider.
              - Setting to 0.0 will disable this function, which may result in a *tiny* performance improvement.""";

    public static final String sectionHumidityAlgo1 = "humidity-algorithm1";
    public static final String sectionHumidityAlgo1Txt = """
             [humidity-algorithm1] are world-specific settings for the 'simple' banding humidity algorithm, i.e. humidity bands that run perpendicular to temperature.
             --------""";
    public final ForgeConfigSpec.DoubleValue humidityAlgo1MimicScale;
    public static final String humidityAlgo1MimicScaleName = "configHumidityAlgo1MimicScale";
    public static final String humidityAlgo1MimicScaleTxt = """

             [humidityAlgo1MimicScale] will scale the humidity bands, in relation to temperature bands.
              - The default of 0.5 for e.g. means that humidity bands will be half as big and twice as frequent as temperature bands (on average).""";
    public static final String sectionHumidityAlgo2 = "humidity-algorithm2";
    public static final String sectionHumidityAlgo2Txt = """
             [sectionHumidityAlgo2] are the world-specific settings for the 'advanced' humidity algorithm, i.e. the classic ocean/river proximity algorithm.
              - This has been largely succeeded by the new "refined" algorithm 3. Not recommended since it's pretty lousy, but kept for backwards-compatibility with
                older worlds.
              - This is a coarse, brute-force and random sampler/search algorithm. It's not very fast, not very accurate, and can regularly produce "artifacts" e.g.
                tiny and noisy patches of humid biomes.
              - Reminder: None of these settings apply to the new default "refined" humidity algorithm, see the new section "humidity-algorithm3" for configuring that.
             --------""";
    public final ForgeConfigSpec.DoubleValue humidityRiverInfluence;
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
    public final ForgeConfigSpec.IntValue humiditySearchDistance;
    public static final String humiditySearchDistanceName = "configHumiditySearchDistance";
    public static final String humiditySearchDistanceTxt = """
             
             [humiditySearchDistance] is the maximal distance in blocks (on XZ/horizontal axes) from rivers and/or oceans to be considered as 'absolutely dry' (the
                lowest humidity). In other words, higher numbers will make humidity drop slower as distance increases from river and/or ocean biomes.
              - In plain terms, it is how far we will search for rivers and/or oceans from any given point, with final humidity being based on actualDistance/searchDistance.
              - Higher values are more expensive on CPU/worldgen time, though it can be mitigated by increasing the humidityResolution value and/or climateSamplerResolution
                at the expense of worldgen quality (you'll see more "chunkyness" in biome borders). Alternatively, you can try adjusting climatesampler-performance settings
                to *maybe* improve efficiency at the expense of increased CPU/RAM load, which may actually end up reducing throughput/worldgen speed anyway.
              - The default value seems decent, but for worldgen mods that create larger landmasses and/or fewer rivers you will probably want to increase this.""";
    public final ForgeConfigSpec.DoubleValue humidityBaseNoisePercent;
    public static final String humidityBaseNoisePercentName = "humidityBaseNoisePercent";
    public static final String humidityBaseNoisePercentTxt = """
             
             [humidityBaseNoisePercent] is how much the original humidity noise will contribute to calculated proximity-based humidity. Default value of 0.1 means 10%.
              - Setting to 0.0 will disable use of the base noise, and provide a *very small* performance improvement (probably not even measurable).
              - This is not based on the resolution of humidity nor sampler so helps to provide a little random variation to the final result.
              - You'll probably want to keep this either fairly low (close to 0.0) or fairly high (close to 1.0), otherwise biome placement becomes very 'noisy', though
                setting higher values might defeat the purpose of using this algorithm at all.""";
    public final ForgeConfigSpec.DoubleValue humidityMiddleWeight;
    public static final String humidityMiddleWeightName = "humidityMiddleWeight";
    public static final String humidityMiddleWeightTxt = """
             
             [humidityMiddleWeight] is how much the near-final humidity value will be pulled towards the center.
              - The default value of 0.2 provides some nicer transitions between biomes while slightly reducing the sizes of the extreme-humidity biomes for middle
                temperatures (e.g. Dark Forest).
              - This adjustment happens before humidityTempWeight but after everything else.
              - Setting to 0.0 will disable this function, though it won't provide any performance benefit and biome transitions will be (subjectively) worse.""";

    public final ForgeConfigSpec.IntValue humidityResolution;
    public static final String humidityResolutionName = "configHumidityResolution";
    public static final String humidityResolutionTxt = """
             
             [humidityResolution] is the accuracy or "resolution" for calculating the distance from river and/or ocean for a given area, where lower values means more accuracy.
              - Represented as a square root, i.e. the default of 4 means each 16x16 area will use the same distance values.
              - This is a performance vs accuracy choice but the default of 4 seems decently balanced; values below 4 start to become extremely expensive on CPU/worldgen time
                with only small improvements in smoothness, and values above 4 get a bit too "chunky". The max value of 8 (each 64x64 area having same humidity) results in a
                silly checkerboard look (but is very fast).""";
    public final ForgeConfigSpec.IntValue climateSamplerCacheSize;
    public static final String climateSamplerCacheSizeTxt = """

             [climateSamplerCacheSize] is the size of the cache for this humidity algorithm, in megabytes (roughly).
              - The default of 5Mb can hold almost 50 thousand samples which should be more than enough for any environment.
              - Setting to zero will disable caching, which is NOT recommended - you will have much higher CPU *and* memory usage without the cache.""";
    public final ForgeConfigSpec.IntValue climateSamplerResolution;
    public static final String climateSamplerResolutionName = "configClimateSamplerResolution";
    public static final String climateSamplerResolutionTxt = """
             
             [climateSamplerResolution] is the accuracy or "resolution" for sampling the climate.
              - The default of -2 is an automatic "square value of humidityResolution". This seems to be the most logical balance between quality and performance - biome edges
                look natural with sporadic patches of other biomes potential to the nearby humidity/temperature values to make blending a bit more exciting.
              - A value of -1 is also possible, which was the old default; corresponds to the "cube value of humidityResolution" but it is VERY inaccurate and NOT recommended.
                It's best to keep this value no higher than the square of humidityResolution, i.e. -2, which results in 16 when humidityResolution is the default 4.
              - If setting manually, for best results, keep it as a multiple of humidityResolution. Otherwise there may be lots of weirdness in biome placement, or even humidity being
                wrongly "shifted".
              - Setting this to a value equal to humidityResolution will provide maximum accuracy but will become VERY expensive on CPU/worldgen time, though that might be desirable
                if you want all biome edges to be smoother and defined with minimal biome 'patches'.
              - If you've raised humidityResolution (meaning less accuracy) for some performance, it might be useful to manually set this value to something closer to the new
                humidityResolution rather than leaving on automatic; such a change could result in better overall smoothness without significant efficiency loss. Do note that
                it's pointless to have this value lower than humidityResolution, though - all that will do is make the CPU work harder for no gain.""";
    public final ForgeConfigSpec.BooleanValue climateSamplerShortcuts;
    public static final String climateSamplerShortcutsName = "climateSamplerShortcuts";
    public static final String climateSamplerShortcutsTxt = """
             
             [climateSamplerShortcuts] will, if true, perform some shortcuts during climate sampling to make it much faster.
              - Skips making some noise calculations that aren't relevant to oceans or rivers. Results in about 200% faster chunk generation.
                 - Oceans are based solely on continentalness, so we can skip generating other noises entirely when searching for oceans.
                 - Rivers can only generate with Valleys PV (weirdness noise between -0.05 and +0.05) so we check this first and abort early if it's not in this range.
                 - Temperature and Humidity noises are also skipped entirely because all ocean and river biomes can spawn in any temperature or humidity.
              - Enabling/disabling does technically result in changed world generation, but it's only in extremely specific circumstances and in tiny areas. Regardless, it will
                still be saved to world config.
              - Highly recommended to keep enabled, unless you're using a mod that changes the biome parameters of rivers and/or oceans.
              - This does include compatibility with Custom Biome Parameters, but ONLY if you've changed the weirdness params of Rivers or the continentalness params of Oceans.
                If your parameter changes to rivers/oceans include other parameters, then you must disable this - otherwise it may result in false positives/negatives.""";
    public static final String sectionHumidityAlgo3 = "humidity-algorithm3";
    public static final String sectionHumidityAlgo3Txt = """
             [sectionHumidityAlgo2] are the world-specific settings for the 'refined' humidity algorithm, i.e. humidity based on proximity to rivers and/or oceans.
              - Based on the "advanced" algorithm (algorithm2), but much faster, more accurate, and more interesting. The new default in 3.0.0.
             --------""";
    public final ForgeConfigSpec.IntValue humidity3_searchDistanceOceans;
    public static final String humidity3_searchDistanceOceansName = "humidity3_searchDistanceOceans";
    public static final String humidity3_searchDistanceOceansTxt = """
            
            [humidity3_searchDistanceOceans] is the maximal x/z block range to search for oceans.
             - If set to zero, ocean search will be disabled, meaning they will have no effect on humidity.
             - Searching for oceans is fairly fast, both for vanilla and most worldgen mods, since it's only based on continentalness noise which is usually cheap - so don't be
               afraid to increase this distance if you desire.""";
    public final ForgeConfigSpec.IntValue humidity3_searchDistanceRivers;
    public static final String humidity3_searchDistanceRiversName = "humidity3_searchDistanceRivers";
    public static final String humidity3_searchDistanceRiversTxt = """
            
            [humidity3_searchDistanceRivers] is the maximal x/z block range to search for rivers.
             - If set to zero, rivers search will be disabled, meaning they will have no effect on humidity.
             - Searching for rivers is much more expensive than oceans, since it's largely based on weirdness which is usually among the most expensive noises (especially for
               heavy worldgen mods like Lithosphere), so be careful with setting this higher.""";
    public final ForgeConfigSpec.DoubleValue humidity3_noiseWeightOcean;
    public static final String humidity3_noiseWeightOceanName = "humidity3_noiseWeightOcean";
    public static final String humidity3_noiseWeightOceanTxt = """
            
            [humidity3_noiseWeightOcean] is the weight of humidity provided by oceans.
             - Best considered as a ratio of how important or "heavy" the humidity of oceans are compared to river humidity. If river search is disabled, it acts as a
               baseline "max humidity" value before the final scaling (via noiseRange setting below) is performed.
             - If set to zero, will have no effect on humidity, BUT SEARCHING WILL STILL BE PERFORMED FOR NO REASON - set distance to 0 instead if you want to disable.
             - If you disable searching for rivers, you will want to increase this value (and/or noiseRange, below) otherwise you'll never get high-humidity biomes.""";
    public final ForgeConfigSpec.DoubleValue humidity3_noiseWeightRiver;
    public static final String humidity3_noiseWeightRiverName = "humidity3_noiseWeightRiver";
    public static final String humidity3_noiseWeightRiverTxt = """
            
            [humidity3_noiseWeightRiver] is the "value" of humidity provided by rivers.
             - As above, except for rivers instead of oceans; it is the value that river humidity provides relative to oceans (or, if ocean search is disabled, it
               acts as a baseline "max humidity" value before the final scaling via noiseRange is performed).""";
    public final ForgeConfigSpec.DoubleValue humidity3_noiseRange;
    public static final String humidity3_noiseRangeName = "humidity3_noiseRange";
    public static final String humidity3_noiseRangeTxt = """
            
            [humidity3_noiseRange] represents the absolute and final range of humidity that will be used.
             - Once the ocean/river humidity values are calculated via their weight and actual-distance-over-max-distance, they are combined, then scaled by this value
               and converted into a bipolar range. I.e. the default of 0.6 means the final humidity will always range from -0.6 to +0.6, regardless of what the literal
               noise weight values are for oceans/rivers (they will be re-scaled).
             - The default of 0.6 prevents the extremely wet or dry biomes from dominating the world too much. Increasing this will make them more common, and vice versa,
               but be careful not to set so low that wettest/dryest biomes can never be generated (with vanilla biome parameters that would happen around 0.4).
             - If you use a biome mod that adds new biomes at the extreme ends, you may want to increase this.""";
    public final ForgeConfigSpec.IntValue humidity3_cellSize;
    public static final String humidity3_cellSizeName = "humidity3_cellSize";
    public static final String humidity3_cellSizeTxt = """
            
            [humidity3_cellSize] is the size of each "macro cell" used during the search for oceans/rivers.
             - Lower values mean higher accuracy and more expensive search process. 64 seems to be enough for vanilla biome sizes.
             - Best kept as a power-of-two (e.g. 16, 32, 64, 128, etc.) but not strictly necessary.
             - If you're using a worldgen mod that makes biomes much bigger, you might be able to increase this to 128 or even 256 for
               improved performance without much loss of quality.
             - If you change this, you might want to change the cellSubsampling sizes below, too.""";
    public final ForgeConfigSpec.IntValue humidity3_cellSubsamplingOcean;
    public static final String humidity3_cellSubsamplingOceanName = "humidity3_cellSubsamplingOcean";
    public static final String humidity3_cellSubsamplingOceanTxt = """
            
            [humidity3_cellSubsamplingOcean] is the level of subsampling to do on a cell when an ocean is detected.
             - This is used to narrow-down a more accurate distance value for oceans, with higher values being more accurate and costing more performance.
             - It is directly related to the "macro" cellSize above. I.e. if cell size is changed, you might want to change this too.
             - The algorithm is an origin-based binary search with comparison on continentalness noise in order to find the nearest ocean, where this value specifies the
               maximum amount of times to shrink the "sub-cell" window.
             - The default value of 2 is suitable for oceans when cellSize is also at the default of 64, since oceans are quite large.
             - There is a soft upper-limit to this value, though it's dependent on cellSize and wont cause any issues if set too high (the algorithm will handle this fine);
               since the smallest sub-cell size can only be 2x2 (64x64 > 32x32 > 16x16 > 8x8 > 4x4 > 2x2 = max of 5 subsamples possible with starting cellSize of 64).
             - A value of zero will disable subsampling completely, which will result in a minor performance boost (for oceans specifically) but a sharp loss in accuracy -
               specifically, humidity distance will be calculated from the center of each cell (i.e. 64x64 by default).""";
    public final ForgeConfigSpec.IntValue humidity3_cellSubsamplingRiver;
    public static final String humidity3_cellSubsamplingRiverName = "humidity3_cellSubsamplingRiver";
    public static final String humidity3_cellSubsamplingRiverTxt = """
            
            [humidity3_cellSubsamplingRiver] is the level of subsampling to do on a cell when a river is detected.
             - This is used to narrow-down a more accurate distance value for rivers, with higher values being more accurate and costing more performance.
             - It is directly related to the "macro" cellSize above. I.e. if cell size is changed, you might want to change this too.
             - Algorithm is similar to Oceans, but since rivers are much smaller than oceans and can have sudden turns we generally want this to be higher than ocean subsampling.
               Searching for rivers is slightly different than oceans - for each macro cell (i.e. 64x64), we sample the four corners *and* the center of the cell, then we check
               if any of the 4 cell edges *or* the 4 lines from each corner to the center have a zero-crossing of weirdness - if so, it means a valley PV exists in this cell,
               so there is a strong possibility that a river exists here too. Then we home-in on the lowest weirdness that still fits within river biome parameters with a bias
               towards the origin (in order to find the closest river - this part is the same as ocean searches).
             - As with ocean subsampling, there is a soft upper-limit to this value, though it's dependent on cellSize and wont cause any issues if set too high.""";
    public final ForgeConfigSpec.IntValue humidity3_cacheSize;
    public static final String humidity3_cacheSizeName = "humidity3_cacheSize";
    public static final String humidity3_cacheSizeTxt = """
            
            [humidity3_cacheSize] is the size of the cache when using refined humidity.
             - Represented as a power-of-two for number of entries. The default of 17 equates to 131,072 cache entries, which is about 12 megabytes of RAM.
             - Unlike the previous humidity algorithm, we no longer use a heavy-weight Window-LFU cache but a "directly-mapped atomic ring buffer". While it may not be as
               space-efficient as the older cache, it is MUCH faster thanks to being completely lock-free via hardware memory visibility (i.e. CAS writes) and zero overheads.
             - For the above reason, the size needs to be bigger than the old cache to be effective, and increasing the size more could measurably improve performance during
               heavy worldgen tasks (e.g. chunk pregeneration and World Preview).
             - Also note that this cache is utilized by cache recycle below, which makes the slightly-higher RAM usage for this cache more than worth it.""";
    public final ForgeConfigSpec.BooleanValue humidity3_cacheRecycleWorld;
    public static final String humidity3_cacheRecycleWorldName = "humidity3_cacheRecycleWorld";
    public static final String humidity3_cacheRecycleWorldTxt = """
            
            [humidity3_cacheRecycleWorld] will, when true, "recycle" cached climate values from humidity searches into the world chunk generation process.
             - Improves in-game chunk generation time, drastically so for heavy world-gen mods like Lithosphere. It costs nothing to have, so only disable if it causes
               crashes with other tweaking/performance mods (but it shouldn't, if it does please report and I'll try and fix it).
             - Note that World Preview will not be any faster at all from this, since it doesn't do 3D sampling. But if you enable level-load benchmark, you'll see that the
               time taken to load into a newly-created world for e.g. (where spawn chunks need to be generated) happens much faster.
             - Trivia: Took a lot of work to get this right but it was worth it! For e.g. we have to detect 2D/3D noise parameters at runtime, then only cache values that
               are based on 2D noise (most are) since that's all the humidity cache needs; whereas 3D noise parameters (like depth) are always fresh and never cached.""";
    public final ForgeConfigSpec.IntValue humidity3_shortcutsLevel;
    public static final String humidity3_shortcutsLevelName = "humidity3_shortcutsLevel";
    public static final String humidity3_shortcutsLevelTxt = """
            
            [humidity3_shortcutsLevel] is the level of shortcuts to take during climate sampling.
             - Higher values represent more shortcuts taken, at the cost of possible false positives/negatives when searching for oceans/rivers.
             - A value of 0 will disable all shortcuts, meaning all climate dimensions will always be computed when sampling for humidity. Only recommended as a last resort.
             - A value of 1 will enable all shortcuts. Only the relevant biome parameters for oceans/rivers will be calculated from noise, along with a fast biome check
               against known ocean/river biome parameters.
             - Currently only those two options for now, I may add more in future if needed.""";
    public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> humidity3_climateDimensionSampleOrderOcean;
    public static final String humidity3_climateDimensionSampleOrderOceanName = "humidity3_climateDimensionSampleOrderOcean";
    public static final String humidity3_climateDimensionSampleOrderOceanTxt = """
            
            [humidity3_climateDimensionSampleOrderOcean] is the order of climate dimensions to sample for when searching for oceans.
             - This is an advanced setting for fine-tuning performance *if* you use mods that drastically change biome parameters.
             - The default value is suitable for vanilla and most mods.
             - This setting only has relevance when certain shortcuts are enabled (see humidity3_shortcutsLevel for specifics). It may also act as the default
               dimension-sample order in some cases under other shortcut levels.
             - Must be six numbers, with each number representing the standard 6 climate dimensions:
                 0 = Temperature
                 1 = Humidity
                 2 = Continentalness
                 3 = Erosion
                 4 = Depth
                 5 = Weirdness
             - Depth (4) and Humidity (5) can be anywhere, since Depth is ignored in the 2D biome space which humidity is based on, and obviously humidity can't sample
               itself (so is also skipped);
             - For oceans, continentalness is first since it's the first defining factor for oceans (with vanilla biome parameters it's the only defining factor; so if the
               shortcutsLevel is appropriate then only continentalness needs to be computed when searching for oceans).
             - Default: [2, 0, 3, 5, 4, 1]""";
    public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> humidity3_climateDimensionSampleOrderRiver;
    public static final String humidity3_climateDimensionSampleOrderRiverName = "humidity3_climateDimensionSampleOrderRiver";
    public static final String humidity3_climateDimensionSampleOrderRiverTxt = """
            
            [humidity3_climateDimensionSampleOrderRiver] is the order of climate dimensions to sample for when searching for rivers.
             - For rivers, continentalness is first since it's generally the fastest noise to compute, and it needs to resolve as non-ocean. Second is weirdness, and while
               this is often the most expensive climate dimension to sample, it is the next most-important dimension for rivers (i.e. Valleys PV range). Next is erosion, since
               that is also sometimes important, though this could be different with biome mods (along with the remaining dimensions).
             - For more information, refer to the similar config option above for oceans (humidity3_climateDimensionSampleOrderOcean).
             - Default: [2, 5, 3, 0, 4, 1]""";

    public static final String sectionBandLimit = "bandLimit";
    public static final String sectionBandLimitTxt = """
             [sectionBandLimit] allows you to set a limit on the temperature bands generated; meaning that a fixed temperature will be used after X number of bands.
              - Can be useful for simulating or complementing world borders, and are accurate based on band size. For instance, if bandSize was 2048 (default), bandPositionShift
                is 0.25 (default) and the bandLimitUpperValue was 2, then the upper "border" starts at 4096 blocks North (Z = -4096) - assuming vertical bands are not enabled.
             --------""";
    public final ForgeConfigSpec.IntValue bandLimitUpperCount;
    public static final String bandLimitUpperCountName = "bandLimitUpperCount";
    public static final String bandLimitUpperCountTxt = """
             
             [bandLimitUpperCount] is used to set the "upper" limit of temperature bands.
              - This is for bands in the Northern direction, or the Eastern direction if using vertical temperature bands.
              - The default of 0 means disabled (no limit).
              - Requires a little bit of math since it DOES consider the current 'bandPositionShift' setting. For example, lets say you're using the default 'bandPositionShift'
                value of 0.25 (where origin is in the temperate zone of 'northern hemisphere'), and you want to simulate a "real earth-like" world where there is only a single
                "hot" band and two "cold" bands at the edges, you would set this value to 2, because the first "cold" band towards the North (or East) is 2 bands away.""";
    public final ForgeConfigSpec.DoubleValue bandLimitUpperValue;
    public static final String bandLimitUpperValueName = "bandLimitUpperValue";
    public static final String bandLimitUpperValueTxt = """
             
             [bandLimitUpperValue] is the forced temperature value for the upper temperature band limit.
              - This is for bands in the Northern direction, or the Eastern direction if using vertical temperature bands.
              - The default of -1.0 means "coldest".
              - For the example already given in bandLimitUpperCount, i.e. a value of 2 to simulate 'real-world and northern-hemisphere start', you will want to keep this on -1.0.""";
    public final ForgeConfigSpec.IntValue bandLimitLowerCount;
    public static final String bandLimitLowerCountName = "bandLimitLowerCount";
    public static final String bandLimitLowerCountTxt = """
             
             [bandLimitLowerCount] is used to set the "lower" limit of temperature bands.
              - This is for bands in the Southern direction, or the Western direction if using vertical temperature bands.
              - The default of 0 means disabled (no limit).
              - Requires a little bit of math since it DOES consider the current 'bandPositionShift' setting. For example, if following the example already explained in
                'bandLimitUpperCount' (i.e. simulate 'real-world and northern-hemisphere start'), you would set this to 6, because the first "cold" band towards the South (or West)
                is 6 bands away.""";
    public final ForgeConfigSpec.DoubleValue bandLimitLowerValue;
    public static final String bandLimitLowerValueName = "bandLimitLowerValue";
    public static final String bandLimitLowerValueTxt = """
             
             [bandLimitLowerValue] is the forced temperature value for the lower temperature band limit.
              - This is for bands in the Southern direction, or the Western direction if using vertical temperature bands.
              - The default of -1.0 means "coldest".
              - For the example already given in bandLimitLowerCount, i.e. a value of 6 to simulate 'real-world and northern-hemisphere start', you will want to keep this on -1.0.""";

    public static final String sectionStronghold = "stronghold-generation-tweaks";
    public static final String sectionStrongholdTxt = """
             [stronghold-generation-tweaks] provides some optional tweaks to Stronghold generation.
              - Not really related to any features of this mod, but it can be very useful if you're using a heavy worldgen mod (e.g. Lithosphere) and are sick of waiting for Strongholds
                whenever you create a new world.
              - Stronghold calculation is fast on vanilla worldgen (and simple worldgen mods) but is a severe bottleneck on initial world creation for heavy worldgen mods like 
                Lithosphere.
              - Refer to the wiki article for background info on how Strongholds are generated: https://minecraft.wiki/w/Stronghold#Java_Edition
              - WARNING: When recreating a world, this function can be buggy if the source world config and mod default config (i.e. this file) differ, regardless of what
                'copyConfigOnRecreateWorld' is set to. It seems to happen only if you recreate a world after loading/recreating a world beforehand without restarting the entire game.
                If in doubt, always restart Minecraft when you want to recreate a final "ready to play" world if the source world config and this mod config differ.
             --------""";
                // TODO: Look into that. Is Minecraft caching stronghold spawn positions for the same seed or something?
    public final ForgeConfigSpec.IntValue strongholdCalcOverride;
    public static final String strongholdCalcOverrideName = "strongholdCalcOverride";
    public static final String strongholdCalcOverrideTxt = """

            [strongholdCalcOverride] provides options for overriding the initial stronghold spawn calculation process.
             - See the section text above, specifically the web link, for context info on how Stronghold calculation is done in vanilla.
             - A setting of 0 will leave Stronghold spawn calculation untouched, i.e. each spawn candidate will search nearby for biomes in the "#stronghold_biased_to" tag.
               Keep this default setting if you experience crashes or other problems, you're probably using another mod that modifies the same code area.
             - A setting of 1 will change Stronghold spawn calculation to immediately use the given chunk without any ideal spawn search at all. This is the fastest option, but it
               will often mean a Stronghold spawns in the ocean. A good option to have if you're working on a mod(pack) and want new world gen to be as fast as possible when testing.
             - A setting of 2 will replicate vanilla logic, but will allow 'strongholdCalcRange' from below to function.
             - A setting of 3 will replicate vanilla logic, but instead check "any land", which is much faster than vanilla's full biome sampling while still trying to keep them out of
               oceans most of the time (but they could still spawn near/under coastlines, or under rivers/swamps). It is a decent option to use for heavy worldgen like Lithosphere.
                - The "any land" condition is satisfied by sampling for continentalness, and checking if that continentalness value is "above" any biome that exists in the #is_ocean
                  biome tag.
                - Consider increasing the "strongholdCalcRange" setting below if using this (if you really want to make sure Strongholds don't spawn in oceans) since it's much faster
                  than the full biome sampling that vanilla usually does.
            """;
    public final ForgeConfigSpec.IntValue strongholdCalcRange;
    public static final String strongholdCalcRangeName = "strongholdCalcRange";
    public static final String strongholdCalcRangeTxt = """
            
            [strongholdCalcRange] allows you to override the distance when searching for ideal Stronghold spawns.
             - The default of 0 means use the unmodified vanilla value, which is %s (measured as a block radius).
             - This setting only works if the above 'strongholdCalcOverride' setting is 2 or above.""".formatted(PlatformConstants.STRONGHOLD_SEARCH_RANGE_DEFAULT);

    public CommonConfig(final ForgeConfigSpec.Builder builder) {
        builder.push(sectionGlobal).comment(sectionGlobalTxt);
        doBenchmark = builder
                .comment(doBenchmarkTxt)
                .define("doBenchmark", false);
        doLevelLoadBenchmark = builder
                .comment(doLevelLoadBenchmarkTxt)
                .define("doLevelLoadBenchmark", true);
        copyConfigOnRecreateWorld = builder
                .comment(copyConfigOnRecreateWorldTxt)
                .define("copyConfigOnRecreateWorld", true);
        ignoreDimensionFailures = builder
                .comment(ignoreDimensionFailuresTxt)
                .define("ignoreDimensionFailures", "minecraft:the_end,");
        dumpRiverAndOceanBiomes = builder
                .comment(dumpRiverAndOceanBiomesTxt)
                .define("dumpRiverAndOceanBiomes", false);
        showStrongholdProgress = builder
                .comment(showStrongholdProgressTxt)
                .define("showStrongholdProgress", true);
        builder.pop();

        builder.push(sectionWorld).comment(sectionWorldTxt);
        bandSize = builder
                .comment(bandSizeTxt + "\nDefault: 2048")
                .defineInRange("bandSize", 2048, 512, 32768);
        useVerticalBands = builder
                .comment(useVerticalBandsTxt)
                .define("useVerticalBands", false);
        bandPositionShift = builder
                .comment(bandPositionShiftTxt + "\nDefault: 0.25")
                .defineInRange("bandPositionShift", 0.25, 0.0, 1.0);
        tempRange = builder
                .comment(tempRangeTxt + "\nDefault: 0.65")
                .defineInRange("tempRange", 0.65, 0.30, 1.0);
        tempGradeShift = builder
                .comment(tempGradeShiftTxt + "\nDefault: -0.05")
                .defineInRange("tempGradeShift", -0.05, -0.5, 0.5);
        bandAlgorithm = builder
                .comment(bandAlgorithmTxt + "\nDefault: 1")
                .defineInRange("bandAlgorithm", 1, 1, 1);
        noiseFactor = builder
                .comment(noiseFactorTxt + "\nDefault: 0.05")
                .defineInRange("noiseFactor", 0.05, 0, 1000);
        distanceFunction = builder
                .comment(distanceFunctionTxt + "\nDefault: 3")
                .defineInRange("distanceFunction", 3, 0, 3);
        dimBlacklist = builder
                .comment(dimBlacklistTxt)
                .define("dimBlacklist", "minecraft:the_nether,minecraft:the_end,");
        dimBlacklistAsWhitelist = builder
                .comment(dimBlacklistAsWhitelistTxt)
                .define("dimBlacklistAsWhitelist", false);
        vanillaNoiseOverride = builder
                .comment(vanillaNoiseOverrideTxt + "\nDefault: 1")
                .defineInRange("vanillaNoiseOverride", 1, 0, 2);
        builder.pop();

        builder.push(sectionAlgo1).comment(sectionAlgo1Txt);
        algo1bandVariance = builder
                .comment(algo1bandVarianceTxt + "\nDefault: 32")
                .defineInRange("bandVariance", 32, 0, 16384);
        algo1bandVarianceSteepness = builder
                .comment(algo1bandVarianceSteepnessTxt + "\nDefault: 0.2")
                .defineInRange("algo1bandVarianceSteepness", 0.2, 0.1, 10.0);
        builder.pop();

        builder.push(sectionHumidityWorld).comment(sectionHumidityWorldTxt);
        humidityAlgorithm = builder
                .comment(humidityAlgorithmTxt + "\nDefault: 2")
                .defineInRange("humidityAlgorithm", 2, 0, 2);
        humidityTempWeight = builder
                .comment(humidityTempWeightTxt + "\nDefault: 0.4")
                .defineInRange("humidityTempWeight", 0.4, 0.0, 1.0);
        builder.pop();

        builder.push(sectionHumidityAlgo1).comment(sectionHumidityAlgo1Txt);
        humidityAlgo1MimicScale = builder
                .comment(humidityAlgo1MimicScaleTxt + "\nDefault: 0.5")
                .defineInRange("humidityAlgo1MimicScale", 0.5, 0.1, 1.0);
        builder.pop();

        builder.push(sectionHumidityAlgo2).comment(sectionHumidityAlgo2Txt);
        humidityRiverInfluence = builder
                .comment(humidityRiverInfluenceTxt + "\nDefault: 0.4")
                .defineInRange("humidityRiverInfluence", 0.4, 0.0, 5.0);
        humiditySearchDistance = builder
                .comment(humiditySearchDistanceTxt + "\nDefault: 600")
                .defineInRange("humiditySearchDistance", 600, 16, 16384);
        humidityBaseNoisePercent = builder
                .comment(humidityBaseNoisePercentTxt + "\nDefault: 0.1")
                .defineInRange("humidityBaseNoisePercent", 0.1, 0.0, 1.0);
        humidityMiddleWeight = builder
                .comment(humidityMiddleWeightTxt + "\nDefault: 0.3")
                .defineInRange("humidityMiddleWeight", 0.3, 0.0, 1.0);
        humidityResolution = builder
                .comment(humidityResolutionTxt + "\nDefault: 4")
                .defineInRange("humidityResolution", 4, 1, 8);
        climateSamplerCacheSize = builder
                .comment(climateSamplerCacheSizeTxt + "\nDefault: 5")
                .defineInRange("climateSamplerCacheSize", 5, 0, 128);
        climateSamplerResolution = builder
                .comment(climateSamplerResolutionTxt + "\nDefault: -2")
                .defineInRange("climateSamplerResolution", -2, -2, 64);
        climateSamplerShortcuts = builder
                .comment(climateSamplerShortcutsTxt)
                .define("climateSamplerShortcuts", true);
        builder.pop();

        builder.push(sectionHumidityAlgo3).comment(sectionHumidityAlgo3Txt);
        humidity3_searchDistanceOceans = builder
                .comment(humidity3_searchDistanceOceansTxt + "\nDefault: 512")
                .defineInRange(humidity3_searchDistanceOceansName, 512, 0, 8192);
        humidity3_searchDistanceRivers = builder
                .comment(humidity3_searchDistanceRiversTxt + "\nDefault: 256")
                .defineInRange(humidity3_searchDistanceRiversName, 256, 0, 8192);
        humidity3_noiseWeightOcean = builder
                .comment(humidity3_noiseWeightOceanTxt + "\nDefault: 0.4")
                .defineInRange(humidity3_noiseWeightOceanName, 0.4, 0.0, 1.0);
        humidity3_noiseWeightRiver = builder
                .comment(humidity3_noiseWeightRiverTxt + "\nDefault: 0.3")
                .defineInRange(humidity3_noiseWeightRiverName, 0.3, 0.0, 1.0);
        humidity3_noiseRange = builder
                .comment(humidity3_noiseRangeTxt + "\nDefault: 0.6")
                .defineInRange(humidity3_noiseRangeName, 0.6, 0.1, 1.0);
        humidity3_cellSize = builder
                .comment(humidity3_cellSizeTxt + "\nDefault: 64")
                .defineInRange(humidity3_cellSizeName, 64, 0, 512);
        humidity3_cellSubsamplingOcean = builder
                .comment(humidity3_cellSubsamplingOceanTxt + "\nDefault: 2")
                .defineInRange(humidity3_cellSubsamplingOceanName, 2, 0, Integer.MAX_VALUE);
        humidity3_cellSubsamplingRiver = builder
                .comment(humidity3_cellSubsamplingRiverTxt + "\nDefault: 5")
                .defineInRange(humidity3_cellSubsamplingRiverName, 5, 0, Integer.MAX_VALUE);
        humidity3_cacheSize = builder
                .comment(humidity3_cacheSizeTxt + "\nDefault: 17")
                .defineInRange(humidity3_cacheSizeName, 17, 0, 30);
        humidity3_cacheRecycleWorld = builder
                .comment(humidity3_cacheRecycleWorldTxt)
                .define("humidity3_cacheRecycleWorld", true);
        humidity3_shortcutsLevel = builder
                .comment(humidity3_shortcutsLevelTxt + "\nDefault: 1")
                .defineInRange(humidity3_shortcutsLevelName, 1, 0, 1);
        humidity3_climateDimensionSampleOrderOcean = builder
                .comment(humidity3_climateDimensionSampleOrderOceanTxt)
                .defineList(humidity3_climateDimensionSampleOrderOceanName,
                            () -> List.of(2, 0, 3, 5, 4, 1),
                            element -> element instanceof Integer
                    );
        humidity3_climateDimensionSampleOrderRiver = builder
                .comment(humidity3_climateDimensionSampleOrderRiverTxt)
                .defineList(humidity3_climateDimensionSampleOrderRiverName,
                        () -> List.of(2, 5, 3, 0, 4, 1),
                        element -> element instanceof Integer
                );
        builder.pop();

        builder.push(sectionBandLimit).comment(sectionBandLimitTxt);
        bandLimitUpperCount = builder
                .comment(bandLimitUpperCountTxt + "\nDefault: 0")
                .defineInRange(bandLimitUpperCountName, 0, 0, Integer.MAX_VALUE);
        bandLimitUpperValue = builder
                .comment(bandLimitUpperValueTxt + "\nDefault: -1.0")
                .defineInRange(bandLimitUpperValueName, -1.0, -1.0, 1.0);
        bandLimitLowerCount = builder
                .comment(bandLimitLowerCountTxt + "\nDefault: 0")
                .defineInRange(bandLimitLowerCountName, 0, 0, Integer.MAX_VALUE);
        bandLimitLowerValue = builder
                .comment(bandLimitLowerValueTxt + "\nDefault: -1.0")
                .defineInRange(bandLimitLowerValueName, -1.0, -1.0, 1.0);
        builder.pop();

        builder.push(sectionStronghold).comment(sectionStrongholdTxt);
        strongholdCalcOverride = builder
                .comment(strongholdCalcOverrideTxt + "\nDefault: 0")
                .defineInRange(strongholdCalcOverrideName, 0, 0, 3);
        strongholdCalcRange = builder
                .comment(strongholdCalcRangeTxt + "\nDefault: 0")
                .defineInRange(strongholdCalcRangeName, 0, 0, Integer.MAX_VALUE);
        builder.pop();
    }
}
