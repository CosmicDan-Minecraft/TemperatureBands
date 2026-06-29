package github.cosmicdan.temperaturebands.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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

    @Mixin(LevelLoadingScreen.class)
    public static abstract class LevelLoadingScreenHooks extends Screen {
        @Shadow
        @Final
        private StoringChunkProgressListener progressListener;

        protected LevelLoadingScreenHooks(Component component) {
            super(component);
        }

        @WrapOperation(
                method = "render",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V")
        )
        public void onDrawCenteredString(GuiGraphics guiGraphics, Font font, String string, int i, int j, int k, Operation<Void> original) {
            if (TemperatureBands.addLoadingScreenText && progressListener.getProgress() < 2) {
                guiGraphics.drawCenteredString(font, "Climate Sampler is warming up, standby...", i, j, k);
            } else {
                original.call(guiGraphics, font, string, i, j, k);
            }
        }
    }
}
