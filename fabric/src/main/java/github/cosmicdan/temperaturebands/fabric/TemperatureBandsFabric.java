package github.cosmicdan.temperaturebands.fabric;

import net.fabricmc.api.ModInitializer;

import github.cosmicdan.temperaturebands.TemperatureBands;

public final class TemperatureBandsFabric implements ModInitializer {
    private static TemperatureBands INSTANCE;

    @Override
    public void onInitialize() {
        INSTANCE = new TemperatureBands(new ModPlatformFabric());
        // Fabric loads client/common config immediately, so just call the event callback in common now
        INSTANCE.onConfigLoaded(false);
    }
}
