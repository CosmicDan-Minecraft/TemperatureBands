package github.cosmicdan.temperaturebands.mixin;

import github.cosmicdan.temperaturebands.TbUtils;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.TimeUnit;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerHooks {

    @Inject(
            method = "loadLevel",
            at = @At("HEAD")
    )
    private void startLevelLoad(CallbackInfo ci) {
        if (TemperatureBands.CONFIG_GLOBAL.doLevelLoadBenchmark()) {
            TbUtils.levelLoadBenchmarkStart = Util.getNanos();
        }
    }

    @Inject(
            method = "loadLevel",
            at = @At("RETURN")
    )
    private void finishLevelLoad(CallbackInfo ci) {
        if (TemperatureBands.CONFIG_GLOBAL.doLevelLoadBenchmark() && TbUtils.levelLoadBenchmarkStart != -1L) {
            long timeMillis = TimeUnit.NANOSECONDS.toMillis(Util.getNanos() - TbUtils.levelLoadBenchmarkStart);
            TbUtils.levelLoadBenchmarkStart = -1L;
            TemperatureBands.LOGGER.info("Loading level took {} seconds.", String.format("%.2f", timeMillis / 1000.0));
        }
    }
}
