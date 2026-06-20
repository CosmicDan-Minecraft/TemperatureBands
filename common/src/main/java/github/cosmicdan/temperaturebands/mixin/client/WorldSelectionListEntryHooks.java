package github.cosmicdan.temperaturebands.mixin.client;

import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Responsible for detecting if a world is being deleted in order to skip loading config for it
 */
@Mixin(WorldSelectionList.WorldListEntry.class)
public class WorldSelectionListEntryHooks {
    @Inject(
            method = "doDeleteWorld",
            at = @At("HEAD")
    )
    public void onDoDeleteWorldStart(CallbackInfo ci) {
        TemperatureBands.isDeleteScreenActive = true;
    }

    @Inject(
            method = "doDeleteWorld",
            at = @At("RETURN")
    )
    public void onDoDeleteWorldEnd(CallbackInfo ci) {
        TemperatureBands.isDeleteScreenActive = false;
    }
}
