package github.cosmicdan.temperaturebands.fabric;

import net.fabricmc.api.ModInitializer;

import github.cosmicdan.temperaturebands.TemperatureBands;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;

public final class TemperatureBandsFabric implements ModInitializer {
    private static TemperatureBands INSTANCE;

    @Override
    public void onInitialize() {
        INSTANCE = new TemperatureBands(new ModPlatformFabric());
        // MinecraftServer#getWorldPath(LevelResource.ROOT)
    }
}
