package github.cosmicdan.temperaturebands.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.cosmicdan.temperaturebands.*;
import github.cosmicdan.temperaturebands.densityfunctions.HumidityShiftedNoise;
import github.cosmicdan.temperaturebands.densityfunctions.TemperatureShiftedNoise;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Contains hooks for the various related to Level creation/loading. Each level represents a dimension on the client or server.
 * TODO: Separate server/client objects.
 */
public abstract class LevelHooks {
    /**
     * Dummy hook to manually "verify" that ShiftedNoise is unchanged between MC updates.
     * Doesn't actually do anything, just blocks compile if signatures and such have changed.
     */
    @Mixin(DensityFunctions.ShiftedNoise.class)
    public static abstract class DensityFunctionsHooks {

        @Inject(
                method = "<init>",
                at = @At("TAIL")
        )
        private void onConstruct(DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, DensityFunction.NoiseHolder noise, CallbackInfo ci) {
        }

        @Inject(
                method = "compute",
                at = @At("TAIL")
        )
        private void onCompute(DensityFunction.FunctionContext functionContext, CallbackInfoReturnable<Double> cir) {
        }
    }

    @Mixin(LevelStorageSource.class)
    public static abstract class LevelStorageSourceHooks {
        @Shadow public abstract Path getBaseDir();

        /**
         * First hook that's called during level (world) creation/load, near the very start. Note that the level object is not yet initialized.
         * Responsible for fetching the save directory of a created/loaded world so we can load configs.
         */
        @Inject(
                method = "createAccess",
                at = @At("TAIL")
        )
        private void onCreateAccess(String saveName, CallbackInfoReturnable<LevelStorageSource.LevelStorageAccess> cir) {
            // (Re)creating a new world OR Deleting a world
            if (!TemperatureBands.isDeleteScreenActive) {
                Path saveDir = getBaseDir().toAbsolutePath().resolve(saveName);
                TemperatureBands.logDebug("onCreateAccess called, (re)creating a world with saveDir '{}'", saveDir.toAbsolutePath().toString());
                DimensionConfig.onLevelStorageLoad(saveDir, true);
            } else
                TemperatureBands.logDebug("Ignoring onCreateAccess because delete world screen is active");
        }

        @Inject(
                method = "validateAndCreateAccess",
                at = @At("TAIL")
        )
        private void onValidateAndCreateAccess(String saveName, CallbackInfoReturnable<LevelStorageSource.LevelStorageAccess> cir) {
            // Loading an existing world (possibly for recreation, which will call onCreateAccess after confirmed)
            Path saveDir = getBaseDir().toAbsolutePath().resolve(saveName);
            TemperatureBands.logDebug("onCreateAccess called, loading an existing world with saveDir '{}'", saveDir.toAbsolutePath().toString());
            DimensionConfig.onLevelStorageLoad(saveDir, false);
        }
    }

    @Mixin(ChunkMap.class)
    public static abstract class ChunkMapHooks {
        @Shadow
        @Final
        ServerLevel level;

        /**
         * Second hook that's called during level (world) creation/load, after LevelStorageSourceHooks. Note that the level object is still not initialized.
         * Responsible for remembering which NoiseRouters need to be overridden with modded ones.
         * Also ensures the dimension is whitelisted.
         */
        @WrapOperation(
                method = "<init>",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/RandomState;create(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)Lnet/minecraft/world/level/levelgen/RandomState;")
        )
        public RandomState onNewRandomState(NoiseGeneratorSettings noiseGeneratorSettings, HolderGetter<NormalNoise.NoiseParameters> holderGetter, long seed, Operation<RandomState> original) {
            final String dimensionName = level.dimension().location().toString();
            if (DimensionConfig.isPendingWorldDimensionWhitelisted(dimensionName)) {
                // Check if dimensiondata already exists. Some mods (e.g. World Preview) create multiple RandomStates for whatever reason (assuming multiple threads)
                //boolean dimDataAlreadyMade = false;
                DimensionData dimDataExisting = TemperatureBands.DIMENSION_DATA_CACHE.getIfPresent(dimensionName);
                if (dimDataExisting == null) {
                    if (TemperatureBands.isFirstDimensionData) {
                        TemperatureBands.DIMENSION_DATA_CACHE.put(dimensionName, new DimensionData(false, DimensionConfig.PENDING_WORLD, level, noiseGeneratorSettings.noiseRouter(), null, null));
                        TemperatureBands.logDebug("New DimensionData for {} was created (from DimensionConfig.PENDING_WORLD)", dimensionName);
                    } else {
                        // TODO: This is where we'll do dimension-specific config, for now just do same
                        TemperatureBands.DIMENSION_DATA_CACHE.put(dimensionName, new DimensionData(false, DimensionConfig.PENDING_WORLD, level, noiseGeneratorSettings.noiseRouter(), null, null));
                        TemperatureBands.logDebug("New DimensionData for {} was created (from DimensionConfig.PENDING_WORLD, TEMPORARY until dimension-specific config is implemented)", dimensionName);
                    }
                } else {
                    TemperatureBands.logDebug("DimensionData for {} was already created", dimensionName);
                }
            }
            return original.call(noiseGeneratorSettings, holderGetter, seed);
        }
    }

