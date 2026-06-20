package github.cosmicdan.temperaturebands.mixin.client;

import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Responsible for clearing data/config when the "Create World" screen is cancelled
 */
@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenHooks {
    @Inject(
            method = "popScreen",
            at = @At("HEAD")
    )
    private void onPopScreen(CallbackInfo ci) {
        TemperatureBands.clearDimensionDataAndConfig();
    }
}
