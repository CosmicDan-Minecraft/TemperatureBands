package github.cosmicdan.temperaturebands.mixin;

import github.cosmicdan.temperaturebands.DimensionConfig;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public abstract class ClientGuiHooks {
    /**
     * Responsible for clearing data/config when the "Create World" screen is cancelled
     */
    @Mixin(CreateWorldScreen.class)
    public static abstract class CreateWorldScreenHooks {
        @Inject(
                method = "popScreen",
                at = @At("HEAD")
        )
        private void onPopScreen(CallbackInfo ci) {
            TemperatureBands.clearDimensionDataAndConfig(null);
        }
    }

    @Mixin(SelectWorldScreen.class)
    public static abstract class SelectWorldScreenHooks {
        @Inject(
                method = "init",
                at = @At("RETURN")
        )
        private void onInit(CallbackInfo ci) {
            if (DimensionConfig.RECREATED_WORLD_SOURCE != null) {
                DimensionConfig.RECREATED_WORLD_SOURCE = null;
                TemperatureBands.logDebug("Cleared RECREATED_WORLD_SOURCE since a fresh Select World screen was opened");
            }
        }
    }

    /**
     * Responsible for detecting if a world is being deleted in order to skip loading config for it
     */
    @Mixin(WorldSelectionList.WorldListEntry.class)
    public static abstract class WorldSelectionListEntryHooks {
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
}
