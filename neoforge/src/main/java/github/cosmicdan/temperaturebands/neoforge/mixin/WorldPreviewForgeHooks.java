package github.cosmicdan.temperaturebands.neoforge.mixin;

import com.caeruleusTait.world.preview.backend.WorkManager;
import com.caeruleusTait.world.preview.backend.worker.WorkBatch;
import com.caeruleusTait.world.preview.backend.worker.WorkResult;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

abstract class WorldPreviewForgeHooks {}

@Mixin(WorkManager.class)
abstract class WorldPreviewForgeHooksWorkManager {
    @Shadow
    @Final
    private List<WorkBatch> currentBatches;

    @Inject(
            method = "queueRangeReal",
            at = @At("HEAD")
    )
    public void onQueueRangeStart(BlockPos topLeftBlock, BlockPos bottomRightBlock, CallbackInfo ci) {
        if (TemperatureBands.configDoBenchmark)
            TemperatureBands.benchmarkReset();
    }

    @Inject(
            method = "queueRangeReal",
            at = @At("RETURN")
    )
    public void onQueueRangeEnd(BlockPos topLeftBlock, BlockPos bottomRightBlock, CallbackInfo ci) {
        if (TemperatureBands.configDoBenchmark)
            TemperatureBands.benchmarkStart(currentBatches.size());
    }
}

@Mixin(WorkBatch.class)
abstract class WorldPreviewHooksWorkBatch {

    @Inject(
            method = "applyChunkResult",
            at = @At("RETURN")
    )
    public void onApplyChunkResultEnd(List<WorkResult> workResultList, CallbackInfo ci) {
        if (TemperatureBands.configDoBenchmark)
            TemperatureBands.benchmarkBatchDone();
    }
}

