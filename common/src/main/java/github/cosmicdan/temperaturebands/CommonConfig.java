package github.cosmicdan.temperaturebands;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfig {
    public static final String sectionWorld = "world";
    public static final String sectionWorldTxt = " Default world settings. These will apply to newly-generated worlds only, existing worlds will remember their own settings.\n";
    public final ForgeConfigSpec.IntValue bandSize;
    public static final String bandSizeTxt = " The average size of each temperature band in blocks.\n" +
            " Vanilla Minecraft generation has 5 temperature zones - Freezing, Cool, Temperate, Warm and Hot - every biome fits into one of these five temperature ranges.\n" +
            " So for example, if this value is 2048, the distance from the *center* of one one Hot band to the next Hot band center will be about 16384 (2048 * 8) blocks (Hot through to Freezing and back again).\n" +
            " If there are any worldgen mods that add biomes with a different 'temperature range' for placement, they should be incorporated within existing bands normally.";
    public final ForgeConfigSpec.BooleanValue useVerticalBands;
    public static final String useVerticalBandsTxt = " If true, bands will be vertical instead of horizontal.";
    public final ForgeConfigSpec.DoubleValue bandPositionShift;
    public static final String bandPositionShiftTxt = " Shift the bands by this percentage.\n" +
            " Normally the Hot band (Desert and such) will generate at origin (0,0) so the default shift of 25% will make the origin temperate (middle temp) instead, shifted\n" +
            " downwards to simulate a 'Northern hemisphere' start (Colder will be North, or East if Vertical bands, and Hotter will be South or West). A value of 0.75 would simulate southern hemisphere start instead.";
    public final ForgeConfigSpec.DoubleValue tempRange;
    public static final String tempRangeTxt = " Absolute min/max temperature range. Increasing this will make the coldest and hottest bands larger while reducing others, lowering this value will have the opposite effect.\n" +
            " The default value makes the hottest/coldest bands a tiny bit smaller than the others.\n" +
            " Be careful, setting this value too high or too low could result in some bands not generating at all.";
    public final ForgeConfigSpec.DoubleValue tempGradeShift;
    public static final String tempGradeShiftTxt = " Vanilla temperature has a bias towards cold, this value is used to make the hottest and coldest roughly the same size.\n" +
            " If you set it to zero, the coldest band will be larger than all other bands and the hottest would be smaller than all others.\n" +
            " You probably wont want to change this, but I made it configurable anyway.";
    public final ForgeConfigSpec.IntValue bandAlgorithm;
    public static final String bandAlgorithmTxt = " Algorithm to use. You can configure and read more about each algorithm in their own section below. Recommended to use World Preview if you want to change things around.\n" +
            " Note: There is only 1 algorithm currently, this config is here just in case I add more later.";
    public final ForgeConfigSpec.IntValue noiseFactor;
    public static final String noiseFactorTxt = " If above zero, will use original noise generation to help make the edges between bands a bit nicer.\n" +
            " See each algorithm for details on how noiseFactor is used.\n" +
            " The unit is arbitrary and very dependent on bandSize. For e.g. 100 is nice for a bandSize of 2048 but might be too wild if bandSize is decreased, or too tame if bandSize is increased.";

    public static final String sectionAlgo1 = "algorithm1";
    public static final String sectionAlgo1Txt = " Settings for algorithm 1, the 'simple' or 'default' algorithm.\n" +
            " This algorithm is very simple and produces jaggy lines that aren't very random or natural looking, but it looks OK as long as you keep useNoise enabled.\n" +
            " Keeping the variance low but noiseFactor a decent amount (around 100 or so) will produce some OK looking curves/waves in the bands, but it you want something more predictable you can reduce noiseFactor and increase variance.\n";
    public final ForgeConfigSpec.IntValue algo1bandVariance;
    public static final String algo1bandVarianceTxt = " Band variance in blocks (roughly). If below 10, each band will have a completely straight edge across the world and disables all remaining algo1 features.\n" +
            " You will want to keep this a fairly small value. Making it too large could result in weirdness, especially if it's too close to the bandSize.";
    public static final int algo1bandVarianceMin = 10;
    public final ForgeConfigSpec.DoubleValue algo1bandVarianceSteepness;
    public static final String algo1bandVarianceSteepnessTxt = " Steepness of band variance. Unit is arbitrary, you'll want to experiment and test if you change it. Probably best left alone though.";

    public CommonConfig(final ForgeConfigSpec.Builder builder) {
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
        builder.pop();

        builder.push(sectionAlgo1).comment(sectionAlgo1Txt);
        algo1bandVariance = builder
                .comment(algo1bandVarianceTxt)
                .defineInRange("bandVariance", 32, 0, 16384);
        algo1bandVarianceSteepness = builder
                .comment(algo1bandVarianceSteepnessTxt)
                .defineInRange("algo1bandVarianceSteepness", 0.2, 0.1, 10.0);
        builder.pop();
    }
}
