package github.cosmicdan.temperaturebands;

import com.github.benmanes.caffeine.cache.Cache;
import github.cosmicdan.temperaturebands.biomehelper.LegacyHelper;
import github.cosmicdan.temperaturebands.biomehelper.SmartHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static github.cosmicdan.temperaturebands.TemperatureBands.LOGGER;

public class DimensionData {
    public final boolean isLevelReady;
    public final DimensionConfig config;
    public final ServerLevel level;
    public final NoiseRouter noiseRouter;
    private final @Nullable DensityFunctions.HolderHolder noiseTemperature;
    private final @Nullable DensityFunctions.HolderHolder noiseHumidity;
    // humidity-related things
    private MultiNoiseBiomeSource biomeSource;
    private LegacyHelper legacyHelper;
    private SmartHelper smartHelper;
    private Cache<Long, CachedClimatePoint> cacheClassic;

    private final AtomicReferenceArray<CacheEntry> cache;

    // config-derived
    private final int cacheMask;

    private record CacheEntry(long packedKey, CachedClimatePoint point) {}

    @SuppressWarnings("LoggingSimilarMessage")
    public DimensionData(boolean isLevelReady, DimensionConfig config, ServerLevel level, NoiseRouter noiseRouter, @Nullable DensityFunctions.HolderHolder noiseTemperature, @Nullable DensityFunctions.HolderHolder noiseHumidity) {
        this.isLevelReady = isLevelReady;
        this.config = config;
        this.level = level;
        this.noiseRouter = noiseRouter;
        this.noiseTemperature = noiseTemperature;
        this.noiseHumidity = noiseHumidity;

        // sanity check
        if (config == null) {
            TbUtils.doCrash("Tried to create new DimensionData but provided config is null, eh?");
            cache = null;
            cacheMask = 0;
        } else {
            if (config.humidityAlgorithm() >= 3) {
                int cacheSize = 1 << config.humidityAlgo3cacheSize();
                cache = new AtomicReferenceArray<>(cacheSize);
                cacheMask = cacheSize - 1;
            } else {
                cache = null;
                cacheMask = 0;
            }
            if (isLevelReady && config.humidityAlgorithm() != 0) {
                biomeSource = TbUtils.findMultiNoiseBiomeSource(level, true);
                TemperatureBands.logDebug("DimensionData finalized for " + level.dimension().location());
            }

        }
    }

    public boolean isCacheRecyclerPossible() {
        return config.humidityAlgorithm() >= 3;
    }

    public boolean isCacheReady() {
        return cache != null;
    }

    public NoiseRouter getNoiseRouter() {
        return noiseRouter;
    }

    public @Nullable DensityFunctions.HolderHolder getNoiseFunctionForName(String noiseName) {
        if (noiseName.equals(DensityFunctionEx.TEMPERATURE_NAME))
            return noiseTemperature;
        else if (noiseName.equals(DensityFunctionEx.HUMIDITY_NAME))
            return noiseHumidity;
        else
            return TbUtils.doCrash("Attempted getting an invalid densityfunctions: " + noiseName);
    }

    public static @Nullable DensityFunction createModdedFunction(String functionName, DensityFunction originalFunction, String dimensionName, DimensionConfig config, long seed) {
        if (functionName.equals(DensityFunctionEx.TEMPERATURE_NAME) || functionName.equals(DensityFunctionEx.HUMIDITY_NAME)) {
            if (config.vanillaNoiseOverride() > 0) {
                if (config.vanillaNoiseOverride() == 2 || !(originalFunction instanceof DensityFunctions.ShiftedNoise)) {
                    RandomSource vanillaRandom = new XoroshiroRandomSource(seed);
                    NormalNoise vanillaNoiseShift = NormalNoise.create(vanillaRandom, new NormalNoise.NoiseParameters(-3, List.of(1.0, 1.0, 1.0, 0.0)));
                    DensityFunction.NoiseHolder vanillaShiftShared = new DensityFunction.NoiseHolder(new Holder.Direct<>(vanillaNoiseShift.parameters()), vanillaNoiseShift);
                    NormalNoise vanillaNoise;

                    if (functionName.equals(DensityFunctionEx.TEMPERATURE_NAME))
                        vanillaNoise = NormalNoise.create(vanillaRandom, new NormalNoise.NoiseParameters(-10, List.of(1.5, 0.0, 1.0, 0.0, 0.0, 0.0)));
                    else
                        vanillaNoise = NormalNoise.create(vanillaRandom, new NormalNoise.NoiseParameters(-8, List.of(1.0, 1.0, 0.0, 0.0, 0.0, 0.0)));

                    LOGGER.info("Replaced base {} noise with vanilla-like noise (because vanillaNoiseOverride is {}{})", functionName, config.vanillaNoiseOverride(), config.vanillaNoiseOverride() == 1 ? " and original noise was type was '" + originalFunction.getClass().getSimpleName() + "'" : "");
                    originalFunction = new DensityFunctions.ShiftedNoise(
                            new DensityFunctions.ShiftA(vanillaShiftShared),
                            DensityFunctions.constant(0.0),
                            new DensityFunctions.ShiftB(vanillaShiftShared),
                            0.25,
                            0.0,
                            new DensityFunction.NoiseHolder(new Holder.Direct<>(vanillaNoise.parameters()), vanillaNoise)
                    );
                }
            }
            return new DensityFunctionEx(dimensionName, functionName, config, originalFunction);
        }
        return null;
    }

