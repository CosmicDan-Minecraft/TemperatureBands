package github.cosmicdan.temperaturebands;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.IConfigSpec;

public interface IModPlatform {
    void registerConfigCommon(final IConfigSpec<ForgeConfigSpec> spec);
    boolean isClientSide();
}