    @Mixin(NoiseRouter.class)
    public static abstract class NoiseRouterHooks {
        @Shadow
        @Final
        private DensityFunction temperature;

        @Shadow
        @Final
        private DensityFunction vegetation;

        @Unique
        private NoiseRouter temperatureBands_$getSelf() {
            return (NoiseRouter)(Object) this;
        }

        /**
         * Third hook that's used during level (world) creation/load, after ChunkMapHooks. Will be called many times, once for each densityfunctions function. Level still not ready.
         * Responsible for overriding relevant DensityFunctions in NoiseRouter if applicable (i.e. has an entry in TemperatureBands.NOISEROUTER_TEMP_OVERRIDES)
         */
        @WrapOperation(
                method = "mapAll",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/DensityFunction;mapAll(Lnet/minecraft/world/level/levelgen/DensityFunction$Visitor;)Lnet/minecraft/world/level/levelgen/DensityFunction;")
        )
        public DensityFunction onMapDensityFunction(DensityFunction instance, DensityFunction.Visitor visitor, Operation<DensityFunction> original) {
            if (DimensionConfig.isOnlyLoadingWorldWithoutConfig) {
                TbUtils.doCrash("Tried to load an existing world that didn't have the mod installed. Please recreate this world instead, or remove the mod.");
            }
            DensityFunction currentFunction = instance;
            if (currentFunction.equals(temperature) || currentFunction.equals(vegetation)) {
                // First find the dimension(data) that this NoiseRouter belongs to
                for (Map.Entry<String, @NonNull DimensionData> dimDataEntry : TemperatureBands.DIMENSION_DATA_CACHE.asMap().entrySet()) {
                    if (dimDataEntry.getValue().getNoiseRouter().equals(temperatureBands_$getSelf())) {
                        String dimensionName = dimDataEntry.getKey();
                        DimensionData dimensionData = dimDataEntry.getValue();
                        if (currentFunction.equals(temperature))
                            currentFunction = TemperatureBands.replaceNoiseIfNeeded(dimensionData, dimensionName, currentFunction, TemperatureShiftedNoise.NAME);
                        else if (currentFunction.equals(vegetation) && dimensionData.config.humidityAlgorithm() != 0)
                            currentFunction = TemperatureBands.replaceNoiseIfNeeded(dimensionData, dimensionName, currentFunction, HumidityShiftedNoise.NAME);
                        break;
                    }
                }
            }
            return original.call(currentFunction, visitor);
        }

    }

    /**
     * Fourth and final hook that's used during level (world) creation/load, after NoiseRouterHooks. The level is now ready.
     * Responsible for finalizing DimensionData. Also contains a hook on close to clear config/data.
     * TODO: Replace with "Level", once sided stuff is done
     */
    @Mixin(ServerLevel.class)
    public static abstract class ServerLevelHooks extends Level {
        @Shadow
        public abstract ServerLevel getLevel();

        protected ServerLevelHooks(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l, int i) {
            super(writableLevelData, resourceKey, registryAccess, holder, supplier, bl, bl2, l, i);
        }
        @Inject(
                method = "<init>",
                at = @At("RETURN")
        )
        private void onCreationDone(MinecraftServer minecraftServer, Executor executor, LevelStorageSource.LevelStorageAccess levelStorageAccess, ServerLevelData serverLevelData, ResourceKey<Level> resourceKey, LevelStem levelStem, ChunkProgressListener chunkProgressListener, boolean bl, long l, List<CustomSpawner> list, boolean bl2, RandomSequences randomSequences, CallbackInfo ci) {
            final String dimensionName = dimension().location().toString();
            DimensionData dimData = TemperatureBands.DIMENSION_DATA_CACHE.getIfPresent(dimensionName);
            if (dimData != null) {
                if (dimensionName.equals(OVERWORLD.location().toString()) && TemperatureBands.CONFIG_GLOBAL.climateSamplerWarmupMsg() && dimData.config.humidityAlgorithm() == 2) {
                    // using advanced humidity and we're loading the overworld, set flag for loading screen
                    TemperatureBands.addLoadingScreenText = true;
                }
                DimensionData.recreateDimensionWithLevelReady(dimensionName, dimData);
            }
        }

        @Inject(
                method = "close",
                at = @At("RETURN")
        )
        private void onClose(CallbackInfo ci) {
            TemperatureBands.clearDimensionDataAndConfig(getLevel().dimension().location().toString());
        }
    }
}
