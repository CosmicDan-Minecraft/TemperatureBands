package github.cosmicdan.temperaturebands.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.cosmicdan.temperaturebands.DimensionConfig;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
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

    /**
     * Responsible for showing Stronghold calculation progress on level loading screen
     */
    @Mixin(LevelLoadingScreen.class)
    public static abstract class LevelLoadingScreenHooks extends Screen {
        protected LevelLoadingScreenHooks(Component component) {
            super(component);
        }

        @WrapOperation(
                method = "render",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;renderChunks(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/server/level/progress/StoringChunkProgressListener;IIII)V")
        )
        public void onRenderChunks(GuiGraphics arg, StoringChunkProgressListener arg2, int i, int j, int k, int l, Operation<Void> original) {
            if (TemperatureBands.CONFIG_GLOBAL.showStrongholdProgress() && TemperatureBands.strongholdCountTotal.get() != -1) {
                int posX = width / 2;
                int posY = height / 2;
                arg.drawCenteredString(font, "Stronghold spawn calculation in progress", posX, posY, 0xFFFFFF);
                posY += font.lineHeight;
                arg.drawCenteredString(font, "This may take some time for some worldgen mods!", posX, posY, 0xFFFFFF);
                posY += font.lineHeight + font.lineHeight;
                MutableComponent string = MutableComponent.create(new TranslatableContents(null, "%s/%s completed", new Object[]{TemperatureBands.strongholdCountCurrent.get(), TemperatureBands.strongholdCountTotal}));
                arg.drawCenteredString(font, string, posX, posY, 0xFFFFFF);
                posY += font.lineHeight + font.lineHeight + font.lineHeight;
                arg.drawCenteredString(font, "Tip: If you want to speed this up, check Temperature Bands config.", posX, posY, 0xFFFFFF);
            } else {
                original.call(arg, arg2, i, j, k, l);
            }
        }
    }
}
