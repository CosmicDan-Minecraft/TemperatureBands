package github.cosmicdan.temperaturebands.mixin;

import com.mojang.datafixers.DataFixer;
import github.cosmicdan.temperaturebands.TemperatureBands;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Mixin(LevelStorageSource.class)
abstract class LevelStorageSourceHooks {
    @Shadow public abstract Path getBaseDir();

    @Inject(
            method = "createAccess",
            at = @At("TAIL")
    )
    private void onCreateAccess(String saveName, CallbackInfoReturnable<LevelStorageSource.LevelStorageAccess> cir) {
        Path saveDir = getBaseDir().toAbsolutePath().resolve(saveName);
        TemperatureBands.onLevelStorageLoad(saveDir);
    }

    @Inject(
            method = "validateAndCreateAccess",
            at = @At("TAIL")
    )
    private void onValidateAndCreateAccess(String saveName, CallbackInfoReturnable<LevelStorageSource.LevelStorageAccess> cir) {
        Path saveDir = getBaseDir().toAbsolutePath().resolve(saveName);
        TemperatureBands.onLevelStorageLoad(saveDir);
    }
}
