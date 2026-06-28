package github.cosmicdan.temperaturebands;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfig {
    public static final String sectionGlobal = "global";
    public static final String sectionGlobalTxt = """
             [global] settings are not world-specific. These will apply to all worlds and will not save per-world since they don't affect world generation.
             --------""";
    public final ForgeConfigSpec.BooleanValue copyConfigOnRecreateWorld;
    public static final String copyConfigOnRecreateWorldTxt = """
             
             [copyConfigOnRecreateWorld] will, when true, copy the previous world Temperature Bands config to the new world when "Recreate" is being done.
              - Set this to false if you want to "upgrade" your world with new features and defaults after updating the mod. You will still need to Recreate the world.
              - Relevant for client only. Servers will need to manually recreate their worlds (e.g. set the same seed/settings in server config and delete old world).""";
    public final ForgeConfigSpec.ConfigValue<String> ignoreDimensionFailures;
    public static final String ignoreDimensionFailuresTxt = """

             [ignoreDimensionFailures] is a list of dimensions to ignore logging errors about when replacing their temperature/humidity functions fail.
              - Separate with commas. Trailing comma doesn't matter.
              - The default entry of The End is there since vanilla End uses a constant 0.0 for temperature (for some reason, though it doesn't use any humidity noise at all)
                and the mod will try to replace temperature anyway, giving a false 'report if you want support for this custom dimension' error. Please don't report that error :)""";

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
    public final ForgeConfigSpec.IntValue noiseFactor;
    public static final String noiseFactorName = "configNoiseFactor";
    public static final String noiseFactorTxt = """
             
             [noiseFactor] will, when above zero, incorporate original noise generation into temperature bands to help make the shape and edges of bands a bit nicer.
              - HIGHLY recommended to keep it on.
              - See each algorithm for details on how noiseFactor is used. The unit is arbitrary and very dependent on bandSize. For e.g. 100 is nice for a bandSize of 2048 but
                might be too wild if bandSize is decreased, or too tame if bandSize is increased.""";
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
              - A value of 1 uses a simpler 'humidity bands' algorithm that run perpendicular to temperature bands.
              - There is no other algorithm available for 1.19.2 and below. See the FAQ on mod description page for why.
              - A value of 0 will disable humidity feature entirely, keeping vanilla random noise humidity.""";
    public final ForgeConfigSpec.DoubleValue humidityTempWeight;
    public static final String humidityTempWeightName = "humidityTempWeight";
    public static final String humidityTempWeightTxt = """
             
             [humidityTempWeight] is how much the temperature will additionally influence humidity.
              - Applies to all humidity algorithms, and is the final adjustment to the humidity value.
              - For humidity algorithm 2 (advanced) it introduces some biome variation between the warmer and cooler sides of the temperate (middle temperature) bands.
              - For humidity algorithm 1 (bands perpendicular to temperature), it applies some angle via narrowing/widening on the humidity bands - as you approach the cold
                band, lower-humidity biomes will become wider; as you approach the hot band, higher-humidity biomes will become wider.
              - Setting to 0.0 will disable this function.""";

    public static final String sectionHumidityAlgo1 = "humidity-algorithm1";
    public static final String sectionHumidityAlgo1Txt = """
             [humidity-algorithm1] are world-specific settings for the 'simple' banding humidity algorithm, i.e. humidity bands that run perpendicular to temperature.
             --------""";
    public final ForgeConfigSpec.DoubleValue humidityAlgo1MimicScale;
    public static final String humidityAlgo1MimicScaleName = "configHumidityAlgo1MimicScale";
    public static final String humidityAlgo1MimicScaleTxt = """

             [humidityAlgo1MimicScale] will scale the humidity bands, in relation to temperature bands.
              - The default of 0.5 for e.g. means that humidity bands will be half as big and twice as frequent as temperature bands (on average).""";

    public CommonConfig(final ForgeConfigSpec.Builder builder) {
        builder.push(sectionGlobal).comment(sectionGlobalTxt);
        copyConfigOnRecreateWorld = builder
                .comment(copyConfigOnRecreateWorldTxt)
                .define("copyConfigOnRecreateWorld", true);
        ignoreDimensionFailures = builder
                .comment(ignoreDimensionFailuresTxt)
                .define("ignoreDimensionFailures", "minecraft:the_end,");
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
                .define("dimBlacklist", "minecraft:the_nether,minecraft:the_end,");
        dimBlacklistAsWhitelist = builder
                .comment(dimBlacklistAsWhitelistTxt)
                .define("dimBlacklistAsWhitelist", false);
        vanillaNoiseOverride = builder
                .comment(vanillaNoiseOverrideTxt)
                .defineInRange("vanillaNoiseOverride", 1, 0, 2);
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
                .defineInRange("humidityAlgorithm", 1, 0, 1);
        humidityTempWeight = builder
                .comment(humidityTempWeightTxt)
                .defineInRange("humidityTempWeight", 0.4, 0.0, 1.0);
        builder.pop();

        builder.push(sectionHumidityAlgo1).comment(sectionHumidityAlgo1Txt);
        humidityAlgo1MimicScale = builder
                .comment(humidityAlgo1MimicScaleTxt)
                .defineInRange("humidityAlgo1MimicScale", 0.5, 0.1, 1.0);
        builder.pop();
    }
}
