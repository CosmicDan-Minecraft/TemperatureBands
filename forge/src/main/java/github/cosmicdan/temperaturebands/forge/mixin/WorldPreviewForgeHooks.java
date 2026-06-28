package github.cosmicdan.temperaturebands.forge.mixin;

import caeruleusTait.world.preview.backend.WorkManager;
import caeruleusTait.world.preview.backend.worker.SampleUtils;
import caeruleusTait.world.preview.backend.worker.WorkBatch;
import caeruleusTait.world.preview.backend.worker.WorkResult;
import com.llamalad7.mixinextras.sugar.Local;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

abstract class WorldPreviewForgeHooks {
    @Mixin(WorkManager.class)
    abstract static class WorkManagerHooks {
        @Shadow(remap = false)
        @Final
        private List<WorkBatch> currentBatches;

        @Inject(
                method = "queueRangeReal",
                at = @At("HEAD"),
                remap = false
        )
        public void onQueueRangeStart(BlockPos topLeftBlock, BlockPos bottomRightBlock, CallbackInfo ci) {
            if (TemperatureBands.CONFIG_GLOBAL.doBenchmark())
                TbUtils.benchmarkReset();
        }

        @Inject(
                method = "queueRangeReal",
                at = @At("RETURN"),
                remap = false
        )
        public void onQueueRangeEnd(BlockPos topLeftBlock, BlockPos bottomRightBlock, CallbackInfo ci, @Local(name = "units") int units) {
            if (TemperatureBands.CONFIG_GLOBAL.doBenchmark())
                TbUtils.benchmarkStart(currentBatches.size(), units);
        }
    }

    @Mixin(WorkBatch.class)
    abstract static class WorkBatchHooks {
        @Inject(
                method = "applyChunkResult",
                at = @At("RETURN"),
                remap = false
        )
        public void onApplyChunkResultEnd(List<WorkResult> workResultList, CallbackInfo ci) {
            if (TemperatureBands.CONFIG_GLOBAL.doBenchmark())
                TbUtils.benchmarkBatchDone();
        }
    }

    @Mixin(SampleUtils.class)
    abstract static class SampleUtilsHooks {
        @Inject(
                method = "close",
                at = @At("HEAD"),
                remap = false
        )
        public void onClose(CallbackInfo ci) {
            TemperatureBands.clearDimensionDataAndConfig(null);
        }
    }
}
