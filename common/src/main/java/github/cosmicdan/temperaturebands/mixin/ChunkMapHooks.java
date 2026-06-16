package github.cosmicdan.temperaturebands.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.DimensionDataDraft;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

/**
 * Responsible for remembering which NoiseRouters need to be overridden with modded ones.
 * Also ensures the dimension is whitelisted.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapHooks {
    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    protected abstract RandomState randomState();

    @Shadow
    @Final
    private RandomState randomState;

    @WrapOperation (
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/RandomState;create(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)Lnet/minecraft/world/level/levelgen/RandomState;")
    )
    public RandomState onNewRandomState(NoiseGeneratorSettings noiseGeneratorSettings, HolderGetter<NormalNoise.NoiseParameters> holderGetter, long seed, Operation<RandomState> original) {
        TemperatureBands.doModConfigIfNeeded();
        final String dimensionName = level.dimension().location().toString();
        if (TemperatureBands.isDimensionWhitelisted(dimensionName)) {
            // check if dimensiondata already exists. Some mods (e.g. World Preview) create multiple RandomStates for whatever reason (assuming multiple threads)
            boolean dimDataAlreadyMade = false;
            for (Map.Entry<String, @NonNull DimensionData> dimDataEntry : TemperatureBands.DIMENSION_DATA_CACHE.asMap().entrySet()) {
                if (dimDataEntry.getKey().equals(dimensionName)) {
                    dimDataAlreadyMade = true;
                    break;
                }
            }
            if (!dimDataAlreadyMade) {
                for (Map.Entry<String, @NonNull DimensionData> dimDataEntry : TemperatureBands.DIMENSION_DATA_CACHE_DRAFTS.asMap().entrySet()) {
                    if (dimDataEntry.getKey().equals(dimensionName)) {
                        dimDataAlreadyMade = true;
                        break;
                    }
                }
            }
            if (!dimDataAlreadyMade)
                TemperatureBands.DIMENSION_DATA_CACHE_DRAFTS.put(dimensionName, new DimensionDataDraft(level, noiseGeneratorSettings.noiseRouter()));
        }
        return original.call(noiseGeneratorSettings, holderGetter, seed);
    }

    /*
    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    public void onNewRandomStateDone(ServerLevel serverLevel, LevelStorageSource.LevelStorageAccess levelStorageAccess, DataFixer dataFixer, StructureTemplateManager structureTemplateManager, Executor executor, BlockableEventLoop blockableEventLoop, LightChunkGetter lightChunkGetter, ChunkGenerator chunkGenerator, ChunkProgressListener chunkProgressListener, ChunkStatusUpdateListener chunkStatusUpdateListener, Supplier supplier, int i, boolean bl, CallbackInfo ci) {
        this.randomState.sampler()
    }
     */
}
