package github.cosmicdan.temperaturebands.forge.mixin;

import caeruleusTait.world.preview.WorldPreviewConfig;
import caeruleusTait.world.preview.backend.WorkManager;
import caeruleusTait.world.preview.backend.worker.WorkBatch;
import caeruleusTait.world.preview.backend.worker.WorkResult;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
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
        @Shadow
        @Final
        private WorldPreviewConfig config;

        @WrapOperation(
                method = "queueRangeReal(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;J)V",
                at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;[Ljava/lang/Object;)V"),
                remap = false
        )
        public void onQueueRangeStartBatch(Logger instance, String s, Object[] objects, Operation<Void> original, @Local(name = "units") int units, @Local(name = "retainedBatches") int retainedBatches) {
            if (TemperatureBands.CONFIG_GLOBAL.doBenchmark()) {
                if (units != 0 && retainedBatches != 0) {
                    if (config.enablePreload)
                        TemperatureBands.LOGGER.warn("World Preview benchmark is requested, but the preload option is enabled which doesn't work properly with benchmarking. Please disable it via 'Settings > Other > Enable drag preloading' then re-open the world (re)creation screen.");
                    else {
                        TbUtils.benchmarkReset();
                        TbUtils.benchmarkStart(retainedBatches, units);
                    }
                }
            }
            original.call(instance, s, objects);
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
}
