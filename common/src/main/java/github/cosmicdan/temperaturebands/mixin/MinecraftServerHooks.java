package github.cosmicdan.temperaturebands.mixin;

import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
abstract class MinecraftServerHooks {
    @Inject(
            method = "stopServer",
            at = @At("HEAD")
    )
    private void onStopServer(CallbackInfo ci) {
        // Clear config so it can be reloaded if a new world is loaded
        TemperatureBands.clearSaveAndConfig();
    }
}
