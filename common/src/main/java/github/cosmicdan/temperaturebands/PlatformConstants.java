package github.cosmicdan.temperaturebands;

public class PlatformConstants {
    public static final long CLIMATE_DIMENSION_VALUE_MIN = -10_000L;
    public static final long CLIMATE_DIMENSION_VALUE_MAX = 10_000L;
    public static final int DIMENSION_COUNT = 6;

    /**
     * Technically not a platform constant, but whatever.
     * Since v3.0.0, we do a runtime test for every dimension that is 2D or 3D and act appropriately, so this specific value shouldn't actually matter.
      */
    public static final int BLOCK_Y_FIXED = 64;

    public static final int STRONGHOLD_SEARCH_RANGE_DEFAULT = 112;
}