    public static void recreateDimDataWithNewNoiseFunction(String dimensionName, DimensionData dimData, String noiseName, DensityFunctions.HolderHolder noiseFunction) {
        if (noiseName.equals(DensityFunctionEx.TEMPERATURE_NAME))
            dimData = new DimensionData(false, dimData.config, dimData.level, dimData.noiseRouter, noiseFunction, dimData.noiseHumidity);
        else if (noiseName.equals(DensityFunctionEx.HUMIDITY_NAME))
            dimData = new DimensionData(false, dimData.config, dimData.level, dimData.noiseRouter, dimData.noiseTemperature, noiseFunction);
        else
            TbUtils.doCrash("Attempted recreating with invalid densityfunctions: " + noiseName);

        TemperatureBands.DIMENSION_DATA_CACHE.put(dimensionName, dimData);
    }

    public static void recreateDimensionWithLevelReady(String dimensionName, DimensionData dimDataToReady) {
        if (!dimDataToReady.isLevelReady) {
            DimensionData dimDataNew = new DimensionData(
                    true,
                    dimDataToReady.config,
                    dimDataToReady.level,
                    dimDataToReady.noiseRouter,
                    dimDataToReady.noiseTemperature,
                    dimDataToReady.noiseHumidity
            );
            TemperatureBands.DIMENSION_DATA_CACHE.put(dimensionName, dimDataNew);
        }
    }

    public MultiNoiseBiomeSource getBiomeSource() {
        if (biomeSource == null)
            // happens sometimes when C2ME + mods that modify BiomeSource (e.g. Blueprnt lib) are installed
            biomeSource = TbUtils.findMultiNoiseBiomeSource(level, false);
        return biomeSource;
    }

    public LegacyHelper getLegacyHelper() {
        if (legacyHelper == null) {
            // happens sometimes e.g. when C2ME is installed
            legacyHelper = new LegacyHelper(level, TbUtils.findMultiNoiseBiomeSource(level, false));
        }
        return legacyHelper;
    }

    public SmartHelper getSmartHelper() {
        if (smartHelper == null) {
            // happens sometimes e.g. when C2ME is installed
            smartHelper = new SmartHelper(this, level, TbUtils.findMultiNoiseBiomeSource(level, false));
        }
        return smartHelper;
    }

    public Climate.Sampler getClimateSampler() {
        return level.getChunkSource().randomState().sampler();
    }

    public @NotNull DensityFunctions.HolderHolder getTemperatureNoise() {
        if (noiseTemperature == null)
            return TbUtils.doCrash("noiseTemperature must not be null");
        return noiseTemperature;
    }

    public CachedClimatePoint getCachedPoint(int blockX, int blockYignored, int blockZ) {
        // Round-down to nearest multiple of 2. Minecraft actually uses a 4x4x4 resolution for climate noise, but this causes accuracy issues when calculating distance.
        blockX = blockX & ~1;
        blockZ = blockZ & ~1;
        long packedKey = ((long) blockX << 32) | (blockZ & 0xFFFFFFFFL);

        // Apply the MurmurHash3 avalanche step to reduce hash collisions in the cache (maximizes bucket array spread)
        long mixedKey = packedKey;
        mixedKey ^= mixedKey >>> 33;
        mixedKey *= 0xff51afd7ed558ccdL;
        mixedKey ^= mixedKey >>> 33;
        mixedKey *= 0xc4ceb9fe1a85ec53L;
        mixedKey ^= mixedKey >>> 33;

        CachedClimatePoint cachedPoint;

        int slot = (int) (mixedKey & cacheMask);
        CacheEntry entry = cache.get(slot);
        if (entry != null && entry.packedKey() == packedKey)
            cachedPoint = entry.point();
        else {
            // Cache miss or hash collision
            cachedPoint = new CachedClimatePoint();
            cache.set(slot, new CacheEntry(packedKey, cachedPoint));
        }
        return cachedPoint;
    }

    public Climate.TargetPoint sampleFinal(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int blockX = QuartPos.toBlock(quartX);
        int blockY = QuartPos.toBlock(quartY);
        int blockZ = QuartPos.toBlock(quartZ);
        CachedClimatePoint cachedPoint = getCachedPoint(blockX, blockY, blockZ);

        return Climate.target(
                cachedPoint.getOrComputeDimensionUnquantized(0, sampler, blockX, blockY, blockZ, false),
                cachedPoint.getOrComputeDimensionUnquantized(1, sampler, blockX, blockY, blockZ, false),
                cachedPoint.getOrComputeDimensionUnquantized(2, sampler, blockX, blockY, blockZ, false),
                cachedPoint.getOrComputeDimensionUnquantized(3, sampler, blockX, blockY, blockZ, false),
                cachedPoint.getOrComputeDimensionUnquantized(4, sampler, blockX, blockY, blockZ, false),
                cachedPoint.getOrComputeDimensionUnquantized(5, sampler, blockX, blockY, blockZ, false)
        );
    }

    @Override
    public String toString() {
        return "DimensionData{" +
                "level=" + level +
                '(' + level.hashCode() + ')' +
                '}';
    }
}
