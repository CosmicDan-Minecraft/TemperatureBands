package github.cosmicdan.temperaturebands.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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
