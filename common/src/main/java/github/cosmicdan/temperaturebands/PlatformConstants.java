package github.cosmicdan.temperaturebands;

public class PlatformConstants {
    /**
     * The number of dimensions per climate parameter point. Unlikely to change any time soon.
     */
    public static final int DIMENSION_COUNT = 6;

    /**
     * Technically not a platform constant, but whatever.
     * Since v3.0.0, we do a runtime test for every dimension that is 2D or 3D and act appropriately, so this specific value shouldn't actually matter.
      */
    public static final int BLOCK_Y_FIXED = 64;

    /**
     * Default stronghold search range in vanilla.
     */
    public static final int STRONGHOLD_SEARCH_RANGE_DEFAULT = 112;

    /**
     * Used internally to represent the "wildcard" match for biomes, as an absolute value. I.e. -1.0 to + 1.0.
     * Even though noise values can actually range to 20000, 10000 is the threshold for Minecraft to consider it as "any" simply because there are
     * no biomes beyond this range (and the biome picker will pick whatever parameters are closest).
     */
    public static final long CLIMATE_DIMENSION_MIN_MAX_WILDCARD = 10_000L;

    /**
     * Some mods add biomes with intentionally out-of-bounds parameters because they use advanced methods to replace vanilla biomes at runtime.
     * This value repretends the absolute min/max of "real" parameter values, so any parameter point with a dimension outside this range will
     * be completely ignored by humidity algorithm(s).
     */
    public static final long CLIMATE_DIMENSION_MIN_MAX_HARD = 20_000L;
}
