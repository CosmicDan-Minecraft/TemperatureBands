package github.cosmicdan.temperaturebands.mixin;

import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Responsible for clearing data/config when a world (server) has closed
 */
@Mixin(MinecraftServer.class)
abstract class MinecraftServerHooks {
    @Inject(
            method = "stopServer",
            at = @At("TAIL")
    )
    private void onStopServer(CallbackInfo ci) {
        TemperatureBands.clearDimensionDataAndConfig();
        TemperatureBands.DIMENSION_DATA_CACHE.invalidateAll();
    }
}
