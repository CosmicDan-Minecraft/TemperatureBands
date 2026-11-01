package github.cosmicdan.temperaturebands.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.util.Pair;
import github.cosmicdan.temperaturebands.DimensionData;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.TemperatureBands;
import github.cosmicdan.temperaturebands.util.ChunkGenStructStateHooksInj;
import github.cosmicdan.temperaturebands.util.StrongholdHooksUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class StrongholdHooks {
    @Mixin(ChunkGeneratorStructureState.class)
    public static abstract class ChunkGenStructStateHooks implements ChunkGenStructStateHooksInj {
        @Shadow
        @Final
        private BiomeSource biomeSource;

        @Shadow
        public abstract RandomState randomState();

        @Unique
        private final AtomicReference<StrongholdHooksUtil.AtomicState> tb_$STATE = new AtomicReference<>();

        @Unique
        @Override
        public void tb$_createState(DimensionData dimData) {
            tb_$STATE.set(new StrongholdHooksUtil.AtomicState(dimData));
        }

        /**
         * Performs replacement (if configured) of Stronghold calculations.
         * Target is the lambda body of "supplyAsync" in #generateRingPositions
         */
        @WrapMethod(
                method = "method_46698"
        )
        public ChunkPos onGenerateRingsAsyncLambda(int i, int j, HolderSet<Biome> holderSet, RandomSource randomSource, Operation<ChunkPos> original) {
            if (holderSet.unwrapKey().isPresent() && holderSet.unwrapKey().get().equals(BiomeTags.STRONGHOLD_BIASED_TO)) {
                StrongholdHooksUtil.initIfNeeded(tb_$STATE, biomeSource);
                int calcOverride = StrongholdHooksUtil.getCalcOverride(tb_$STATE.get());
                if (calcOverride == 0) {
                    // no changes
                    return original.call(i, j, holderSet, randomSource);
                } else if (calcOverride == 1) {
                    // return current chunk
                    return new ChunkPos(i, j);
                } else if (calcOverride == 2) {
                    // replicate vanilla but use modded range
                    Pair<BlockPos, Holder<Biome>> pair = this.biomeSource.findBiomeHorizontal(
                            SectionPos.sectionToBlockCoord(i, 8),
                            0,
                            SectionPos.sectionToBlockCoord(j, 8),
                            StrongholdHooksUtil.getRange(tb_$STATE.get()),
                            holderSet::contains,
                            randomSource,
                            randomState().sampler()
                    );
                    if (pair != null) {
                        BlockPos blockPos = pair.getFirst();
                        return new ChunkPos(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()));
                    } else {
                        return new ChunkPos(i, j);
                    }
                } else if (calcOverride == 3) {
                    //
                    BlockPos found = StrongholdHooksUtil.findAnyLandHorizontal(
                            SectionPos.sectionToBlockCoord(i, 8),
                            0,
                            SectionPos.sectionToBlockCoord(j, 8),
                            tb_$STATE.get(),
                            randomSource,
                            randomState().sampler()
                    );
                    if (found != null) {
                        return new ChunkPos(SectionPos.blockToSectionCoord(found.getX()), SectionPos.blockToSectionCoord(found.getZ()));
                    } else {
                        return new ChunkPos(i, j);
                    }
                } else {
                    TbUtils.doCrash("Unhandled 'strongholdCalcOverride' setting '" + calcOverride + "', fix me!");
                }
            }
            return original.call(i, j, holderSet, randomSource);
        }

        /**
         * Responsible for counting stronghold calculation progress, if that feature is enabled.
         */
        @WrapOperation(
                method = "generateRingPositions",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;sequence(Ljava/util/List;)Ljava/util/concurrent/CompletableFuture;")
        )
        private CompletableFuture<List<ChunkPos>> onGenerateRingsSequence(List<? extends CompletableFuture<ChunkPos>> list, Operation<CompletableFuture<List<ChunkPos>>> original, Holder<StructureSet> holderSet) {
            if (TemperatureBands.CONFIG_GLOBAL.showStrongholdProgress()) {
                if (holderSet.unwrapKey().isPresent() && holderSet.unwrapKey().get().equals(BuiltinStructureSets.STRONGHOLDS)) {
                    StrongholdHooksUtil.initIfNeeded(tb_$STATE, biomeSource);
                    TemperatureBands.logDebug("Using strongholdCalcOverride of {} and strongholdCalcRange of {}", String.valueOf(StrongholdHooksUtil.getCalcOverride(tb_$STATE.get())), String.valueOf(StrongholdHooksUtil.getRange(tb_$STATE.get())));
                    TemperatureBands.strongholdCountTotal.set(list.size());
                    TemperatureBands.strongholdCountCurrent.set(0);
                    for (CompletableFuture<ChunkPos> future : list) {
                        future.thenApply(result -> {
                            int strongholdCount = TemperatureBands.strongholdCountCurrent.incrementAndGet();
                            if (strongholdCount >= TemperatureBands.strongholdCountTotal.get()) {
                                TemperatureBands.strongholdCountTotal.set(-1);
                                TemperatureBands.strongholdCountCurrent.set(0);
                            } return result;
                        });
                    }
                }
            }
            return original.call(list);
        }
    }
}
